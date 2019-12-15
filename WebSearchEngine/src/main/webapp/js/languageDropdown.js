$(document).ready(function() {
	$('.dropdown-toggle').dropdown();
	
	$('.dropdown-menu a').click(function(){
		$('.active').toggleClass('active');
		$('.dropdown-toggle').html($(this).html());
		$(this).toggleClass('active');
		$('.inputLanguage').val($(this).html().toLowerCase());
		if($(this).html().toLowerCase() === 'no matter'){
			$('.inputLanguage').val("english german");
		}
	});
});