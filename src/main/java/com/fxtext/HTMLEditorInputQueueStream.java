package com.fxtext;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class HTMLEditorInputQueueStream extends InputStream {
    final BlockingQueue<byte[]> q;

    public HTMLEditorInputQueueStream() {
        q = new LinkedBlockingQueue<>();
    }

    private byte[] s;
    int pos;

    @Override
    public int read() throws IOException {
        while (null == s || s.length <= pos) {
            try {
                s = q.take();
                pos = 0;
            } catch (InterruptedException ex) {
                log.error(null, ex);
            }
        }
        int ret = (int) s[pos];
        pos++;
        return ret;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytes_copied = 0;
        while (bytes_copied < 1) {
            while (null == s || s.length <= pos) {
                try {
                    s = q.take();
                    log.info("HTMLEditorInputQueueStream: {}", s);
                    pos = 0;
                } catch (InterruptedException ex) {
                    log.error(null, ex);
                }
            }
            int bytes_to_copy = len < s.length - pos ? len : s.length - pos;
            System.arraycopy(s, pos, b, off, bytes_to_copy);
            pos += bytes_to_copy;
            bytes_copied += bytes_to_copy;
        }
        return bytes_copied;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length); //To change body of generated methods, choose Tools | Templates.
    }
}
