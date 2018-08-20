package com.github.rahmnathan.localmovies.app.data;

public enum MovieGenre {
    ACTION("action"),
    SCIFI("sci-fi"),
    HORROR("horror"),
    COMEDY("comedy"),
    THRILLER("thriller"),
    FANTASY("fantasy");

    private final String formattedName;

    MovieGenre(String formattedName) {
        this.formattedName = formattedName;
    }

    public String getFormattedName() {
        return formattedName;
    }
}
