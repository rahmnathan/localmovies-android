package com.rahmnathan;

import java.util.List;

public interface MovieInfoProviderInterface {

    public List<MovieInfo> getMovieInfo(List<String> titleList, String currentPath, String dataDirectory);
}
