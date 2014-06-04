function ajaxAction(verb, endpoint, shouldReload, payload, content) {
  // Makes the ajax call to specified endpoing using specified verb.
  // If shouldReload is true, reload the page on success.
  // Always reload the page on failure.
  $.ajax({
    type: verb,
    url: endpoint,
    data: payload,
    contentType: content
  }).done( function(data, textStatus, jqXHR) {
    if (shouldReload) {
      location.reload(true);
    }
  }).fail( function(data, textStatus, jqXHR) {
    alert("Could not complete a " + verb + " request on endpoint " + endpoint);
    // Always reload on failure.
    location.reload(true);
  });
}

function populateWithContent(name, isEditing) {
  var job = entries[name];
  var command = job["command"];
  var owner = job["owner"];
  var parents = job["parents"]; // parents is an array of strings here, but we want a single string as input to the function
  var schedule = job["schedule"];
  var disabled = job["disabled"];
  var type = job["jobType"]
  var cpus = job["cpus"];
  var mem = job["mem"];
  var disk = job["disk"];
  if (parents === undefined) {
    var parentString = "";
  } else {
    var parentString = parents.join(", ");
  }
  if (schedule === undefined) {
    schedule = "";
  }
  populateJobModal(name, command, owner, parentString, schedule, disabled, type, isEditing, cpus, mem, disk)
}

function toggle(name) {
  var job = entries[name];
  job["disabled"] = !job["disabled"];
  var result = confirm('Toggling job ' + name + ', click to continue.');
  if (result) {
    if (job["parents"]) {
      ajaxAction("POST", "/scheduler/dependent", true, JSON.stringify(job), "application/json");
    } else {
      ajaxAction("POST", "/scheduler/iso8601", true, JSON.stringify(job), "application/json");
    }
  }
}

$(function() {

  // The only global scope variable - this is a hash from job_name strings to job objects.
  entries = genTableEntries();

  buildResultsTable();
  setTotalAndFailingJobs();

  // Handle dropdown actions.
  $('[id^="run."]').click( function() {
    var name = $(this).attr("id").split(".")[1];
    var result = confirm('Running job ' + name + ', click to continue.');
    if (result) {
      ajaxAction("PUT", "/scheduler/job/"+name, false);
    }
  });

  $('[id^="kill."]').click( function() {
    var name = $(this).attr("id").split(".")[1];
    var result = confirm('Killing tasks for job ' + name + ', click to continue.');
    if (result) {
      ajaxAction("DELETE", "scheduler/task/kill/"+name, false);
    }
  });

  $('[id^="delete."]').click( function() {
    var name = $(this).attr("id").split(".")[1];
    var result = confirm('Are you sure you want to delete `' + name + '`?');
    if (result) {
      ajaxAction("DELETE", "/scheduler/job/"+name, true);
    }
  });

  $('[id^="edit."]').click( function() {
    var name = $(this).attr("id").split(".")[1];
    populateWithContent(name, true);
  });

  $('[id^="toggle."]').click( function() {
    var name = $(this).attr("id").split(".")[1];
    toggle(name);
  });

  // Add animations to dropdown.
  $('.dropdown').on('show.bs.dropdown', function(e){
    $(this).find('.dropdown-menu').first().stop(true, true).slideDown();
  });

  $('.dropdown').on('hide.bs.dropdown', function(e){
    $(this).find('.dropdown-menu').first().stop(true, true).slideUp();
  });

  // Set enable/disable readonly toggling on parents / schedule fields.
  $('#parentsInput').change( function() {
    if ($('#modificationType').val() != "editing") {
      if ($(this).val().length > 0) {
        // Disable when it's got content
        $('#repeatsInput').val('').prop('readonly', true);
        $('#dateInput').val('').prop('readonly', true);
        $('#timeInput').val('').prop('readonly', true);
        $('#periodInput').val('').prop('readonly', true);
      } else {
        // Enable when it's empty
        $('#repeatsInput').prop('readonly', false);
        $('#dateInput').prop('readonly', false);
        $('#timeInput').prop('readonly', false);
        $('#periodInput').prop('readonly', false);
      }
    }
  });

  $('#repeatsInput, #dateInput, #timeInput, #periodInput').change( function() {
    if ($(this).val().length > 0) {
      $('#parentsInput').prop('readonly', true);
    }
    else {
      if ( $('#repeatsInput').val().length == 0 &&
           $('#dateInput').val().length == 0 &&
           $('#timeInput').val().length == 0 &&
           $('#periodInput').val().length == 0) {
            // Enable when all of them are empty.
            $('#parentsInput').prop('readonly', false);
      }
    }
  });


  // Construct tablesorter plugin and options.
  $.extend($.tablesorter.themes.bootstrap, {
    table      : 'table table-bordered',
    header     : 'bootstrap-header', // give the header a gradient background
    footerRow  : '',
    footerCells: '',
    icons      : '', // add "icon-white" to make them white; this icon class is added to the <i> in the header
    sortNone   : 'bootstrap-icon-unsorted',
    sortAsc    : 'icon-chevron-up glyphicon glyphicon-chevron-up',     // includes classes for Bootstrap v2 & v3
    sortDesc   : 'icon-chevron-down glyphicon glyphicon-chevron-down', // includes classes for Bootstrap v2 & v3
    active     : '', // applied when column is sorted
    hover      : '', // use custom css here - bootstrap class may not override it
    filterRow  : '', // filter row class
    even       : '', // odd row zebra striping
    odd        : ''  // even row zebra striping
  });

  $("#jobtable").tablesorter({
    theme : "bootstrap",
    headerTemplate : '{content} {icon}',
    widthFixed: true,
    widgets : [ "uitheme", "resizable", "filter", "zebra"],
    widgetOptions : {
      zebra : ["even", "odd"],
      resizable_addLastColumn : true
    }
  }).tooltip({
    delay: 0
  });

});

function isEmail(email) {
  var regex = /^([a-zA-Z0-9_.+-])+\@(([a-zA-Z0-9-])+\.)+([a-zA-Z0-9]{2,4})+$/;
  return regex.test(email);
}

var entityMap = {
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  '"': '&quot;',
  "'": '&#39;',
  "/": '&#x2F;'
};

function escapeHtml(string) {
  return String(string).replace(/[&<>"'\/]/g, function (s) {
    return entityMap[s];
  });
}

function secondsToTime(secs) {
  var t = new Date(1970,0,1);
  t.setSeconds(secs);
  var s = t.toTimeString().substr(0,8);
  if(secs > 86399)
    s = Math.floor((t - Date.parse("1/1/70")) / 3600000) + s.substr(2);
  return s;
}

function setTotalAndFailingJobs() {
  var totalJobs = Object.keys(entries).length;
  var failedJobs = 0;
  for(var job_name in entries) {
    if(entries[job_name]["lastStatus"] === "Failed") {
      failedJobs++;
    }
  }
  $('#totalJobs').html(totalJobs + " Jobs");
  $('#failedJobs').html(failedJobs + " Failing");
}

function genTableEntries() {
  var details = getJobDetails();
  var stats_vals = getJobStats();
  for (var job_name in details) {
    var job = details[job_name];
    job["stats"] = stats_vals[job_name];
  }
  return details;
}

function getJobDetails() {
  var job_details = {}; // A hash mapping names to full job JSON blobs
  $.ajax({
    url: '/scheduler/jobs',
    dataType: 'json',
    success: function(entries) {
      $.each(entries, function(i, entry) {
        if (entry["lastSuccess"]) {
          var lastSuccess = entry["lastSuccess"];
          lastSuccess = lastSuccess.slice(0, lastSuccess.lastIndexOf("."));
          entry["lastSuccess"] = lastSuccess;
        }
        if (entry["lastError"]) {
          var lastError = entry["lastError"];
          lastError = lastError.slice(0, lastError.lastIndexOf("."));
          entry["lastError"] = lastError;
        }

        if (entry["lastError"] && entry["lastSuccess"]) {
          var errorTime = Date.parse(entry["lastError"]);
          var successTime = Date.parse(entry["lastSuccess"]);
          entry["lastStatus"] = ((errorTime > successTime) ? "Failed" : "Succeeded");
        } else if (entry["lastSuccess"]) {
          entry["lastStatus"] = "Succeeded";
        } else if (entry["lastError"]) {
          entry["lastStatus"] = "Failed";
        } else {
          entry["lastStatus"] = "Fresh";
        }

        if (entry["parents"]) {
          entry["jobType"] = "dependent";
        } else {
          entry["jobType"] = "scheduled";
        }

        job_details[entry.name] = entry;
      });
    },
    async: false
  });
  return job_details;
}

function getJobStats() {
  var all_stats = {};
  var base_url = "/scheduler/stats/"
  var ranks = ["99thPercentile", "95thPercentile", "75thPercentile", "median"];
  $.each(ranks, function(i, rank) {
    var path = base_url + rank;
    $.ajax({
      url: path,
      dataType: 'json',
      success: function(stats) {
        $.each(stats, function(i, nametime) {
          var name = nametime.jobNameLabel;
          var time = Math.round(nametime.time);
          if (all_stats[name]) {
            all_stats[name][rank] = time;
          } else {
            all_stats[name] = {}
            all_stats[name][rank] = time;
          }
        });
      },
      async: false
    });
  });
  return all_stats;
}

function buildResultsTable() {
  var trstrings = [];
  $.each(entries, function(key, entry) {
    var name = entry.name;
    var owner = entry.owner;
    var command = entry.command;
    var disabled = entry.disabled;
    var lastStatus = entry.lastStatus;
    var successCount = entry.successCount;
    var errorCount = entry.errorCount;
    var lastSuccess = entry.lastSuccess.replace("T", " ");
    var lastError = entry.lastError.replace("T", " ");
    var entry99 = entry.stats["99thPercentile"];
    var entry95 = entry.stats["95thPercentile"];
    var entry75 = entry.stats["75thPercentile"];
    var entry50 = entry.stats["median"];
    var toggle_icon = "";
    var toggle_verb = "";
    if (disabled) {
      toggle_icon = "plus-sign";
      toggle_verb = "Enable";
    } else {
      toggle_icon = "minus-sign";
      toggle_verb = "Disable";
    }
    var trstring =
      ['<tr>',
       '  <td title="'+escapeHtml(command)+'">',
       '    <div class="dropdown">',
       '      <a href="#" id="dropdown.'+name+'" data-toggle="dropdown">'+name+'</a>',
       '      <ul class="dropdown-menu" style="background:none; border:none; box-shadow:none;" role="menu">',
       '        <li class="btn-group">',
       '          <button class="btn btn-success" title="Force Run" id="run.'+name+'"><span class="glyphicon glyphicon-play"></span></button>',
       '          <button class="btn btn-warning" title="Force Stop" id="kill.'+name+'"><span class="glyphicon glyphicon-stop"></span></button>',
       '          <button class="btn btn-primary" title="'+toggle_verb+' Job"  id="toggle.'+name+'"><span class="glyphicon glyphicon-'+toggle_icon+'"></span></button>',
       '          <button class="btn btn-primary" title="Modify Job" data-toggle="modal" data-target="#jobModal" id="edit.'+name+'"><span class="glyphicon glyphicon-wrench"></span></button>',
       '          <button class="btn btn-danger" title="Delete" id="delete.'+name+'"><span class="glyphicon glyphicon-trash"></span></button>',
       '        </li>',
       '      </ul>',
       '    </div>',
       '  </td>',
       '  <td>'+owner+'</td>',
       '  <td>'+disabled+'</td>',
       '  <td>'+lastStatus+'</td>',
       '  <td>'+successCount+'</td>',
       '  <td>'+errorCount+'</td>',
       '  <td>'+lastSuccess+'</td>',
       '  <td>'+lastError+'</td>',
       '  <td>'+secondsToTime(entry99)+'</td>',
       '  <td>'+secondsToTime(entry95)+'</td>',
       '  <td>'+secondsToTime(entry75)+'</td>',
       '  <td>'+secondsToTime(entry50)+'</td>',
       '</tr>'].join('\n');

    trstrings.push(trstring)
  });
  $('#jobData').html(trstrings.join("\n"));
}

function populateJobModal(name, command, owner, parents, schedule, disabled, type, isEditing, cpus, mem, disk) {
  $('#jobModal').on('show.bs.modal', function() {
    // If creating a new job, pass empty strings.
    $('#nameInput').val(name);
    $('#commandInput').val(command);
    $('#ownerInput').val(owner);
    $('#parentsInput').val(parents);
    $('#cpusInput').val(cpus);
    $('#memInput').val(mem);
    $('#diskInput').val(disk);
    // Parse the schedule string into repeats, date, time, period
    if (type === "scheduled") {
      var parts = schedule.split("/");
      var repeats = parts[0].substring(1);
      var datetime = parts[1].slice(0,-1);
      var datetimeparts = datetime.split("T");
      var date = datetimeparts[0];
      var time = datetimeparts[1];
      var dotInd = time.lastIndexOf(".");
      if (dotInd !== -1) {
        time = time.substring(0, dotInd);
      }
      var period = parts[2].substring(1);

      $('#repeatsInput').val(repeats);
      $('#dateInput').val(date);
      $('#timeInput').val(time);
      $('#periodInput').val(period);
    } else {
      $('#repeatsInput').val("");
      $('#dateInput').val("");
      $('#timeInput').val("");
      $('#periodInput').val("");
    }
    if (disabled) {
      $('#statusInputDisabled').prop("checked", true);
    } else {
      $('#statusInputEnabled').prop("checked", true);
    }

    if (isEditing) {
      // Go through and make some properties readonly.
      $('#jobModalLabel').html('Editing ' + name);
      $('#nameInput').prop("readonly", true);
      $('#modificationType').val("editing");
      if (type === "dependent") {
        $('#repeatsInput').prop("readonly", true);
        $('#dateInput').prop("readonly", true);
        $('#timeInput').prop("readonly", true);
        $('#periodInput').prop("readonly", true);
      } else {
        $('#parentsInput').prop("readonly", true);
      }
    } else {
      $('#jobModalLabel').html('Create new job');
      $('#nameInput').prop("readonly", false);
      $('#modificationType').val("creating");
      if (type === "dependent") {
        $('#repeatsInput').prop("readonly", true);
        $('#dateInput').prop("readonly", true);
        $('#timeInput').prop("readonly", true);
        $('#periodInput').prop("readonly", true);
        $('#parentsInput').prop("readonly", false);
      } else {
        $('#parentsInput').prop("readonly", true);
      }
    }
  });
}
