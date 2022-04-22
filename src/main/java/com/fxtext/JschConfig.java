package com.fxtext;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

public class JschConfig implements Serializable {

    private List<SessionConfig> sessionConfigs;

    public List<SessionConfig> getSessionConfigs() {
        return sessionConfigs;
    }

    public void setSessionConfigs(List<SessionConfig> sessionConfigs) {
        this.sessionConfigs = sessionConfigs;
    }

    @Data
    public static class SessionConfig implements Serializable {
        private String sessionName;
        private String host;
        private String user;
        private String passwordAesString;

        public void setPasswordAesString(String passwordAesString) {
            if (passwordAesString == null || "".equals(passwordAesString)) {
                return;
            }
            this.passwordAesString = MyAESUtil.encryptByUsername(passwordAesString);
        }

        public String getPasswordAesString() {
            if (passwordAesString == null || "".equals(passwordAesString)) {
                return null;
            }
            return MyAESUtil.decryptByUsername(passwordAesString);
        }
    }
}
