$(function(){
	$('.accordion').accordion({
		heightStyle: 'content',
		collapsible : true
	});

	// ***************************
	//  ARRAY
	// ***************************
	var arry = [
	[ 'ID', 'Name', 'Age', 'Date' ], // header
	[ 'A42b', 'Parker', 28, 'Jul 6, 2006 8:14 AM' ],  // row 1
	[ 'A255', 'Hood', 33, 'Dec 10, 2002 5:14 AM' ],   // row 2
	[ 'A33', 'Kent', 18, 'Jan 12, 2003 11:14 AM' ],   // row 3
	[ 'A1', 'Franklin', 45, 'Jan 18, 2001 9:12 AM' ], // row 4
	[ 'A102', 'Evans', 22, 'Jan 18, 2007 9:12 AM' ],  // row 5
	[ 'A42a', 'Everet', 22, 'Jan 18, 2007 9:12 AM' ], // row 6
	[ 'ID', 'Name', 'Age', 'Date' ]  // footer
	];

	$('#array2Table').tablesorter({
		theme: 'blue',
		data : arry,
		widgetOptions : {
			// build_type   : 'array',  // can be detected if undefined
			// build_source : arry,    // overrides the data setting above
			build_headers   : {
				widths : ['15%', '30%', '15%', '40%'] // set header cell widths
			},
			build_footers : {
				text : [ 'ID (a###)', 'Last Name', 'Age (joined)', 'Date (joined)' ]
			}
		}
	});

	// ***************************
	//  ARRAY (from string)
	// ***************************
	$('#string2Table').tablesorter({
		theme: 'blue',
		widgetOptions: {
			build_type   : 'array',
			build_source : 'header 1,header 2,header 3;r1c1,r1c2,r1c3;r2c1,r2c2,r2c3;r3c1,r3c2,r3c3;"footer, 1","footer, 2","footer, 3"',
			build_processing : function(data, wo) {
				var rows = data.split(';');
				return $.each(rows, function(i,cells) {
					// similar to using rows[i] = cells.split(',') but the splitCSV script
					// doesn't split the cell if the separator (comma) is within quotes 
					rows[i] = $.tablesorter.buildTable.splitCSV(cells, ',');
				});
			}
		}
	});

	// ***************************
	//  CSV (DOM)
	// ***************************
	$('#csv2Table').tablesorter({
		theme: 'blue',
		widgetOptions: {
			// *** build widget core ***
			build_type      : 'csv',     // array, object, csv, ajax
			build_source    : $('.csv'), // url, dom
			build_complete  : 'tablesorter-build-complete', // triggered event when build completes

			// *** CSV & array ***
			build_headers   : {
				rows    : 1,   // Number of header rows from the csv
				classes : [],  // Header classes to apply to cells
				text    : [],  // Header cell text
				widths  : ['3%', '27%', '50%', '20%'] // set header cell widths
			},
			build_footers : {
				rows    : 1,   // Number of header rows from the csv
				classes : [],  // Footer classes to apply to cells
				text    : []   // Footer cell text
			},
			build_numbers : {
				addColumn : '#', // include row numbering column?
				sortable  : true // make column sortable?
			},

			// *** CSV options ***
			build_csvStartLine : 0, // line within the csv to start adding to table
			build_csvSeparator : ",", // csv separator
		}
	});

	// ***************************
	//  CSV (Ajax)
	// ***************************
	$('#csv2Table2').tablesorter({
		theme: 'blue',
		widgetOptions: {
			// *** build widget core ***
			build_type      : 'csv',
			build_source    : { url: 'assets/build.txt', dataType: 'html' },
			build_headers   : {
				widths : ['30%', '50%', '20%'] // set header cell widths
			}
		}
	});

	// ***************************
	//  OBJECT (Variable)
	// ***************************
	var dataObject = {
		headers : [
			[
				// each object/string is a cell
				{ text: 'First Name', class: 'fname', width: '10%' }, // row 0 cell 1
				'Last Name',
				{ text: 'Age', class: 'age', 'data-sorter' : false }, // row 0 cell 3
				'Total',
				{ text: 'Discount', class : 'sorter-false' },         // row 0 cell 5
				{ text: 'Date', class : 'date' }                      // row 0 cell 6
			]
		],
		footers : 'clone', // clone headers or assign array like headers
		rows : [
			// TBODY 1
			[ 'Peter', 'Parker',   28, '$9.99',   '20%', 'Jul 6, 2006 8:14 AM'   ], // row 1
			[ 'John',  'Hood',     33, '$19.99',  '25%', 'Dec 10, 2002 5:14 AM'  ], // row 2
			[ 'Clark', 'Kent',     18, '$15.89',  '44%', 'Jan 12, 2003 11:14 AM' ], // row 3

			// TBODY 2
			{ newTbody: true, class: 'tablesorter-infoOnly' },
			{ cells : [ { html: '<strong>Info Row</strong>', colSpan: 6 } ] },      // row 4

			// TBODY 3
			{ newTbody: true },
			[ 'Bruce', 'Evans',    22, '$13.19',  '11%', 'Jan 18, 2007 9:12 AM'  ], // row 5
			[ 'Brice', 'Almighty', 45, '$153.19', '44%', 'Jan 18, 2001 9:12 AM'  ], // row 6

			{ class: 'specialRow', // row 7
				cells: [
					// each object/string is a cell
					{ text: 'Fred', class: 'fname' },
					{ text: 'Smith', class: 'lname' },
					{ text: 18, class: 'age', 'data-info': 'fake ID!, he is really 16' },
					{ text: '$22.44', class: 'total' },
					'8%',
					{ text: 'Aug 20, 2012 10:15 AM', class: 'date' }
				],
				'data-info' : 'This row likes turtles'
			}
		]
	};

	$('#object2Table').tablesorter({
		theme: 'blue',
		data : dataObject,
		widgetOptions : {
			// *** build object options ***
			build_objectRowKey    : 'rows',    // object key containing table rows
			build_objectCellKey   : 'cells',   // object key containing table cells (within the rows object)
			build_objectHeaderKey : 'headers', // object key containing table headers
			build_objectFooterKey : 'footers'  // object key containing table footers
		}
	});

	// ***************************
	//  OBJECT (JSON via Ajax)
	// ***************************
	$('#object2Table2').tablesorter({
		theme: 'blue',
		widgetOptions: {
			build_type   : 'json',
			build_source : { url: 'assets/build.json', dataType: 'json' }
		}
	});

});