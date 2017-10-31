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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GoogleCastUtils {
    private static final Logger logger = Logger.getLogger(GoogleCastUtils.class.getName());

    public static MediaQueueItem[] assembleMediaQueue(List<MovieInfo> titles, String posterPath, Client myClient) {
        List<MediaQueueItem> mediaQueueItems = new ArrayList<>();
        titles.forEach(movieInfo -> {
            MediaMetadata metaData = new MediaMetadata();
            metaData.addImage(new WebImage(Uri.parse(myClient.getComputerUrl()
                    + "/movie-api/poster?access_token=" + myClient.getAccessToken() + "&path="
                    + encodeParameter(posterPath))));

            metaData.putString(MediaMetadata.KEY_TITLE, movieInfo.getTitle());
            String url = myClient.getComputerUrl() + "/movie-api/video.mp4?access_token="
                    + myClient.getAccessToken() + "&path=" + encodeParameter(myClient.getCurrentPath() + movieInfo.getFilename());
            MediaInfo mediaInfo = new MediaInfo.Builder(url)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setContentType(MediaType.MP4_VIDEO.toString())
                    .setMetadata(metaData)
                    .build();
            MediaQueueItem queueItem = new MediaQueueItem.Builder(mediaInfo)
                    .setAutoplay(true)
                    .setPreloadTime(20)
                    .build();
            mediaQueueItems.add(queueItem);
        });
        MediaQueueItem[] queueItems = new MediaQueueItem[mediaQueueItems.size()];
        for (int i = 0; i < mediaQueueItems.size(); i++) {
            queueItems[i] = mediaQueueItems.get(i);
        }

        return queueItems;
    }

    private static String encodeParameter(String parameter) {
        try {
            return URLEncoder.encode(parameter, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            logger.severe(e.toString());
            return "";
        }
    }
}