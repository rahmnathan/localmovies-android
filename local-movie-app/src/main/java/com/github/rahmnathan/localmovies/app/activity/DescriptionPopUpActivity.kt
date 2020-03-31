package com.github.rahmnathan.localmovies.app.activity

import android.app.Activity
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.github.rahmnathan.localmovies.app.adapter.list.ListAdapterUtils.mapImageToView
import com.github.rahmnathan.localmovies.app.adapter.list.ListAdapterUtils.mapTitleToView
import com.github.rahmnathan.localmovies.app.dagger.AppComponent
import com.github.rahmnathan.localmovies.app.dagger.AppModule
import com.github.rahmnathan.localmovies.app.dagger.DaggerAppComponent
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

                val plotView = findViewById<TextView>(R.id.detailedPlot)
                mapTextToViewDynamic(plotView, media.plot, 14)

                val imageView = findViewById<ImageView>(R.id.detailedPoster)
                mapImageToView(media.image, imageView)

                val titleView = findViewById<TextView>(R.id.detailedTitle)
                mapTitleToView(media.title, titleView, 22)

                val yearLabelView = findViewById<TextView>(R.id.detailedYearLabel)
                mapTextToViewLabel(yearLabelView, "Year")
                val yearView = findViewById<TextView>(R.id.detailedYear)
                mapTextToViewDynamic(yearView, media.releaseYear, 16)

                val metaRatingLabelView = findViewById<TextView>(R.id.detailedMetaRatingLabel)
                mapTextToViewLabel(metaRatingLabelView, "Metacritic")
                val metaRatingView = findViewById<TextView>(R.id.detailedMetaRating)
                mapTextToViewDynamic(metaRatingView, media.metaRating, 16)

                val imdbRatingLabelView = findViewById<TextView>(R.id.detailedIMDBRatingLabel)
                mapTextToViewLabel(imdbRatingLabelView, "IMDB")
                val imdbRatingView = findViewById<TextView>(R.id.detailedIMDBRating)
                mapTextToViewDynamic(imdbRatingView, media.imdbRating, 16)

                val actorView = findViewById<TextView>(R.id.detailedActors)
                mapTextToViewDynamic(actorView, media.actors, 14)
                val actorLabelView = findViewById<TextView>(R.id.detailedActorsLabel)
                mapTextToViewLabel(actorLabelView, "Starring")
            }
        }
    }

    private fun mapTextToViewLabel(textView: TextView, value: String) {
        textView.paintFlags = textView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.textSize = 18f
        textView.text = value
    }

    private fun mapTextToViewDynamic(textView: TextView, value: String, fontSize: Int) {
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.textSize = fontSize.toFloat()
        textView.text = value
    }

    companion object {
        const val MOVIE = "media"
    }
}