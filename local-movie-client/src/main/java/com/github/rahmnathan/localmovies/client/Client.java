package com.github.rahmnathan.localmovies.client;

import java.io.Serializable;

public class Client implements Serializable {
    private final LocalMediaPath mainPath = new LocalMediaPath();
    private LocalMediaPath currentPath = mainPath;
    private String computerUrl = "https://localmovies.hopto.org";
    private String userName;
    private String password;
    private String videoPath;
    private String accessToken;
    private Integer movieCount;

    public Client(String computerUrl, String userName, String password) {
        this.computerUrl = computerUrl;
        this.userName = userName;
        this.password = password;
    }

    public Client() {
    }

    public boolean isViewingEpisodes(){
        return currentPath.size() == 3;
    }

    private boolean isViewingMovies(){
        return getCurrentPath().toString().toLowerCase().contains("movies");
    }

    public void resetCurrentPath(){
        currentPath = new LocalMediaPath();
    }

    public boolean isViewingVideos(){
        return isViewingEpisodes() || isViewingMovies();
    }

    public LocalMediaPath getCurrentPath() {
        return currentPath;
    }

    public void popOneDirectory(){
        currentPath.removeLast();
    }

    public void appendToCurrentPath(String directory){
        currentPath.addLast(directory);
    }

    public int getMovieCount() {
        return movieCount;
    }

    public void setMovieCount(Integer movieCount) {
        this.movieCount = movieCount;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
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

    public String getComputerUrl() {
        return computerUrl;
    }

    public void setComputerUrl(String computerUrl){
        this.computerUrl = computerUrl;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }
}