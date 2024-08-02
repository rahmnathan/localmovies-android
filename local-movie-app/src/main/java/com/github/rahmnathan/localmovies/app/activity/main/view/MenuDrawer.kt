package com.github.rahmnathan.localmovies.app.activity.main.view

import android.content.Intent
import android.graphics.Color
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import com.github.rahmnathan.localmovies.app.Client
import com.github.rahmnathan.localmovies.app.activity.main.MainActivity
import com.github.rahmnathan.localmovies.app.activity.setup.SetupActivity
import com.github.rahmnathan.localmovies.app.media.data.MediaEndpoint
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import rahmnathan.localmovies.R

object MenuDrawer {

    @JvmStatic
    fun build(searchView: SearchView,
              client: Client,
              activity: MainActivity,
              toolbar: Toolbar) {

        val homeItem = PrimaryDrawerItem()
                .withIdentifier(1)
                .withName("Home")
                .withTextColor(Color.WHITE)
                .withSelectable(false)
                .withOnDrawerItemClickListener { _: View?, _: Int, _: IDrawerItem<*, *>? ->
                    client.endpoint = MediaEndpoint.MEDIA
                    activity.getRootVideos(MainActivity.MOVIES, searchView)
                    false
                }
        val historyItem = PrimaryDrawerItem()
                .withIdentifier(2)
                .withName("History")
                .withTextColor(Color.WHITE)
                .withSelectable(false)
                .withOnDrawerItemClickListener { _: View?, _: Int, _: IDrawerItem<*, *>? ->
                    client.endpoint = MediaEndpoint.HISTORY
                    activity.getRootVideos(MainActivity.MOVIES, searchView)
                    false
                }
        val settingsItem = PrimaryDrawerItem()
                .withIdentifier(3)
                .withName("My Account")
                .withTextColor(Color.WHITE)
                .withSelectable(false)
                .withOnDrawerItemClickListener { _: View?, _: Int, _: IDrawerItem<*, *>? ->
                    activity.startActivity(Intent(activity, SetupActivity::class.java))
                    false
                }

        val result = DrawerBuilder()
                .withActivity(activity)
                .withHeader(R.layout.menu_drawer_header)
                .withToolbar(toolbar)
                .addDrawerItems(homeItem, historyItem, settingsItem)
                .withSliderBackgroundColor(Color.BLACK)
                .build()
    }
}