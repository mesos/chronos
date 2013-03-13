/**
 * CreateClass.js
 *
 */
define([
  'underscore',
  'backbone',
  'vendor/benchmark'
],
function(_, Backbone, Benchmark) {
  var CreateClass;

  CreateClass = function CreateClass(name, _instanceMethods, _classMethods) {
    var _class, instanceMethods, classMethods, fnsToMap;

    if (_.isEmpty(name) || !_.isString(name)) {
      throw new Error("Name must be a non-empty string");
    }

    fnsToMap = function(fns) { return _.reduce((fns || []), function(memo, fn) {
      memo[fn.name] = fn;
      return memo;
    }, {}); };

    instanceMethods = fnsToMap(_instanceMethods);
    classMethods    = fnsToMap(_classMethods);

    _class = eval.call(null, [
      '(function() { function ', name, '() { ',
      '  this._constructor.apply(this, Array.prototype.slice.call(arguments));',
      '}; return ', name, '; }())'
    ].join(''));

    _.extend(_class.prototype, {
      constructor: function() {}
    }, instanceMethods, {
      _constructor: function() {
        this.constructor.apply(this, Array.prototype.slice.call(arguments));
      }
    })

    _.extend(_class, {
      extend: Backbone.Model.extend
    }, classMethods);

    return _class;
  };

  var Generic = function Generic() {};
  Generic.extend = Backbone.Model.extend;

  CreateClass.Benchmark = function(opts) {

    var suite = new Benchmark.Suite('Class Benchmarks'),
        CreateGenericClass, CreateNamedClass,
        AnonymousClass, NamedClass;

    CreateGenericClass = function() {
      return Generic.extend({
        sayHello: function () {
          console.log('hello');
          return 'hello';
        }
      }, {
        makeMe: function () {
          return new TestKlass;
        }
      });
    };

    CreateNamedClass = function() {
      return CreateClass('TestKlass', [
        function sayHello() {
          console.log('hello');
          return 'hello';
        }
      ], [
        function makeMe() {
          return new TestKlass;
        }
      ]);
    };

    AnonymousClass = CreateGenericClass();
    NamedClass     = CreateNamedClass();

    suite.add('CC creation', function() {
      var TestKlass = CreateNamedClass();
    });

    suite.add('extend creation', function() {
      var TestKlass = CreateGenericClass();
    });

    suite.add('CC instantiation', function() {
      var testInstance = new AnonymousClass();
    });

    suite.add('extend instantiation', function() {
      var testInstance = new NamedClass();
    });

    return suite;

  };

  return CreateClass;
});
