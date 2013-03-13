.SUFFIXES: .js .css

NPM=npm
JS_VENDOR=app/scripts/vendor/tests
CSS_VENDOR=app/scripts/styles/vendor/tests
JS_MODULE=node_modules
REQUIREJS=app/scripts/require.js
PARSER_PATH=app/scripts/parsers
LODASH_OPTIONS_PLUS=random template uniq times object omit range memoize flatten merge compact isNull isUndefined isNumber pluck
LODASH_OPTIONS_SETTINGS='{interpolate : /{{([\s\S]+?)}}/g}'
SPACE :=
SPACE +=
COMMA :=,
LODASH_CMD := lodash backbone plus=$(subst $(SPACE),$(COMMA),$(LODASH_OPTIONS_PLUS)) settings=$(LODASH_OPTIONS_SETTINGS)

$(REQUIREJS): $(JS_MODULE)/requirejs/require.js
	$(NPM) install
	mkdir -p app/scripts
	cp -n $(JS_MODULE)/requirejs/require.js app/scripts/require.js

TEST_JS   = chai chai-jquery chai-backbone mocha
TEST_CSS  = mocha

JS_SOURCE  := $(foreach file,$(TEST_JS),$(JS_MODULE)/$(file)/$(file).js)
CSS_SOURCE := $(foreach file,$(TEST_JS),$(JS_MODULE)/$(file)/$(file).js)
JS_DEST   := $(foreach file,$(TEST_JS),$(JS_VENDOR)/$(file).js)
CSS_DEST  := $(foreach file,$(TEST_CSS),$(CSS_VENDOR)/$(file).css)

$(JS_VENDOR):
	mkdir $(JS_VENDOR)

$(CSS_VENDOR):
	mkdir $(CSS_VENDOR)

$(JS_VENDOR)/%.js: $(JS_VENDOR)
	cp $(JS_MODULE)/$*/$*.js $@

$(CSS_VENDOR)/%.css: $(CSS_VENDOR)
	cp $(JS_MODULE)/$*/$*.css $@

$(PARSER_PATH):
	mkdir $(PARSER_PATH)

grammars/%.pegjs: $(PARSER_PATH)
	pegjs --cache $@ $(PARSER_PATH)/$*.js

$(PARSER_PATH)/%.js: grammars/%.pegjs
	./grammars/amdify.bash $@

peg-clean: $(PARSER_PATH)
	rm $(PARSER_PATH)/*.js || true

peg: $(PARSER_PATH)/iso8601.js

test: $(JS_DEST) $(CSS_DEST)

app: $(REQUIREJS) volo
	volo add
	
all: test app

lodash:
	${LODASH_CMD}
	mv lodash.custom.js app/scripts/vendor/lodash.js
	rm lodash.custom.min.js

.PHONY: $(REQUIREJS) test app all make_test_dirs peg peg-clean grammars/%.pegjs lodash
