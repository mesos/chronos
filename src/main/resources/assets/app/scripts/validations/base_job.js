/**
 * Base Job Model Validator
 *
 */
define([
  'require',
  'backbone',
  'underscore',
  'models/base_job',
  'backbone/validations'
],
function(require,
         Backbone,
         _,
         BaseJob) {

  function getBaseJob() {
    return BaseJob || (BaseJob = require('models/base_job'));
  };

  var BaseValues;

  var ModelScope = function(model) {
    var attrIs, attrIsNot;

    attrIs = function(attr, val) {
      val || (val = model.get('attr'));
      return function(m) { return m.get(attr) === val; };
    };

    attrIsNot = function(attr, val) {
      val || (val = model.get('attr'));
      return function(m) { return m.get(attr) !== val; };
    };

    return {attrIs: attrIs, attrIsNot: attrIsNot};
  };

  var defaultValidations = {
    name: function(options, attributeName, model, valueToSet) {
      var defaults, conflictingModel;

      defaults = getBaseJob().prototype.defaults.call(null);

      if (defaults.name === model.get('name')) {
        return 'Job should have a name.';
      }

      conflictingModel = model.collection && model.collection.some(function(m) {
        return (model.get('name') === m.get('name')) && (model.id !== m.id);
      });

      if (!!conflictingModel) {
        return "Job should have a unique name.";
      }
    },
    command: function(options, attributeName, model, valueToSet) {
      var defaults = getBaseJob().prototype.defaults.call(null),
          isDefault = (defaults.command === valueToSet);

      if (isDefault || _.isEmpty(valueToSet)) {
        return 'Job should have a command.';
      }
    }
  };

  BaseValues = _.reduce(defaultValidations, function(memo, v, k) {
    var name = ['base_job', k].join(':');
    Backbone.Validations.addValidator(name, v);
    memo[k] || (memo[k] = {});
    memo[k][name] = "";

    return memo;
  }, {});

  return _.extend(BaseValues, {
    owner: {
      required: true,
      minlength: 3,
      // This pattern is based on the one described at http://www.regular-expressions.info/email.html.
      pattern: /[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?/
    }
  });
});

