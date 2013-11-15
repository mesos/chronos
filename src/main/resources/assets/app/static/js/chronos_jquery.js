$(function() {
  entries = genTableEntries();
  buildResultsTable();
  setTotalAndFailingJobs();

  $('[id^="run."]').click( function() {
    var parts = $(this).attr("id").split(".");
    var name = parts[1];
    var result = confirm('Running job ' + name + ', click to continue.');
    if (result) {
      $.ajax({
        type: "PUT",
        url: '/scheduler/job/'+name
      }).done( function(data, textStatus, jqXHR) {
      }).fail( function(data, textStatus, jqXHR) {
        alert("Could not run " + name);
        location.reload(true);
      });
    }
  });

  $('[id^="kill."]').click( function() {
    var parts = $(this).attr("id").split(".");
    var name = parts[1];
    var result = confirm('Killing tasks for job ' + name + ', click to continue.');
    if (result) {
      $.ajax({
        type: "DELETE",
        url: '/scheduler/task/kill/'+name
      }).done( function(data, textStatus, jqXHR) {
      }).fail( function(data, textStatus, jqXHR) {
        alert("Could not kill " + name);
        location.reload(true);
      });
    }
  });

  $('[id^="edit."]').click( function() {
    var parts = $(this).attr("id").split(".");
    var name = parts[1];
    buildEditJobModal(name);
  });

  $('#delete_job').click( function() {
    var name = prompt('Which job name do you want to delete?');
    if (!name) {
      return;
    }
    if (!entries[name]) {
      alert("No such job (" + name + ") exists.");
      return;
    }
    var result = confirm('Are you sure you want to delete `' + name + '`?');
    if (result) {
      $.ajax({
        type: "DELETE",
        url: '/scheduler/job/'+name,
      }).done( function(data, textStatus, jqXHR) {
        location.reload(true);
      }).fail( function(data, textStatus, jqXHR) {
        alert("Could not delete " + name);
        location.reload(true);
      });
    }
  });

  $('#editjob-modal-submit').click( function(e) {
    var job_hash = {};
    var disabled = ($('#statusInputDisabled').is(":checked") ? true : false);
    var command = $('#commandInput').val();
    var name = $('#nameInput').val();
    var owner = $('#ownerInput').val();
    var owners = owner.split(",");
    var brokenOwner = false;
    $.each(owners, function(i, ownerstring) {
      if (!isEmail(ownerstring.trim())) {
        alert("The address " + ownerstring + " does not seem to be valid.");
        console.log("bad email: " + ownerstring);
        brokenOwner = true;
      }
    });
    if (brokenOwner) {
      return;
    }

    job_hash["async"] = false;
    job_hash["epsilon"] = "PT30M";
    job_hash["executor"] = "";
    job_hash["disabled"] = disabled;
    job_hash["command"] = command;
    job_hash["name"] = name;
    job_hash["owner"] = owner;

    if ($('#parentsInput').exists() && $('#parentsInput').val().length > 0) {
      job_hash["parents"] = []
      var splitParents = $('#parentsInput').val().split(",");
      $.each(splitParents, function(i, parent) {
        var trimmed_parent = parent.trim();
        if (!entries[trimmed_parent]) {
          alert("The parent job " + trimmed_parent + " does not exist.");
          return;
        }
        job_hash["parents"].push(trimmed_parent);
      });
      var path = "/scheduler/dependency";
    } else {
      var repeats = $('#repeatsInput').val();
      var date = $('#dateInput').val();
      var time = $('#timeInput').val();
      var period = $('#periodInput').val();
      job_hash["schedule"] = "R"+repeats+"/"+date+"T"+time+"Z/P"+period;
      var path = "/scheduler/iso8601";
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


    request = $.ajax({
      type: "PUT",
      url: path,
      contentType: 'application/json',
      dataType: 'json',
      data: JSON.stringify(job_hash)
    }).done( function() {
      location.reload(true);
    }).fail( function(data, textStatus, jqXHR) {
      alert("Could not edit " + name);
      location.reload(true);
    });
  });

  $('#newjob-modal-submit').click( function(e) {
    var job_hash = {};
    var disabled = ($('#newStatusInputDisabled').is(":checked") ? true : false);
    var command = $('#newCommandInput').val();
    var name = $('#newNameInput').val();
    var owner = $('#newOwnerInput').val();
    var owners = owner.split(",");
    var brokenOwner = false;
    $.each(owners, function(i, ownerstring) {
      if (!isEmail(ownerstring.trim())) {
        alert("The address " + ownerstring + " does not seem to be valid.");
        console.log("bad email: " + ownerstring);
        brokenOwner = true;
      }
    });
    if (brokenOwner) {
      return;
    }

    job_hash["async"] = false;
    job_hash["epsilon"] = "PT30M";
    job_hash["executor"] = "";
    job_hash["disabled"] = disabled;
    job_hash["command"] = command;
    job_hash["name"] = name;
    job_hash["owner"] = owner;

    if ($('#newParentsInput').val().length > 0) {
      job_hash["parents"] = []
      var splitParents = $('#newParentsInput').val().split(",");
      $.each(splitParents, function(i, parent) {
        var trimmed_parent = parent.trim();
        if (!entries[trimmed_parent]) {
          alert("The parent job " + trimmed_parent + " does not exist.");
          return;
        }
        job_hash["parents"].push(trimmed_parent);
      });
      var path = "/scheduler/dependency";
    } else {
      var repeats = $('#newRepeatsInput').val();
      var date = $('#newDateInput').val();
      var time = $('#newTimeInput').val();
      var period = $('#newPeriodInput').val();
      job_hash["schedule"] = "R"+repeats+"/"+date+"T"+time+"Z/P"+period;
      var path = "/scheduler/iso8601";
      var dateRegexp = /^\d{4}-\d{2}-\d{2}$/;
      var timeRegexp = /^\d{2}:\d{2}:\d{2}$/;
      var periodRegexp = /^T(\d+[HMSD])+$/;

      if (!dateRegexp.test(date)) {
        console.log("bad date: " + date);
        alert("Date must be in YYYY-MM-DD format.");
        return;
      }
      if (!timeRegexp.test(time)) {
        console.log("bad time: " + time);
        alert("Time must be in HH:MM:SS format.");
        return;
      }
      if (!periodRegexp.test(period)) {
        console.log("bad period: " + period);
        alert("Period must be in T(\\d+[HMS])+ format.");
        return;
      }
    }

    request = $.ajax({
      type: "POST",
      url: path,
      contentType: 'application/json',
      dataType: 'json',
      data: JSON.stringify(job_hash)
    }).done( function() {
      location.reload(true);
    }).fail( function(data, textStatus, jqXHR) {
      alert("Could not create " + name);
      location.reload(true);
    });
  });

  // When you bring up the new job modal, populate it by default with the current time.
  $('#newJobModal').on('show.bs.modal', function() {
    var d = new Date();
    var year = d.getUTCFullYear();
    var month = ('0' + (d.getUTCMonth()+1)).slice(-2);
    var day = ('0' + d.getUTCDate()).slice(-2);
    var hour = ('0' + d.getUTCHours()).slice(-2);
    var minute = ('0' + d.getUTCMinutes()).slice(-2);
    var second = ('0' + d.getUTCSeconds()).slice(-2);
    var dstring = year+"-"+month+"-"+day;
    var tstring = hour+":"+minute+":"+second;
    $('#newDateInput').val(dstring);
    $('#newTimeInput').val(tstring);
    $('#newPeriodInput').val("T6H");
    $('#newParentsInput').prop("disabled", true);
  });

  // ADD SLIDEDOWN ANIMATION TO DROPDOWN //
  $('.dropdown').on('show.bs.dropdown', function(e){
    $(this).find('.dropdown-menu').first().stop(true, true).slideDown();
  });

  // ADD SLIDEUP ANIMATION TO DROPDOWN //
  $('.dropdown').on('hide.bs.dropdown', function(e){
    $(this).find('.dropdown-menu').first().stop(true, true).slideUp();
  });

  // If the owners field is focused, clear and invalidate the schedule fields.
  // If any of the schedule fields are focused, clear and invalidate the owner field
  $('#newParentsInput').change( function() {
    if ($(this).val().length > 0) {
      $('#newRepeatsInput').val('');
      $('#newDateInput').val('');
      $('#newTimeInput').val('');
      $('#newPeriodInput').val('');
      $('#newRepeatsInput').prop('disabled', true);
      $('#newDateInput').prop('disabled', true);
      $('#newTimeInput').prop('disabled', true);
      $('#newPeriodInput').prop('disabled', true);
    } else {
      // Enable when it's empty
      $('#newRepeatsInput').prop('disabled', false);
      $('#newDateInput').prop('disabled', false);
      $('#newTimeInput').prop('disabled', false);
      $('#newPeriodInput').prop('disabled', false);
    }
  });
  $('#newRepeatsInput').change( function() {
    if ($(this).val().length > 0) {
      $('#newParentsInput').val('');
      $('#newParentsInput').prop('disabled', true);
    }
    else {
      $('#newParentsInput').prop('disabled', false);
    }
  }); 
  $('#newDateInput').change( function() {
    if ($(this).val().length > 0) {
      $('#newParentsInput').val('');
      $('#newParentsInput').prop('disabled', true);
    }
    else {
      $('#newParentsInput').prop('disabled', false);
    }
  });
  $('#newTimeInput').change( function() {
    if ($(this).val().length > 0) {
      $('#newParentsInput').val('');
      $('#newParentsInput').prop('disabled', true);
    }
    else {
      $('#newParentsInput').prop('disabled', false);
    }
  }); 
  $('#newPeriodInput').change( function() {
    if ($(this).val().length > 0) {
      $('#newParentsInput').val('');
      $('#newParentsInput').prop('disabled', true);
    }
    else {
      $('#newParentsInput').prop('disabled', false);
    }
  });

  // Tooltip settings.
  $("#jobtable").tooltip({
    delay:0
  });

  $.extend($.tablesorter.themes.bootstrap, {
    // these classes are added to the table. To see other table classes available,
    // look here: http://twitter.github.com/bootstrap/base-css.html#tables
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
  // call the tablesorter plugin and apply the uitheme widget
  $("#jobtable").tablesorter({
    // this will apply the bootstrap theme if "uitheme" widget is included
    // the widgetOptions.uitheme is no longer required to be set
    theme : "bootstrap",
    headerTemplate : '{content} {icon}', // new in v2.7. Needed to add the bootstrap icon!
    // widget code contained in the jquery.tablesorter.widgets.js file
    // use the zebra stripe widget if you plan on hiding any rows (filter widget)
    widgets : [ "uitheme", "resizable", "filter", "zebra"],
    widgetOptions : {
      // using the default zebra striping class name, so it actually isn't included in the theme variable above
      // this is ONLY needed for bootstrap theming if you are using the filter widget, because rows are hidden
      zebra : ["even", "odd"],
      resizable_addLastColumn : true
    }
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
       '          <button class="btn btn-warning" title="Modify Job" data-toggle="modal" data-target="#editModal" id="edit.'+name+'"><span class="glyphicon glyphicon-wrench"></span></button>',
       '          <button class="btn btn-danger" title="Force Stop" id="kill.'+name+'"><span class="glyphicon glyphicon-stop"></span></button>',
       '        </li>',
       '      </ul>',
       '    </div>',
       '  </td>',
       '  <td title="'+owner+'">'+owner+'</td>',
       '  <td>'+disabled+'</td>',
       '  <td>'+lastStatus+'</td>',
       '  <td>'+successCount+'</td>',
       '  <td>'+errorCount+'</td>',
       '  <td>'+lastSuccess+'</td>',
       '  <td>'+lastError+'</td>',
       '  <td>'+secondstotime(entry99)+'</td>',
       '  <td>'+secondstotime(entry95)+'</td>',
       '  <td>'+secondstotime(entry75)+'</td>',
       '  <td>'+secondstotime(entry50)+'</td>',
       '</tr>'].join('\n');

    trstrings.push(trstring)
  });
  $('#jobData').html(trstrings.join("\n"));
}

function secondstotime(secs) {
  var t = new Date(1970,0,1);
  t.setSeconds(secs);
  var s = t.toTimeString().substr(0,8);
  if(secs > 86399)
    s = Math.floor((t - Date.parse("1/1/70")) / 3600000) + s.substr(2);
  return s;
}

function truncate(string, n) {
  if (string.length > n)
    return string.substring(0, n)+"...";
  else
    return string;
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

// Allow us to check if a selector exists
$.fn.exists = function() {
  return this.length !== 0;
}

function buildEditJobModal(name) {
  $('#editModal').on('show.bs.modal', function() {
    var job = entries[name];
    command = job["command"]
    owner = job["owner"]
    disabled = job["disabled"]
    type = job["jobType"]
    $('#editModalLabel').html('Editing ' + name);
    $('#nameInput').val(name);
    $('#commandInput').val(command);
    $('#ownerInput').val(owner)
    if (disabled) {
      $('#statusInputDisabled').prop("checked", true);
    } else {
      $('#statusInputEnabled').prop("checked", true);
    }

    var parentInputHTML =
       ['  <label for="parentsInput">Parents</label>',
        '  <input type="text" class="form-control" name="parentsInput" id="parentsInput" placeholder="job_parent1, job_parent2" value="">'].join('\n');
    var scheduleInputHTML =
       ['  <div class="col-xs-2">',
        '    <label for="repeatsInput">Repeats</label>',
        '    <input type="text" class="form-control" name="repeatsInput" id="repeatsInput" placeholder="∞" value="">',
        '  </div>',
        '  <div class="col-xs-4">',
        '    <label for="dateInput">Launch Day</label>',
        '    <input type="date" class="form-control" name="dateInput" id="dateInput" min="2013-01-01" placeholder="YYYY-MM-DD" value="">',
        '  </div>',
        '  <div class="col-xs-3">',
        '    <label for="timeInput">Launch Time</label>',
        '    <input type="text" class="form-control" name="timeInput" id="timeInput" placeholder="HH:MM:SS" value="">',
        '  </div>',
        '  <div class="col-xs-3">',
        '    <label for="periodInput">Period</label>',
        '    <input type="text" class="form-control" name="periodInput" id="periodInput" placeholder="T24H or T1D or similar" value="">',
        '  </div>'].join('\n');

    if (type === "dependent") {
      parents = job["parents"];
      $('#customEditFields').html(parentInputHTML);
      $('#parentsInput').val(parents);
    } else {
      $('#customEditFields').html(scheduleInputHTML);
      schedule = job["schedule"]
      var parts = schedule.split("/");
      reps = parts[0].substring(1);
      datetime = parts[1]; // of the form 2013-10-25T01:00:00.000Z
      datetime = datetime.slice(0,-1);
      datetimeparts = datetime.split("T");
      date = datetimeparts[0];

      time = datetimeparts[1];
      var dotInd = time.lastIndexOf(".");
      if (dotInd !== -1) {
        time = time.substring(0, dotInd);
      }
      period = parts[2].substring(1);
      $('#repeatsInput').val(reps);
      $('#dateInput').val(date);
      $('#timeInput').val(time);
      $('#periodInput').val(period);
    }
  });
}
