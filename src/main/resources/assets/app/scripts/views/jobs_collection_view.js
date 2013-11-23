/**
 * Jobs Collection View
 */
define([
  'jquery',
  'views/results_header_view',
  'views/job_item_view',
  'components/mixable_view',
  'components/parent_view',
  'components/tooltip_view'
],
function($,
         ResultsHeaderView,
         JobItemView,
         MixableView,
         ParentView,
         TooltipView) {

  'use strict';

  var JobsCollectionView = MixableView.extend({
    mixins: {
      collectionViews: ParentView.InstanceMethods,
      tooltips: TooltipView.InstanceMethods
    },

    el: '.joblist',

    events: {
      'click .item': 'clickJobItem'
    },

    initialize: function() {
      this.header = new ResultsHeaderView();

      this.bindCollectionViews(JobItemView, this.collection).
        listenTo(this, {
          'parentView:afterRenderChildren': this.childrenRendered
        });

      this.listenTo(app.detailsCollection, {
        add: this.setActiveJobItem,
        remove: this.removeActiveJobItem
      });

      this.addTooltips('parentView:afterRenderChildren');
    },

    render: function() {
      this.trigger('render');
      return this;
    },

    childrenRendered: function() {
      app.Helpers.filterList();
    },

    clickJobItem: function(e) {
      var $jobItem = $(e.currentTarget);

      if ($jobItem.hasClass('ignore')) {
        return;
      }

      var model = this.collection.get($jobItem.data('cid'));
      var active = app.detailsCollection.get(model.id);

      if (active == null) {
        app.detailsCollection.add(model, {at: 0});
      } else {
        app.detailsCollection.remove(model);
      }
    },

    removeActiveJobItem: function(model, collection) {
      if (model != null) {
        this.$('[data-cid="' + model.cid + '"]').removeClass('active');
      }
    },

    setActiveJobItem: function(model, collection) {
      var isAdd = model && collection;
      var active = app.detailsCollection.get(model.id);

      if (isAdd || active) {
        this.$('[data-cid="' + model.cid + '"]').addClass('active');
      } else {
        this.removeActive(model);
      }
    },

    remove: function() {
      this.unbindCollectionViews();
      return MixableView.prototype.remove.call(this);
    }

  });

  return JobsCollectionView;
});
