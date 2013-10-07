#!/bin/bash
set -o errexit -o nounset -o pipefail

# Set this to your base location.
declare -r service_dir=/usr/local/mesos
declare -r source_dir="$service_dir/src"
declare -r build_dir="$source_dir/build"
declare -r install_dir="$service_dir/install"
declare -r branch_name="snapshot"

# This is the git revision of ~ 0.12.0 with the state abstraction which we use.
declare -r git_revision=3758114ee4492dcbb784d5aac65d43ac54ddb439

function all {
  echo "Updating mesos source..."
  if [ -d "$source_dir" ]; then
    pushd $source_dir
    local revision=`git rev-parse HEAD`
    git reset --hard
    if [ $revision != `git rev-parse HEAD` ]; then
      build
    fi
    popd
  else
    git clone https://github.com/apache/mesos.git $source_dir
    (cd $source_dir
      git checkout $git_revision -b $branch_name
    )
    build
  fi
}

function build {
  echo "Building mesos source..."
  mkdir $build_dir
  pushd $source_dir
  ./bootstrap
  pushd $build_dir
  "$source_dir/configure" --prefix="$install_dir" --with-curl --disable-perftools
  make -j 16 check TESTS=
  ./bin/mesos-tests.sh --gtest_filter=-Cgroups*:FsTest*:Proc*
  make install
  popd
  popd
}

echo "Installing mesos as user \"$(whoami)\""

mkdir -p "$service_dir"
all
