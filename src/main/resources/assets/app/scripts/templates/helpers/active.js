define(['handlebars'], function (Handlebars) {
  function active(context, sep) {
    if (context && context === sep) {
      return 'active';
    } else {
      return '';
    }
  }
  Handlebars.registerHelper('active', active);
  return active;
});
