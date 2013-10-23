#!/bin/bash
set -o errexit -o nounset -o pipefail
function -h {
cat <<USAGE
 USAGE: ubuntu.bash

  Installs Chronos in one step.

  Installs all deb packages, downloads and builds Mesos and Chronos from
  Github and then installs a Chronos wrapper in $prefix/bin.

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
  if exists sw_vers
  then cmd=( os_x )
  fi
  "${cmd[@]}"
}

jdk_link=http://www.oracle.com/technetwork/java/javase/downloads/index.html
function os_x {(
  tmp
  exists brew || err "Please install Homebrew."
  exists g++  || err "Please install the Apple developer tools."
  exists gcc  || err "Please install the Apple developer tools."
  ( exists java && java -version 2>&1 | fgrep -q 1.7. ) ||
    err "Please install: JDK 1.7\n  $jdk_link"
  exists mvn  || err "Please install Maven (\`brew install maven' will work)."
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
  if [[ -d mesos ]]
  then msg "Already downloaded Mesos."
  else github_tgz apache/mesos "$mesos_ref" | tgz_into mesos
  fi
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
  if [[ -d chronos ]]
  then msg "Already downloaded Chronos."
  else github_tgz airbnb/chronos "$chronos_ref" | tgz_into chronos
  fi
}

function chronos_build {
  export MESOS_NATIVE_LIBRARY="$prefix"/lib/libmesos.so
  mvn package
  rsync -ai ./ "$prefix"/chronos
  chronos_runner
}

function chronos_runner {
cat > "$prefix"/bin/chronos <<EOF
export MESOS_NATIVE_LIBRARY='$prefix'/lib/libmesos.so
if [ "\$1" = "-h" ]
then
cat <<USAGE
 USAGE: chronos
        chronos /path/to/config
USAGE
else
  java -cp '$prefix'/chronos/target/chronos*.jar com.airbnb.scheduler.Main
fi
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
  local rand="$1"."$(printf '%04x%04x\n' $RANDOM $RANDOM)"
  mkdir -p "$rand"
  tar -xz -C "$rand" --strip-components 1 # Yes, this last option is portable.
  mv "$rand" "$1"
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
  msg "$1 $t0 start..."
  set +o errexit               ## Fancy error code capturing is portable
  ( set -o errexit             ## across old and new versions of Bash and
    "$@" 1>task.out 2>task.err ## works with calls to Bash functions as
    set +o errexit )           ## well as external commands.
  local code=$?                ####
  set -o errexit               ####
  if [[ $code -eq 0 ]]
  then
    msg "$1 $t0/$(date +%T) ...finish"
  else
    msg "==== tail -n20 $dir/task.out"
    tail -n20 "$dir"/task.out >&2
    echo
    msg "==== tail -n20 $dir/task.err"
    tail -n20 "$dir"/task.err >&2
    msg "$1 $t0/$(date +%T) failed in $dir"
    return $code
  fi
}

function exists {
  which "$1" &>/dev/null
}

function msg { out "$*" >&2 ;}
function err { local x=$? ; msg "$*" ; return $(( $x == 0 ? 1 : $x )) ;}
function out { printf '%s\n' "$*" ;}

if [[ ${1:-} ]] && declare -F | cut -d' ' -f3 | fgrep -qx -- "${1:-}"
then "$@"
else main "$@"
fi

