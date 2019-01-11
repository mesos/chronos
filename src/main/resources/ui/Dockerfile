FROM nginx:1.10

RUN apt-get update && apt-get install -y --no-install-recommends \
		ca-certificates \
		curl \
		wget \
    gnupg

ENV APP chronos-ui
ENV HTML_FOLDER /usr/share/nginx/html

RUN curl -sL https://deb.nodesource.com/setup_7.x | bash - \
  && apt-get install -y nodejs && apt-get install -y build-essential \
  && rm -rf /var/lib/apt/lists/* \
  && mkdir -p /$APP $HTML_FOLDER

COPY . /$APP/
COPY deploy.js /


RUN cd /$APP/ && npm install && npm run build \
  && cp index.html $HTML_FOLDER \
  && cp -R assets $HTML_FOLDER/assets \
  && cp -R build/assets/js $HTML_FOLDER/assets

EXPOSE 80

CMD NODE_ENV=production node /deploy.js && nginx -g 'daemon off;'
