---
title: Job Management
---

## Job Management

For larger installations, the web UI may be insufficient for managing jobs.  At
Airbnb, there are well over 700 production Chronos jobs.  Rather than using the
web UI for making edits, we created a script called `chronos-sync.rb` which can
be used to synchronize configuration from disk to Chronos.  For example, you
may have a Git repository that contains all of the Chronos job configurations,
and then you could run an hourly Chronos job that checks out the repository and
runs `chronos-sync.rb`.

You can initialize the configuration data by running:
```
$ bin/chronos-sync.rb -u http://chronos/ -p /path/to/jobs/config -c
```

After that, you can run the normal sync like this:

```
$ bin/chronos-sync.rb -u http://chronos/ -p /path/to/jobs/config
```

You can also forcefully update the configuration in Chronos from disk by
passing the `-f` or `--force` parameter.  In the example above,
`/path/to/jobs/config` is the path where you would like the configuration data
to live.

Note: `chronos-sync.rb` does not delete jobs by default. You can pass the `--delete-missing` flag to `chronos-sync.rb` to remove jobs. Alternatively, you can manually remove it using the API or web UI.
