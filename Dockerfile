FROM ubuntu:14.04

ENV DEBIAN_FRONTEND=noninteractive

RUN echo "deb http://repos.mesosphere.io/ubuntu/ trusty main" > /etc/apt/sources.list.d/mesosphere.list && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF && \
    apt-get update -q -y && \
    apt-get install -q -y --no-install-recommends \
        curl \
        default-jdk \
        maven \
        mesos \
        node \
        npm \
        scala

COPY . /chronos

WORKDIR /chronos

RUN mvn clean package

EXPOSE 8081

ENTRYPOINT ["bin/start-chronos.bash"]
