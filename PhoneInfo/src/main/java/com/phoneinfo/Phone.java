package com.phoneinfo;

import java.io.Serializable;

public class Phone implements Serializable {
    private final String mainPath;
    private String computerIP;
    private String currentPath;
    private String videoPath;

    public Phone(String mainPath) {
        this.mainPath = mainPath;
    }

    public String getComputerIP() {
        return computerIP;
    }

    public void setComputerIP(String address){
        this.computerIP = address;
    }

    public void setCurrentPath(String currentPath){
        this.currentPath = currentPath;
    }

    public String getCurrentPath(){
        return currentPath;
    }

    public String getMainPath(){
        return mainPath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }
}