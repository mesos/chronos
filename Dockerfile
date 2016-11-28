FROM ubuntu:14.04

RUN echo "deb http://repos.mesosphere.io/ubuntu/ trusty main" > /etc/apt/sources.list.d/mesosphere.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv E56151BF && \
    apt-get update && \
    apt-get install --no-install-recommends -y maven \
    npm \
    default-jdk \
    mesos \
    scala \
    curl && \
    apt-get clean all && \
    ln -s /usr/bin/nodejs /usr/bin/node

ADD . /chronos

RUN cd /chronos && \
    mvn -Dmaven.test.skip=true clean package && \
    mv target/chronos*jar /chronos.jar && \
    mv bin/entrypoint.sh /entrypoint.sh && \
    cd / && rm -rf /chronos && \
    dpkg --purge npm scala && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
