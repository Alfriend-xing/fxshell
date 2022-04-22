package com.fxtext;

import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class HTMLEditorSample extends javafx.application.Application {
    public JschConfig jschConfig;
    private static Alert alert;
    public static Map<String, HTMLEditorTabPane> editorTabPaneMap = new HashMap<>(100);
    private TabPane tabPane;
    private String INITIAL_TEXT = "Lorem ipsum dolor sit "
            + "amet, consectetur adipiscing elit. Nam tortor felis, pulvinar "
            + "in scelerisque cursus, pulvinar at ante. Nulla consequat"
            + "congue lectus in sodales. Nullam eu est a felis ornare "
            + "bibendum et nec tellus. Vivamus non metus tempus augue auctor "
            + "ornare. Duis pulvinar justo ac purus adipiscing pulvinar. "
            + "<p contenteditable=\"false\" class = \"content-c\">这是一个可编辑的段落。</p>"
            + "Integer congue faucibus dapibus. Integer id nisl ut elit "
            + "aliquam sagittis gravida eu dolor. Etiam sit amet ipsum "
            + "sem.";

    @Override
    public void start(Stage stage) {
        loadConfig();
        stage.setTitle("HTMLEditor Sample");
        stage.setX(600);
        stage.setY(500);
        stage.setWidth(1080);
        stage.setHeight(700);


        tabPane = new TabPane();
        tabPane.setPrefHeight(900);

        ListView<String> listView = new ListView<>();
        ListViewContextMenu listViewContextMenu = new ListViewContextMenu(listView, jschConfig);
        listView.setContextMenu(listViewContextMenu.getCm());
        listView.setPrefSize(300, 250);
        listView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                int index = listView.getSelectionModel().getSelectedIndex();
                if (index == -1) {
                    return;
                }
                log.info("open new tab:" + listView.getSelectionModel().getSelectedItem());
                JschConfig.SessionConfig sessionConfig = jschConfig.getSessionConfigs().get(index);
                Tab tab = new Tab(listView.getSelectionModel().getSelectedItem());
                tab.setId(UUID.randomUUID().toString());
                HTMLEditorTabPane htmlEditorTabPane =
                        new HTMLEditorTabPane(tabPane, tab, sessionConfig.getHost(), sessionConfig.getUser(), sessionConfig.getPasswordAesString());
                editorTabPaneMap.put(tab.getId(), htmlEditorTabPane);
                VBox paneBox = new VBox();
                paneBox.getChildren().addAll(htmlEditorTabPane.getHtmlEditor(), htmlEditorTabPane.getTextArea());
                tab.setContent(paneBox);
                tabPane.getTabs().addAll(tab);
                tabPane.getSelectionModel().selectLast();
            }
        });


        for (JschConfig.SessionConfig sessionConfig : jschConfig.getSessionConfigs()) {
            listView.getItems().addAll(sessionConfig.getSessionName());
        }

        MenuBar menuBar = new MenuBar();
        Menu menuSetting = new Menu("设置");
//        Menu menuEdit = new Menu("编辑");
//        Menu menuView = new Menu("视图");
        CheckMenuItem cmi = new CheckMenuItem("debug");
        cmi.setSelected(false);
        cmi.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> ov, Boolean old_val,
                 Boolean new_val) -> {
                    HTMLEditorOutputCharStream.asniFilterSwitch = !new_val;
                });
        menuSetting.getItems().addAll(cmi, new SeparatorMenuItem());
        menuBar.getMenus().addAll(menuSetting);

//        HBox hBoxButton = new HBox();
//        hBoxButton.getChildren().addAll(button, on, off);
        VBox vBoxRight = new VBox();
        vBoxRight.getChildren().addAll(tabPane);
        HBox hBox = new HBox();
        hBox.getChildren().addAll(listView, vBoxRight);
        VBox vBox = new VBox(menuBar, hBox);
        Scene scene = new Scene(vBox);
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(event -> {
            saveConfig();
            if (tabPane.getTabs().size() == 0) {
                return;
            }
            alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("正在关闭连接");
            alert.show();
            tabPane.getTabs().forEach(tab -> {
                editorTabPaneMap.get(tab.getId()).closeFxSh();
//                ((HTMLEditorTabPane)((VBox)tab.getContent()).getChildren().get(0)).closeFxSh();
            });
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {

            }
            alert.close();
        });
        try {
            String encryptData = FxSh.encrypt("123456");
            String password = FxSh.decrypt(encryptData);
        } catch (Exception e) {
            log.error(null, e);
        }
    }

    public void loadConfig() {
        try {
            jschConfig = getConfig();
        } catch (Exception e) {

        }
//        saveConfig();
    }

    public JschConfig getConfig() {
        jschConfig = null;
        try {
            File file = new File(getCurrentJarDir().getPath(), "JschConfig.ser");
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            jschConfig = (JschConfig) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            log.error("无法找到配置文件");
        } catch (ClassNotFoundException c) {
            log.error("配置文件反序列化错误");
        }
        if (jschConfig == null) {
            jschConfig = new JschConfig();
            jschConfig.setSessionConfigs(new ArrayList<>());
        }
        return jschConfig;
    }

    public void saveConfig() {
        try {
            File file = new File(getCurrentJarDir().getPath(), "JschConfig.ser");
            FileOutputStream fileOut =
                    new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(jschConfig);
            out.close();
            fileOut.close();
            log.info("Serialized data is saved in JschConfig.ser:"+file.getAbsolutePath());
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    private File getCurrentJarDir() {
        try {
            String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
            path = java.net.URLDecoder.decode(path, "UTF-8");
            File file = new File(path);
            if (file.isFile()) {
                return new File(file.getParent());
            } else {
                return file;
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}