'use strict';
//This file is used by Docker during execution.
//His job is to replace constants by environments variables.

let fs = require('fs');

let fileName = 'bundle.js';

let file = fs.readFileSync(process.env.HTML_FOLDER + '/assets/js/' + fileName, 'utf8');

let apiUrl = process.env.CHRONOS_API_URL ? process.env.CHRONOS_API_URL : "";

file = file.replace(/%CHRONOS_API_URL%/g, apiUrl);

fs.writeFileSync(process.env.HTML_FOLDER + '/assets/js/' + fileName, file, 'utf8');
