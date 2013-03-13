/**
 * Basic parser for URL properties
 * @author Miller Medeiros
 * @version 0.1.0 (2011/12/06)
 * MIT license
 */

define([],function(){function n(t){var n,i={};while(n=e.exec(t))i[n[1]]=r(n[2]||n[3]);return i}function r(e){return t.test(e)?e=e.replace(t,"$1").split(","):e==="null"?e=null:e==="false"?e=!1:e==="true"?e=!0:e===""||e==="''"||e==='""'?e="":isNaN(e)||(e=+e),e}var e=/([\w-]+)\s*:\s*(?:(\[[^\]]+\])|([^,]+)),?/g,t=/^\[([^\]]+)\]$/;return{parseProperties:n,typecastVal:r}});