define([
  'handlebars',
],
function(Handlebars) {

  function exists(val, options) {
      if (typeof val === 'undefined') {
        return options.inverse(this);
      } else {
        return options.fn(this);
      }
  }

  Handlebars.registerHelper('exists', exists);
  return exists;
});
