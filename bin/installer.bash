#!/bin/bash
#TODO(FL): Add some color to the installer.
set -o errexit -o nounset -o pipefail

# Global vars
declare -r BIN_DIRECTORY="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
declare -r DEFAULT_MESOS_JAR_STRING="0.12.0-SNAPSHOT_JDK1.7"

mesos_installation="/tmp"

function install_mesos {
  echo "Do you have mesos installed already? Type 'yes' or 'no' followed by [ENTER]:"
  read installed_already

  case $installed_already in
      no)
        echo "Type the target directory (absolute path) where you would like mesos to be installed followed by [ENTER]:"
        read dest_dir
        echo "Trying to create ${dest_dir}"
        mkdir -p "$dest_dir"
        local esc_dest_dir="${dest_dir//\//\\/}"
        sed -i '' -e "s/service_dir=.*\$/service_dir=$esc_dest_dir/" "${BIN_DIRECTORY}/install_mesos.bash"
        bash "${BIN_DIRECTORY}/install_mesos.bash"
        echo "Installed mesos in: ${dest_dir}"
        mesos_installation="$dest_dir"
        ;;
      yes)
        echo "Type the path of the compiled mesos installation followed by [ENTER]:"
        read dest_dir
        mesos_installation="$dest_dir"
        echo "Skipping mesos installation"
        ;;
      *)
        echo "Error: Input not understood. Please answer with 'yes' or 'no'."
        install_mesos
        ;;
  esac
}

function install_chronos {

  pushd "$mesos_installation"
  local mesos_jar_file=$(find . -name "mesos-*.jar")
  local mesos_version="$(echo $mesos_jar_file | sed -e 's/^.*mesos-//g' | sed -e 's/\.jar//g')"
  local mesos_version_string="${mesos_version}-SNAPSHOT"
  if [[ -z "$mesos_version" ]] ; then
      echo "Could not determine mesos version. Try reinstalling mesos. Aborting."
      exit 1
  fi

  echo "Installing snapshot of mesos version $mesos_version into local mvn repository"
  mvn install:install-file -DgroupId=org.apache.mesos -DartifactId=mesos -Dversion="$mesos_version_string" -Dpackaging=jar  -Dfile="$mesos_jar_file"
  echo "Replacing pom.xml mesos dependency"
  sed -i '' -e "s/$DEFAULT_MESOS_JAR_STRING/$mesos_version_string/g" "$BIN_DIRECTORY/../pom.xml"
  popd
  pushd "$BIN_DIRECTORY" ; cd ..
  echo "Installing chronos"
  mvn package
  popd
  local esc_mesos_dir="${mesos_installation//\//\\/}"
  local project_dir="${BIN_DIRECTORY}/../"
  local esc_project_dir="${project_dir//\//\\/}"
  esc_project_dir="${esc_project_dir//./\.}"
  echo "Updating the start-chronos script in ${BIN_DIRECTORY} to point to your installation at ${mesos_installation}"
  sed -i '' -e "s/MESOS_HOME=.*\$/MESOS_HOME=$esc_mesos_dir/" "${BIN_DIRECTORY}/start-chronos.bash"
  sed -i '' -e "s/CHRONOS_HOME=.*\$/CHRONOS_HOME=$esc_project_dir/" "${BIN_DIRECTORY}/start-chronos.bash"
}

echo ; echo "Welcome to the interactive chronos installation. This script will first install mesos and then chronos." ; echo
echo "Depending on your hardware and internet connection, this script might take 15 - 25 minutes as we're compiling mesos from source."
echo "If you run into any issues, please check the FAQ in the chronos repo."

install_mesos
install_chronos
echo "Starting chronos..."
bash "${BIN_DIRECTORY}/start-chronos.bash"

