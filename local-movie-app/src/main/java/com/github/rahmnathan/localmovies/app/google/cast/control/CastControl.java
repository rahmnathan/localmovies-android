package com.github.rahmnathan.localmovies.app.google.cast.control;

import android.net.Uri;

import com.github.rahmnathan.localmovies.client.Client;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.common.images.WebImage;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CastControl {

    private static final Logger logger = Logger.getLogger(CastControl.class.getName());

    public static MediaQueueItem[] assembleMediaQueue(List<String> titles, String posterPath, Client myClient) {
        List<MediaQueueItem> mediaQueueItems = new ArrayList<>();
        titles.forEach(title -> {
            MediaMetadata metaData = new MediaMetadata();
            metaData.addImage(new WebImage(Uri.parse(myClient.getComputerUrl()
                    + "/movie-api/v1/poster?access_token=" + myClient.getAccessToken() + "&path="
                    + encodeParameter(posterPath))));

            metaData.putString(MediaMetadata.KEY_TITLE, title.substring(0, title.length() - 4));
            String url = myClient.getComputerUrl() + "/movie-api/v1/video.mp4?access_token="
                    + myClient.getAccessToken() + "&path=" + encodeParameter(myClient.getCurrentPath() + title);
            MediaInfo mediaInfo = new MediaInfo.Builder(url)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setContentType("videos/mp4")
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
        try{
            return URLEncoder.encode(parameter, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e){
            logger.severe(e.toString());
            return "";
        }
    }
}
