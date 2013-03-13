#/usr/bin/env bash
AMD_START="define(['underscore', 'components\/date_node', 'components\/parser_utils'], function(_, DateNode, ParserState) { return "
AMD_END="});"

sed -i "" "s/module.exports = /$AMD_START/" $1
echo "$AMD_END" >> $1
