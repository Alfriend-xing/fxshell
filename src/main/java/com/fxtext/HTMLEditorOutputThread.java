package com.fxtext;

import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;

/**
 * 根据字符打印，将行尾的\r\n转为br标签显示在webView中
 */
@Slf4j
public class HTMLEditorOutputThread extends Thread {
    private HTMLEditorOutputCharStream htmlEditorOutputCharStream;
    private String str;
    private WebView webView;

    private String fgValue = "lightgrey";
    private String bgValue = "black";
    private String underlineValue = "";
    private String cusblinkValue = "";
    private Integer x;
    private Integer y;

    private String outputNodeScript = "";
    // document.getElementById("full_screen").style["border"]="3px solid #7fc0d3";
    private static String fullScreenStyleOn = "document.getElementById(\"full_screen\").style[\"display\"]=\"block\";window.scrollTo(0, document.body.scrollHeight);";
    // document.getElementById("full_screen").style["border"]="";document.getElementById("full_screen").innerHTML="";
    private static String fullScreenStyleOff = "document.getElementById(\"full_screen\").style[\"display\"]=\"none\";window.scrollTo(0, document.body.scrollHeight);";
    private Boolean fullScreenMode = false;
    private Boolean cursorVisiable = false;
    private Boolean cleanScreen = false;
    private Boolean cleanLine = false;
    public static final String cleanConnecting = "document.getElementById(\"connecting\").remove()";
    public static final String initWebViewScript = "document.getElementsByTagName(\"body\")[0].style['backgroundColor']='black';" +
            "document.getElementsByTagName(\"body\")[0].style['color']='lightgrey';";

    public HTMLEditorOutputThread(HTMLEditorOutputCharStream htmlEditorOutputCharStream, String str) {
        this.webView = htmlEditorOutputCharStream.getWebView();
        this.str = str.replaceAll(" ", "\\\\xa0");
        WebViewExecuteScriptThread.StyleClazz styleClazz = htmlEditorOutputCharStream.getStyleClazz();
        this.x = htmlEditorOutputCharStream.getPositionClazz().x;
        this.y = htmlEditorOutputCharStream.getPositionClazz().y;
        cursorVisiable = htmlEditorOutputCharStream.getPositionClazz().cursorVisiable;
        cleanScreen = htmlEditorOutputCharStream.getPositionClazz().cleanScreen;
        cleanLine = htmlEditorOutputCharStream.getPositionClazz().cleanLine;
        if (styleClazz != null) {
            this.fgValue = styleClazz.frontClolor == null ? "lightgrey" : styleClazz.frontClolor;
            this.bgValue = styleClazz.bgColor == null ? "black" : styleClazz.bgColor;
            if (styleClazz.effect.contains("cusunderline")) {
                this.underlineValue = "node.style[\"textDecoration\"]=\"underline\";";
            }
            if (styleClazz.effect.contains("cusReversDisplay")) {
                String tmpColor = this.fgValue;
                this.fgValue = this.bgValue;
                this.bgValue = tmpColor;
            }
            if (styleClazz.effect.contains("cusblink")) {
                this.cusblinkValue = "node.classList.add(\"cusblink\");";
            }
        }
        this.fullScreenMode = htmlEditorOutputCharStream.getFullScreenMode();
    }

    @Override
    public synchronized void run() {
//        changeScreenMode(fullScreenMode);
        if(fullScreenMode){
//            updateCursor();
            if(cleanLine){
                cleanLineFromCursorForFullScreen();
            }else{
                writeToFullScreen();
            }
        }else{
            if(str.length()==0){
                return;
            }else if (((Character) '\r').equals(str.charAt(0))) {

            } else if (((Character) '\n').equals(str.charAt(0))) {
                changeLine();
            } else if (((Character) '\b').equals(str.charAt(0))) {
                delLastChar();
            } else {
                write();
            }
        }

    }

    private void write() {
        Long id = System.nanoTime();
        Integer addCount = Integer.toHexString((int)str.charAt(0)).length()/2;
        String addNode = "";
        if(str.length()==1 && addCount>1){
            addNode = "for(var adn = 0; adn<"+(addCount-1)+";adn++){" +
                        "var nodeToAdd = document.createElement('div');" +
                        "nodeToAdd.style[\"display\"]=\"inline\";" +
                        "document.getElementById(\"command_output\").lastChild.appendChild(nodeToAdd);" +
                    "}";
        }
        webView.getEngine().executeScript(
                "(function(){" +
                        "if(document.getElementById(\"command_output\").lastChild==null)" +
                        "{document.getElementById(\"command_output\").appendChild(document.createElement('div'));}" +
                        "var node = document.createElement('div');" +
                        "node.contentEditable=false;" +
                        "node.style[\"lineHeight\"]=\"20px\";" +
                        "node.style[\"margin\"]=\"0\";" +
                        "node.style[\"padding\"]=\"0\";" +
                        "node.style[\"display\"]=\"inline\";" +
                        "node.style[\"backgroundColor\"]=\"" + bgValue + "\";" +
                        "node.style[\"color\"]=\"" + fgValue + "\";" +
                        "node.id=" + id + ";" +
                        "node.innerHTML = `" + str + "`;" +
                        underlineValue + cusblinkValue +
//                        "document.getElementById(\"cc\").parentNode.appendChild(node);" +
//                        "document.getElementsByTagName(\"body\")[0].appendChild(node);" +
                        "document.getElementById(\"command_output\").lastChild.appendChild(node);" +
                        "node.scrollIntoView();" + addNode +
                        "}())");
    }

    private void changeLine() {
        webView.getEngine().executeScript(
                "(function(){" +
                        "var node = document.createElement('div');" +
                        "node.contentEditable=false;" +
                        "document.getElementById(\"command_output\").appendChild(node);" +
                        "node.scrollIntoView();" +
                        "}())");
    }

    private void delLastChar() {
        log.info("HTMLEditorOutputThread delLastChar:[{}]",
                webView.getEngine().executeScript("document.getElementById(\"command_output\").lastChild.lastChild.textContent;"));
        webView.getEngine().executeScript(
                "(function(){" +
                        "var node = document.getElementById(\"command_output\").lastChild.lastChild;" +
//                        "if(node.tagName=='DIV' && node.textContent.length != 0)" +
                        "if(node.tagName=='DIV')" +
                        "{node.remove();}" +
                        "}())");
//        webView.getEngine().executeScript(
//                "(function(){" +
//                        "var node = document.getElementById(\"command_output\").lastChild;" +
//                        "if(node.tageName=='div' && node.textContent.length != 0)" +
//                        "{node.textContent = node.textContent.substr(0,node.textContent.length-1);}" +
//                        "}())");
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

    public static String UPDATE_CURSOR = "(function(){" +
            "var node = document.getElementById('fs_'+%d+'_'+%d);" +
            "if(node!=null){" +
                "node.classList=[];" +
            "}" +
            "var node = document.getElementById('fs_'+%d+'_'+ %d );" +
            "if(node!=null){" +
                "node.classList.add(\"cursorblink\");" +
            "}" +
            "}())";

    private void updateCursor(){
        try {
            webView.getEngine().executeScript(
                    "(function(){" +
                        "for(var y=0;y<24;y++){" +
                            "for(var x=0;x<80;x++){" +
                                "var node = document.getElementById('fs_'+y+'_'+x);" +
                                "if(node!=null){" +
                                    "node.classList=[];" +
                                "}" +
                            "}" +
                        "}"+
                        "var node = document.getElementById('fs_'+" + y + "+'_'+" + x + ");" +
                        "if(node!=null){" +
                            "node.classList.add(\"cursorblink\");" +
                        "}" +
                    "}())"
            );
        } catch (Exception e) {
            log.error("webView.getEngine().executeScript error:", e);
        }
    }

    private synchronized void writeToFullScreen(){
        if("\r".equals(str) || "\n".equals(str) || "\b".equals(str) || "\r\n".equals(str)){
            return;
        }
        if (x == null || x > 100 || y == null || y > 23 || str == null
                || "".equals(str)) {
            log.error("writeToLine error x{} y{} text{}", x, y, str);
            return;
        }
//        log.info("write line y{} x{} text{}", y, x, str);
        try {
            // "document.getElementById(\"fs_" + y + "\");"
//            log.info("x{},line.length{}",x,webView.getEngine().executeScript("document.getElementById(\"fs_" + y + "\").children.length;"));
            webView.getEngine().executeScript(
                    "(function(){" +
                            "var node = document.getElementById(\"fs_" + y + "_" + x + "\");" +
                            "if(node!=null){" +
                                "node.innerHTML = `" + str + "`;" +
                                "node.style[\"backgroundColor\"]=\"" + bgValue + "\";" +
                                "node.style[\"color\"]=\"" + fgValue + "\";" +
                                underlineValue + cusblinkValue +
                            "}" +
                            "window.scrollTo(0, document.body.scrollHeight);" +
                            "}())"
            );
        } catch (Exception e) {
            log.error("webView.getEngine().executeScript error:", e);
        }
    }


    private void cleanLineFromCursorForFullScreen(){
//        log.info("cleanLineFromCursorForFullScreen:y{},x{}",y,x);
        try {
            webView.getEngine().executeScript(
                    "(function(){" +
                        "var initY = " + y + ";" +
                        "var initX = " + x + ";" +
                        "for(var x=initX;x<80;x++){" +
                            "var node = document.getElementById('fs_'+ initY +'_' + x);" +
                            "if(node!=null){" +
                            "node.innerHTML = '';" +
                            "node.style[\"backgroundColor\"]='black';" +
                            "node.style[\"color\"]='lightgrey';" +
                            "node.style[\"textDecoration\"]='';" +
                            "node.classList=[];" +
                            "}" +
                        "}" +
                    "}())"
            );
        } catch (Exception e) {
            log.error("webView.getEngine().executeScript error:", e);
        }
    }

    public static String CLEAN_SCREEN=
            "(function(){" +
                    "for(var y=0;y<24;y++){" +
                    "for(var x=0;x<80;x++){" +
                    "var node = document.getElementById('fs_'+y+'_'+x);" +
                    "if(node!=null){" +
                    "node.innerHTML = '';" +
                    "node.style[\"backgroundColor\"]='black';" +
                    "node.style[\"color\"]='lightgrey';" +
                    "node.style[\"textDecoration\"]='';" +
                    "node.classList=[];" +
                    "}" +
                    "}" +
                    "}"+
                    "}())";

    private void cleanScreenForFullScreen(){
        try {
            webView.getEngine().executeScript(
                    "(function(){" +
                        "for(var y=0;y<24;y++){" +
                            "for(var x=0;x<80;x++){" +
                                "var node = document.getElementById('fs_'+y+'_'+x);" +
                                "if(node!=null){" +
                                    "node.innerHTML = '';" +
                                    "node.style[\"backgroundColor\"]='black';" +
                                    "node.style[\"color\"]='lightgrey';" +
                                    "node.style[\"textDecoration\"]='';" +
                                    "node.classList=[];" +
                                "}" +
                            "}" +
                        "}"+
                    "}())"
            );
        } catch (Exception e) {
            log.error("webView.getEngine().executeScript error:", e);
        }
    }

    public static String SCROLL_SCREEN=
            "(function(){" +
                    "var direction = `%s`;" +
                    "if(direction=='up'){" +
                    "var full_screen_node = document.getElementById('full_screen');" +
                    "var linenode = full_screen_node.children[0];" +
                    "for(var cid=0; cid<linenode.childElementCount;cid++){" +
                    "linenode.children[cid].textContent='';" +
                    "}" +
                    "full_screen_node.insertBefore(full_screen_node.children[0],full_screen_node.children[full_screen_node.childElementCount-1]);" +
                    "}else{" +
                    "var full_screen_node = document.getElementById('full_screen');" +
                    "var linenode = full_screen_node.children[full_screen_node.childElementCount-1-1];" +
                    "for(var cid=0; cid<linenode.childElementCount;cid++){" +
                    "linenode.children[cid].textContent='';" +
                    "}" +
                    "full_screen_node.insertBefore(full_screen_node.children[full_screen_node.childElementCount-1-1],full_screen_node.children[0]);" +
                    "}" +
                    // 最后一行状态行不参与滚动
                    "for(var y=0;y<24;y++){" +
                    "for(var x=0;x<80;x++){" +
                    "full_screen_node.children[y].children[x].id='fs_'+y+'_'+x;" +
                    "}" +
                    "}" +
                    "}())";

    private void scrollScreen(){
        String direction = "up";
        try {
            webView.getEngine().executeScript(
                    "(function(){" +
                        "var direction = `"+ direction + "`;" +
                        "if(direction=='up'){" +
                            "var full_screen_node = document.getElementById('full_screen');" +
                            "var linenode = full_screen_node.children[0];" +
                            "for(var cid=0; cid<linenode.childElementCount;cid++){" +
                                "linenode.children[cid].textContent='';" +
                            "}" +
                            "full_screen_node.insertBefore(full_screen_node.children[0],full_screen_node.children[full_screen_node.childElementCount-1-1]);" +
                        "}else{" +
                            "var full_screen_node = document.getElementById('full_screen');" +
                            "var linenode = full_screen_node.children[full_screen_node.childElementCount-1-1];" +
                            "for(var cid=0; cid<linenode.childElementCount;cid++){" +
                                "linenode.children[cid].textContent='';" +
                            "}" +
                            "full_screen_node.insertBefore(full_screen_node.children[full_screen_node.childElementCount-1-1],full_screen_node.children[0]);" +
                            "}" +
                            // 最后一行状态行不参与滚动
                        "for(var y=0;y<24;y++){" +
                            "for(var x=0;x<80;x++){" +
                                "full_screen_node.children[y].children[x].id='fs_'+y+'_'+x;" +
                                "}" +
                            "}" +
                    "}())"
            );
        } catch (Exception e) {
            log.error("webView.getEngine().executeScript error:", e);
        }
    }
}
