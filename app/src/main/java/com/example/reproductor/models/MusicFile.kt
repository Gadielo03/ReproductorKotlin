package com.example.reproductor.models

import android.graphics.Bitmap

data class MusicFile(
    val name: String,
    val path: String,
    val duration: Long,
    val artist: String?,
    val album: String?,
    val coverArt: ByteArray? = null,
    val coverBitmap: Bitmap? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MusicFile

        if (name != other.name) return false
        if (path != other.path) return false
        if (duration != other.duration) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (coverArt != null) {
            if (other.coverArt == null) return false
            if (!coverArt.contentEquals(other.coverArt)) return false
        } else if (other.coverArt != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (coverArt?.contentHashCode() ?: 0)
        return result
    }
}