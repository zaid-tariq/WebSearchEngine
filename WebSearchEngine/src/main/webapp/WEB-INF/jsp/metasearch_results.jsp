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
<!-- 	<div class="position-absolute" style="top: 1em; right: 1em">
		<div class="btn-group">
			<button type="button"
				class="btn btn-primary btn-sm dropdown-toggle scoring-method"
				data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
				${param.scoringMethod == 1 ? 'TF*IDF' : (param.scoringMethod == 2 ? 'BM25' : 'Combined Score')}
			</button>
			<div
				class="dropdown-menu dropdown-menu-scoring-method dropdown-menu-right">
				<a class="dropdown-item ${param.scoringMethod == 1 ? 'active' : ''}"
					href="#" value="1">TF*IDF</a> <a
					class="dropdown-item ${param.scoringMethod == 2 ? 'active' : ''}"
					href="#" value="2">BM25</a> <a
					class="dropdown-item ${param.scoringMethod == 3 ? 'active' : ''}"
					href="#" value="3">Combined Score</a>
			</div>
		</div>
		<div class="btn-group">
			<button type="button"
				class="btn btn-primary btn-sm dropdown-toggle language ${param.lang != 'english' && param.lang != 'german' ? '' : 'capitalize'}"
				data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
				${param.lang != 'english' && param.lang != 'german' ? 'No matter' : param.lang}
			</button>
			<div class="dropdown-menu dropdown-menu-language dropdown-menu-right">
				<a class="dropdown-item ${param.lang == 'english' ? 'active' : ''}"
					href="#">English</a> <a
					class="dropdown-item ${param.lang == 'german' ? 'active' : ''}"
					href="#">German</a> <a
					class="dropdown-item ${param.lang != 'english' && param.lang != 'german' ? 'active' : ''}"
					href="#">No matter</a>
			</div>
		</div>
	</div> -->
	<div class="row justify-content-center align-items-center pt-5">
		<div class="card col-5"
			style="background-color: transparent; border: none">
			<div class="card-body text-center">
				<div class="row mb-4">
					<a href="/is-project/index.html" class="col-6 offset-3"> <img
						style="width: inherit; height: auto" src="../images/TUgle.png"
						alt="TUgle">
					</a>
				</div>
				<form action="results" method="get">
					<div class="form-group">
						<input class="form-control" type="text" name="query">
					</div>
					<input type="submit" class="btn btn-primary"
						style="background-color: #006A99; border-color: #047699"
						value="Suchen">
				</form>
			</div>
		</div>
	</div>
	<div class="container">
		<!-- <form action="results" method="get">
			<input style="display: none" type="text" name="query"
				value="${didYouMean}">
			<c:if test="${not empty didYouMean}">
				<i> Did you mean: <input class="btn btn-link" type="submit"
					value="${didYouMean}"> ? <input type="hidden"
					class="inputLanguage" name="lang" value="english"> <input
					type="hidden" class="scoringMethod" name="scoringMethod" value="3">
				</i>
			</c:if>
		</form> -->
		<c:choose>
			<c:when test="${fn:length(response.resultList)==0}">
				<h2 class="text-center">Oops... no results found!</h2>
			</c:when>
			<c:otherwise>
				<table class="table table-borderless table-sm">
					<c:forEach items="${response.resultList}" var="result">
						<tr>
							<td>${result.rank}</td>
							<td><a href="${result.url}">${result.url}</a></td>
							<td>${result.source}</td>
							<td>${result.score}</td>
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