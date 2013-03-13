define([
  'handlebars',
  'underscore'
],
function(Handlebars, _) {

  function orNone(context) {
    if (!context || _.isEmpty(context)) {
      return 'None';
    } else {
      return context;
    }
  }

  Handlebars.registerHelper('orNone', orNone);
  return orNone;
});
