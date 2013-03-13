/**
 * Application Collection
 *
 */
define([
         'backbone', 'models/application'
       ], function(Backbone, ApplicationModel) {

  var ApplicationCollection;

  ApplicationCollection = Backbone.Collection.extend({
    model: ApplicationModel
  });

  return ApplicationCollection;
});
