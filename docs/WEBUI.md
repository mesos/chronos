# Chronos Web UI

* [Compiling Assets](#compiling-assets)
* [Modifying Assets](#modifying-assets)
* [Build Requirements](#build-requirements)

## Compiling Assets

**Node.js is required to build assets**

Assets are automatically compiled when running `mvn package`. If you change assets, and want them updated in your jar, you must either `rm -rf src/main/resources/assets/build` or `mvn clean`.

## Modifying Assets

To modify assets and have changes reflected on the fly, you will need to start Chronos with a specially configured YAML file. The configuration is quite simple and is documented at [dropwizard-configurable-assets-bundle](https://github.com/bazaarvoice/dropwizard-configurable-assets-bundle/blob/master/README.md).

There are also two included example YAML files that make use of this configuration:

* [config/local_scheduler.yml](/airbnb/chronos/blob/master/config/local_scheduler.yml#L4)
* [config/local_scheduler_nozk.yml](/airbnb/chronos/blob/master/config/local_scheduler_nozk.yml#L7)

## Build Requirements

Building and optimizing assets seems to require more than 1GB of RAM on
the machine, according to [Issue #42](/airbnb/chronos/issues/42).
