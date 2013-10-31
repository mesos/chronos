$(function() {
  $('[id^="run."]').click( function() {
    var parts = $(this).attr("id").split(".");
    var name = parts[1];
    alert('Running job ' + name);
    request = $.ajax({
      type: "POST",
      url: '/run',
      data: {"name": name}
    }).done( function() {
      location.reload(true);
    });
  });

  $('[id^="kill."]').click( function() {
    var parts = $(this).attr("id").split(".");
    var name = parts[1];
    alert('Killing tasks for job ' + name);
    request = $.ajax({
      type: "POST",
      url: '/kill',
      data: {"name": name}
    }).done( function() {
      location.reload(true);
    });
  });

  $('#delete_job').click( function() {
    var name = prompt('Which job name do you want to delete?');
    if (!name) {
      return;
    }
    var result = confirm('Are you sure you want to delete `' + name + '`?');
    if (result) {
      request = $.ajax({
        type: "POST",
        url: '/delete',
        data: {"name": name}
      }).done( function() {
        location.reload(true);
      });
    }
  });
});

function buildEditJobModal(name, command, owner, parents, schedule, disabled) {
  $('#editModal').on('show.bs.modal', function() {
    $('#editModalLabel').html('Editing ' + name);
    $('#nameInput').val(name);
    $('#commandInput').val(command);
    $('#ownerInput').val(owner)
    if (disabled == "True") {
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
        '    <input type="text" class="form-control" name="dateInput" id="dateInput" placeholder="YYYY-MM-DD" value="">',
        '  </div>',
        '  <div class="col-xs-3">',
        '    <label for="timeInput">Launch Time</label>',
        '    <input type="text" class="form-control" name="timeInput" id="timeInput" placeholder="HH:MM:SS" value="">',
        '  </div>',
        '  <div class="col-xs-3">',
        '    <label for="periodInput">Period</label>',
        '    <input type="text" class="form-control" name="periodInput" id="periodInput" placeholder="T24H or T1D or similar" value="">',
        '  </div>'].join('\n');

    if (parents.length > 0) {
      $('#customEditFields').html(parentInputHTML);
      $('#parentsInput').val(parents.join(', '));
    } else {
      $('#customEditFields').html(scheduleInputHTML);
      var parts = schedule.split("/");
      reps = parts[0].substring(1);
      datetime = parts[1]; // of the form 2013-10-25T01:00:00.000Z
      datetime = datetime.slice(0,-1);
      datetimeparts = datetime.split("T");
      date = datetimeparts[0];
      time = datetimeparts[1];
      period = parts[2].substring(1);
      $('#repeatsInput').val(reps);
      $('#dateInput').val(date);
      $('#timeInput').val(time);
      $('#periodInput').val(period);
    }
  });
}
