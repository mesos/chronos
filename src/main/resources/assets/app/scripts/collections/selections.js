/**
 * Selections Collection
 *
 */
define([
         'backbone',
         'underscore'
       ], function(Backbone, _) {

  var SelectionModel,
      SelectionCollection;

  SelectionModel = Backbone.Model.extend({});

  SelectionCollection = Backbone.Collection.extend({
    sync: function() {},
    create: function() {
      throw new Error('Selections can\'t be created.');
    }
  });

  return SelectionCollection;
});


