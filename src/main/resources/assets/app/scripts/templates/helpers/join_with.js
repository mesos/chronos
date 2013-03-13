define(['handlebars'], function (Handlebars) {
  function joinWith(context, sep) {
    if (_.isArray(context) && _.isString(sep)) {
      return context.join(sep);
    } else {
      return '';
    }
  }
  Handlebars.registerHelper('joinWith', joinWith);
  return joinWith;
});
