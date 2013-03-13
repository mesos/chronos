/**
 * @license handlebars hbs 0.4.0 - Alex Sexton, but Handlebars has it's own licensing junk
 *
 * Available via the MIT or new BSD license.
 * see: http://github.com/jrburke/require-cs for details on the plugin this was based off of
 */

/* Yes, deliciously evil. */
/*jslint evil: true, strict: false, plusplus: false, regexp: false */
/*global require: false, XMLHttpRequest: false, ActiveXObject: false,
define: false, process: false, window: false */
define([
//>>excludeStart('excludeHbs', pragmas.excludeHbs)
'handlebars', 'underscore', 'i18nprecompile', 'json2'
//>>excludeEnd('excludeHbs')
], function (
//>>excludeStart('excludeHbs', pragmas.excludeHbs)
 Handlebars, _, precompile, JSON
//>>excludeEnd('excludeHbs')
) {
//>>excludeStart('excludeHbs', pragmas.excludeHbs)
  var fs, getXhr,
        progIds = ['Msxml2.XMLHTTP', 'Microsoft.XMLHTTP', 'Msxml2.XMLHTTP.4.0'],
        fetchText = function () {
            throw new Error('Environment unsupported.');
        },
        buildMap = [],
        filecode = "w+",
        templateExtension = "hbs",
        customNameExtension = "@hbs",
        devStyleDirectory = "/styles/",
        buildStyleDirectory = "/demo-build/styles/",
        helperDirectory = "template/helpers/",
        i18nDirectory = "template/i18n/",
        buildCSSFileName = "screen.build.css";

    if (typeof window !== "undefined" && window.navigator && window.document && !window.navigator.userAgent.match(/Node.js/)) {
        // Browser action
        getXhr = function () {
            //Would love to dump the ActiveX crap in here. Need IE 6 to die first.
            var xhr, i, progId;
            if (typeof XMLHttpRequest !== "undefined") {
                return new XMLHttpRequest();
            } else {
                for (i = 0; i < 3; i++) {
                    progId = progIds[i];
                    try {
                        xhr = new ActiveXObject(progId);
                    } catch (e) {}

                    if (xhr) {
                        progIds = [progId];  // so faster next time
                        break;
                    }
                }
            }

            if (!xhr) {
                throw new Error("getXhr(): XMLHttpRequest not available");
            }

            return xhr;
        };

        fetchText = function (url, callback) {
            var xhr = getXhr();
            xhr.open('GET', url, true);
            xhr.onreadystatechange = function (evt) {
                //Do not explicitly handle errors, those should be
                //visible via console output in the browser.
                if (xhr.readyState === 4) {
                    callback(xhr.responseText);
                }
            };
            xhr.send(null);
        };

    } else if (typeof process !== "undefined" &&
               process.versions &&
               !!process.versions.node) {
        //Using special require.nodeRequire, something added by r.js.
        fs = require.nodeRequire('fs');
        fetchText = function ( path, callback ) {
            var body = fs.readFileSync(path, 'utf8') || "";
            // we need to remove BOM stuff from the file content
            body = body.replace(/^\uFEFF/, '');
            callback(body);
        };
    } else if (typeof java !== "undefined" && typeof java.io !== "undefined") {
        fetchText = function(path, callback) {
            var f = new java.io.File(path);
            var is = new java.io.FileReader(f);
            var reader = new java.io.BufferedReader(is);
            var line;
            var text = "";
            while ((line = reader.readLine()) !== null) {
                text += new String(line) + "\n";
            }
            reader.close();
            callback(text);
        };
    }

    var cache = {};
    var fetchOrGetCached = function ( path, callback ){
      if ( cache[path] ){
        callback(cache[path]);
      }
      else {
        fetchText(path, function(data){
          cache[path] = data;
          callback.call(this, data);
        });
      }
    };
    var styleList = [], styleMap = {};
//>>excludeEnd('excludeHbs')

      return {

        get: function () {
            return Handlebars;
        },

        write: function (pluginName, name, write) {

            if ( (name + customNameExtension ) in buildMap) {
                var text = buildMap[name + customNameExtension];
                write.asModule(pluginName + "!" + name, text);
            }
        },

        version: '0.4.0',

        load: function (name, parentRequire, load, config) {
          //>>excludeStart('excludeHbs', pragmas.excludeHbs)

            var compiledName = name + customNameExtension,
                disableI18n = (config.hbs && config.hbs.disableI18n),
                partialDeps = [];

            function recursiveNodeSearch( statements, res ) {
              _(statements).forEach(function ( statement ) {
                if ( statement && statement.type && statement.type === 'partial' ) {
                    res.push(statement.id.string);
                }
                if ( statement && statement.program && statement.program.statements ) {
                  recursiveNodeSearch( statement.program.statements, res );
                }
                if ( statement && statement.program && statement.program.inverse && statement.program.inverse.statements ) {
                  recursiveNodeSearch( statement.program.inverse.statements, res );
                }
              });
              return res;
            }

            // TODO :: use the parser to do this!
            function findPartialDeps( nodes ) {
              var res   = [];
              if ( nodes && nodes.statements ) {
                res = recursiveNodeSearch( nodes.statements, [] );
              }
              return _(res).unique();
            }

            // See if the first item is a comment that's json
            function getMetaData( nodes ) {
              var statement, res, test;
              if ( nodes && nodes.statements ) {
                statement = nodes.statements[0];
                if ( statement && statement.type === "comment" ) {
                  try {
                    res = ( statement.comment ).replace(new RegExp('^[\\s]+|[\\s]+$', 'g'), '');
                    test = JSON.parse(res);
                    return res;
                  }
                  catch (e) {
                    return "{}";
                  }
                }
              }
              return "{}";
            }
            function composeParts ( parts ) {
              if ( !parts ) {
                return [];
              }
              var res = [parts[0]],
                  cur = parts[0],
                  i;

              for (i = 1; i < parts.length; ++i) {
                if ( parts.hasOwnProperty(i) ) {
                  cur += "." + parts[i];
                  res.push( cur );
                }
              }
              return res;
            }

            function recursiveVarSearch( statements, res, prefix, helpersres ) {
              prefix = prefix ? prefix+"." : "";

              var  newprefix = "", flag = false;

              // loop through each statement
              _(statements).forEach(function ( statement ) {
                var parts, part, sideways;

                // if it's a mustache block
                if ( statement && statement.type && statement.type === 'mustache' ) {

                  // If it has params, the first part is a helper or something
                  if ( !statement.params || ! statement.params.length ) {
                    parts = composeParts( statement.id.parts );
                    for( part in parts ) {
                      if ( parts[ part ] ) {
                        newprefix = parts[ part ] || newprefix;
                        res.push( prefix + parts[ part ] );
                      }
                    }
                    res.push(prefix + statement.id.string);
                  }

                  var paramsWithoutParts = ['this', '.', '..', './..', '../..', '../../..'];

                  // grab the params
                  if ( statement.params ) {
                    _(statement.params).forEach(function(param) {
                      if ( _(paramsWithoutParts).contains(param.original) ) {
                        helpersres.push(statement.id.string);
                      }

                      parts = composeParts( param.parts );

                      for(var part in parts ) {
                        if ( parts[ part ] ) {
                          newprefix = parts[part] || newprefix;
                          helpersres.push(statement.id.string);
                          res.push( prefix + parts[ part ] );
                        }
                      }
                    });
                  }
                }

                // If it's a meta block
                if ( statement && statement.mustache  ) {
                  recursiveVarSearch( [statement.mustache], res, prefix + newprefix, helpersres );
                }

                // if it's a whole new program
                if ( statement && statement.program && statement.program.statements ) {
                  sideways = recursiveVarSearch([statement.mustache],[], "", helpersres)[0] || "";
                  if ( statement.program.inverse && statement.program.inverse.statements ) {
                    recursiveVarSearch( statement.program.inverse.statements, res, prefix + newprefix + (sideways ? (prefix+newprefix) ? "."+sideways : sideways : ""), helpersres);
                  }
                  recursiveVarSearch( statement.program.statements, res, prefix + newprefix + (sideways ? (prefix+newprefix) ? "."+sideways : sideways : ""), helpersres);
                }
              });
              return res;
            }

            // This finds the Helper dependencies since it's soooo similar
            function getExternalDeps( nodes ) {
              var res   = [];
              var helpersres = [];

              if ( nodes && nodes.statements ) {
                res = recursiveVarSearch( nodes.statements, [], undefined, helpersres );
              }

              var defaultHelpers = ["helperMissing", "blockHelperMissing", "each", "if", "unless", "with"];

              return {
                vars : _(res).chain().unique().map(function(e){
                  if ( e === "" ) {
                    return '.';
                  }
                  if ( e.length && e[e.length-1] === '.' ) {
                    return e.substr(0,e.length-1) + '[]';
                  }
                  return e;
                }).value(),
                helpers : _(helpersres).chain().unique().map(function(e){
                  if ( _(defaultHelpers).contains(e) ) {
                    return undefined;
                  }
                  return e;
                }).compact().value()
              };
            }

            function fetchAndRegister(langMap){
              fetchText(path, function (text) {
                  // for some reason it doesn't include hbs _first_ when i don't add it here...
                  var nodes = Handlebars.parse(text),
                      deps = findPartialDeps( nodes ),
                      meta = getMetaData( nodes ),
                      extDeps = getExternalDeps( nodes ),
                      vars = extDeps.vars,
                      helps = extDeps.helpers || [],
                      depStr = deps.join("', 'hbs!").replace(/_/g, '/'),
                      helpDepStr = config.hbs && config.hbs.disableHelpers ?
                      "" : (function (){
                        var i, paths = [],
                            pathGetter = config.hbs && config.hbs.helperPathCallback
                              ? config.hbs.helperPathCallback
                              : function (name){return (config.hbs && config.hbs.helperDirectory ? config.hbs.helperDirectory : helperDirectory) + name;};

                        for ( i = 0; i < helps.length; i++ ) {
                          paths[i] = "'" + pathGetter(helps[i]) + "'"
                        }
                        return paths;
                      })().join(','),
                      debugOutputStart = "",
                      debugOutputEnd   = "",
                      debugProperties = "",
                      metaObj, head, linkElem;

                  if ( depStr ) {
                    depStr = ",'hbs!" + depStr + "'";
                  }
                  if ( helpDepStr ) {
                    helpDepStr = "," + helpDepStr;
                  }

                  if ( meta !== "{}" ) {
                    try {
                      metaObj = JSON.parse(meta);
                      if ( metaObj && metaObj.styles ) {
                        styleList = _.union(styleList, metaObj.styles);

                        // In dev mode in the browser
                        if ( require.isBrowser && ! config.isBuild ) {
                          head = document.head || document.getElementsByTagName('head')[0];
                          _(metaObj.styles).forEach(function (style) {
                            if ( !styleMap[style] ) {
                              linkElem = document.createElement('link');
                              linkElem.href = config.baseUrl + devStyleDirectory + style + '.css';
                              linkElem.media = 'all';
                              linkElem.rel = 'stylesheet';
                              linkElem.type = 'text/css';
                              head.appendChild(linkElem);
                              styleMap[style] = linkElem;
                            }
                          });
                        }
                        else if ( config.isBuild ) {
                          (function(){
                            var fs  = require.nodeRequire('fs'),
                                str = _(metaObj.styles).map(function (style) {
                                  if (!styleMap[style]) {
                                    styleMap[style] = true;
                                    return "@import url("+style+".css);\n";
                                  }
                                  return "";
                                }).join("\n");

                            // I write out my import statements to a file in order to help me build stuff.
                            // Then I use a tool to inline my import statements afterwards. (you can run r.js on it too)
                            fs.open(__dirname + buildStyleDirectory + buildCSSFileName, filecode, '0666', function( e, id ) {
                              fs.writeSync(id, str, null, encoding='utf8');
                              fs.close(id);
                            });
                            filecode = "a";
                          })();
                        }
                      }
                    }
                    catch(e){
                      console.log('error injecting styles');
                    }
                  }

                  if ( ! config.isBuild && ! config.serverRender ) {
                    debugOutputStart = "<!-- START - " + name + " -->";
                    debugOutputEnd = "<!-- END - " + name + " -->";
                    debugProperties = "t.meta = " + meta + ";\n" +
                                      "t.helpers = " + JSON.stringify(helps) + ";\n" +
                                      "t.deps = " + JSON.stringify(deps) + ";\n" +
                                      "t.vars = " + JSON.stringify(vars) + ";\n";
                  }

                  var mapping = disableI18n? false : _.extend( langMap, config.localeMapping ),
                      prec = precompile( text, mapping, { originalKeyFallback: (config.hbs || {}).originalKeyFallback });

                  text = "/* START_TEMPLATE */\n" +
                         "define(['hbs','handlebars'"+depStr+helpDepStr+"], function( hbs, Handlebars ){ \n" +
                           "var t = Handlebars.template(" + prec + ");\n" +
                           "Handlebars.registerPartial('" + name.replace( /\//g , '_') + "', t);\n" +
                           debugProperties +
                           "return t;\n" +
                         "});\n" +
                         "/* END_TEMPLATE */\n";

                  //Hold on to the transformed text if a build.
                  if (config.isBuild) {
                      buildMap[compiledName] = text;
                  }

                  //IE with conditional comments on cannot handle the
                  //sourceURL trick, so skip it if enabled.
                  /*@if (@_jscript) @else @*/
                  if (!config.isBuild) {
                      text += "\r\n//@ sourceURL=" + path;
                  }
                  /*@end@*/

                  for ( var i in deps ) {
                    if ( deps.hasOwnProperty(i) ) {
                      deps[ i ] = 'hbs!' + deps[ i ].replace(/_/g, '/');
                    }
                  }

                  if ( !config.isBuild ) {
                    require( deps, function (){
                      load.fromText(text);

                      //Give result to load. Need to wait until the module
                      //is fully parse, which will happen after this
                      //execution.
                      parentRequire([name], function (value) {
                        load(value);
                      });
                    });
                  }
                  else {
                    load.fromText(text);

                    //Give result to load. Need to wait until the module
                    //is fully parse, which will happen after this
                    //execution.
                    parentRequire([name], function (value) {
                      load(value);
                    });
                  }

                  if ( config.removeCombined ) {
                    fs.unlinkSync(path);
                  }
              });
            }

            var path = parentRequire.toUrl(name +'.'+ (config.hbs && config.hbs.templateExtension? config.hbs.templateExtension : templateExtension));

            if (disableI18n){
                fetchAndRegister(false);
            } else {
                fetchOrGetCached(parentRequire.toUrl((config.hbs && config.hbs.i18nDirectory ? config.hbs.i18nDirectory : i18nDirectory) + (config.locale || "en_us") + '.json'), function (langMap) {
                  fetchAndRegister(JSON.parse(langMap));
                });
            }
          //>>excludeEnd('excludeHbs')
        }
      };
});
/* END_hbs_PLUGIN */
