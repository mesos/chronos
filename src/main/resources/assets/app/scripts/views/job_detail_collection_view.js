 /**
 * Job Detail Collection View
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'views/job_detail_view',
  'components/parent_view',
  'components/mixable_view'
],
function($,
         _,
         Backbone,
         JobDetailView,
         ParentView,
         MixableView) {

  'use strict';

  var Remove = MixableView.prototype.remove,
      JobDetailCollectionView;

  JobDetailCollectionView = MixableView.extend({
    mixins: {
      collectionViews: ParentView.InstanceMethods
    },

    el: '.right-pane',

    initialize: function() {
      this.bindCollectionViews(JobDetailView, this.collection);
      this.listenTo(this, {
        'parentView:renderChild': function(data) {
          if (!(data && data.childView)) { return null; }
          if (data.childView.model.isNew()) {
            data.childView.setNew();
          } else {
            data.childView.model.fetchStats();
          }
        }
      });
    },

    render: function() {
      this.trigger('render');

      return this;
    },

    remove: function() {
      this.unbindCollectionViews();
      return Remove.call(this);
    }

  });

  return JobDetailCollectionView;
});
