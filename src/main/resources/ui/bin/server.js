#!/usr/bin/env node
var path = require('path');
var fs = require('fs');

var babelrc = JSON.parse(fs.readFileSync('./.babelrc'));
require('babel-register')(babelrc);

global.__CLIENT__ = false;
global.__SERVER__ = true;

require('../server');
