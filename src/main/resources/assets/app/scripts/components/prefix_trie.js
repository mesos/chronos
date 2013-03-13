define(['underscore', 'components/run_counter'],
       function(_, RunCounter) {

  var slice  = Array.prototype.slice,
      concat = Array.prototype.concat,
      bind   = _.bind,
      PrefixTrieNode;

  function listToResult(list, matches) {
    return _.map(list, function(word) {
      return {
        word: word,
        matches: matches
      };
    });
  }

  PrefixTrieNode = function() {
    this.initialize.apply(this, slice.call(arguments));
  };

  _.extend(PrefixTrieNode.prototype, {
    initialize: function(value) {
      this.children = {};
      this.value    = (value || '').toLocaleLowerCase();
      this.words    = {};
    },
    addWord: function(word) {
      if (!!word) {
        this._addWord(word, word);
      }
      return this;
    },
    _addWord: function(word, wholeWord) {
      var firstLetter, child, restLetters, restWord;

      firstLetter = _.first(slice.call(word, 0, 1))
      restLetters = slice.call(word, 1)
      restWord    = restLetters.join('');

      if (!firstLetter) {
        if (arguments.length == 2) {
          // Recursive call result, add _wholeWord.
          var downcased = wholeWord.toLocaleLowerCase();
          this.words[downcased] = wholeWord;
        }
        return;
      } else {
        firstLetter = firstLetter.toLocaleLowerCase();
      }

      if (_.has(this.children, firstLetter)) {
        child = this.getChild(firstLetter);
      } else {
        child = this.children[firstLetter] = new PrefixTrieNode(firstLetter);
      }

      child._addWord(restWord, wholeWord);
    },
    addWords: function(wordList) {
      _.each(wordList, function(word) {
        this.addWord(word);
      }, this);
    },
    removeWord: function(word) {
      if (!!word) {
        this._removeWord(word, word);
      }
      return this;
    },
    _removeWord: function(word, wholeWord) {
      var firstLetter, restLetters, child, hasProperChild, childRetVal;

      firstLetter    = _.first(slice.call(word, 0, 1));
      restLetters    = slice.call(word, 1);
      hasProperChild = _.has(this.children, firstLetter);

      if (!firstLetter) {
        var downcased, hasProperWord;

        downcased     = wholeWord.toLocaleLowerCase();
        hasProperWord = _.has(this.words, downcased);

        delete this.words[downcased];
        return hasProperWord;
      } else if (!hasProperChild) {
        return false;
      }

      child = this.getChild(firstLetter);
      childRetVal = child._removeWord;

      if (childRetVal && (child.getOwnWordsCount() <= 0)) {
        child.markedForRemoval = true;
      }

      return childRetVal;
    },

    getChild: function(letter) {
      var l = letter || '';
      return this.children[l.toLocaleLowerCase()];
    },

    getChildCount: function() {
      return _.size(this.children);
    },

    getCasedWords: function() {
      return _.keys(this.words);
    },

    getWords: function() {
      return _.values(this.words);
    },

    getAllWordsMap: function() {
      var words = _.map(this.children, function(node, letter) {
        return node.getAllWordsMap();
      });

      return _.extend.apply(null, ([{}, this.words]).concat(words));
    },

    getAllWords: function() {
      return _.values(this.getAllWordsMap());
    },

    getOwnWordsCount: function() {
      return _.size(this.words);
    },

    getAllWordsCount: function() {
      return _.size(this.getAllWordsMap());
    },

    getValue: function() {
      return this.value;
    },

    getSimilar: function(q, maxDistance) {
      maxDistance || (maxDistance = 0);
      var similar = this.getSimilarWithin(q, q, maxDistance, 0, [], new RunCounter());

      var uniq = _.chain(similar).groupBy(function(r) {
        return r.word;
      }).map(function(words, word) {
        return _.max(words, function(r) {
          if (!r.matches) { return -Infinity; }
          r.matchScore = _.reduce(r.matches, function(memo, match) {
            return memo + (match.end - match.start);
          }, 0);
          return r.matchScore;
        });
      }).value();

      return uniq;
    },

    containsWord: function(w) {
      return !!this.words[w.toLocaleLowerCase()];
    },

    containsWithin: function(q2, maxDistance) {
      function _containsWithin(q1, currentDistance) {
      }
    },

    getSimilarInChildren: function(q, maxDistance, currentDistance) {
    },

    /*
     * q1 - Current query val
     * q2 - Absolute query val
     * maxDistance - maximum edit distance
     * currentDistance - current edit distance
     * results - current matches
     * matches - range of current matches
     */
    getSimilarWithin: function(q1, q2, maxDistance, currentDistance, results, runs) {
      if (this.containsWord(q2)) {
        return results.concat([{
          word: q2,
          matches: runs.getRuns()
        }]).concat(listToResult(this.getAllWords(), runs.getRuns()));
      } else if (currentDistance > maxDistance) {
        return _.clone(results);
      }

      var firstLetter   = _.first(q1),
          restLetters   = _.rest(q1),
          restWord      = restLetters.join(''),
          usePrefix     = true,
          lettersPassed = q2.length - q1.length,
          lettersEqual  = (firstLetter &&
            (this.getValue() === firstLetter.toLocaleLowerCase())),
          shouldFuzzForChildren = ((this.getChildCount() === 0) &&
            (currentDistance < maxDistance)),
          shouldFuzz = shouldFuzzForChildren;

      if (!firstLetter || shouldFuzz) {
        // Out of letters to match by or fuzzing.
        var words = usePrefix ? this.getAllWords() : this.getWords();
        if (lettersEqual) {
          runs.run();
        }

        return listToResult(words, runs.getRuns()).concat(results);
      //} else if (currentDistance === maxDistance) {
      } else {
        if (lettersEqual) {
          runs.run();
        } else {
          if (this.getValue() && this.getValue().length > 0) {
            //currentDistance += 1;
            runs.skip();
          }
        }

        var childResults = _.reduce(this.children, (function(runs) {
          return function(memo, childNode) {
            var similar2 = [],
                similar,
                args1, args2;

            args1 = [
              q1, q2, maxDistance, currentDistance, _.clone(results),
              runs.clone()
            ];

            if (runs.lastWas('skip')) {
              _.extend(args1, {
                '3': currentDistance + 1
              });
              args2 = _.extend([], args1, {
                '0': restWord,
                '5': runs.clone()
              });
            } else if (runs.lastWas('run')) {
              args1[0] = restWord;
            } else if (runs.lastWas(null)) {
            }

            similar = childNode.getSimilarWithin.apply(childNode, args1);
            if (!!args2) {
              similar2 = childNode.getSimilarWithin.apply(childNode, args2);
            }

            return memo.concat(similar, similar2);
          };
        })(runs.clone()), []);

        return childResults.concat(results);
      }

      //return concat.apply(results, _results);
    }
  });

  return PrefixTrieNode;
});

