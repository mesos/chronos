require.config({
  paths: {
    'hbs'                   : 'vendor/require-handlebars-plugin/hbs',
    'handlebars'            : 'vendor/require-handlebars-plugin/Handlebars',
    'text'                  : 'vendor/text',
    'jquery'                : 'vendor/jquery-1.10.2',
    'lodash'                : 'vendor/lodash',
    'backbone'              : 'vendor/backbone',
    'backbone/declarative'  : 'vendor/backbone.declarative',
    'backbone/validations'  : 'vendor/backbone.validations',
    'backbone/mousetrap'    : 'vendor/backbone.mousetrap',
    'bootstrap/alert'       : 'vendor/bootstrap/js/bootstrap-alert',
    'bootstrap/button'      : 'vendor/bootstrap/js/bootstrap-button',
    'bootstrap/collapse'    : 'vendor/bootstrap/js/bootstrap-collapse',
    'bootstrap/dropdown'    : 'vendor/bootstrap/js/bootstrap-dropdown',
    'bootstrap/timepicker'  : 'vendor/bootstrap-timepicker/js/bootstrap-timepicker',
    'bootstrap/tooltip'     : 'vendor/bootstrap/js/bootstrap-tooltip',
    'bootstrap/transition'  : 'vendor/bootstrap/js/bootstrap-transition',
    'd3'                    : 'vendor/d3.v3',
    'rivets'                : 'vendor/rivets-0.5.13',
    'underscore'            : 'vendor/lodash',
    'moment'                : 'vendor/moment.min',
    'backpack'              : 'vendor/backpack',
    'jquery/autotype'       : 'vendor/bootstrap-timepicker/js/jquery.autotype',
    'jquery/select2'        : 'vendor/select2',
    'jquery/pickadate'      : 'vendor/pickadate',
    'jquery/visibility'     : 'vendor/jquery/jquery.visibility',
    'jquery/fastLiveFilter' : 'vendor/jquery/jquery.fastLiveFilter',
    'coffee-script'         : 'vendor/coffee-script',
    'cs'                    : 'vendor/cs',
    'mousetrap'             : 'vendor/mousetrap',
    'mocha'                 : 'vendor/tests/mocha',
    'chai'                  : 'vendor/tests/chai',
    'chai-jquery'           : 'vendor/tests/chai-jquery',
    'chai-backbone'         : 'vendor/tests/chai-backbone'
  },
  map: {
    '*': {
      'css-builder'  : 'vendor/require-css/css-builder',
      'less-builder' : 'vendor/require-less/less-builder',
      'lessc-server' : 'vendor/require-less/lessc-server',
      'lessc'        : 'vendor/require-less/lessc',
      'css'          : 'vendor/require-css/css',
      'less'         : 'vendor/require-less/less'
    },
    'css': {
      'normalize'   : 'vendor/require-css/normalize'
    },
    'less': {
      'normalize'   : 'vendor/require-css/normalize'
    },
    'hbs': {
      'i18nprecompile' : 'vendor/require-handlebars-plugin/hbs/i18nprecompile',
      'json2'          : 'vendor/require-handlebars-plugin/hbs/json2'
    }
  },
  hbs: {
    disableI18n: true,
    helperDirectory: 'templates/helpers/',
    helperPathCallback: function(name) {
      var n = name.split(/(?=[A-Z])/);
      return 'templates/helpers/' + n.join('_').toLocaleLowerCase();
    }
  },
  shim: {
    'jquery/select2': {
      deps: ['jquery'],
      exports: 'jQuery.fn.select2'
    },
    'jquery/fastLiveFilter': {
      deps: ['jquery'],
      exports: 'jQuery.fn.fastLiveFilter'
    },
    'jquery/pickadate': {
      deps: ['jquery'],
      exports: 'jQuery.fn.pickadate'
    },
    'jquery/visibility': {
      deps: ['jquery'],
      exports: 'jQuery.fn._pageVisibility'
    },
    'bootstrap/tooltip': {
      deps: ['jquery'],
      exports: 'jQuery.fn.tooltip'
    },
    'bootstrap/alert': {
      deps: ['jquery'],
      exports: 'jQuery.fn.alert'
    },
    'bootstrap/button': {
      deps: ['jquery'],
      exports: 'jQuery.fn.button'
    },
    'bootstrap/timepicker': {
      deps: ['jquery', 'jquery/autotype', 'bootstrap/dropdown'],
      exports: 'jQuery.fn.timepicker'
    },
    'mousetrap': {
      deps: [],
      exports: 'Mousetrap'
    },
    'chai-jquery': {
      deps: ['jquery', 'chai']
    },
    'chai-backbone': {
      deps: ['backbone', 'chai']
    },
    'backbone/validations': {
      deps: ['backbone', 'underscore'],
      exports: 'Backbone.Validations.Model'
    }
  }
});

require(['styles'], function(){});

require([
  'jquery',
  'underscore',
  'backbone',
  'routers/application',
  'collections/jobs',
  'collections/details',
  'collections/results',
  'collections/job_graph',
  'parsers/iso8601',
  'components/configured_rivets',
  'jquery/fastLiveFilter'
],
function($,
         _,
         Backbone,
         ApplicationRouter,
         JobsCollection,
         DetailsCollection,
         ResultsCollection,
         JobGraphCollection) {

  'use strict';

  var app = window.app = {
    Models: {},
    Collections: {},
    Views: {},
    Routers: {},

    Helpers: {
      filterList: function() {
        var $jobs = $('.result-jobs-count');

        $('#search-filter').fastLiveFilter('#job-list' , {
          callback: function(total, results) {
            $jobs.html(total);
            app.resultsCollection.trigger('change');
            if (!results) { return false; }
          }
        });
      },

      makePath: function() {
        var hash = window.location.hash,
            parts = hash.split('/') ;

        if (hash) {
          parts = parts.join('/') + '/';
          return parts;
        } else {
          return 'jobs/';
        }
      }
    },

    init: function() {
      var jobsCollection = new JobsCollection();

      jobsCollection.fetch({remove: false}).done(function() {
        jobsCollection.each(function(job) {
          job.set({persisted: true}, {silent: true});
        });

        window.app.detailsCollection = new DetailsCollection();
        window.app.resultsCollection = new ResultsCollection(jobsCollection.models);
        window.app.jobsGraphCollection = new JobGraphCollection();

        window.app.jobsGraphCollection.registerAccessoryCollection(
          jobsCollection).fetch();

        window.app.router = new ApplicationRouter();
        Backbone.history.start();
        app.Helpers.filterList();

        jobsCollection.startPolling();
        window.app.jobsGraphCollection.startPolling();
      });

      window.app.jobsCollection = jobsCollection;
    }
  };

  $(document).ready(function() {
    app.init();
  });

});
