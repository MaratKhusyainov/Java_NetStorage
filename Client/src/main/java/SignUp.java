import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SignUp implements Initializable {
    public TextField nick;
    public TextField login;
    public PasswordField password;
    public Button signUp;
    public VBox Box;

    public void signUpAction(ActionEvent actionEvent) {
        Network.getInstance().sendMessage(new CommandMessage(Command.SIGN_UP, nick.getText(),
        login.getText(), password.getText()));
        login.clear();
        password.clear();
        nick.clear();
    }

    public void buttonExit() {
        Network.getInstance().close();
        Platform.exit();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        new Thread(() -> Network.getInstance().getClientHandler().setCallback(o -> {
            if (o instanceof CommandMessage) {
                CommandMessage cm = (CommandMessage) o;
                if (cm.getCommand() == Command.SIGN_UP) {
                    if (cm.getParam().equals("true")) {
                        Platform.runLater(() -> {
                            Box.getScene().getWindow().hide();
                            FXMLLoader loader = new FXMLLoader();
                            loader.setLocation(getClass().
                                    getResource("main.fxml"));
                            try {
                                loader.load();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Parent root = loader.getRoot();
                            Stage stage = new Stage();
                            stage.setScene(new Scene(root));
                            stage.initStyle(StageStyle.UNDECORATED);
                            stage.show();
                        });
                    } else {
                        login.appendText("Login is busy");
                        nick.appendText("Nick is busy");
                    }
                }
            }
        })).start();
    }
}
