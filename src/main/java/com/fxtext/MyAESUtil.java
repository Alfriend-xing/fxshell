package com.fxtext;

import lombok.extern.slf4j.Slf4j;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;

@Slf4j
public class MyAESUtil {

    // 加密
    public static String Encrypt(String sSrc, byte[] raw) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");//"算法/模式/补码方式"
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));
        return new BASE64Encoder().encode(encrypted);//此处使用BASE64做转码功能，同时能起到2次加密的作用。
    }

    // 解密
    public static String Decrypt(String sSrc, byte[] raw) throws Exception {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] encrypted1 = new BASE64Decoder().decodeBuffer(sSrc);//先用base64解密
            try {
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original, "utf-8");
                return originalString;
            } catch (Exception e) {
                System.out.println(e.toString());
                return null;
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
            return null;
        }
    }

    public static String encryptByUsername(String sSrc) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            String key = new String();
            return MyAESUtil.Encrypt(sSrc, Arrays.copyOf(md5.digest(System.getenv().get("USERNAME").getBytes()), 16));
        } catch (Exception e) {
            log.error(null, e);
        }
        return null;
    }

    public static String decryptByUsername(String sSrc) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            String key = new String(md5.digest(System.getenv().get("USERNAME").getBytes("utf-8")));
            return MyAESUtil.Decrypt(sSrc, Arrays.copyOf(md5.digest(System.getenv().get("USERNAME").getBytes()), 16));
        } catch (Exception e) {
            log.error(null, e);
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        /*
         * 此处使用AES-128-ECB加密模式，key需要为16位。
         */
        String cKey = "jkl;POIU1234++==";
        // 需要加密的字串
        String cSrc = "www.gowhere.so";
        System.out.println(cSrc);
        // 加密
//        String enString = MyAESUtil.Encrypt(cSrc, cKey);
        String enString = MyAESUtil.encryptByUsername(cSrc);
        System.out.println("加密后的字串是：" + enString);

        // 解密
//        String DeString = MyAESUtil.Decrypt(enString, cKey);
        String DeString = MyAESUtil.decryptByUsername(enString);
        System.out.println("解密后的字串是：" + DeString);
    }
}