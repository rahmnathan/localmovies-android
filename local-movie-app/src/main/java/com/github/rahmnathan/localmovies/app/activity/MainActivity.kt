package com.github.rahmnathan.localmovies.app.activity

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import com.github.rahmnathan.localmovies.app.activity.DescriptionPopUpActivity
import com.github.rahmnathan.localmovies.app.activity.MainActivity
import com.github.rahmnathan.localmovies.app.activity.SetupActivity
import com.github.rahmnathan.localmovies.app.adapter.external.keycloak.KeycloakAuthenticator
import com.github.rahmnathan.localmovies.app.adapter.list.MovieListAdapter
import com.github.rahmnathan.localmovies.app.control.MainActivityUtils.getPhoneInfo
import com.github.rahmnathan.localmovies.app.control.MainActivityUtils.sortVideoList
import com.github.rahmnathan.localmovies.app.control.MovieClickListener.Builder.Companion.newInstance
import com.github.rahmnathan.localmovies.app.control.MovieClickListener.Companion.getVideos
import com.github.rahmnathan.localmovies.app.control.MovieEventLoader
import com.github.rahmnathan.localmovies.app.control.MoviePersistenceManager
import com.github.rahmnathan.localmovies.app.control.MovieSearchTextWatcher
import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.localmovies.app.data.Media
import com.github.rahmnathan.localmovies.app.google.cast.config.ExpandedControlActivity
import com.github.rahmnathan.localmovies.app.persistence.MovieHistory
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import rahmnathan.localmovies.R
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val movieCache: ConcurrentMap<String, MutableList<Media>> = ConcurrentHashMap()
    private val executorService = Executors.newSingleThreadExecutor()

    @Volatile
    private var persistenceManager: MoviePersistenceManager? = null
    private var listAdapter: MovieListAdapter? = null
    private var history: MovieHistory? = null
    private var progressBar: ProgressBar? = null
    private var gridView: GridView? = null
    private var castContext: CastContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        history = MovieHistory(this)
        progressBar = findViewById(R.id.progressBar)
        listAdapter = MovieListAdapter(this, ArrayList())
        gridView = findViewById(R.id.gridView)
        gridView?.setAdapter(listAdapter)
        castContext = CastContext.getSharedInstance(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        menuInflater.inflate(R.menu.cast, toolbar.menu)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, toolbar.menu, R.id.media_route_menu_item)
        val searchView = MenuItemCompat.getActionView(toolbar.menu.findItem(R.id.action_search)) as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnQueryTextListener(MovieSearchTextWatcher(listAdapter!!))
        val homeItem = PrimaryDrawerItem()
                .withIdentifier(1)
                .withName("Home")
                .withTextColor(Color.WHITE)
                .withOnDrawerItemClickListener { view: View?, position: Int, drawerItem: IDrawerItem<*, *>? ->
                    getRootVideos(MOVIES, searchView)
                    false
                }
        val historyItem = PrimaryDrawerItem()
                .withIdentifier(2)
                .withName("History")
                .withTextColor(Color.WHITE)
                .withOnDrawerItemClickListener { view: View?, position: Int, drawerItem: IDrawerItem<*, *>? ->
                    client!!.resetCurrentPath()
                    client!!.appendToCurrentPath(MOVIES)
                    listAdapter!!.display(history!!.historyList)
                    false
                }
        val settingsItem = PrimaryDrawerItem()
                .withIdentifier(3)
                .withName("My Account")
                .withTextColor(Color.WHITE)
                .withOnDrawerItemClickListener { view: View?, position: Int, drawerItem: IDrawerItem<*, *>? ->
                    this@MainActivity.startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                    false
                }
        val result = DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(homeItem, historyItem, settingsItem)
                .withSliderBackgroundColor(Color.BLACK)
                .build()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_movies
        bottomNavigationView.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED
        val popup = PopupMenu(this, bottomNavigationView, Gravity.END)
        popup.setOnMenuItemClickListener { item: MenuItem? ->
            sortVideoList(item!!, listAdapter!!, gridView!!)
            true
        }
        popup.menuInflater.inflate(R.menu.settings, popup.menu)
        bottomNavigationView.menu.setGroupCheckable(0, false, true)
        bottomNavigationView.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_controls -> {
                    val session = castContext?.getSessionManager()!!.currentCastSession
                    if (session != null && session.isConnected) {
                        startActivity(Intent(this@MainActivity, ExpandedControlActivity::class.java))
                    } else {
                        Toast.makeText(this, "No video playing", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.action_movies -> getRootVideos(MOVIES, searchView)
                R.id.action_series -> getRootVideos(SERIES, searchView)
                R.id.action_more -> popup.show()
            }
            true
        }
        persistenceManager = MoviePersistenceManager(movieCache, this, executorService)
        try {
            client = getPhoneInfo(openFileInput("setup"))
            client!!.appendToCurrentPath(MOVIES)
            CompletableFuture.runAsync(KeycloakAuthenticator(client!!), executorService)
                    .thenRun { loadVideos() }
                    .thenRun(MovieEventLoader(listAdapter!!, client!!, persistenceManager!!, this))
        } catch (e: Exception) {
            startActivity(Intent(this@MainActivity, SetupActivity::class.java))
        }

        val clickListener = newInstance()
                .setCastContext(castContext)
                .setContext(this)
                .setProgressBar(progressBar)
                .setClient(client)
                .setMovieListAdapter(listAdapter)
                .setMovieInfoCache(persistenceManager)
                .setMovieHistory(history)
                .build()
        gridView?.setOnItemClickListener(clickListener)
        gridView?.setOnItemLongClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val intent = Intent(this, DescriptionPopUpActivity::class.java)
            intent.putExtra(DescriptionPopUpActivity.MOVIE, listAdapter!!.getMovie(position))
            startActivity(intent)
            true
        }
    }

    private fun getRootVideos(path: String, searchText: SearchView) {
        client!!.resetCurrentPath()
        searchText.setQuery("", false)
        client!!.appendToCurrentPath(path)
        loadVideos()
    }

    private fun loadVideos() {
        getVideos(persistenceManager, client, listAdapter, this, progressBar)
    }

    override fun onBackPressed() {
        val currentDirectory = client!!.currentPath.peekLast()
        if (currentDirectory.equals(SERIES, ignoreCase = true) || currentDirectory.equals(MOVIES, ignoreCase = true)) System.exit(8)
        client!!.popOneDirectory()
        loadVideos()
    }

    companion object {
        private const val MOVIES = "Movies"
        private const val SERIES = "Series"
        var client: Client? = null
    }
}