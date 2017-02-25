package com.phoneinfo;

import java.io.Serializable;

public class Phone implements Serializable {
    private final String mainPath = "/home/nathan/LocalMedia/";
    private String computerIP = "localmovies.hopto.org";
    private String userName;
    private String password;
    private String currentPath;
    private String videoPath;
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public Phone setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getComputerIP() {
        return computerIP;
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