package com.github.rahmnathan.localmovies.app.google.cast.control;

import android.net.Uri;

import com.github.rahmnathan.localmovies.app.data.Client;
import com.github.rahmnathan.localmovies.app.data.Media;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.common.images.WebImage;
import com.google.common.net.MediaType;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GoogleCastUtils {
    private static final Logger logger = Logger.getLogger(GoogleCastUtils.class.getName());

    public static MediaQueueItem[] assembleMediaQueue(List<Media> media, String posterPath, Client myClient) {
        return media.stream()
                .map(title -> buildMediaQueueItem(title, posterPath, myClient))
                .toArray(MediaQueueItem[]::new);
    }

    private static MediaQueueItem buildMediaQueueItem(Media media, String posterPath, Client myClient){
        WebImage image = new WebImage(Uri.parse(myClient.getComputerUrl()
                + "/localmovie/v2/media/poster?access_token=" + myClient.getAccessToken()
                + "&path=" + posterPath));

        String movieUrl = myClient.getComputerUrl()
                + "/localmovie/v2/media/stream.mp4?access_token=" + myClient.getAccessToken()
                + "&path=" + encodeParameter(myClient.getCurrentPath() + File.separator + media.getFilename());

        MediaMetadata metaData = new MediaMetadata();
        metaData.putString(MediaMetadata.KEY_TITLE, media.getTitle());
        metaData.addImage(image);

        MediaInfo mediaInfo = new MediaInfo.Builder(movieUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(MediaType.ANY_VIDEO_TYPE.toString())
                .setMetadata(metaData)
                .build();

        return new MediaQueueItem.Builder(mediaInfo)
                .setAutoplay(true)
                .setPreloadTime(30)
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