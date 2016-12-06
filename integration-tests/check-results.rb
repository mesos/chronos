#!/usr/bin/env ruby

require 'cassandra'
require 'securerandom'

CASSANDRA_HOSTS = ['node-0.cassandra.mesos', 'node-1.cassandra.mesos', 'node-2.cassandra.mesos']
CASSANDRA_PORT = 9042

cluster = Cassandra.cluster(hosts: CASSANDRA_HOSTS, port: CASSANDRA_PORT)

session = cluster.connect

exit_code = 0

future = session.execute_async(
  'SELECT job_name, ts, task_state FROM metrics.chronos WHERE ts >= ? ALLOW FILTERING',
  arguments: [(DateTime.now - 7).to_time]
)
result = []
future.on_success do |rows|
  rows.each do |row|
    result.push({
      :job_name => row['job_name'],
      :ts => row['ts'],
      :task_state => row['task_state'],
    })
  end
end
future.join

grouped = result.group_by {|r| r[:job_name]}

def check_count_equals(count, expected, name, state)
  if count != expected
    puts "State count for name=#{name} and state=#{state} didn't match expected value (got #{count}, expected #{expected}"
    return true
  end
  false
end

def check_count_at_least(count, expected, name, state)
  if count < expected
    puts "State count for name=#{name} and state=#{state} didn't match expected >= value (got #{count}, expected #{expected}"
    return true
  end
  false
end

def get_expected(name)
  if name.include?('hourly')
    24*7
  elsif name.include?('daily')
    7
  elsif name.include?('weekly')
    1
  else
    0
  end
end

had_error = false
grouped.each do |name, result|
  states = result.group_by {|r| r[:task_state]}
  counts = states.map{|k, v| {:state => k, :count =>v.size}}
  puts "Summary for #{name}:"
  puts counts
  expected = get_expected(name)
  next if expected == 0
  counts.each do |v|
    if v[:state] == 'TASK_FINISHED'
      if check_count_equals(v[:count], expected, name, v[:state])
        had_error = true
      end
    elsif v[:state] == 'TASK_RUNNING'
      if check_count_at_least(v[:count], expected, name, v[:state])
        had_error = true
      end
    end
  end
end

if had_error
  exit_code = 1
end

session.close

exit exit_code
