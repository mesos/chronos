function drawBarChart(chartID, dataSet, selectString, numJobs) {
  // chartID => A unique drawing identifier that has no spaces, no "." and no "#" characters.
  // dataSet => Input Data for the chart, itself.
  // selectString => String that allows you to pass in
  //           a D3 select string.
  // numJobs => The number of bars to render
  // TODO: handle jobs that take zero time
  numJobs = Math.min(dataSet.length, numJobs);
  var canvasWidth = $(selectString).parent().width();
  var barsWidthTotal = canvasWidth / 2;
  var barHeight = 20;
  var barsHeightTotal = barHeight * numJobs;
  var canvasHeight = numJobs * barHeight + 10; // +10 puts a little space at bottom.
  var legendOffset = barHeight/2;
  var legendBulletOffset = 30;
  var legendTextOffset = 20;
  var maxTime = d3.max(dataSet, function(d) {return d.time; });
  var minTime = d3.min(dataSet, function(d) {return d.time; });
  var x = d3.scale.linear().domain([0, maxTime]).rangeRound([0, barsWidthTotal]);
  var y = d3.scale.linear().domain([0, numJobs]).range([0, barsHeightTotal]);

  // Color Scale Handling...
  var colorScale = d3.scale.linear();
  colorScale.domain([minTime, maxTime]);
  colorScale.range(["seagreen", "red"]).interpolate(d3.interpolateHcl);

  var synchronizedMouseOver = function() {
    var bar = d3.select(this);
    var indexValue = bar.attr("index_value");

    var barSelector = "." + "bars-" + chartID + "-bar-" + indexValue;
    var selectedBar = d3.selectAll(barSelector);
    selectedBar.style("fill", "Maroon");

    var textSelector = "." + "bars-" + chartID + "-legendText-" + indexValue;
    var selectedLegendText = d3.selectAll(textSelector);
    selectedLegendText.style("fill", "Maroon");
  };

  var synchronizedMouseOut = function() {
    var bar = d3.select(this);
    var indexValue = bar.attr("index_value");

    var barSelector = "." + "bars-" + chartID + "-bar-" + indexValue;
    var selectedBar = d3.selectAll(barSelector);
    var colorValue = selectedBar.attr("color_value");
    selectedBar.style("fill", colorValue);

    var textSelector = "." + "bars-" + chartID + "-legendText-" + indexValue;
    var selectedLegendText = d3.selectAll(textSelector);
    selectedLegendText.style("fill", "Blue");
  };

  // Create the svg drawing canvas...
  $(selectString).empty();
  var canvas = d3.select(selectString)
    .append("svg:svg")
    .attr("width", canvasWidth)
    .attr("height", canvasHeight);

  // Draw individual hyper text enabled bars...
  canvas.selectAll("rect")
  .data(dataSet)
  .enter().append("svg:a")
    .append("svg:rect")
      .attr("x", 0) // Right to left
      .attr("y", function(d, i) { return y(i); })
      .attr("height", barHeight)
      .on('mouseover', synchronizedMouseOver)
      .on("mouseout", synchronizedMouseOut)
      .style("fill", "White" )
      .style("stroke", "White" )
      .transition()
      .ease("bounce")
        .duration(500)
        .delay(function(d, i) { return i * 100; })
      .attr("width", function(d) { return x(d.time); })
      .style("fill", function(d, i) { colorVal = colorScale(d.time); return colorVal; } )
      .attr("index_value", function(d, i) { return "index-" + i; })
      .attr("class", function(d, i) { return "bars-" + chartID + "-bar-index-" + i; })
      .attr("color_value", function(d, i) { return colorScale(d.time); }) // Bar fill color...
      .style("stroke", "white"); // Bar border color...


  // Create text values that go at end of each bar...
  canvas.selectAll("text")
  .data(dataSet) // Bind dataSet to text elements
  .enter().append("svg:text") // Append text elements
    .attr("x", x)
    .attr("y", function(d, i) { return y(i); })
    //.attr("y", function(d) { return y(d) + y.rangeBand() / 2; })
    .attr("dx", function(d) { return x(d.time) - 5; })
    .attr("dy", barHeight-5) // vertical-align: middle
    .attr("text-anchor", "end") // text-align: right
    .text(function(d) { return Math.round(d.time);})
    .attr("fill", "White");

  // Create hyper linked text at right that acts as label key...
  canvas.selectAll("a.legend_link")
  .data(dataSet) // Instruct to bind dataSet to text elements
  .enter().append("svg:a") // Append legend elements
    .attr("xlink:href", function(d) { return "/#jobs/" + d.jobNameLabel; })
      .append("text")
        .attr("text-anchor", "center")
        .attr("x", barsWidthTotal + legendBulletOffset + legendTextOffset)
        .attr("y", function(d, i) { return legendOffset + i*barHeight; } )
        .attr("dx", 0)
        .attr("dy", "5px") // Controls padding to place text above bars
        .text(function(d) { return d.jobNameLabel;})
        .style("fill", "Blue")
        .attr("index_value", function(d, i) { return "index-" + i; })
        .attr("class", function(d, i) { return "bars-" + chartID + "-legendText-index-" + i; })
        .on('mouseover', synchronizedMouseOver)
        .on("mouseout", synchronizedMouseOut);
}

$(function() {
  $("#chart_mean").click( function() {
    $.getJSON("/scheduler/stats/mean", function(result) {
        drawBarChart("mean", result, "#barchart .chart", 10);
    });
  });
  $("#chart_median").click( function() {
    $.getJSON("/scheduler/stats/median", function(result) {
        drawBarChart("median", result, "#barchart .chart", 10);
    });
  });
  $("#chart_75").click( function() {
    $.getJSON("/scheduler/stats/75thPercentile", function(result) {
        drawBarChart("75", result, "#barchart .chart", 10);
    });
  });
  $("#chart_95").click( function() {
    $.getJSON("/scheduler/stats/95thPercentile", function(result) {
        drawBarChart("95", result, "#barchart .chart", 10);
    });
  });
  $("#chart_99").click( function() {
    $.getJSON("/scheduler/stats/99thPercentile", function(result) {
        drawBarChart("99", result, "#barchart .chart", 10);
    });
  });
});