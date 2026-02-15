package com.github.rahmnathan.localmovies.app.ui.main

import com.github.rahmnathan.localmovies.app.cast.GoogleCastUtils
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import com.github.rahmnathan.localmovies.app.data.repository.Result
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.data.SignedUrls
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackCoordinatorTest {

    private lateinit var mediaRepository: MediaRepository
    private lateinit var googleCastUtils: GoogleCastUtils
    private lateinit var playbackCoordinator: PlaybackCoordinator

    private val testMedia = createTestMedia("test-id", "Test Movie")
    private val testSignedUrls = SignedUrls(
        stream = "https://example.com/stream",
        poster = "https://example.com/poster",
        updatePosition = "https://example.com/position"
    )

    @Before
    fun setup() {
        mediaRepository = mockk()
        googleCastUtils = mockk()
        playbackCoordinator = PlaybackCoordinator(mediaRepository, googleCastUtils)
    }

    @Test
    fun `play returns PlayLocally when Cast is not active`() = runTest {
        // Given
        every { googleCastUtils.isCastSessionActive() } returns false
        coEvery { mediaRepository.getSignedUrls(any()) } returns Result.Success(testSignedUrls)

        // When
        val result = playbackCoordinator.play(testMedia, resumePosition = 5000)

        // Then
        assertTrue(result is PlaybackResult.PlayLocally)
        val playLocally = result as PlaybackResult.PlayLocally
        assertEquals("https://example.com/stream", playLocally.streamUrl)
        assertEquals("https://example.com/position", playLocally.updatePositionUrl)
        assertEquals("test-id", playLocally.mediaId)
        assertEquals(5000L, playLocally.resumePosition)
    }

    @Test
    fun `play returns PlayingOnCast when Cast session is active and succeeds`() = runTest {
        // Given
        every { googleCastUtils.isCastSessionActive() } returns true
        coEvery { googleCastUtils.playOnCast(any(), any(), any()) } returns true

        // When
        val result = playbackCoordinator.play(testMedia, resumePosition = 0)

        // Then
        assertTrue(result is PlaybackResult.PlayingOnCast)
    }

    @Test
    fun `play falls back to local when Cast fails`() = runTest {
        // Given
        every { googleCastUtils.isCastSessionActive() } returns true
        coEvery { googleCastUtils.playOnCast(any(), any(), any()) } returns false
        coEvery { mediaRepository.getSignedUrls(any()) } returns Result.Success(testSignedUrls)

        // When
        val result = playbackCoordinator.play(testMedia)

        // Then
        assertTrue(result is PlaybackResult.PlayLocally)
    }

    @Test
    fun `play returns Error when stream URL is null`() = runTest {
        // Given
        every { googleCastUtils.isCastSessionActive() } returns false
        coEvery { mediaRepository.getSignedUrls(any()) } returns Result.Success(
            SignedUrls(stream = null, poster = null, updatePosition = "https://example.com/position")
        )

        // When
        val result = playbackCoordinator.play(testMedia)

        // Then
        assertTrue(result is PlaybackResult.Error)
        assertEquals("Invalid stream URL", (result as PlaybackResult.Error).message)
    }

    @Test
    fun `play returns Error when updatePosition URL is null`() = runTest {
        // Given
        every { googleCastUtils.isCastSessionActive() } returns false
        coEvery { mediaRepository.getSignedUrls(any()) } returns Result.Success(
            SignedUrls(stream = "https://example.com/stream", poster = null, updatePosition = null)
        )

        // When
        val result = playbackCoordinator.play(testMedia)

        // Then
        assertTrue(result is PlaybackResult.Error)
        assertEquals("Invalid update position URL", (result as PlaybackResult.Error).message)
    }

    @Test
    fun `play returns Error when repository fails`() = runTest {
        // Given
        every { googleCastUtils.isCastSessionActive() } returns false
        coEvery { mediaRepository.getSignedUrls(any()) } returns Result.Error(
            RuntimeException("Network error"),
            "Network error"
        )

        // When
        val result = playbackCoordinator.play(testMedia)

        // Then
        assertTrue(result is PlaybackResult.Error)
        assertTrue((result as PlaybackResult.Error).message.contains("Network error"))
    }

    @Test
    fun `getRemainingEpisodes returns empty list for movies`() {
        // Given - a movie (no episode number)
        val movie = createTestMedia("movie-id", "Test Movie", number = null)
        val mediaList = listOf(movie)

        // When
        val result = playbackCoordinator.getRemainingEpisodes(movie, mediaList)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRemainingEpisodes returns remaining episodes for series`() {
        // Given - series episodes
        val episode1 = createTestMedia("ep-1", "Episode 1", number = "1")
        val episode2 = createTestMedia("ep-2", "Episode 2", number = "2")
        val episode3 = createTestMedia("ep-3", "Episode 3", number = "3")
        val mediaList = listOf(episode1, episode2, episode3)

        // When
        val result = playbackCoordinator.getRemainingEpisodes(episode1, mediaList)

        // Then
        assertEquals(2, result.size)
        assertEquals("ep-2", result[0].mediaFileId)
        assertEquals("ep-3", result[1].mediaFileId)
    }

    @Test
    fun `getRemainingEpisodes returns empty for last episode`() {
        // Given
        val episode1 = createTestMedia("ep-1", "Episode 1", number = "1")
        val episode2 = createTestMedia("ep-2", "Episode 2", number = "2")
        val mediaList = listOf(episode1, episode2)

        // When
        val result = playbackCoordinator.getRemainingEpisodes(episode2, mediaList)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRemainingEpisodes returns empty when media not in list`() {
        // Given
        val episode1 = createTestMedia("ep-1", "Episode 1", number = "1")
        val episode2 = createTestMedia("ep-2", "Episode 2", number = "2")
        val notInList = createTestMedia("ep-3", "Episode 3", number = "3")
        val mediaList = listOf(episode1, episode2)

        // When
        val result = playbackCoordinator.getRemainingEpisodes(notInList, mediaList)

        // Then
        assertTrue(result.isEmpty())
    }

    private fun createTestMedia(
        id: String,
        title: String,
        number: String? = null
    ): Media {
        return Media(
            title = title,
            imdbRating = null,
            metaRating = null,
            image = null,
            releaseYear = null,
            created = null,
            genre = null,
            filename = "$title.mp4",
            actors = null,
            plot = null,
            path = "/movies/$title",
            number = number,
            type = if (number == null) "MOVIE" else "EPISODE",
            mediaFileId = id,
            streamable = true
        )
    }
}
