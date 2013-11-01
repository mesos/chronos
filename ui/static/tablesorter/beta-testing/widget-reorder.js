/*! tablesorter column reorder - beta testing
* Requires tablesorter v2.8+ and jQuery 1.7+
* by Rob Garrison
*/
/*jshint browser:true, jquery:true, unused:false */
/*global jQuery: false */
;(function($){
	"use strict";

$.tablesorter.addWidget({
	id: 'reorder',
	priority: 70,
	options : {
		reorder_axis        : 'xy', // x or xy
		reorder_delay       : 300,
		reorder_helperClass : 'tablesorter-reorder-helper',
		reorder_helperBar   : 'tablesorter-reorder-helper-bar',
		reorder_noReorder   : 'reorder-false',
		reorder_blocked     : 'reorder-block-left reorder-block-end',
		reorder_complete    : null // callback
	},
	init: function(table, thisWidget, c, wo) {
		var i, timer, $helper, $bar, clickOffset,
		lastIndx = -1,
		ts = $.tablesorter,
		endIndex = -1,
		startIndex = -1,
		t = wo.reorder_blocked.split(' '),
		noReorderLeft = t[0] || 'reorder-block-left',
		noReorderLast = t[1] || 'reorder-block-end',
		lastOffset = c.$headers.not('.' + noReorderLeft).first(),
		offsets = c.$headers.map(function(i){
			var s, $t = $(this);
			if ($t.hasClass(noReorderLeft)) {
				s = lastOffset;
				$t = s;
				//lastOffset = $t;
			}
			lastOffset = $t;
			return $t.offset().left;
		}).get(),
		len = offsets.length,
		startReorder = function(e, $th){
			var p = $th.position(),
			r = $th.parent().position(),
			i = startIndex = $th.index();
			clickOffset = [ e.pageX - p.left, e.pageY - r.top ];
			$helper = c.$table.clone();
			$helper.find('> thead > tr:first').children('[data-column!=' + i + ']').remove();
			$helper.find('thead tr:gt(0), caption, colgroup, tbody, tfoot').remove();
			$helper
			.css({
				position: 'absolute',
				zIndex : 1,
				left: p.left - clickOffset[0],
				top: r.top - clickOffset[1],
				width: $th.outerWidth()
			})
			.appendTo('body')
			.find('th, td').addClass(wo.reorder_helperClass);
			$bar = $('<div class="' + wo.reorder_helperBar + '" />')
			.css({
				position : 'absolute',
				top : c.$table.find('thead').offset().top,
				height : $th.closest('thead').outerHeight() + c.$table.find('tbody').height()
			})
			.appendTo('body');
			positionBar(e);
			lastIndx = endIndex;
		},
		positionBar = function(e){
			for (i = 0; i <= len; i++) {
				if ( i > 0 && e.pageX < offsets[i-1] + (offsets[i] - offsets[i-1])/2 && !c.$headers.eq(i).hasClass(noReorderLeft) ) {
					endIndex = i - 1;
					// endIndex = offsets.lastIndexOf( offsets[i-1] ); // lastIndexOf not supported by IE8 and older
					if (endIndex >= 0 && lastIndx === endIndex) { return false; }
					lastIndx = endIndex;
					if (c.debug) {
						ts.log( endIndex === 0 ? 'target before column 0' : endIndex === len ? 'target after last column' : 'target between columns ' + startIndex + ' and ' + endIndex);
					}
					$bar.css('left', offsets[i-1]);
					return false;
				}
			}
			if (endIndex < 0) {
				endIndex = len;
				$bar.css('left', offsets[len]);
			}
		},
		finishReorder = function(){
			$helper.remove();
			$bar.remove();
			// finish reorder
			var adj, s = startIndex,
			rows = c.$table.find('tr'),
			cols;
			startIndex = -1; // stop mousemove updates
			if ( s > -1 && endIndex > -1 && s != endIndex && s + 1 !== endIndex ) {
				adj = endIndex !== 0;
				if (c.debug) {
					ts.log( 'Inserting column ' + s + (adj ? ' after' : ' before') + ' column ' + (endIndex - adj ? 1 : 0) );
				}
				rows.each(function() {
					cols = $(this).children();
					cols.eq(s)[ adj ? 'insertAfter' : 'insertBefore' ]( cols.eq( endIndex - (adj ? 1 : 0) ) );
				});
				cols = [];
				// stored header info needs to be modified too!
				for (i = 0; i < len; i++) {
					if (i === s) { continue; }
					if (i === endIndex - (adj ? 1 : 0)) {
						if (!adj) { cols.push(c.headerContent[s]); }
						cols.push(c.headerContent[i]);
						if (adj) { cols.push(c.headerContent[s]); }
					} else {
						cols.push(c.headerContent[i]);
					}
				}
				c.headerContent = cols;
				// cols = c.headerContent.splice(s, 1);
				// c.headerContent.splice(endIndex - (adj ? 1 : 0), 0, cols);
				c.$table.trigger('updateAll', [ true, wo.reorder_complete ]);
			}
			endIndex = -1;
		},
		mdown = function(e, el){
			var $t = $(el), evt = e;
			if ($t.hasClass(wo.reorder_noReorder)) { return; }
			timer = setTimeout(function(){
				$t.addClass('tablesorter-reorder');
				startReorder(evt, $t);
			}, wo.reorder_delay);
		};

		console.log( c.$headers.last().hasClass(noReorderLast) );

		if ( c.$headers.last().hasClass(noReorderLast) ) {
			offsets.push( offsets[ offsets.length - 1 ] );
		} else {
			offsets.push( c.$table.offset().left + c.$table.outerWidth() );
		}

		c.$headers.not('.' + wo.reorder_noReorder).bind('mousedown.reorder', function(e){
			mdown(e, this);
		});

		$(document)
		.bind('mousemove.reorder', function(e){
			if (startIndex !== -1){
				var c = { left : e.pageX - clickOffset[0] };
				endIndex = -1;
				if (/y/.test(wo.reorder_axis)) {
					c.top = e.pageY - clickOffset[1];
				}
				$helper.css(c);
				positionBar(e);
			}
		})
		.add( c.$headers )
		.bind('mouseup.reorder', function(){
			clearTimeout(timer);
			if (startIndex !== -1 && endIndex !== -1){
				finishReorder();
			} else {
				startIndex = -1;
			}
		});

		// has sticky headers?
		c.$table.bind('stickyHeadersInit', function(){
			wo.$sticky.find('thead').children().not('.' + wo.reorder_noReorder).bind('mousedown.reorder', function(e){
				mdown(e, this);
			});
		});

	}
});

// add mouse coordinates
$x = $('#main h1:last'); $(document).mousemove(function(e){ $x.html( e.pageX ); });

})(jQuery);