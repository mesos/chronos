<!DOCTYPE html>
<html>
<head>
 <meta charset="utf-8">
	<title>jQuery plugin: Tablesorter 2.0 - Pager plugin + Filter widget</title>

	<!-- jQuery -->
	<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.7/jquery.min.js"></script>

	<!-- Demo stuff -->
	<link rel="stylesheet" href="css/jq.css">
	<link href="css/prettify.css" rel="stylesheet">
	<script src="js/prettify.js"></script>
	<script src="js/docs.js"></script>

	<!-- Tablesorter: required -->
	<link rel="stylesheet" href="../css/theme.blue.css">
	<script src="../js/jquery.tablesorter.js"></script>

	<!-- Tablesorter: optional -->
	<link rel="stylesheet" href="../addons/pager/jquery.tablesorter.pager.css">
	<script src="../addons/pager/jquery.tablesorter.pager.js"></script>
	<script src="../js/jquery.tablesorter.widgets.js"></script>

	<script id="js">$(function(){

	// define pager options
	var pagerOptions = {
		// target the pager markup - see the HTML block below
		container: $(".pager"),
		// output string - default is '{page}/{totalPages}'; possible variables: {page}, {totalPages}, {startRow}, {endRow} and {totalRows}
		output: '{startRow} - {endRow} / {filteredRows} ({totalRows})',
		// if true, the table will remain the same height no matter how many records are displayed. The space is made up by an empty
		// table row set to a height to compensate; default is false
		fixedHeight: true,
		// remove rows from the table to speed up the sort of large tables.
		// setting this to false, only hides the non-visible rows; needed if you plan to add/remove rows with the pager enabled.
		removeRows: false,
		// go to page selector - select dropdown that sets the current page
		cssGoto:	 '.gotoPage'
	};

	// Initialize tablesorter
	// ***********************
	$("table")
		.tablesorter({
			theme: 'blue',
			headerTemplate : '{content} {icon}', // new in v2.7. Needed to add the bootstrap icon!
			widthFixed: true,
			widgets: ['zebra', 'filter']
		})

		// initialize the pager plugin
		// ****************************
		.tablesorterPager(pagerOptions);

		// Add two new rows using the "addRows" method
		// the "update" method doesn't work here because not all rows are
		// present in the table when the pager is applied ("removeRows" is false)
		// ***********************************************************************
		var r, $row, num = 50,
			row = '<tr><td>Student{i}</td><td>{m}</td><td>{g}</td><td>{r}</td><td>{r}</td><td>{r}</td><td>{r}</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>' +
				'<tr><td>Student{j}</td><td>{m}</td><td>{g}</td><td>{r}</td><td>{r}</td><td>{r}</td><td>{r}</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>';
		$('button:contains(Add)').click(function(){
			// add two rows of random data!
			r = row.replace(/\{[gijmr]\}/g, function(m){
				return {
					'{i}' : num + 1,
					'{j}' : num + 2,
					'{r}' : Math.round(Math.random() * 100),
					'{g}' : Math.random() > 0.5 ? 'male' : 'female',
					'{m}' : Math.random() > 0.5 ? 'Mathematics' : 'Languages'
				}[m];
			});
			num = num + 2;
			$row = $(r);
			$('table')
				.find('tbody').append($row)
				.trigger('addRows', [$row]);
			return false;
		});

		// Delete a row
		// *************
		$('table').delegate('button.remove', 'click' ,function(){
			var t = $('table');
			// disabling the pager will restore all table rows
			t.trigger('disable.pager');
			// remove chosen row
			$(this).closest('tr').remove();
			// restore pager
			t.trigger('enable.pager');
		});

		// Destroy pager / Restore pager
		// **************
		$('button:contains(Destroy)').click(function(){
			// Exterminate, annhilate, destroy! http://www.youtube.com/watch?v=LOqn8FxuyFs
			var $t = $(this);
			if (/Destroy/.test( $t.text() )){
				$('table').trigger('destroy.pager');
				$t.text('Restore Pager');
			} else {
				$('table').tablesorterPager(pagerOptions);
				$t.text('Destroy Pager');
			}
			return false;
		});

		// Disable / Enable
		// **************
		$('.toggle').click(function(){
			var mode = /Disable/.test( $(this).text() );
			$('table').trigger( (mode ? 'disable' : 'enable') + '.pager');
			$(this).text( (mode ? 'Enable' : 'Disable') + 'Pager');
			return false;
		});
		$('table').bind('pagerChange', function(){
			// pager automatically enables when table is sorted.
			$('.toggle').text('Disable');
		});

});</script>
</head>
<body id="pager-demo">
	<div id="banner">
		<h1>table<em>sorter</em></h1>
		<h2>Pager plugin + Filter widget</h2>
		<h3>Flexible client-side table sorting</h3>
		<a href="index.html">Back to documentation</a>
	</div>

	<div id="main">

		<p class="tip">
			<em>NOTE!</em> The following are not part of the original plugin:
			<ul>
				<li>When using this pager plugin with the filter widget, make sure that the <code>removeRows</code> option is set to <code>false</code> or it won't work.</li>
				<li>This combination was not possible in tablesorter versions prior to version 2.4.</li>
				<li>This combination can not be applied to the original tablesorter.</li>
			</ul>
		</p>

	<h1>Demo</h1>
	<br>
	<button type="button">Add Rows</button> <button type="button" class="toggle">Disable Pager</button> <button type="button">Destroy Pager</button>
	<br><br>
	<div class="pager">
		Page: <select class="gotoPage"></select>
		<img src="../addons/pager/icons/first.png" class="first" alt="First" title="First page" />
		<img src="../addons/pager/icons/prev.png" class="prev" alt="Prev" title="Previous page" />
		<span class="pagedisplay"></span> <!-- this can be any element, including an input -->
		<img src="../addons/pager/icons/next.png" class="next" alt="Next" title="Next page" />
		<img src="../addons/pager/icons/last.png" class="last" alt="Last" title= "Last page" />
		<select class="pagesize">
			<option selected="selected" value="10">10</option>
			<option value="20">20</option>
			<option value="30">30</option>
			<option value="40">40</option>
		</select>
	</div>

<table class="tablesorter">
	<thead>
		<tr>
			<th>Name</th>
			<th>Major</th>
			<th>Sex</th>
			<th>English</th>
			<th>Japanese</th>
			<th>Calculus</th>
			<th>Geometry</th>
			<th class="filter-false remove sorter-false"></th>
		</tr>
	</thead>
	<tfoot>
		<tr>
			<th>Name</th>
			<th>Major</th>
			<th>Sex</th>
			<th>English</th>
			<th>Japanese</th>
			<th>Calculus</th>
			<th>Geometry</th>
			<th></th>
		</tr>
	</tfoot>
	<tbody>
		<tr>
			<td>Student01</td>
			<td>Languages</td>
			<td>male</td>
			<td>80</td>
			<td>70</td>
			<td>75</td>
			<td>80</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student02</td>
			<td>Mathematics</td>
			<td>male</td>
			<td>90</td>
			<td>88</td>
			<td>100</td>
			<td>90</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student03</td>
			<td>Languages</td>
			<td>female</td>
			<td>85</td>
			<td>95</td>
			<td>80</td>
			<td>85</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student04</td>
			<td>Languages</td>
			<td>male</td>
			<td>60</td>
			<td>55</td>
			<td>100</td>
			<td>100</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student05</td>
			<td>Languages</td>
			<td>female</td>
			<td>68</td>
			<td>80</td>
			<td>95</td>
			<td>80</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student06</td>
			<td>Mathematics</td>
			<td>male</td>
			<td>100</td>
			<td>99</td>
			<td>100</td>
			<td>90</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student07</td>
			<td>Mathematics</td>
			<td>male</td>
			<td>85</td>
			<td>68</td>
			<td>90</td>
			<td>90</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student08</td>
			<td>Languages</td>
			<td>male</td>
			<td>100</td>
			<td>90</td>
			<td>90</td>
			<td>85</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student09</td>
			<td>Mathematics</td>
			<td>male</td>
			<td>80</td>
			<td>50</td>
			<td>65</td>
			<td>75</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student10</td>
			<td>Languages</td>
			<td>male</td>
			<td>85</td>
			<td>100</td>
			<td>100</td>
			<td>90</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student11</td>
			<td>Languages</td>
			<td>male</td>
			<td>86</td>
			<td>85</td>
			<td>100</td>
			<td>100</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student12</td>
			<td>Mathematics</td>
			<td>female</td>
			<td>100</td>
			<td>75</td>
			<td>70</td>
			<td>85</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student13</td>
			<td>Languages</td>
			<td>female</td>
			<td>100</td>
			<td>80</td>
			<td>100</td>
			<td>90</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student14</td>
			<td>Languages</td>
			<td>female</td>
			<td>50</td>
			<td>45</td>
			<td>55</td>
			<td>90</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student15</td>
			<td>Languages</td>
			<td>male</td>
			<td>95</td>
			<td>35</td>
			<td>100</td>
			<td>90</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student16</td>
			<td>Languages</td>
			<td>female</td>
			<td>100</td>
			<td>50</td>
			<td>30</td>
			<td>70</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student17</td>
			<td>Languages</td>
			<td>female</td>
			<td>80</td>
			<td>100</td>
			<td>55</td>
			<td>65</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student18</td>
			<td>Mathematics</td>
			<td>male</td>
			<td>30</td>
			<td>49</td>
			<td>55</td>
			<td>75</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student19</td>
			<td>Languages</td>
			<td>male</td>
			<td>68</td>
			<td>90</td>
			<td>88</td>
			<td>70</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student20</td>
			<td>Mathematics</td>
			<td>male</td>
			<td>40</td>
			<td>45</td>
			<td>40</td>
			<td>80</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student21</td>
			<td>Languages</td>
			<td>male</td>
			<td>50</td>
			<td>45</td>
			<td>100</td>
			<td>100</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr>
			<td>Student22</td>
			<td>Mathematics</td>
			<td>male</td>
			<td>100</td>
			<td>99</td>
			<td>100</td>
			<td>90</td>
			<td><button type="button" class="remove" title="Remove this row">X</button></td>
		</tr>
		<tr><td>Student23</td><td>Mathematics</td><td>male</td><td>82</td><td>77</td><td>0</td><td>79</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student24</td><td>Languages</td><td>female</td><td>100</td><td>91</td><td>13</td><td>82</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student25</td><td>Mathematics</td><td>male</td><td>22</td><td>96</td><td>82</td><td>53</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student26</td><td>Languages</td><td>female</td><td>37</td><td>29</td><td>56</td><td>59</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student27</td><td>Mathematics</td><td>male</td><td>86</td><td>82</td><td>69</td><td>23</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student28</td><td>Languages</td><td>female</td><td>44</td><td>25</td><td>43</td><td>1</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student29</td><td>Mathematics</td><td>male</td><td>77</td><td>47</td><td>22</td><td>38</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student30</td><td>Languages</td><td>female</td><td>19</td><td>35</td><td>23</td><td>10</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student31</td><td>Mathematics</td><td>male</td><td>90</td><td>27</td><td>17</td><td>50</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student32</td><td>Languages</td><td>female</td><td>60</td><td>75</td><td>33</td><td>38</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student33</td><td>Mathematics</td><td>male</td><td>4</td><td>31</td><td>37</td><td>15</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student34</td><td>Languages</td><td>female</td><td>77</td><td>97</td><td>81</td><td>44</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student35</td><td>Mathematics</td><td>male</td><td>5</td><td>81</td><td>51</td><td>95</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student36</td><td>Languages</td><td>female</td><td>70</td><td>61</td><td>70</td><td>94</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student37</td><td>Mathematics</td><td>male</td><td>60</td><td>3</td><td>61</td><td>84</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student38</td><td>Languages</td><td>female</td><td>63</td><td>39</td><td>0</td><td>11</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student39</td><td>Mathematics</td><td>male</td><td>50</td><td>46</td><td>32</td><td>38</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student40</td><td>Languages</td><td>female</td><td>51</td><td>75</td><td>25</td><td>3</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student41</td><td>Mathematics</td><td>male</td><td>43</td><td>34</td><td>28</td><td>78</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student42</td><td>Languages</td><td>female</td><td>11</td><td>89</td><td>60</td><td>95</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student43</td><td>Mathematics</td><td>male</td><td>48</td><td>92</td><td>18</td><td>88</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student44</td><td>Languages</td><td>female</td><td>82</td><td>2</td><td>59</td><td>73</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student45</td><td>Mathematics</td><td>male</td><td>91</td><td>73</td><td>37</td><td>39</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student46</td><td>Languages</td><td>female</td><td>4</td><td>8</td><td>12</td><td>10</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student47</td><td>Mathematics</td><td>male</td><td>89</td><td>10</td><td>6</td><td>11</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student48</td><td>Languages</td><td>female</td><td>90</td><td>32</td><td>21</td><td>18</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student49</td><td>Mathematics</td><td>male</td><td>42</td><td>49</td><td>49</td><td>72</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
		<tr><td>Student50</td><td>Languages</td><td>female</td><td>56</td><td>37</td><td>67</td><td>54</td><td><button type="button" class="remove" title="Remove this row">X</button></td></tr>
	</tbody>
</table>

<div class="pager">
	Page: <select class="gotoPage"></select>		<img src="../addons/pager/icons/first.png" class="first" alt="First" title="First page" />
	<img src="../addons/pager/icons/prev.png" class="prev" alt="Prev" title="Previous page" />
	<span class="pagedisplay"></span> <!-- this can be any element, including an input -->
	<img src="../addons/pager/icons/next.png" class="next" alt="Next" title="Next page" />
	<img src="../addons/pager/icons/last.png" class="last" alt="Last" title= "Last page" />
	<select class="pagesize">
		<option selected="selected" value="10">10</option>
		<option value="20">20</option>
		<option value="30">30</option>
		<option value="40">40</option>
	</select>
</div>

	<h1>Javascript</h1>
	<div id="javascript">
		<pre class="prettyprint lang-javascript"></pre>
	</div>

<div class="next-up">
	<hr />
	Next up: <a href="example-empty-table.html">Initializing tablesorter on a empty table &rsaquo;&rsaquo;</a>
</div>

</div>

</body>
</html>

