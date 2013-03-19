# Chronos Configuration Files

We have included sample configuration files for local Chronos development as well as running Chronos in production.

[`local_cluster_scheduler.yml`](#local_cluster_scheduleryml)
[`local_cluster_asset_dev.yml`](#local_cluster_asset_devyml)
[`local_scheduler.yml`](#local_scheduleryml)
[`local_scheduler_nozk.yml`](#local_scheduler_nozkyml)
[`sample_scheduler.yml`](#sample_scheduleryml)

## `local_cluster_scheduler.yml`
This configuration file assumes you have a mesos slave and master running locally. 

## `local_cluster_asset_dev.yml`
This configuration file is the same as the above ([`local_cluster_scheduler.yml`](#local_cluster_scheduleryml)), but also specifies asset overrides. Asset overrides should only be in your configuration file if you are modifying assets locally. If asset overrides are present in your production config, you will be unable to use the UI, as unpackaged assets are not included in the jar.

## `local_scheduler_nozk.yml`
Very basic configuration file, sufficient for testing Chronos locally. **Never** run such a configuration in production.

## `sample_scheduler.yml`
This configuration file specifies all relevant options in order to get Chronos running in production. Use this as a basis for your Chronos production configuration.
