package com.github.rahmnathan.localmovies.app.dagger

import com.github.rahmnathan.localmovies.app.activity.MainActivity
import com.github.rahmnathan.localmovies.app.activity.SetupActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(target: MainActivity)
    fun inject(target: SetupActivity)
}