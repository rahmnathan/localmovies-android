package com.github.rahmnathan.localmovies.info.provider.data;

public class MovieInfoRequest {
    private final int page;
    private final int resultsPerPage;
    private final String path;
    private final String deviceId;
    private final String pushToken;
    private final String client = "ANDROID";

    public MovieInfoRequest(int page, int resultsPerPage, String path, String deviceId, String pushToken) {
        this.page = page;
        this.resultsPerPage = resultsPerPage;
        this.path = path;
        this.deviceId = deviceId;
        this.pushToken = pushToken;
    }

    public int getPage() {
        return page;
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public String getPath() {
        return path;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public String getClient() {
        return client;
    }

    public static class Builder {
        private int page;
        private int resultsPerPage;
        private String path;
        private String deviceId;
        private String pushToken;

        public static Builder newInstance(){
            return new Builder();
        }

        public Builder setPage(int page) {
            this.page = page;
            return this;
        }

        public Builder setResultsPerPage(int resultsPerPage) {
            this.resultsPerPage = resultsPerPage;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setDeviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder setPushToken(String pushToken) {
            this.pushToken = pushToken;
            return this;
        }

        public MovieInfoRequest build(){
            return new MovieInfoRequest(page, resultsPerPage, path, deviceId, pushToken);
        }
    }
}
