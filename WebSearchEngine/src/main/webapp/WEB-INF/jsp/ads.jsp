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
<div class="container h-100">
    <div class="row justify-content-center mt-5 mb-5">
    	<a href="/is-project/ads.html" class="col-5">
        	<img style="width:inherit;height:auto" src="../images/TUgle.png" alt="TUgle">
        </a>
    </div>
    <div class="row justify-content-center">
    	<form class="col-6" action="ad-add" name="adForm" method="post">
    		<div class="form-row mb-3">
	    		<div class="col">
	    			<label for="pricePerClick">Price per click (&dollar;)</label>
	    			<input type="number" step="0.01" class="form-control" name="pricePerClick" id="pricePerClick" required>
	    		</div>
	    		<div class="col">
	    			<label for="totalBudget">Budget (&dollar;)</label>
	    			<input type="number" step="0.01" class="form-control" name="totalBudget" id="totalBudget" required>
	    		</div>
	    	</div>
	    	<div class="form-row mb-3">
	    		<div class="col">
	    			<label for="url">URL</label>
	    			<input type="text" class="form-control" name="url" id="url" required>
	    		</div>
	    		<div class="col">
	    			<label for="imageURL">Image URL (optional)</label>
	    			<input type="text" class="form-control" name="imageURL" id="imageURL">	
	    		</div>
	    	</div>
	    	<div class="form-row mb-3">
	    		<div class="col">
	    			<label for="ngrams">N-grams ({[database course],[informatik kaiserslautern]})</label>
	    			<textarea class="form-control" rows="2" name="ngrams" id="ngrams" required></textarea>
	    		</div>
	    	</div>
	    	<div class="form-row mb-3">
	    		<div class="col">
	    			<label for="description">Ad text</label>
	    			<textarea class="form-control" rows="2" name="description" id="description" required></textarea>
	    		</div>
	    	</div>
	    	<div class="form-row">
	    		<div class="col text-center">
	    			<button type="submit" class="btn btn-primary text-center">Submit</button>
	    		</div>
	    	</div>
		</form>
    </div>
    <script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js"
            integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
            crossorigin="anonymous"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
            integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
            crossorigin="anonymous"></script>
</div>
</body>
</html>