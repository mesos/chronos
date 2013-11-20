function ajaxAction(verb, endpoint, shouldReload) {
  // Makes the ajax call to specified endpoing using specified verb.
  // If shouldReload is true, reload the page on success.
  // Always reload the page on failure.
  $.ajax({
    type: verb,
    url: endpoint
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
  var parents = job["parents"];
  var schedule = job["schedule"];
  var disabled = job["disabled"];
  var type = job["jobType"]
  if (parents === undefined) {
    parents = "";
  }
  if (schedule === undefined) {
    schedule = "";
  }
  populateJobModal(name, command, owner, parents, schedule, disabled, type, isEditing)
}

$(function() {

  // The only global scope variable - this is a hash from job_name strings to job objects.
  entries = genTableEntries();

  buildResultsTable();
  setParentsSelectorOptions();
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

  $('[id^="duplicate."]').click( function() {
    var name = $(this).attr("id").split(".")[1];
    populateWithContent(name, false);
  });

  // Handle new_job button click.
  $('#new_job').click( function() {
    var d = new Date();
    var year = d.getUTCFullYear();
    var month = ('0' + (d.getUTCMonth()+1)).slice(-2);
    var day = ('0' + d.getUTCDate()).slice(-2);
    var hour = ('0' + d.getUTCHours()).slice(-2);
    var minute = ('0' + d.getUTCMinutes()).slice(-2);
    var second = ('0' + d.getUTCSeconds()).slice(-2);
    var dstring = year+"-"+month+"-"+day;
    var tstring = hour+":"+minute+":"+second;
    var schedule = "R/"+dstring+"T"+tstring+"Z/PT6H";
    populateJobModal("", "", "", "", schedule, false, "scheduled", false);
  });


  // Handle modal submission.
  $('#job-modal-submit').click( function(e) {
    var job_hash = {};
    var modificationType = $('#modificationType').val();
    var verb = ( modificationType === "editing" ? "PUT" : "POST");
    var path = "/scheduler/";
    var disabled = ($('#statusInputDisabled').is(":checked") ? true : false);
    var command = $('#commandInput').val();
    var name = $('#nameInput').val();
    var owner = $('#ownerInput').val(); // May be comma separated list.
    var owners = owner.split(",");

    // Sanity check for proper email addresses.
    var hasBrokenOwner = false;
    $.each(owners, function(i, ownerstring) {
      if (!isEmail(ownerstring.trim())) {
        alert("The address " + ownerstring + " does not seem to be valid.");
        console.log("bad email: " + ownerstring);
        hasBrokenOwner = true;
      }
    });
    if (hasBrokenOwner) {
      return;
    }

    // Start to populate the hash we send off to an appropriate endpoint.
    job_hash["async"] = false;
    job_hash["epsilon"] = "PT30M";
    job_hash["executor"] = "";
    job_hash["disabled"] = disabled;
    job_hash["command"] = command;
    job_hash["name"] = name;
    job_hash["owner"] = owner;

    if ($('#parentsInput').val().length > 0) {
      // This is a dependent job.
      job_hash["parents"] = $('#parentsInput').val();
      path += "dependency";

    } else {
      // This is a scheduled job.
      var repeats = $('#repeatsInput').val();
      var date = $('#dateInput').val();
      var time = $('#timeInput').val();
      var period = $('#periodInput').val();
      job_hash["schedule"] = "R"+repeats+"/"+date+"T"+time+"Z/P"+period;
      path += "iso8601";

      // Sanity check for proper formatted date/time/period.
      var dateRegexp = /^\d{4}-\d{2}-\d{2}$/;
      var timeRegexp = /^\d{2}:\d{2}:\d{2}$/;
      var periodRegexp = /^T(\d+[HMSD])+$/;

      if (!dateRegexp.test(date)) {
        alert("Date must be in YYYY-MM-DD format.");
        console.log("bad date: " + date);
        return;
      }
      if (!timeRegexp.test(time)) {
        alert("Time must be in HH:MM:SS format.");
        console.log("bad time: " + time);
        return;
      }
      if (!periodRegexp.test(period)) {
        alert("Period must be in T(\\d+[HMS])+ format.");
        console.log("bad period: " + period);
        return;
      }
    }
    // Make the actual submission request.
    $.ajax({
      type: verb,
      url: path,
      contentType: 'application/json',
      dataType: 'json',
      data: JSON.stringify(job_hash)
    }).done( function() {
      location.reload(true);
    }).fail( function(data, textStatus, jqXHR) {
      alert("Could not finish " + modificationType + " " + name);
      location.reload(true);
    });
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
    if ($('#modificationType').val() != "editing") {
      if ($(this).val().length > 0) {
        // Disable when any of them have content
        $('#parentsInput :selected').prop('selected', false);
        $('#parentsInput').prop('readonly', true);
      }
      else {
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
    var trstring =
      ['<tr>',
       '  <td title="'+escapeHtml(command)+'">',
       '    <div class="dropdown">',
       '      <a href="#" id="dropdown.'+name+'" data-toggle="dropdown">'+name+'</a>',
       '      <ul class="dropdown-menu" style="background:none; border:none; box-shadow:none;" role="menu">',
       '        <li class="btn-group">',
       '          <button class="btn btn-success" title="Force Run" id="run.'+name+'"><span class="glyphicon glyphicon-play"></span></button>',
       '          <button class="btn btn-primary" title="Modify Job" data-toggle="modal" data-target="#jobModal" id="edit.'+name+'"><span class="glyphicon glyphicon-wrench"></span></button>',
       '          <button class="btn btn-info" title="Duplicate Job" data-toggle="modal" data-target="#jobModal" id="duplicate.'+name+'"><span class="glyphicon glyphicon-file"></span></button>',
       '          <button class="btn btn-warning" title="Force Stop" id="kill.'+name+'"><span class="glyphicon glyphicon-stop"></span></button>',
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

function setParentsSelectorOptions() {
  var optionStrings = ["<option value></option>"];
  $.each(entries, function(key, entry) {
    var name = entry.name;
    var optionString = "<option value='"+name+"'>"+name+"</option>";
    optionStrings.push(optionString);
  });
  $('#parentsInput').html(optionsStrings.join("\n"));
}

function populateJobModal(name, command, owner, parents, schedule, disabled, type, isEditing) {
  $('#jobModal').on('show.bs.modal', function() {
    // If creating a new job, pass empty strings.
    $('#nameInput').val(name);
    $('#commandInput').val(command);
    $('#ownerInput').val(owner);
    $('#parentsInput').val(parents.split(","));
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
      // Create a scheduled job by default.
      $('#parentsInput').prop("readonly", true);
    }
  });
}
