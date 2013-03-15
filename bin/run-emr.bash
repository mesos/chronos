#!/bin/bash
set -o nounset	# exit if trying to use an uninitialized var
set -o errexit	# exit if any program fails
set -o pipefail # exit if any program in a pipeline fails, also
set -x 					# dubug mode

# the main dispatcher for running async jobs on the emr cluster
job_type=$1 ; shift
job_url="$1" ; shift
job_args="$@"

# get some environment variables
source /etc/environment
export HOME="/mnt" 			#required for things like RDS CLI

# some useful functions
emr_ssh_opts="-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentityFile=/home/ubuntu/.ssh/bot -o User=hadoop"

function get_emr_jobtracker() {
	source /etc/environment
	emr_list=`ruby1.8 /opt/elastic-mapreduce-ruby/elastic-mapreduce --credentials /srv/emr/credentials.json --list`
	# TOOD(FL|IS): There may be a case in which the JobFlow is in "stopping state / shutting down"
	emr_tracker=`echo "$emr_list" | grep '^[^ ]' | grep -i production |  egrep 'RUNNING|WAITING' | head -n 1 | awk '{print $3}'`

	echo "$emr_tracker"
}

function get_chronos_host() {
	echo "`dig +short +tcp _*._*.chronos2.g.aws.airbnb.com SRV | cut -d' ' -f 4 | tail -n1`:4400"
}

function encode {
	local string="${1}"
	local strlen=${#string}
	local encoded=""

	for (( pos=0 ; pos<strlen ; pos++ )); do
		c=${string:$pos:1}
		case "$c" in
			[-_.~a-zA-Z0-9] ) o="${c}" ;;
			* )               printf -v o '%%%02x' "'$c"
		esac
		encoded+="${o}"
	done
	echo "${encoded}"    # You can either set a return variable (FASTER)
	REPLY="${encoded}"   #+or echo the result (EASIER)... or both... :p
}

function make_async_script() {
	script_filename=`mktemp`

	cat > $script_filename <<-"END"
		#!/bin/bash

		set -o nounset
		set -x

		start_time=`date +%s`
		echo "$start_time: starting task $task_id"

		already_notified=''
		# url-encodes a string
		function encode {
			local string="${1}"
			local strlen=${#string}
			local encoded=""

			for (( pos=0 ; pos<strlen ; pos++ )); do
				c=${string:$pos:1}
				case "$c" in
					[-_.~a-zA-Z0-9] ) o="${c}" ;;
					* )               printf -v o '%%%02x' "'$c"
				esac
				encoded+="${o}"
			done
			echo "${encoded}"    # You can either set a return variable (FASTER)
			REPLY="${encoded}"   #+or echo the result (EASIER)... or both... :p
		}

		function notify_chronos {
			exit_code=$?
			stop_time=`date +%s`

			# don't notify twice
			if [[ $already_notified ]]; then
				echo "already notified chronos about exit status $already_notified"
				return
			fi

			already_notified=$exit_code
			echo -n "notifying chronos host $chronos_host that we exited with status $exit_code..."
			curl -L -s -S -X PUT -H "Content-Type: application/json" -d "{\"statusCode\":\"$exit_code\"}" $( echo http://"$chronos_host"/scheduler/task/$( encode "$task_id" ) )
			echo "done with code $?"
		}

		# notify chronos when we exit or fail
		trap notify_chronos EXIT ERR
	END

	echo $script_filename
}

# generate the script that runs the job
script=`make_async_script`
trap "rm -f $script" EXIT

# make a place for the remote job to live
emr_tracker=`get_emr_jobtracker`
remote_dir="/mnt/chronos/${mesos_task_id}"
ssh $emr_ssh_opts $emr_tracker "mkdir -p $remote_dir"

# put the job on the emr job tracker
if [[ $job_url == "file://"* ]]
then
	local_path=${job_url:7}
	job_path="${remote_dir}/`basename $local_path`"

	scp $emr_ssh_opts $local_path $emr_tracker:$job_path

elif [[ $job_url == "sssp://"* ]]
then
	job_path="set inside the script"
	job_url=${job_url:7} 							# strip out the 'sssp://'
	cat >> $script <<-"END"
		latest_release=`wget -q -O - http://sssp.musta.ch:8080/${job_url}?always_list=1`
		job_path="/mnt/chronos/${task_id}/${latest_release}"

		mkdir -p `dirname $job_path`
		cd `dirname $job_path`
		wget --no-verbose -N --content-disposition http://sssp.musta.ch:8080/${job_url}
		cd -
	END

elif [[ $job_url == "http://"* || $job_url == "https://"* ]]
then
	job_path="set inside the script"
	cat >> $script <<-"END"
		job_path="/mnt/chronos/${task_id}/`basename $job_url`"
		if [[ ! -e "${job_path}" ]]
		then
			mkdir -p `dirname ${job_path}`
			wget --no-verbose --no-check-certificate -O "${job_path}" ${job_url}
		fi
	END

elif [[ $job_url == "s3://"* ]]
then
	job_path="$job_url"
	echo "S3 Url given, attempting to submit directly!"

elif [[ $job_url == "-" ]]
then
	job_path="/mnt/chronos/${mesos_task_id}"
	echo "File omitted, passing through all options"
else
	echo "invalid job url! no valid protocol specified"
	exit 1
fi

# add the command to run the job
if [[ $job_type == "jar" ]]
then
	cat >> $script <<-"END"

	echo "running jar file on hadoop..."
	bin/hadoop jar ${job_path} ${job_args}
	END
elif [[ $job_type == "pig" ]]
then
	cat >> $script <<-"END"

	encoded_job_path="$(encode ${job_path})"
	echo "running pig script for file: $encoded_job_path"

	bin/pig -f ${encoded_job_path} ${job_args}
	END
elif [[ $job_type == "hive" ]]
then
	cat >> $script <<-"END"

	echo "running hive job: ${job_path} with args: ${job_args}"
	bin/hive -f ${job_path} ${job_args}
	END
else
	echo "Invalid job type '$job_type'"
	exit 2
fi

# put the script on the emr box
suffix=$(shuf -i 2000-650000 -n 1)
remote_script="${remote_dir}/dispatch_script-$(date '+%H%m%S')-${suffix}.bash"
scp $emr_ssh_opts $script $emr_tracker:$remote_script

# what command are we going to run?
ssh_cmd="ssh -n $emr_ssh_opts"
output="${remote_dir}/output"
runner="nohup bash $remote_script >>${output}.stdout 2>>${output}.stderr &"

# run the script
echo "running script $remote_script on tracker $emr_tracker"
chronos_host=`get_chronos_host`
$ssh_cmd $emr_tracker \
	chronos_host="\"$chronos_host\"" task_id="\"$mesos_task_id\"" \
	job_url="\"$job_url\"" job_path="\"$job_path\"" job_args="\"$job_args\"" \
	"$runner"

# we're done!
code=$?
echo "job dispatched successfully!"

exit $code
