$(document).ready(function() {
	$('.scoring-method').dropdown();
	
	$('.scoringMethod').val($('.dropdown-menu-scoring-method a.active').attr("value"));
	
	$('.dropdown-menu-scoring-method a').click(function(){
		$('.active').toggleClass('active');
		$('.scoring-method').html($(this).html());
		$(this).toggleClass('active');
		$('.scoringMethod').val($(this).attr("value"));
	});
});