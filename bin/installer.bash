#!/bin/bash
#TODO(FL): Add some color to the installer.
set -o errexit -o nounset -o pipefail

# Global vars
declare -r BIN_DIRECTORY="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

detected_os="$(uname)"
case $detected_os in
  Linux)
    sed_in_place() {
      sed -i $@
    }
  ;;
  *)
    sed_in_place() {
      sed -i '' $@
    }
  ;;
esac

mesos_installation="/tmp"

# Wait for a user to type in a directory, then evaluate it to expand
# environment variables, like $HOME, and ~.
function read_dir {
  read dest_dir
  eval echo $dest_dir
}

function install_mesos {
  echo "Do you have mesos installed already? Type 'yes' or 'no' followed by [ENTER]:"
  read installed_already

  case $installed_already in
      no)
        echo "Type the target directory (absolute path) where you would like mesos to be installed followed by [ENTER]:"
        dest_dir=$(read_dir)
        if [[ -d "$dest_dir/src" ]]; then
          echo "A Mesos install already exists in this directory, would you like to delete it? Type 'yes' or 'no' followed by [ENTER]"
          read delete_dest_dir
          case $delete_dest_dir in
            no)
              echo "Error: Mesos already exists in this directory. Aborting."
              exit 1
              ;;
            yes)
              echo "Removing $dest_dir/src"
              rm -rf $dest_dir/src
              ;;
            *)
              echo "Error: Input not understood. Please answer with 'yes' or 'no'."
              install_mesos
              ;;
          esac
        fi
        echo "Trying to create ${dest_dir}"
        mkdir -p "$dest_dir"
        local esc_dest_dir="${dest_dir//\//\\/}"
        sed_in_place -e "s/service_dir=.*\$/service_dir=$esc_dest_dir/" ${BIN_DIRECTORY}/install_mesos.bash
        bash "${BIN_DIRECTORY}/install_mesos.bash"
        echo "Installed mesos in: ${dest_dir}"
        mesos_installation="$dest_dir"
        ;;
      yes)
        echo "Type the path of the compiled mesos installation followed by [ENTER]:"
        dest_dir=$(read_dir)
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
  pushd "$BIN_DIRECTORY" ; cd ..
  echo "Installing chronos"
  mvn package
  popd
  local esc_mesos_dir="${mesos_installation//\//\\/}"
  local project_dir="${BIN_DIRECTORY}/../"
  local esc_project_dir="${project_dir//\//\\/}"
  esc_project_dir="${esc_project_dir//./\.}"
  echo "Updating the start-chronos script in ${BIN_DIRECTORY} to point to your installation at ${mesos_installation}"
  sed_in_place -e "s/MESOS_HOME=.*\$/MESOS_HOME=$esc_mesos_dir/" "${BIN_DIRECTORY}/start-chronos.bash"
  sed_in_place -e "s/CHRONOS_HOME=.*\$/CHRONOS_HOME=$esc_project_dir/" "${BIN_DIRECTORY}/start-chronos.bash"
}

echo ; echo "Welcome to the interactive chronos installation. This script will first install mesos and then chronos." ; echo
echo "Depending on your hardware and internet connection, this script might take 15 - 25 minutes as we're compiling mesos from source."
echo "If you run into any issues, please check the FAQ in the chronos repo."

install_mesos
install_chronos
echo "Starting chronos..."
bash "${BIN_DIRECTORY}/start-chronos.bash"
