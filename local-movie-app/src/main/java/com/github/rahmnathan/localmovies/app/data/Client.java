package com.github.rahmnathan.localmovies.app.data;

import java.io.Serializable;

public class Client implements Serializable {
    private final LocalMediaPath mainPath = new LocalMediaPath();
    private String computerUrl = "https://movies.nathanrahm.com";
    private LocalMediaPath currentPath = mainPath;
    private Integer movieCount;
    private String accessToken;
    private Long lastUpdate;
    private String userName;
    private String password;

    public Client(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public Client() {
    }

    public Long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean isViewingEpisodes(){
        return currentPath.size() == 3;
    }

    private boolean isViewingMovies(){
        return currentPath.toString().toLowerCase().contains("movies");
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

    public String getPassword() {
        return password;
    }

    public String getComputerUrl() {
        return computerUrl;
    }
}