<!DOCTYPE html>
<html lang="en" class="h-100">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>TUgle </title>

    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
          integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
    <link rel="stylesheet" href="../css/style.css">
</head>
<body class="h-100">
<div class="container-fluid h-100 position-relative">
	<div class="position-absolute" style="top:1em;right:1em">
		<div class="btn-group">
		  	<button type="button" class="btn btn-primary btn-sm dropdown-toggle language" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
		    	English
		  	</button>
		  	<div class="dropdown-menu dropdown-menu-language dropdown-menu-right">
		    	<a class="dropdown-item active" href="#">English</a>
  				<a class="dropdown-item" href="#">German</a>
  				<a class="dropdown-item" href="#">No matter</a>
		  	</div>
		</div>
	</div>
    <div class="row justify-content-center align-items-center h-75">
        <div class="card col-5" style="background-color:transparent;border:none">
            <div class="card-body text-center">
            	<div class="row mb-4">
            		<a href="/is-project/index.html" class="col-6 offset-3">
            			<img style="width:inherit;height:auto" src="../images/TUgleImages.png" alt="TUgle">
            		</a>
            	</div>                
            	<form action="images-results" method="get">
                    <div class="form-group">
                        <input class="form-control" type="text" name="query">
                    </div>
                    <input type="hidden" class="inputLanguage" name="lang" value="english">
                    <input type="submit" class="btn btn-primary" value="Suchen">
                </form>
            </div>
        </div>
    </div>
    <script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js"
            integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
            crossorigin="anonymous"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
            integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
            crossorigin="anonymous"></script>
    <script src="../js/languageDropdown.js"></script>
</div>
</body>
</html>