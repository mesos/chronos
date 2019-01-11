# Chronos-ui

### Development
First install the dependencies by running the command :
``` sh
$ npm install
```

Now, you can run the following command to start the dev server :
``` sh
$ npm start
```

### Build
Install the dependencies then run the build command :
``` sh
$ npm install && npm build
```

### Docker
Build docker :
``` sh
$ docker build -t chronos-ui .
```
You can replace 'chronos-ui' by the name you want

When build is finished, you can run the container.
To make it works, you need to pass the Chronos api url at execution.
``` sh
$ docker run -t -e CHRONOS_API_URL=http://your.url/ chronos-ui
```

After that, you need to find the ip address of your container
``` sh
$ docker inspect $(docker ps | grep chronos-ui | awk '{print $1}') | grep '"IPAddress":'
```

You can now copy/paste the ip address found in your browser.
