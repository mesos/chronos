/** @license
 * RequireJS plugin for async dependency load like JSONP and Google Maps
 * Author: Miller Medeiros
 * Version: 0.1.1 (2011/11/17)
 * Released under the MIT license
 */

define([],function(){function n(e){var t,n;t=document.createElement("script"),t.type="text/javascript",t.async=!0,t.src=e,n=document.getElementsByTagName("script")[0],n.parentNode.insertBefore(t,n)}function r(t,n){var r=/!(.+)/,i=t.replace(r,""),s=r.test(t)?t.replace(/.+!/,""):e;return i+=i.indexOf("?")<0?"?":"&",i+s+"="+n}function i(){return t+=1,"__async_req_"+t+"__"}var e="callback",t=0;return{load:function(e,t,s,o){if(o.isBuild)s(null);else{var u=i();window[u]=s,n(r(e,u))}}}});