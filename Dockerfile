FROM ubuntu:14.04

ENV DEBIAN_FRONTEND=noninteractive

EXPOSE 8080

RUN echo "deb http://repos.mesosphere.io/ubuntu/ trusty main" > /etc/apt/sources.list.d/mesosphere.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv E56151BF && \
    apt-get update -q -y && \
    apt-get install -q -y --no-install-recommends \
        curl \
        default-jdk \
        maven \
        mesos \
        npm \
        scala && \
    apt-get clean -q all && \
    ln -s /usr/bin/nodejs /usr/bin/node

COPY . /chronos

WORKDIR /chronos

RUN export MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1" && \
    mvn clean package && \
    unset MAVEN_OPTS

ENTRYPOINT ["bin/start-chronos.bash"]
