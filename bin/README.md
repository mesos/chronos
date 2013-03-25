# scripts

Various support scripts.

## `install_mesos.bash`

Installs mesos.

### Currently supported environments:
* Mac OSX

_Feel free to pull request support for additional environments!_

## `installer.bash`

Installs mesos & chronos then boots a local chronos. Uses `installer_mesos.bash`.

### Currently supported environments:
* Mac OSX

_Feel free to pull request support for additional environments!_

## `run-emr.bash`

Specifically setup to ship a script (e.g. BASH script encapsulating a pig or hive script) to a remote machine, and run it in the background. Currently setup to handle [sssp](https://github.com/airbnb/sssp) (S3 Proxy) urls, HTTP and S3 urls. For the latter
to work, one has to obviously install s3cmd and set the appropriate variables.

## `url-runner`

Allows running an arbitrary url via chronos and ensuring we don't always have to download it if it exists already.
This is intended as a wrapper for arbitrary shell scripts that may or may not be deployed to the mesos-slaves yet but is
obtainable via supported URL scheme.

## `start-chronos.bash`

Starts chronos making some assumptions such as that the `installer.bash` script was previously run since it will do some in-place edits inside inside `start-chronos.bash` to ensure the mesos variables are properly set and point to the correct locations.

## `run`

Run script intended to be used with [runit](http://smarden.org/runit/). This reflects the production setup of airbnb and is specifically setup to work on ec2.
