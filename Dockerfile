FROM vixns/mesos

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    npm \
    scala && \
    apt-get clean all && \
    ln -s /usr/bin/nodejs /usr/bin/node

ADD . /chronos
RUN cd /chronos && \
    mvn -Dmaven.test.skip=true clean package && \
    mv target/chronos*jar /chronos.jar && \
    mv bin/start-chronos.bash /run.sh && \
    cd / && rm -rf /chronos && \
    dpkg --purge npm scala && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8080

ENTRYPOINT ["/run.sh"]
