$(document).ready(function() {
	
	$('.adclick').click(function() {
		console.log('Clicked ad top');
		console.log($(this));
		$.get('/is-project/clickedAd', {'id': $(this).attr("adid")}, function(){
			console.log('Clicked ad');
		});
	});
});