package com.rahmnathan;

import java.io.Serializable;

public class MovieInfo implements Serializable {

    private final String title;
    private final String IMDBRating;
    private final String metaRating;
    private final String image;
    private final String releaseYear;

    private MovieInfo(String title, String IMDBRating, String metaRating, String image, String releaseYear) {
        this.title = title;
        this.IMDBRating = IMDBRating;
        this.metaRating = metaRating;
        this.image = image;
        this.releaseYear = releaseYear;
    }

    public String getReleaseYear(){
        return releaseYear;
    }

    public String getTitle(){
        return title;
    }

    public String getIMDBRating(){
        return IMDBRating;
    }

    public String getMetaRating(){
        return metaRating;
    }

    public String getImage() {
        return image;
    }

    @Override
    public String toString(){
        return title;
    }

    public static class Builder {

        private String title;
        private String IMDBRating;
        private String metaRating;
        private String image;
        private String releaseYear;

        public static Builder newInstace(){
            return new Builder();
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setIMDBRating(String IMDBRating) {
            this.IMDBRating = IMDBRating;
        }

        public void setMetaRating(String metaRating) {
            this.metaRating = metaRating;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public void setReleaseYear(String releaseYear) {
            this.releaseYear = releaseYear;
        }
        public MovieInfo build(){
            return new MovieInfo(title, IMDBRating, metaRating, image, releaseYear);
        }
    }
}