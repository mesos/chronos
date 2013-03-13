/** @license
 * RequireJS Image Plugin
 * Author: Miller Medeiros
 * Version: 0.2.1 (2011/12/15)
 * Released under the MIT license
 */

define([],function(){function r(){}function i(n){return n=n.replace(t,""),n+=n.indexOf("?")<0?"?":"&",n+e+"="+Math.round(2147483647*Math.random())}var e="bust",t="!bust",n="!rel";return{load:function(e,t,i,s){var o;s.isBuild?i(null):(o=new Image,o.onload=function(e){i(o);try{delete o.onload}catch(t){o.onload=r}},e.indexOf(n)!==-1?o.src=t.toUrl(e.replace(n,"")):o.src=e)},normalize:function(e,n){return e.indexOf(t)===-1?e:i(e)}}});