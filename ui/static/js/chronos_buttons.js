$(function() {
  $('[id^="run."]').click( function() {
    var name = $(this).attr("id").substring(4);
    alert('Running job ' + name);
    $.ajax({
      //url: {{chronos_host}} + '/scheduler/job/' + name,
      url: 'http://localhost:4400/scheduler/job/' + name,
      type: 'PUT',
      success: function(result) {
        alert('Ran that job');
        alert(result);
      }
    });

  });

  $('[id^="edit."]').click( function() {
    var name = $(this).attr("id").substring(5);
    alert('Editing job ' + name);
  });

  $('[id^="kill."]').click( function() {
    var name = $(this).attr("id").substring(5);
    alert('Killing tasks for job ' + name);
    request = $.ajax({
      //url: {{chronos_host}} + '/scheduler/job/' + name,
      url: 'http://localhost:4400/scheduler/task/kill' + name,
      type: 'DELETE'
    });
    request.done(function (response, textStatus, jqXHR) {
      console.log("It seemed to work");
    });
    request.fail(function (jqXHR, textStatus, errorThrown) {
      console.error("The following error occured: " + textStatus, errorThrown);
    });
    event.preventDefault();
  });


  $('#delete_job').click( function() {
    var name = prompt('Which job name do you want to delete?');
    var result = confirm('Are you sure you want to delete `' + name + '`?');
    if (result) {
      request = $.ajax({
        //url: {{chronos_host}} + '/scheduler/job/' + name,
        url: 'http://localhost:4400/scheduler/job/' + name,
        type: 'DELETE'
      });
      console.log(request);
      request.done(function (response, textStatus, jqXHR) {
        console.log("It seemed to work");
      });
      request.fail(function (jqXHR, textStatus, errorThrown) {
        console.error("The following error occured: " + textStatus, errorThrown);
      });
    }
  });

  $('#new_job').click( function() {
    alert('You clicked the new job button');
  });

});
