package com.fxtext;

import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 按行输出，无法满足实时回显和tab键自动提示补全的功能，新建线程类根据字符打印
 */
@Slf4j
public class WebViewExecuteScriptThread extends Thread {

    public static String INITIAL_TEXT = "<style> .cusblink { animation: blink 1s linear infinite;    -webkit-animation: blink 1s linear infinite;    -moz-animation: blink 1s linear infinite;    -ms-animation: blink 1s linear infinite;    -o-animation: blink 1s linear infinite;}\n" +
            "@keyframes blink { 0% {opacity : 1;} 50% {opacity : 1;} 50.01% {opacity : 0;}  100% {opacity : 0;} }" +
            ".cursorblink { animation: cursorblinkkey 1s linear infinite;    -webkit-animation: cursorblinkkey 1s linear infinite;    -moz-animation: cursorblinkkey 1s linear infinite;    -ms-animation: cursorblinkkey 1s linear infinite;    -o-animation: cursorblinkkey 1s linear infinite;}\n" +
            "@keyframes cursorblinkkey { 0% {background-color:lightgrey;color:black;} " +
            "50% {background-color:lightgrey;color:black;} " +
            "50.01% {background-color:black;color:lightgrey;}  " +
            "100% {background-color:black;color:lightgrey;} }" +
            "p{font-family: SFMono-Regular,Consolas,Liberation Mono,Menlo,monospace;}" +
            "div{font-family: SFMono-Regular,Consolas,Liberation Mono,Menlo,monospace;}</style>" +
            "<p id=\"connecting\" contentEditable=false " +
            "style=\"line-height:20px;margin:0;padding:0;color:blue;\">连接中...</p>" +
            "<p id=\"command_output\" contentEditable=false " +
            "style=\"line-height:20px;margin:0;padding:0;color:lightgrey;background-color:black;\"></p>" +
            "<div id=\"full_screen\" contentEditable=false " +
            "style=\"line-height:20px;margin:0;padding:0;color:lightgrey;background-color:black;border:3px solid #7fc0d3;display:none;\">" +
            "<div id=\"fs_0\" style=\"height:20px\"></div>" +
            "<div id=\"fs_1\" style=\"height:20px\"></div>" +
            "<div id=\"fs_2\" style=\"height:20px\"></div>" +
            "<div id=\"fs_3\" style=\"height:20px\"></div>" +
            "<div id=\"fs_4\" style=\"height:20px\"></div>" +
            "<div id=\"fs_5\" style=\"height:20px\"></div>" +
            "<div id=\"fs_6\" style=\"height:20px\"></div>" +
            "<div id=\"fs_7\" style=\"height:20px\"></div>" +
            "<div id=\"fs_8\" style=\"height:20px\"></div>" +
            "<div id=\"fs_9\" style=\"height:20px\"></div>" +
            "<div id=\"fs_10\" style=\"height:20px\"></div>" +
            "<div id=\"fs_11\" style=\"height:20px\"></div>" +
            "<div id=\"fs_12\" style=\"height:20px\"></div>" +
            "<div id=\"fs_13\" style=\"height:20px\"></div>" +
            "<div id=\"fs_14\" style=\"height:20px\"></div>" +
            "<div id=\"fs_15\" style=\"height:20px\"></div>" +
            "<div id=\"fs_16\" style=\"height:20px\"></div>" +
            "<div id=\"fs_17\" style=\"height:20px\"></div>" +
            "<div id=\"fs_18\" style=\"height:20px\"></div>" +
            "<div id=\"fs_19\" style=\"height:20px\"></div>" +
            "<div id=\"fs_20\" style=\"height:20px\"></div>" +
            "<div id=\"fs_21\" style=\"height:20px\"></div>" +
            "<div id=\"fs_22\" style=\"height:20px\"></div>" +
            "<div id=\"fs_23\" style=\"height:20px\"></div>" +
//            "<div id=\"fs_24\" style=\"color:red;\"></div>" +
//            "<div id=\"fs_25\" style=\"color:yellow;\"></div>" +
            "</div>";
    public static String INITIAL_FULL_SCREEN_SCRIPT =
            "(function(){" +
                "for(var y=0;y<24;y++){" +
                    "for(var x=0;x<80;x++){" +
                        "var node=document.createElement(\"div\");" +
                        "node['id']='fs_'+y+'_'+x;" +
                        "node.contentEditable=false;" +
                        "node.style[\"lineHeight\"]=\"20px\";" +
                        "node.style[\"margin\"]=\"0\";" +
                        "node.style[\"padding\"]=\"0\";" +
                        "node.style[\"display\"]=\"inline\";" +
                        "document.getElementById('fs_'+y).appendChild(node);" +
                    "}" +
                "}"+
            "}())";
    private WebView webView;
    private String str;
    private String script;
    private StyleClazz styleClazz;
    private String fgValue = "lightgrey";
    private String bgValue = "black";
    private String effectValue = "";
    private String underlineValue = "";
    private String cusReversDisplayValue = "";
    private String outputNodeScript = "";
    // document.getElementById("full_screen").style["border"]="3px solid #7fc0d3";
    public static String fullScreenStyleOn = "document.getElementById(\"full_screen\").style[\"display\"]=\"block\";window.scrollTo(0, document.body.scrollHeight);";
    // document.getElementById("full_screen").style["border"]="";document.getElementById("full_screen").innerHTML="";
    public static String fullScreenStyleOff = "document.getElementById(\"full_screen\").style[\"display\"]=\"none\";window.scrollTo(0, document.body.scrollHeight);";
    private Boolean fullScreenMode = false;
    private Integer x;
    private Integer y;
    private Boolean cursorVisiable = false;
    private Boolean cleanScreen = false;
    private Boolean cleanLine = false;

    /**
     * 换行标记
     */
    private Integer kFlag;

    public static final String cleanConnecting = "document.getElementById(\"connecting\").remove()";

    public static final String initWebViewScript = "document.getElementsByTagName(\"body\")[0].style['backgroundColor']='black';" +
            "document.getElementsByTagName(\"body\")[0].style['color']='lightgrey';";

    /**
     * 新的构造函数，支持输出文本或执行脚本
     *
     * @param htmlEditorOutputStream
     * @param str
     * @param script
     */
    public WebViewExecuteScriptThread(HTMLEditorOutputStream htmlEditorOutputStream, String str, String script) {
        this.webView = htmlEditorOutputStream.getWebView();
        this.str = str;
        this.script = script;
        this.kFlag = htmlEditorOutputStream.getkFlag();
        this.styleClazz = htmlEditorOutputStream.getStyleClazz();
        this.x = htmlEditorOutputStream.getPositionClazz().x;
        this.y = htmlEditorOutputStream.getPositionClazz().y;
        cursorVisiable = htmlEditorOutputStream.getPositionClazz().cursorVisiable;
        cleanScreen = htmlEditorOutputStream.getPositionClazz().cleanScreen;
        cleanLine = htmlEditorOutputStream.getPositionClazz().cleanLine;
        if (styleClazz != null) {
            this.fgValue = styleClazz.frontClolor == null ? "lightgrey" : styleClazz.frontClolor;
            this.bgValue = styleClazz.bgColor == null ? "black" : styleClazz.bgColor;
            if (styleClazz.effect.size() > 0) {
                this.effectValue = "node.classList.add(\"" + String.join("\",\"", styleClazz.effect) + "\");";
                if (styleClazz.effect.contains("cusunderline")) {
                    this.underlineValue = "node.style[\"textDecoration\"]=\"underline\";";
                }
                if (styleClazz.effect.contains("cusReversDisplay")) {
                    this.cusReversDisplayValue = "node.style[\"color\"]=\"black\";" +
                            "node.style[\"backgroundColor\"]=\"lightgrey\";";
                }
            }
        }
        this.fullScreenMode = htmlEditorOutputStream.getFullScreenMode();
    }

    /**
     * 旧的构造函数，主要用于执行脚本，输出文本的样式会有问题
     *
     * @param webView
     * @param str
     * @param script
     * @param styleClazz
     * @param kFlag
     */
    public WebViewExecuteScriptThread(WebView webView, String str, String script, StyleClazz styleClazz, Integer kFlag) {
        this.webView = webView;
        this.str = str;
        this.script = script;
        this.kFlag = kFlag;
        this.styleClazz = styleClazz;
        if (styleClazz == null) {
            this.fgValue = "lightgrey";
            this.bgValue = "black";
            this.effectValue = "";
        } else {
            this.fgValue = styleClazz.frontClolor == null ? "lightgrey" : styleClazz.frontClolor;
            this.bgValue = styleClazz.bgColor == null ? "black" : styleClazz.bgColor;
            if (styleClazz.effect.size() > 0) {
                this.effectValue = "node.classList.add(\"" + String.join("\",\"", styleClazz.effect) + "\");";
            } else {
                effectValue = "";
            }
        }
    }

    @Override
    public void run() {
//        changeScreenMode(fullScreenMode);
        if (cleanLine) {
            cleanLineFromCursor();
        }else if (fullScreenMode && str != null) {
            writeToLine(x, y, str);
        } else {
            exeScript();
        }
    }

    private synchronized void exeScript() {
        //更新JavaFX的主线程的代码放在此处 document.getElementById("content_left").remove()
        try {
            if (script != null && !"".equals(script)) {
                webView.getEngine().executeScript(script);
            }
            if (str != null && !"".equals(str)) {
                Long id = System.nanoTime();
//                        log.info(line);
                webView.getEngine().executeScript(
                        "(function(){" +
                                "var node = document.createElement('div');" +
                                "node.contentEditable=false;" +
                                "node.style[\"lineHeight\"]=\"20px\";" +
                                "node.style[\"margin\"]=\"0\";" +
                                "node.style[\"padding\"]=\"0\";" +
                                "node.style[\"display\"]=\"inline\";" +
                                "node.style[\"backgroundColor\"]=\"" + bgValue + "\";" +
                                "node.style[\"color\"]=\"" + fgValue + "\";" +
                                "node.id=" + id + ";" +
                                "node.textContent = `" + str + "`;" +
                                underlineValue + cusReversDisplayValue +
                                effectValue +
// 闪烁效果class         "node.classList.add(\"cusblink\");" +
//                        "document.getElementById(\"cc\").parentNode.appendChild(node);" +
//                        "document.getElementsByTagName(\"body\")[0].appendChild(node);" +
                                "document.getElementById(\"command_output\").appendChild(node);" +
                                "node.scrollIntoView();" +
                                "}())");

                if (kFlag == 1) {
                    changeLine();
                } else if (kFlag == 2) {
//                    webView.getEngine().executeScript(clearKFlag1);
//                    log.warn(webView.getEngine().executeScript(clearKFlag).toString());
                } else if (kFlag == 3) {
//                    webView.getEngine().executeScript(clearScreen);
//                    log.warn(webView.getEngine().executeScript(clearKFlag).toString());
                }
            }
        } catch (Exception e) {
            log.error("error:" + str, e);
        }
    }

    // synchronized
    private synchronized void writeToLine(Integer x, Integer y, String text) {
        if (x == null || x > 100 || y == null || y > 26 || text == null
                || "".equals(text) || "\r".equals(text) || "\n".equals(text) || "\r\n".equals(text)) {
            log.error("writeToLine error x{} y{} text{}", x, y, text);
            return;
        }
        log.info("write line y{} x{} text{}", y, x, text);
        try {
            // "document.getElementById(\"fs_" + y + "\");"
            if(cleanScreen){
                cleanScreen();
            }
            webView.getEngine().executeScript(
                    "(function(){" +
                            "var node = document.getElementById(\"fs_" + y + "\");" +
                            "if(node!=null){" +
                            "var tmpString = node.textContent;" +
                            "var addString = `" + text + "`;" +
                            "var x=" + x + ";" +
                            "node.style[\"display\"]=\"block\";" +
                            underlineValue + cusReversDisplayValue +
                            "if(tmpString==''){" +
                            "node.textContent=addString;}" +
                            "else{" +
//                                    "var replaceString = tmpString.substr(" + x + "," + text.length() + ");" +
//                                    "node.textContent = tmpString.replace(tmpString,`" + text + "`);" +

//                            "node.textContent = tmpString + 'b';" +
//                            "node.textContent = addString;" +

                            "node.textContent = tmpString.substring(0,x) + addString + tmpString.substring(x+addString.length);" +
                            "}" +
                            "}" +
                            "window.scrollTo(0, document.body.scrollHeight);" +
                            "}())"
            );
        } catch (Exception e) {
            log.error("webView.getEngine().executeScript error:", e);
        }
    }

    private synchronized void changeLine() {

        Long id = System.nanoTime();
//                        log.info(line);
        webView.getEngine().executeScript(
                // 立即执行函数会创建一个独立的作用域，让外部无法访问作用域内部的变量，
                // 从而避免变量污染。并且该函数只会执行一次，执行后自动被垃圾回收。
                "(function(){" +
                        "var node2 = document.createElement('br');" +
                        "node2.contentEditable=false;" +
                        "node2.style[\"lineHeight\"]=\"20px\";" +
                        "node2.style[\"margin\"]=\"0\";" +
                        "node2.style[\"padding\"]=\"0\";" +
                        "node2.id=" + id + ";" +
                        "document.getElementById(\"command_output\").appendChild(node2);" +
                        "node2.scrollIntoView();" +
                        "}())"
        );
//        log.info(outputNodeScript);
    }

    /**
     * 执行脚本修改输出节点样式
     *
     * @param mode
     */
    private void changeScreenMode(Boolean mode) {
        if (mode == null) {
            return;
        } else if (mode) {
            webView.getEngine().executeScript(fullScreenStyleOn);
        } else {
            webView.getEngine().executeScript(fullScreenStyleOff);
        }
    }

    private void cleanScreen() {
        webView.getEngine().executeScript(
                "(function(){" +
                        "for(i=0;i<26;i++){" +
                        "document.getElementById(\"fs_\"+i).innerHTML='　';" +
//                        "document.getElementById(\"fs_\"+i).innerHTML='';" +
                        "document.getElementById(\"fs_\"+i).style['backgroundColor']='black';" +
                        "document.getElementById(\"fs_\"+i).style['color']='lightgrey';" +
                        "}" +
                        "}())"
        );
    }

    private void cleanLineFromCursor() {
        webView.getEngine().executeScript(
                "(function(){" +
                        "var node1 = document.getElementById(\"fs_" + y + "\");" +
                        "if(node1!=null){" +
                            "var tmpString = node1.textContent;" +
                            "var x=" + x + ";" +
                            "if(x==0){node1.textContent=\"　\"}" +
                            "else{node1.textContent=tmpString.substring(0,x);}}" +
                        "}())"
        );
    }

    public static class StyleClazz {

        public String frontClolor;
        public String bgColor;
        public List<String> effect;

        public StyleClazz() {
            effect = new ArrayList<>();
        }

        @Override
        public String toString() {
            return String.format("frontClolor:%s bgColor:%s effect:%s", frontClolor, bgColor, String.join(",", effect));
        }
    }

    public static class PositionClazz {
        public Integer x = 0;
        public Integer y = 0;
        public Boolean cursorVisiable = false;
        public Boolean cleanScreen = false;
        public Boolean cleanLine = false;
        public Boolean rolling = false;
    }
}
