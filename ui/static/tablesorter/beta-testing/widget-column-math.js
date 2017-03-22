/*! tablesorter column reorder - beta testing
* Requires tablesorter v2.8+ and jQuery 1.7+
* by Rob Garrison
*/
/*jshint browser:true, jquery:true, unused:false */
/*global jQuery: false */
;(function($){
	"use strict";

var ts = $.tablesorter = $.tablesorter || {};

	ts.columnMath = {

		equations : {
			// sum all the cells in the row
			sumrow   : function(table, $el, c, wo, direct){
				var total = 0, row = ts.columnMath.getRow(table, $el, direct);
				$.each( row, function(i){
					total += row[i];
				});
				return total;
			},
			// sum all the cells in the column
			sumcol   : function(table, $el, c, wo, direct){},
			// sum all of the cells
			sumall   : function(table, $el, c, wo, direct){},
			// sum all of the cells in the column above the current one, until the next sumabove is reached
			sumabove : function(table, $el, c, wo, direct){},
			// target cells in a specific column c1, c2, c3, etc
			col      : function(table, $el, c, wo, direct){},
			// target cells in a specific row r1, r2, r3, etc.
			row      : function(table, $el, c, wo, direct){}
			// target
		},

		// get all of the row numerical values in an array
		// el is the table cell where the data is added; direct means get the
		// numbers directly from the table, otherwise it gets it from the cache.
		getRow : function(table, $el, direct){
			var i, txt, arry = [],
				c = table.config,
				$tb = c.$table.find('tbody'),
				cIndex = $el[0].cellIndex,
				$row = $el.closest('tr'),
				bIndex = $tb.index( $row.closest('tbody') ),
				rIndex = $tb.eq(bIndex).find('tr').index( $row ),
				row = c.cache[bIndex].normalized[rIndex] || [];
			if (direct) {
				arry = $row.children().map(function(){
					txt = (c.supportsTextContent) ? this.textContent : $(this).text();
					txt = ts.formatFloat(txt.replace(/[^\w,. \-()]/g, ""), table);
					return isNaN(txt) ? 0 : txt;
				}).get();
			} else {
				for (i = 0; i < row.length - 1; i++) {
					arry.push( (i === cIndex || isNaN(row[i]) ) ? 0 : row[i] );
				}
			}
			return arry;
		},

		output : function($el, wo, value) {
			value = wo.columnMath_format.output_prefix + ts.columnMath.addCommas(value, wo) + wo.columnMath_format.output_suffix;
			if ($.isFunction(wo.columnMath_format.format_complete)) {
				value = wo.columnMath_format.format_complete(value, $el);
			}
			$el.html('<div class="align-decimal">' + value + '</div>');
		},

		addCommas : function(num, wo) {
			var parts = ( num.toFixed( wo.columnMath_format.decimal_places ) + '' ).split('.');
			parts[0] = parts[0].replace( wo.columnMath_regex, "$1" + wo.columnMath_format.thousands_separator );
			return parts.join( wo.columnMath_format.decimal_separator );
		},

		recalculate : function(table, c, wo){
			if (c) {
				wo.columnMath_regex = new RegExp('(\\d)(?=(\\d{' + (wo.columnMath_format.thousands_grouping || 3) + '})+(?!\\d))', 'g' );
				var n, t, $t,
					priority = [ 'sumrow', 'sumabove', 'sumcol', 'sumall' ],
					eq = ts.columnMath.equations,
					dat = 'data-' + (wo.columnMath_data || 'math'),
					$mathCells = c.$tbodies.find('[' + dat + ']');
					// cells with a target are calculated last
					$mathCells.not('[' + dat + '-target]').each(function(){
						$t = $(this);
						n = $t.attr(dat);
						// check for built in math
						// eq = n.match(/(\w+)[\s+]?\(/g);
						if (eq[n]) {
							t = eq[n](table, $t, c, wo);
							if (t) {
								ts.columnMath.output( $t, wo, t );
							}
						}
					});
					// console.log($mathCells);
			}
		}

	};

	// add new widget called repeatHeaders
	// ************************************
	$.tablesorter.addWidget({
		id: "column-math",
		options: {
			columnMath_data   : 'math',
			// columnMath_ignore : 'zero, text, empty',
			columnMath_format : {
				output_prefix       : '$ ',
				output_suffix       : '',
				thousands_separator : ',',
				thousands_grouping  : 3,
				decimal_separator   : '.',
				decimal_places      : 2,
				format_complete     : null // function(number, $cell){ return number; }
			}
		},
		init : function(table, thisWidget, c, wo){
			var $t = $(table).bind('update.tsmath updateRow.tsmath', function(){
				$.tablesorter.columnMath.recalculate(table, c, wo);
			});
			$.tablesorter.columnMath.recalculate(table, c, wo);
		},
		// format is called when the on init and when a sorting has finished
		format: function(table) {
			// do nothing
		},
		// this remove function is called when using the refreshWidgets method or when destroying the tablesorter plugin
		// this function only applies to tablesorter v2.4+
		remove: function(table, c, wo){
			$(table)
				.unbind('update.tsmath updateRows.tsmath')
				.find('[data-' + wo.columnMath_data + ']').empty();
		}
	});

})(jQuery);