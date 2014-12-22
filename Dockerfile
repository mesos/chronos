FROM redjack/mesos

RUN apt-get update && apt-get install -y maven node npm

ADD . /chronos

WORKDIR /chronos

RUN mvn clean package

EXPOSE 8081

ENTRYPOINT ["bin/start-chronos.bash"]
