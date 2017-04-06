---
title: Configuration
---

# Configuration

Starting with Chronos v2.0, Chronos uses the [Chaos Framework](https://github.com/mesosphere/chaos), a minimal Jetty, Guice, Jersey & Jackson library, that also handles command line arguments - which in turn uses [Scallop](https://github.com/scallop/scallop).

To get a full list of options, use ` --help`.

If you installed chronos via package, run `/usr/bin/chronos run_jar --help`.

## Chronos v3.0.0 Command Line Flags
```
--cassandra_consistency  <arg>              Consistency to use for Cassandra
-c, --cassandra_contact_points  <arg>           Comma separated list of contact
                                            points for Cassandra
--cassandra_keyspace  <arg>                 Keyspace to use for Cassandra
--cassandra_port  <arg>                     Port for Cassandra
--cassandra_user  <arg>                     User for Cassandra
                                            (default = None)
--cassandra_password  <arg>                 Password for Cassandra
                                            (default = None)
--cassandra_stat_count_table  <arg>         Table to track stat counts in
                                            Cassandra
--cassandra_table  <arg>                    Table to use for Cassandra
--cassandra_ttl  <arg>                      TTL for records written to
                                            Cassandra
--cluster_name  <arg>                       The name of the cluster where
                                            Chronos is run
--decline_offer_duration  <arg>             (Default: Use mesos default of 5
                                            seconds) The duration
                                            (milliseconds) for which to
                                            decline offers by default
-d, --disable_after_failures  <arg>             Disables a job after this many
                                            failures have occurred
--disable_http                              Disable listening for HTTP
                                            requests completely. HTTPS is
                                            unaffected.
-f, --failover_timeout  <arg>                   The failover timeout in seconds
                                            for Mesos
--failure_retry  <arg>                      Number of ms between retries
--graphite_group_prefix  <arg>              Group prefix for Graphite
-g, --graphite_host_port  <arg>                 Host and port (in the form
                                            `host:port`) for Graphite
--graphite_reporting_interval  <arg>        Graphite reporting interval
                                            (seconds)
--hostname  <arg>                           The advertised hostname of this
                                            Chronos instance for network
                                            communication. This is used by
                                            otherChronos instances and the
                                            Mesos master to communicate with
                                            this instance
--notification_level  <arg>                 The notification level to use
                                            (DISABLED | FAILURES | ALL)
--http_address  <arg>                       The address to listen on for
                                            HTTP requests
--http_compression                          (Default) Enable http
                                            compression.
--disable_http_compression                  Disable http compression.
--http_credentials  <arg>                   Credentials for accessing the
                                            http service. If empty, anyone
                                            can access the HTTP endpoint. A
                                            username:password pair is
                                            expected where the username must
                                            not contain ':'. May also be
                                            specified with the
                                            `MESOSPHERE_HTTP_CREDENTIALS`
                                            environment variable.
--http_notification_credentials  <arg>      Http notification URL
                                            credentials in format
                                            username:password
-h, --http_notification_url  <arg>              Http URL for notifying failures
--http_port  <arg>                          The port to listen on for HTTP
                                            requests
--http_realm  <arg>                         The security realm (aka 'area')
                                            associated with the credentials
--https_address  <arg>                      The address to listen on for
                                            HTTPS requests.
--https_port  <arg>                         The port to listen on for HTTPS
                                            requests
-j, --job_history_limit  <arg>                  Number of past job executions to
                                            show in history view
-l, --leader_max_idle_time  <arg>               The look-ahead time for
                                            scheduling tasks in milliseconds
--mail_from  <arg>                          Mail from field
--mail_password  <arg>                      Mail password (for auth)
-m, --mail_server  <arg>                        Address of the mailserver in
                                            server:port format
--mail_ssl                                  Mail SSL
--mail_user  <arg>                          Mail user (for auth)
--master  <arg>                             The URL of the Mesos master
--mattermost_url  <arg>                     Webhook URL for posting to
                                            Mattermost
--mesos_authentication_principal  <arg>     Mesos Authentication Principal
--mesos_authentication_secret_file  <arg>   Mesos Authentication Secret
--mesos_checkpoint                          Enable checkpointing in Mesos
--mesos_framework_name  <arg>               The framework name
--mesos_role  <arg>                         The Mesos role to run tasks
                                            under
--mesos_task_cpu  <arg>                     Number of CPUs to request from
                                            Mesos for each task
--mesos_task_disk  <arg>                    Amount of disk capacity to
                                            request from Mesos for each task
                                            (MB)
--mesos_task_mem  <arg>                     Amount of memory to request from
                                            Mesos for each task (MB)
--min_revive_offers_interval  <arg>         Do not ask for all offers (also
                                            already seen ones) more often
                                            than this interval (ms).
                                            (Default: 5000)
-r, --raven_dsn  <arg>                          Raven DSN for connecting to a
                                            raven or sentry event service
--reconciliation_interval  <arg>            Reconciliation interval in
                                            seconds
--revive_offers_for_new_jobs                Whether to call reviveOffers for
                                            new or changed jobs. (Default:
                                            do not use reviveOffers)
-s, --slack_url  <arg>                          Webhook URL for posting to Slack
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
                                            tasks, in seconds
-u, --user  <arg>                               The chronos user to run the
                                            processes under
--webui_url  <arg>                          The http(s) url of the web ui,
                                            defaulting to the advertised
                                            hostname
--zk_auth  <arg>                            Authorization string for
                                            ZooKeeper
-z, --zk_hosts  <arg>                           The list of ZooKeeper servers
                                            for storing state
--zk_path  <arg>                            Path in ZooKeeper for storing
                                            state
--zk_timeout  <arg>                         The timeout for ZooKeeper in
                                            milliseconds
--help                                      Show help message
```
