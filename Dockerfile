FROM ubuntu:14.04

ENV DEBIAN_FRONTEND=noninteractive

EXPOSE 8080

RUN echo "deb http://repos.mesosphere.io/ubuntu/ trusty main" > /etc/apt/sources.list.d/mesosphere.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv E56151BF && \
    apt-get update -q -y && \
    apt-get install -q -y --no-install-recommends \
        curl
        default-jdk \
        maven \
        mesos \
        npm \
        scala && \
    apt-get clean all && \
    ln -s /usr/bin/nodejs /usr/bin/node

COPY . /chronos

WORKDIR /chronos

RUN mvn clean package

ENTRYPOINT ["bin/start-chronos.bash"]
