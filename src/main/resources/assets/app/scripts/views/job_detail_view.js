/**
 * Job Detail View
 */
define([
  'jquery',
  'underscore',
  'views/job_detail_header_view',
  'views/job_detail_stats',
  'views/bound_view',
  'components/tooltip_view',
  'components/fuzzy_select2',
  'hbs!templates/job_detail_view',
  'hbs!templates/job_persistence_error',
  'hbs!templates/job_persistence_success',
  'templates/helpers/join_with',
  'bootstrap/alert',
  'bootstrap/button',
  'bootstrap/collapse',
  'bootstrap/tooltip',
  'bootstrap/transition',
  'jquery/pickadate',
  'bootstrap/timepicker'
],
function($,
         _,
         JobDetailHeaderView,
         JobDetailStatsView,
         BoundView,
         TooltipView,
         FuzzySelect2,
         JobDetailViewTpl,
         JobPersistenceErrorTpl,
         JobPersistenceSuccessTpl) {

  'use strict';

  var JobDetailView, asyncExecutorPath, Remove;

  var LIST_SEPARATOR = ', ';

  asyncExecutorPath = '/srv/mesos/utils/async-executor.arx';

  Remove = BoundView.prototype.remove;

  function isHttpError(v) {
    if (_.isNumber(v)) { return (v >= 400) && (v <= 599); }
    else if (v && v.status) { return isHttpError(v.status); }
    else { return false; }
  }

  function NormalizeErrors(v, k) {
    if (_.isArray(v) && _.chain(v).first().isObject().value()) {
      return _.chain(v).map(function(_v) {
        return _.keys(_v);
      }).flatten().uniq().map(function(v2) {
        return {name: [k, v2].join(' '), value: v2, field: v2};
      }).value();
    } else {
      return {name: k, value: v, field: k};
    }
  }

  JobDetailView = BoundView.extend({

    mixins: {
      tooltips: TooltipView.InstanceMethods
    },

    className: function() {
      var cssClasses = ['job-details-wrapper'];

      if (this.model && this.model.cid) {
        cssClasses.push('job-' + this.model.cid);
      }

      return cssClasses.join(' ');
    },

    template: JobDetailViewTpl,

    events: {
      'click .invalid-field': 'highlightInvalidField'
    },

    initialize: function() {
      _.bindAll(this,
                'updateParents',
                'saveError',
                'createError',
                'saveSuccess',
                'createSuccess');

      this.listenTo(this.model, {
        invalid: this.renderInvalid,
        save: this.save
      });

      this.addRivets();
      this.addTooltips();
      this.$('.collapse').collapse();
    },

    remove: function() {
      this.disableEdit();
      this.undelegateEvents();
      return Remove.call(this);
    },

    getBindModels: function() {
      return {
        job: this.model
      };
    },

    render: function() {
      var data = this.model.toData();
      data.cid = this.model.cid;

      var html = this.template(data);
      this.$el.html(html);

      this.addRivets();
      this.renderHeader();
      this.renderParents();
      this.renderStats();
      this.trigger('render');

      if (this.$el.hasClass('edit-job')) {
        this.enableEdit();
      }

      return this;
    },

    renderInvalid: function(model, errors) {
      var errorData = { save: true };

      this.enableEdit();
      errorData.validationErrors = _.map(errors, function(v, k) {
        this.highlightValidationError(v, k);
        return NormalizeErrors(v, k);
      }, this);

      this.renderMessage('error', JobPersistenceErrorTpl, errorData);
      return this;
    },

    renderHeader: function() {
      var view = this.headerView;
      if (!view) {
        view = new JobDetailHeaderView({
          model: this.model
        });

        this.headerView = view;

        this.listenTo(view, {
          'delete:error': this.deleteError,
          edit: this.edit,
          cancel: this.cancel,
          close: this.close,
          save: this.save,
          create: this.create,
          toggle: this.toggle
        });
      }
      return view.setElement(this.$('.job-detail-nav-view')).render();
    },

    renderStats: function() {
      var view = this.statsView;
      if (!view) {
        try {
          view = new JobDetailStatsView({
            model: this.model
          });

          this.statsView = view;
        } catch (e) {
        }
      }
      if (!view) { return; }
      return view.setElement(this.$('.stats-row')).render();
    },

    renderParents: function() {
      if (this.model.get('parents').length) {
        var $parentsWrapper = this.$el.find('.parents-wrapper'),
            parentsList     = this.model.get('parents').join(LIST_SEPARATOR);

        this.model.set('parentsList', parentsList);
        $parentsWrapper.find('.parents').html(parentsList);
        $parentsWrapper.find('input').val(parentsList);
        $parentsWrapper.show();
      }
    },

    renderDisplayName: function() {
      this.$el.find('.toggleName').html(this.model.get('name'));
    },

    serialize: function() {
      return $('#job-form').serialize();
    },

    click: function() {
      this.blurAll();
    },

    setNew: function() {
      var $form = this.$('form');

      if (!$form.hasClass('create-job')) {
        this.$('form').addClass('edit-job create-job');
        this.enableEdit();
      }
    },

    save: function() {
      this.serializeInputs();
      this.model.save(null, {
        success: this.saveSuccess,
        error: this.saveError
      });
      this.disableEdit();
    },

    deleteError: function(data) {
      var xhr = data.xhr || {};

      this.renderMessage('error', JobPersistenceErrorTpl, {
        'delete': true,
        jobName: this.model.get('name'),
        serverError: {
          text: xhr.responseText,
          status: xhr.status
        }
      });
    },

    persistenceError: function(model, verb, jqXhr) {
      var validationError = model && model.validationError,
          errors          = {
            create: !!(verb === 'create'),
            save:   !!(verb === 'save')
          },
          originalId;

      if (isHttpError(jqXhr)) {
        errors.serverError = {text: jqXhr.responseText, status: jqXhr.status};
      } else if (!validationError) {
        originalId = model.id;
        model.id += Math.random();

        model.validationError = model.validate(model.attributes);
        model.id = originalId;

        if (!model.validationError) { return; }
        else {
          return this.persistenceError(model, verb);
        }
      }

      if (!!validationError) {
        errors.validationErrors = _.reduce(validationError, function(memo, v, k) {
          this.highlightValidationError(v, k);
          var normalizedErrors = NormalizeErrors(v, k);
          return memo.concat(normalizedErrors);
        }, [], this);
      }

      this.renderMessage('error', JobPersistenceErrorTpl, errors);
    },

    highlightValidationError: function(error, errorName) {
      var $form = this.$('form'),
          $el,
          $parent;

      if (_.isArray(error) && (error.length === 1)) {
        error = error[0];
      }

      if (_.isObject(error)) {
        _.each(error, this.highlightValidationError, this);
      } else {
        $el = $form.find('[name="' + errorName + '"]');
        $parent = $el.parents('.control-group').first().addClass('error');

        if (error !== true) {
          $parent.find('.help-inline').text(error).removeClass('hide');
        }
      }
    },

    createError: function(model, jqXhr, options) {
      this.setNew();

      this.persistenceError(model, 'create', jqXhr);
      console.log('create error', 'args', arguments, 'this', this);
    },

    saveError: function(model, jqXhr, options) {
      this.enableEdit();

      this.persistenceError(model, 'save', jqXhr);
      console.log('save error', 'args', arguments, 'this', this);
    },

    persistenceSuccess: function(model, verb) {
      var data = {
        jobName: model.get('name'),
        create: (verb === 'create'),
        save: (verb === 'save')
      };

      this.disableEdit();
      this.render();
      this.renderMessage('success', JobPersistenceSuccessTpl, data);
    },

    createSuccess: function(model, c, options) {
      this.persistenceSuccess(model, 'create');
    },

    saveSuccess: function(model, c, options) {
      this.persistenceSuccess(model, 'save');
    },

    renderMessage: function(classes, tpl, data) {
      var c, $el, name;
      data || (data = {});

      c = (['alert', classes]).join('-');
      $el = this.$('.job-detail-message').show();
      name = this.model.get('name');

      $el.find('.job-message').html(tpl(_.extend({}, data, {
        jobName: name,
        alertClass: c
      }))).find('.alert').alert().on('closed', function() {
        $el.hide();
        $el.parents('form').find('.control-group.error').removeClass('error');
      });
    },

    serializeInputs: function() {
      var $inputs = this.$el.find('.job-input'),
          model = this.model;

      $inputs.each(function(index, el){
        var $el = $(el),
            name = $el.attr('name'),
            val = $el.val();

        console.log(name, val);

        if (name === 'async') {
          if (!$el.is(':checked')) { return; }

          val = (parseInt(val, 10) !== 0);
        } else if (name === 'parents') {
          val = val.split(',');
          val = _.map(val, function(v) {
            return v.trim();
          });
          if (val[0] === '') { val = []; }
        } else if (name === 'disabled') {
          if (!$el.is(':checked')) { return; }

          val = (parseInt(val, 10) === 0);
        }

        console.log("setting model", model, "name", name, "to", val);
        model.set(name, val);

      });

      model.trigger('setSchedule');
    },

    dispose: function() {
      this.off();
      $('.right-pane').html('');
    },

    '$parents': function() {
      return this.$('input.job-input[name="parents"]');
    },

    disableEdit: function() {
      FuzzySelect2.unattach(this.$parents());
      this.$('.async .btn-group .btn').addClass('disabled');
      this.$('form').removeClass('edit-job create-job');
      this.$('.timepicker').timepicker('remove');

      this._remember = null;
    },

    enableEdit: function() {
      var $form = this.$el.find('form'),
          oldData = this.model.toData();

      $form.addClass('edit-job');

      this.$('.async .btn-group .btn').removeClass('disabled');

      this.$('.datepicker').pickadate({
        format: 'yyyy-mm-dd',
        formatSubmit: 'yyyy-mm-dd',
        dateMin: true
      });

      this.$('.timepicker').timepicker({template: false});

      FuzzySelect2.attach(this.$parents(), {
        collection: app.jobsCollection,
        width: '98%',
        multiple: true,
        exclusions: this.model ? [this.model.id] : []
      }).on('change', this.updateParents);

      this._remember = oldData;
    },

    updateParents: function(e) {
      var added   = e.added,
          removed = e.removed,
          $target = this.$('.job-detail.sched'),
          parents = this.model.get('parents');

      if (added) {
        parents = parents.concat(added.id);
      }

      if (removed) {
        parents = _.without(parents, removed.id);
      }

      this.model.set({parents: parents});

      _.isEmpty(parents) ? $target.show() : $target.hide();
    },

    edit: function(event) {
      var $form = this.$('form');

      if (this.model.isNew()) {
        this.setNew();
      } else {
        $form.addClass('edit-job');
        this.enableEdit();
      }
      $form.find('input:enabled:visible').first().focus();
    },

    create: function(event) {
      event && event.preventDefault() && event.stopPropagation();
      var oldModel, newModel, newModelAdded, newModelValid, view;

      this.disableEdit();
      this.serializeInputs();
      view = this;

      oldModel = this.model;
      newModel = app.jobsCollection.create(oldModel.toData(), {
        success: function(model, jqXhr, options) {
          view.createSuccess(model, jqXhr, options);
          setTimeout(function() {
            newModel.set({persisted: true}, {silent: true});

            app.resultsCollection.add(newModel);
            app.detailsCollection.remove(oldModel).unshift(newModel);
          }, 1000);
        },
        error: function(model, jqXhr, options) {
          if (!!newModelValid) {
            view.createError(model, jqXhr, options);
          }
        }
      });

      newModelAdded = app.jobsCollection.get(newModel.cid);
      newModelValid = newModel.isValid() && !!newModelAdded;

      if (!newModelValid) {
        this.createError(newModel);
      }
    },

    toggle: function(e) {
      if (this.$el.hasClass('up')) {
        this.$el.removeClass('up');
        this.enableEdit();
      } else {
        this.disableEdit();
        this.$el.addClass('up');
      }
    },

    cancel: function(event) {
      var data = this._remember;
      event && event.preventDefault();

      this.disableEdit();

      if (this.model.isNew()) { this.close(); }
      else if (!!data) { this.model.set(data); }
    },

    close: function() {
      var mCid = this.model.cid;

      if (this.model.isNew()) {
        app.detailsCollection.remove(mCid, {create: true});
      } else {
        app.detailsCollection.remove(mCid);
      }

      $('.' + mCid).removeClass('active');
      this.remove();
    },

    blur: function(event) {
      var newVal = $(event.currentTarget).val(),
          name = $(event.currentTarget).attr('name');

      this.$el.find('.'+name).html(newVal);
      this.model.set(name, ''+newVal);
    },

    validate: function(e) {
      e && e.preventDefault();
      var validation = this.model.validate(this.model.attributes);

      $(e.target).css({
        'background-color': (!!validation ? 'red': 'green')
      });
    },

    highlightInvalidField: function(e) {
      var $el = $(e.target),
          fieldName = $el.data('form-field');

      e && e.preventDefault();
      this.enableEdit();
      this.$('form').find('input[name="' + fieldName + '"]').focus();
    }
  });

  return JobDetailView;
});
