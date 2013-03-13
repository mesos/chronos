define([
  'underscore',
  'backbone'
],
function(_,
         Backbone) {

  function isNumber (o) {
    return !isNaN(o - 0) &&
      (o != null) &&
      !o.length &&
      (o !== '');
  }

  function Range(start, end) {
    if (!(this instanceof Range)) { return new Range(start, end); }

    this.start = start;
    this.end   = end;
  }

  _.extend(Range.prototype, {
    clone: function() {
      return new Range(this.start, this.end);
    },
    has: function (prop) {
      return isNumber(this[prop]);
    },
    isValid: function() {
      return this.has('start') && this.has('end');
    },
    set: function(prop, val) {
      if (isNumber(val)) {
        this[prop] = val;
      }
      return this;
    }
  });

  function RunCounter() {
    if (!(this instanceof RunCounter)) { return new RunCounter(); }

    this.currentRange = null;
    this.lastValue = null;
    this.ranges    = [];
    this.index     = -1;
  }

  _.extend(RunCounter.prototype, {
    clone: function() {
      var c = new RunCounter();
      _.extend(c, {
        currentRange: this.currentRange && this.currentRange.clone(),
        lastValue: this.lastValue,
        ranges: _.clone(this.ranges),
        index: this.index
      });
      if (this.currentRange && (this.currentRange.has('start') || this.currentRange.has('end'))) {
        //debugger
      }

      return c;
    },
    increment: function(val) {
      var valChanged = (this.lastValue !== val);
      this.index += 1;

      if (valChanged) {
        if (this.currentRange && this.currentRange.isValid()) {
          this.ranges.push(this.currentRange);
          this.currentRange = new Range();
        }
      }

      this.lastValue = val;
      return valChanged;
    },
    run: function() {
      this.increment('run');

      if (!this.currentRange) {
        this.currentRange = new Range(this.index)
      } else if (!this.currentRange.has('start')) {
        this.currentRange.set('start', this.index);
      }
    },
    skip: function() {
      //debugger
      var valChanged = this.increment('skip');

      if (this.currentRange &&
          this.currentRange.has('start') &&
          !this.currentRange.has('end')) {
        this.currentRange.set('end', this.index);
      }
    },
    getRuns: function() {
      var i = this.index,
          lVal = this.lastValue,
          runs;

      this.skip();
      this.increment('getRuns');
      runs = _.map(this.ranges, function(range) {
        return {start: range.start, end: range.end};
      });

      this.index = i;
      this.lastValue = lVal;

      return runs;
    },
    lastWas: function(val) {
      return this.lastValue === val;
    }
  });

  return RunCounter;
});
