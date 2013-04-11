#!/bin/bash
set -o errexit -o nounset -o pipefail
function -h {
cat <<USAGE
 USAGE: one-shot.bash
        one-shot.bash os_x|ubuntu|...

  Installs Chronos in one step. With no arguments, auto detects your OS and
  performs appropriate installation.

USAGE
}; function --help { -h ;}

prefix=/usr/local

function main {
  local cmd=()
  if [[ -f /etc/issue ]]
  then
    case "$(cut -d' ' -f1 < /etc/issue)" in
      Ubuntu) cmd=( ubuntu ) ;;
      *)      err "Not able to ID your system from /etc/issue." ;;
    esac
  fi
  if which sw_vers &>/dev/null
  then cmd=( os_x )
  fi
  "${cmd[@]}"
}

function os_x {(
  tmp
  # Check for g++, autoconf, Java, Homebrew. Error if not found.
  # Install Maven.
  mesos
  chronos
)}

ubuntu_debs=( autoconf make gcc g++ cpp patch python-dev libtool
              default-jdk default-jdk-builddep default-jre maven 
              gzip libghc-zlib-dev libcurl4-openssl-dev )
function ubuntu {(
  tmp
  task_wrapper debs "${ubuntu_debs[@]}"
  mesos
  chronos
)}

mesos_ref=0.12.x
function mesos {(
  tmp
  task_wrapper mesos_download
  cd mesos
  task_wrapper mesos_build
)}

function mesos_download {
  github_tgz apache/mesos "$mesos_ref" | tgz_into mesos
}

function mesos_build {
  [[ -f ./configure ]] || ./bootstrap
  ./configure --prefix="$prefix" \
    --with-webui --with-included-zookeeper --disable-perftools
  make
  make install
}

chronos_ref=master
function chronos {(
  tmp
  task_wrapper chronos_download
  cd chronos
  task_wrapper chronos_build
)}

function chronos_download {
  github_tgz airbnb/chronos "$chronos_ref" | tgz_into chronos
}

function chronos_build {
  export MESOS_NATIVE_LIBRARY="$prefix"/lib/libmesos.so
  mvn package
  rsync -ai ./ "$prefix"/chronos
  chronos_runner
}

function chronos_runner {
cat > /usr/local/bin/chronos <<EOF
export MESOS_NATIVE_LIBRARY="$prefix"/lib/libmesos.so
java -cp "$prefix"/chronos/target/chronos*.jar com.airbnb.scheduler.Main \
     server "$prefix"/chronos/config/local_scheduler_nozk.yml
EOF
chmod a+rx "$prefix"/bin/chronos
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
  tar -xz -C "$1" -k --strip-components 1 # Yes, this is portable.
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
  $tmp_msg || msg "Building in $tmp"
  tmp_msg=true
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

