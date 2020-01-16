$(document).ready(function() {
	$('.smallImg').on('click',function() {
		$('#bigImage').attr('src', $('.smallImg').attr('src'));
		$('.modal').modal('show');
	});

	$('#bigImage').click(function() {
		$('.modal').modal('hide');
	});
});