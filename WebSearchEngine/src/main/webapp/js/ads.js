$(document).ready(function() {
	
	$('.adclick').click(function() {
		$.get('is-project/clickedAd', {'id': $(this).adID}, function(){
			console.log('Clicked ad');
		});
	});
});