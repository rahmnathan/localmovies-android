<h1>LocalMovies</h1>

[![Build Status](https://jenkins.nathanrahm.com/buildStatus/icon?job=localmovies-android)](https://jenkins.nathanrahm.com/job/localmovies-android/)

Android client for LocalMovies backend system.

Android app displays media metadata based on sub-directory query and plays media locally or on a Google Cast device.
The app is 'event-based' in that it only loads all media metadata on first use and then stores the data in
a local database. Each time the app is launched, a query is made for events (create/update/delete), 
processes them, and adds them to the database. This strategy ensures a better user experience 
as well as reducing the load on the backend services.

## Generated API Client Artifact

This app can consume the backend-generated OpenAPI Java client:

- Group: `com.github.rahmnathan.localmovie`
- Artifact: `localmovie-api-client`
- Repo: `https://artifactory.nathanrahm.com/artifactory/rahmnathan-libraries`

Set `localmovieApiClientVersion` in `gradle.properties` (or pass `-PlocalmovieApiClientVersion=<version>`) to enable it.
