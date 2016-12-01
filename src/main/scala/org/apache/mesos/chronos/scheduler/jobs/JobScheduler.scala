package org.apache.mesos.chronos.scheduler.jobs

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.locks.{Lock, ReentrantLock}
import java.util.concurrent.{Executors, Future, TimeUnit}
import java.util.logging.{Level, Logger}

import akka.actor.ActorSystem
import com.google.common.base.Joiner
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.mesos.MesosDriverFactory
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import com.google.common.util.concurrent.AbstractIdleService
import com.google.inject.Inject
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.leader.{LeaderLatch, LeaderLatchListener}
import org.apache.mesos.Protos.TaskStatus
import org.joda.time.{DateTime, DateTimeZone, Duration, Period}

import scala.collection.mutable.ListBuffer

/**
 * Constructs concrete tasks given a  list of schedules and a global scheduleHorizon.
 * The schedule horizon represents the advance-time the schedule is constructed.
 *
 * A lot of the methods in this class are broken into small pieces to allow for better unit testing.
 * @author Florian Leibert (flo@leibert.de)
 */
class JobScheduler @Inject()(val taskManager: TaskManager,
                             val jobGraph: JobGraph,
                             val persistenceStore: PersistenceStore,
                             val mesosDriver: MesosDriverFactory = null,
                             val curator: CuratorFramework = null,
                             val leaderLatch: LeaderLatch = null,
                             val leaderPath: String = null,
                             val jobsObserver: JobsObserver.Observer,
                             val failureRetryDelay: Long = 60000,
                             val disableAfterFailures: Long = 0,
                             val jobMetrics: JobMetrics)
//Allows us to let Chaos manage the lifecycle of this class.
  extends AbstractIdleService {

  val localExecutor = Executors.newFixedThreadPool(1)
  val schedulerThreadFuture = new AtomicReference[Future[_]]
  val leaderExecutor = Executors.newSingleThreadExecutor()
  val lock = new ReentrantLock
  val condition = lock.newCondition

  val actorSystem = ActorSystem()
  val akkaScheduler = actorSystem.scheduler

  //TODO(FL): Take some methods out of this class.
  val running = new AtomicBoolean(false)
  val leader = new AtomicBoolean(false)
  val conditionNotified = new AtomicBoolean(false)
  private[this] val log = Logger.getLogger(getClass.getName)

  def isLeader: Boolean = leader.get()

  def getLeader: String = {
    try {
      leaderLatch.getLeader.getId
    } catch {
      case e: Exception =>
        log.log(Level.SEVERE, "Error trying to talk to zookeeper. Exiting.", e)
        System.exit(1)
        null
    }
  }

  /**
   * Update job definition
   * @param oldJob job definition
   * @param newJob new job definition
   */
  def updateJob(oldJob: BaseJob, newJob: BaseJob) {
    //TODO(FL): Ensure we're using job-ids rather than relying on jobs names for identification.
    assert(newJob.name == oldJob.name, "Renaming jobs is currently not supported!")

    replaceJob(oldJob, newJob)
  }

  def notifyCondition(): Unit = {
    // Notify run() that there may be new work
    lock.lock()
    try {
      conditionNotified.set(true)
      condition.signal()
    } finally {
      lock.unlock()
    }
  }

  def reset(purgeQueue: Boolean = false) {
    lock.synchronized {
      jobGraph.reset()
      if (purgeQueue) {
        log.warning("Purging locally queued tasks!")
        taskManager.flush()
      }
    }
  }

  def loadJob(job: BaseJob) {
    lock.synchronized {
      log.info("Persisting job:" + job.name)
      persistenceStore.persistJob(job)
    }
    notifyCondition()
  }

  def registerJobs(jobs: List[BaseJob]): List[ScheduleBasedJob] = {
    var scheduledJobList = List[ScheduleBasedJob]()
    lock.synchronized {
      require(isLeader, "Cannot register a job with this scheduler, not the leader!")
      val scheduleBasedJobs = ListBuffer[ScheduleBasedJob]()
      val dependencyBasedJobs = ListBuffer[DependencyBasedJob]()

      jobs.foreach {
        case x: DependencyBasedJob =>
          dependencyBasedJobs += x
        case x: ScheduleBasedJob =>
          scheduleBasedJobs += x
        case x: Any =>
          throw new IllegalStateException("Error, job is neither ScheduleBased nor DependencyBased:" + x.toString)
      }

      if (scheduleBasedJobs.nonEmpty) {
        val newScheduledJobs = scheduleBasedJobs.sortWith({(lhs, rhs) =>
          JobUtils.getScheduledTime(lhs).isBefore(JobUtils.getScheduledTime(rhs))
        })
        scheduleBasedJobs.foreach({
          job =>
            jobGraph.lookupVertex(job.name) match {
              case Some(_) =>
                replaceJob(job, job)
              case _ =>
                jobGraph.addVertex(job)
            }
        })
        if (newScheduledJobs.nonEmpty) {
          scheduledJobList = newScheduledJobs.toList
        }
      }

      dependencyBasedJobs.foreach {
        job =>
          jobGraph.lookupVertex(job.name) match {
            case Some(_) =>
              replaceJob(job, job)
            case _ =>
              jobGraph.addVertex(job)
          }
      }
      dependencyBasedJobs.foreach {
        job =>
          import scala.collection.JavaConversions._
          log.info("Adding dependencies for %s -> [%s]".format(job.name, Joiner.on(",").join(job.parents)))

          jobGraph.parentJobsOption(job) match {
            case None =>
              log.warning(s"Coudn't find all parents of job ${job.name}... dropping it.")
              jobGraph.removeVertex(job)
            case Some(parentJobs) =>
              parentJobs.foreach {
                //Setup all the dependencies
                parentJob: BaseJob =>
                  jobGraph.addDependency(parentJob.name, job.name)
              }
          }
      }
    }
    scheduledJobList
  }

  def deregisterJob(job: BaseJob) {
    require(isLeader, "Cannot deregister a job with this scheduler, not the leader!")
    lock.synchronized {
      log.info("Removing vertex")

      jobGraph.getChildren(job.name)
        .map(x => jobGraph.lookupVertex(x).get)
        .filter {
          case j: DependencyBasedJob => true
          case _ => false
        }
        .map(x => x.asInstanceOf[DependencyBasedJob])
        .filter(x => x.parents.size > 1)
        .foreach({
          childJob =>
            log.info("Updating job %s".format(job.name))
            val copy = childJob.copy(parents = childJob.parents.filter(_ != job.name))
            updateJob(childJob, copy)
        })

      jobGraph.removeVertex(job)
      taskManager.cancelMesosTasks(job)
      jobsObserver.apply(JobRemoved(job))

      log.info("Removing job from underlying state abstraction:" + job.name)
      persistenceStore.removeJob(job)
    }
  }

  def handleStartedTask(taskStatus: TaskStatus, count: Int) {
    val taskId = taskStatus.getTaskId.getValue
    if (!TaskUtils.isValidVersion(taskId)) {
      log.warning("Found old or invalid task, ignoring!")
      return
    }
    val jobName = TaskUtils.getJobNameForTaskId(taskId)
    val jobOption = jobGraph.lookupVertex(jobName)

    if (jobOption.isEmpty) {
      log.warning("JobSchedule '%s' no longer registered.".format(jobName))
    } else {
      val job = jobOption.get
      val (_, _, attempt, _) = TaskUtils.parseTaskId(taskId)
      jobsObserver.apply(JobStarted(job, taskStatus, attempt, count))

      job match {
        case j: DependencyBasedJob =>
          jobGraph.resetDependencyInvocations(j.name)
        case _ =>
      }

      val newJob = getNewRunningJob(job)
      replaceJob(job, newJob)
    }
    notifyCondition()
  }

  /**
   * Takes care of follow-up actions for a finished task, i.e. update the job schedule in the persistence store or
   * launch tasks for dependent jobs
   */
  def handleFinishedTask(taskStatus: TaskStatus, taskDate: Option[DateTime] = None, count: Int) {
    // `taskDate` is purely for unit testing
    val taskId = taskStatus.getTaskId.getValue
    if (!TaskUtils.isValidVersion(taskId)) {
      log.warning("Found old or invalid task, ignoring!")
      return
    }

    val jobName = TaskUtils.getJobNameForTaskId(taskId)
    val jobOption = jobGraph.lookupVertex(jobName)

    if (jobOption.isEmpty) {
      log.warning("JobSchedule '%s' no longer registered.".format(jobName))
    } else {
      val (_, start, attempt, _) = TaskUtils.parseTaskId(taskId)
      jobMetrics.updateJobStat(jobName, timeMs = DateTime.now(DateTimeZone.UTC).getMillis - start)
      jobMetrics.updateJobStatus(jobName, success = true)
      val job = jobOption.get
      jobsObserver.apply(JobFinished(job, taskStatus, attempt, count))

      val newJob = getNewSuccessfulJob(job)
      replaceJob(job, newJob)
      processDependencies(jobName, taskDate)

      log.fine("Cleaning up finished task '%s'".format(taskId))

      /* TODO(FL): Fix.
         Cleanup potentially exhausted job. Note, if X tasks were fired within a short period of time (~ execution time
        of the job, the first returning Finished-task may trigger deletion of the job! This is a known limitation and
        needs some work but should only affect long running frequent finite jobs or short finite jobs with a tiny pause
        in between */
      job match {
        case job: ScheduleBasedJob =>
          val scheduleBasedJob: ScheduleBasedJob = newJob.asInstanceOf[ScheduleBasedJob]
          Iso8601Expressions.parse(scheduleBasedJob.schedule, scheduleBasedJob.scheduleTimeZone) match {
            case Some((recurrences, _, _)) =>
              if (recurrences == 0) {
                log.info("Disabling job that reached a zero-recurrence count!")

                val disabledJob: ScheduleBasedJob = scheduleBasedJob.copy(disabled = true)
                jobsObserver.apply(JobDisabled(job, """JobSchedule '%s' has exhausted all of its recurrences and has been disabled.
                                                        |Please consider either removing your job, or updating its schedule and re-enabling it.
                                                      """.stripMargin.format(job.name)))
                replaceJob(scheduleBasedJob, disabledJob)
              }
            case None =>
          }
        case _ =>
      }
    }
  }


  /**
   * Mark job by job name as successful. Trigger any dependent children jobs that should be run as a result
   */
  def markJobSuccessAndFireOffDependencies(jobName : String): Boolean = {
    val optionalJob = jobGraph.getJobForName(jobName)
    if (optionalJob.isEmpty) {
      log.warning("%s not found in job graph, not marking success".format(jobName))
      return false
    } else {
      val job = optionalJob.get
      jobMetrics.updateJobStatus(jobName, success = true)
      val newJob = getNewSuccessfulJob(job)
      replaceJob(job, newJob)
      log.info("Resetting dependency invocations for %s".format(newJob))
      jobGraph.resetDependencyInvocations(jobName)
      log.info("Processing dependencies for %s".format(jobName))
      processDependencies(jobName, Option(DateTime.parse(newJob.lastSuccess)))
    }
    true
  }

  def getNewSuccessfulJob(job: BaseJob): BaseJob = {
    val newJob = job match {
      case job: ScheduleBasedJob =>
        val now = DateTime.now(DateTimeZone.UTC)
        job.copy(successCount = job.successCount + 1,
          errorsSinceLastSuccess = 0,
          lastSuccess = now.toString
        )
      case job: DependencyBasedJob =>
        job.copy(successCount = job.successCount + 1,
          errorsSinceLastSuccess = 0,
          lastSuccess = DateTime.now(DateTimeZone.UTC).toString)
      case _ =>
        throw new scala.IllegalArgumentException("Cannot handle unknown task type")
    }
    newJob
  }

  def getNewRunningJob(job: BaseJob): BaseJob = {
    val newJob = job match {
      case job: ScheduleBasedJob =>
        val now = DateTime.now(DateTimeZone.UTC)
        val nextJobSchedule = JobUtils.skipForward(job, now).get
        job.copy(
          schedule = nextJobSchedule.schedule
        )
      case job: DependencyBasedJob =>
        job.copy()
      case _ =>
        throw new scala.IllegalArgumentException("Cannot handle unknown task type")
    }
    newJob
  }

  def replaceJob(oldJob: BaseJob, newJob: BaseJob) {
    lock.synchronized {
      jobGraph.replaceVertex(oldJob, newJob)
      persistenceStore.persistJob(newJob)
    }
  }

  private def processDependencies(jobName: String, taskDate: Option[DateTime]) {
    val dependents = jobGraph.getExecutableChildren(jobName)
    if (dependents.nonEmpty) {
      log.fine("%s has dependents: %s .".format(jobName, dependents.mkString(",")))
      dependents.foreach {
        //TODO(FL): Ensure that the job for the given x exists. Lock.
        x =>
          val dependentJob = jobGraph.getJobForName(x).get
          if (!dependentJob.disabled) {
            val date = taskDate match {
              case Some(d) => d
              case None => DateTime.now(DateTimeZone.UTC)
            }
            taskManager.enqueue(TaskUtils.getTaskId(dependentJob,
              date), dependentJob.highPriority)

            log.fine("Enqueued depedent job." + x)
          }
      }
    } else {
      log.fine("%s does not have any ready dependents.".format(jobName))
    }
  }

  def handleFailedTask(taskStatus: TaskStatus, count: Int) {
    val taskId = taskStatus.getTaskId.getValue
    if (!TaskUtils.isValidVersion(taskId)) {
      log.warning("Found old or invalid task, ignoring!")
    } else {
      val (jobName, _, attempt, _) = TaskUtils.parseTaskId(taskId)
      log.warning("Task of job: %s failed.".format(jobName))
      val jobOption = jobGraph.lookupVertex(jobName)
      jobOption match {
        case Some(job) =>
          jobsObserver.apply(JobFailed(Right(job), taskStatus, attempt, count))

          val hasAttemptsLeft: Boolean = attempt < job.retries
          val hadRecentSuccess: Boolean = try {
            job.lastError.length > 0 && job.lastSuccess.length > 0 &&
              (DateTime.parse(job.lastSuccess).getMillis - DateTime.parse(job.lastError).getMillis) >= 0
          } catch {
            case ex: IllegalArgumentException =>
              log.warning(s"Couldn't parse last run date from ${job.name}")
              false
            case _: Exception => false
          }

          if (hasAttemptsLeft && (job.lastError.length == 0 || hadRecentSuccess)) {
            log.warning("Retrying job: %s, attempt: %d".format(jobName, attempt))
            /* Schedule the retry up to 60 seconds in the future */
            val delayDuration = new Duration(failureRetryDelay)
            val newTaskId = TaskUtils.getTaskId(job, DateTime.now(DateTimeZone.UTC)
              .plus(delayDuration), attempt + 1)
            val delayedTask = new Runnable {
              def run() {
                log.info(s"Enqueuing failed task $newTaskId")
                taskManager.enqueue(newTaskId, job.highPriority)
              }
            }
            implicit val executor = actorSystem.dispatcher

            akkaScheduler.scheduleOnce(
              delay = scala.concurrent.duration.Duration(delayDuration.getMillis, TimeUnit.MILLISECONDS),
              runnable = delayedTask)
          } else {
            val disableJob =
              (disableAfterFailures > 0) && (job.errorsSinceLastSuccess + 1 >= disableAfterFailures)

            val lastErrorTime = DateTime.now(DateTimeZone.UTC)
            val newJob = {
              job match {
                case job: ScheduleBasedJob =>
                  job.copy(errorCount = job.errorCount + 1,
                    errorsSinceLastSuccess = job.errorsSinceLastSuccess + 1,
                    lastError = lastErrorTime.toString, disabled = disableJob)
                case job: DependencyBasedJob =>
                  job.copy(errorCount = job.errorCount + 1,
                    errorsSinceLastSuccess = job.errorsSinceLastSuccess + 1,
                    lastError = lastErrorTime.toString, disabled = disableJob)
                case _ => throw new IllegalArgumentException("Cannot handle unknown task type")
              }
            }
            updateJob(job, newJob)
            if (job.softError) processDependencies(jobName, Option(lastErrorTime))

            // Handle failure by either disabling the job and notifying the owner,
            // or just notifying the owner.
            if (disableJob) {
              log.warning("JobSchedule failed beyond retries! JobSchedule will now be disabled after "
                + newJob.errorsSinceLastSuccess + " failures (disableAfterFailures=" + disableAfterFailures + ").")
              val msg = "\nFailed at '%s', %d failures since last success\nTask id: %s\n"
                .format(DateTime.now(DateTimeZone.UTC), newJob.errorsSinceLastSuccess, taskId)
              jobsObserver.apply(JobDisabled(job, TaskUtils.appendSchedulerMessage(msg, taskStatus)))
            } else {
              log.warning("JobSchedule failed beyond retries!")
              jobsObserver.apply(JobRetriesExhausted(job, taskStatus, attempt))
            }
            jobMetrics.updateJobStatus(jobName, success = false)
          }
        case None =>
          log.warning("Could not find job for task: %s JobSchedule may have been deleted while task was in flight!"
            .format(taskId))
      }
    }
  }

  /**
   * Task has been killed. Do appropriate cleanup
   * Possible reasons for task being killed:
   *   -invoked kill via task manager API
   *   -job is deleted
   */
  def handleKilledTask(taskStatus: TaskStatus, count: Int) {
    val taskId = taskStatus.getTaskId.getValue
    if (!TaskUtils.isValidVersion(taskId)) {
      log.warning("Found old or invalid task, ignoring!")
      return
    }

    val (jobName, start, attempt, _) = TaskUtils.parseTaskId(taskId)
    val jobOption = jobGraph.lookupVertex(jobName)

    jobsObserver.apply(JobFailed(jobOption.toRight(jobName), taskStatus, attempt, count))
  }

  def nanosUntilNextJob(scheduledJobs: List[ScheduleBasedJob]): Long = {
    scheduledJobs.foreach {
      job =>
        Iso8601Expressions.parse(job.schedule, job.scheduleTimeZone) match {
          case Some((_, schedule, _)) =>
            if (!job.disabled) {
              val nanos = new Duration(DateTime.now(DateTimeZone.UTC), schedule).getMillis * 1000000
              if (nanos > 0) {
                return nanos
              }
              return 0
            }
          case _ =>
        }
    }
    60000000000l // 60 seconds
  }

  def mainLoop() {
    log.info("Starting main loop for JobScheduler. CurrentTime: %s".format(DateTime.now(DateTimeZone.UTC)))
    var nanos = 1000000000l // 1 second
    while (running.get) {
      lock.lock()
      try {
        if (!conditionNotified.getAndSet(false) && nanos > 0) {
          condition.awaitNanos(nanos)
        }
      } finally {
        lock.unlock()
      }
      lock.synchronized {
        try {
          log.info("Reloading jobs")
          val baseJobs = JobUtils.loadJobs(persistenceStore)
          val scheduledJobs = registerJobs(baseJobs)
          val (jobsToRun, jobsNotToRun) = getJobsToRun(scheduledJobs)
          log.info(s"jobsToRun.size=${jobsToRun.size}, jobsNotToRun.size=${jobsNotToRun.size}")
          runJobs(jobsToRun)
          nanos = nanosUntilNextJob(jobsNotToRun)
        } catch {
          case e: Exception =>
            log.log(Level.SEVERE, "Loading tasks or jobs failed. Exiting.", e)
            System.exit(1)
        }
      }
    }
    log.info("No longer running.")
  }

  def getJobsToRun(scheduledJobs: List[ScheduleBasedJob]): (List[ScheduleBasedJob], List[ScheduleBasedJob]) = {
    val now = DateTime.now(DateTimeZone.UTC)
    scheduledJobs.partition({
      job =>
        Iso8601Expressions.parse(job.schedule, job.scheduleTimeZone) match {
          case Some((repeat, schedule, _)) =>
            !job.disabled && repeat != 0 && schedule.isBefore(now)
          case _ =>
            false
        }
    })
  }

  //Begin Service interface
  override def startUp() {
    assert(!running.get, "This scheduler is already running!")
    log.info("Trying to become leader.")

    leaderLatch.addListener(new LeaderLatchListener {
      override def notLeader(): Unit = {
        leader.set(false)
        onDefeated()
      }

      override def isLeader(): Unit = {
        leader.set(true)
        onElected()
      }
    }, leaderExecutor)
    leaderLatch.start()
  }

  override def shutDown() {
    running.set(false)
    log.info("Shutting down job scheduler")

    leaderLatch.close(LeaderLatch.CloseMode.NOTIFY_LEADER)
    leaderExecutor.shutdown()
  }

  //Begin Leader interface, which is required for CandidateImpl.
  def onDefeated() {
    mesosDriver.close()

    log.info("Defeated. Not the current leader.")
    running.set(false)
    jobGraph.reset() // So we can rebuild it later.
    schedulerThreadFuture.get.cancel(true)
  }

  def onElected() {
    log.info("Elected as leader.")

    running.set(true)

    val jobScheduler = this
    //Consider making this a background thread or control via an executor.

    val f = localExecutor.submit(
      new Thread() {
        override def run() {
          log.info("Running background thread")
          jobScheduler.mainLoop()
        }
      })

    schedulerThreadFuture.set(f)
    log.info("Starting chronos driver")
    mesosDriver.start()
  }

  final def runJobs(jobs: List[ScheduleBasedJob]) {
    jobs.foreach {
      job =>
        log.info("Scheduling:" + job.name)
        taskManager.enqueue(TaskUtils.getTaskId(job, DateTime.now(DateTimeZone.UTC)), job.highPriority)
    }
  }

}
