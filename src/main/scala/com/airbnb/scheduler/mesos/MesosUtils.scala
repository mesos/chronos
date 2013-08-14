package com.airbnb.scheduler.mesos

import java.util.logging.Logger

import com.airbnb.scheduler.jobs.BaseJob
import com.google.protobuf.ByteString
import com.google.common.base.Charsets
import org.apache.mesos.Protos._
import org.apache.mesos.Protos.Environment.Variable

/**
 * Helpers for dealing dealing with tasks such as generating taskIds based on jobs, parsing them and ensuring that their
 * names are valid.
 * @author Florian Leibert (flo@leibert.de)
 */
object MesosUtils {
  private[this] val log = Logger.getLogger(getClass.getName)
  val taskNameTemplate = "ChronosTask:%s"
  //args|command.
  //  e.g. args: -av (async job), verbose mode
  val executorArgsPattern = "%s|%s"

  def getMesosTaskInfoBuilder(taskIdStr: String, job: BaseJob): TaskInfo.Builder = {
    //TODO(FL): Allow adding more fine grained resource controls.
    val taskId = TaskID.newBuilder().setValue(taskIdStr).build()
    val taskInfo = TaskInfo.newBuilder()
      .setName(taskNameTemplate.format(job.name))
      .setTaskId(taskId)
    val environment = Environment.newBuilder()
      .addVariables(Variable.newBuilder()
        .setName("mesos_task_id").setValue(taskIdStr))
      .addVariables(Variable.newBuilder()
        .setName("CHRONOS_JOB_OWNER").setValue(job.owner))
    if (!job.executor.isEmpty) {
      appendExecutorData(taskInfo, job)
    } else {
      taskInfo.setCommand(
        if (job.command.startsWith("http") || job.command.startsWith("ftp")) {
          val uri1 = CommandInfo.URI.newBuilder().setValue(job.command).setExecutable(true).build()
          CommandInfo.newBuilder().addUris(uri1)
            .setValue("\"." + job.command.substring(job.command.lastIndexOf("/")) + "\"")
            .setEnvironment(environment)
        } else {
          CommandInfo.newBuilder().setValue(job.command).setEnvironment(environment)
        })
    }
    return taskInfo
  }

  def getExecutorName(x: String) = "%s".format(x)

  def getDataBytes(executorFlags: String, executorArgs: String) = {
    val dataStr = executorArgsPattern.format(executorFlags, executorArgs)
    ByteString.copyFrom(dataStr.getBytes(Charsets.UTF_8))
  }

  def appendExecutorData(taskInfo: TaskInfo.Builder, job: BaseJob) {
    log.info("Appending executor:" + job.executor + ", flags:" + job.executorFlags + ", command:" + job.command)
    taskInfo.setExecutor(
      ExecutorInfo.newBuilder()
        .setExecutorId(ExecutorID.newBuilder().setValue("shell-wrapper-executor"))
        .setCommand(CommandInfo.newBuilder().setValue(job.executor)))
      .setData(getDataBytes(job.executorFlags, job.command))
  }
}
