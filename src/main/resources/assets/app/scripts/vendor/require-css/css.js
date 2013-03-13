/*
 * css! loader plugin
 * Allows for loading stylesheets with the 'css!' syntax.
 *
 * in Chrome 19+, IE10+, Firefox 9+, Safari 6+ <link> tags are used with onload support
 * in all other environments, <style> tags are used with injection to mimic onload support
 *
 * <link> tag can be enforced with the configuration useLinks, although support cannot be guaranteed.
 *
 * External stylesheets loaded with link tags, unless useLinks explicitly set to false assuming CORS support.
 *
 * Stylesheet parsers always use <style> injection even on external urls, which may cause origin issues.
 *
 */
define(['./normalize', 'module'], function(normalize, module) {
  if (typeof window == 'undefined')
    return { load: function(n, r, load){ load() } };
  
  var head = document.getElementsByTagName('head')[0];

  var agentMatch = window.navigator.userAgent.match(/Chrome\/([^ \.]*)|MSIE ([^ ;]*)|Firefox\/([^ ;]*)|Version\/([\d\.]*) Safari\//);

  var browserEngine = window.opera ? 'opera' : '';
  if (agentMatch) {
    if (agentMatch[4])
      browserEngine = 'webkit'
    if (agentMatch[3])
      browserEngine = 'mozilla';
    else if (agentMatch[2])
      browserEngine = 'ie';
    else if (agentMatch[1])
      browserEngine = 'webkit';
  }
  var useLinks = (browserEngine && (parseInt(agentMatch[4]) > 5 || parseInt(agentMatch[3]) > 8 || parseInt(agentMatch[2]) > 9 || parseInt(agentMatch[1]) > 18)) || undefined;

  var config = module.config();
  if (config && config.useLinks !== undefined)
    useLinks = config.useLinks;
  
  /* XHR code - copied from RequireJS text plugin */
  var progIds = ['Msxml2.XMLHTTP', 'Microsoft.XMLHTTP', 'Msxml2.XMLHTTP.4.0'];
  var fileCache = {};
  var get = function(url, callback, errback) {
    if (fileCache[url]) {
      callback(fileCache[url]);
      return;
    }

    var xhr, i, progId;
    if (typeof XMLHttpRequest !== 'undefined')
      xhr = new XMLHttpRequest();
    else if (typeof ActiveXObject !== 'undefined')
      for (i = 0; i < 3; i += 1) {
        progId = progIds[i];
        try {
          xhr = new ActiveXObject(progId);
        }
        catch (e) {}
  
        if (xhr) {
          progIds = [progId];  // so faster next time
          break;
        }
      }
    
    xhr.open('GET', url, requirejs.inlineRequire ? false : true);
  
    xhr.onreadystatechange = function (evt) {
      var status, err;
      //Do not explicitly handle errors, those should be
      //visible via console output in the browser.
      if (xhr.readyState === 4) {
        status = xhr.status;
        if (status > 399 && status < 600) {
          //An http 4xx or 5xx error. Signal an error.
          err = new Error(url + ' HTTP status: ' + status);
          err.xhr = xhr;
          errback(err);
        }
        else {
          fileCache[url] = xhr.responseText;
          callback(xhr.responseText);
        }
      }
    };
    
    xhr.send(null);
  }
  
  //main api object
  var cssAPI = {};
  
  cssAPI.pluginBuilder = './css-builder';
  
  //uses the <style> load method
  var stylesheet = document.createElement('style');
  stylesheet.type = 'text/css';
  head.appendChild(stylesheet);
  
  if (stylesheet.styleSheet)
    cssAPI.inject = function(css) {
      stylesheet.styleSheet.cssText += css;
    }
  else
    cssAPI.inject = function(css) {
      stylesheet.appendChild(document.createTextNode(css));
    }


  var webkitLoadCheck = function(link, callback) {
    setTimeout(function() {
      for (var i = 0; i < document.styleSheets.length; i++) {
        var sheet = document.styleSheets[i];
        //console.log(sheet.href);
        if (sheet.href == link.href)
          return callback();
      }
      webkitLoadCheck(link, callback);
    }, 10);
  }

  var mozillaLoadCheck = function(style, callback) {
    setTimeout(function() {
      try {
        style.sheet.cssRules;
        return callback();
      } catch (e){}
      mozillaLoadCheck(style, callback);
    }, 10);
  }

  // uses the <link> load method
  var createLink = function(url) {
    var link = document.createElement('link');
    link.type = 'text/css';
    link.rel = 'stylesheet';
    link.href = url;
    return link;
  }

  cssAPI.linkLoad = function(url, callback) {
    if (browserEngine == 'webkit') {
      var link = createLink(url);
      webkitLoadCheck(link, callback);
      head.appendChild(link);
    }
    // onload support only in firefox 18+
    else if (browserEngine == 'mozilla' && agentMatch[3] < 18) {
      var style = document.createElement('style');
      style.textContent = '@import "' + url + '"';
      mozillaLoadCheck(style, callback);
      head.appendChild(style);
    }
    else {
      var link = createLink(url);
      link.onload = callback;
      head.appendChild(link);
    }
  }


  cssAPI.inspect = function() {
    if (stylesheet.styleSheet)
      return stylesheet.styleSheet.cssText;
    else if (stylesheet.innerHTML)
      return stylesheet.innerHTML;
  }
  
  cssAPI.normalize = function(name, normalize) {
    if (name.substr(name.length - 4, 4) == '.css')
      name = name.substr(0, name.length - 4);
    
    return normalize(name);
  }

  // NB add @media query support for media imports
  var importRegEx = /@import\s*(url)?\s*(('([^']*)'|"([^"]*)")|\(('([^']*)'|"([^"]*)"|([^\)]*))\))\s*;?/g;

  var pathname = window.location.pathname.split('/');
  pathname.pop();
  pathname = pathname.join('/') + '/';

  var loadCSS = function(fileUrl, callback, errback) {

    //make file url absolute
    if (fileUrl.substr(0, 1) != '/')
      fileUrl = '/' + normalize.convertURIBase(fileUrl, pathname, '/');

    get(fileUrl, function(css) {

      // normalize the css (except import statements)
      css = normalize(css, fileUrl, pathname);

      // detect all import statements in the css and normalize
      var importUrls = [];
      var importIndex = [];
      var importLength = [];
      var match;
      while (match = importRegEx.exec(css)) {
        var importUrl = match[4] || match[5] || match[7] || match[8] || match[9];

        // add less extension if necessary
        if (importUrl.indexOf('.') == -1)
          importUrl += '.less';

        importUrls.push(importUrl);
        importIndex.push(importRegEx.lastIndex - match[0].length);
        importLength.push(match[0].length);
      }

      // load the import stylesheets and substitute into the css
      var completeCnt = 0;
      for (var i = 0; i < importUrls.length; i++)
        (function(i) {
          loadCSS(importUrls[i], function(importCSS) {
            css = css.substr(0, importIndex[i]) + importCSS + css.substr(importIndex[i] + importLength[i]);
            var lenDiff = importCSS.length - importLength[i];
            for (var j = i + 1; j < importUrls.length; j++)
              importIndex[j] += lenDiff;
            completeCnt++;
            if (completeCnt == importUrls.length) {
              callback(css);
            }
          }, errback);
        })(i);

      if (importUrls.length == 0)
        callback(css);
    }, errback);
  }
  
  cssAPI.load = function(cssId, req, load, config, parse) {
    var fileUrl = cssId;
    
    if (fileUrl.substr(fileUrl.length - 4, 4) != '.css' && !parse)
      fileUrl += '.css';
    
    fileUrl = req.toUrl(fileUrl);
    
    //external url -> add as a <link> tag to load
    if (!parse && useLinks !== false && (fileUrl.substr(0, 7) == 'http://' || fileUrl.substr(0, 8) == 'https://' || useLinks)) {
      cssAPI.linkLoad(fileUrl, function() {
        load(cssAPI);
      });
    }
    //internal url or parsing -> inject into <style> tag
    else {
      loadCSS(fileUrl, function(css) {
        // run parsing last - since less is a CSS subset this works fine
        if (parse)
          css = parse(css);

        cssAPI.inject(css);

        load(cssAPI);
      }, load.error);
    }
  }
  
  return cssAPI;
});
