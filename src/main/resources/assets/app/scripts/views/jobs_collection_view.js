/**
 * Jobs Collection View
 *
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'views/results_header_view',
  'views/job_item_view',
  'components/mixable_view',
  'components/parent_view'
],
function($,
         _,
         Backbone,
         ResultsHeaderView,
         JobItemView,
         MixableView,
         ParentView) {

  var Remove = MixableView.prototype.remove,
      JobsCollectionView;

  JobsCollectionView = MixableView.extend({
    mixins: {
      collectionViews: ParentView.InstanceMethods
    },

    el: '.joblist',

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

    remove: function() {
      this.unbindCollectionViews();
      return Remove.call(this);
    }

  });

  return JobsCollectionView;
});
