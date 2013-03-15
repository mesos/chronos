# Chronos Web UI

Compiling assets for the web ui is simple and done on demand, in order to speed the main build process.

You *must* have built assets before packaging.

* [Compiling Assets](#compiling-assets)
* [Modifying Assets](#modifying-assets)
* [Assets and Order of Operations](#assets-and-order-of-operations)
* [Rebuild Assets by Default](#rebuild-assets-by-default)

## Compiling Assets

To compile assets simply run:
`mvn requirejs:optimize -P requirejs`

## Modifying Assets

To modify assets and have changes reflected on the fly, you will need to start Chronos with a specially configured YAML file. The configuration is quite simple and is documented at [dropwizard-configurable-assets-bundle](https://github.com/bazaarvoice/dropwizard-configurable-assets-bundle/blob/master/README.md).

There are also two included example YAML files that make use of this configuration:
* [config/local_scheduler.yml](/airbnb/chronos/blob/master/config/local_scheduler.yml#L4)
* [config/local_scheduler_nozk.yml](/airbnb/chronos/blob/master/config/local_scheduler_nozk.yml#L7)

## Assets and Order of Operations

If you wish to deploy, or otherwise run, a production build of Chronos with full Web UI, you must ensure that you have [built assets](#compiling-assets) before running `mvn package`.

## Rebuild Assets by Default

If you don't want to worry about which order you build Chronos in, you can modify `pom.xml` and switch the requirejs profile's activation from

`<activeByDefault>false</activeByDefault>`

to

`<activeByDefault>true</activeByDefault>`