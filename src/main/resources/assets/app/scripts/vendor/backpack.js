define(['jquery', 'backbone', 'underscore'], function($, Backbone, _) {
  var Backpack, LightboxModel, Lightbox;

  Backpack = {
    Models: {}
  };

  LightboxModel = Backbone.Model.extend({

    defaults: {
      'open': false,
      'lock': false,
      'backgroundColor': 'rgba(0,0,0,0.9)'
    },

    setContent: function(content){
      this.set('content', content);
    },

    open: function(){
      this.set('open', true);
    },

    close: function() {
      var view = this.get('content');
      this.set('open', false);
      this.trigger('close');

      if (view) {
        view.trigger('close');
        if (_.isFunction(view.close)) { view.close(); }
      }
    },

    dismiss: function(){
      if (!this.get('lock')) {
        this.close();
      }
    },

    lock: function(){
      this.set('lock', true);
    },

    unlock: function(){
      this.set('lock', false);
    },

    color: function(color){
      this.set('backgroundColor', color);
    }

  });

  Backpack.Models.Lightbox = LightboxModel;

  Lightbox = Backbone.View.extend({

    template:  _.template([
      "<div class='lightbox-inner'>",
        "<div class='content'></div>",
      "</div>"
    ].join('')),

    className: 'lightbox',
    events: {
      'click': 'dismiss',
      'click .content': 'noop',
      'click [data-lightbox-close]': 'close'
    },

    bindings: function(){
      this.listenTo(this.model, {
        'change:open': this.toggle,
        'change:content': this.updateContent,
        'change:backgroundColor': this.updateColor
      });
    },

    initialize: function(){
      this.model = new Backpack.Models.Lightbox;
      this.bindings();
      this.toggle();
      this.append();
      if (this.options.content) {
        this.content(this.options.content);
      }
    },

    render: function(){
      var template = this.template();
      this.$el.html(template);
      return this;
    },

    content: function(content){
      this.model.setContent(content);
      return this;
    },

    updateContent: function(){
      var content = this.model.get('content');
      var el = content.render().el;
      this.$content = this.$el.find('.content');
      this.$content.html(el);
    },

    updateColor: function(){
      var color = this.model.get('backgroundColor');
      this.$el.css('background-color', color);
    },

    color: function(color){
      this.model.color(color);
      return this;
    },

    append: function(){
      this.render();
      $('body').append(this.$el);
    },

    toggle: function(){
      var open = this.model.get('open');
      this.$el.toggle(open);
    },

    lock: function(){
      this.model.lock();
      return this;
    },

    unlock: function(){
      this.model.unlock();
      return this;
    },

    open: function(event){
      this.model.open();
      return this;
    },

    close: function(event){
      this.model.close();
      return this;
    },

    dismiss: function(event){
      this.model.dismiss();
      return this;
    },

    noop: function(event){
      event.stopPropagation();
    },

    addClass: function(name) {
      this.$el.find('.content').addClass(name);
      return this;
    },

    removeClass: function(name) {
      this.$el.find('.content').removeClass(name);
      return this;
    }

  });

  Backpack.Lightbox = Lightbox;

  return Backpack;
});
