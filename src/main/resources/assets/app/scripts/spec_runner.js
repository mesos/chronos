require.config({
  paths: {
    'text'                  : 'vendor/text',
    'jquery'                : 'vendor/jquery',
    'lodash'                : 'vendor/lodash',
    'backbone'              : 'vendor/backbone',
    'backbone/declarative'  : 'vendor/backbone.declarative',
    'backbone/validations'  : 'vendor/backbone.validations',
    'backbone/mousetrap'    : 'vendor/backbone.mousetrap',
    'bootstrap/tooltip'     : 'vendor/bootstrap/js/bootstrap-tooltip',
    'bootstrap/button'      : 'vendor/bootstrap/js/bootstrap-button',
    'd3'                    : 'vendor/d3.v3',
    'underscore'            : 'vendor/lodash',
    'moment'                : 'vendor/moment',
    'backpack'              : 'vendor/backpack',
    'jquery/select2'        : 'vendor/select2',
    'jquery/pickadate'      : 'vendor/pickadate.min',
    'jquery/fastLiveFilter' : 'vendor/jquery.fastLiveFilter',
    'css'                   : 'vendor/require-css/css',
    'less'                  : 'vendor/require-less/less',
    'coffee-script'         : 'vendor/coffee-script',
    'cs'                    : 'vendor/cs',
    'propertyParser'        : 'vendor/requirejs-plugins/src/propertyParser',
    'font'                  : 'vendor/requirejs-plugins/src/font',
    'json'                  : 'vendor/requirejs-plugins/src/json',
    'mousetrap'             : 'vendor/mousetrap',
    'benchmark'             : 'vendor/benchmark',
    'mocha'                 : 'vendor/tests/mocha',
    'chai'                  : 'vendor/tests/chai',
    'chai-jquery'           : 'vendor/tests/chai-jquery',
    'chai-backbone'         : 'vendor/tests/chai-backbone'
  },
  map: {
    'css': {
      'normalize': 'vendor/require-css/normalize',
      'css-builder': 'vendor/require-css/css-builder'
    },
    'less': {
      'lessc': 'vendor/require-less/lessc'
    }
  },
  shim: {
    'jquery/select2': {
      deps: ['jquery'],
      exports: 'jQuery.fn.select2'
    },
    'benchmark': {
      deps: [],
      exports: 'Benchmark'
    },
    'jquery/fastLiveFilter': {
      deps: ['jquery'],
      exports: 'jQuery.fn.fastLiveFilter'
    },
    'jquery/pickadate': {
      deps: ['jquery'],
      exports: 'jQuery.fn.pickadate'
    },
    'bootstrap/tooltip': {
      deps: ['jquery'],
      exports: 'jQuery.fn.tooltip'
    },
    'bootstrap/button': {
      deps: ['jquery'],
      exports: 'jQuery.fn.button'
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
  /** Start Tests Only **/
  ,urlArgs: 'v='+(new Date()).getTime()
  /** End   Tests Only **/
});

require([
  'require',
  'mocha',
  'chai',
  'jquery',
  'chai-jquery',
  'chai-backbone',
  'css',
  'css!../styles/vendor/tests/mocha.css'
],
function(require, _mocha, chai, $, chai$, chaiBackbone) {

  // Chai
  var should = chai.should();
  chai.use(chai$);
  chai.Assertion.includeStack = true;

  mocha.setup('bdd');

  require([
    'specs/models/jobs',
    'specs/components/prefix_trie',
    'specs/parsers/iso8601'
  ], function(require) {
    mocha.run();
  });

});
