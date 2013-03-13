/** @license
 * Plugin to load JS files that have dependencies but aren't wrapped into
 * `define` calls.
 * Author: Miller Medeiros
 * Version: 0.1.0 (2011/12/13)
 * Released under the MIT license
 */

define([],function(){var e=/^(.*)\[([^\]]*)\]$/;return{load:function(t,n,r,i){var s=e.exec(t);n(s[2].split(","),function(){n([s[1]],function(e){r(e)})})}}});