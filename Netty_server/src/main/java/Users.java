import

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Users {
    public List<UserEntity> usersList;
    private PreparedStatement ps;

    public Users() {
    }


    public String getNick(String login, String password) throws SQLException {
        String select = String.format("SELECT nick FROM users WHERE login = '%s' and password = '%s'", login, password);
        try {
            ps = DB_connection.getInstance()
                    .connection()
                    .prepareStatement(select);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                String s = resultSet.getString(1);
                ps.close();
                return s;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ps.close();
        return null;
    }

    public String addNick(String nick, String login, String password) {
        try {
            ps = DB_connection.getInstance()
                    .connection()
                    .prepareStatement("INSERT INTO users (nick, login, password) VALUES (?,?,?)");
            ps.setString(1, nick);
            ps.setString(2, login);
            ps.setString(3, password);
            ps.executeUpdate();
            ps.close();
            return "true";
        } catch (SQLException s) {
            s.printStackTrace();
            return "false";
        }
    }

    public class UserEntity {
        private String login;
        private String password;
        private String rooth;

        public void setLogin(String login) {
            this.login = login;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setRooth(String rooth) {
            this.rooth = rooth;
        }

    }
}
