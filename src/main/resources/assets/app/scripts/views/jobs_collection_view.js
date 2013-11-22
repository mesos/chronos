/**
 * Jobs Collection View
 */
define([
  'jquery',
  'views/results_header_view',
  'views/job_item_view',
  'components/mixable_view',
  'components/parent_view'
],
function($,
         ResultsHeaderView,
         JobItemView,
         MixableView,
         ParentView) {

  'use strict';

  var JobsCollectionView = MixableView.extend({
    mixins: {
      collectionViews: ParentView.InstanceMethods
    },

    el: '.joblist',

    events: {
      'click .item': 'clickJobItem'
    },

    initialize: function() {
      this.header = (new ResultsHeaderView()).render();

      this.bindCollectionViews(JobItemView, this.collection).
        listenTo(this, {
          'parentView:afterRenderChildren': this.childrenRendered
        });
    },

    render: function() {
      this.trigger('render');
      return this;
    },

    childrenRendered: function() {
      $('.all-jobs-count').html(app.resultsCollection.size());
      app.Helpers.filterList();
    },

    clickJobItem: function(e) {
      var $jobItem = $(e.currentTarget);

      if ($jobItem.hasClass('ignore')) {
        return;
      }

      var model = this.collection.get($jobItem.data('cid'));
      var active = app.detailsCollection.get(model.cid);

      if (active == null) {
        app.detailsCollection.add(model, {at: 0});
      } else {
        app.detailsCollection.remove(model);
      }
    },

    remove: function() {
      this.unbindCollectionViews();
      return MixableView.prototype.remove.call(this);
    }

  });

  return JobsCollectionView;
});
