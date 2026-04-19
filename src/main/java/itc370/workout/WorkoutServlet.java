package itc370.workout;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/WorkoutServlet")
public class WorkoutServlet extends HttpServlet {

    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {

        try {
            String dbName = "jdbc:postgresql://localhost/workout_tracker";
            Connection con = DriverManager.getConnection(dbName, "postgres", "19970327");

            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            String action = request.getParameter("action");
            if (action == null) action = "list";

            if (action.equals("add")) {
                String date = request.getParameter("workout_date");
                String type = request.getParameter("workout_type");
                String duration = request.getParameter("duration_minutes");
                String notes = request.getParameter("notes");
                String sql = "INSERT INTO workout (workout_date, workout_type, duration_minutes, notes) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setDate(1, Date.valueOf(date));
                ps.setString(2, type);
                ps.setInt(3, Integer.parseInt(duration));
                ps.setString(4, notes);
                ps.executeUpdate();
                ps.close();

            } else if (action.equals("delete")) {
                int id = Integer.parseInt(request.getParameter("id"));
                PreparedStatement ps = con.prepareStatement("DELETE FROM workout WHERE id = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
                ps.close();

            } else if (action.equals("edit")) {
                int id = Integer.parseInt(request.getParameter("id"));
                String date = request.getParameter("workout_date");
                String type = request.getParameter("workout_type");
                String duration = request.getParameter("duration_minutes");
                String notes = request.getParameter("notes");
                String sql = "UPDATE workout SET workout_date=?, workout_type=?, duration_minutes=?, notes=? WHERE id=?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setDate(1, Date.valueOf(date));
                ps.setString(2, type);
                ps.setInt(3, Integer.parseInt(duration));
                ps.setString(4, notes);
                ps.setInt(5, id);
                ps.executeUpdate();
                ps.close();
            }

            // weekly summary
            Statement stWeek = con.createStatement();
            ResultSet rsWeek = stWeek.executeQuery(
                "SELECT COUNT(*) as cnt FROM workout WHERE workout_date >= CURRENT_DATE - INTERVAL '7 days'"
            );
            int weekCount = 0;
            if (rsWeek.next()) {
                weekCount = rsWeek.getInt("cnt");
            }
            rsWeek.close();
            stWeek.close();

            // filter by workout type
            String filterType = request.getParameter("filter_type");
            String query;
            if (filterType != null && !filterType.trim().isEmpty()) {
                query = "SELECT * FROM workout WHERE workout_type = '" + filterType + "' ORDER BY workout_date DESC";
            } else {
                filterType = "";
                query = "SELECT * FROM workout ORDER BY workout_date DESC";
            }
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query);

            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html><head><title>Workout Tracker</title></head><body>");
            out.println("<h2>Workout Tracker</h2>");
            out.println("<p>This week's workouts: " + weekCount + "</p>");
            out.println("<a href='index.html'>Add New Workout</a><br><br>");

            // filter form
            out.println("<form action='WorkoutServlet' method='get'>");
            out.println("<input type='hidden' name='action' value='list'>");
            out.println("Filter by type: <input type='text' name='filter_type' value='" + filterType + "'>");
            out.println("<input type='submit' value='Filter'>");
            out.println("<a href='WorkoutServlet' style='margin-left:10px'>Clear</a>");
            out.println("</form><br>");

            out.println("<table border='1'>");
            out.println("<tr><th>ID</th><th>Date</th><th>Type</th><th>Duration (min)</th><th>Notes</th><th>Action</th></tr>");
            while (rs.next()) {
                int id = rs.getInt("id");
                out.println("<tr>");
                out.println("<td>" + id + "</td>");
                out.println("<td>" + rs.getDate("workout_date") + "</td>");
                out.println("<td>" + rs.getString("workout_type") + "</td>");
                out.println("<td>" + rs.getInt("duration_minutes") + "</td>");
                out.println("<td>" + rs.getString("notes") + "</td>");
                out.println("<td><a href='edit.html?id=" + id
                    + "&workout_date=" + rs.getDate("workout_date")
                    + "&workout_type=" + rs.getString("workout_type")
                    + "&duration_minutes=" + rs.getInt("duration_minutes")
                    + "&notes=" + rs.getString("notes")
                    + "'>Edit</a> | <a href='WorkoutServlet?action=delete&id=" + id + "'>Delete</a></td>");
                out.println("</tr>");
            }
            out.println("</table>");
            out.println("</body></html>");

            rs.close();
            st.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
            res.getWriter().write("Error: " + e.getMessage());
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}