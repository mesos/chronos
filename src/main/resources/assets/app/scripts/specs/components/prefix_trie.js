define([
  'underscore',
  'mocha',
  'chai',
  'components/prefix_trie'
],
function(_,
         _mocha,
         Chai,
         PrefixTrie) {

  var expect, x;
  x = expect = Chai.expect;

  describe('A prefix trie', function() {
    var root, testWords;

    testWords = [
      'hive',
      'hello',
      'zulu'
    ];

    beforeEach(function() {
      root = new PrefixTrie();
      testWords.forEach(root.addWord, root);
    });

    it('should have the words that are added to it', function() {
      var result = root.getAllWords();

      testWords.forEach(function(w) {
        expect(result).to.include(w);
      });
    });

    it('should know which words are its own', function() {
      var result = root.getWords();
    });

    it('should allow more words to be added', function() {
      expect(root.addWord('word')).to.be.eql(root);
      expect(root.getAllWordsCount()).to.be.eql(4);
    });

    it('should not allow words to be duplicated', function() {
      expect(root.addWord(testWords[0])).to.be.eql(root);
      expect(root.getAllWordsCount()).to.be.eql(3);
    });

    it('should be able to find words', function() {
      //debugger
      var results = root.getSimilar(testWords[0], 0);

      expect(results).to.have.lengthOf(1);
      expect(results[0]).to.include.key('word');
      expect(results[0].word).to.be.eql(testWords[0]);
      //expect(results[0]).to.be.eql(testWords[0]);
    });

    it('should be able to "fuzz" similar words', function() {
      //root.addWord('heft');
      expect(root.getAllWordsCount()).to.be.eql(3);
      var results = root.getSimilar('hilt', 2);

      expect(results).to.have.lengthOf(2, 'hilt should have two matches');
    });

    it('should be able to locate similarities when fuzzing "have"', function() {
      root.addWord('give');
      expect(root.getAllWordsCount()).to.be.eql(4);

      var results = root.getSimilar('have', 3),
          ranges  = {
            'hive': [
              [0, 1],
              [2, 4]
            ],
            'hello': [
              [0, 1]
            ],
            'give': [
              [2, 4]
            ]
          };

      function verify(w, o, pos, val) {
        expect(o).to.include.key(pos);
        expect(o[pos]).to.be.eql(val, [
          'Matches for', w, 'should have value', val, 'at position', pos
        ].join(' '));
      }

      function verifyRange(w, o, _start, _end) {
        verify(w, o, 'start', _start);
        verify(w, o, 'end', _end);
      }

      expect(results).to.have.lengthOf(3, 'there should be 3 results');

      results.forEach(function(result) {
        expect(ranges).to.be.ok();
        expect(result).to.be.ok();
        expect(result).to.include.key('matches');

        var word = result.word,
            matches = result.matches,
            solution;

        expect(ranges).to.include.key(word);

        solution = ranges[word];

        expect(matches).to.have.lengthOf(solution.length, [
          word, 'should have', solution.length, 'matches'
        ].join(' '));

        matches.forEach(function(match, i) {
          verifyRange(word, match, solution[i][0], solution[i][1]);
        }, this);
      });
    });
  });
});
