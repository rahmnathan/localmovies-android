package com.github.rahmnathan.localmovies.info.provider.data;

import java.io.Serializable;
import java.util.Comparator;

public class MovieInfo implements Serializable, Comparator<MovieInfo> {
    private final String title;
    private final String IMDBRating;
    private final String metaRating;
    private final String image;
    private final String releaseYear;
    private final Long created;
    private final int views;

    private MovieInfo(String title, String IMDBRating, String metaRating, String image, String releaseYear,
                      Long created, int views) {
        this.title = title;
        this.IMDBRating = IMDBRating;
        this.metaRating = metaRating;
        this.image = image;
        this.releaseYear = releaseYear;
        this.created = created;
        this.views = views;
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

    public Long getCreated() {
        return created;
    }

    public int getViews() {
        return views;
    }

    @Override
    public String toString(){
        return title;
    }

    @Override
    public int compare(MovieInfo info1, MovieInfo info2){
        return info1.getTitle().compareTo(info2.getTitle());
    }

    public static class Builder {
        private String title;
        private String IMDBRating;
        private String metaRating;
        private String image;
        private String releaseYear;
        private long created;
        private int views;

        public static Builder newInstance(){
            return new Builder();
        }

        public Builder setCreated(long created) {
            this.created = created;
            return this;
        }

        public Builder setViews(int views) {
            this.views = views;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setIMDBRating(String IMDBRating) {
            this.IMDBRating = IMDBRating;
            return this;
        }

        public Builder setMetaRating(String metaRating) {
            this.metaRating = metaRating;
            return this;
        }

        public Builder setImage(String image) {
            this.image = image;
            return this;
        }

        public Builder setReleaseYear(String releaseYear) {
            this.releaseYear = releaseYear;
            return this;
        }

        public MovieInfo build(){
            return new MovieInfo(title, IMDBRating, metaRating, image, releaseYear, created, views);
        }
    }
}