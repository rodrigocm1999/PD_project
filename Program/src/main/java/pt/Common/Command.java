package pt.Common;

import java.io.Serializable;

public class Command implements Serializable {

    public static final long serialVersionUID = 54893972L;

    private String protocol;
    private Object extras;

    public Command(String protocol, Object extras) {
        this.protocol = protocol;
        this.extras = extras;
    }

    public Command(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Object getExtras() {
        return extras;
    }

    public void setExtras(Object extras) {
        this.extras = extras;
    }

    @Override
    public String toString() {
        return "Command{" +
                "protocol='" + protocol + '\'' +
                ", extras='" + extras + '\'' +
                '}';
    }
}
