<html>
	<head>
		<title>Search Results</title>
	</head>
	<body>
		<div style='text-align: center;'>
			<table>
		       <c:forEach items="${results}" var="result">
		            <tr>
		                <td>${result.rank}</td>
		                <td>${result.url}/></td>
		                <td>${result.score}/></td>  
		            </tr>
		        </c:forEach>
		    </table>		 
	    </div>
	</body>
</html>