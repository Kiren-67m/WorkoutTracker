package itc370.workout;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
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
            ResultSet rs;
            PreparedStatement stFilter;
            if (filterType != null && !filterType.trim().isEmpty()) {
                stFilter = con.prepareStatement(
                    "SELECT * FROM workout WHERE workout_type = ? ORDER BY workout_date DESC");
                stFilter.setString(1, filterType);
            } else {
                filterType = "";
                stFilter = con.prepareStatement(
                    "SELECT * FROM workout ORDER BY workout_date DESC");
            }
            rs = stFilter.executeQuery();

            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Workout Tracker</title>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            out.println("<style>");
            out.println("* { box-sizing: border-box; margin: 0; padding: 0; }");
            out.println("body { font-family: Arial, sans-serif; background-color: #f4f4f4; color: #333333; }");
            out.println("header { background-color: #222222; color: #ffffff; padding: 20px 40px; }");
            out.println("header h1 { font-size: 24px; letter-spacing: 1px; }");
            out.println(".container { max-width: 960px; margin: 30px auto; padding: 0 20px; }");
            out.println(".summary-bar { background-color: #ffffff; border-left: 4px solid #555555; padding: 14px 20px; margin-bottom: 24px; font-size: 15px; color: #444444; }");
            out.println(".top-actions { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }");
            out.println(".btn { display: inline-block; padding: 9px 20px; background-color: #333333; color: #ffffff; text-decoration: none; font-size: 14px; border: none; cursor: pointer; }");
            out.println(".btn:hover { background-color: #555555; }");
            out.println(".btn-light { background-color: #ffffff; color: #333333; border: 1px solid #cccccc; }");
            out.println(".btn-light:hover { background-color: #eeeeee; }");
            out.println(".filter-form { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }");
            out.println(".filter-form label { font-size: 14px; color: #555555; }");
            out.println(".filter-form input[type='text'] { padding: 8px 12px; border: 1px solid #cccccc; font-size: 14px; width: 180px; }");
            out.println("table { width: 100%; border-collapse: collapse; background-color: #ffffff; font-size: 14px; }");
            out.println("thead { background-color: #333333; color: #ffffff; }");
            out.println("thead th { padding: 12px 14px; text-align: left; font-weight: normal; letter-spacing: 0.5px; }");
            out.println("tbody tr { border-bottom: 1px solid #e0e0e0; }");
            out.println("tbody tr:hover { background-color: #f9f9f9; }");
            out.println("tbody td { padding: 11px 14px; vertical-align: middle; }");
            out.println(".action-links a { color: #333333; text-decoration: none; font-size: 13px; margin-right: 10px; border-bottom: 1px solid #aaaaaa; }");
            out.println(".action-links a:hover { color: #000000; border-bottom-color: #333333; }");
            out.println(".action-links .delete-link { color: #888888; border-bottom-color: #cccccc; }");
            out.println(".empty-msg { text-align: center; padding: 30px; color: #aaaaaa; font-size: 14px; }");
            out.println("footer { text-align: center; padding: 30px; font-size: 12px; color: #aaaaaa; margin-top: 40px; }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");

            out.println("<header><h1>Workout Tracker</h1></header>");
            out.println("<div class='container'>");

            out.println("<div class='summary-bar'>This week's workouts: <strong>" + weekCount + "</strong></div>");

            out.println("<div class='top-actions'>");
            out.println("<a href='index.html' class='btn'>+ Add New Workout</a>");
            out.println("<form class='filter-form' action='WorkoutServlet' method='get'>");
            out.println("<input type='hidden' name='action' value='list'>");
            out.println("<label>Filter by type:</label>");
            out.println("<input type='text' name='filter_type' value='" + filterType + "' placeholder='e.g. Running'>");
            out.println("<button type='submit' class='btn'>Filter</button>");
            out.println("<a href='WorkoutServlet' class='btn btn-light'>Clear</a>");
            out.println("</form>");
            out.println("</div>");

            out.println("<table>");
            out.println("<thead><tr><th>ID</th><th>Date</th><th>Type</th><th>Duration (min)</th><th>Notes</th><th>Action</th></tr></thead>");
            out.println("<tbody>");

            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                int id = rs.getInt("id");
                String notesVal = rs.getString("notes");
                if (notesVal == null) notesVal = "";
                out.println("<tr>");
                out.println("<td>" + id + "</td>");
                out.println("<td>" + rs.getDate("workout_date") + "</td>");
                out.println("<td>" + rs.getString("workout_type") + "</td>");
                out.println("<td>" + rs.getInt("duration_minutes") + "</td>");
                out.println("<td>" + notesVal + "</td>");
                out.println("<td class='action-links'>");
                out.println("<a href='edit.html?id=" + id
                    + "&workout_date=" + rs.getDate("workout_date")
                    + "&workout_type=" + URLEncoder.encode(rs.getString("workout_type"), "UTF-8")
                    + "&duration_minutes=" + rs.getInt("duration_minutes")
                    + "&notes=" + URLEncoder.encode(notesVal, "UTF-8")
                    + "'>Edit</a>");
                out.println("<a href='WorkoutServlet?action=delete&id=" + id + "' class='delete-link' onclick=\"return confirm('Delete this entry?')\">Delete</a>");
                out.println("</td>");
                out.println("</tr>");
            }

            if (!hasRows) {
                out.println("<tr><td colspan='6' class='empty-msg'>No workouts found. Add your first entry!</td></tr>");
            }

            out.println("</tbody></table>");
            out.println("</div>");
            out.println("</body></html>");

            rs.close();
            stFilter.close();
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