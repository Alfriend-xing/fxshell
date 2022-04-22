package com.fxtext;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class HTMLEditorOutputStream extends OutputStream {
    /**
     * 是否全屏，目前根据是否显示光标判断，之后会被positionClazz.cursorVisiable替换
     */
    private Boolean fullScreenMode = false;
    private byte[] buffer = new byte[9999];
    private List<Character> controlCharacter = new ArrayList<>();
    private HTMLEditorTabPane htmlEditorTabPane;
    private String tt;
    private volatile Boolean styleFlag = false;
    private Integer kFlag = 0;
    private Integer len = 0;
    private Integer cur = 0;
    private WebView webView;
    String line = "";
    private WebViewExecuteScriptThread.StyleClazz styleClazz;
    private WebViewExecuteScriptThread.PositionClazz positionClazz;

    // 是否过滤asni控制符，默认过滤，过滤后只会显示可见字符
    public static Boolean asniFilterSwitch = true;

    public HTMLEditorOutputStream(WebView webView, HTMLEditorTabPane htmlEditorTabPane) {
        this.webView = webView;
        this.htmlEditorTabPane = htmlEditorTabPane;
        this.styleClazz = new WebViewExecuteScriptThread.StyleClazz();
        this.positionClazz = new WebViewExecuteScriptThread.PositionClazz();
    }

    //                                                                              m    M                       R    r
    public static Integer[] asniEndChar = new Integer[]{65, 66, 67, 68, 72, 74, 75, 109, 77, 115, 117, 108, 104, 82, 114};
    public static Set<Integer> asniEndCharSet = new HashSet<>(Arrays.asList(asniEndChar));

    /**
     * @param b
     * @throws IOException
     */
    @Override
    public synchronized void write(int b) throws IOException {
        try {
            writeWrapException(b);
        } catch (Exception e) {
            log.error("outputstream write error:", e);
        }
    }

    private synchronized void writeWrapException(int b) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            log.error(null, e);
        });
        // 部分字符到int的转换：0x1b=27 m=109 H=72 K=75 h J s u l
        // A65 B66 C67 D68 H72 J74 K75 m109 s115 u117 l108 h104
        // 部分ascii码含义: 0x1b=Escape 样式开始; 0x0f=shift in; 0x07=bell;
        if (asniFilterSwitch) {
            if (b == 15) {
                return;
            }
            if (b == 27) {
                // 检测到0x1b符号，开始记录asni控制符
                // 0x1b不记录 从'['开始
                // controlCharacter.add((char) b);
                if (cur != 0 || len != 0) {
                    int tmpLen = len;
                    int tmpCur = cur;
                    send(buffer, cur, len, 0);
                    this.positionClazz.x += tmpLen - tmpCur;
                }
                styleFlag = true;
                return;
            }
            if (styleFlag) {
                if (asniEndCharSet.contains(b)) {
                    // 检测到结束符号，结束记录asni控制符
                    controlCharacter.add((char) b);
//                    buffer[cur + len] = (byte) ' ';
//                    len++;
                    styleFlag = false;
                    tt = controlCharacter.stream().map(String::valueOf).collect(Collectors.joining());
                    parseASNI(tt);
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
            // 过滤除了换行、回车、空格之外的ascii控制符，目前无操作
            String tmpHexString = Integer.toHexString(b);
            if (tmpHexString.length() == 1) {
                tmpHexString = String.format("<0x0%s>", tmpHexString);
            } else {
                tmpHexString = String.format("<0x%s>", tmpHexString);
            }
            for (char c : tmpHexString.toCharArray()) {
                buffer[len] = (byte) c;
                len++;
            }
            return;
        }

        // collect all character into the buffer
//        buffer[cur + len] = (byte) b;
        buffer[len] = (byte) b;
        len++;
        // on '\n' character append to output window
        if ("\n".equals(String.valueOf((char) b)) || "\b".equals(String.valueOf((char) b))) {
//            arr = new byte[]{(byte)b};
//            StringBuilder sb = new StringBuilder(buffer.size());
//            for (Character c : buffer) sb.append(c);
//            line = sb.toString();
//            log.info(integers.toString());
//            line = new String(buffer, cur, len, Charset.defaultCharset());
//            cur = len;
//            len = 0;
            send(buffer, cur, len, 1);
            if (fullScreenMode) {
                // 换行前清除从光标到行尾的内容
//                this.positionClazz.cleanLine = true;
//                Platform.runLater(new WebViewExecuteScriptThread(this, null, null));
//                this.positionClazz.cleanLine = false;
                // 如果是全屏模式，输出一行后更新光标位置
                this.positionClazz.y++;
                if (positionClazz.y > 26) {
                    positionClazz.y = 0;
                }
                this.positionClazz.x = 0;
            }
            cur = 0;
            len = 0;
            buffer = new byte[9999];
//            log.info(line);
            if (kFlag == 3) {
                kFlag = 1;
            }
            controlCharacter.clear();
            tt = "";
        }
    }

    private void send(byte[] buffer, Integer cur, Integer len, Integer kFlag) {
        if (cur.equals(len)) {
            return;
        }
        this.kFlag = kFlag;
        byte[] tmp = Arrays.copyOfRange(buffer, cur, len);
        List<Integer> integers = new ArrayList<>();
        for (byte by : tmp) {
            integers.add((int) by);
        }
        line = new String(tmp);
//        Platform.runLater(new WebViewExecuteScriptThread(this, line.replaceAll("  ", "　"), null));
        Platform.runLater(new WebViewExecuteScriptThread(this, line.replaceAll(" ", "\\\\xa0"), null));
//        Platform.runLater(new WebViewExecuteScriptThread(this, line, null));
        this.cur = len;
        this.positionClazz.cleanScreen = false;
        this.positionClazz.cleanLine = false;
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
//        } else if (asniControlString.contains("[25;1H") || asniControlString.contains("[24;1H")) {
        } else if (asniControlString.contains("[?1l")) {
            // 结束全屏模式，隐藏光标
            log.info("结束全屏模式");
            fullScreenMode = false;
            this.positionClazz.cursorVisiable = false;
            this.positionClazz.x = 0;
            this.positionClazz.y = 0;
            // 结束的地方没有换行符，没办法打印最后一行
            htmlEditorTabPane.enter();
        } else if (asniControlString.startsWith("[") && asniControlString.endsWith("H")) {
            // 移动光标命令
            if (asniControlString.length() > 2) {
                String[] codes = asniControlString.substring(1, asniControlString.length() - 1).split(";");
                if (codes.length == 2) {
                    // 获取到xy坐标
                    this.positionClazz.y = Integer.valueOf(codes[0]);
                    this.positionClazz.x = Integer.valueOf(codes[1]);
                }
            } else {
                // 收到[H
                this.positionClazz.y = 0;
                this.positionClazz.x = 0;
            }
        } else if ("[J".equals(asniControlString)) {
            // 清屏命令
            this.positionClazz.cleanScreen = true;
        } else if ("[K".equals(asniControlString)) {
            // 清除从光标到行尾的内容
            this.positionClazz.cleanLine = true;
            Platform.runLater(new WebViewExecuteScriptThread(this, null, null));
            this.positionClazz.cleanLine = false;
        }
//        log.info("parseASNI"+ styleClazz.toString());
    }

    public Boolean getFullScreenMode() {
        return fullScreenMode;
    }

    public Integer getkFlag() {
        return kFlag;
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

    public static void main(String[] args) {
//        String asni = "[m";
//        System.out.println(parseASNI(asni));
//        String asni1 = "[31;4;43m";
//        System.out.println(parseASNI(asni1));
//        String asni2 = "[01:31;4;43m";
//        System.out.println(parseASNI(asni2));
        Character.getType(15);
        Integer.toHexString(15);
    }

}

