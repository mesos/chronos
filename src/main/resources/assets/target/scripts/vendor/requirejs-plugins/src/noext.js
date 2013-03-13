/** @license
 * RequireJS plugin for loading files without adding the JS extension, useful for
 * JSONP services and any other kind of resource that already contain a file
 * extension or that shouldn't have one (like dynamic scripts).
 * Author: Miller Medeiros
 * Version: 0.3.1 (2011/12/07)
 * Released under the MIT license
 */

define([],function(){var e="noext";return{load:function(e,t,n,r){t([t.toUrl(e)],function(e){n(e)})},normalize:function(t,n){return t+=t.indexOf("?")<0?"?":"&",t+e+"=1"}}});