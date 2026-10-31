package com.metrolist.music.playback

import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.models.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidAutoLyricsTest {
    @Test
    fun `changing the lyric subtitle preserves the queue and playback position`() {
        val player = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()
        try {
            val song = MediaMetadata(
                id = "song", title = "Title", duration = 120,
                artists = listOf(MediaMetadata.Artist(id = "artist", name = "Artist")),
            ).toMediaItem()
            val nextSong = song.buildUpon().setMediaId("next-song").build()
            player.setMediaItems(listOf(song, nextSong))
            player.seekTo(0, 6_000)
            player.playWhenReady = true
            val updated = song.buildUpon().setMediaMetadata(
                song.mediaMetadata.buildUpon().setSubtitle("Current lyric").build(),
            ).build()

            player.replaceMediaItem(0, updated)

            assertEquals(6_000L, player.currentPosition)
            assertEquals(0, player.currentMediaItemIndex)
            assertEquals(2, player.mediaItemCount)
            assertEquals("next-song", player.getMediaItemAt(1).mediaId)
            assertEquals("Current lyric", player.currentMediaItem?.mediaMetadata?.subtitle)
            assertEquals(song.localConfiguration, player.currentMediaItem?.localConfiguration)
            assertEquals(true, player.playWhenReady)
        } finally {
            player.release()
        }
    }

    @Test
    fun `timed lyrics follow backward seeks and expose empty instrumental lines`() {
        val lines = listOf(LyricsEntry(1_000, "First"), LyricsEntry(3_000, ""), LyricsEntry(5_000, "Last"))
        assertEquals(-1, LyricsUtils.findCurrentLineIndex(lines, 0))
        assertEquals(2, LyricsUtils.findCurrentLineIndex(lines, 6_000))
        assertEquals(0, LyricsUtils.findCurrentLineIndex(lines, 1_500))
        assertEquals("", lines[LyricsUtils.findCurrentLineIndex(lines, 3_500)].text)
        assertEquals(-1, LyricsUtils.findCurrentLineIndex(emptyList(), 6_000))
    }
}
