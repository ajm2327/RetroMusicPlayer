/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package code.name.monkey.retromusic.model

import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.SortingUtil
import java.text.Collator

data class Artist(
    val id: Long,
    val albums: List<Album>,
    val isAlbumArtist: Boolean = false
) {
    constructor(
        artistName: String,
        albums: List<Album>,
        isAlbumArtist: Boolean = false
    ) : this(albums[0].artistId, albums, isAlbumArtist) {
        name = artistName
    }

    var name: String = "-"
        get() {
            val name = if (isAlbumArtist) getAlbumArtistName()
            else getArtistName()
            return when {
                MusicUtil.isVariousArtists(name) ->
                    VARIOUS_ARTISTS_DISPLAY_NAME

                MusicUtil.isArtistNameUnknown(name) ->
                    UNKNOWN_ARTIST_DISPLAY_NAME

                else -> name!!
            }
        }

    val songCount: Int
        get() {
            var songCount = 0
            for (album in albums) {
                songCount += album.songCount
            }
            return songCount
        }

    val albumCount: Int
        get() = albums.size

    val songs: List<Song>
        get() = albums.flatMap { it.songs }

    // Update the sortedSongs getter
    val sortedSongs: List<Song>
        get() {
            return songs.sortedWith(
                when (PreferenceUtil.artistDetailSongSortOrder) {
                    SortOrder.ArtistSongSortOrder.SONG_A_Z -> { o1, o2 ->
                        val title1 = SortingUtil.getSortableString(o1.title, o1.titleSort)
                        val title2 = SortingUtil.getSortableString(o2.title, o2.titleSort)
                        SortingUtil.compare(title1, title2)
                    }

                    SortOrder.ArtistSongSortOrder.SONG_Z_A -> { o1, o2 ->
                        val title1 = SortingUtil.getSortableString(o1.title, o1.titleSort)
                        val title2 = SortingUtil.getSortableString(o2.title, o2.titleSort)
                        SortingUtil.compare(title2, title1)
                    }

                    SortOrder.ArtistSongSortOrder.SONG_ALBUM -> { o1, o2 ->
                        val album1 = SortingUtil.getSortableString(o1.albumName, o1.albumSort)
                        val album2 = SortingUtil.getSortableString(o2.albumName, o2.albumSort)
                        SortingUtil.compare(album1, album2)
                    }

                    SortOrder.ArtistSongSortOrder.SONG_YEAR -> { o1, o2 ->
                        o2.year.compareTo(o1.year)
                    }

                    SortOrder.ArtistSongSortOrder.SONG_DURATION -> { o1, o2 ->
                        o1.duration.compareTo(o2.duration)
                    }

                    else -> {
                        throw IllegalArgumentException("invalid ${PreferenceUtil.artistDetailSongSortOrder}")
                    }
                })
        }

    // Update the sortedAlbums getter
    val sortedAlbums: List<Album>
        get() {
            return albums.sortedWith(
                when (PreferenceUtil.artistAlbumSortOrder) {
                    SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z -> { o1, o2 ->
                        val album1Sort = o1.songs.firstOrNull()?.albumSort
                        val album2Sort = o2.songs.firstOrNull()?.albumSort
                        val name1 = SortingUtil.getSortableString(o1.title, album1Sort)
                        val name2 = SortingUtil.getSortableString(o2.title, album2Sort)
                        SortingUtil.compare(name1, name2)
                    }

                    SortOrder.ArtistAlbumSortOrder.ALBUM_Z_A -> { o1, o2 ->
                        val album1Sort = o1.songs.firstOrNull()?.albumSort
                        val album2Sort = o2.songs.firstOrNull()?.albumSort
                        val name1 = SortingUtil.getSortableString(o1.title, album1Sort)
                        val name2 = SortingUtil.getSortableString(o2.title, album2Sort)
                        SortingUtil.compare(name2, name1)
                    }

                    SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR_ASC -> { o1, o2 ->
                        o1.year.compareTo(o2.year)
                    }

                    SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR -> { o1, o2 ->
                        o2.year.compareTo(o1.year)
                    }

                    else -> {
                        throw IllegalArgumentException("invalid ${PreferenceUtil.artistAlbumSortOrder}")
                    }
                })
        }

    fun safeGetFirstAlbum(): Album {
        return albums.firstOrNull() ?: Album.empty
    }

    private fun getArtistName(): String {
        return safeGetFirstAlbum().safeGetFirstSong().artistName
    }

    private fun getAlbumArtistName(): String? {
        return safeGetFirstAlbum().safeGetFirstSong().albumArtist
    }

    companion object {
        const val UNKNOWN_ARTIST_DISPLAY_NAME = "Unknown Artist"
        const val VARIOUS_ARTISTS_DISPLAY_NAME = "Various Artists"
        const val VARIOUS_ARTISTS_ID: Long = -2
        val empty = Artist(-1, emptyList())

    }
}
