package com.example;

import java.io.Serializable;

public class Phone implements Serializable {
    private final String mainPath;
    private final String castIP;
    private final String phoneName;
    private String computerIP;
    private String path;

    public Phone(String castIP, String phoneName, String mainPath) {
        this.castIP = castIP;
        this.phoneName = phoneName;
        this.mainPath = mainPath;
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

    public String getMainPath(){
        return mainPath;
    }
}