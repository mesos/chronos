---
title: Contributor Guidelines
---


# Contributor Guidelines

## Submitting Changes to Chronos

- A GitHub pull request is the preferred way of submitting patch sets.

- Any changes in the public API or behavior must be reflected in the project
  documentation.

- Pull requests should include appropriate additions to the unit test suite.

- If the change is a bugfix, then the added tests must fail without the patch
  as a safeguard against future regressions.

- Changes should not result in a drop in code coverage.  Coverage may be
  checked by running `mvn scoverage:check`

## Contributing Documentation

We heartily welcome contributions to Chronos's documentation. Documentation should be submitted as a pull request againstthe `master` branch and published to our GitHub pages site by a Chronos committer using the instructions in [docs/README.md](https://github.com/mesos/chronos/tree/master/docs).

For development, you can use [dcos-vagrant](https://github.com/dcos/dcos-vagrant) to test Chronos. On macOS, you can install libmesos with `brew install mesos`, and start Chronos against a DC/OS vagrant installation like so:

    $ java -Djava.library.path=/usr/local/lib -jar target/chronos-3.0.0-SNAPSHOT.jar --zk_hosts 192.168.65.90:2181 --master zk://192.168.65.90:2181/mesos
