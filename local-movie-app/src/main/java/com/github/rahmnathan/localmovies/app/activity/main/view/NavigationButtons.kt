package com.github.rahmnathan.localmovies.app.activity.main.view

import android.content.Intent
import android.view.Gravity
import android.view.MenuItem
import android.widget.GridView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import com.github.rahmnathan.localmovies.app.Client
import com.github.rahmnathan.localmovies.app.activity.main.MainActivity
import com.github.rahmnathan.localmovies.app.cast.config.ExpandedControlActivity
import com.github.rahmnathan.localmovies.app.media.data.MediaEndpoint
import com.google.android.gms.cast.framework.CastContext
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import rahmnathan.localmovies.R

object NavigationButtons {

    @JvmStatic
    fun build(searchView: SearchView, activity: MainActivity, listAdapter: MediaListAdapter, gridView: GridView, castContext: CastContext, client: Client) {
        val bottomNavigationView = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_movies
        bottomNavigationView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_AUTO
        val popup = PopupMenu(activity, bottomNavigationView, Gravity.END)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            ListAdapterUtils.sortVideoList(item, listAdapter, gridView)
            true
        }
        popup.menuInflater.inflate(R.menu.settings, popup.menu)
        bottomNavigationView.menu.setGroupCheckable(0, false, true)
        bottomNavigationView.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_controls -> {
                    val session = castContext.sessionManager.currentCastSession
                    if (session != null && session.isConnected) {
                        activity.startActivity(Intent(activity, ExpandedControlActivity::class.java))
                    } else {
                        Toast.makeText(activity, "No video playing", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.action_movies -> {
                    client.endpoint = MediaEndpoint.MEDIA
                    activity.getRootVideos(MainActivity.MOVIES, searchView)
                }
                R.id.action_series -> {
                    client.endpoint = MediaEndpoint.MEDIA
                    activity.getRootVideos(MainActivity.SERIES, searchView)
                }
                R.id.action_more -> popup.show()
            }
            true
        }
    }
}