package com.github.rahmnathan.localmovies.info.provider.data;

import java.io.Serializable;
import java.util.Comparator;

public class Movie implements Serializable, Comparator<Movie> {
    private final String title;
    private final String filename;
    private final String IMDBRating;
    private final String metaRating;
    private final String image;
    private final String releaseYear;
    private final String genre;
    private final Long created;
    private final int views;

    private Movie(String title, String IMDBRating, String metaRating, String image, String releaseYear,
                  Long created, int views, String genre, String filename) {
        this.title = title;
        this.filename = filename;
        this.IMDBRating = IMDBRating;
        this.metaRating = metaRating;
        this.image = image;
        this.releaseYear = releaseYear;
        this.created = created;
        this.views = views;
        this.genre = genre;
    }

    public String getFilename() {
        return filename;
    }

    public String getGenre() {
        return genre;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public String getTitle() {
        return title;
    }

    public String getIMDBRating() {
        return IMDBRating;
    }

    public String getMetaRating() {
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
    public String toString() {
        return title;
    }

    @Override
    public int compare(Movie info1, Movie info2) {
        return info1.getTitle().compareTo(info2.getTitle());
    }

    public static class Builder {
        private String title;
        private String fileName;
        private String IMDBRating;
        private String metaRating;
        private String image;
        private String releaseYear;
        private String genre = "";
        private long created;
        private int views;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder setGenre(String genre) {
            if(genre != null)
                this.genre = genre.toLowerCase();
            return this;
        }

        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
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

        public Movie build() {
            return new Movie(title, IMDBRating, metaRating, image, releaseYear, created, views, genre, fileName);
        }
    }
}