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
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    public VBox auth;
    public TextField login;
    public PasswordField password;
    public Button signUpBtn;
    public VBox storage;
    public TableView<FileManager> clientTable;
    public TextField pathFieldLeft;
    public TableView<FileManager> storageTable;
    public TextField pathFieldRight;


    private Path path = Paths.get("Client", "ClientStorage");
    private final String PATH = path.toString();
    private ContextMenu contextMenu = new ContextMenu();
    private String currentUserNick;
    private boolean isAuthorized;


    private void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if (!isAuthorized) {
            auth.setVisible(true);
            auth.setManaged(true);
            storage.setVisible(false);
            storage.setManaged(false);
        } else {
            auth.setVisible(false);
            auth.setManaged(false);
            storage.setVisible(true);
            storage.setManaged(true);
        }
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {
        Network.getInstance();
        setAuthorized(false);
        TableInitializer.initialize(clientTable, "Client files");
        TableInitializer.initialize(storageTable, "Storage files");
        updateClientDirectory();
        requestToUpdateStorageDirectory();
        updateClientFilesList(Paths.get(PATH));
        initializeDeleteFile();
        initializeRenameFile();
        initializeCreateDir();
        new Thread(() -> Network.getInstance().getClientHandler().setCallback(o -> {
            if (o instanceof ListMessage) {
                Platform.runLater(() -> {
                    ListMessage lm = (ListMessage) o;
                    storageTable.getItems().clear();
                    List<FileManager> list = lm.getFilesList();
                    list.forEach(f -> storageTable.getItems().add(f));
                    list.forEach(s->pathFieldRight.setText(s.getPath()));
                });
            }
            if (o instanceof FileMessage) {
                try {
                    FileMessage fm = (FileMessage) o;
                    Files.write(Paths.get(pathFieldLeft.getText(), fm.getName()), fm.getData(), StandardOpenOption.CREATE);
                    updateClientFilesList(Path.of(pathFieldLeft.getText()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (o instanceof CommandMessage) {
                CommandMessage cm = (CommandMessage) o;
                switch (cm.getCommand()) {
                    case AUTH -> {
                        setAuthorized(true);
                        currentUserNick = ((CommandMessage) o).getParam();
                        requestToUpdateStorageFilesList(currentUserNick);
                    }
                    case STORAGE_FILES_LIST -> requestToUpdateStorageFilesList(cm.getParam());
                    case DIRECTORY_FILES_LIST -> requestToUpdateStorageDirectoryList();
                }
            }
        })).start();
    }

    private void updateClientFilesList(Path path) {
        try {
            pathFieldLeft.setText(path.normalize().toAbsolutePath().toString());
            clientTable.getItems().clear();
            clientTable.getItems().addAll(Files.list(path).map(FileManager::new)
                    .collect(Collectors.toList()));
            clientTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Unable to update list of files.", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void requestToUpdateStorageFilesList(String s) {
        System.out.println(s);
            Network.getInstance().sendMessage(new CommandMessage(Command.STORAGE_FILES_LIST, s));
       
    }


    private void requestToUpdateStorageDirectoryList() {
        Network.getInstance().sendMessage(new CommandMessage(Command.DIRECTORY_FILES_LIST));
    }

    private void updateClientDirectory() {
        clientTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Path path = Paths.get(pathFieldLeft.getText()).resolve(clientTable.getSelectionModel()
                        .getSelectedItem().getFileName());
                if (Files.isDirectory(path)) {
                    updateClientFilesList(path);
                }
            }
        });
    }

    private String getSelectedStorageDirectoryName() {
        if (!storageTable.isFocused()) {
            return null;
        }
        return pathFieldRight.getText() + "\\" + storageTable.getSelectionModel().getSelectedItem().getDirectoryName();
    }

    private void requestToUpdateStorageDirectory() {
        storageTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (getSelectedStorageDirectoryName() != null) {
                    Network.getInstance().sendMessage(new CommandMessage(Command.FILE_DIR, getSelectedStorageDirectoryName()));
                }
            }
        });
    }

    private String getSelectedFileName(TableView<FileManager> table) {
        if (!table.isFocused()) {
            return null;
        }
        return table.getSelectionModel().getSelectedItem().getFileName();
    }

    private String getSelectedTable(TableView<FileManager> table) {
        if (!table.isFocused()) {
            return null;
        }
        return table.getSelectionModel().toString();
    }

    private String getCurrentPath(TextField pathField) {
        return pathField.getText();
    }

    public void uploadFile(ActionEvent actionEvent) throws IOException {
        if (getSelectedFileName(clientTable) != null) {
            Network.getInstance().sendMessage(new FileMessage(Paths.get(getCurrentPath(pathFieldLeft), getSelectedFileName(clientTable)), pathFieldRight.getText()));
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No file selected", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void downloadFile() {
        if (getSelectedFileName(storageTable) != null) {
            Network.getInstance().sendMessage(new CommandMessage(Command.FILE_REQUEST, getCurrentPath(pathFieldRight), getSelectedFileName(storageTable)));
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No file selected", ButtonType.OK);
            alert.showAndWait();
        }
        updateClientFilesList(Paths.get(getCurrentPath(pathFieldLeft)));
    }

    private void initializeDeleteFile() {
        MenuItem deleteItem = new MenuItem("Delete file");
        deleteItem.setOnAction(actionEvent -> {
            if (getSelectedFileName(storageTable) != null) {
                Network.getInstance().sendMessage(new CommandMessage(Command.FILE_DELETE, pathFieldRight.getText() + "\\" + getSelectedFileName(storageTable)));
            }
            if (getSelectedFileName(clientTable) != null) {
                Path path = Paths.get(getCurrentPath(pathFieldLeft), getSelectedFileName(clientTable));
                try {
                    Files.delete(path);
                    updateClientFilesList(Paths.get(getCurrentPath(pathFieldLeft)));
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Failed to delete file.", ButtonType.OK);
                    alert.showAndWait();
                }
            }
        });
        contextMenu.getItems().add(deleteItem);
        requestShow();
    }

    private void initializeCreateDir() {
        MenuItem deleteItem = new MenuItem("Create dir");
        deleteItem.setOnAction(actionEvent -> {
            if (getSelectedTable(storageTable) != null) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Create dir");
                dialog.setContentText("Directory name: ");
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(s -> Network.getInstance().sendMessage(new CommandMessage(Command.CREATE_DIR, pathFieldRight.getText(), s)));
            }
            if (getSelectedTable(clientTable) != null) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Create dir");
                dialog.setContentText("Directory name: ");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    try {
                        createDir(Paths.get((getCurrentPath(pathFieldLeft) + "/")), result.get());
                        updateClientFilesList(Paths.get(getCurrentPath(pathFieldLeft)));
                    } catch (IOException e) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Failed to rename file.", ButtonType.OK);
                        alert.showAndWait();

                    }
                }

            }
        });
        contextMenu.getItems().add(deleteItem);
        requestShow();
    }

    private void initializeRenameFile() {
        MenuItem renameItem = new MenuItem("Rename file");
        renameItem.setOnAction(actionEvent -> {
            if (getSelectedFileName(storageTable) != null) {
                TextInputDialog dialog = new TextInputDialog(getSelectedFileName(storageTable));
                dialog.setTitle("Rename file");
                dialog.setContentText("New file name: ");
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(value -> Network.getInstance().sendMessage(new CommandMessage(Command.FILE_RENAME, getSelectedFileName(storageTable), value, pathFieldRight.getText())));
            }
            if (getSelectedFileName(clientTable) != null) {
                TextInputDialog dialog = new TextInputDialog(getSelectedFileName(clientTable));
                dialog.setTitle("Rename file");
                dialog.setContentText("New file name: ");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    try {
                        renameFile(Paths.get((getCurrentPath(pathFieldLeft) + "/" + getSelectedFileName(clientTable))), result.get());
                        updateClientFilesList(Paths.get(getCurrentPath(pathFieldLeft)));
                    } catch (IOException e) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Failed to rename file.", ButtonType.OK);
                        alert.showAndWait();
                    }
                }

            }
        });
        contextMenu.getItems().add(renameItem);
        requestShow();
    }

    private void requestShow() {
        storageTable.setOnContextMenuRequested(event -> contextMenu.show(storageTable, event.getScreenX(), event.getScreenY()));
        clientTable.setOnContextMenuRequested(event -> contextMenu.show(clientTable, event.getScreenX(), event.getScreenY()));
    }


    private void renameFile(Path path, String newFileName) throws IOException {
        Path newName = path.resolveSibling(newFileName);
        Files.move(path, newName, StandardCopyOption.REPLACE_EXISTING);
    }

    private void createDir(Path path, String dirName) throws IOException {
        Files.createDirectory(Path.of(path.toString() + "/" + dirName));
    }

    public void buttonExit() {
        Network.getInstance().close();
        Platform.exit();
    }

    public void clientButtonUp() {
        Path path = Paths.get(pathFieldLeft.getText()).getParent();
        if (path != null) {
            updateClientFilesList(path);
        }
    }

    public void storageButtonUp() {
        Path path = Paths.get(pathFieldRight.getText()).getParent();
        if (path != null && path.getNameCount() > 2) {
            requestToUpdateStorageFilesList(path.toString());
        }
    }

    public void logInAction() {
        Network.getInstance();
        Network.getInstance().sendMessage(new CommandMessage(Command.AUTH, login.getText(), password.getText()));
        login.clear();
        password.clear();
    }

    public void setSignUp() {
        signUpBtn.getScene().getWindow().hide();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().
                getResource("signUp.fxml"));
        try {
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Parent root = loader.getRoot();
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.showAndWait();
    }
}
