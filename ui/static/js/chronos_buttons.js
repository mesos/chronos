$(function() {
  $('[id^="run."]').click( function() {
    alert('Running job ' + $(this).attr("id").substring(4));
  });

  $('[id^="edit."]').click( function() {
    alert('Editing job ' + $(this).attr("id").substring(5));
  });

  $('[id^="delete."]').click( function() {
    alert('Are you ONE HUNDRED percent sure you want to delete ' + $(this).attr("id").substring(7) + '?');
  });

  $('#new_job').click( function() {
    alert('You clicked the new job button');
  });

});
