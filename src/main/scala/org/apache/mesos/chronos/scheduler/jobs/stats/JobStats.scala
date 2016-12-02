package org.apache.mesos.chronos.scheduler.jobs.stats

import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.{Level, Logger}

import com.datastax.driver.core._
import com.datastax.driver.core.exceptions.{DriverException, NoHostAvailableException, QueryExecutionException, QueryValidationException}
import com.datastax.driver.core.querybuilder.{Insert, QueryBuilder}
import com.google.inject.Inject
import org.apache.mesos.Protos.{TaskState, TaskStatus}
import org.apache.mesos.chronos.scheduler.config.CassandraConfiguration
import org.apache.mesos.chronos.scheduler.jobs._
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.collection._

object CurrentState extends Enumeration {
  type CurrentState = Value
  val idle, queued, running = Value
}

class JobStats @Inject()(clusterBuilder: Option[Cluster.Builder], config: CassandraConfiguration) {

  val log = Logger.getLogger(getClass.getName)
  val statements = new ConcurrentHashMap[String, PreparedStatement]().asScala
  protected val jobStates = new mutable.HashMap[String, String]()
  // Cassandra table column names
  private val ATTEMPT: String = "attempt"
  private val ELEMENTS_PROCESSED = "elements_processed"
  private val IS_FAILURE: String = "is_failure"
  private val JOB_NAME: String = "job_name"
  private val JOB_OWNER: String = "job_owner"
  private val JOB_PARENTS: String = "job_parents"
  private val JOB_SCHEDULE: String = "job_schedule"
  private val MESSAGE: String = "message"
  private val SLAVE_ID: String = "slave_id"
  private val TASK_ID: String = "id"
  private val TASK_STATE: String = "task_state"
  private val TIMESTAMP: String = "ts"
  private val terminalStates = Set(TaskState.TASK_FINISHED, TaskState.TASK_FAILED, TaskState.TASK_KILLED, TaskState.TASK_LOST).map(_.toString)
  var _session: Option[Session] = None

  def getJobState(jobName: String): String = {
    /**
      * NOTE: currently everything stored in memory, look into moving
      * this to Cassandra. ZK is not an option cause serializers and
      * deserializers need to be written. Need a good solution, potentially
      * lots of writes and very few reads (only on failover)
      */
    jobStates.getOrElse(jobName, CurrentState.idle.toString)
  }

  def updateJobState(jobName: String, nextState: CurrentState.Value, count: Option[Int] = None) {
    val c = count.getOrElse(0)
    val updatedState = if (c > 0) {
      s"$c running"
    } else {
      nextState.toString
    }
    val shouldUpdate = jobStates.get(jobName).forall(
      prevState =>
        !(prevState.contains("running") && nextState == CurrentState.queued)
    )

    if (shouldUpdate) {
      log.info("Updating state for job (%s) to %s".format(jobName, nextState))
      jobStates.put(jobName, updatedState)
    }
  }

  /**
    * Returns most recent tasks by job and returns only numTasks
    *
    * @param job      job to search the tasks for
    * @param numTasks maximum number of tasks to return
    * @return returns a list of past and currently running tasks,
    *         the first element is the most recent.
    */
  def getMostRecentTaskStatsByJob(job: BaseJob, numTasks: Int): List[TaskStat] = {

    val sortedDescTaskStatList = getParsedTaskStatsByJob(job).sortWith(recentDateCompareFnc).slice(0, numTasks)

    if (job.dataProcessingJobType) {
      /*
       * Retrieve stat count for these tasks. This should be done
       * after slicing as an optimization.
       */
      sortedDescTaskStatList.foreach {
        taskStat => taskStat.numElementsProcessed = getTaskStatCount(job, taskStat.taskId)
      }
    }

    sortedDescTaskStatList
  }

  /**
    * Compare function for TaskStat by most recent date.
    */
  private def recentDateCompareFnc(a: TaskStat, b: TaskStat): Boolean = {
    val compareAscDate = a.taskStartTs match {
      case Some(aTs: DateTime) =>
        b.taskStartTs match {
          case Some(bTs: DateTime) => aTs.compareTo(bTs) <= 0
          case None => false
        }
      case None => true
    }
    !compareAscDate
  }

  /**
    * Queries Cassandra stat table to get the element processed count
    * for a specific job and a specific task
    *
    * @param job    job to find stats for
    * @param taskId task id for which to find stats for
    * @return element processed count
    */
  private def getTaskStatCount(job: BaseJob, taskId: String): Option[Long] = {
    var taskStatCount: Option[Long] = None
    try {
      getSession.foreach {
        session =>
          val query = s"SELECT * FROM ${config.cassandraStatCountTable()} WHERE job_name='${job.name}' AND task_id='$taskId';"
          val prepared = statements.getOrElseUpdate(query,
            session.prepare(
              new SimpleStatement(query)
                .setConsistencyLevel(readConsistencyLevel())
                .asInstanceOf[RegularStatement]
            )
          )
          val resultSet = session.execute(prepared.bind())

          //should just be one row
          val resultRow = resultSet.one()
          if (resultRow != null) {
            val cDef = resultRow.getColumnDefinitions
            if (cDef.contains(ELEMENTS_PROCESSED)) {
              taskStatCount = Some(resultRow.getLong(ELEMENTS_PROCESSED))
            }
          } else {
            log.info("No elements processed count found for job_name %s taskId %s".format(job.name, taskId))
          }
      }
    } catch {
      case e: NoHostAvailableException =>
        resetSession()
        log.log(Level.WARNING, "No hosts were available, will retry next time.", e)
      case e: QueryExecutionException =>
        log.log(Level.WARNING, "Query execution failed:", e)
      case e: QueryValidationException =>
        log.log(Level.WARNING, "Query validation failed:", e)
    }
    taskStatCount
  }

  def getSession: Option[Session] = {
    _session match {
      case Some(s) => Some(s)
      case None =>
        clusterBuilder match {
          case Some(c) =>
            try {
              val session = c.build.connect()
              session.execute(new SimpleStatement(
                s"CREATE KEYSPACE IF NOT EXISTS ${config.cassandraKeyspace()} WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };"
              ))
              session.execute(new SimpleStatement(
                s"USE ${config.cassandraKeyspace()};"
              ))

              session.execute(new SimpleStatement(
                s"CREATE TABLE IF NOT EXISTS ${config.cassandraTable()}" +
                  """
                    |(
                    |   id             VARCHAR,
                    |   ts             TIMESTAMP,
                    |   job_name       VARCHAR,
                    |   job_owner      VARCHAR,
                    |   job_schedule   VARCHAR,
                    |   job_parents    SET<VARCHAR>,
                    |   task_state     VARCHAR,
                    |   slave_id       VARCHAR,
                    |   message        VARCHAR,
                    |   attempt        INT,
                    |   is_failure     BOOLEAN,
                    | PRIMARY KEY (id, ts))
                    | WITH bloom_filter_fp_chance=0.100000 AND
                    | compaction = {'class':'LeveledCompactionStrategy'}
                  """.stripMargin
              ))
              session.execute(new SimpleStatement(
                s"CREATE INDEX IF NOT EXISTS ON ${config.cassandraTable()} ($JOB_NAME);"
              ))

              /*
               * highest bloom filter to reduce memory consumption and reducing
               * false positives
               */
              session.execute(new SimpleStatement(
                s"CREATE TABLE IF NOT EXISTS ${config.cassandraStatCountTable()}" +
                  """
                    |(
                    |   task_id              VARCHAR,
                    |   job_name             VARCHAR,
                    |   elements_processed   COUNTER,
                    | PRIMARY KEY (job_name, task_id))
                    | WITH bloom_filter_fp_chance=0.100000 AND
                    | compaction = {'class':'LeveledCompactionStrategy'}
                  """.stripMargin
              ))

              _session = Some(session)
              _session
            } catch {
              case e: DriverException =>
                log.log(Level.WARNING, "Caught exception when creating Cassandra JobStats session", e)
                None
            }
          case None => None
        }
    }
  }

  def resetSession() {
    statements.clear()
    _session match {
      case Some(session) =>
        session.close()
      case _ =>
    }
    _session = None
  }

  private def readConsistencyLevel(): ConsistencyLevel = {
    if (ConsistencyLevel.ANY.name().equalsIgnoreCase(config.cassandraConsistency())) {
      //reads do not support ANY
      ConsistencyLevel.ONE
    } else {
      ConsistencyLevel.valueOf(config.cassandraConsistency())
    }
  }

  /**
    * Returns a list of tasks (TaskStat) found for the specified job name
    *
    * @param job job to search for task stats
    * @return list of past and current running tasks for the job
    */
  private def getParsedTaskStatsByJob(job: BaseJob): List[TaskStat] = {
    val taskMap = mutable.Map[String, TaskStat]()

    getTaskDataByJob(job).fold {
      log.info("No row list found for jobName=%s".format(job.name))
    } {
      rowsList =>
        for (row <- rowsList) {
          /*
           * Go through all the rows and construct a job history.
           * Group elements by task id
           */
          if (isValidTaskData(row)) {
            val taskId = row.getString(TASK_ID)
            val taskStat = taskMap.getOrElseUpdate(taskId,
              new TaskStat(taskId,
                row.getString(JOB_NAME),
                row.getString(SLAVE_ID)))
            updateTaskStat(taskStat, row)
          } else {
            log.info("Invalid row found in cassandra table for jobName=%s".format(job.name))
          }
        }
    }

    taskMap.values.toList
  }

  /**
    * Queries Cassandra table for past and current job statistics by jobName
    * and limits by numTasks. The result is not sorted by execution time
    *
    * @param job job to find task data for
    * @return list of cassandra rows
    */
  private def getTaskDataByJob(job: BaseJob): Option[List[Row]] = {
    var rowsListFinal: Option[List[Row]] = None
    try {
      getSession match {
        case Some(session: Session) =>
          val query = s"SELECT * FROM ${config.cassandraTable()} WHERE $JOB_NAME='${job.name}';"
          val prepared = statements.getOrElseUpdate(query,
            session.prepare(
              new SimpleStatement(query)
                .setConsistencyLevel(readConsistencyLevel())
                .asInstanceOf[RegularStatement]
            )
          )

          val resultSet = session.execute(prepared.bind())
          val rowsList = resultSet.all().asScala.toList
          rowsListFinal = Some(rowsList)
        case None => rowsListFinal = None
      }
    } catch {
      case e: NoHostAvailableException =>
        resetSession()
        log.log(Level.WARNING, "No hosts were available, will retry next time.", e)
      case e: QueryExecutionException =>
        log.log(Level.WARNING, "Query execution failed:", e)
      case e: QueryValidationException =>
        log.log(Level.WARNING, "Query validation failed:", e)
    }
    rowsListFinal
  }

  /**
    * Determines if row from Cassandra represents a valid Mesos task
    *
    * @param row cassandra row
    * @return true if valid, false otherwise
    */
  private def isValidTaskData(row: Row): Boolean = {
    if (row == null) {
      false
    } else {
      val cDefs = row.getColumnDefinitions
      cDefs.contains(JOB_NAME) &&
        cDefs.contains(TASK_ID) &&
        cDefs.contains(TIMESTAMP) &&
        cDefs.contains(TASK_STATE) &&
        cDefs.contains(SLAVE_ID) &&
        cDefs.contains(IS_FAILURE)
    }
  }

  /**
    * Parses the contents of the data row and updates the TaskStat object
    *
    * @param taskStat task stat to be updated
    * @param row      data row from which to update the task stat object
    * @return updated TaskStat object
    */
  private def updateTaskStat(taskStat: TaskStat, row: Row): TaskStat = {
    val taskTimestamp = new Date(row.getDate(TIMESTAMP).getMillisSinceEpoch)
    val taskState = row.getString(TASK_STATE)

    if (taskState == TaskState.TASK_RUNNING.toString) {
      taskStat.setTaskStartTs(taskTimestamp)
      taskStat.setTaskStatus(ChronosTaskStatus.Running)
    } else if (terminalStates.contains(taskState)) {
      taskStat.setTaskEndTs(taskTimestamp)
      val status = if (TaskState.TASK_FINISHED.toString == taskState) ChronosTaskStatus.Success else ChronosTaskStatus.Fail
      taskStat.setTaskStatus(status)
    }

    taskStat
  }

  /**
    * Updates the number of elements processed by a task. This method
    * is not idempotent
    *
    * @param job                         job for which to perform the update
    * @param taskId                      task id for which to perform the update
    * @param additionalElementsProcessed number of elements to increment bt
    */
  def updateTaskProgress(job: BaseJob,
                         taskId: String,
                         additionalElementsProcessed: Long) {
    try {
      getSession.foreach {
        session =>
          val validateQuery = s"SELECT * FROM ${config.cassandraTable()} WHERE job_name='${job.name}' AND id='$taskId';"
          var prepared = statements.getOrElseUpdate(validateQuery, {
            session.prepare(
              new SimpleStatement(validateQuery)
                .setConsistencyLevel(readConsistencyLevel())
                .asInstanceOf[RegularStatement]
            )
          })
          val validateResultSet = session.execute(prepared.bind())

          if (validateResultSet.one() != null) {
            /*
             * Only update stat count if entry exists in main table.
             */
            val query = s"UPDATE ${config.cassandraStatCountTable()}" +
              s" SET elements_processed = elements_processed + $additionalElementsProcessed" +
              s" WHERE job_name='${job.name}' AND task_id='$taskId';"
            prepared = statements.getOrElseUpdate(query,
              session.prepare(
                new SimpleStatement(query)
                  .asInstanceOf[RegularStatement]
              )
            )
          } else {
            throw new IllegalArgumentException("Task id  %s not found".format(taskId))
          }
          session.executeAsync(prepared.bind())
      }
    } catch {
      case e: NoHostAvailableException =>
        resetSession()
        log.log(Level.WARNING, "No hosts were available, will retry next time.", e)
      case e: QueryExecutionException =>
        log.log(Level.WARNING, "Query execution failed:", e)
      case e: QueryValidationException =>
        log.log(Level.WARNING, "Query validation failed:", e)
    }
  }

  def asObserver: JobsObserver.Observer = JobsObserver.withName({
    case JobExpired(job, _) => updateJobState(job.name, CurrentState.idle)
    case JobRemoved(job) => removeJobState(job)
    case JobQueued(job, taskId, attempt) => jobQueued(job, taskId, attempt)
    case JobStarted(job, taskStatus, attempt, runningCount) => jobStarted(job, taskStatus, attempt, runningCount)
    case JobFinished(job, taskStatus, attempt, runningCount) => jobFinished(job, taskStatus, attempt, runningCount)
    case JobFailed(job, taskStatus, attempt, runningCount) => jobFailed(job, taskStatus, attempt, runningCount)
  }, getClass.getSimpleName)

  private def removeJobState(job: BaseJob) = jobStates.remove(job.name)

  private def jobQueued(job: BaseJob, taskId: String, attempt: Int) {
    updateJobState(job.name, CurrentState.queued)
  }

  private def jobStarted(job: BaseJob, taskStatus: TaskStatus, attempt: Int, runningCount: Int) {
    updateJobState(job.name, CurrentState.running, Some(runningCount))

    var jobSchedule: Option[String] = None
    var jobParents: Option[java.util.Set[String]] = None
    job match {
      case job: ScheduleBasedJob =>
        jobSchedule = Some(job.schedule)
      case job: DependencyBasedJob =>
        jobParents = Some(job.parents.asJava)
    }
    insertToStatTable(
      id = Some(taskStatus.getTaskId.getValue),
      timestamp = Some(new java.util.Date()),
      jobName = Some(job.name),
      jobOwner = Some(job.owner),
      jobSchedule = jobSchedule,
      jobParents = jobParents,
      taskState = Some(taskStatus.getState.toString),
      slaveId = Some(taskStatus.getSlaveId.getValue),
      message = None,
      attempt = Some(attempt),
      isFailure = None)
  }

  private def jobFinished(job: BaseJob, taskStatus: TaskStatus, attempt: Int, runningCount: Int) {
    updateJobState(job.name, CurrentState.idle, Some(runningCount))

    var jobSchedule: Option[String] = None
    var jobParents: Option[java.util.Set[String]] = None
    job match {
      case job: ScheduleBasedJob =>
        jobSchedule = Some(job.schedule)
      case job: DependencyBasedJob =>
        jobParents = Some(job.parents.asJava)
    }
    insertToStatTable(
      id = Some(taskStatus.getTaskId.getValue),
      timestamp = Some(new java.util.Date()),
      jobName = Some(job.name),
      jobOwner = Some(job.owner),
      jobSchedule = jobSchedule,
      jobParents = jobParents,
      taskState = Some(taskStatus.getState.toString),
      slaveId = Some(taskStatus.getSlaveId.getValue),
      message = None,
      attempt = Some(attempt),
      isFailure = None)
  }

  private def jobFailed(jobNameOrJob: Either[String, BaseJob], taskStatus: TaskStatus, attempt: Int, runningCount: Int): Unit = {
    val jobName = jobNameOrJob.fold(name => name, _.name)
    val jobSchedule = jobNameOrJob.fold(_ => None, {
      case job: ScheduleBasedJob => Some(job.schedule)
      case _ => None
    })
    val jobParents: Option[java.util.Set[String]] = jobNameOrJob.fold(_ => None, {
      case job: DependencyBasedJob => Some(job.parents.asJava)
      case _ => None
    })

    updateJobState(jobName, CurrentState.idle, Some(runningCount))
    insertToStatTable(
      id = Some(taskStatus.getTaskId.getValue),
      timestamp = Some(new java.util.Date()),
      jobName = Some(jobName),
      jobOwner = jobNameOrJob.fold(_ => None, job => Some(job.owner)),
      jobSchedule = jobSchedule,
      jobParents = jobParents,
      taskState = Some(taskStatus.getState.toString),
      slaveId = Some(taskStatus.getSlaveId.getValue),
      message = Some(taskStatus.getMessage),
      attempt = Some(attempt),
      isFailure = Some(true))
  }

  /**
    * Helper method that performs an insert statement to update the
    * job statistics (chronos) table. All arguments are surrounded
    * by options so that a subset of values can be inserted.
    */
  private def insertToStatTable(id: Option[String],
                                timestamp: Option[java.util.Date],
                                jobName: Option[String],
                                jobOwner: Option[String],
                                jobSchedule: Option[String],
                                jobParents: Option[java.util.Set[String]],
                                taskState: Option[String],
                                slaveId: Option[String],
                                message: Option[String],
                                attempt: Option[Integer],
                                isFailure: Option[Boolean]) = {
    try {
      getSession.foreach {
        session =>
          val query: Insert = QueryBuilder.insertInto(config.cassandraTable())

          //set required values (let these throw an exception)
          query.value(TASK_ID, id.get)
            .value(JOB_NAME, jobName.get)
            .value(TIMESTAMP, timestamp.get)

          jobOwner match {
            case Some(jo: String) => query.value(JOB_OWNER, jo)
            case _ =>
          }
          jobSchedule match {
            case Some(js: String) => query.value(JOB_SCHEDULE, js)
            case _ =>
          }
          jobParents match {
            case Some(jp: java.util.Set[String]) => query.value(JOB_PARENTS, jp)
            case _ =>
          }
          taskState match {
            case Some(ts: String) => query.value(TASK_STATE, ts)
            case _ =>
          }
          slaveId match {
            case Some(s: String) => query.value(SLAVE_ID, s)
            case _ =>
          }
          message match {
            case Some(m: String) => query.value(MESSAGE, m)
            case _ =>
          }
          attempt match {
            case Some(a: Integer) => query.value(ATTEMPT, a)
            case _ =>
          }
          isFailure match {
            case Some(f: Boolean) => query.value(IS_FAILURE, f)
            case _ =>
          }

          query.setConsistencyLevel(ConsistencyLevel.valueOf(config.cassandraConsistency()))
            .asInstanceOf[RegularStatement]

          session.executeAsync(query)
      }
    } catch {
      case e: NoHostAvailableException =>
        resetSession()
        log.log(Level.WARNING, "No hosts were available, will retry next time.", e)
      case e: QueryExecutionException =>
        log.log(Level.WARNING, "Query execution failed: ", e)
      case e: QueryValidationException =>
        log.log(Level.WARNING, "Query validation failed: ", e)
    }

  }
}
