package com.github.rahmnathan.localmovies.app.activity.main

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import com.github.rahmnathan.localmovies.app.LocalMoviesApplication
import com.github.rahmnathan.localmovies.app.activity.setup.SetupActivity
import com.github.rahmnathan.localmovies.app.activity.setup.SetupActivity.Companion.SETUP_FILE
import com.github.rahmnathan.localmovies.app.media.provider.control.MediaFacade
import com.github.rahmnathan.localmovies.app.Client
import com.github.rahmnathan.localmovies.app.activity.main.view.*
import com.github.rahmnathan.localmovies.app.cast.control.GoogleCastUtils
import com.github.rahmnathan.localmovies.app.media.provider.boundary.MediaEventLoader
import com.github.rahmnathan.localmovies.app.media.provider.boundary.MediaRepository
import com.github.rahmnathan.localmovies.app.persistence.MediaHistory
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import rahmnathan.localmovies.R
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private val logger: Logger = Logger.getLogger(MainActivity::class.qualifiedName!!);
    private lateinit var listAdapter: MediaListAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var gridView: GridView

    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    @Inject lateinit var persistenceService: MediaPersistenceService
    @Inject lateinit var castContext: CastContext
    @Inject @Volatile lateinit var client: Client
    @Inject lateinit var mediaFacade: MediaFacade
    @Inject lateinit var castUtils: GoogleCastUtils
    lateinit var mediaRepository: MediaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!File(this.filesDir, SETUP_FILE).exists()) {
            startActivity(Intent(this@MainActivity, SetupActivity::class.java))
            return
        }

        (application as LocalMoviesApplication).appComponent.inject(this)

        setContentView(R.layout.activity_main)


        // Initialize required vars
        progressBar = findViewById(R.id.progressBar)
        listAdapter = MediaListAdapter(this@MainActivity, ArrayList())
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
                client,
                executorService
        )

        val mediaHistory = MediaHistory(this)
        MenuDrawer.build(searchView, client, this, mediaHistory, listAdapter, toolbar)
        NavigationButtons.build(searchView, this, listAdapter, gridView, castContext)

        // Trigger initial media loading
        client.appendToCurrentPath(MOVIES)
        CompletableFuture.runAsync(Runnable {mediaRepository.getVideos()}, executorService)
                .thenRun(MediaEventLoader(listAdapter, client, mediaFacade, persistenceService, this))

        // Initialize media click listeners
        gridView.onItemClickListener = MediaClickListener(
                listAdapter = listAdapter,
                context = this@MainActivity,
                castContext = castContext,
                mediaRepository = mediaRepository,
                history = mediaHistory,
                client = client,
                castUtils = castUtils,
                executorService = executorService
        )

        gridView.setOnItemLongClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val intent = Intent(this, DescriptionPopUpActivity::class.java)
            intent.putExtra(DescriptionPopUpActivity.MOVIE, listAdapter.getMovie(position))
            startActivity(intent)
            true
        }
    }

    fun getRootVideos(path: String, searchText: SearchView) {
        CompletableFuture.runAsync(Runnable {client.resetCurrentPath()}, executorService)
                .thenRun{searchText.setQuery("", false)}
                .thenRun{client.appendToCurrentPath(path)}
                .thenRun{mediaRepository.getVideos()}
    }

    override fun onBackPressed() {
        val currentDirectory = client.currentPath.peekLast()
        if (currentDirectory.equals(SERIES, ignoreCase = true) || currentDirectory.equals(MOVIES, ignoreCase = true)) exitProcess(8)
        client.popOneDirectory()
        CompletableFuture.runAsync(Runnable {mediaRepository.getVideos()}, executorService)
    }

    companion object {
        const val MOVIES = "Movies"
        const val SERIES = "Series"
    }
}