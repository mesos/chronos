FROM debian:jessie

RUN apt-get update \
  && apt-get install --no-install-recommends -y --force-yes ruby ruby-dev build-essential \
  && gem install --no-ri --no-rdoc cassandra-driver \
  && apt-get purge -y --auto-remove build-essential \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ADD https://raw.githubusercontent.com/mesos/chronos/master/bin/chronos-sync.rb /chronos/chronos-sync.rb
COPY . /chronos
