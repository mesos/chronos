/**
 * Basic parser for URL properties
 * @author Miller Medeiros
 * @version 0.1.0 (2011/12/06)
 * MIT license
 */

/** @license
 * RequireJS plugin for loading web fonts using the WebFont Loader
 * Author: Miller Medeiros
 * Version: 0.2.0 (2011/12/06)
 * Released under the MIT license
 */

define("propertyParser",[],function(){function n(t){var n,i={};while(n=e.exec(t))i[n[1]]=r(n[2]||n[3]);return i}function r(e){return t.test(e)?e=e.replace(t,"$1").split(","):e==="null"?e=null:e==="false"?e=!1:e==="true"?e=!0:e===""||e==="''"||e==='""'?e="":isNaN(e)||(e=+e),e}var e=/([\w-]+)\s*:\s*(?:(\[[^\]]+\])|([^,]+)),?/g,t=/^\[([^\]]+)\]$/;return{parseProperties:n,typecastVal:r}}),define("font",["propertyParser"],function(e){function n(n){var r={},i=n.split("|"),s=i.length,o;while(s--)o=t.exec(i[s]),r[o[1]]=e.parseProperties(o[2]);return r}var t=/^([^,]+),([^\|]+)\|?/;return{load:function(e,t,r,i){if(i.isBuild)r(null);else{var s=n(e);s.active=r,s.inactive=function(){r(!1)},t([(document.location.protocol==="https:"?"https":"http")+"://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js"],function(){WebFont.load(s)})}}}}),define("fonts",["font!google,families:[Russo One,Quicksand,Inconsolata]"],function(){return!0});