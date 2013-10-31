$(function() {
  // ADD SLIDEDOWN ANIMATION TO DROPDOWN //
  $('.dropdown').on('show.bs.dropdown', function(e){
    $(this).find('.dropdown-menu').first().stop(true, true).slideDown();
  });

  // ADD SLIDEUP ANIMATION TO DROPDOWN //
  $('.dropdown').on('hide.bs.dropdown', function(e){
    $(this).find('.dropdown-menu').first().stop(true, true).slideUp();
  });

  // If the owners is focused, clear and invalidate the schedule fields.
  // If any of the schedule fields are focused, clear invalidate the owner field
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
      // Undisable when it's empty
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

});

