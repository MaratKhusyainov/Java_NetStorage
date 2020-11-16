import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class DB_connection {
    private static DB_connection instance;
    private Connection conn;

    private DB_connection() throws SQLException {
        ResourceBundle rb = ResourceBundle.getBundle("db");
        String host = rb.getString("host");
        String port = rb.getString("port");
        String db = rb.getString("db");
        String user = rb.getString("user");
        String password = rb.getString("password");

        String jdbcURL = MessageFormat.format("jdbc:mysql://{0}:{1}/{2}", host, port, db);
        conn = DriverManager.getConnection(jdbcURL, user, password);
    }

    public static DB_connection getInstance() {
        if (instance == null) {
            try {
                instance = new DB_connection();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public Connection connection() {
        return conn;
    }
}
