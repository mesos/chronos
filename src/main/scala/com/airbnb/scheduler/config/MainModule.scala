package com.airbnb.scheduler.config

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.logging.{Level, Logger}

import com.airbnb.scheduler.mesos.{MesosDriverFactory, MesosJobFramework}
import com.airbnb.scheduler.SchedulerHealthCheck
import com.airbnb.scheduler.jobs.{TaskManager, JobScheduler}
import com.airbnb.scheduler.graph.JobGraph
import com.airbnb.scheduler.state.PersistenceStore
import com.airbnb.notification.MailClient
import com.google.inject.{Provides, Singleton, Inject, AbstractModule}
import com.google.common.util.concurrent.{ListeningScheduledExecutorService, ThreadFactoryBuilder, MoreExecutors}
import com.twitter.common.zookeeper.Candidate
import org.apache.mesos.Protos.{FrameworkID, FrameworkInfo}
import org.apache.mesos.Scheduler
import org.joda.time.Seconds

/**
 * Guice glue code of application logic components.
 * @author Florian Leibert (flo@leibert.de)
 */
class MainModule(val config: SchedulerConfiguration) extends AbstractModule {
  private[this] val log = Logger.getLogger(getClass.getName)

  override def configure() {
    log.info("Wiring up the application")

    bind(classOf[Scheduler]).to(classOf[MesosJobFramework]).asEagerSingleton()
    bind(classOf[FrameworkInfo]).toInstance(
      //TODO(FL): Make the name configurable.
      FrameworkInfo.newBuilder().setName("chronos" + System.currentTimeMillis()).setId(FrameworkID.newBuilder()
        .setValue("chronos").build())
        .setFailoverTimeout(config.failoverTimeoutSeconds).setUser(config.user).build()
    )

    bind(classOf[SchedulerHealthCheck]).asEagerSingleton()
    bind(classOf[TaskManager]).asEagerSingleton()
    bind(classOf[SchedulerConfiguration]).toInstance(config)

    //TODO(FL): Only bind this if config.dependentJobs is turned on.
    bind(classOf[JobGraph]).asEagerSingleton()
  }

  @Singleton
  @Provides
  def provideMesosSchedulerDriverFactory(mesosScheduler: Scheduler, frameworkInfo: FrameworkInfo): MesosDriverFactory =
    new MesosDriverFactory(mesosScheduler, frameworkInfo, config)

  @Singleton
  @Provides
  def provideTaskScheduler(
      taskManager: TaskManager,
      dependencyScheduler: JobGraph,
      persistenceStore: PersistenceStore,
      mesosSchedulerDriver: MesosDriverFactory,
      candidate: Candidate,
      mailClient: Option[MailClient]): JobScheduler = {
    new JobScheduler(Seconds.seconds(config.scheduleHorizonSeconds).toPeriod, taskManager,
      dependencyScheduler, persistenceStore, mesosSchedulerDriver, candidate, mailClient,
      config.failureRetryDelay)
  }

  @Singleton
  @Provides
  def provideMailClient(): Option[MailClient] = {
    if (config.mailServer.isEmpty || config.mailFrom.isEmpty || !config.mailServer.get.contains(":")) {
      log.warning("No mailFrom or mailServer configured. Email Notfications are disabled!")
      None
    } else {
      val mailClient = new MailClient(config.mailServer.get, config.mailFrom.get, config.mailUser, config.mailPassword, config.mailSslOn)
      log.warning("Starting mail client.")
      mailClient.start()
      Some(mailClient)
    }
  }

  @Singleton
  @Provides
  def provideListeningExecutorService(): ListeningScheduledExecutorService = {
    val uncaughtExceptionHandler = new UncaughtExceptionHandler {
      def uncaughtException(thread: Thread, t: Throwable) {
        log.log(Level.SEVERE, "Error occurred in ListeningExecutorService, catching in thread", t)
      }
    }
    MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(5,
      new ThreadFactoryBuilder().setNameFormat("task_executor_thread-%d")
        .setUncaughtExceptionHandler(uncaughtExceptionHandler).build()))
  }
}
