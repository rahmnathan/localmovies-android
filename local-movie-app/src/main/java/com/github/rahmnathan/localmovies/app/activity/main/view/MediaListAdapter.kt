package com.github.rahmnathan.localmovies.app.activity.main.view

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.rahmnathan.localmovies.app.activity.main.view.ListAdapterUtils.mapImageToView
import com.github.rahmnathan.localmovies.app.activity.main.view.ListAdapterUtils.mapRatingsToView
import com.github.rahmnathan.localmovies.app.activity.main.view.ListAdapterUtils.mapTitleToView
import com.github.rahmnathan.localmovies.app.activity.main.view.ListAdapterUtils.mapYearToView
import com.github.rahmnathan.localmovies.app.activity.main.view.ListAdapterUtils.sort
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.data.MediaGenre
import com.github.rahmnathan.localmovies.app.media.data.MediaOrder
import rahmnathan.localmovies.R
import java.util.*
import java.util.Comparator.comparing
import java.util.stream.Collectors

class MediaListAdapter(private val context: Activity, private var media: MutableList<Media>) : ArrayAdapter<Media?>(context, R.layout.my_adapter, media as List<Media?>), Filterable {
    private val originalMediaList: MutableList<Media> = ArrayList()
    private val adapterFilter = AdapterFilter()
    var chars: CharSequence = ""
        private set

    override fun getView(position: Int, rowView: View?, parent: ViewGroup): View {
        var rowView = rowView
        val inflater = context.layoutInflater
        if (rowView == null) rowView = inflater.inflate(R.layout.my_adapter, parent, false)
        if (position >= media.size) return View(context)
        val titleView = rowView!!.findViewById<TextView>(R.id.textView)
        val imageView = rowView.findViewById<ImageView>(R.id.imageView)
        val yearView = rowView.findViewById<TextView>(R.id.year)
        val ratingView = rowView.findViewById<TextView>(R.id.rating)
        val media = media[position]

        var title = media.title
        if (media.type.equals("episode", ignoreCase = true)) {
            title = "#" + media.number + " - " + title
        } else if (media.title.startsWith("Episode ") && media.number != null) {
            title = "#" + media.number
        }

        mapTitleToView(title, titleView, 16)
        mapImageToView(media.image, imageView)
        mapYearToView(media.releaseYear, yearView, 12)
        mapRatingsToView(media.imdbRating, media.metaRating, ratingView)
        return rowView
    }

    override fun getFilter(): Filter {
        return adapterFilter
    }

    fun filterGenre(genre: MediaGenre) {
        val filteredList = originalMediaList.stream()
                .sorted(comparing(Media::title))
                .filter { movieInfo: Media -> movieInfo.genre.toLowerCase(Locale.getDefault()).contains(genre.formattedName) }
                .collect(Collectors.toList())
        display(filteredList)
    }

    fun clearLists() {
        media.clear()
        originalMediaList.clear()
    }

    fun updateHistoryList(mediaList: List<Media>?) {
        media.addAll(mediaList!!)
        originalMediaList.addAll(mediaList)
    }

    fun updateList(mediaList: List<Media>?) {
        media.addAll(mediaList!!)
        originalMediaList.addAll(mediaList)

        // If we have media, apply default sorting.
        if(mediaList.isNotEmpty()){
            val mediaType = mediaList[0].type.toLowerCase(Locale.US);
            if("episode" == mediaType || "season" == mediaType){
                sort(media, MediaOrder.NUMBER)
            } else {
                sort(media, MediaOrder.TITLE)
            }
        }
    }

    fun getOriginalMediaList(): List<Media> {
        return originalMediaList
    }

    fun getMovie(position: Int): Media {
        return media[position]
    }

    override fun getCount(): Int {
        return media.size
    }

    private inner class AdapterFilter : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            chars = charSequence
            val filterResults = FilterResults()
            media.clear()
            if (charSequence.isEmpty()) {
                media.addAll(originalMediaList)
            } else {
                media.addAll(originalMediaList.stream()
                        .filter { movie: Media -> movie.title.toLowerCase(Locale.getDefault()).contains(charSequence.toString().toLowerCase(Locale.getDefault())) }
                        .collect(Collectors.toList()))
            }
            filterResults.values = media
            filterResults.count = media.size
            return filterResults
        }

        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            media = filterResults.values as MutableList<Media>
            notifyDataSetChanged()
        }
    }

    fun display(newMedia: List<Media>) {
        media.clear()
        media.addAll(newMedia)
        notifyDataSetChanged()
    }

    fun sort(order: MediaOrder?) {
        sort(media, order)
        notifyDataSetChanged()
    }
}