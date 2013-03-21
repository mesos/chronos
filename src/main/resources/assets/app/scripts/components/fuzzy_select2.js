define([
  'jquery',
  'underscore',
  'components/fuzzy_matcher',
  'hbs!templates/select2_choice',
  'components/presence_map',
  'jquery/select2',
  'less!styles/select2.less'
],
function($,
         _,
         FuzzyJobMatcher,
         Select2ChoiceTpl,
         PresenceMap) {
  'use strict';

  var methods = {},
      nsKey   = 'airbnb',
      dataKey = 'airbnb.fuzzySelect2',
      attachedKey = 'airbnb.fuzzySelect2.attached',
      select2Key  = 'select2',
      ListSeparator = / *, */;

  // Namespaces events using key.
  function evt(n) {
    return [n, nsKey].join('.');
  }

  function formatResult(results, context, exclusions) {
    results || (results = []);
    context || (context = {});

    return _.extend({}, {
      results: _.reject(results, function(result) {
        var rText = result.text,
            rId   = result.id;
        return !!(exclusions[rText] || exclusions[rId]);
      }),
      more:    false,
      context: context
    });
  };

  function AttachSelect2($el, opts) {
    var matcher,
        formattedMatcher,
        options,
        exclusions,
        exclusionsMap,
        s2;

    if (!!$el.data(attachedKey)) { return null; }
    else { $el.data(attachedKey, true); }

    matcher = new FuzzyJobMatcher.FuzzyMatcher({
      collection: opts.collection
    });

    formattedMatcher = FuzzyJobMatcher.getFormattedMatcher(3, matcher);
    exclusions = (opts && opts.exclusions) || [];
    exclusionsMap = PresenceMap(exclusions);

    options = _.extend({}, {
      tokenSeparators: [',', ' ', ', '],
      query: function(options) {
        /*
         * options ~= {
         *   term: 'SEARCH TERM',
         *   context: {search state},
         *   callback: function(result){}
         * }
         * options.callback.result ~= {
         *   results: [{id, text}],
         *   more:    Boolean,
         *   context: {search state}
         * }
         */
        var _results   = formattedMatcher(options.term),
            s2Data     = $el.select2('data'),
            selectionExclusions = PresenceMap(_.pluck(s2Data, 'id')),
            exclusions = _.extend({}, exclusionsMap, selectionExclusions);

        options.callback(formatResult(_results, options.context, exclusions));
      },

      createSearchChoice: function (term) {
        return null;
      },

      formatSelection: function(data, $container) {
        return Select2ChoiceTpl({data: data});
      },

      escapeMarkup: function(m) { return m; },

      dropdownCssClass: 'fuzzy-select2-dropdown',

      tokenizer: function(input, selection, selectCallback, opts) {
      },

      formatResult: function(result, container, query) {
        var ranges, parts, start, text, term, reduction;

        text   = result.text;
        term   = query.term;
        ranges = result.matches;
        parts  = [];

        if (_.isEmpty(ranges)) {
          if (!_.isEmpty(text)) {
            return text;
          } else {
            return null;
          }
        }

        start  = _.chain(ranges).first().first().value();

        parts.push(text.slice(0, start));

        reduction = _.reduce(ranges, function(memo, range, i, list) {
          var nextIndex = i + 1,
              results;

          results = [
            '<span class="select2-match">',
            text.slice(range[0], range[1]),
            '</span>'
          ];

          if ((nextIndex < list.length) && (range[1] !== list[nextIndex][0]))  {
            results.push(text.slice(range[1], list[nextIndex][0]));
          } else if (nextIndex == list.length) {
            results.push(text.slice(range[1]));
          }

          return memo.concat(results);
        }, []);

        return parts.concat(reduction).join('');
      },

      initSelection: function(element, callback) {
        var selections, parentMap, results;

        selections = element.val().split(ListSeparator);
        parentMap  = PresenceMap(selections);

        _.extend(exclusionsMap, parentMap);

        results    = matcher.collection.chain().filter(function(job) {
          return !!parentMap[job.get('name')];
        }).map(function(job) {
          return {
            text: job.get('name'),
            id:   job.get('name')
          };
        }).value()

        callback(results);
      }

    }, _.omit(opts, 'collection', 'exclusions'));

    $el.data(dataKey, {
      destroy: function() {
        matcher.unobserveCollection();
      }
    });

    function changed(e) {
      if (e && e.added) {
        exclusionsMap[e.added.id] = true;
      } else if (e && e.removed) {
        exclusionsMap[e.removed.id] = null;
        delete exclusionsMap[e.removed.id];
      }
    }

    return (s2 = $el.select2(options).on(evt('change'), changed));
  };

  function UnattachSelect2($el) {
    var fuzzySelect2;

    if (!!(fuzzySelect2 = $el.data(dataKey))) {
      $el.removeData(attachedKey).
          removeData(dataKey).
          off(evt(''));

      fuzzySelect2.destroy();
    }

    return $el.select2('destroy');
  };

  _.extend(methods, {
    attach: AttachSelect2,
    unattach: UnattachSelect2
  });

  return methods;
});
