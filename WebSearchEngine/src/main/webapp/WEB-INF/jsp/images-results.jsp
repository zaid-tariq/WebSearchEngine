<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<html>
<head>
<title>Search Results</title>
<link rel="stylesheet"
	href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
	integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
	crossorigin="anonymous">
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/css/style.css">
</head>
<body class="position-relative">
	<div class="modal fade" id="myModal" role="dialog">
		<div class="modal-dialog modal-xl">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal">
						<span aria-hidden="true">&times;</span>
					</button>
				</div>
				<div class="modal-body text-center">
					<img id="bigImage" src="" class="img-fluid">
				</div>
			</div>
		</div>
	</div>
	<div class="position-absolute" style="top: 1em; right: 1em">
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
	</div>
	<div class="row justify-content-center align-items-center pt-5">
		<div class="card col-5"
			style="background-color: transparent; border: none">
			<div class="card-body text-center">
				<div class="row mb-4">
					<a href="/is-project/images.html" class="col-6 offset-3"> <img
						style="width: inherit; height: auto"
						src="../images/TUgleImages.png" alt="TUgle">
					</a>
				</div>
				<form action="images-results" method="get">
					<div class="form-group">
						<input class="form-control" type="text" name="query">
					</div>
					<input type="hidden" class="inputLanguage" name="lang"
						value="english"> <input type="submit"
						class="btn btn-primary"
						style="background-color: #006A99; border-color: #047699"
						value="Suchen">
				</form>
			</div>
		</div>
	</div>
	<div class="container">
		<form action="results" method="get">
			<input style="display: none" type="text" name="query"
				value="${didYouMean}">
			<c:if test="${not empty didYouMean}">
				<i> Did you mean: <input class="btn btn-link" type="submit"
					value="${didYouMean}"> ? <input type="hidden"
					class="inputLanguage" name="lang" value="english">
				</i>
			</c:if>
		</form>
		<c:choose>
			<c:when test="${fn:length(results)==0}">
				<h2 class="text-center">Oops... no results found!</h2>
			</c:when>
			<c:otherwise>
				<c:forEach varStatus="loop" items="${results}" var="result">
					<c:if test="${loop.index % 4 == 0}">${'<div class= \"row\">'}</c:if>
					<div class="col-3">
						<div class="card">
							<img class="smallImg card-img-top" src="${result.url}"
								style="height: auto">
							<div class="card-body">
								<p>Score: ${result.score}</p>
								<a href="${result.url2}">View Website</a>
							</div>
						</div>
					</div>
					<c:if test="${loop.index % 4 == 3}">${'</div>'}</c:if>
				</c:forEach>
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
	<script type="text/javascript"
		src="<%=request.getContextPath()%>/js/languageDropdown.js"></script>
	<script type="text/javascript"
		src="<%=request.getContextPath()%>/js/images.js"></script>
</body>
</html>