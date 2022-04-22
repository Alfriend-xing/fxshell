package com.fxtext;

import com.jcraft.jsch.*;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
public class FxSh extends Thread {

    private static final int KEY_SIZE = 128;
    private static final int DATA_LENGTH = 128;
    private static Cipher encryptionCipher;
    private WebView webView;
    private HTMLEditor htmlEditor;
    private HTMLEditorTabPane htmlEditorTabPane;
    private TextArea textArea;
    private HTMLEditorInputQueueStream htmlEditorInputQueueStream;
    private HTMLEditorOutputStream htmlEditorOutputStream;
    private HTMLEditorOutputCharStream htmlEditorOutputCharStream;
    public String host;
    public String user;
    public String passwd;

    public Session session;
    public Channel channel;

    private static SecretKey aseKey;
    public static byte[] digest;

    static {
        try {
//            System.getenv().get("USERNAME")
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(KEY_SIZE);
//            aseKey = keyGenerator.generateKey();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            digest = md5.digest(System.getenv().get("USERNAME").getBytes("utf-8"));
            aseKey = new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            log.error(null, e);
        }
    }

    public FxSh(WebView webView, String host, String user, String pwd,
                HTMLEditorInputQueueStream htmlEditorInputQueueStream, TextArea textArea,
                HTMLEditor htmlEditor, HTMLEditorTabPane htmlEditorTabPane) {
        this.webView = webView;
        this.host = host;
        this.user = user;
        this.passwd = pwd;
        this.htmlEditorInputQueueStream = htmlEditorInputQueueStream;
        this.textArea = textArea;
        this.htmlEditor = htmlEditor;
        this.htmlEditorTabPane = htmlEditorTabPane;
    }

    @Override
    public void run() {
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
            log.error(t.getName(), e);
        });
        try {
            JSch jsch = new JSch();

            //jsch.setKnownHosts("/home/foo/.ssh/known_hosts");
            if(user==null || "".equals(user)){
                user = JOptionPane.showInputDialog("Enter username, content will not be saved.");
            }
            session = jsch.getSession(user, host, 22);
            if(passwd!=null && !"".equals(passwd)){
                session.setPassword(passwd);
            }else{
                passwd = JOptionPane.showInputDialog("Enter password, content will not be saved.");
                session.setPassword(passwd);
            }
            session.setConfig("StrictHostKeyChecking", "no");

            UserInfo ui = new MyUserInfo() {
                @Override
                public void showMessage(String message) {
                    JOptionPane.showMessageDialog(null, message);
                }

                @Override
                public boolean promptYesNo(String message) {
                    Object[] options = {"yes", "no"};
                    int foo = JOptionPane.showOptionDialog(null,
                            message,
                            "Warning",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null, options, options[0]);
                    return foo == 0;
                }

                // If password is not given before the invocation of Session#connect(),
                // implement also following methods,
                //   * UserInfo#getPassword(),
                //   * UserInfo#promptPassword(String message) and
                //   * UIKeyboardInteractive#promptKeyboardInteractive()

            };

            session.setUserInfo(ui);

            // It must not be recommended, but if you want to skip host-key check,
            // invoke following,
            // session.setConfig("StrictHostKeyChecking", "no");

            //session.connect();
            session.connect(30000);   // making a connection with timeout.
            channel = session.openChannel("shell");
            // Enable agent-forwarding.
            //((ChannelShell)channel).setAgentForwarding(true);

              /*
              // Choose the pty-type "vt102".
              */
//            ((ChannelShell)channel).setPtyType("xterm");

            // Enable agent-forwarding.
            //((ChannelShell)channel).setAgentForwarding(true);

//            channel.setInputStream(System.in);
            channel.setInputStream(htmlEditorInputQueueStream);
      /*
      // a hack for MS-DOS prompt on Windows.
      channel.setInputStream(new FilterInputStream(System.in){
          public int read(byte[] b, int off, int len)throws IOException{
            return in.read(b, off, (len>1024?1024:len));
          }
        });
       */

//        channel.setOutputStream(System.out);
//            htmlEditorOutputStream = new HTMLEditorOutputStream(webView, htmlEditorTabPane);
            htmlEditorOutputCharStream = new HTMLEditorOutputCharStream(webView);
            channel.setOutputStream(htmlEditorOutputCharStream);
      /*
      // Choose the pty-type "vt102".
      设置终端类型
      ((ChannelShell)channel).setPtyType("vt102");
      设置终端类型和终端尺寸
    ((ChannelShell)channel).setPtyType("vt102",80, 24, 640, 480);
      */

      /*
      // Set environment variable "LANG" as "ja_JP.eucJP".
      ((ChannelShell)channel).setEnv("LANG", "ja_JP.eucJP");
      修改编码无法解决删除中文时多删除字符的bug
      ((ChannelShell)channel).setEnv("LANG", "zh_CN.UTF-8");
      ((ChannelShell)channel).setEnv("LANG", "zh_CN.UTF-16");
      */
            ((ChannelShell)channel).setEnv("LANG", "zh_CN.UTF-8");

            //channel.connect();
            channel.connect(3 * 1000);
            log.info("ssh连接成功:{}@{}", user, host);
            String id = String.valueOf(System.currentTimeMillis());

            Platform.runLater(new HTMLEditorRemoveTabRunnable(htmlEditor));
            Platform.runLater(new WebViewExecuteScriptThread(webView, null, WebViewExecuteScriptThread.initWebViewScript, null, 0));
            Platform.runLater(new WebViewExecuteScriptThread(webView, null, WebViewExecuteScriptThread.cleanConnecting, null, 0));
            Platform.runLater(new WebViewExecuteScriptThread(webView, null, WebViewExecuteScriptThread.INITIAL_FULL_SCREEN_SCRIPT, null, 0));
            Platform.runLater(new TextAreaUpdateThread(textArea));
        } catch (Exception e) {
            log.error("FxSH run error:", e);
            JOptionPane.showMessageDialog(null,"FxSH run error:"+e.getMessage());
        }
    }


    public static abstract class MyUserInfo
            implements UserInfo, UIKeyboardInteractive {
        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean promptYesNo(String str) {
            return false;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public boolean promptPassphrase(String message) {
            return false;
        }

        @Override
        public boolean promptPassword(String message) {
            return false;
        }

        @Override
        public void showMessage(String message) {
        }

        @Override
        public String[] promptKeyboardInteractive(String destination,
                                                  String name,
                                                  String instruction,
                                                  String[] prompt,
                                                  boolean[] echo) {
            return null;
        }
    }

    public static String encrypt(String data) throws Exception {

        aseKey = new SecretKeySpec(digest, "AES");
        encryptionCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//        encryptionCipher = Cipher.getInstance("AES/GCM/NoPadding");
        encryptionCipher.init(Cipher.ENCRYPT_MODE, aseKey);
        return bytesToHexString(encryptionCipher.doFinal(data.getBytes()));
    }

    public static String decrypt(String data) throws Exception {

        aseKey = new SecretKeySpec(digest, "AES");
        byte[] dataInBytes = hexStringToBytes(data);
        Cipher decryptionCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//        GCMParameterSpec spec = new GCMParameterSpec(DATA_LENGTH, encryptionCipher.getIV());
//        decryptionCipher.init(Cipher.DECRYPT_MODE, aseKey, spec);
        decryptionCipher.init(Cipher.DECRYPT_MODE, aseKey);
        byte[] decryptedBytes = decryptionCipher.doFinal(dataInBytes);
        return new String(decryptedBytes);
    }

    private static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private static byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static String printHexString(byte[] b) {
        String a = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }

            a = a + hex;
        }
        return a;
    }

    public HTMLEditorOutputStream getHtmlEditorOutputStream() {
        return htmlEditorOutputStream;
    }
}
