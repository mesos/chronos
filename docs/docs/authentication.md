---
title: Mesos Framework Authentication
---


# Mesos Framework Authentication

To enable framework authentication in Chronos:

* Run Chronos with `--mesos_authentication_principal` set to a Mesos-authorized principal. For Mesos' built-in CRAM-MD5 authentication, you must also provide `--mesos_authentication_secret_file` pointing to a file containing your authentication secret.

The secret file cannot have a trailing newline. To not add a newline simply run:

```bash
$ echo -n "secret" > /path/to/secret/file
```

* If using the built-in CRAM-MD5 authentication mechanism, run `mesos-master` with the credentials flag and the path to the file with authorized users and their secrets: `--credentials=/path/to/credential/file`

Note that this `--credentials` file is for all frameworks and agents registering with Mesos. In enterprise installations, the cluster admin will have already configured credentials in Mesos, so the user launching Chronos just needs to specify the principal+secret given to them by the cluster/security admin.

Each line in the file should be a principal and corresponding secret separated by a single space:

```bash
$ cat /path/to/credential/file
principal secret
principal2 secret2
```
