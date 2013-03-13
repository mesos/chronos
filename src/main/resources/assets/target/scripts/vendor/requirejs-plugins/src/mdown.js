/** @license
 * RequireJS plugin for loading Markdown files and converting them into HTML.
 * Author: Miller Medeiros
 * Version: 0.1.1 (2012/02/17)
 * Released under the MIT license
 */

define(["text","markdownConverter"],function(e,t){var n={};return{load:function(r,i,s,o){e.get(i.toUrl(r),function(e){e=t.makeHtml(e),o.isBuild?(n[r]=e,s(e)):s(e)})},write:function(t,r,i){if(r in n){var s=e.jsEscape(n[r]);i.asModule(t+"!"+r,"define(function () { return '"+s+"';});\n")}}}});