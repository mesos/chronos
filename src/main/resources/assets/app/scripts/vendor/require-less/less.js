define(['css'], function(css) {
  
  var less = {};
  
  less.pluginBuilder = './less-builder';
  
  if (typeof window == 'undefined') {
    less.load = function(n, r, load) { load(); }
    return less;
  }
  
  //copy api methods from the css plugin
  var instantCallbacks = {};
  less.normalize = function(name, normalize) {
    var instantCallback;
    instantCallback = name.substr(name.length - 1, 1) == '!';

    if (instantCallback)
      name = name.substr(0, name.length - 1);

    if (name.substr(name.length - 5, 5) == '.less')
      name = name.substr(0, name.length - 5);

    name = normalize(name);

    if (instantCallback)
      instantCallbacks[name] = true;

    return name;
  }
  
  less.load = function(lessId, req, load, config) {
    var instantCallback = instantCallbacks[lessId];
    if (instantCallback)
      delete instantCallbacks[lessId];
    
    if (!less.parse) {
      require(['./lessc'], function(lessc) {
        var parser = new lessc.Parser();
        less.parse = function(less) {
          var css;
          parser.parse(less, function(err, tree) {
            if (err)
              throw err;
            css = tree.toCSS();
          });
          //instant callback luckily
          return css;
        }
        css.load(lessId + '.less', req, instantCallback ? function(){} : load, config, less.parse);
      });
    }
    else
      css.load(lessId + '.less', req, instantCallback ? function(){} : load, config, less.parse);
    
    if (instantCallback)
      load();
  }
  
  return less;
});
