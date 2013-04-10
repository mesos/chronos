#!/bin/bash
set -o errexit -o nounset -o pipefail
function -h {
cat <<USAGE
 USAGE: ...

  Installs Chronos in one step.

USAGE
}; function --help { -h ;}

prefix=/usr/local

function main {
  ubuntu
}

ubuntu_debs=( autoconf make gcc g++ cpp patch python-dev libtool
              default-jdk default-jdk-builddep default-jre gzip
              libghc-zlib-dev libcurl4-openssl-dev )
function ubuntu {(
  tmp
  task_wrapper debs "${ubuntu_debs[@]}"
  mesos
  chronos
)}

mesos_ref=0.12.x
function mesos {(
  tmp
  github_tgz apache/mesos "$mesos_ref" | tgz_into mesos
  cd mesos
  task_wrapper mesos_build
)}

function mesos_build {
  ./bootstrap
  ./configure --prefix="$prefix" \
    --with-webui --with-included-zookeeper --disable-perftools
  make || make # Always fails the first time.
  make install
}

chronos_ref=master
function chronos {(
  tmp
  github_tgz airbnb/chronos "$chronos_ref" | tgz_into chronos
  cd chronos
  task_wrapper chronos_build
)}

function chronos_build {
  export MESOS_NATIVE_LIBRARY="$prefix"/lib/libmesos.so
  mvn package
}

function debs {
  DEBIAN_FRONTEND=noninteractive apt-get install --yes "$@"
}

function github_tgz {
  local repo="$1" ; shift
  local ref="$1" ; shift
  curl -fL https://api.github.com/repos/"$repo"/tarball/"$ref" "$@"
}

function tgz_into {
  mkdir -p "$1"
  tar -xz -C "$1" --strip-components 1 # Yes, this is portable.
}

declare hasher
function hasher {
  if [[ ! ${hasher:+isset} ]]
  then
    hasher="$(which md5sum)" ||
    hasher="$(which md5)" ||
    err "No hashing program found!"
  fi
  "$hasher" <<<"$*" | cut -d' ' -f1
}

tmp_msg=false
tmp=/tmp/chronos-build."$(hasher "$prefix" "$chronos_ref" "$mesos_ref")" 
function tmp {
  if [[ -d "$tmp" ]] && ! $tmp_msg
  then
    msg "Recycling build in $tmp"
    tmp_msg=true
  fi
  mkdir -p "$tmp"
  cd "$tmp"
}

function task_wrapper {
  local dir="$(pwd -P)"
  local t0="$(date +%T)"
  msg "$1 $t0 Starting..."
  if "$@" 1>task.out 2>task.err
  then msg "$1 $t0/$(date +%T)"
  else
    local x=$?
    msg "==== tail -n20 $dir/task.out"
    tail -n20 "$dir"/task.out >&2
    msg "==== tail -n20 $dir/task.err"
    tail -n20 "$dir"/task.err >&2
    msg "$1 $t0/$(date +%T) failed in $dir."
    return $(( $x == 0 ? 1 : $x ))
  fi
}

function msg { out "$*" >&2 ;}
function err { local x=$? ; msg "$*" ; return $(( $x == 0 ? 1 : $x )) ;}
function out { printf '%s\n' "$*" ;}

if [[ ${1:-} ]] && declare -F | cut -d' ' -f3 | fgrep -qx -- "${1:-}"
then "$@"
else main "$@"
fi

