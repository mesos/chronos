/**
 * Job View
 */
define([
  'views/bound_view',
  'hbs!templates/job_item_view',
],
function(BoundView,
         JobItemViewTpl) {

  'use strict';

  var JobItemView = BoundView.extend({

    template: JobItemViewTpl,

    getBindModels: function() {
      return {
        job: this.model
      };
    },

    render: function() {
      var element = document.createElement('div');
      element.innerHTML = this.toHTML();

      this.setElement(element.firstChild);
      this.trigger('render', {sync: true});

      return this;
    },

    setElement: function(element) {
      BoundView.prototype.setElement.call(this, element);
      this.addRivets();
      this.trigger('render', {sync: true});
    },

    toHTML: function() {
      var data = this.model.toData(),
          html = this.template(data);

      return html;
    }
  });

  return JobItemView;
});
