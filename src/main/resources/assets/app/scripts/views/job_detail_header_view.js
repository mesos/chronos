/**
 * Job Detail Header View
 *
 */
define([
  'jquery',
  'backbone',
  'underscore',
  'hbs!templates/job_detail_header_view',
  'bootstrap/tooltip'
],
function($,
         Backbone,
         _,
         JobDetailHeaderViewTpl) {
  'use strict';

  var JobDetailHeaderView;

  JobDetailHeaderView = Backbone.View.extend({
    className: 'row-fluid job-detail-view job-detail-nav-view',

    template: JobDetailHeaderViewTpl,

    events: {
      'click .delete': 'delete',
      'click .close': 'close',
      'click .cancel': 'cancel',
      'click .run': 'run',
      'click .edit': 'edit',
      'click .save': 'save',
      'click .create': 'create',
      'click .duplicate': 'duplicate',
      'click': 'toggle'
    },

    render: function() {
      var html = this.template(this.model.toData());
      this.$el.html(html);

      return this;
    },

    edit: function(e) {
      this.trigger('edit', e);
    },

    cancel: function(e) {
      this.trigger('cancel', e);
    },

    close: function(e) {
      this.trigger('close', e);
    },

    save: function(e) {
      this.trigger('save', e);
    },

    create: function(e) {
      this.trigger('create', e);
    },

    'delete': function() {
      var model = this.model,
          view  = this,
          destroy = confirm('Are you sure you want to destroy: ' + model.get('name') + '?');

      if (destroy) {
        console.log('destroy')

        model.destroy({
          success: function(model, response, options) {
            console.log('success');
            view.trigger('delete delete:success', {
              success: true,
              response: response
            });

            // TODO: results/details should listen to jobs remove event
            _.each([
              'jobsCollection',
              'resultsCollection',
              'detailsCollection'
            ], function(k) { app[k].remove(model); });
          },

          error: function(model, xhr, options) {
            console.log('error: model was not destroyed', arguments)
            view.trigger('delete delete:error', {
              success: false,
              xhr: xhr
            });
          },

          wait: true
        });
      }
    },

    toggle: function(e) {
      var $el = $(e.target);

      if ($el.hasClass('nav-item') || $el.is('i')) { return null; }

      e && e.preventDefault();
      this.trigger('toggle', e);
    },

    run: function() {
      var model = this.model,
          run = confirm('Are you sure you want to run: ' + model.get('name') + '?');

      if (run) {
        model.run({
          wait: true,

          success: function() {
            console.log('ran: ', model.get('name'))
          },

          error: function() {
            console.log('error: could not run', model.get('name'))
          }
        });
      }
    },

    duplicate: function(event) {
      var copy = this.model.clone();
      app.detailsCollection.unshift(copy, {
        validate: false
      });
    }
  });

  return JobDetailHeaderView;
});
