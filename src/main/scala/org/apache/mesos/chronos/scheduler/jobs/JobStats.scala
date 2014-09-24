package org.apache.mesos.chronos.scheduler.jobs

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.{Level, Logger}

import org.apache.mesos.chronos.scheduler.config.CassandraConfiguration
import com.datastax.driver.core._
import com.datastax.driver.core.exceptions.{DriverException, NoHostAvailableException, QueryExecutionException, QueryValidationException}
import com.datastax.driver.core.Row
import com.datastax.driver.core.querybuilder.{QueryBuilder, Insert}
import com.google.inject.Inject
import org.apache.mesos.Protos.{TaskState, TaskStatus}
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.collection.mutable.{HashMap}

object CurrentState extends Enumeration {
  type CurrentState = Value
  val idle, queued, running = Value
}

class JobStats @Inject()(clusterBuilder: Option[Cluster.Builder], config: CassandraConfiguration) {

  // Cassandra table column names
  private val TASK_ID: String  = "id"
  private val TIMESTAMP: String = "ts"
  private val JOB_NAME: String = "job_name"
  private val JOB_OWNER: String = "job_owner"
  private val JOB_SCHEDULE: String = "job_schedule"
  private val JOB_PARENTS: String = "job_parents"
  private val TASK_STATE: String = "task_state"
  private val SLAVE_ID: String = "slave_id"
  private val MESSAGE: String = "message"
  private val ATTEMPT: String = "attempt"
  private val IS_FAILURE: String = "is_failure"

  protected val jobStates = new HashMap[String, CurrentState.Value]()

  val log = Logger.getLogger(getClass.getName)
  val statements = new ConcurrentHashMap[String, PreparedStatement]().asScala
  var _session: Option[Session] = None

  def getJobState(jobName: String) : CurrentState.Value = {
    /**
     * NOTE: currently everything stored in memory, look into moving
     * this to Cassandra. ZK is not an option cause serializers and
     * deserializers need to be written. Need a good solution, potentially
     * lots of writes and very few reads (only on failover)
     */
    val status = jobStates.get(jobName) match {
      case Some(s) =>
        s
      case _ =>
        CurrentState.idle
    }
    status
  }

  def updateJobState(jobName: String, state: CurrentState.Value) {
    var shouldUpdate = true
    jobStates.get(jobName) match {
      case Some(s: CurrentState.Value) => {
        if ((s == CurrentState.running) &&
            (state == CurrentState.queued)) {
          //don't update status if already running
          shouldUpdate = false
        }
      }
      case None =>
    }

    if (shouldUpdate) {
      log.info("Updating state for job (%s) to %s".format(jobName, state))
      jobStates.put(jobName, state)
    }
  }

  def removeJobState(job: BaseJob) {
    jobStates.remove(job.name)
  }

  def jobQueued(job: BaseJob, attempt: Int) {
    updateJobState(job.name, CurrentState.queued)
  }

  def jobStarted(job: BaseJob, taskStatus: TaskStatus, attempt: Int) {
    updateJobState(job.name, CurrentState.running)

    var jobSchedule:Option[String] = None
    var jobParents:Option[java.util.Set[String]] = None
    job match {
      case job: ScheduleBasedJob =>
        jobSchedule = Some(job.schedule)
      case job: DependencyBasedJob =>
        jobParents = Some(job.parents.asJava)
    }
    insertToStatTable(
            id=Some(taskStatus.getTaskId.getValue),
            timestamp=Some(new java.util.Date()),
            jobName=Some(job.name),
            jobOwner=Some(job.owner),
            jobSchedule=jobSchedule,
            jobParents=jobParents,
            taskState=Some(taskStatus.getState.toString),
            slaveId=Some(taskStatus.getSlaveId.getValue),
            message=None,
            attempt=Some(attempt),
            isFailure=None)
  }

  def getSession: Option[Session] = {
    _session match {
      case Some(s) => Some(s)
      case None =>
        clusterBuilder match {
          case Some(c) =>
            try {
              val session = c.build.connect(config.cassandraKeyspace())
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

  def jobFinished(job: BaseJob, taskStatus: TaskStatus, attempt: Int) {
    updateJobState(job.name, CurrentState.idle)

    var jobSchedule:Option[String] = None
    var jobParents:Option[java.util.Set[String]] = None
    job match {
      case job: ScheduleBasedJob =>
        jobSchedule = Some(job.schedule)
      case job: DependencyBasedJob =>
        jobParents = Some(job.parents.asJava)
    }
    insertToStatTable(
            id=Some(taskStatus.getTaskId.getValue),
            timestamp=Some(new java.util.Date()),
            jobName=Some(job.name),
            jobOwner=Some(job.owner),
            jobSchedule=jobSchedule,
            jobParents=jobParents,
            taskState=Some(taskStatus.getState.toString),
            slaveId=Some(taskStatus.getSlaveId.getValue),
            message=None,
            attempt=Some(attempt),
            isFailure=None)
  }

  def jobFailed(job: BaseJob, taskStatus: TaskStatus, attempt: Int) {
    updateJobState(job.name, CurrentState.idle)

    var jobSchedule:Option[String] = None
    var jobParents:Option[java.util.Set[String]] = None
    job match {
      case job: ScheduleBasedJob =>
        jobSchedule = Some(job.schedule)
      case job: DependencyBasedJob =>
        jobParents = Some(job.parents.asJava)
    }
    insertToStatTable(
            id=Some(taskStatus.getTaskId.getValue),
            timestamp=Some(new java.util.Date()),
            jobName=Some(job.name),
            jobOwner=Some(job.owner),
            jobSchedule=jobSchedule,
            jobParents=jobParents,
            taskState=Some(taskStatus.getState.toString),
            slaveId=Some(taskStatus.getSlaveId.getValue),
            message=Some(taskStatus.getMessage),
            attempt=Some(attempt),
            isFailure=Some(true))
  }

  /**
   * Overloaded method of jobFailed. Reports that a job identified by only
   * its job name failed during execution. This is only used to report
   * a failure when there is no corresponding job object, which only happens
   * when a job is destroyed. When a job is destroyed, all tasks are killed
   * and this method is called when a task is killed.
   * @param jobName
   * @param taskStatus
   * @param attempt
   */
  def jobFailed(jobName: String, taskStatus: TaskStatus, attempt: Int) {
    updateJobState(jobName, CurrentState.idle)

    insertToStatTable(
        id=Some(taskStatus.getTaskId.getValue),
        timestamp=Some(new java.util.Date()),
        jobName=Some(jobName),
        jobOwner=None, jobSchedule=None, jobParents=None,
        taskState=Some(taskStatus.getState().toString()),
        slaveId=Some(taskStatus.getSlaveId().getValue()),
        message=Some(taskStatus.getMessage()),
        attempt=Some(attempt),
        isFailure=Some(true))
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
      getSession match {
        case Some(session: Session) =>
          var query:Insert = QueryBuilder.insertInto(config.cassandraTable())

          //set required values (let these throw an exception)
          query.value(TASK_ID , id.get)
            .value(JOB_NAME , jobName.get)
            .value(TIMESTAMP , timestamp.get)

          jobOwner match {
            case Some(jo: String) => query.value(JOB_OWNER , jo)
            case _ =>
          }
          jobSchedule match {
            case Some(js: String) => query.value(JOB_SCHEDULE , js)
            case _ =>
          }
          jobParents match {
            case Some(jp: java.util.Set[String]) => query.value(JOB_PARENTS , jp)
            case _ =>
          }
          taskState match {
            case Some(ts: String) => query.value(TASK_STATE , ts)
            case _ =>
          }
          slaveId match {
            case Some(s: String) => query.value(SLAVE_ID , s)
            case _ =>
          }
          message match {
            case Some(m: String) => query.value(MESSAGE , m)
            case _ =>
          }
          attempt match {
            case Some(a: Integer) => query.value(ATTEMPT , a)
            case _ =>
          }
          isFailure match {
            case Some(f: Boolean) => query.value(IS_FAILURE , f)
            case _ =>
          }

          query.setConsistencyLevel(ConsistencyLevel.valueOf(config.cassandraConsistency()))
            .asInstanceOf[RegularStatement]

          session.executeAsync(query)
        case None =>
      }
    } catch {
      case e: NoHostAvailableException =>
        resetSession()
        log.log(Level.WARNING, "No hosts were available, will retry next time.", e)
      case e: QueryExecutionException =>
        log.log(Level.WARNING,"Query execution failed: ", e)
      case e: QueryValidationException =>
        log.log(Level.WARNING,"Query validation failed: ", e)
    }

  }
}
