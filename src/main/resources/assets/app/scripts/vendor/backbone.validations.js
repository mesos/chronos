define(function (require, exports, module) {
// Copyright (C) 2011 Neal Stewart
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
// of the Software, and to permit persons to whom the Software is furnished to do
// so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.


(function(Backbone) {

// Require Underscore and Backbone if there's a `require` function.
// This makes `backbone.validations` work on the server or when using
// `browserify`.
if (typeof require !== 'undefined') {
  _ = require('underscore');
  Backbone = require('backbone');
}

// Premade Validators
Backbone.Validations = {};

var validators = {
  "custom" : function(methodName, attributeName, model, valueToSet) {
    return model[methodName](attributeName, valueToSet);
  },

  "required" : function(isRequired, attributeName, model, valueToSet) {
    if (isRequired && (_.isNull(valueToSet) || _.isUndefined(valueToSet) || valueToSet === "")) {
      return "required";
    } else {
      return false;
    }
  },

  "in" : function(whitelist, attributeName, model, valueToSet) {
    return _.include(whitelist, valueToSet) ? undefined : "in";
  },

  "email" : function(type, attributeName, model, valueToSet) {
    var emailRegex = new RegExp("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", "i");

    if (_.isString(valueToSet) && !valueToSet.match(emailRegex)) {
      return "email";
    }
  },

  "url" : function(type, attributeName, model, valueToSet) {
    // taken from jQuery UI validation
    var urlRegex = /^(https?|ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i;
    if (_.isString(valueToSet) && !valueToSet.match(urlRegex)) {
      return "url";
    }
  },

  "number" : function(type, attributeName, model, valueToSet) {
    return isNaN(valueToSet) ? 'number' : undefined;
  },

  "Array": function(type, attributeName, model, valueToSet) {
    return _.isArray(valueToSet) ? undefined : 'Array';
  },

  "Boolean": function(type, attributeName, model, valueToSet) {
    return _.isBoolean(valueToSet) ? undefined : 'Boolean';
  },

  "String": function(type, attributeName, model, valueToSet) {
    return _.isString(valueToSet) ? undefined : 'String';
  },

  "digits": function (type, attributeName, model, valueToSet) {
    var isBeingSet = !_.isUndefined(valueToSet);
    return (!/^\d+$/.test(valueToSet) && isBeingSet) ? 'digits' : undefined;
  },

  "pattern" : function(pattern, attributeName, model, valueToSet) {
    if (_.isString(valueToSet)) {
      if (pattern.test(valueToSet)) {
        return false;
      } else {
        return "pattern";
      }
    }
  },

  "min" : function(minimumValue, attributeName, model, valueToSet) {
    if (valueToSet < minimumValue) {
      return "min";
    }
  },

  "max" : function(maximumValue, attributeName, model, valueToSet) {
    if (valueToSet > maximumValue) {
      return "max";
    }
  },

  "minlength" : function(minlength, attributeName, model, valueToSet) {
    if (_.isString(valueToSet)) {
      if (valueToSet.length < minlength) { return "minlength"; }
    }
  },

  "maxlength" : function(maxlength, attributeName, model, valueToSet) {
    if (_.isString(valueToSet)) {
      if (valueToSet.length > maxlength) { return "maxlength"; }
    }
  },

  "arrayElem": function(validator, attributeName, model, valueToSet) {
    if (_.isArray(valueToSet) && !_.all(valueToSet, validator)) {
      return "validElem";
    } else return false;
  }
};

var customValidators = {};
var getCustomValidator = function(name) {
  var cv = customValidators[name];
  if (!cv) { throw "custom validator '"+name+"' could not be found."; }
  return cv;
};

Backbone.Validations.addValidator = function(name, validator) {
  if (validators.hasOwnProperty(name) || customValidators.hasOwnProperty(name)) {
    throw "existing validator";
  }
  customValidators[name] = validator;
};


/*
  The newValidate method overrides validate in Backbone.Model.
  It has the same interface as the validate function which you
  would provide.

  The returned object looks like this:

    {
      attributeName : ["required", "of", "errors"],
      otherAttributeName: ["and", "so", "on"]
    }

  */
function newValidate(attributes) {
  var errorsForAttribute,
      errorHasOccured,
      errors = {};

  this && this.trigger && this.trigger('before:validate');

  for (var attrName in this._attributeValidators) {
    try {
    var valueToSet = attributes[attrName];
    } catch(e) {}
    var validateAttribute = this._attributeValidators[attrName];
    if (validateAttribute)  {
      errorsForAttribute = validateAttribute(this, valueToSet);
    }
    if (errorsForAttribute) {
      errorHasOccured = true;
      errors[attrName] = errorsForAttribute;
    }
  }

  return errorHasOccured ? errors : false;
}

function createMinValidator(attributeName, minimumValue) {
  return _.bind(validators.min, null, minimumValue);
}

function createMaxValidator(attributeName, maximumValue) {
  return _.bind(validators.max, null, maximumValue);
}


/* createValidator takes in:
    - the model
    - the name of the attribute
    - the type of validation
    - the description of the validation

   returns a function that takes in:
     - the value being set for the attribute

     and either returns nothing (undefined),
     or the error name (string).
  */
function createValidator(attributeName, type, description) {
  var validator,
      validatorMethod,
      customValidator;

  if (type === "type") {
    type = description;
  }
  validator = validators[type];

  if (!validator) { validator = getCustomValidator(type); }

  if (!validator) { throw "Improper validation type '"+type+"'" ; }

  return _.bind(validator, null, description, attributeName);
}

function createAttributeValidator(attributeName, attributeDescription) {
  var validatorsForAttribute = [],
      type,
      desc;

  for (type in attributeDescription) {
    desc = attributeDescription[type];
    validatorsForAttribute.push(createValidator(attributeName, type, desc));
  }

  return function(model, valueToSet, hasOverridenError, options) {
    var validator,
        result,
        errors = [];

    for (var i = 0, length = validatorsForAttribute.length; i < length; i++) {
      validator = validatorsForAttribute[i];
      result = validator(model, valueToSet);
      if (result) {
        if (_.isArray(result)) {
          errors = errors.concat(result);
        } else {
          errors.push(result);
        }
      }
    }

    if (errors.length) {
      return errors;
    } else {
      return false;
    }
  };
}

function createValidators(modelValidations) {
  var attributeValidations,
      attributeValidators = {};

  for (var attrName in modelValidations) {
    attributeValidations = modelValidations[attrName];
    attributeValidators[attrName] = createAttributeValidator(attrName, attributeValidations);
  }

  return attributeValidators;
}

var oldValidate = Backbone.Model.prototype._validate;
function newPerformValidation(attrs, options) {
  var result = oldValidate.apply(this, arguments);

  if(!result){
    _.each(this.validationError, function(error, name) {
      this.trigger('invalid:' + name, this, this.validationError, options);
    }, this);
  }
  return result;
}

// save the old backbone
var oldModel = Backbone.Model,
    oldCtor  = Backbone.Model.prototype.constructor;

// Constructor for our new Validations Model
Backbone.Validations.Model = Backbone.Model.extend({
  constructor : function() {
    // if they pass an object, construct the new validations
    if (typeof this.validate === "object" && this.validate !== null) {
      if (!this.constructor.prototype._attributeValidators) {
        this.constructor.prototype._attributeValidators = createValidators(this.validate);
        this.constructor.prototype.validate = newValidate;
        this.constructor.prototype._validate = newPerformValidation;
      }

      this.validate = _.bind(function(attrs) {
        return newValidate(attrs || this.attributes);
      }, this);
      this._attributeValidators = this.constructor.prototype._attributeValidators;
    }

    oldCtor.apply(this, arguments);
  }
});

// Override Backbone.Model with our new Model
Backbone.Model = Backbone.Validations.Model;


// Requisite noConflict
Backbone.Validations.Model.noConflict =  function() {
  Backbone.Model = oldModel;
};

}(typeof Backbone === 'undefined' ? null : Backbone));

});
