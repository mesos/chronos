define([
  'underscore',
  'd3'
],
function(_,
         d3) {

  function shadowedText(selection, options) {
    var opts, $el, classes;
    opts = _.merge({
      attributes: {}
    }, (options || {}));

    classes = opts.attributes['class'] || '';
    opts.attributes = _.omit(opts.attributes, 'class');

    _.each(['shadow', ''], function(className) {
      selection.append('svg:text').text(opts.text || _.identity).
        attr('class', [classes, className].join(' ')).
        each(function(d, i) {
          $el = d3.select(this);
          _.each(opts.attributes, function(v, k) { $el.attr(k, v); });
        });
    });
  }

  return shadowedText;
});
