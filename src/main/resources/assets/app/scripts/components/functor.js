define([], function() {
  function Functor(v) { return (function() { return v; }); }

  return Functor;
});
