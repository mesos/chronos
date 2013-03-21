/**
 * Job Detail Stats View
 *
 */
define([
  'jquery',
  'backbone',
  'underscore',
  'components/duration_humanizer',
  'hbs!templates/job_detail_stats'
],
function($,
         Backbone,
         _,
         FormatMS,
         JobDetailStatsViewTpl) {
  'use strict';

  var JobDetailStatsView,
      PercentileKeys;

  PercentileKeys = {
    'median': '50th',
    '75thPercentile': '75th',
    '95thPercentile': '95th',
    '99thPercentile': '99th'
  };

  JobDetailStatsView = Backbone.View.extend({
    template: JobDetailStatsViewTpl,

    events: {},

    initialize: function() {
      this.listenTo(this.model, {
        'change:stats': this.render
      });
    },

    render: function() {
      var stats = this.model.get('stats'),
          keys = [stats].concat(_(PercentileKeys).keys()),
          data = {},
          percentiles,
          html;

      if (stats) {
        percentiles = _.pick.apply(null, keys);
        data.percentiles = _(percentiles).map(function(v, k) {
          return {
            percentile: PercentileKeys[k],
            value: FormatMS(v)
          };
        });
      }

      html = this.template(data);

      this.$el.html(html);
      this.trigger('render');

      return this;
    }
  });

  return JobDetailStatsView;
});
