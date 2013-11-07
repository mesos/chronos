/*
 * custom pager controls - beta testing
 */
/*jshint browser:true, jquery:true, unused:false, loopfunc:true */
/*global jQuery: false, localStorage: false, navigator: false */

;(function($){
"use strict";

$.tablesorter = $.tablesorter || {};

$.tablesorter.customPagerControls = function(options) {
	var defaults = {
		table          : 'table',
		pager          : '.pager',
		pageSize       : '.left a',
		currentPage    : '.right a',
		ends           : 2,                        // number of pages to show of either end
		aroundCurrent  : 1,                        // number of pages surrounding the current page
		link           : '<a href="#">{page}</a>', // page element; use {page} to include the page number
		currentClass   : 'current',                // current page class name
		adjacentSpacer : ' | ',                    // spacer for page numbers next to each other
		distanceSpacer : ' &#133; ',               // spacer for page numbers away from each other (ellipsis)
		addKeyboard    : true                      // add left/right keyboard arrows to change current page
	},
	o = $.extend({}, defaults, options),
	$table = $(o.table);

	$table
		.on('pagerInitialized pagerComplete', function (e, c) {
			var i, pages = $('<div/>'), t = [],
			cur = c.page + 1,
			start = cur > 1 ? (c.totalPages - cur < o.aroundCurrent ? -(o.aroundCurrent + 1) + (c.totalPages - cur) : -o.aroundCurrent) : 0,
			end = cur < o.aroundCurrent + 1 ? o.aroundCurrent + 3 - cur : o.aroundCurrent + 1;
			for (i = start; i < end; i++) {
				if (cur + i >= 1 && cur + i < c.totalPages) { t.push( cur + i ); }
			}
			// include first and last pages (ends) in the pagination
			for (i = 0; i < o.ends; i++){
				if ($.inArray(i + 1, t) === -1) { t.push(i + 1); }
				if ($.inArray(c.totalPages - i, t) === -1) { t.push(c.totalPages - i); }
			}
			// sort the list
			t = t.sort(function(a, b){ return a - b; });
			// make links and spacers
			$.each(t, function(j, v){
				pages
					.append( $(o.link.replace(/\{page\}/g, v)).toggleClass(o.currentClass, v === cur).attr('data-page', v) )
					.append( '<span>' + (j < t.length - 1 && ( t[j+1] - 1 !== v ) ? o.distanceSpacer : ( j >= t.length - 1 ? '' : o.adjacentSpacer )) + '</span>' );
			});
			$('.pagecount').html(pages.html());
		});

	// set up pager controls
	$(o.pager).find(o.pageSize).on('click', function () {
		$(this)
		.addClass(o.currentClass)
		.siblings()
		.removeClass(o.currentClass);
		$table.trigger('pageSize', $(this).html());
		return false;
	}).end()
	.on('click', o.currentPage, function(){
		$(this)
		.addClass(o.currentClass)
		.siblings()
		.removeClass(o.currentClass);
		$table.trigger('pageSet', $(this).attr('data-page'));
		return false;
	});

	// make right/left arrow keys work
	if (o.addKeyboard) {
		$(document).on('keydown', function(e){
			// ignore arrows inside form elements
			if (/input|select|textarea/i.test(e.target.tagName)) { return; }
			if (e.which === 37) {
				// left
				$(o.pager).find(o.currentPage).filter('.' + o.currentClass).prevAll(':not(span):first').click();
			} else if (e.which === 39) {
				// right
				$(o.pager).find(o.currentPage).filter('.' + o.currentClass).nextAll(':not(span):first').click();
			}
		});
	}
};
})(jQuery);