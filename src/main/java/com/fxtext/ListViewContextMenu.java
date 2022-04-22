package com.fxtext;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ListViewContextMenu {

    private final ContextMenu cm = new ContextMenu();
    private final MenuItem cmItem1 = new MenuItem("添加");
    private final MenuItem cmItem2 = new MenuItem("删除");
    private final MenuItem cmItem3 = new MenuItem("修改");
    private final Dialog<Boolean> confirmDialog = new Dialog<>();
    private final Dialog<JschConfig.SessionConfig> hostEditor = new Dialog<>();

    Label sessionLabel = new Label("sessionName");
    TextField sessionNameText = new TextField();
    Label hostLabel = new Label("hostname");
    TextField hostText = new TextField();
    Label usernameLabel = new Label("username");
    TextField usernameText = new TextField();
    Label passwordLabel = new Label("password");
    PasswordField passwordText = new PasswordField();

    private ListView<String> listView;
    private JschConfig jschConfig;

    public ListViewContextMenu(ListView<String> listView, JschConfig jschConfig) {
        this.jschConfig = jschConfig;
        this.listView = listView;
        initHostEditorDialog();
        initHostAdd();
        initConfirmDialog();
        cmItem1.setOnAction((ActionEvent e) -> {
            hostEditor.setTitle("添加");
            clearHostEdirotContent();
            hostEditor.showAndWait();
            JschConfig.SessionConfig sessionConfig = hostEditor.getResult();
            if (sessionConfig != null) {
                listView.getItems().add(sessionConfig.getSessionName());
                jschConfig.getSessionConfigs().add(sessionConfig);
            }
            clearHostEdirotContent();
        });
        cmItem2.setOnAction((ActionEvent e) -> {
            int sessionIndex = listView.getSelectionModel().getSelectedIndex();
            if (sessionIndex == -1) {
                return;
            }
            JschConfig.SessionConfig sessionConfig = jschConfig.getSessionConfigs().get(sessionIndex);
            confirmDialog.setContentText("确认删除会话：" + sessionConfig.getSessionName() + "吗？");
            confirmDialog.showAndWait();
            if (confirmDialog.getResult() != null && confirmDialog.getResult()) {
                jschConfig.getSessionConfigs().remove(sessionIndex);
                listView.getItems().remove(sessionIndex);
            }
        });
        cmItem3.setOnAction((ActionEvent e) -> {
            int sessionIndex = listView.getSelectionModel().getSelectedIndex();
            if (sessionIndex == -1) {
                return;
            }
            JschConfig.SessionConfig sessionConfig = jschConfig.getSessionConfigs().get(sessionIndex);
            hostEditor.setTitle("编辑：" + sessionConfig.getSessionName());
            setHostEditorContent(sessionConfig);
            hostEditor.showAndWait();
            JschConfig.SessionConfig sessionConfigUpdate = hostEditor.getResult();
            if (sessionConfigUpdate != null) {
                listView.getItems().remove(sessionIndex);
                listView.getItems().add(sessionIndex, sessionConfigUpdate.getSessionName());
                jschConfig.getSessionConfigs().remove(sessionIndex);
                jschConfig.getSessionConfigs().add(sessionIndex, sessionConfigUpdate);
            }
            clearHostEdirotContent();
        });
        cm.getItems().addAll(cmItem1, cmItem3, cmItem2);
    }

    private void initConfirmDialog() {
        confirmDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        confirmDialog.setResultConverter(bt -> {
            if (bt.equals(ButtonType.OK)) {
                return true;
            } else {
                return false;
            }
        });
    }

    private void initHostEditorDialog() {
        hostEditor.setTitle("修改连接");
        hostEditor.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        HBox sessionHbox = new HBox();
        sessionHbox.getChildren().addAll(sessionLabel, sessionNameText);
        sessionHbox.setPrefSize(300, 30);
        HBox hostHbox = new HBox();
        hostHbox.getChildren().addAll(hostLabel, hostText);
        hostHbox.setPrefSize(300, 30);
        HBox usernameHbox = new HBox();
        usernameHbox.setPrefSize(300, 30);
        usernameHbox.getChildren().addAll(usernameLabel, usernameText);
        HBox passwordHbox = new HBox();
        passwordHbox.setPrefSize(300, 30);
        passwordHbox.getChildren().addAll(passwordLabel, passwordText);
        VBox vbox = new VBox();
        vbox.getChildren().addAll(sessionHbox, hostHbox, usernameHbox, passwordHbox);
        hostEditor.getDialogPane().setContent(vbox);
        hostEditor.setResultConverter(b -> {
            if (b.equals(ButtonType.OK)) {
                log.info(hostText.getText());
                JschConfig.SessionConfig sessionConfig = new JschConfig.SessionConfig();
                sessionConfig.setSessionName(sessionNameText.getText());
                sessionConfig.setHost(hostText.getText());
                sessionConfig.setUser(usernameText.getText());
                sessionConfig.setPasswordAesString(passwordText.getText());
                return sessionConfig;
            }
            return null;
        });
    }

    private void setHostEditorContent(JschConfig.SessionConfig sessionConfig) {
        sessionNameText.setText(sessionConfig.getSessionName());
        hostText.setText(sessionConfig.getHost());
        usernameText.setText(sessionConfig.getUser());
        passwordText.setText(sessionConfig.getPasswordAesString());
    }

    private void clearHostEdirotContent() {
        hostEditor.setResult(null);
        sessionNameText.setText(null);
        hostText.setText(null);
        usernameText.setText(null);
        passwordText.setText(null);
    }

    private void initHostAdd() {

    }
}
