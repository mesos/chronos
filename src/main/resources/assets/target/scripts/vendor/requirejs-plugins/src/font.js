/** @license
 * RequireJS plugin for loading web fonts using the WebFont Loader
 * Author: Miller Medeiros
 * Version: 0.2.0 (2011/12/06)
 * Released under the MIT license
 */

define(["propertyParser"],function(e){function n(n){var r={},i=n.split("|"),s=i.length,o;while(s--)o=t.exec(i[s]),r[o[1]]=e.parseProperties(o[2]);return r}var t=/^([^,]+),([^\|]+)\|?/;return{load:function(e,t,r,i){if(i.isBuild)r(null);else{var s=n(e);s.active=r,s.inactive=function(){r(!1)},t([(document.location.protocol==="https:"?"https":"http")+"://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js"],function(){WebFont.load(s)})}}}});