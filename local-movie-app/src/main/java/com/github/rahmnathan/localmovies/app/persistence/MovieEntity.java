package com.github.rahmnathan.localmovies.app.persistence;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.github.rahmnathan.localmovies.app.data.Media;

@Entity
public class MovieEntity {
    @PrimaryKey(autoGenerate = true)
    private Long id;
    private String directoryPath;
    @Embedded
    private Media media;

    public MovieEntity(String directoryPath, Media media) {
        this.directoryPath = directoryPath;
        this.media = media;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String path) {
        this.directoryPath = path;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }
}
