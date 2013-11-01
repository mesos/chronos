/*
===============================================================================
Chili is the jQuery code highlighter plugin
...............................................................................
LICENSE: http://www.opensource.org/licenses/mit-license.php
WEBSITE: http://noteslog.com/chili/

											   Copyright 2008 / Andrea Ercolino
===============================================================================
*/

ChiliBook.recipeLoading = false;

ChiliBook.recipes[ "html.js" ] = 
{
	  _name: 'html'
	, _case: false
	, _main: {
		  doctype: { 
			  _match: /<!DOCTYPE\b[\w\W]*?>/ 
			, _style: "color: #CC6600;"
		}
		, ie_style: {
			  _match: /(<!--\[[^\]]*\]>)([\w\W]*?)(<!\[[^\]]*\]-->)/
			, _replace: function( all, open, content, close ) {
				return "<span class='ie_style'>" + this.x( open ) + "</span>" 
					  + this.x( content, '//style' ) 
					  + "<span class='ie_style'>" + this.x( close ) + "</span>";
			}
			, _style: "color: DarkSlateGray; font-weight: bold;"
		}
		, comment: { 
			  _match: /<!--[\w\W]*?-->/ 
			, _style: "color: gray;"
		}
		, script: { 
			  _match: /(<script\s+[^>]*>)([\w\W]*?)(<\/script\s*>)/
			, _replace: function( all, open, content, close ) { 
				  return this.x( open, '//tag_start' ) 
					  + this.x( content, 'js' ) 
					  + this.x( close, '//tag_end' );
			} 
		}
		, style: { 
			  _match: /(<style\s+[^>]*>)([\w\W]*?)(<\/style\s*>)/
			, _replace: function( all, open, content, close ) { 
				  return this.x( open, '//tag_start' ) 
					  + this.x( content, 'css' ) 
					  + this.x( close, '//tag_end' );
			} 
		}
		// matches a starting tag of an element (with attrs)
		// like "<div ... >" or "<img ... />"
		, tag_start: { 
			  _match: /(<\w+)((?:[?%]>|[\w\W])*?)(\/>|>)/ 
			, _replace: function( all, open, content, close ) { 
				  return "<span class='tag_start'>" + this.x( open ) + "</span>" 
					  + this.x( content, '/tag_attrs' ) 
					  + "<span class='tag_start'>" + this.x( close ) + "</span>";
			}
			, _style: "color: navy;"
		} 
		// matches an ending tag
		// like "</div>"
		, tag_end: { 
			  _match: /<\/\w+\s*>|\/>/ 
			, _style: "color: navy;"
		}
		, entity: { 
			  _match: /&\w+?;/ 
			, _style: "color: blue;"
		}
	}
	, tag_attrs: {
		// matches a name/value pair
		attr: {
			// before in $1, name in $2, between in $3, value in $4
			  _match: /(\W*?)([\w-]+)(\s*=\s*)((?:\'[^\']*(?:\\.[^\']*)*\')|(?:\"[^\"]*(?:\\.[^\"]*)*\"))/ 
			, _replace: "$1<span class='attr_name'>$2</span>$3<span class='attr_value'>$4</span>"
			, _style: { attr_name:  "color: royalBlue;", attr_value: "color: maroon;" }
		}
	}
};

ChiliBook.recipes[ "js.js" ] = 
{
	  _name: 'js'
	, _case: true
	, _main: {
		  ml_comment: { 
			  _match: /\/\*[^*]*\*+(?:[^\/][^*]*\*+)*\//
			, _style: 'color: gray;'
		}
		, sl_comment: { 
			  _match: /\/\/.*/
			, _style: 'color: gray;'
		}
		, string: { 
			  _match: /(?:\'[^\'\\\n]*(?:\\.[^\'\\\n]*)*\')|(?:\"[^\"\\\n]*(?:\\.[^\"\\\n]*)*\")/
			, _style: 'color: teal;'
		}
		, num: { 
			  _match: /\b[+-]?(?:\d*\.?\d+|\d+\.?\d*)(?:[eE][+-]?\d+)?\b/
			, _style: 'color: #a00;'
		}
		, boolean: {
			  _match: /\b(true|false|null)\b/
			, _style: 'color: #c00;'
		}
		, reg_not: { //this prevents "a / b / c" to be interpreted as a reg_exp
			  _match: /(?:\w+\s*)\/[^\/\\\n]*(?:\\.[^\/\\\n]*)*\/[gim]*(?:\s*\w+)/
			, _replace: function( all ) {
				return this.x( all, '//num' );
			}
		}
		, reg_exp: { 
			  _match: /\/[^\/\\\n]*(?:\\.[^\/\\\n]*)*\/[gim]*/
			, _style: 'color: maroon;'
		}
		, brace: { 
			  _match: /[\{\}]/
			, _style: 'color: #c00; font-weight: bold;'
		}
		, statement: { 
			  _match: /\b(with|while|var|try|throw|switch|return|if|for|finally|else|do|default|continue|const|catch|case|break)\b/
			, _style: 'color: navy; font-weight: bold;'
		}
		, error: { 
			  _match: /\b(URIError|TypeError|SyntaxError|ReferenceError|RangeError|EvalError|Error)\b/
			, _style: 'color: Coral;'
		}
		, object: { 
			  _match: /\b(String|RegExp|Object|Number|Math|Function|Date|Boolean|Array)\b/
			, _style: 'color: DeepPink;'
		}
		, property: { 
			  _match: /\b(undefined|arguments|NaN|Infinity)\b/
			, _style: 'color: Purple; font-weight: bold;'
		}
		, 'function': { 
			  _match: /\b(parseInt|parseFloat|isNaN|isFinite|eval|encodeURIComponent|encodeURI|decodeURIComponent|decodeURI)\b/
			, _style: 'color: olive;'
		}
		, operator: {
			  _match: /\b(void|typeof|this|new|instanceof|in|function|delete)\b/
			, _style: 'color: RoyalBlue; font-weight: bold;'
		}
		, liveconnect: {
			  _match: /\b(sun|netscape|java|Packages|JavaPackage|JavaObject|JavaClass|JavaArray|JSObject|JSException)\b/
			, _style: 'text-decoration: overline;'
		}
	}
};

ChiliBook.recipes[ "css.js" ] = 
{
	  _name: 'css'
	, _case: true
	, _main: {
		  comment: { 
			  _match: /\/\*[^*]*\*+(?:[^\/][^*]*\*+)*\// 
			, _style: "color: gray;"
		}
		, directive: {
			  _match: /@\w+/
			, _style: "color: fuchsia;"
		}
		, url: {
			  _match: /\b(url\s*\()([^)]+)(\))/
			, _replace: "<span class='url'>$1</span><span class='number'>$2</span><span class='url'>$3</span>"
			, _style: "color: #490093;"
		}
		, block:   {
			  _match: /\{([\w\W]*?)\}/
			, _replace: function( all, pairs ) {
				return '{' + this.x( pairs, '/definition' ) + '}';
			}
		}
		, 'class': {
			  _match: /\.\S+/
			, _style: "color: #0069d2; font-weight: bold;"
		}
		, id:      {
			  _match: /#\S+/
			, _style: "color: #75029B; font-weight: bold;"
		}
		, pseudo:  {
			  _match: /:\w+/
			, _style: "color: #CC9900;"
		}
		, element: {
			  _match: /\w+/
			, _style: "color: #004080; font-weight: bold;"
		}
	}
	, definition: {
		  comment: { 
			  _match: /\/\*[^*]*\*+(?:[^\/][^*]*\*+)*\//
		}
		, property: {
			  _match: /\b(?:zoom|z-index|writing-mode|word-wrap|word-spacing|word-break|width|widows|white-space|volume|voice-family|visibility|vertical-align|unicode-bidi|top|text-underline-position|text-transform|text-shadow|text-overflow|text-kashida-space|text-justify|text-indent|text-decoration|text-autospace|text-align-last|text-align|table-layout|stress|speech-rate|speak-punctuation|speak-numeral|speak-header|speak|size|scrollbar-track-color|scrollbar-shadow-color|scrollbar-highlight-color|scrollbar-face-color|scrollbar-dark-shadow-color|scrollbar-base-color|scrollbar-arrow-color|scrollbar-3d-light-color|ruby-position|ruby-overhang|ruby-align|right|richness|quotes|position|play-during|pitch-range|pitch|pause-before|pause-after|pause|page-break-inside|page-break-before|page-break-after|page|padding-top|padding-right|padding-left|padding-bottom|padding|overflow-Y|overflow-X|overflow|outline-width|outline-style|outline-color|outline|orphans|min-width|min-height|max-width|max-height|marks|marker-offset|margin-top|margin-right|margin-left|margin-bottom|margin|list-style-type|list-style-position|list-style-image|list-style|line-height|line-break|letter-spacing|left|layout-grid-type|layout-grid-mode|layout-grid-line|layout-grid-char-spacing|layout-grid-char|layout-grid|layout-flow|layer-background-image|layer-background-color|include-source|ime-mode|height|font-weight|font-variant|font-style|font-stretch|font-size-adjust|font-size|font-family|font|float|filter|empty-cells|elevation|display|direction|cursor|cue-before|cue-after|cue|counter-reset|counter-increment|content|color|clip|clear|caption-side|bottom|border-width|border-top-width|border-top-style|border-top-color|border-top|border-style|border-spacing|border-right-width|border-right-style|border-right-color|border-right|border-left-width|border-left-style|border-left-color|border-left|border-color|border-collapse|border-bottom-width|border-bottom-style|border-bottom-color|border-bottom|border|behavior|background-repeat|background-position-y|background-position-x|background-position|background-image|background-color|background-attachment|background|azimuth|accelerator|opacity)\s*:/
			, _style: "color: #330066;"
		}
		, special: {
			  _match: /\b(?:-use-link-source|-set-link-source|-replace|-moz-user-select|-moz-user-modify|-moz-user-input|-moz-user-focus|-moz-outline-width|-moz-outline-style|-moz-outline-color|-moz-outline|-moz-opacity|-moz-border-top-colors|-moz-border-right-colors|-moz-border-radius-topright|-moz-border-radius-topleft|-moz-border-radius-bottomright|-moz-border-radius-bottomleft|-moz-border-radius|-moz-border-left-colors|-moz-border-bottom-colors|-moz-binding|-webkit-transition|-moz-transition|-o-transition|transition|-webkit-box-sizing|-moz-box-sizing|box-sizing)\s*:/
			, _style: "color: #330066;"
		}
		, url: {
			  _match: /\b(url\s*\()([^)]+)(\))/
			, _replace: "<span class='url'>$1</span><span class='number'>$2</span><span class='url'>$3</span>"
		}
		, value: {
			  _match: /\b(?:xx-small|xx-large|x-soft|x-small|x-slow|x-low|x-loud|x-large|x-high|x-fast|wider|wait|w-resize|visible|url|uppercase|upper-roman|upper-latin|upper-alpha|underline|ultra-expanded|ultra-condensed|tv|tty|transparent|top|thin|thick|text-top|text-bottom|table-row-group|table-row|table-header-group|table-footer-group|table-column-group|table-column|table-cell|table-caption|sw-resize|super|sub|status-bar|static|square|spell-out|speech|solid|soft|smaller|small-caption|small-caps|small|slower|slow|silent|show|separate|semi-expanded|semi-condensed|se-resize|scroll|screen|s-resize|run-in|rtl|rightwards|right-side|right|ridge|rgb|repeat-y|repeat-x|repeat|relative|projection|print|pre|portrait|pointer|overline|outside|outset|open-quote|once|oblique|nw-resize|nowrap|normal|none|no-repeat|no-open-quote|no-close-quote|ne-resize|narrower|n-resize|move|mix|middle|message-box|medium|marker|ltr|lowercase|lower-roman|lower-latin|lower-greek|lower-alpha|lower|low|loud|local|list-item|line-through|lighter|level|leftwards|left-side|left|larger|large|landscape|justify|italic|invert|inside|inset|inline-table|inline|icon|higher|high|hide|hidden|help|hebrew|handheld|groove|format|fixed|faster|fast|far-right|far-left|fantasy|extra-expanded|extra-condensed|expanded|embossed|embed|e-resize|double|dotted|disc|digits|default|decimal-leading-zero|decimal|dashed|cursive|crosshair|cross|crop|counters|counter|continuous|condensed|compact|collapse|code|close-quote|circle|center-right|center-left|center|caption|capitalize|braille|bottom|both|bolder|bold|block|blink|bidi-override|below|behind|baseline|avoid|auto|aural|attr|armenian|always|all|absolute|above|not-allowed|inherit)\b/
			, _style: "color: #3366FF;"
		}
		, string: { 
			  _match: /(?:\'[^\'\\\n]*(?:\\.[^\'\\\n]*)*\')|(?:\"[^\"\\\n]*(?:\\.[^\"\\\n]*)*\")/ 
			, _style: "color: teal;"
		}
		, number: { 
			_match: /(?:\b[+-]?(?:\d*\.?\d+|\d+\.?\d*))(?:%|(?:(?:px|pt}rem|em|s|)\b))/ 
			, _style: "color: maroon;"
		}
		, color : { 
			  _match: /(?:\#[a-fA-F0-9]{3,6})|\b(?:yellow|white|teal|silver|red|purple|olive|navy|maroon|lime|green|gray|fuchsia|blue|black|aqua|YellowGreen|Yellow|WhiteSmoke|White|Wheat|Violet|Turquoise|Tomato|Thistle|Teal|Tan|SteelBlue|SpringGreen|Snow|SlateGrey|SlateGray|SlateBlue|SkyBlue|Silver|Sienna|SeaShell|SeaGreen|SandyBrown|Salmon|SaddleBrown|RoyalBlue|RosyBrown|Red|Purple|PowderBlue|Plum|Pink|Peru|PeachPuff|PapayaWhip|PaleVioletRed|PaleTurquoise|PaleGreen|PaleGoldenRod|Orchid|OrangeRed|Orange|OliveDrab|Olive|OldLace|Navy|NavajoWhite|Moccasin|MistyRose|MintCream|MidnightBlue|MediumVioletRed|MediumTurquoise|MediumSpringGreen|MediumSlateBlue|MediumSeaGreen|MediumPurple|MediumOrchid|MediumBlue|MediumAquaMarine|Maroon|Magenta|Linen|LimeGreen|Lime|LightYellow|LightSteelBlue|LightSlateGrey|LightSlateGray|LightSkyBlue|LightSeaGreen|LightSalmon|LightPink|LightGrey|LightGreen|LightGray|LightGoldenRodYellow|LightCyan|LightCoral|LightBlue|LemonChiffon|LawnGreen|LavenderBlush|Lavender|Khaki|Ivory|Indigo|IndianRed|HotPink|HoneyDew|Grey|GreenYellow|Green|Gray|GoldenRod|Gold|GhostWhite|Gainsboro|Fuchsia|ForestGreen|FloralWhite|FireBrick|DodgerBlue|DimGrey|DimGray|DeepSkyBlue|DeepPink|Darkorange|DarkViolet|DarkTurquoise|DarkSlateGrey|DarkSlateGray|DarkSlateBlue|DarkSeaGreen|DarkSalmon|DarkRed|DarkOrchid|DarkOliveGreen|DarkMagenta|DarkKhaki|DarkGrey|DarkGreen|DarkGray|DarkGoldenRod|DarkCyan|DarkBlue|Cyan|Crimson|Cornsilk|CornflowerBlue|Coral|Chocolate|Chartreuse|CadetBlue|BurlyWood|Brown|BlueViolet|Blue|BlanchedAlmond|Black|Bisque|Beige|Azure|Aquamarine|Aqua|AntiqueWhite|AliceBlue)\b/ 
			, _style: "color: green;"
		}
	}
};
