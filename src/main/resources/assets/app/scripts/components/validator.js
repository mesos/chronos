/**
 * Validator
 *
 */
define([
  'backbone',
  'underscore'
],
function(Backbone, _) {
  var Validator, ShouldContext, ValidatorDefinition;

  ValidatorDefinition = function ValidatorDefinition() {
  };

  _.extend(ValidatorDefinition, {
    merge: function(definitions) {
      return _.extend.apply(null, [{}].concat(definitions));
    }
  });

  ShouldContext = function ShouldContext(name, model) {
    if (!(this instanceof ShouldContext)) {
      return new ShouldContext(name);
    };

    this.name = name;
    this.failures = [];
    this.subfailures = {};
  };

  _.extend(ShouldContext.prototype, {

    should: function should(name, predicate) {
      if (!this.failures.length && !predicate.call()) {
        this.failures.push(name);
      }
    },

    context: function context(name, fn, vals) {
      var ctx, args;
      this.subfailures[name] = ctx = new ShouldContext(name);
      args = !!vals ? (_.isArray(vals) ? vals : [vals]) : [];

      fn.apply(ctx, args.concat([]));
      //fn.call(ctx, (val || null), ctx.should, ctx.context);
    },

    getModel: function getModel() {
      if (this.model && (this.model instanceof Backbone.Model)) {
        return this.model;
      }

      return null;
    },

    withCollection: function withCollection(fn) {
      var c = this.getCollection();

      return !!c && !!fn && !!fn.call(null, c);
    },

    getCollection: function getCollection() {
      var m = this.getModel();
      return !!m ? m.collection : null;
    },

    getFailures: function getFailures() {
      var failures;

      failures = _.extend.apply(null, ([{}]).concat(_.map(this.subfailures,
        function(v, k) {
          var o = {};
          o[k]  = _.map(v.getFailures()[k], function(v2) {
            return [k, v2].join(' ');
          });
          return o;
        }
      )));

      failures[this.name] = _.map(this.failures, function(v) {
        return [this.name, 'should', v].join(' ');
      }, this);

      return failures;
    }
  });

  Validator = function Validator(model, validations) {
    if (!(this instanceof Validator)) {
      return new Validator(model, validations);
    }

    this._setOptions(model, validations);
  };

  _.extend(Validator.prototype, {
    initialize: function() {},

    _setOptions: function(model, validations) {
      this.validations = _.reduce(validations, function(memo, v, k) {
        var validatorFn;

        if (_.isString(v)) {
          validatorFn = Validator._namedValidations[v];
        } else if (_.isFunction(v)) {
          validatorFn = v;
        }

        if (!!validatorFn) { memo[k] = validatorFn; }

        return memo;
      }, {}, this);
      this.model   = model;
      this.errors  = {};
      this.isPlain = !(this.model instanceof Backbone.Model);
    },

    get: function(key) {
      if (!!this.model) {
        return this.isPlain ? this.model[key] : this.model.get(key);
      } else {
        return null;
      }
    },

    validate: function() {
      var errors;

      errors = _.reduce(this.validations, function(memo, v, k) {
        var modelValue, error, assertionHolds, shouldContext;

        shouldContext = new ShouldContext('base', this.model);
        modelValue    = this.get(k);

        shouldContext.context(k, v, modelValue);

        error          = shouldContext.getFailures();
        assertionHolds = _.isEmpty(error);

        if (!assertionHolds) { memo[k] = error[k]; }

        return memo;
      }, {}, this);

      return this.errors = errors;
    },

    isValid: function() {
      this.validate();
      return _.size(this.errors) === 0;
    },

    getErrors: function() {
      this.validate();
      return _.extend({}, this.errors, {
        size: _.size(this.errors)
      });
    }
  });

  _.extend(Validator, {
    extend: Backbone.Model.extend,

    addNamedValidation: function(name, fn) {
      if (_.isFunction(fn)) {
        Validator._namedValidations[name] = fn;
      }
    },

    removeNamedValidation: function(name) {
      Validator._namedValidations[name] = null;
      return Validator;
    },

    _namedValidations: {},

    from: function() {
      var args, definitions, definition;
      args = Array.prototype.slice.call(arguments);

      definitions = _.filter(args, function(definition) {
        return (definition instanceof ValidatorDefinition) ||
          _.isObject(definition);
      });

      definition = ValidatorDefinition.merge.call(null, definitions);

      return {
        create: function(attrs) {
          return new Validator(attrs, definition);
        }
      };
    }
  });

  return Validator;
});
