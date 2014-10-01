/**
 * Job Detail Stats View
 *
 */
define([
  'jquery',
  'backbone',
  'underscore',
  'hbs!templates/job_detail_history'
],
function($,
         Backbone,
         _,
         JobDetailHistoryViewTpl) {
  'use strict';

  var JobDetailHistoryView;

  JobDetailHistoryView = Backbone.View.extend({
    template: JobDetailHistoryViewTpl,

    events: {},

    initialize: function() {
      this.listenTo(this.model, {
        'change:taskStatHistory': this.render
      });
    },

    render: function() {
      var history = this.model.get('taskStatHistory'),
          data = {},
          html;

      _.each(history, function(task) {
        if(task.status) {
          if (task.status.toLowerCase() === 'success') {
            task.statusClass = "success";
          } else if (task.status.toLowerCase() === 'fail') {
            task.statusClass = "error";
          }
        }
      })

      data.tasks = history;
      if (data.tasks && (data.tasks.length > 0)) {
        this.$el.removeClass('hide');
      }

      html = this.template(data);

      this.$el.html(html);
      this.trigger('render');

      return this;
    }
  });

  return JobDetailHistoryView;
});
