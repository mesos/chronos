define([
  'handlebars',
],
function(Handlebars) {

  function boolHigh(context) {
    return !!context ? 'High' : 'Normal';
  }

  Handlebars.registerHelper('boolHigh', boolHigh);
  return boolHigh;
});
