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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import com.github.rahmnathan.localmovies.app.LocalMoviesApplication
import com.github.rahmnathan.localmovies.app.activity.SetupActivity.Companion.SETUP_FILE
import com.github.rahmnathan.localmovies.app.adapter.external.keycloak.KeycloakAuthenticator
import com.github.rahmnathan.localmovies.app.adapter.external.localmovie.MediaFacade
import com.github.rahmnathan.localmovies.app.adapter.list.MediaListAdapter
import com.github.rahmnathan.localmovies.app.control.*
import com.github.rahmnathan.localmovies.app.data.Client
import com.github.rahmnathan.localmovies.app.google.cast.config.ExpandedControlActivity
import com.github.rahmnathan.localmovies.app.persistence.MediaHistory
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import rahmnathan.localmovies.R
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private val logger: Logger = Logger.getLogger(MainActivity::class.qualifiedName!!);
    private lateinit var listAdapter: MediaListAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var gridView: GridView

    @Inject lateinit var executorService: ExecutorService
    @Inject lateinit var persistenceService: MediaPersistenceService
    @Inject lateinit var castContext: CastContext
    @Inject @Volatile lateinit var client: Client
    @Inject lateinit var mediaFacade: MediaFacade
    lateinit var mediaRepository: MediaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!File(this.filesDir, SETUP_FILE).exists()) {
            startActivity(Intent(this@MainActivity, SetupActivity::class.java))
        }

        (application as LocalMoviesApplication).appComponent.inject(this)

        // Initialize required vars
        progressBar = findViewById(R.id.progressBar)
        listAdapter = MediaListAdapter(this, ArrayList())
        gridView = findViewById(R.id.gridView)
        gridView.adapter = listAdapter

        // Initialize cast toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        menuInflater.inflate(R.menu.cast, toolbar.menu)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, toolbar.menu, R.id.media_route_menu_item)

        // Initialize Search functionality
        val searchView = MenuItemCompat.getActionView(toolbar.menu.findItem(R.id.action_search)) as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnQueryTextListener(MediaSearchTextWatcher(listAdapter))

        mediaRepository = MediaRepository(
                persistenceService,
                listAdapter,
                mediaFacade,
                progressBar,
                this,
                client,
                executorService
        )

        val mediaHistory = MediaHistory(this)
        assembleMenuDrawer(searchView, toolbar, mediaHistory)
        assembleNavigationButtons(searchView)

        // Trigger initial media loading
        client.appendToCurrentPath(MOVIES)
        CompletableFuture.runAsync(KeycloakAuthenticator(client), executorService)
                .thenRun { mediaRepository.getVideos() }
                .thenRun(MediaEventLoader(listAdapter, client, mediaFacade, persistenceService, this))

        // Initialize media click listeners
        gridView.onItemClickListener = MediaClickListener(
                listAdapter = listAdapter,
                context = this,
                castContext = castContext,
                mediaRepository = mediaRepository,
                history = mediaHistory,
                client = client
        )

        gridView.setOnItemLongClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val intent = Intent(this, DescriptionPopUpActivity::class.java)
            intent.putExtra(DescriptionPopUpActivity.MOVIE, listAdapter.getMovie(position))
            startActivity(intent)
            true
        }
    }

    private fun assembleNavigationButtons(searchView: SearchView) {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_movies
        bottomNavigationView.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED
        val popup = PopupMenu(this, bottomNavigationView, Gravity.END)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            MainActivityUtils.sortVideoList(item, listAdapter, gridView)
            true
        }
        popup.menuInflater.inflate(R.menu.settings, popup.menu)
        bottomNavigationView.menu.setGroupCheckable(0, false, true)
        bottomNavigationView.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_controls -> {
                    val session = castContext.sessionManager!!.currentCastSession
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
    }

    private fun assembleMenuDrawer(searchView: SearchView, toolbar: Toolbar, mediaHistory: MediaHistory) {
        // Initialize menu
        val homeItem = PrimaryDrawerItem()
                .withIdentifier(1)
                .withName("Home")
                .withTextColor(Color.WHITE)
                .withOnDrawerItemClickListener { _: View?, _: Int, _: IDrawerItem<*, *>? ->
                    getRootVideos(MOVIES, searchView)
                    false
                }
        val historyItem = PrimaryDrawerItem()
                .withIdentifier(2)
                .withName("History")
                .withTextColor(Color.WHITE)
                .withOnDrawerItemClickListener { _: View?, _: Int, _: IDrawerItem<*, *>? ->
                    client.resetCurrentPath()
                    client.appendToCurrentPath(MOVIES)
                    listAdapter.display(mediaHistory.historyList)
                    false
                }
        val settingsItem = PrimaryDrawerItem()
                .withIdentifier(3)
                .withName("My Account")
                .withTextColor(Color.WHITE)
                .withOnDrawerItemClickListener { _: View?, _: Int, _: IDrawerItem<*, *>? ->
                    this@MainActivity.startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                    false
                }
        val result = DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(homeItem, historyItem, settingsItem)
                .withSliderBackgroundColor(Color.BLACK)
                .build()
    }

    private fun getRootVideos(path: String, searchText: SearchView) {
        client.resetCurrentPath()
        searchText.setQuery("", false)
        client.appendToCurrentPath(path)
        mediaRepository.getVideos()
    }

    override fun onBackPressed() {
        val currentDirectory = client.currentPath.peekLast()
        if (currentDirectory.equals(SERIES, ignoreCase = true) || currentDirectory.equals(MOVIES, ignoreCase = true)) exitProcess(8)
        client.popOneDirectory()
        mediaRepository.getVideos()
    }

    companion object {
        private const val MOVIES = "Movies"
        private const val SERIES = "Series"
    }
}