/*!
     * chai
     * Copyright(c) 2011-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai version
     */

/*!
     * Primary `Assertion` prototype
     */

/*!
     * Assertion Error
     */

/*!
     * Utils for plugins (not exported)
     */

/*!
     * Core Assertions
     */

/*!
     * Expect interface
     */

/*!
     * Should interface
     */

/*!
     * Assert interface
     */

/*!
     * chai
     * http://chaijs.com
     * Copyright(c) 2011-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Module dependencies.
     */

/*!
     * Module export.
     */

/*!
     * Assertion Constructor
     *
     * Creates object for chaining.
     *
     * @api private
     */

/*!
      * ### Assertion.includeStack
      *
      * User configurable property, influences whether stack trace
      * is included in Assertion error message. Default of false
      * suppresses stack trace in the error message
      *
      *     Assertion.includeStack = true;  // enable stack on error
      *
      * @api public
      */

/*!
     * ### Assertion.showDiff
     *
     * User configurable property, influences whether or not
     * the `showDiff` flag should be included in the thrown
     * AssertionErrors. `false` will always be `false`; `true`
     * will be true when the assertion has requested a diff
     * be shown.
     *
     * @api public
     */

/*!
     * ### .assert(expression, message, negateMessage, expected, actual)
     *
     * Executes an expression and check expectations. Throws AssertionError for reporting if test doesn't pass.
     *
     * @name assert
     * @param {Philosophical} expression to be tested
     * @param {String} message to display if fails
     * @param {String} negatedMessage to display if negated expression fails
     * @param {Mixed} expected value (remember to check for negation)
     * @param {Mixed} actual (optional) will default to `this.obj`
     * @api private
     */

/*!
     * ### ._obj
     *
     * Quick reference to stored `actual` value for plugin developers.
     *
     * @api private
     */

/*!
     * Main export
     */

/*!
     * Inherit from Error
     */

/*!
       * Chai dependencies.
       */

/*!
       * Module export.
       */

/*!
       * Undocumented / untested
       */

/*!
       * Aliases.
       */

/*!
     * Chai - addChainingMethod utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Module dependencies
     */

/*!
     * Module variables
     */

/*!
     * Chai - addMethod utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai - addProperty utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai - flag utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai - getActual utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai - getEnumerableProperties utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai - message composition utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Module dependancies
     */

/*!
     * Chai - getName utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai - getPathValue utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * @see https://github.com/logicalparadox/filtr
     * MIT Licensed
     */

/*!
     * ## parsePath(path)
     *
     * Helper function used to parse string object
     * paths. Use in conjunction with `_getPathValue`.
     *
     *      var parsed = parsePath('myobject.property.subprop');
     *
     * ### Paths:
     *
     * * Can be as near infinitely deep and nested
     * * Arrays are also valid using the formal `myobject.document[3].property`.
     *
     * @param {String} path
     * @returns {Object} parsed
     * @api private
     */

/*!
     * ## _getPathValue(parsed, obj)
     *
     * Helper companion function for `.parsePath` that returns
     * the value located at the parsed address.
     *
     *      var value = getPathValue(parsed, obj);
     *
     * @param {Object} parsed definition from `parsePath`.
     * @param {Object} object to search against
     * @returns {Object|Undefined} value
     * @api private
     */

/*!
     * Chai - getProperties utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * chai
     * Copyright(c) 2011 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Main exports
     */

/*!
     * test utility
     */

/*!
     * type utility
     */

/*!
     * message utility
     */

/*!
     * actual utility
     */

/*!
     * Inspect util
     */

/*!
     * Object Display util
     */

/*!
     * Flag utility
     */

/*!
     * Flag transferring utility
     */

/*!
     * Deep equal utility
     */

/*!
     * Deep path value
     */

/*!
     * Function name
     */

/*!
     * add Property
     */

/*!
     * add Method
     */

/*!
     * overwrite Property
     */

/*!
     * overwrite Method
     */

/*!
     * Add a chainable method
     */

/*!
     * Chai - overwriteMethod utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai - overwriteProperty utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai - test utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai - transferFlags utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Chai - type utility
     * Copyright(c) 2012-2013 Jake Luer <jake@alogicalparadox.com>
     * MIT Licensed
     */

/*!
     * Detectable javascript natives
     */

!function(e,t,n){typeof require=="function"&&typeof exports=="object"&&typeof module=="object"?module.exports=n():typeof define=="function"&&typeof define.amd=="object"?define([],function(){return n()}):t[e]=n()}("chai",this,function(){function require(e){var t=require.resolve(e),n=require.modules[t];if(!n)throw new Error('failed to require "'+e+'"');return n.exports||(n.exports={},n.call(n.exports,n,n.exports,require.relative(t))),n.exports}return require.modules={},require.resolve=function(e){var t=e,n=e+".js",r=e+"/index.js";return require.modules[n]&&n||require.modules[r]&&r||t},require.register=function(e,t){require.modules[e]=t},require.relative=function(e){return function(t){if("."!=t.charAt(0))return require(t);var n=e.split("/"),r=t.split("/");n.pop();for(var i=0;i<r.length;i++){var s=r[i];".."==s?n.pop():"."!=s&&n.push(s)}return require(n.join("/"))}},require.alias=function(e,t){var n=require.modules[e];require.modules[t]=n},require.register("chai.js",function(e,t,n){var r=[],t=e.exports={};t.version="1.5.0",t.Assertion=n("./chai/assertion"),t.AssertionError=n("./chai/error");var i=n("./chai/utils");t.use=function(e){return~r.indexOf(e)||(e(this,i),r.push(e)),this};var s=n("./chai/core/assertions");t.use(s);var o=n("./chai/interface/expect");t.use(o);var u=n("./chai/interface/should");t.use(u);var a=n("./chai/interface/assert");t.use(a)}),require.register("chai/assertion.js",function(e,t,n){function o(e,t,n){s(this,"ssfi",n||arguments.callee),s(this,"object",e),s(this,"message",t)}var r=n("./error"),i=n("./utils"),s=i.flag;e.exports=o,o.includeStack=!1,o.showDiff=!0,o.addProperty=function(e,t){i.addProperty(this.prototype,e,t)},o.addMethod=function(e,t){i.addMethod(this.prototype,e,t)},o.addChainableMethod=function(e,t,n){i.addChainableMethod(this.prototype,e,t,n)},o.overwriteProperty=function(e,t){i.overwriteProperty(this.prototype,e,t)},o.overwriteMethod=function(e,t){i.overwriteMethod(this.prototype,e,t)},o.prototype.assert=function(e,t,n,u,a,f){var l=i.test(this,arguments);!0!==f&&(f=!1),!0!==o.showDiff&&(f=!1);if(!l){var t=i.getMessage(this,arguments),c=i.getActual(this,arguments);throw new r({message:t,actual:c,expected:u,stackStartFunction:o.includeStack?this.assert:s(this,"ssfi"),showDiff:f})}},Object.defineProperty(o.prototype,"_obj",{get:function(){return s(this,"object")},set:function(e){s(this,"object",e)}})}),require.register("chai/core/assertions.js",function(e,t,n){e.exports=function(e,t){function s(e,n){n&&i(this,"message",n),e=e.toLowerCase();var r=i(this,"object"),s=~["a","e","i","o","u"].indexOf(e.charAt(0))?"an ":"a ";this.assert(e===t.type(r),"expected #{this} to be "+s+e,"expected #{this} not to be "+s+e)}function o(){i(this,"contains",!0)}function u(e,n){n&&i(this,"message",n);var r=i(this,"object");this.assert(~r.indexOf(e),"expected #{this} to include "+t.inspect(e),"expected #{this} to not include "+t.inspect(e))}function a(){var e=i(this,"object"),t=Object.prototype.toString.call(e);this.assert("[object Arguments]"===t,"expected #{this} to be arguments but got "+t,"expected #{this} to not be arguments")}function f(e,t){t&&i(this,"message",t);var n=i(this,"object");if(i(this,"deep"))return this.eql(e);this.assert(e===n,"expected #{this} to equal #{exp}","expected #{this} to not equal #{exp}",e,this._obj,!0)}function l(e,n){n&&i(this,"message",n),this.assert(t.eql(e,i(this,"object")),"expected #{this} to deeply equal #{exp}","expected #{this} to not deeply equal #{exp}",e,this._obj,!0)}function c(e,t){t&&i(this,"message",t);var r=i(this,"object");if(i(this,"doLength")){(new n(r,t)).to.have.property("length");var s=r.length;this.assert(s>e,"expected #{this} to have a length above #{exp} but got #{act}","expected #{this} to not have a length above #{exp}",e,s)}else this.assert(r>e,"expected #{this} to be above "+e,"expected #{this} to be at most "+e)}function h(e,t){t&&i(this,"message",t);var r=i(this,"object");if(i(this,"doLength")){(new n(r,t)).to.have.property("length");var s=r.length;this.assert(s>=e,"expected #{this} to have a length at least #{exp} but got #{act}","expected #{this} to have a length below #{exp}",e,s)}else this.assert(r>=e,"expected #{this} to be at least "+e,"expected #{this} to be below "+e)}function p(e,t){t&&i(this,"message",t);var r=i(this,"object");if(i(this,"doLength")){(new n(r,t)).to.have.property("length");var s=r.length;this.assert(s<e,"expected #{this} to have a length below #{exp} but got #{act}","expected #{this} to not have a length below #{exp}",e,s)}else this.assert(r<e,"expected #{this} to be below "+e,"expected #{this} to be at least "+e)}function d(e,t){t&&i(this,"message",t);var r=i(this,"object");if(i(this,"doLength")){(new n(r,t)).to.have.property("length");var s=r.length;this.assert(s<=e,"expected #{this} to have a length at most #{exp} but got #{act}","expected #{this} to have a length above #{exp}",e,s)}else this.assert(r<=e,"expected #{this} to be at most "+e,"expected #{this} to be above "+e)}function v(e,n){n&&i(this,"message",n);var r=t.getName(e);this.assert(i(this,"object")instanceof e,"expected #{this} to be an instance of "+r,"expected #{this} to not be an instance of "+r)}function m(e,n){n&&i(this,"message",n);var r=i(this,"object");this.assert(r.hasOwnProperty(e),"expected #{this} to have own property "+t.inspect(e),"expected #{this} to not have own property "+t.inspect(e))}function g(){i(this,"doLength",!0)}function y(e,t){t&&i(this,"message",t);var r=i(this,"object");(new n(r,t)).to.have.property("length");var s=r.length;this.assert(s==e,"expected #{this} to have a length of #{exp} but got #{act}","expected #{this} to not have a length of #{act}",e,s)}function b(e){var n=i(this,"object"),r,s=!0;e=e instanceof Array?e:Array.prototype.slice.call(arguments);if(!e.length)throw new Error("keys required");var o=Object.keys(n),u=e.length;s=e.every(function(e){return~o.indexOf(e)}),!i(this,"negate")&&!i(this,"contains")&&(s=s&&e.length==o.length);if(u>1){e=e.map(function(e){return t.inspect(e)});var a=e.pop();r=e.join(", ")+", and "+a}else r=t.inspect(e[0]);r=(u>1?"keys ":"key ")+r,r=(i(this,"contains")?"contain ":"have ")+r,this.assert(s,"expected #{this} to "+r,"expected #{this} to not "+r)}function w(e,r,s){s&&i(this,"message",s);var o=i(this,"object");(new n(o,s)).is.a("function");var u=!1,a=null,f=null,l=null;arguments.length===0?(r=null,e=null):e&&(e instanceof RegExp||"string"==typeof e)?(r=e,e=null):e&&e instanceof Error?(a=e,e=null,r=null):typeof e=="function"?f=(new e).name:e=null;try{o()}catch(c){if(a)return this.assert(c===a,"expected #{this} to throw #{exp} but #{act} was thrown","expected #{this} to not throw #{exp}",a,c),this;if(e){this.assert(c instanceof e,"expected #{this} to throw #{exp} but #{act} was thrown","expected #{this} to not throw #{exp} but #{act} was thrown",f,c);if(!r)return this}var h="object"===t.type(c)&&"message"in c?c.message:""+c;if(h!=null&&r&&r instanceof RegExp)return this.assert(r.exec(h),"expected #{this} to throw error matching #{exp} but got #{act}","expected #{this} to throw error not matching #{exp}",r,h),this;if(h!=null&&r&&"string"==typeof r)return this.assert(~h.indexOf(r),"expected #{this} to throw error including #{exp} but got #{act}","expected #{this} to throw error not including #{act}",r,h),this;u=!0,l=c}var p="",d=f!==null?f:a?"#{exp}":"an error";u&&(p=" but #{act} was thrown"),this.assert(u===!0,"expected #{this} to throw "+d+p,"expected #{this} to not throw "+d+p,a,l)}var n=e.Assertion,r=Object.prototype.toString,i=t.flag;["to","be","been","is","and","have","with","that","at","of"].forEach(function(e){n.addProperty(e,function(){return this})}),n.addProperty("not",function(){i(this,"negate",!0)}),n.addProperty("deep",function(){i(this,"deep",!0)}),n.addChainableMethod("an",s),n.addChainableMethod("a",s),n.addChainableMethod("include",u,o),n.addChainableMethod("contain",u,o),n.addProperty("ok",function(){this.assert(i(this,"object"),"expected #{this} to be truthy","expected #{this} to be falsy")}),n.addProperty("true",function(){this.assert(!0===i(this,"object"),"expected #{this} to be true","expected #{this} to be false",this.negate?!1:!0)}),n.addProperty("false",function(){this.assert(!1===i(this,"object"),"expected #{this} to be false","expected #{this} to be true",this.negate?!0:!1)}),n.addProperty("null",function(){this.assert(null===i(this,"object"),"expected #{this} to be null","expected #{this} not to be null")}),n.addProperty("undefined",function(){this.assert(undefined===i(this,"object"),"expected #{this} to be undefined","expected #{this} not to be undefined")}),n.addProperty("exist",function(){this.assert(null!=i(this,"object"),"expected #{this} to exist","expected #{this} to not exist")}),n.addProperty("empty",function(){var e=i(this,"object"),t=e;Array.isArray(e)||"string"==typeof object?t=e.length:typeof e=="object"&&(t=Object.keys(e).length),this.assert(!t,"expected #{this} to be empty","expected #{this} not to be empty")}),n.addProperty("arguments",a),n.addProperty("Arguments",a),n.addMethod("equal",f),n.addMethod("equals",f),n.addMethod("eq",f),n.addMethod("eql",l),n.addMethod("eqls",l),n.addMethod("above",c),n.addMethod("gt",c),n.addMethod("greaterThan",c),n.addMethod("least",h),n.addMethod("gte",h),n.addMethod("below",p),n.addMethod("lt",p),n.addMethod("lessThan",p),n.addMethod("most",d),n.addMethod("lte",d),n.addMethod("within",function(e,t,r){r&&i(this,"message",r);var s=i(this,"object"),o=e+".."+t;if(i(this,"doLength")){(new n(s,r)).to.have.property("length");var u=s.length;this.assert(u>=e&&u<=t,"expected #{this} to have a length within "+o,"expected #{this} to not have a length within "+o)}else this.assert(s>=e&&s<=t,"expected #{this} to be within "+o,"expected #{this} to not be within "+o)}),n.addMethod("instanceof",v),n.addMethod("instanceOf",v),n.addMethod("property",function(e,n,r){r&&i(this,"message",r);var s=i(this,"deep")?"deep property ":"property ",o=i(this,"negate"),u=i(this,"object"),a=i(this,"deep")?t.getPathValue(e,u):u[e];if(o&&undefined!==n){if(undefined===a)throw r=r!=null?r+": ":"",new Error(r+t.inspect(u)+" has no "+s+t.inspect(e))}else this.assert(undefined!==a,"expected #{this} to have a "+s+t.inspect(e),"expected #{this} to not have "+s+t.inspect(e));undefined!==n&&this.assert(n===a,"expected #{this} to have a "+s+t.inspect(e)+" of #{exp}, but got #{act}","expected #{this} to not have a "+s+t.inspect(e)+" of #{act}",n,a),i(this,"object",a)}),n.addMethod("ownProperty",m),n.addMethod("haveOwnProperty",m),n.addChainableMethod("length",y,g),n.addMethod("lengthOf",y,g),n.addMethod("match",function(e,t){t&&i(this,"message",t);var n=i(this,"object");this.assert(e.exec(n),"expected #{this} to match "+e,"expected #{this} not to match "+e)}),n.addMethod("string",function(e,r){r&&i(this,"message",r);var s=i(this,"object");(new n(s,r)).is.a("string"),this.assert(~s.indexOf(e),"expected #{this} to contain "+t.inspect(e),"expected #{this} to not contain "+t.inspect(e))}),n.addMethod("keys",b),n.addMethod("key",b),n.addMethod("throw",w),n.addMethod("throws",w),n.addMethod("Throw",w),n.addMethod("respondTo",function(e,n){n&&i(this,"message",n);var r=i(this,"object"),s=i(this,"itself"),o="function"===t.type(r)&&!s?r.prototype[e]:r[e];this.assert("function"==typeof o,"expected #{this} to respond to "+t.inspect(e),"expected #{this} to not respond to "+t.inspect(e))}),n.addProperty("itself",function(){i(this,"itself",!0)}),n.addMethod("satisfy",function(e,n){n&&i(this,"message",n);var r=i(this,"object");this.assert(e(r),"expected #{this} to satisfy "+t.objDisplay(e),"expected #{this} to not satisfy"+t.objDisplay(e),this.negate?!1:!0,e(r))}),n.addMethod("closeTo",function(e,t,n){n&&i(this,"message",n);var r=i(this,"object");this.assert(Math.abs(r-e)<=t,"expected #{this} to be close to "+e+" +/- "+t,"expected #{this} not to be close to "+e+" +/- "+t)})}}),require.register("chai/error.js",function(e,t,n){function r(e){e=e||{},this.message=e.message,this.actual=e.actual,this.expected=e.expected,this.operator=e.operator,this.showDiff=e.showDiff;if(e.stackStartFunction&&Error.captureStackTrace){var t=e.stackStartFunction;Error.captureStackTrace(this,t)}}e.exports=r,r.prototype=Object.create(Error.prototype),r.prototype.name="AssertionError",r.prototype.constructor=r,r.prototype.toString=function(){return this.message}}),require.register("chai/interface/assert.js",function(module,exports,require){module.exports=function(chai,util){var Assertion=chai.Assertion,flag=util.flag,assert=chai.assert=function(e,t){var n=new Assertion(null);n.assert(e,t,"[ negation message unavailable ]")};assert.fail=function(e,t,n,r){throw new chai.AssertionError({actual:e,expected:t,message:n,operator:r,stackStartFunction:assert.fail})},assert.ok=function(e,t){(new Assertion(e,t)).is.ok},assert.equal=function(e,t,n){var r=new Assertion(e,n);r.assert(t==flag(r,"object"),"expected #{this} to equal #{exp}","expected #{this} to not equal #{act}",t,e)},assert.notEqual=function(e,t,n){var r=new Assertion(e,n);r.assert(t!=flag(r,"object"),"expected #{this} to not equal #{exp}","expected #{this} to equal #{act}",t,e)},assert.strictEqual=function(e,t,n){(new Assertion(e,n)).to.equal(t)},assert.notStrictEqual=function(e,t,n){(new Assertion(e,n)).to.not.equal(t)},assert.deepEqual=function(e,t,n){(new Assertion(e,n)).to.eql(t)},assert.notDeepEqual=function(e,t,n){(new Assertion(e,n)).to.not.eql(t)},assert.isTrue=function(e,t){(new Assertion(e,t)).is["true"]},assert.isFalse=function(e,t){(new Assertion(e,t)).is["false"]},assert.isNull=function(e,t){(new Assertion(e,t)).to.equal(null)},assert.isNotNull=function(e,t){(new Assertion(e,t)).to.not.equal(null)},assert.isUndefined=function(e,t){(new Assertion(e,t)).to.equal(undefined)},assert.isDefined=function(e,t){(new Assertion(e,t)).to.not.equal(undefined)},assert.isFunction=function(e,t){(new Assertion(e,t)).to.be.a("function")},assert.isNotFunction=function(e,t){(new Assertion(e,t)).to.not.be.a("function")},assert.isObject=function(e,t){(new Assertion(e,t)).to.be.a("object")},assert.isNotObject=function(e,t){(new Assertion(e,t)).to.not.be.a("object")},assert.isArray=function(e,t){(new Assertion(e,t)).to.be.an("array")},assert.isNotArray=function(e,t){(new Assertion(e,t)).to.not.be.an("array")},assert.isString=function(e,t){(new Assertion(e,t)).to.be.a("string")},assert.isNotString=function(e,t){(new Assertion(e,t)).to.not.be.a("string")},assert.isNumber=function(e,t){(new Assertion(e,t)).to.be.a("number")},assert.isNotNumber=function(e,t){(new Assertion(e,t)).to.not.be.a("number")},assert.isBoolean=function(e,t){(new Assertion(e,t)).to.be.a("boolean")},assert.isNotBoolean=function(e,t){(new Assertion(e,t)).to.not.be.a("boolean")},assert.typeOf=function(e,t,n){(new Assertion(e,n)).to.be.a(t)},assert.notTypeOf=function(e,t,n){(new Assertion(e,n)).to.not.be.a(t)},assert.instanceOf=function(e,t,n){(new Assertion(e,n)).to.be.instanceOf(t)},assert.notInstanceOf=function(e,t,n){(new Assertion(e,n)).to.not.be.instanceOf(t)},assert.include=function(e,t,n){var r=new Assertion(e,n);Array.isArray(e)?r.to.include(t):"string"==typeof e&&r.to.contain.string(t)},assert.match=function(e,t,n){(new Assertion(e,n)).to.match(t)},assert.notMatch=function(e,t,n){(new Assertion(e,n)).to.not.match(t)},assert.property=function(e,t,n){(new Assertion(e,n)).to.have.property(t)},assert.notProperty=function(e,t,n){(new Assertion(e,n)).to.not.have.property(t)},assert.deepProperty=function(e,t,n){(new Assertion(e,n)).to.have.deep.property(t)},assert.notDeepProperty=function(e,t,n){(new Assertion(e,n)).to.not.have.deep.property(t)},assert.propertyVal=function(e,t,n,r){(new Assertion(e,r)).to.have.property(t,n)},assert.propertyNotVal=function(e,t,n,r){(new Assertion(e,r)).to.not.have.property(t,n)},assert.deepPropertyVal=function(e,t,n,r){(new Assertion(e,r)).to.have.deep.property(t,n)},assert.deepPropertyNotVal=function(e,t,n,r){(new Assertion(e,r)).to.not.have.deep.property(t,n)},assert.lengthOf=function(e,t,n){(new Assertion(e,n)).to.have.length(t)},assert.Throw=function(e,t,n,r){if("string"==typeof t||t instanceof RegExp)n=t,t=null;(new Assertion(e,r)).to.Throw(t,n)},assert.doesNotThrow=function(e,t,n){"string"==typeof t&&(n=t,t=null),(new Assertion(e,n)).to.not.Throw(t)},assert.operator=function(val,operator,val2,msg){if(!~["==","===",">",">=","<","<=","!=","!=="].indexOf(operator))throw new Error('Invalid operator "'+operator+'"');var test=new Assertion(eval(val+operator+val2),msg);test.assert(!0===flag(test,"object"),"expected "+util.inspect(val)+" to be "+operator+" "+util.inspect(val2),"expected "+util.inspect(val)+" to not be "+operator+" "+util.inspect(val2))},assert.closeTo=function(e,t,n,r){(new Assertion(e,r)).to.be.closeTo(t,n)},assert.ifError=function(e,t){(new Assertion(e,t)).to.not.be.ok},function alias(e,t){return assert[t]=assert[e],alias}("Throw","throw")("Throw","throws")}}),require.register("chai/interface/expect.js",function(e,t,n){e.exports=function(e,t){e.expect=function(t,n){return new e.Assertion(t,n)}}}),require.register("chai/interface/should.js",function(e,t,n){e.exports=function(e,t){function r(){Object.defineProperty(Object.prototype,"should",{set:function(e){Object.defineProperty(this,"should",{value:e,enumerable:!0,configurable:!0,writable:!0})},get:function(){return this instanceof String||this instanceof Number?new n(this.constructor(this)):this instanceof Boolean?new n(this==1):new n(this)},configurable:!0});var e={};return e.equal=function(e,t,r){(new n(e,r)).to.equal(t)},e.Throw=function(e,t,r,i){(new n(e,i)).to.Throw(t,r)},e.exist=function(e,t){(new n(e,t)).to.exist},e.not={},e.not.equal=function(e,t,r){(new n(e,r)).to.not.equal(t)},e.not.Throw=function(e,t,r,i){(new n(e,i)).to.not.Throw(t,r)},e.not.exist=function(e,t){(new n(e,t)).to.not.exist},e["throw"]=e.Throw,e.not["throw"]=e.not.Throw,e}var n=e.Assertion;e.should=r,e.Should=r}}),require.register("chai/utils/addChainableMethod.js",function(e,t,n){var r=n("./transferFlags"),i="__proto__"in Object,s=/^(?:length|name|arguments|caller)$/;e.exports=function(e,t,n,o){typeof o!="function"&&(o=function(){}),Object.defineProperty(e,t,{get:function(){o.call(this);var t=function(){var e=n.apply(this,arguments);return e===undefined?this:e};if(i)t.__proto__=this;else{var u=Object.getOwnPropertyNames(e);u.forEach(function(n){if(!s.test(n)){var r=Object.getOwnPropertyDescriptor(e,n);Object.defineProperty(t,n,r)}})}return r(this,t),t},configurable:!0})}}),require.register("chai/utils/addMethod.js",function(e,t,n){e.exports=function(e,t,n){e[t]=function(){var e=n.apply(this,arguments);return e===undefined?this:e}}}),require.register("chai/utils/addProperty.js",function(e,t,n){e.exports=function(e,t,n){Object.defineProperty(e,t,{get:function(){var e=n.call(this);return e===undefined?this:e},configurable:!0})}}),require.register("chai/utils/eql.js",function(e,t,n){function o(e,t,n){if(e===t)return!0;if(i.isBuffer(e)&&i.isBuffer(t)){if(e.length!=t.length)return!1;for(var r=0;r<e.length;r++)if(e[r]!==t[r])return!1;return!0}return e instanceof Date&&t instanceof Date?e.getTime()===t.getTime():typeof e!="object"&&typeof t!="object"?e===t:f(e,t,n)}function u(e){return e===null||e===undefined}function a(e){return Object.prototype.toString.call(e)=="[object Arguments]"}function f(e,t,n){if(u(e)||u(t))return!1;if(e.prototype!==t.prototype)return!1;var i;if(n){for(i=0;i<n.length;i++)if(n[i][0]===e&&n[i][1]===t||n[i][0]===t&&n[i][1]===e)return!0}else n=[];if(a(e))return a(t)?(e=pSlice.call(e),t=pSlice.call(t),o(e,t,n)):!1;try{var s=r(e),f=r(t),l}catch(c){return!1}if(s.length!=f.length)return!1;s.sort(),f.sort();for(i=s.length-1;i>=0;i--)if(s[i]!=f[i])return!1;n.push([e,t]);for(i=s.length-1;i>=0;i--){l=s[i];if(!o(e[l],t[l],n))return!1}return!0}e.exports=o;var r=n("./getEnumerableProperties"),i;try{i=n("buffer").Buffer}catch(s){i={isBuffer:function(){return!1}}}}),require.register("chai/utils/flag.js",function(e,t,n){e.exports=function(e,t,n){var r=e.__flags||(e.__flags=Object.create(null));if(arguments.length!==3)return r[t];r[t]=n}}),require.register("chai/utils/getActual.js",function(e,t,n){e.exports=function(e,t){var n=t[4];return"undefined"!=typeof n?n:e._obj}}),require.register("chai/utils/getEnumerableProperties.js",function(e,t,n){e.exports=function(t){var n=[];for(var r in t)n.push(r);return n}}),require.register("chai/utils/getMessage.js",function(e,t,n){var r=n("./flag"),i=n("./getActual"),s=n("./inspect"),o=n("./objDisplay");e.exports=function(e,t){var n=r(e,"negate"),s=r(e,"object"),u=t[3],a=i(e,t),f=n?t[2]:t[1],l=r(e,"message");return f=f||"",f=f.replace(/#{this}/g,o(s)).replace(/#{act}/g,o(a)).replace(/#{exp}/g,o(u)),l?l+": "+f:f}}),require.register("chai/utils/getName.js",function(e,t,n){e.exports=function(e){if(e.name)return e.name;var t=/^\s?function ([^(]*)\(/.exec(e);return t&&t[1]?t[1]:""}}),require.register("chai/utils/getPathValue.js",function(e,t,n){function i(e){var t=e.replace(/\[/g,".["),n=t.match(/(\\\.|[^.]+?)+/g);return n.map(function(e){var t=/\[(\d+)\]$/,n=t.exec(e);return n?{i:parseFloat(n[1])}:{p:e}})}function s(e,t){var n=t,r;for(var i=0,s=e.length;i<s;i++){var o=e[i];n?("undefined"!=typeof o.p?n=n[o.p]:"undefined"!=typeof o.i&&(n=n[o.i]),i==s-1&&(r=n)):r=undefined}return r}var r=e.exports=function(e,t){var n=i(e);return s(n,t)}}),require.register("chai/utils/getProperties.js",function(e,t,n){e.exports=function(t){function r(e){n.indexOf(e)===-1&&n.push(e)}var n=Object.getOwnPropertyNames(subject),i=Object.getPrototypeOf(subject);while(i!==null)Object.getOwnPropertyNames(i).forEach(r),i=Object.getPrototypeOf(i);return n}}),require.register("chai/utils/index.js",function(e,t,n){var t=e.exports={};t.test=n("./test"),t.type=n("./type"),t.getMessage=n("./getMessage"),t.getActual=n("./getActual"),t.inspect=n("./inspect"),t.objDisplay=n("./objDisplay"),t.flag=n("./flag"),t.transferFlags=n("./transferFlags"),t.eql=n("./eql"),t.getPathValue=n("./getPathValue"),t.getName=n("./getName"),t.addProperty=n("./addProperty"),t.addMethod=n("./addMethod"),t.overwriteProperty=n("./overwriteProperty"),t.overwriteMethod=n("./overwriteMethod"),t.addChainableMethod=n("./addChainableMethod")}),require.register("chai/utils/inspect.js",function(e,t,n){function o(e,t,n,r){var i={showHidden:t,seen:[],stylize:function(e){return e}};return f(i,e,typeof n=="undefined"?2:n)}function f(e,n,o){if(n&&typeof n.inspect=="function"&&n.inspect!==t.inspect&&(!n.constructor||n.constructor.prototype!==n))return n.inspect(o);var f=l(e,n);if(f)return f;if(a(n))return u(n);var b=s(n),w=e.showHidden?i(n):b;if(w.length===0||y(n)&&(w.length===1&&w[0]==="stack"||w.length===2&&w[0]==="description"&&w[1]==="stack")){if(typeof n=="function"){var E=r(n),S=E?": "+E:"";return e.stylize("[Function"+S+"]","special")}if(m(n))return e.stylize(RegExp.prototype.toString.call(n),"regexp");if(g(n))return e.stylize(Date.prototype.toUTCString.call(n),"date");if(y(n))return c(n)}var x="",T=!1,N=["{","}"];v(n)&&(T=!0,N=["[","]"]);if(typeof n=="function"){var E=r(n),S=E?": "+E:"";x=" [Function"+S+"]"}m(n)&&(x=" "+RegExp.prototype.toString.call(n)),g(n)&&(x=" "+Date.prototype.toUTCString.call(n));if(y(n))return c(n);if(w.length!==0||!!T&&n.length!=0){if(o<0)return m(n)?e.stylize(RegExp.prototype.toString.call(n),"regexp"):e.stylize("[Object]","special");e.seen.push(n);var C;return T?C=h(e,n,o,b,w):C=w.map(function(t){return p(e,n,o,b,t,T)}),e.seen.pop(),d(C,x,N)}return N[0]+x+N[1]}function l(e,t){switch(typeof t){case"undefined":return e.stylize("undefined","undefined");case"string":var n="'"+JSON.stringify(t).replace(/^"|"$/g,"").replace(/'/g,"\\'").replace(/\\"/g,'"')+"'";return e.stylize(n,"string");case"number":return e.stylize(""+t,"number");case"boolean":return e.stylize(""+t,"boolean")}if(t===null)return e.stylize("null","null")}function c(e){return"["+Error.prototype.toString.call(e)+"]"}function h(e,t,n,r,i){var s=[];for(var o=0,u=t.length;o<u;++o)Object.prototype.hasOwnProperty.call(t,String(o))?s.push(p(e,t,n,r,String(o),!0)):s.push("");return i.forEach(function(i){i.match(/^\d+$/)||s.push(p(e,t,n,r,i,!0))}),s}function p(e,t,n,r,i,s){var o,u;t.__lookupGetter__&&(t.__lookupGetter__(i)?t.__lookupSetter__(i)?u=e.stylize("[Getter/Setter]","special"):u=e.stylize("[Getter]","special"):t.__lookupSetter__(i)&&(u=e.stylize("[Setter]","special"))),r.indexOf(i)<0&&(o="["+i+"]"),u||(e.seen.indexOf(t[i])<0?(n===null?u=f(e,t[i],null):u=f(e,t[i],n-1),u.indexOf("\n")>-1&&(s?u=u.split("\n").map(function(e){return"  "+e}).join("\n").substr(2):u="\n"+u.split("\n").map(function(e){return"   "+e}).join("\n"))):u=e.stylize("[Circular]","special"));if(typeof o=="undefined"){if(s&&i.match(/^\d+$/))return u;o=JSON.stringify(""+i),o.match(/^"([a-zA-Z_][a-zA-Z_0-9]*)"$/)?(o=o.substr(1,o.length-2),o=e.stylize(o,"name")):(o=o.replace(/'/g,"\\'").replace(/\\"/g,'"').replace(/(^"|"$)/g,"'"),o=e.stylize(o,"string"))}return o+": "+u}function d(e,t,n){var r=0,i=e.reduce(function(e,t){return r++,t.indexOf("\n")>=0&&r++,e+t.length+1},0);return i>60?n[0]+(t===""?"":t+"\n ")+" "+e.join(",\n  ")+" "+n[1]:n[0]+t+" "+e.join(", ")+" "+n[1]}function v(e){return Array.isArray(e)||typeof e=="object"&&b(e)==="[object Array]"}function m(e){return typeof e=="object"&&b(e)==="[object RegExp]"}function g(e){return typeof e=="object"&&b(e)==="[object Date]"}function y(e){return typeof e=="object"&&b(e)==="[object Error]"}function b(e){return Object.prototype.toString.call(e)}var r=n("./getName"),i=n("./getProperties"),s=n("./getEnumerableProperties");e.exports=o;var u=function(e){if("outerHTML"in e)return e.outerHTML;var t="http://www.w3.org/1999/xhtml",n=document.createElementNS(t,"_"),r=(window.HTMLElement||window.Element).prototype,i=new XMLSerializer,s;return document.xmlVersion?i.serializeToString(e):(n.appendChild(e.cloneNode(!1)),s=n.innerHTML.replace("><",">"+e.innerHTML+"<"),n.innerHTML="",s)},a=function(e){return typeof HTMLElement=="object"?e instanceof HTMLElement:e&&typeof e=="object"&&e.nodeType===1&&typeof e.nodeName=="string"}}),require.register("chai/utils/objDisplay.js",function(e,t,n){var r=n("./inspect");e.exports=function(e){var t=r(e),n=Object.prototype.toString.call(e);if(t.length>=40){if(n==="[object Function]")return!e.name||e.name===""?"[Function]":"[Function: "+e.name+"]";if(n==="[object Array]")return"[ Array("+e.length+") ]";if(n==="[object Object]"){var i=Object.keys(e),s=i.length>2?i.splice(0,2).join(", ")+", ...":i.join(", ");return"{ Object ("+s+") }"}return t}return t}}),require.register("chai/utils/overwriteMethod.js",function(e,t,n){e.exports=function(e,t,n){var r=e[t],i=function(){return this};r&&"function"==typeof r&&(i=r),e[t]=function(){var e=n(i).apply(this,arguments);return e===undefined?this:e}}}),require.register("chai/utils/overwriteProperty.js",function(e,t,n){e.exports=function(e,t,n){var r=Object.getOwnPropertyDescriptor(e,t),i=function(){};r&&"function"==typeof r.get&&(i=r.get),Object.defineProperty(e,t,{get:function(){var e=n(i).call(this);return e===undefined?this:e},configurable:!0})}}),require.register("chai/utils/test.js",function(e,t,n){var r=n("./flag");e.exports=function(e,t){var n=r(e,"negate"),i=t[0];return n?!i:i}}),require.register("chai/utils/transferFlags.js",function(e,t,n){e.exports=function(e,t,n){var r=e.__flags||(e.__flags=Object.create(null));t.__flags||(t.__flags=Object.create(null)),n=arguments.length===3?n:!0;for(var i in r)if(n||i!=="object"&&i!=="ssfi"&&i!="message")t.__flags[i]=r[i]}}),require.register("chai/utils/type.js",function(e,t,n){var r={"[object Arguments]":"arguments","[object Array]":"array","[object Date]":"date","[object Function]":"function","[object Number]":"number","[object RegExp]":"regexp","[object String]":"string"};e.exports=function(e){var t=Object.prototype.toString.call(e);return r[t]?r[t]:e===null?"null":e===undefined?"undefined":e===Object(e)?"object":typeof e}}),require.alias("./chai.js","chai"),require("chai")});