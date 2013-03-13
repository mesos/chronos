define([
  'underscore'
],
function(_) {
  function Stack(vals) {
    if (!(this instanceof Stack)) { return new Stack(vals); }

    this._stack = [];
    this.pushAll(vals);
  }

  _.extend(Stack.prototype, {
    push: function(val) {
      if (!!val || val === 0) {
        return this._stack.push(val);
      }
    },
    pop: function() {
      return this._stack.pop();
    }
  });

  function ParserState() {
    if (!(this instanceof ParserState)) { return new ParserState(); }

    this.balances = {};
    this.stacks = {};
  }

  function addBalance(balances, ruleName, token, val) {
    balances[ruleName] || (balances[ruleName] = {});
    balances[ruleName][ruleName + token] = (token === val);
  }

  function removeBalance(balances, ruleName, token) {
    var r = balances[ruleName],
        r2 = r && r[ruleName + token];
    delete r[ruleName + token];
    return r && r2;
  }

  _.extend(ParserState.prototype, {
    /*
     * startBalance: Tracks the presence of balanced pairs of tokens in rules.
     *
     * @returns False so that it can be returned from predicates. Ex:
     *  !{ return parserState.startBalance('ruleName', ':', t1); }
     */
    startBalance: function(ruleName, token, startValue) {
      addBalance(this.balances, ruleName, token, startValue);
      return false;
    },

    hasBalance: function(ruleName, token) {
      return !!(this.balances &&
        this.balances[ruleName] &&
        this.balances[ruleName][ruleName + token]);
    },

    /**
     *
     * getBalance: Gets the presence of a balanced pair of tokens, based upon
     *             the supplied input and last state added by
     *             {@link ParserState#startBalance}.
     *
     * @returns Boolean indicating if the token is balanced in the statement
     *                  matched by the rule.
     */
    getBalance: function(ruleName, token, endValue) {
      var endHasToken = (token === endValue);
      if (removeBalance(this.balances, ruleName, token) === endHasToken) {
        return true;
      //} else {
        //throw new Error('Unbalanced token - ' + token);
      };
    },

    forceBalance: function(ruleName, token) {
      removeBalance(this.balances, ruleName, token);
      return true;
    }

  });

  return ParserState;
});
