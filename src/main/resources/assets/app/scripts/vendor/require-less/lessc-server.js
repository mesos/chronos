define(['./lessc'], function(less) {

  var fs, path;

  function FS() {}
  function Path() {}

  FS.readFileSync = function(pathname, encoding) {
    var val;

    try {
      val = readFile(pathname, encoding);
    } catch (e) {
      console.log('\n\n', 'Got error in readFileSync on', pathname);
    }

    return val;
  }

  FS.statSync = function(pathname) {
    if (!java.nio.file.Files.exists(pathname)) {
      throw new Error('File does not exist');
    }

    return true;
  }

  Path.join = function() {
    var paths = Array.prototype.slice.call(arguments),
        resolvedPath = java.nio.file.Paths.get(paths[0]),
        passedFirst = false,
        fullPath;

    fullPath = paths.reduce(function(memo, path) {
      return memo.resolve(path);
    }, resolvedPath);

    return fullPath;
  }

  Path.dirname = function(_path) {
    var path = java.nio.file.Paths.get(_path);

    return path.getParent();
  }

  if (typeof environment === "object" && ({}).toString.call(environment) === "[object Environment]") {
    // Rhino
    // Details on how to detect Rhino: https://github.com/ringo/ringojs/issues/88
    fs = FS;
    path = Path;
  } else  {
    fs = require.nodeRequire('fs');
    path = require.nodeRequire('path');
  }

    if (less && less.Parser) {
      less.Parser.importer = function (file, paths, callback, env) {
          var pathname, data;

          // TODO: Undo this at some point,
          // or use different approach.
          var paths = [].concat(paths);
          paths.push('.');

          for (var i = 0; i < paths.length; i++) {
              try {
                  pathname = path.join(paths[i], file);
                  fs.statSync(pathname);
                  break;
              } catch (e) {
                  pathname = null;
              }
          }

          paths = paths.slice(0, paths.length - 1);

          if (!pathname) {
              if (typeof(env.errback) === "function") {
                  env.errback(file, paths, callback);
              } else {
                  callback({ type: 'File', message: "'" + file + "' wasn't found.\n" });
              }
              return;
          }

          function parseFile(e, data) {
              if (e) return callback(e);
                  env.contents = env.contents || {};
                  env.contents[pathname] = data;      // Updating top importing parser content cache.
              new(less.Parser)({
                      paths: [path.dirname(pathname)].concat(paths),
                      filename: pathname,
                      contents: env.contents,
                      files: env.files,
                      syncImport: env.syncImport,
                      dumpLineNumbers: env.dumpLineNumbers
              }).parse(data, function (e, root) {
                      callback(e, root, pathname);
              });
          };

          try {
              data = fs.readFileSync(pathname, 'utf-8');
              parseFile(null, data);
          } catch (e) {
              parseFile(e);
          }
      }
    } else {
      throw new Error('lessc-server could not load less.Parser');
    }

    return less;
});
