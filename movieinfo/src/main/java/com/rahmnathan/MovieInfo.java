package com.rahmnathan;

import java.io.Serializable;

public class MovieInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private String title;
    private String IMDBRating;
    private String MetaRating;
    private byte[] image;
    private String releaseYear;
    private String rating;
    private String actors;

    public void setActors(String actors) {
        this.actors = actors;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public void setReleaseYear(String releaseYear){
        this.releaseYear = releaseYear;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setIMDBRating(String IMDBRating) {
        this.IMDBRating = IMDBRating;
    }

    public void setImage(byte[] image){
        this.image = image;
    }

    public void setMetaRating(String metaRating) {
        MetaRating = metaRating;
    }

    public String getActors() {
        return actors;
    }

    public String getRating() {
        return rating;
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
        return MetaRating;
    }

    public byte[] getImage() {
        return image;
    }

    @Override
    public String toString(){
        return title;
    }
}