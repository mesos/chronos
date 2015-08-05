---
title: Configuration
---

# Configuration

Starting with Chronos 2.0, Chronos uses the [Chaos Framework](https://github.com/mesosphere/chaos), a minimal Jetty, Guice, Jersey & Jackson library, that also handles command line arguments - which in turn uses [Scallop](https://github.com/scallop/scallop).

To get a full list of options, use `start-chronos.bash --help`.

If you installed chronos via package, run `/usr/bin/chronos run_jar --help`.

## Chronos v2.3.4 Command Line Flags
```
    --assets_path  <arg>                        Set a local file system path to
                                                load assets from, instead of
                                                loading them from the packaged
                                                jar.
    --cassandra_consistency  <arg>              Consistency to use for Cassandra
                                                (default = ANY)
-c, --cassandra_contact_points  <arg>           Comma separated list of contact
                                                points for Cassandra
    --cassandra_keyspace  <arg>                 Keyspace to use for Cassandra
                                                (default = metrics)
    --cassandra_port  <arg>                     Port for Cassandra
                                                (default = 9042)
    --cassandra_stat_count_table  <arg>         Table to track stat counts in
                                                Cassandra
                                                (default = chronos_stat_count)
    --cassandra_table  <arg>                    Table to use for Cassandra
                                                (default = chronos)
    --cassandra_ttl  <arg>                      TTL for records written to
                                                Cassandra (default = 31536000)
    --cluster_name  <arg>                       The name of the cluster where
                                                Chronos is run
-d, --disable_after_failures  <arg>             Disables a job after this many
                                                failures have occurred
                                                (default = 0)
-f, --failover_timeout  <arg>                   The failover timeout in seconds
                                                for Mesos (default = 604800)
    --failure_retry  <arg>                      Number of ms between retries
                                                (default = 60000)
    --graphite_group_prefix  <arg>              Group prefix for Graphite
                                                (default = )
-g, --graphite_host_port  <arg>                 Host and port (in the form
                                                `host:port`) for Graphite
    --graphite_reporting_interval  <arg>        Graphite reporting interval
                                                (seconds) (default = 60)
-h, --hostname  <arg>                           The advertised hostname stored
                                                in ZooKeeper so another standby
                                                host can redirect to this
                                                elected leader
                                                (default = mesos01.ops.edge.iad.brigade.com)
    --http_address  <arg>                       The address to listen on for
                                                HTTP requests
    --http_credentials  <arg>                   Credentials for accessing the
                                                http service. If empty, anyone
                                                can access the HTTP endpoint. A
                                                username:password pair is
                                                expected where the username must
                                                not contain ':'. May also be
                                                specified with the
                                                `MESOSPHERE_HTTP_CREDENTIALS`
                                                environment variable.
    --http_port  <arg>                          The port to listen on for HTTP
                                                requests (default = 8080)
    --http_realm  <arg>                         The security realm (aka 'area')
                                                associated with the credentials
                                                (default = Mesosphere)
    --https_port  <arg>                         The port to listen on for HTTPS
                                                requests (default = 8443)
-j, --job_history_limit  <arg>                  Number of past job executions to
                                                show in history view
                                                (default = 5)
-l, --leader_max_idle_time  <arg>               The look-ahead time for
                                                scheduling tasks in milliseconds
                                                (default = 5000)
    --mail_from  <arg>                          Mail from field
    --mail_password  <arg>                      Mail password (for auth)
-m, --mail_server  <arg>                        Address of the mailserver in
                                                server:port format
    --mail_ssl                                  Mail SSL
    --mail_user  <arg>                          Mail user (for auth)
    --master  <arg>                             The URL of the Mesos master
                                                (default = local)
    --mesos_authentication_principal  <arg>     Mesos Authentication Principal
    --mesos_authentication_secret_file  <arg>   Mesos Authentication Secret
    --mesos_checkpoint                          Enable checkpointing in Mesos
    --mesos_framework_name  <arg>               The framework name
                                                (default = chronos-2.3.4)
    --mesos_role  <arg>                         The Mesos role to run tasks
                                                under (default = *)
    --mesos_task_cpu  <arg>                     Number of CPUs to request from
                                                Mesos for each task
                                                (default = 0.1)
    --mesos_task_disk  <arg>                    Amount of disk capacity to
                                                request from Mesos for each task
                                                (MB) (default = 256.0)
    --mesos_task_mem  <arg>                     Amount of memory to request from
                                                Mesos for each task (MB)
                                                (default = 128.0)
-r, --raven_dsn  <arg>                          Raven DSN for connecting to a
                                                raven or sentry event service
    --reconciliation_interval  <arg>            Reconciliation interval in
                                                seconds (default = 600)
-s, --schedule_horizon  <arg>                   The look-ahead time for
                                                scheduling tasks in seconds
                                                (default = 60)
    --slack_url  <arg>                          Webhook URL for posting to Slack
    --ssl_keystore_password  <arg>              Password for the keystore
                                                supplied with the
                                                `ssl_keystore_path` option.
                                                Required if `ssl_keystore_path`
                                                is supplied. May also be
                                                specified with the
                                                `MESOSPHERE_KEYSTORE_PASS`
                                                environment variable.
    --ssl_keystore_path  <arg>                  Path to the SSL keystore. HTTPS
                                                (SSL) will be enabled if this
                                                option is supplied. Requires
                                                `--ssl_keystore_password`. May
                                                also be specified with the
                                                `MESOSPHERE_KEYSTORE_PATH`
                                                environment variable.
-t, --task_epsilon  <arg>                       The default epsilon value for
                                                tasks, in seconds (default = 60)
-u, --user  <arg>                               The chronos user to run the
                                                processes under (default = root)
    --webui_url  <arg>                          The http(s) url of the web ui,
                                                defaulting to the advertised
                                                hostname
    --zk_hosts  <arg>                           The list of ZooKeeper servers
                                                for storing state
                                                (default = localhost:2181)
-z, --zk_path  <arg>                            Path in ZooKeeper for storing
                                                state (default = /chronos/state)
    --zk_timeout  <arg>                         The timeout for ZooKeeper in
                                                milliseconds (default = 10000)
    --help                                      Show help message
```
