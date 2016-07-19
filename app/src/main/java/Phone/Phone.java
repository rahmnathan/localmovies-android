package Phone;

import java.io.Serializable;

/**
 * Created by Nathan on 6/11/2016.
 */
public class Phone implements Serializable {
    private static final long serialVersionUID = 514387573;
    private String phoneIP;
    private String computerIP;
    private String castIP;
    private String phoneName;
    private String path;
    private boolean casting;

    public Phone(String castIP, String phoneIP, String phoneName, String path) {
        this.phoneIP = phoneIP;
        this.castIP = castIP;
        this.phoneName = phoneName;
        this.path = path;
    }

    public String getPhoneIP() {
        return phoneIP;
    }

    public String getComputerIP() {
        return computerIP;
    }

    public String getCastIP() {
        return castIP;
    }

    public void setComputerIP(String address){
        this.computerIP = address;
    }

    public void setPath(String path){
        this.path = path;
    }

    public String getPhoneName() {
        return phoneName;
    }

    public String getPath(){
        return path;
    }

    public void setCasting(boolean casting){
        this.casting = casting;
    }

    public boolean isCasting(){
        return casting;
    }
}