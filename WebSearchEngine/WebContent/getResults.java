import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.org.controller.Result;

/**
 * Servlet implementation class getResults
 */
@WebServlet("/results")
public class getResults extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public getResults() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		handleRequest(request, response);
	}

	private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		int k = Integer.valueOf(request.getParameter("k"));
		String query = request.getParameter("query");

		// TODO: request top k result for query

		// TODO: format response to the JSON format --> build JSON object then write it
		// to the response
		request.setAttribute("result", "This is then the result");
		request.getRequestDispatcher(search.jsp).forward(request, response);
	}
}