define(['handlebars'], function (Handlebars) {
  function boolDisabled(context) {
    return !!context ? 'Disabled' : 'Enabled';
  }
  Handlebars.registerHelper('boolDisabled', boolDisabled);
  return boolDisabled;
});
