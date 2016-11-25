#!/bin/sh
set -e

branch=`git rev-parse --abbrev-ref HEAD`
if [ "$branch" = "master" ]; then
  set +e # ignore error of following command
  tag=`git describe --exact-match --tags HEAD`
  set -e
  exit_code=$?
  if [ $exit_code = 0 ]; then
    image_tag=`git describe --tags`
  else
    image_tag="latest"
  fi
else
  image_tag=$branch
fi
version=`git describe --tags`

echo "Branch: $branch"
echo "Tag: $tag"
echo "Image tag: $image_tag"
echo "Version: $version"

mkdir -p tmp

# build jar
docker run -v `pwd`:/mnt/build --entrypoint=/bin/sh maven:3-jdk-8 -c "\
  curl -sL https://deb.nodesource.com/setup_7.x | bash - \
  && apt-get update && apt-get install -y --no-install-recommends nodejs \
  && ln -sf /usr/bin/nodejs /usr/bin/node \
  && cp -r /mnt/build /chronos \
  && cd /chronos \
  && mvn clean \
  && mvn versions:set -DnewVersion=$version \
  && mvn package \
  && cp target/chronos-$version.jar /mnt/build/tmp/chronos.jar \
  "

# build image
docker build -t mesosphere/chronos:$image_tag .

if [ ! -z ${DOCKER_HUB_USERNAME+x} -a ! -z ${DOCKER_HUB_PASSWORD+x} ]; then
  # login to dockerhub
  docker login -u "${DOCKER_HUB_USERNAME}" -p "${DOCKER_HUB_PASSWORD}"

  # push image
  docker push mesosphere/chronos:$image_tag
fi
