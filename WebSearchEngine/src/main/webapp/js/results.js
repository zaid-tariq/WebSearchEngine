$(document).ready(function() {
    $(".tr").on("click",function() {
    	console.log("HELLO");
        window.document.location = $(this).data("href");
    });
});