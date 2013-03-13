define([
  'underscore',
  'components/functor'
], function(_,
            Functor) {

  function PresenceMap(list) {
    if (!_.isArray(list)) {
      return {};
    }

    return _.object(list, _.times(list.length, Functor(true)));
  }

  return PresenceMap;
});

