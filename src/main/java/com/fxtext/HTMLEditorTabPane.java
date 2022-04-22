package com.fxtext;

import com.sun.javafx.scene.web.skin.HTMLEditorSkin;
import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.WebPage;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HTMLEditorTabPane {
    private TabPane tabPane;
    private Tab tab;
    private String host;
    private String user;
    private String pwd;
    private WebView webView;
    private WebPage webPage;
    private FxSh fxSh;
    private HTMLEditorInputQueueStream htmlEditorInputQueueStream;
    private TextArea textArea;
    private HTMLEditor htmlEditor;
    private Boolean fullScreenInsertFlag = null;
    private static Map<Character, Character> shiftCharMap = new HashMap<>();

    static {
        shiftCharMap.put('`', '~');
        shiftCharMap.put('1', '!');
        shiftCharMap.put('2', '@');
        shiftCharMap.put('3', '#');
        shiftCharMap.put('4', '$');
        shiftCharMap.put('5', '%');
        shiftCharMap.put('6', '^');
        shiftCharMap.put('7', '&');
        shiftCharMap.put('8', '*');
        shiftCharMap.put('9', '(');
        shiftCharMap.put('0', ')');

        shiftCharMap.put('a', 'A');
        shiftCharMap.put('b', 'B');
        shiftCharMap.put('c', 'C');
        shiftCharMap.put('d', 'D');
        shiftCharMap.put('e', 'E');
        shiftCharMap.put('f', 'F');
        shiftCharMap.put('g', 'G');
        shiftCharMap.put('h', 'H');
        shiftCharMap.put('i', 'I');
        shiftCharMap.put('j', 'J');
        shiftCharMap.put('k', 'K');
        shiftCharMap.put('l', 'L');
        shiftCharMap.put('m', 'M');
        shiftCharMap.put('n', 'N');
        shiftCharMap.put('o', 'O');
        shiftCharMap.put('p', 'P');
        shiftCharMap.put('q', 'Q');
        shiftCharMap.put('r', 'R');
        shiftCharMap.put('s', 'S');
        shiftCharMap.put('t', 'T');
        shiftCharMap.put('u', 'U');
        shiftCharMap.put('v', 'V');
        shiftCharMap.put('w', 'W');
        shiftCharMap.put('x', 'X');
        shiftCharMap.put('y', 'Y');
        shiftCharMap.put('z', 'Z');

        shiftCharMap.put('[', '{');
        shiftCharMap.put(']', '}');
        shiftCharMap.put(';', ':');
        shiftCharMap.put('\'', '"');
        shiftCharMap.put('\\', '|');
        shiftCharMap.put(',', '<');
        shiftCharMap.put('.', '>');
        shiftCharMap.put('/', '?');
    }


    private List<String> cmdQueue = new ArrayList<>();
    private Integer cmdQueueFlag = -1;

    public HTMLEditorTabPane(TabPane tabPane, Tab tab, String host, String user, String pwd) {

        htmlEditor = new HTMLEditor();
//        HTMLEditorBehavior behavior = new HTMLEditorBehavior(htmlEditor);
        htmlEditor.setHtmlText(WebViewExecuteScriptThread.INITIAL_TEXT);
        HTMLEditorSkin htmlEditorSkin = (HTMLEditorSkin) htmlEditor.getSkin();
        htmlEditor.setPrefHeight(680);
        webView = (WebView) htmlEditor.lookup(".web-view");
        webPage = Accessor.getPageFor(webView.getEngine());
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefHeight(20);
//        textArea.setMaxHeight(90);
        textArea.setEditable(false);
        htmlEditorInputQueueStream = new HTMLEditorInputQueueStream();

        this.tabPane = tabPane;
        this.tab = tab;
        this.host = host;
        this.user = user;
        this.pwd = pwd;

        fxSh = new FxSh(webView, host, user, pwd, htmlEditorInputQueueStream, textArea, htmlEditor, this);
        fxSh.setDaemon(true);
        fxSh.start();

        htmlEditor.getStylesheets().add("cus.css");

        tab.setOnClosed(event -> {
            closeFxSh();
            try {
                HTMLEditorSample.editorTabPaneMap.remove(tab.getId());
                htmlEditorInputQueueStream.close();
//                log.info("close {} htmlEditorInputQueueStream", host);
            } catch (IOException e) {
                log.error("htmlEditorInputQueueStream close error: ", e);
            }
        });
        setTextAreaAction();
    }

    public void closeFxSh() {
        try {
            fxSh.channel.disconnect();
            fxSh.session.disconnect();
            log.info("disconnect {} from {}", user, host);
        } catch (NullPointerException npe) {

        } catch (Exception e) {
            log.error("closeFxSh", e);
        }
    }

    private void setTextAreaAction() {
        textArea.setOnKeyPressed((KeyEvent event) -> {

            if (event.isControlDown()) {
                handleCtrlCommand(event);
            } else if (event.isShiftDown()) {
                handleShiftCommand(event);
            } else if (event.getCode().equals(KeyCode.ENTER)) {
                handleEnter(event);
            } else {
                if (!"".equals(event.getText())) {
                    htmlEditorInputQueueStream.q.add((event.getText() + "").getBytes());
                } else if (event.getCode().equals(KeyCode.BACK_SPACE)) {
                    // 删除 退格：8 和 删除：127都可以
//                htmlEditorInputQueueStream.q.add(new byte[]{(byte)8});
                    htmlEditorInputQueueStream.q.add(new byte[]{(byte) 127});
                } else if (event.getCode().equals(KeyCode.PAGE_DOWN)) {
                    // ESC [ 6 ~
                    htmlEditorInputQueueStream.q.add(new byte[]{27, 91, 54, 126});
//                htmlEditorInputQueueStream.q.add(new byte[]{(byte)KeyCode.CONTROL.impl_getCode(),
//                        (byte)102});
//                htmlEditorInputQueueStream.q.add(new byte[]{(byte)94, (byte)70});
//                htmlEditorInputQueueStream.q.add("\r\n".getBytes());
//                    htmlEditorInputQueueStream.q.add(new byte[]{(byte) 6});
//                    htmlEditorInputQueueStream.q.add(new byte[]{(byte) 34});
                    log.info("key press [{}]", event.getCode().getName());
                } else if (event.getCode().equals(KeyCode.PAGE_UP)) {
                    // http://www.xfree86.org/4.8.0/ctlseqs.html
                    // https://wenku.baidu.com/view/f5bde60a7075a417866fb84ae45c3b3567ecdde3.html
                    // ESC [ 5 ~
                    htmlEditorInputQueueStream.q.add(new byte[]{27, 91, 53, 126});
//                htmlEditorInputQueueStream.q.add(new byte[]{(byte)KeyCode.CONTROL.impl_getCode(),
//                        (byte)98});
//                htmlEditorInputQueueStream.q.add(new byte[]{(byte)94, (byte)66});
//                htmlEditorInputQueueStream.q.add("\r\n".getBytes());
//                    htmlEditorInputQueueStream.q.add(new byte[]{(byte) 2});
//                    htmlEditorInputQueueStream.q.add(new byte[]{(byte) 33});
                    log.info("key press [{}]", event.getCode().getName());
                } else if (event.getCode().equals(KeyCode.UP)) {
                    // https://stackoverflow.com/questions/2876275/what-are-the-ascii-values-of-up-down-left-right
                    // ESC [ A
                    htmlEditorInputQueueStream.q.add(new byte[]{27, 91, 65});
                } else if (event.getCode().equals(KeyCode.DOWN)) {
                    // ESC [ B
                    htmlEditorInputQueueStream.q.add(new byte[]{27, 91, 66});
                } else if (event.getCode().equals(KeyCode.LEFT)) {
                    // ESC [ C
                    htmlEditorInputQueueStream.q.add(new byte[]{27, 91, 67});
                } else if (event.getCode().equals(KeyCode.RIGHT)) {
                    // ESC [ D
                    htmlEditorInputQueueStream.q.add(new byte[]{27, 91, 68});
                } else {
                    log.info("handleEnter unknown {}", event.getCharacter());
                }
            }
//            log.info("setTextAreaAction{}",event.getCode().name());
        });
        textArea.setOnKeyTyped(event -> {
            log.info("keytyped:{}", event.getCharacter());
        });
        textArea.setOnInputMethodTextChanged(event -> {
            if (!"".equals(event.getCommitted())) {
                // 这里有一个bug，输入中文后每次按删除时会删除两个字符，解决方法是针对中文多添加一个div
                htmlEditorInputQueueStream.q.add(event.getCommitted().getBytes());
                log.info("setOnInputMethodTextChanged {}", event.getCommitted());
            }
        });
    }

    private void handleEnter(KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)) {
            htmlEditorInputQueueStream.q.add("\n".getBytes());
            event.consume();
            textArea.clear();
        }
    }

    private void handleUpDownPage(KeyEvent event) {
        if (event.getCode().equals(KeyCode.UP)) {
            Integer index = cmdQueue.indexOf(textArea.getText());
            if (index != -1 && index != 0 && cmdQueue.get(index - 1) != null) {
                textArea.setText(cmdQueue.get(index - 1));
            } else {
                textArea.setText(cmdQueue.get(cmdQueue.size() - 1));
            }
        } else if (event.getCode().equals(KeyCode.DOWN)) {
            Integer index = cmdQueue.indexOf(textArea.getText());
            if (index != -1 && index + 1 < cmdQueue.size()) {
                textArea.setText(cmdQueue.get(index + 1));
            } else {
                textArea.setText("");
            }
        }
    }

    /**
     * https://www.physics.udel.edu/~watson/scen103/ascii.html
     *
     * @param event
     */
    private void handleCtrlCommand(KeyEvent event) {
        if (!event.isControlDown()) {
            return;
        } else if (event.getCode().equals(KeyCode.AT)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 0});
            log.info("press key: [CTRL+@]");
        } else if (event.getCode().equals(KeyCode.A)) {
//            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 1});
//            log.info("press key: [CTRL+A]");
        } else if (event.getCode().equals(KeyCode.B)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 2});
            log.info("press key: [CTRL+B]");
        } else if (event.getCode().equals(KeyCode.C)) {
            htmlEditorInputQueueStream.q.add((textArea.getText()).getBytes());
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) KeyCode.CANCEL.impl_getCode()});
            textArea.clear();
            log.info("press key: [CTRL+C]");
//            enter();
        } else if (event.getCode().equals(KeyCode.D)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 4});
            log.info("press key: [CTRL+D]");
        } else if (event.getCode().equals(KeyCode.E)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 5});
            log.info("press key: [CTRL+E]");
        } else if (event.getCode().equals(KeyCode.F)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 6});
            log.info("press key: [CTRL+F]");
        } else if (event.getCode().equals(KeyCode.G)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 7});
            log.info("press key: [CTRL+G]");
        } else if (event.getCode().equals(KeyCode.H)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 8});
            log.info("press key: [CTRL+H]");
        } else if (event.getCode().equals(KeyCode.I)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 9});
            log.info("press key: [CTRL+I]");
        } else if (event.getCode().equals(KeyCode.J)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 10});
            log.info("press key: [CTRL+J]");
        } else if (event.getCode().equals(KeyCode.K)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 11});
            log.info("press key: [CTRL+K]");
        } else if (event.getCode().equals(KeyCode.L)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 12});
            log.info("press key: [CTRL+L]");
        } else if (event.getCode().equals(KeyCode.M)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 13});
            log.info("press key: [CTRL+M]");
        } else if (event.getCode().equals(KeyCode.N)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 14});
            log.info("press key: [CTRL+N]");
        } else if (event.getCode().equals(KeyCode.O)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 15});
            log.info("press key: [CTRL+O]");
        } else if (event.getCode().equals(KeyCode.P)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 16});
            log.info("press key: [CTRL+P]");
        } else if (event.getCode().equals(KeyCode.Q)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 17});
            log.info("press key: [CTRL+Q]");
        } else if (event.getCode().equals(KeyCode.R)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 18});
            log.info("press key: [CTRL+R]");
        } else if (event.getCode().equals(KeyCode.S)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 19});
            log.info("press key: [CTRL+S]");
        } else if (event.getCode().equals(KeyCode.T)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 20});
            log.info("press key: [CTRL+T]");
        } else if (event.getCode().equals(KeyCode.U)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 21});
            log.info("press key: [CTRL+U]");
        } else if (event.getCode().equals(KeyCode.V)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 22});
            log.info("press key: [CTRL+V]");
            Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
            try {
                String cvString = (String) c.getData(DataFlavor.stringFlavor);
                for (char s : cvString.toCharArray()) {
                    htmlEditorInputQueueStream.q.add(new byte[]{(byte) s});
                }
            } catch (UnsupportedFlavorException | IOException e) {

            }
        } else if (event.getCode().equals(KeyCode.W)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 23});
            log.info("press key: [CTRL+W]");
        } else if (event.getCode().equals(KeyCode.X)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 24});
            log.info("press key: [CTRL+X]");
        } else if (event.getCode().equals(KeyCode.Y)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 25});
            log.info("press key: [CTRL+Y]");
        } else if (event.getCode().equals(KeyCode.Z)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 26});
            log.info("press key: [CTRL+Z]");
        } else if (event.getCode().equals(KeyCode.OPEN_BRACKET)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 27});
            log.info("press key: [CTRL+'[']");
        } else if (event.getCode().equals(KeyCode.BACK_SLASH)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 28});
            log.info("press key: [CTRL+'\\']");
        } else if (event.getCode().equals(KeyCode.CLOSE_BRACKET)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 29});
            log.info("press key: [CTRL+']']");
        } else if (event.getCode().equals(KeyCode.CIRCUMFLEX)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 30});
            log.info("press key: [CTRL+'^']");
        } else if (event.getCode().equals(KeyCode.UNDERSCORE)) {
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 31});
            log.info("press key: [CTRL+'_']");
        }
    }

    private void handleShiftCommand(KeyEvent event) {
        if (event.isShiftDown() && !event.getCode().equals(KeyCode.SHIFT)) {
            if (event.getCode().equals(KeyCode.SEMICOLON)) {
                htmlEditorInputQueueStream.q.add(new byte[]{(byte) 58});
                log.info("press key: [:]", KeyCode.COLON.name());
            } else if (event.getCode().equals(KeyCode.DIGIT1)) {
                htmlEditorInputQueueStream.q.add(new byte[]{(byte) 33});
                log.info("press key: [!]");
            } else if (!"".equals(event.getText()) && event.getText().length() == 1 &&
                    shiftCharMap.keySet().contains(event.getText().charAt(0))) {
                htmlEditorInputQueueStream.q.add(new byte[]{(byte) (char) shiftCharMap.get(event.getText().charAt(0))});
                log.info("press key: [{}]", event.getText().charAt(0));
            } else {
                log.error("shiftCommand notfound:{}", event.toString());
            }
        }
    }

    private void handleFullScreenCommand(KeyEvent event) {
        if (event.isShiftDown() && !event.getCode().equals(KeyCode.SHIFT)) {
            if (event.getCode().equals(KeyCode.SEMICOLON)) {
//                htmlEditorInputQueueStream.q.add(new byte[]{(byte) 58});
                log.info("press key: [:]", KeyCode.COLON.name());
                textArea.setEditable(true);
            }
        } else if (event.isControlDown() && !event.getCode().equals(KeyCode.CONTROL)) {
            if (event.getCode().equals(KeyCode.OPEN_BRACKET)) {
                htmlEditorInputQueueStream.q.add(new byte[]{(byte) 27});
                log.info("press key: [CTRL+'[']");
                textArea.setEditable(false);
            } else if (event.getCode().equals(KeyCode.C)) {
                htmlEditorInputQueueStream.q.add(new byte[]{(byte) KeyCode.CANCEL.impl_getCode()});
                log.info("press key: [CTRL+C]");
                textArea.setEditable(false);
            }
        } else {
            if (textArea.isEditable()) {
                handleEnter(event);
            } else {
                if (!"".equals(event.getText()) && Character.CONTROL != Character.getType(event.getText().charAt(0))) {
                    if (shiftCharMap.keySet().contains(event.getText().charAt(0))) {
                        if (event.isShiftDown()) {
                            htmlEditorInputQueueStream.q.add(new byte[]{(byte) (char) shiftCharMap.get(event.getText().charAt(0))});
                        } else {
                            htmlEditorInputQueueStream.q.add(new byte[]{(byte) event.getText().charAt(0)});
                        }
                    }
                } else {
                    handleFullScreenUpDownPageUpPageDown(event);
                }
            }
            log.info(event.getCode().name());
        }
    }

    private void handleFullScreenUpDownPageUpPageDown(KeyEvent event) {

        if (event.getCode().equals(KeyCode.H) || event.getCode().equals(KeyCode.LEFT)) {
//                htmlEditorInputQueueStream.q.add(new byte[]{(byte) 104, (byte)'\r', (byte)'\n'});
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 104});
            log.info("press key: [H or LEFT]");
            event.consume();
            textArea.setText("");
        } else if (event.getCode().equals(KeyCode.J) || event.getCode().equals(KeyCode.DOWN)) {
//                htmlEditorInputQueueStream.q.add(new byte[]{(byte) 106, (byte)'\r', (byte)'\n'});
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 106});
            log.info("press key: [J or DOWN]");
            event.consume();
            textArea.setText("");
        } else if (event.getCode().equals(KeyCode.K) || event.getCode().equals(KeyCode.UP)) {
//                htmlEditorInputQueueStream.q.add(new byte[]{(byte) 107, (byte)'\r', (byte)'\n'});
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 107});
            log.info("press key: [K or UP]");
            event.consume();
            textArea.setText("");
        } else if (event.getCode().equals(KeyCode.L) || event.getCode().equals(KeyCode.RIGHT)) {
//                htmlEditorInputQueueStream.q.add(new byte[]{(byte) 108, (byte)'\r', (byte)'\n'});
            htmlEditorInputQueueStream.q.add(new byte[]{(byte) 108});
            log.info("press key: [L or RIGHT]");
            event.consume();
            textArea.setText("");
        }
    }

    public HTMLEditor getHtmlEditor() {
        return htmlEditor;
    }

    public TextArea getTextArea() {
        return textArea;
    }

    public WebView getWebView() {
        return webView;
    }

    public void enter() {
//        htmlEditorInputQueueStream.q.add("\r\n".getBytes());
        log.info("enter function");
        htmlEditorInputQueueStream.q.add(new byte[]{(byte) KeyCode.ENTER.impl_getCode()});
    }
}
