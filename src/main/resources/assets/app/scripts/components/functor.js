define([], function() {
  'use strict';

  function Functor(v) { return function() { return v; }; }

  return Functor;
});
