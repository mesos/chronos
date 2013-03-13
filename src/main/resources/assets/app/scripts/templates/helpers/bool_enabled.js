define(['handlebars'], function (Handlebars) {
  function boolEnabled(context) {
    return !!context ? 'Enabled' : 'Disabled';
  }
  Handlebars.registerHelper('boolEnabled', boolEnabled);
  return boolEnabled;
});
