require-less
===========

Optimizable LESS requiring with RequireJS

Based on the [require-css module](https://github.com/guybedford/require-css), read the documentation there for usage instructions.

Basic Overview
--------------

Allows the construction of scripts that can require LESS files, using the simple RequireJS syntax:

```javascript
define(['less!styles/main'], function() {
  //code that requires the stylesheet: styles/main.less
});
```

When run the in the browser, less is downloaded, parsed and injected. When running a build with the RequireJS optimizer, less is compiled into the build layers dynamically as css with compression support.

Installation and Setup
----------------------

Download the require-less folder manually or use [volo](https://github.com/volojs/volo)(`npm install volo -g`):

```
volo add guybedford/require-less
```

Volo will automatically download [require-css](https://github.com/guybedford/require-css/zipball/master), which is a needed dependency.

Then add the following [map configuration](http://requirejs.org/docs/api.html#config-map) in RequireJS:

```javascript
map: {
  '*': {
    'css': 'require-css/css', // or whatever the path to require-css and require-less are
    'less': 'require-less/less'
  }
}
```

Builds
------

The RequireCSS build system is used to build LESS. The exact same options thus apply.

Pending [r.js issue 289](https://github.com/jrburke/r.js/issues/289), the modules `require-css/css-builder` and `require-less/lessc` require a shallow exclude.

Thus, add the following shallow exclusions at the module level:

```javascript
{
  excludeShallow: ['require-css/css-builder', 'require-less/lessc-server']
}
```


