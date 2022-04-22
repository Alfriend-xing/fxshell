package com.fxtext;

import javafx.scene.control.ToolBar;
import javafx.scene.layout.GridPane;
import javafx.scene.web.HTMLEditor;

public class HTMLEditorRemoveTabRunnable extends Thread {

    private HTMLEditor htmlEditor;

    public HTMLEditorRemoveTabRunnable(HTMLEditor htmlEditor) {
        this.htmlEditor = htmlEditor;
    }

    @Override
    public void run() {

        ((ToolBar) ((GridPane) htmlEditor.lookup(".grid")).getChildren().get(0)).getItems().removeAll();
        ToolBar topToolBar = (ToolBar) htmlEditor.lookup(".top-toolbar");
        ToolBar bottomToolBar = (ToolBar) htmlEditor.lookup(".bottom-toolbar");
        Integer a = topToolBar.getItems().size();
        Integer b = bottomToolBar.getItems().size();
        topToolBar.getItems().remove(0, a);
        bottomToolBar.getItems().remove(0, b);
//        ((GridPane)htmlEditor.lookup(".grid")).getChildren().remove(0);
//        ToolBar bottomToolBar1 = (ToolBar) htmlEditor.lookup(".grid");
    }
}
