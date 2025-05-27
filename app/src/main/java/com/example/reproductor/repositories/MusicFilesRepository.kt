package com.example.reproductor.repositories

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.reproductor.models.MusicFile
import java.io.File

class MusicFilesRepository {

    /**
     * Carga los archivos de música desde una ruta específica
     * @param pathname La ruta de la carpeta para buscar archivos de música
     * @return Una lista de objetos MusicFile
     */
    fun loadMusicFiles(pathname: String): List<MusicFile> {
        val musicList = ArrayList<MusicFile>()
        val musicFolder = File(pathname)

        if (musicFolder.exists() && musicFolder.isDirectory) {
            val files = musicFolder.listFiles()

            files?.forEach { file ->
                if (file.isFile && isMusicFile(file)) {
                    processMusicFile(file, musicList)
                }
            }
        } else {
            Log.e(TAG, "La carpeta no existe: ${musicFolder.absolutePath}")
        }

        return musicList
    }

    /**
     * Procesa un archivo de música y lo agrega a la lista
     */
    private fun processMusicFile(file: File, musicList: ArrayList<MusicFile>) {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        try {
            mediaMetadataRetriever.setDataSource(file.absolutePath)

            val name = file.name
            val path = file.absolutePath
            val duration = mediaMetadataRetriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0
            val artist = mediaMetadataRetriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_ARTIST
            )
            val album = mediaMetadataRetriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_ALBUM
            )

            // Extraer la portada y convertirla directamente a bitmap
            val coverArt = mediaMetadataRetriever.embeddedPicture
            val coverBitmap = if (coverArt != null) {
                try {
                    BitmapFactory.decodeByteArray(coverArt, 0, coverArt.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al convertir portada a bitmap: ${e.message}")
                    null
                }
            } else null

            musicList.add(
                MusicFile(
                    name,
                    path,
                    duration,
                    artist,
                    album,
                    coverArt,
                    coverBitmap
                )
            )
            Log.d(TAG, "Archivo cargado: $name, tiene portada: ${coverBitmap != null}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar archivo ${file.name}: ${e.message}")
            e.printStackTrace()
        } finally {
            mediaMetadataRetriever.release()
        }
    }

    /**
     * Verifica si un archivo es un archivo de música basado en su extensión
     */
    private fun isMusicFile(file: File): Boolean {
        val name = file.name.lowercase()
        return (name.endsWith(".mp3") || name.endsWith(".wav") ||
                name.endsWith(".ogg") || name.endsWith(".m4a") ||
                name.endsWith(".flac"))
    }

    companion object {
        private const val TAG = "MusicFilesRepository"
    }
}