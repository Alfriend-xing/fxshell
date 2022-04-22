package com.fxtext;

import javafx.scene.control.TextArea;

public class TextAreaUpdateThread extends Thread {

    private TextArea textArea;

    public TextAreaUpdateThread(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void run() {
        textArea.setEditable(true);
        textArea.requestFocus();
    }
}
