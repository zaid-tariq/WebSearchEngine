<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<html>
<head>
<title>Search Results</title>
<link rel="stylesheet"
	href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
	integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
	crossorigin="anonymous">
<link rel="stylesheet" href="../css/style.css">
</head>
<body class="position-relative">
	<div class="position-absolute" style="top:1em;right:1em">
		<div class="btn-group">
			<a href="/is-project/metasearch/config" type="button" class="btn btn-primary btn-sm">
			   	Config
			</a>
		</div>
	</div>
	<div class="row justify-content-center align-items-center pt-5">
		<div class="card col-5"
			style="background-color: transparent; border: none">
			<div class="card-body text-center">
				<div class="row mb-4">
					<a href="/is-project/index.html" class="col-6 offset-3"> <img
						style="width: inherit; height: auto" src="/images/TUgle.png"
						alt="TUgle">
					</a>
				</div>
				<form action="results" method="get">
					<div class="form-group">
						<input class="form-control" type="text" name="query">
					</div>
					<input type="submit" class="btn btn-primary"
						style="background-color: #006A99; border-color: #047699"
						value="Metasearch">
				</form>
			</div>
		</div>
	</div>
	<div class="container">
		<c:choose>
			<c:when test="${fn:length(response.resultList)==0}">
				<h2 class="text-center">Oops... no results found!</h2>
			</c:when>
			<c:otherwise>
				<table class="table table-borderless table-sm">
					<c:forEach items="${response.resultList}" var="result">
						<tr>
							<td><a href="${result.url}">${result.url}</a></td>
							<td>${result.score}</td>
						</tr>
						<tr>
							<td colspan=2>
								<div class="mb-5" style="display: block; word-wrap: break-word; width: 70vw">
								<small>Source: <i>${result.source}</i></small> 
								</div>
							</td>
						</tr>
					</c:forEach>
				</table>
			</c:otherwise>
		</c:choose>
	</div>

	<script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
	<script
		src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js"
		integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
		crossorigin="anonymous"></script>
	<script
		src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
		integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
		crossorigin="anonymous"></script>
	<script src="../js/results.js"></script>
	<script src="../js/languageDropdown.js"></script>
	<script src="../js/scoringDropdown.js"></script>
</body>
</html>