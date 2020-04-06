package com.github.rahmnathan.localmovies.app.activity.main.view

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.data.MediaOrder
import rahmnathan.localmovies.R

object ListAdapterUtils {

    @JvmStatic
    fun sort(mediaList: MutableList<Media>, order: MediaOrder?) {
        when (order) {
            MediaOrder.DATE_ADDED -> mediaList.sortByDescending { media -> media.created }
            MediaOrder.RATING -> mediaList.sortByDescending { media -> media.imdbRating }
            MediaOrder.RELEASE_YEAR -> mediaList.sortByDescending { media -> media.releaseYear }
            MediaOrder.TITLE -> mediaList.sortBy { media -> media.title }
        }
    }

    @JvmStatic
    fun mapTitleToView(title: String?, titleView: TextView, fontSize: Int) {
        titleView.text = title
        titleView.textSize = fontSize.toFloat()
        titleView.gravity = Gravity.CENTER
        titleView.setTextColor(Color.WHITE)
    }

    @JvmStatic
    fun mapRatingsToView(imdbRating: String?, metaRating: String?, ratings: TextView) {
        ratings.gravity = Gravity.CENTER
        ratings.setTextColor(Color.WHITE)
        ratings.textSize = 12f
        ratings.text = String.format("IMDB: %s Meta: %s", imdbRating, metaRating)
    }

    @JvmStatic
    fun mapImageToView(base64Image: String?, imageView: ImageView) {
        if (base64Image != null && base64Image != "" && base64Image != "null") {
            val image = Base64.decode(base64Image, Base64.DEFAULT)
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.size))
        } else {
            imageView.setImageResource(R.mipmap.no_poster)
        }
    }

    @JvmStatic
    fun mapYearToView(releaseYear: String?, year: TextView, fontSize: Int) {
        year.text = String.format("Release Year: %s", releaseYear)
        year.setTextColor(Color.WHITE)
        year.gravity = Gravity.CENTER
        year.textSize = fontSize.toFloat()
    }
}