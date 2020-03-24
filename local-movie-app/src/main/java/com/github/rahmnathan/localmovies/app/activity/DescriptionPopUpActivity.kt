package com.github.rahmnathan.localmovies.app.activity

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.github.rahmnathan.localmovies.app.adapter.list.ListAdapterUtils
import com.github.rahmnathan.localmovies.app.data.Media
import rahmnathan.localmovies.R

class DescriptionPopUpActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.description_popup)

        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.8f)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        window.setLayout(java.lang.Double.valueOf(width * .75).toInt(), java.lang.Double.valueOf(height * .75).toInt())

        val bundle = intent.extras
        if (bundle != null) {
            val `object` = bundle[MOVIE]
            if (`object` != null) {
                val media = `object` as Media
                val titleView = findViewById<TextView>(R.id.detailedTitle)
                val imageView = findViewById<ImageView>(R.id.detailedPoster)
                val yearView = findViewById<TextView>(R.id.detailedYear)
                val metaRatingView = findViewById<TextView>(R.id.detailedMetaRating)
                val imdbRatingView = findViewById<TextView>(R.id.detailedIMDBRating)
                val plotView = findViewById<TextView>(R.id.detailedPlot)
                val actorView = findViewById<TextView>(R.id.detailedActors)
                ListAdapterUtils.mapImageToView(media.image, imageView)
                ListAdapterUtils.mapTitleToView(media.title, titleView, 22)
                ListAdapterUtils.mapYearToView(media.releaseYear, yearView, 16)
                mapTextToView(metaRatingView, String.format("Metacritic Rating: %s", media.metaRating), 16)
                mapTextToView(imdbRatingView, String.format("IMDB Rating: %s", media.imdbRating), 16)
                mapTextToView(plotView, media.plot, 14)
                mapTextToView(actorView, String.format("Starring: %s", media.actors), 14)
            }
        }
    }

    private fun mapTextToView(textView: TextView, value: String, fontSize: Int) {
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.textSize = fontSize.toFloat()
        textView.text = value
    }

    companion object {
        const val MOVIE = "media"
    }
}