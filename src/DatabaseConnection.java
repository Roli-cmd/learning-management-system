import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // 1. Ensure your port (e.g., 3306 or 3307) matches your MySQL server setup
    private static final String URL = "jdbc:mysql://localhost:3306/lms_java";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {
            // Explicitly load the MySQL CJ driver from your JAR
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found in classpath.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}