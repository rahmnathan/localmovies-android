package com.rahmnathan;

import java.io.Serializable;

public class MovieInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private String title;
    private String IMDBRating;
    private String MetaRating;
    private byte[] image;
    private String releaseYear;

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