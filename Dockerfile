FROM java:8-jre

ADD ./tmp/chronos.jar .
ADD bin/start.sh .
ENTRYPOINT ["bin/start.sh"]
