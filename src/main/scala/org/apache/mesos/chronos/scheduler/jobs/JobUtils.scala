package org.apache.mesos.chronos.scheduler.jobs

import java.util.logging.Logger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.common.base.Charsets
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.apache.mesos.chronos.utils.{JobDeserializer, JobSerializer}
import org.joda.time.{Period, DateTime}
import org.joda.time.format.DateTimeFormat

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * @author Florian Leibert (flo@leibert.de)
 */
object JobUtils {

  val jobNamePattern = """([\w\s\.#_-]+)""".r
  val stats = new mutable.HashMap[String, DescriptiveStatistics]()
  val maxValues = 100
  //The object mapper, which is, according to the docs, Threadsafe once configured.
  val objectMapper = new ObjectMapper
  val mod = new SimpleModule("JobModule")
  private[this] val log = Logger.getLogger(getClass.getName)

  mod.addSerializer(classOf[BaseJob], new JobSerializer)
  mod.addDeserializer(classOf[BaseJob], new JobDeserializer)
  objectMapper.registerModule(mod)

  def toBytes[T <: BaseJob](job: T): Array[Byte] =
    objectMapper.writeValueAsString(job).getBytes(Charsets.UTF_8)


  def fromBytes(data: Array[Byte]): BaseJob = {
    //TODO(FL): Fix this, as it is very inefficient since we're parsing twice.
    //          Link to article, doing this by creating a deserializer handling polymorphism nicer (but more code):
    //          http://programmerbruce.blogspot.com.es/2011/05/deserialize-json-with-jackson-into.html
    val strData = new String(data, Charsets.UTF_8)

    objectMapper.readValue(strData, classOf[BaseJob])
  }
  
  def convertJobToStored(job: BaseJob): Option[StoredJob] = job match {
    case j: ScheduleBasedJob =>
      Schedule.parse(j.schedule, j.scheduleTimeZone) map { parsedSched =>
        InternalScheduleBasedJob(
          parsedSched,
          name = j.name,
          command = j.command,
          epsilon = j.epsilon,
          successCount = j.successCount,
          errorCount = j.errorCount,
          executor = j.executor,
          executorFlags = j.executorFlags,
          retries = j.retries,
          owner = j.owner,
          ownerName = j.ownerName,
          description = j.description,
          lastError = j.lastError,
          lastSuccess = j.lastSuccess,
          async = j.async,
          cpus = j.cpus,
          disk = j.disk,
          mem = j.mem,
          disabled = j.disabled,
          errorsSinceLastSuccess = j.errorsSinceLastSuccess,
          uris = j.uris,
          highPriority = j.highPriority,
          runAsUser = j.runAsUser,
          container = j.container,
          environmentVariables = j.environmentVariables,
          shell = j.shell,
          arguments = j.arguments,
          softError = j.softError,
          dataProcessingJobType = j.dataProcessingJobType,
          constraints = j.constraints
        )
      }
    case otherwise: StoredJob => Some(otherwise)
  }

  def convertStoredToJob(job: StoredJob): BaseJob = job match {
    case is: InternalScheduleBasedJob => convertInternalScheduleToExternalScheduled(is)
    case other => other
  }

  def convertInternalScheduleToExternalScheduled(j: InternalScheduleBasedJob): ScheduleBasedJob = {
    ScheduleBasedJob(
      schedule = j.scheduleData.toZeroOffsetISO8601Representation,
      name = j.name,
      command = j.command,
      epsilon = j.epsilon,
      successCount = j.successCount,
      errorCount = j.errorCount,
      executor = j.executor,
      executorFlags = j.executorFlags,
      retries = j.retries,
      owner = j.owner,
      ownerName = j.ownerName,
      description = j.description,
      lastError = j.lastError,
      lastSuccess = j.lastSuccess,
      async = j.async,
      cpus = j.cpus,
      disk = j.disk,
      mem = j.mem,
      disabled = j.disabled,
      errorsSinceLastSuccess = j.errorsSinceLastSuccess,
      uris = j.uris,
      highPriority = j.highPriority,
      runAsUser = j.runAsUser,
      container = j.container,
      environmentVariables = j.environmentVariables,
      shell = j.shell,
      arguments = j.arguments,
      softError = j.softError,
      dataProcessingJobType = j.dataProcessingJobType,
      constraints = j.constraints
    )
  }

  def isValidJobName(jobName: String): Boolean = {
    jobName match {
      case jobNamePattern(part) => true
      case _ => false
    }
  }

  //TODO(FL): Think about moving this back into the JobScheduler, though it might be a bit crowded.
  def loadJobs(scheduler: JobScheduler, store: PersistenceStore) {
    //TODO(FL): Create functions that map strings to jobs

    val jobs = store.getJobs
    val dependencyBasedJobs = jobs.collect({case d: DependencyBasedJob => d }).toList
    val scheduledJobs= jobs.collect({ case s: InternalScheduleBasedJob => s }).toList


    log.info("Registering jobs:" + scheduledJobs.size)
    scheduler.registerJob(scheduledJobs.toList)

    //We cannot simply register
    dependencyBasedJobs.foreach({ x =>
      log.info("Adding vertex in the vertex map:" + x.name)
      scheduler.jobGraph.addVertex(x)
    })

    dependencyBasedJobs.foreach {
      x =>
        log.info("mapping:" + x)
        log.info("Adding dependencies for %s -> [%s]".format(x.name, x.parents.mkString(",")))

        scheduler.jobGraph.parentJobsOption(x) match {
          case None =>
            log.warning(s"Couldn't find all parents of job ${x.name}... dropping it.")
            scheduler.jobGraph.removeVertex(x)
          case Some(parentJobs) =>
            parentJobs.foreach {
              //Setup all the dependencies
              y: BaseJob =>
                scheduler.jobGraph.addDependency(y.name, x.name)
            }
        }
    }
  }

  def makeScheduleStream(job: InternalScheduleBasedJob, dateTime: DateTime): Option[ScheduleStream] = {
    val scheduleStream = new ScheduleStream(job.name, job.scheduleData)
    skipForward(scheduleStream, dateTime, job.epsilon)
  }

  def skipForward(originalStream: ScheduleStream, now: DateTime, epsilon: Period, skippedAlready: Int = 0): Option[ScheduleStream] = {

    @tailrec
    def skip(scheduleStream: Option[ScheduleStream], now: DateTime, skippedAlready: Int): Option[ScheduleStream] = {
     scheduleStream match {
        case None =>
          log.warning("Filtered job %s as it is no longer valid.".format(originalStream.jobName))
          None
        case Some(stream) =>
          if (!stream.schedule.invocationTime.plus(epsilon).isBefore(now)) {
            if(skippedAlready != 0) {
              log.warning("Skipped job %s forward %d iterations, modified start from '%s' to '%s"
                .format(
                  originalStream.jobName,
                  skippedAlready,
                  originalStream.schedule.invocationTime.toString(DateTimeFormat.fullDate),
                  stream.schedule.invocationTime.toString(DateTimeFormat.fullDate)))
            }
            Some(stream)
          } else {
            skip(stream.tail, now, skippedAlready + 1)
          }
      }
    }
    println("Checking stream ....")
    skip(Some(originalStream), now, 0)
  }

  def getJobWithArguments(job: StoredJob, arguments: String): BaseJob = {
    val commandWithArgs = job.command + " " + arguments
    val jobWithArguments = job match {
      case j: DependencyBasedJob =>
        new DependencyBasedJob(
          parents = Set(),
          name = job.name,
          command = commandWithArgs,
          epsilon = job.epsilon,
          successCount = job.successCount,
          errorCount = job.errorCount,
          executor = job.executor,
          executorFlags = job.executorFlags,
          retries = job.retries,
          owner = job.owner,
          lastError = job.lastError,
          lastSuccess = job.lastSuccess,
          async = job.async,
          cpus = job.cpus,
          disk = job.disk,
          mem = job.mem,
          disabled = job.disabled,
          errorsSinceLastSuccess = job.errorsSinceLastSuccess,
          softError = job.softError,
          uris = job.uris,
          highPriority = job.highPriority,
          runAsUser = job.runAsUser,
          container = job.container,
          environmentVariables = job.environmentVariables,
          shell = job.shell,
          arguments = job.arguments
        )
      case j: InternalScheduleBasedJob =>
        new InternalScheduleBasedJob(
          scheduleData = j.scheduleData,
          scheduleTimeZone = j.scheduleTimeZone,
          name = job.name,
          command = commandWithArgs,
          epsilon = job.epsilon,
          successCount = job.successCount,
          errorCount = job.errorCount,
          executor = job.executor,
          executorFlags = job.executorFlags,
          retries = job.retries,
          owner = job.owner,
          lastError = job.lastError,
          lastSuccess = job.lastSuccess,
          async = job.async,
          cpus = job.cpus,
          disk = job.disk,
          mem = job.mem,
          disabled = job.disabled,
          errorsSinceLastSuccess = job.errorsSinceLastSuccess,
          softError = job.softError,
          uris = job.uris,
          highPriority = job.highPriority,
          runAsUser = job.runAsUser,
          container = job.container,
          environmentVariables = job.environmentVariables,
          shell = job.shell,
          arguments = job.arguments
        )
    }
    jobWithArguments
  }
}
