FROM ubuntu:14.04

RUN echo "deb http://repos.mesosphere.io/ubuntu/ trusty main" > /etc/apt/sources.list.d/mesosphere.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv E56151BF && \
    apt-get update && \
    apt-get install -y maven \
    npm \
    default-jdk \
    mesos \
    scala \
    curl && \
    apt-get clean all && \
    ln -s /usr/bin/nodejs /usr/bin/node

ADD . /chronos

WORKDIR /chronos

RUN mvn clean package -DskipTests

EXPOSE 8080

ENTRYPOINT ["bin/start-chronos.bash"]
