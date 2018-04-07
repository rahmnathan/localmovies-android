package com.github.rahmnathan.localmovies.app.google.cast.control;

import android.net.Uri;

import com.github.rahmnathan.localmovies.client.Client;
import com.github.rahmnathan.localmovies.info.provider.data.MovieInfo;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.common.images.WebImage;
import com.google.common.net.MediaType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GoogleCastUtils {
    private static final Logger logger = Logger.getLogger(GoogleCastUtils.class.getName());

    public static MediaQueueItem[] assembleMediaQueue(List<MovieInfo> titles, String posterPath, Client myClient) {
        List<MediaQueueItem> mediaQueueItems = titles.stream()
                .map(title -> buildMediaQueue(title, posterPath, myClient))
                .collect(Collectors.toList());

        MediaQueueItem[] queueItems = new MediaQueueItem[mediaQueueItems.size()];
        mediaQueueItems.toArray(queueItems);
        return queueItems;
    }

    private static MediaQueueItem buildMediaQueue(MovieInfo movieInfo, String posterPath, Client myClient){
        WebImage image = new WebImage(Uri.parse(myClient.getComputerUrl()
                + "/movie-api/poster?access_token=" + myClient.getAccessToken()
                + "&path=" + encodeParameter(posterPath)));

        String movieUrl = myClient.getComputerUrl()
                + "/movie-api/video.mp4?access_token=" + myClient.getAccessToken()
                + "&path=" + encodeParameter(myClient.getCurrentPath() + movieInfo.getFilename());

        MediaMetadata metaData = new MediaMetadata();
        metaData.putString(MediaMetadata.KEY_TITLE, movieInfo.getTitle());
        metaData.addImage(image);

        MediaInfo mediaInfo = new MediaInfo.Builder(movieUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(MediaType.MP4_VIDEO.toString())
                .setMetadata(metaData)
                .build();

        return new MediaQueueItem.Builder(mediaInfo)
                .setAutoplay(true)
                .setPreloadTime(20)
                .build();
    }

    private static String encodeParameter(String parameter) {
        try {
            return URLEncoder.encode(parameter, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "Failed to encode parameter: " + parameter, e);
            return "";
        }
    }
}