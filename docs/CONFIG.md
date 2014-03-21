# Chronos configuration

Starting with Chronos 2.0, Chronos uses the [Chaos Framework](https://github.com/mesosphere/chaos), a minimal Jetty, Guice, Jersey & Jackson library, that also handles command line arguments - which in turn uses [Scallop](https://github.com/scallop/scallop).

To get a full list of options, use start-chronos.bash --help:

    [florian@Macintosh ~/mesosphere/chronos (master)]$ bin/start-chronos.bash --help
    Chronos home set to /Users/florian/mesosphere/chronos
    curl: (28) Connection timed out after 1000 milliseconds
    Using jar file: /Users/florian/mesosphere/chronos/target/chronos-2.0.1_mesos-0.14.0-rc4.jar[0]
    Nov 20, 2013 3:42:09 PM com.airbnb.scheduler.Main$delayedInit$body apply
    INFO: ---------------------
    Nov 20, 2013 3:42:09 PM com.airbnb.scheduler.Main$delayedInit$body apply
    INFO: Initializing chronos.
    Nov 20, 2013 3:42:09 PM com.airbnb.scheduler.Main$delayedInit$body apply
    INFO: ---------------------
          --default_job_owner  <arg>            Job Owner (default = flo@mesosphe.re)
      -d, --disable_after_failures  <arg>       Disables a job after this many
                                                failures have occurred (default = 0)
      -f, --failover_timeout  <arg>             The failover timeout in seconds for
                                                Mesos (default = 1200)
          --failure_retry  <arg>                Number of ms between retries
                                                (default = 60000)
          --ganglia_group_prefix  <arg>         Group prefix for Ganglia (default = )
      -g, --ganglia_host_port  <arg>            Host and port for Ganglia
          --ganglia_reporting_interval  <arg>   Ganglia reporting interval (seconds)
                                                (default = 60)
          --ganglia_spoof  <arg>                IP:host to spoof for Ganglia
                                                (default = actual hostname)
      -h, --hostname  <arg>                     The advertised hostname stored in
                                                ZooKeeper so another standby host can
                                                redirect to this elected leader
                                                (default = actual hostname)
          --http_credentials  <arg>             Credentials for accessing the http
                                                service.If empty, anyone can access
                                                the HTTP endpoint. A
                                                username:passwordis expected where the
                                                username must not contain ':'
          --http_port  <arg>                    The port to listen on for HTTP
                                                requests (default = 8080)
          --https_port  <arg>                   The port to listen on for HTTPS
                                                requests (default = 8443)
      -l, --leader_max_idle_time  <arg>         The look-ahead time for scheduling
                                                tasks in milliseconds (default = 5000)
          --log_config  <arg>                   The path to the log config
          --mail_from  <arg>                    Mail from field
          --mail_password  <arg>                Mail password (for auth)
      -m, --mail_server  <arg>                  Address of the mailserver
          --mail_ssl                            Mail SSL
          --mail_user  <arg>                    Mail user (for auth)
          --master  <arg>                       The URL of the Mesos master
                                                (default = local)
          --mesos_checkpoint                    Enable checkpointing in Mesos
          --mesos_framework_name  <arg>         The framework name
                                                (default = chronos-2.0.1)
          --mesos_role  <arg>                   The Mesos role to run tasks under
                                                (default = *)
          --mesos_task_cpu  <arg>               Number of CPUs to request from Mesos
                                                for each task (default = 0.1)
          --mesos_task_disk  <arg>              Amount of disk capacity to request
                                                from Mesos for each task (MB)
                                                (default = 256)
          --mesos_task_mem  <arg>               Amount of memory to request from Mesos
                                                for each task (MB) (default = 128)
      -s, --schedule_horizon  <arg>             The look-ahead time for scheduling
                                                tasks in seconds (default = 60)
          --ssl_keystore_password  <arg>        The password for the keystore
          --ssl_keystore_path  <arg>            Provides the keystore, if supplied,
                                                SSL is enabled
      -u, --user  <arg>                         The mesos user to run the processes
                                                under (default = root)
      -z, --zk_hosts  <arg>                     The list of ZooKeeper servers for
                                                storing state
                                                (default = localhost:2181)
          --zk_path  <arg>                      Path in ZooKeeper for storing state
                                                (default = /chronos/state)
          --zk_timeout  <arg>                   The timeout for ZooKeeper in
                                                milliseconds (default = 10000)
          --help                                Show help message
