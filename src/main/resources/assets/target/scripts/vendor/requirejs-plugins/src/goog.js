/** @license
 * RequireJS plugin for loading Google Ajax API modules thru `google.load`
 * Author: Miller Medeiros
 * Version: 0.2.0 (2011/12/06)
 * Released under the MIT license
 */

define(["async","propertyParser"],function(e,t){function r(e){var r=n.exec(e),i={moduleName:r[1],version:r[2]||"1"};return i.settings=t.parseProperties(r[3]),i}var n=/^([^,]+)(?:,([^,]+))?(?:,(.+))?/;return{load:function(e,t,n,i){if(i.isBuild)n(null);else{var s=r(e),o=s.settings;o.callback=n,t(["async!"+(document.location.protocol==="https:"?"https":"http")+"://www.google.com/jsapi"],function(){google.load(s.moduleName,s.version,o)})}}}});