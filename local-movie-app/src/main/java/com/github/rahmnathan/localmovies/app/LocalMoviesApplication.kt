package com.github.rahmnathan.localmovies.app

import android.app.Application
import com.github.rahmnathan.localmovies.app.dagger.AppComponent
import com.github.rahmnathan.localmovies.app.dagger.AppModule
import com.github.rahmnathan.localmovies.app.dagger.DaggerAppComponent

class LocalMoviesApplication : Application() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = initDagger(this)
    }

    private fun initDagger(app: LocalMoviesApplication): AppComponent =
            DaggerAppComponent.builder()
                    .appModule(AppModule(app))
                    .build()
}