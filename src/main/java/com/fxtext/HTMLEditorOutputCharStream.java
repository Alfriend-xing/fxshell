package com.fxtext;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class HTMLEditorOutputCharStream extends OutputStream {

    private List<Character> controlCharacter = new ArrayList<>();
    private List<Byte> chineseCharacter = new ArrayList<>(9);
    // 是否过滤asni控制符，默认过滤，过滤后只会显示可见字符
    public static Boolean asniFilterSwitch = true;
    private WebView webView;
    private WebViewExecuteScriptThread.StyleClazz styleClazz;
    private WebViewExecuteScriptThread.PositionClazz positionClazz;
    private Boolean styleFlag = false;
    private Boolean fullScreenMode = false;
    //                                                                              m    M                       R    r
    public static Integer[] asniEndChar = new Integer[]{65, 66, 67, 68, 72, 74, 75, 109, 77, 115, 117, 108, 104, 82, 114};
    public static Set<Integer> asniEndCharSet = new HashSet<>(Arrays.asList(asniEndChar));

    public HTMLEditorOutputCharStream(WebView webView) {
        this.webView = webView;
        this.styleClazz = new WebViewExecuteScriptThread.StyleClazz();
        this.positionClazz = new WebViewExecuteScriptThread.PositionClazz();
    }

    @Override
    public void write(int b) throws IOException {
        try {
            writeWrapException(b);
        } catch (Exception e) {
            log.error("outputstream write error:", e);
        }
    }

    private void writeWrapException(int b) {
        // 部分字符到int的转换：0x1b=27 m=109 H=72 K=75 h J s u l
        // A65 B66 C67 D68 H72 J74 K75 m109 s115 u117 l108 h104
        // 部分ascii码含义: 0x1b=Escape 样式开始; 0x0f=shift in; 0x07=bell;
        if (asniFilterSwitch) {
            if (b == 15 || b == 7) {
                // 0x07 ascii bell控制符，响铃操作
                return;
            }
            if (b == 27) {
                // 检测到0x1b符号，开始记录asni控制符
                // 0x1b不记录 从'['开始
                // controlCharacter.add((char) b);
                styleFlag = true;
                return;
            }
            if (styleFlag) {
                if (asniEndCharSet.contains(b)) {
                    // 检测到结束符号，结束记录asni控制符
                    controlCharacter.add((char) b);
                    styleFlag = false;
                    parseASNI(controlCharacter.stream().map(String::valueOf).collect(Collectors.joining()));
                    controlCharacter.clear();
                    return;
                } else {
                    controlCharacter.add((char) b);
                    return;
                }
            }
        } else if (Character.CONTROL == Character.getType(b) &&
                !"\n".equals(String.valueOf((char) b)) &&
                !"\r".equals(String.valueOf((char) b)) &&
                !"\b".equals(String.valueOf((char) b))) {
            // 过滤除了换行、回车、退格、响铃之外的ascii控制符，将其转为十六进制字符串打印出来
            // \r return，输入光标回到行的开头
            // \n newline，换行
            // \b是退格符，按键盘上的Backspace键就是，它也是一个字符，但显示的时候是将光标退回前一个字符，但不会删除光标位置的字符
            String tmpHexString = Integer.toHexString(b);
            if (tmpHexString.length() == 1) {
                tmpHexString = String.format("<0x0%s>", tmpHexString);
            } else {
                tmpHexString = String.format("<0x%s>", tmpHexString);
            }
            send(tmpHexString);
            return;
        }
        String tmp = new String(new byte[]{(byte) b});
        if (b < 0) {
            chineseCharacter.add((byte) b);
            if (chineseCharacter.size() == 3) {
                String tmp1 = new String(new byte[]{chineseCharacter.get(0), chineseCharacter.get(1), chineseCharacter.get(2)});
                send(tmp1);
                chineseCharacter.clear();
            }
        } else {
            send(tmp);
            if (fullScreenMode) {
                int oldX = this.positionClazz.x;
                int oldY = this.positionClazz.y;
                if (!"\r".equals(tmp) && !"\n".equals(tmp) && !"\b".equals(tmp)) {
                    this.positionClazz.x++;
                }
                if ("\n".equals(tmp)) {
                    this.positionClazz.y++;
                    this.positionClazz.x = 0;
                    if (this.positionClazz.rolling) {
                        Platform.runLater(new WebViewExecuteScriptThread(webView, null, String.format(HTMLEditorOutputThread.SCROLL_SCREEN, "up"), null, 0));
                    }
                } else if ("\r".equals(tmp)) {
                    this.positionClazz.x = 0;
                } else if ("\b".equals(tmp)) {
                    this.positionClazz.x--;
                }
                if (!this.positionClazz.x.equals(oldX) || !this.positionClazz.y.equals(oldY)) {
                    Platform.runLater(new WebViewExecuteScriptThread(webView, null,
                            String.format(HTMLEditorOutputThread.UPDATE_CURSOR, oldY, oldX, this.positionClazz.y, this.positionClazz.x), null, 0));
                }
            }
        }
    }

    private void send(String str) {
        Platform.runLater(new HTMLEditorOutputThread(this, str));
    }


    public static Map<String, String> fg = new HashMap<>();
    public static Map<String, String> fgNameToCode = new HashMap<>();
    public static Map<String, String> fgLight = new HashMap<>();
    public static Map<String, String> bg = new HashMap<>();

    static {
        fg.put("30", "black");
        fg.put("31", "indianred");
        fg.put("32", "green");
        fg.put("33", "gold");
        fg.put("34", "blue");
        fg.put("35", "purple");
        fg.put("36", "darkcyan");
        fg.put("37", "lightgrey");
        fgLight.put("30", "black");
        fgLight.put("31", "red");
        fgLight.put("32", "lime");
        fgLight.put("33", "yellow");
        fgLight.put("34", "dodgerblue");
        fgLight.put("35", "darkviolet");
        fgLight.put("36", "cyan");
        fgLight.put("37", "lightgrey")
        ;
        bg.put("40", "black");
        bg.put("41", "indianred");
        bg.put("42", "green");
        bg.put("43", "gold");
        bg.put("44", "blue");
        bg.put("45", "purple");
        bg.put("46", "darkcyan");
        bg.put("47", "lightgrey");
        fg.forEach((s, s2) -> fgNameToCode.put(s2, s));
    }

    /**
     * ANSI Escape Sequences : https://gist.github.com/fnky/458719343aabd01cfb17a3a4f7296797
     * ANSI控制码的说明 https://www.cnblogs.com/knowlegefield/p/7774693.html
     * \33[0m 关闭所有属性 ([m)
     * \33[1m 设置高亮度,可以和颜色同时使用,可以高亮之前的颜色
     * \33[4m 下划线
     * \33[5m 闪烁
     * \33[7m 反显 字体和背景对换了颜色
     * \33[8m 消隐
     * \33[30m -- \33[37m 设置前景色; 30:黑black black  31:红 indianred red  32:绿 green lime  33:黄 gold yellow  34:蓝色 blue dodgerblue  35:紫色 purple darkviolet  36:深绿 darkcyan cyan  37:白色 lightgrey
     * \33[40m -- \33[47m 设置背景色; 40:黑black  41:深红 indianred  42:绿 green  43:黄色 gold  44:蓝色 blue  45:紫色 purple  46:深绿 darkcyan 47:白色 lightgrey
     * \33[nA 光标上移n行
     * \33[nB 光标下移n行
     * \33[nC 光标右移n行
     * \33[nD 光标左移n行
     * \33[y;xH设置光标位置
     * \33[2J 清屏
     * \33[K 清除从光标到行尾的内容
     * \33[s 保存光标位置
     * \33[u 恢复光标位置
     * \33[?25l 隐藏光标
     * \33[?25h 显示光标
     * <p>
     * 例如 红色文字，黄色底色，下划线 \033[31;4;43m something here \033[m
     *
     * @param asniControlString
     */
    public void parseASNI(String asniControlString) {
        if (asniControlString.startsWith("[") && asniControlString.endsWith("m")) {
            if (asniControlString.length() > 2) {
                String[] codes = asniControlString.substring(1, asniControlString.length() - 1).split(";");
                List<String> tmp = Arrays.asList(codes);
                Collections.reverse(tmp);
                for (String code : tmp) {
                    if (fg.containsKey(code)) {
                        styleClazz.frontClolor = fg.get(code);
                    } else if (("01".equals(code) || "1".equals(code)) && styleClazz.frontClolor != null) {
                        styleClazz.frontClolor = fgLight.get(fgNameToCode.get(styleClazz.frontClolor));
                    } else if (bg.containsKey(code)) {
                        styleClazz.bgColor = bg.get(code);
                    } else if ("4".equals(code)) {
                        styleClazz.effect.add("cusunderline");
                    } else if ("5".equals(code)) {
                        styleClazz.effect.add("cusblink");
                    } else if ("7".equals(code)) {
                        styleClazz.effect.add("cusReversDisplay");
                    } else {
                        styleClazz.frontClolor = null;
                        styleClazz.bgColor = null;
                        styleClazz.effect = new ArrayList<>();
                    }
                }
            } else {
                styleClazz.frontClolor = null;
                styleClazz.bgColor = null;
                styleClazz.effect = new ArrayList<>();
            }
        } else if (asniControlString.contains("[?1h")) {
            // 开始全屏模式，显示光标
            log.info("开始全屏模式");
            fullScreenMode = true;
            this.positionClazz.cursorVisiable = true;

            Platform.runLater(new WebViewExecuteScriptThread(webView, null, HTMLEditorOutputThread.CLEAN_SCREEN, null, 0));
            Platform.runLater(new WebViewExecuteScriptThread(webView, null, WebViewExecuteScriptThread.fullScreenStyleOn, null, 0));
////        } else if (asniControlString.contains("[25;1H") || asniControlString.contains("[24;1H")) {
        } else if (asniControlString.contains("[?1l")) {
            // 结束全屏模式，隐藏光标
            log.info("结束全屏模式");
            fullScreenMode = false;
            this.positionClazz.cursorVisiable = false;
            this.positionClazz.x = 0;
            this.positionClazz.y = 0;
            Platform.runLater(new WebViewExecuteScriptThread(webView, null, WebViewExecuteScriptThread.fullScreenStyleOff, null, 0));
////       // 结束的地方没有换行符，没办法打印最后一行
//            htmlEditorTabPane.enter();
        } else if (asniControlString.startsWith("[") && asniControlString.endsWith("H")) {
            // 移动光标命令
            int oldX = this.positionClazz.x;
            int oldY = this.positionClazz.y;
            if (asniControlString.length() > 2) {
                String[] codes = asniControlString.substring(1, asniControlString.length() - 1).split(";");
                if (codes.length == 2) {
                    // 获取到xy坐标
                    this.positionClazz.y = Integer.valueOf(codes[0]) - 1;
                    this.positionClazz.x = Integer.valueOf(codes[1]) - 1;
                }
            } else {
                // 收到[H
                this.positionClazz.y = 0;
                this.positionClazz.x = 0;
            }
            Platform.runLater(new WebViewExecuteScriptThread(webView, null,
                    String.format(HTMLEditorOutputThread.UPDATE_CURSOR, oldY, oldX, this.positionClazz.y, this.positionClazz.x), null, 0));
        } else if ("[J".equals(asniControlString)) {
            // 清屏命令
            this.positionClazz.cleanScreen = true;
            Platform.runLater(new WebViewExecuteScriptThread(webView, null, HTMLEditorOutputThread.CLEAN_SCREEN, null, 0));
        } else if ("[K".equals(asniControlString)) {
            // 清除从光标到行尾的内容
            this.positionClazz.cleanLine = true;
            Platform.runLater(new HTMLEditorOutputThread(this, ""));
            this.positionClazz.cleanLine = false;
        } else if ("[1;23r".equals(asniControlString)) {
            // 滚动屏幕开始
            this.positionClazz.rolling = true;
        } else if ("M".equals(asniControlString)) {
            // 向下滚动，第一行添加新行； \r\n为向上滚动，最后一行添加新行
            Platform.runLater(new WebViewExecuteScriptThread(webView, null, String.format(HTMLEditorOutputThread.SCROLL_SCREEN, "down"), null, 0));
        } else if ("[1;24r".equals(asniControlString)) {
            // 滚动屏幕结束
            this.positionClazz.rolling = false;
        }
//        log.info("parseASNI"+ styleClazz.toString());
    }

    public WebView getWebView() {
        return webView;
    }

    public WebViewExecuteScriptThread.StyleClazz getStyleClazz() {
        return styleClazz;
    }

    public WebViewExecuteScriptThread.PositionClazz getPositionClazz() {
        return positionClazz;
    }

    public Boolean getFullScreenMode() {
        return fullScreenMode;
    }
}
