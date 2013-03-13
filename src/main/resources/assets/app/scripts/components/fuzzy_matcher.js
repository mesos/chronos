define(['underscore', 'components/prefix_trie'],
       function(_, PrefixTrieNode) {

  var slice  = Array.prototype.slice,
      concat = Array.prototype.concat,
      bind   = _.bind,
      matcher,
      FuzzyMatcher;

  FuzzyMatcher = function() {
    this.initialize.apply(this, slice.call(arguments));
  };

  _.extend(FuzzyMatcher.prototype, {
    initialize: function(options) {
      var opts = _.defaults(options, {
        wordAttribute: 'name'
      });

      _.extend(this, {
        collection    : opts.collection,
        wordAttribute : opts.wordAttribute
      });

      this.buildTrie();
      this.observeCollection();
    },
    buildTrie: function() {
      this.rootNode = new PrefixTrieNode();
      this.collection.each(this.updateAddNode, this);
    },
    getChangeEvent: function() {
      return [
        'change',
        this.wordAttribute
      ].join(':');
    },
    observeCollection: function() {
      var changeEvent = this.getChangeEvent();

      this.collection.
        on('reset', this.buildTrie, this).
        on('add', this.updateAddNode, this).
        on('remove', this.updateRemoveNode, this).
        on(changeEvent, this.updateTrie, this);
    },
    updateTrie: function(model) {
      var oldWord, newWord;
      oldWord = model.previous(this.wordAttribute);
      newWord = model.get(this.wordAttribute);
      this.rootNode.
        addWord(newWord).
        removeWord(oldWord);
    },
    updateAddNode: function(model) {
      var newWord = model.get(this.wordAttribute);
      this.rootNode.addWord(newWord);
    },
    updateRemoveNode: function(model) {
      var oldWord = model.get(this.wordAttribute);
      this.rootNode.removeWord(oldWord);
    },
    off: function() {
      return this.unobserveCollection();
    },
    unobserveCollection: function() {
      var changeEvent = this.getChangeEvent();

      this.collection.
        off('reset', this.buildTrie, this).
        off('add', this.updateAddNode, this).
        off('remove', this.updateRemoveNode, this).
        off(changeEvent, this.updateTrie, this);
    },
    getMatches: function(q, maxDistance) {
      var maxDist = maxDistance || 1;
      return this.rootNode.getSimilar(q, maxDist);
    },
    getMatcher: function(maxDistance) {
      var _this = this;
      return function(q) {
        return _this.getMatches(q, maxDistance);
      };
    }
  });

  methods = {
    FuzzyMatcher: FuzzyMatcher,
    create: function(collection) {
      if (!matcher) {
        matcher = new FuzzyMatcher({
          collection: collection
        });
      }
    },
    getMatcher: function(maxEditDistance) {
      return matcher.getMatcher(maxEditDistance);
    },
    getFormattedMatcher: function(maxEditDistance, fuzzyMatcher) {
      var _matcher;

      if (!!fuzzyMatcher) {
        _matcher = fuzzyMatcher.getMatcher(maxEditDistance);
      } else {
        _matcher = methods.getMatcher(maxEditDistance);
      }

      return function(text) {
        var results = _matcher(text);
        return _.chain(results).map(function(result) {
          return {
            text: result.word,
            id:   result.word,
            matches: _.map(result.matches, function(match) {
              return [match.start, match.end];
            }),
            matchScore: -1 * result.matchScore
          }
        }).sortBy('matchScore').value();
      }
    },
    longestCommonSubranges: function(word, q, offset) {
      var text        = word.toLocaleLowerCase(),
          term        = q.toLocaleLowerCase(),
          termLetters = slice.call(term),
          startPos    = text.indexOf(term),
          popped      = [];

      offset || (offset = 0);

      while (startPos < 0 && !!termLetters.length) {
        popped.unshift(termLetters.pop());
        startPos = text.indexOf(termLetters.join(''))
      }

      if ((startPos >= 0) && (termLetters.length > 0)) {
        var endPos = startPos + termLetters.length,
            range  = [[startPos + offset, endPos + offset]];

        if (popped.length > 0) {
          var nextSubranges = methods.longestCommonSubranges(
            word.slice(endPos), popped.join(''), endPos);

          return range.concat(nextSubranges);
        } else {
          return range;
        }
      } else {
        return []
      }
    }
  };

  return methods;
});
