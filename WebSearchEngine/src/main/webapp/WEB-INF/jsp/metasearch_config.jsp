<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<html>
<head>
<title>Meta Search Engine: Config</title>
<link rel="stylesheet"
	href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
	integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
	crossorigin="anonymous">
<link rel="stylesheet" href="../css/style.css">
</head>
<body>
	<div style="top:1em;float: right;right:1em;position: absolute;">
		<div class="btn-group">
			<a href="/is-project/metasearch/" type="button" class="btn btn-primary btn-sm">
			   	Metasearch Page
			</a>
		</div>
	</div>
	
	<div class="container" style="margin-top: 10%;">
		<div>
			<b>Add New Search Engine:</b>
			<form action="config" method="get" class="row">
				<input class="form-control col-sm-6" placeholder="Enter URL" type="text" name="url" value="${engine.url}">
				<input style="display: none" type="text" name="action" value="add">
				<input class="btn-sm btn-primary" type="submit" value="Save"> 
			</form>
		</div>
		<c:choose>
			<c:when test="${fn:length(results.urls)==0}">
				<b class="text-center">There are currently no search engines in the DB</b>
			</c:when>
			<c:otherwise>
				<b>Saved Search Engines:</b>
				<table class="table table-borderless table-sm">
					<c:forEach items="${results.urls}" var="engine">
						<tr>
							<td><a href="${engine.url}">${engine.url}</a></td>
							<td>
								<form action="config" method="get">
									<input style="display: none" type="text" name="url" value="${engine.url}">
									<input style="display: none" type="text" name="action" value="delete">
									<input class="btn btn-link" type="submit" value="Delete"> 
								</form>
							</td>
							<td>
								<form action="config" method="get">
									<input style="display: none" type="text" name="url" value="${engine.url}">
									<input style="display: none" type="text" name="action" value="${engine.enabled?'disable':'enable'}">
									<input class="btn btn-link" type="submit"  value="${engine.enabled?'Disable':'Enable'}"> 
								</form>
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