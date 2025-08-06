//ALBUM REPO BACKUP:
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

package code.name.monkey.retromusic.repository

import android.provider.MediaStore.Audio.AudioColumns
import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.SortingUtil
import java.text.Collator
import code.name.monkey.retromusic.fragments.albums.AlbumsFragment


/**
 * Created by hemanths on 11/08/17.
 */
interface AlbumRepository {
    fun albums(): List<Album>

    fun albums(query: String): List<Album>

    fun album(albumId: Long): Album
}

class RealAlbumRepository(private val songRepository: RealSongRepository) :
    AlbumRepository {

    override fun albums(): List<Album> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                null,
                null,
                getSongLoaderSortOrder()
            )
        )
        return splitIntoAlbums(songs)
    }

    override fun albums(query: String): List<Album> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                AudioColumns.ALBUM + " LIKE ?",
                arrayOf("%$query%"),
                getSongLoaderSortOrder()
            )
        )
        return splitIntoAlbums(songs)
    }

    override fun album(albumId: Long): Album {
        val cursor = songRepository.makeSongCursor(
            AudioColumns.ALBUM_ID + "=?",
            arrayOf(albumId.toString()),
            getSongLoaderSortOrder()
        )
        val songs = songRepository.songs(cursor)
        val album = Album(albumId, songs)
        return sortAlbumSongs(album)
    }

    // We don't need sorted list of songs (with sortAlbumSongs())
    // cuz we are just displaying Albums(Cover Arts) anyway and not songs
    fun splitIntoAlbums(
        songs: List<Song>,
        sorted: Boolean = true
    ): List<Album> {
        val grouped = songs.groupBy { it.albumId }.map { Album(it.key, it.value) }
        if (!sorted) return grouped

        return when (PreferenceUtil.albumSortOrder) {
            SortOrder.AlbumSortOrder.ALBUM_A_Z -> {
                grouped.sortedWith { a1, a2 ->
                    val album1Sort = a1.songs.firstOrNull()?.albumSort
                    val album2Sort = a2.songs.firstOrNull()?.albumSort
                    val name1 = SortingUtil.getSortableString(a1.title, album1Sort)
                    val name2 = SortingUtil.getSortableString(a2.title, album2Sort)
                    SortingUtil.compare(name1, name2)
                }
            }
            SortOrder.AlbumSortOrder.ALBUM_Z_A -> {
                grouped.sortedWith { a1, a2 ->
                    val album1Sort = a1.songs.firstOrNull()?.albumSort
                    val album2Sort = a2.songs.firstOrNull()?.albumSort
                    val name1 = SortingUtil.getSortableString(a1.title, album1Sort)
                    val name2 = SortingUtil.getSortableString(a2.title, album2Sort)
                    SortingUtil.compare(name2, name1)
                }
            }
            AlbumsFragment.ALBUM_SORT_ORDER_ARTIST -> {
                grouped.sortedWith { a1, a2 ->
                    val artist1Sort = a1.songs.firstOrNull()?.artistSort
                    val artist2Sort = a2.songs.firstOrNull()?.artistSort
                    val name1 = SortingUtil.getSortableString(a1.artistName, artist1Sort)
                    val name2 = SortingUtil.getSortableString(a2.artistName, artist2Sort)
                    SortingUtil.compare(name1, name2)
                }
            }
            AlbumsFragment.ALBUM_SORT_ORDER_ARTIST_LAST_NAME -> {
                grouped.sortedWith { a1, a2 ->
                    val artist1 = if (a1.songs.firstOrNull()?.artistSort.isNullOrBlank()) {
                        SortingUtil.getLastName(a1.artistName)
                    } else {
                        a1.songs.firstOrNull()?.artistSort ?: a1.artistName
                    }
                    val artist2 = if (a2.songs.firstOrNull()?.artistSort.isNullOrBlank()) {
                        SortingUtil.getLastName(a2.artistName)
                    } else {
                        a2.songs.firstOrNull()?.artistSort ?: a2.artistName
                    }
                    SortingUtil.compare(artist1, artist2)
                }
            }
            SortOrder.AlbumSortOrder.ALBUM_ARTIST -> {
                grouped.sortedWith { a1, a2 ->
                    val artist1Sort = a1.songs.firstOrNull()?.albumArtistSort ?: a1.songs.firstOrNull()?.artistSort
                    val artist2Sort = a2.songs.firstOrNull()?.albumArtistSort ?: a2.songs.firstOrNull()?.artistSort
                    val name1 = SortingUtil.getSortableString(a1.albumArtist ?: a1.artistName, artist1Sort)
                    val name2 = SortingUtil.getSortableString(a2.albumArtist ?: a2.artistName, artist2Sort)
                    SortingUtil.compare(name1, name2)
                }
            }
            SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS -> {
                grouped.sortedByDescending { it.songCount }
            }
            else -> grouped
        }
    }

    private fun sortAlbumSongs(album: Album): Album {
        val collator = Collator.getInstance()
        val songs = when (PreferenceUtil.albumDetailSongSortOrder) {
            SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST -> album.songs.sortedWith { o1, o2 ->
                o1.trackNumber.compareTo(o2.trackNumber)
            }
            SortOrder.AlbumSongSortOrder.SONG_A_Z -> {
                album.songs.sortedWith { o1, o2 -> collator.compare(o1.title, o2.title) }
            }
            SortOrder.AlbumSongSortOrder.SONG_Z_A -> {
                album.songs.sortedWith { o1, o2 -> collator.compare(o2.title, o1.title) }
            }
            SortOrder.AlbumSongSortOrder.SONG_DURATION -> album.songs.sortedWith { o1, o2 ->
                o1.duration.compareTo(o2.duration)
            }
            else -> throw IllegalArgumentException("invalid ${PreferenceUtil.albumDetailSongSortOrder}")
        }
        return album.copy(songs = songs)
    }

    private fun getSongLoaderSortOrder(): String {
        var albumSortOrder = PreferenceUtil.albumSortOrder
        // Convert custom sort orders to valid MediaStore sort orders
        when (albumSortOrder) {
            AlbumsFragment.ALBUM_SORT_ORDER_ARTIST -> albumSortOrder = "artist"
            AlbumsFragment.ALBUM_SORT_ORDER_ARTIST_LAST_NAME -> albumSortOrder = "artist"
            SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS -> albumSortOrder = SortOrder.AlbumSortOrder.ALBUM_A_Z
        }
        return albumSortOrder + ", " +
                PreferenceUtil.albumSongSortOrder
    }
}
