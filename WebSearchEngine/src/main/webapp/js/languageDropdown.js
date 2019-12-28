$(document).ready(function() {
	$('.language').dropdown();
	
	if($('.dropdown-menu-language a.active').html().toLowerCase() === 'no matter'){
		$('.inputLanguage').val("english german");
	}else{
		$('.inputLanguage').val($('.dropdown-menu-language a.active').html().toLowerCase())
	}
	
	$('.dropdown-menu-language a').click(function(){
		$('.active').toggleClass('active');
		$('.language').html($(this).html());
		$(this).toggleClass('active');
		$('.inputLanguage').val($(this).html().toLowerCase());
		if($(this).html().toLowerCase() === 'no matter'){
			$('.inputLanguage').val("english german");
		}
	});
});