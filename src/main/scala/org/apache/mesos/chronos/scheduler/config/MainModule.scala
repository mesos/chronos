package org.apache.mesos.chronos.scheduler.config

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.logging.{Level, Logger}
import javax.inject.Named

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import com.codahale.metrics.MetricRegistry
import com.google.common.util.concurrent.{ListeningScheduledExecutorService, MoreExecutors, ThreadFactoryBuilder}
import com.google.inject.{AbstractModule, Inject, Provides, Singleton}
import mesosphere.chaos.http.HttpConf
import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.mesos.Scheduler
import org.apache.mesos.chronos.notification.{HttpClient, JobNotificationObserver, MailClient, RavenClient, SlackClient, MattermostClient}
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs.stats.JobStats
import org.apache.mesos.chronos.scheduler.jobs.{JobMetrics, JobScheduler, JobsObserver, TaskManager}
import org.apache.mesos.chronos.scheduler.mesos._
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.joda.time.Seconds

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Guice glue code of application logic components.
 * @author Florian Leibert (flo@leibert.de)
 */
class MainModule(val config: SchedulerConfiguration with HttpConf)
    extends AbstractModule {

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

  @Singleton
  @Provides
  def provideMesosSchedulerDriverFactory(mesosScheduler: Scheduler, frameworkIdUtil: FrameworkIdUtil): MesosDriverFactory =
    new MesosDriverFactory(mesosScheduler, frameworkIdUtil, config)

  @Singleton
  @Provides
  def provideTaskScheduler(
                            taskManager: TaskManager,
                            dependencyScheduler: JobGraph,
                            persistenceStore: PersistenceStore,
                            mesosSchedulerDriver: MesosDriverFactory,
                            curator: CuratorFramework,
                            leaderLatch: LeaderLatch,
                            jobsObserver: JobsObserver.Observer,
                            metrics: JobMetrics): JobScheduler = {
    new JobScheduler(
      scheduleHorizon = Seconds.seconds(config.scheduleHorizonSeconds()).toPeriod,
      taskManager = taskManager,
      jobGraph = dependencyScheduler,
      persistenceStore = persistenceStore,
      mesosDriver = mesosSchedulerDriver,
      curator = curator,
      leaderLatch = leaderLatch,
      leaderPath = config.zooKeeperCandidatePath,
      jobsObserver = jobsObserver,
      failureRetryDelay = config.failureRetryDelayMs(),
      disableAfterFailures = config.disableAfterFailures(),
      jobMetrics = metrics)
  }

  @Singleton
  @Provides
  def provideJobsObservers(jobStats: JobStats, notificationClients: List[ActorRef]): JobsObserver.Observer = {
    val notifier = new JobNotificationObserver(notificationClients, config.clusterName.get)
    JobsObserver.composite(List(notifier.asObserver, jobStats.asObserver))
  }

  @Provides
  @Singleton
  def provideActorSystem(): ActorSystem = ActorSystem("chronos-actors")

  @Singleton
  @Provides
  def provideNotificationClients(system: ActorSystem): List[ActorRef] = {
    implicit val timeout = Timeout(36500 days)

    def create(clazz: Class[_], args: scala.Any*): ActorRef = {
      log.warning("Starting [%s] notification client.".format(clazz))
      system.actorOf(Props(clazz, args: _*))
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
      },
      for {
        webhookUrl <- config.slackWebhookUrl.get if !config.slackWebhookUrl.isEmpty
      } yield {
        create(classOf[SlackClient], webhookUrl)
      },
      for {
        endpointUrl <- config.httpNotificationUrl.get if !config.httpNotificationUrl.isEmpty
      } yield {
        create(classOf[HttpClient], endpointUrl, config.httpNotificationCredentials.get)
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

  @Named(MesosOfferReviverActor.NAME)
  @Provides
  @Singleton
  @Inject
  def provideOfferReviverActor(
                                system: ActorSystem,
                                conf: SchedulerConfiguration,
                                mesosDriverFactory: MesosDriverFactory,
                                registry: MetricRegistry): ActorRef =
  {
    val props = MesosOfferReviverActor.props(conf, mesosDriverFactory, registry)
    system.actorOf(props, MesosOfferReviverActor.NAME)
  }

  @Provides
  @Singleton
  @Inject
  def provideOfferReviver(
                           @Named(MesosOfferReviverActor.NAME) reviverRef: ActorRef,
                           registry: MetricRegistry): MesosOfferReviver = {
    new MesosOfferReviverDelegate(reviverRef, registry)
  }
}
