package com.airbnb.scheduler.config

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.logging.{Level, Logger}

import com.airbnb.scheduler.mesos.{MesosTaskBuilder, MesosDriverFactory, MesosJobFramework}
import com.airbnb.scheduler.jobs.{JobStats, JobMetrics, TaskManager, JobScheduler}
import com.airbnb.scheduler.graph.JobGraph
import com.airbnb.scheduler.state.PersistenceStore
import com.airbnb.notification.{MailClient,RavenClient}
import com.google.inject.{Inject, Provides, Singleton, AbstractModule}
import com.google.common.util.concurrent.{ListeningScheduledExecutorService, ThreadFactoryBuilder, MoreExecutors}
import org.apache.mesos.Protos.FrameworkInfo
import org.apache.mesos.Scheduler
import org.joda.time.Seconds
import mesosphere.mesos.util.FrameworkIdUtil
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import scala.concurrent.duration._
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.framework.CuratorFramework

/**
 * Guice glue code of application logic components.
 * @author Florian Leibert (flo@leibert.de)
 */
class MainModule(val config: SchedulerConfiguration) extends AbstractModule {
  private[this] val log = Logger.getLogger(getClass.getName)

  override def configure() {
    log.info("Wiring up the application")

    bind(classOf[SchedulerConfiguration]).toInstance(config)
    bind(classOf[Scheduler]).to(classOf[MesosJobFramework]).asEagerSingleton()
    bind(classOf[TaskManager]).asEagerSingleton()
    bind(classOf[MesosTaskBuilder]).asEagerSingleton()

    //TODO(FL): Only bind this if config.dependentJobs is turned on.
    bind(classOf[JobGraph]).asEagerSingleton()
  }

  @Inject
  @Singleton
  @Provides
  def provideFrameworkInfo(frameworkIdUtil: FrameworkIdUtil): FrameworkInfo = {
    val frameworkInfo = FrameworkInfo.newBuilder()
      .setName(config.mesosFrameworkName())
      .setCheckpoint(config.mesosCheckpoint())
      .setRole(config.mesosRole())
      .setFailoverTimeout(config.failoverTimeoutSeconds())
      .setUser(config.user())
    frameworkIdUtil.setIdIfExists(frameworkInfo)
    frameworkInfo.build()
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
                            curator: CuratorFramework,
                            leaderLatch: LeaderLatch,
                            notificationClients: List[ActorRef],
                            metrics: JobMetrics,
                            stats: JobStats): JobScheduler = {
    new JobScheduler(
      scheduleHorizon = Seconds.seconds(config.scheduleHorizonSeconds()).toPeriod,
      taskManager = taskManager,
      jobGraph = dependencyScheduler,
      persistenceStore = persistenceStore,
      mesosDriver = mesosSchedulerDriver,
      curator = curator,
      leaderLatch = leaderLatch,
      leaderPath = config.zooKeeperCandidatePath,
      notificationClients = notificationClients,
      failureRetryDelay = config.failureRetryDelayMs(),
      disableAfterFailures = config.disableAfterFailures(),
      jobMetrics = metrics,
      jobStats = stats,
      clusterName = config.clusterName()
      )
  }

  @Singleton
  @Provides
  def provideNotificationClients(): List[ActorRef] = {
    implicit val system = ActorSystem("chronos-actors")
    implicit val timeout = Timeout(36500 days)

    def create(clazz: Class[_], args: scala.Any*) : ActorRef = {
      log.warning("Starting [%s] notification client.".format(clazz))
      system.actorOf(Props(clazz, args:_*))
    }

    List(
      for {
        server <- config.mailServer.get if !server.isEmpty && server.contains(":")
        from <- config.mailFrom.get if !from.isEmpty
      } yield {
        create(classOf[MailClient], server, from, config.mailUser.get, config.mailPassword.get, config.mailSslOn())
      },
      for {
        ravenDsn <- config.ravenDsn.get if !ravenDsn.isEmpty
      } yield {
        create(classOf[RavenClient], ravenDsn)
      }
    ).flatten
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
