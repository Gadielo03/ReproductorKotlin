package com.example.reproductor.player

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.reproductor.models.MusicFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class MusicPlayerManager(application: Application) : AndroidViewModel(application) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    val currentPosition = mutableStateOf(0L)
    val duration = mutableStateOf(0L)
    val currentSongIndex = mutableStateOf(-1)
    private var playlist = mutableListOf<MusicFile>()
    val currentSong = mutableStateOf<MusicFile?>(null)
    val isRepeatEnabled = mutableStateOf(false)
    val isRandomEnabled = mutableStateOf(false)
    private var positionUpdateJob: Job? = null

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        } else {
            mediaPlayer?.reset()
        }
        setupMediaPlayerListeners()
    }

    private fun setupMediaPlayerListeners() {
        mediaPlayer?.setOnCompletionListener {
            if (isRepeatEnabled.value) {
                seekTo(0)
                play()
            } else {
                playNext()
            }
        }
        mediaPlayer?.setOnPreparedListener { mp ->
            duration.value = mp.duration.toLong()
        }
        mediaPlayer?.setOnErrorListener { _, what, extra ->
            println("Error en MediaPlayer: $what, $extra")
            false
        }
    }

    fun setPlaylist(songs: List<MusicFile>) {
        playlist.clear()
        playlist.addAll(songs)
        if (songs.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firstSong = songs[0]
                    val uri = Uri.fromFile(File(firstSong.path))
                    // Usa el contexto de aplicación correctamente
                    getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadSong(songIndex: Int) {
        if (songIndex < 0 || songIndex >= playlist.size) return
        val wasPlaying = mediaPlayer?.isPlaying ?: false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val song = playlist[songIndex]
                val uri = Uri.fromFile(File(song.path))
                initMediaPlayer()
                // Usa el contexto de aplicación correctamente
                mediaPlayer?.setDataSource(getApplication<Application>(), uri)
                withContext(Dispatchers.Main) {
                    currentSongIndex.value = songIndex
                    currentSong.value = song
                    _isPlaying.value = false
                    currentPosition.value = 0
                    duration.value = 0
                    mediaPlayer?.setOnPreparedListener { mp ->
                        duration.value = mp.duration.toLong()
                        if (wasPlaying) play()
                    }
                    mediaPlayer?.prepareAsync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePlayPause() {
        if (currentSongIndex.value < 0 && playlist.isNotEmpty()) {
            loadSong(0)
            play()
            return
        }
        if (mediaPlayer?.isPlaying == true) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        try {
            if (mediaPlayer?.isPlaying == false && currentSong.value != null) {
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressUpdate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopProgressUpdate()
        }
    }

    fun playNext() {
        if (playlist.isEmpty()) return
        val nextIndex = if (isRandomEnabled.value) {
            (0 until playlist.size).random()
        } else {
            (currentSongIndex.value + 1) % playlist.size
        }
        loadSong(nextIndex)
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return
        if (currentPosition.value > 3000) {
            seekTo(0)
            return
        }
        val prevIndex = if (isRandomEnabled.value) {
            (0 until playlist.size).random()
        } else {
            if (currentSongIndex.value <= 0) playlist.size - 1 else currentSongIndex.value - 1
        }
        loadSong(prevIndex)
    }

    fun seekTo(position: Int) {
        mediaPlayer?.let {
            if (it.isPlaying || it.currentPosition > 0) {
                it.seekTo(position)
                currentPosition.value = position.toLong()
            }
        }
    }

    fun toggleRepeat() {
        isRepeatEnabled.value = !isRepeatEnabled.value
    }

    fun toggleRandom() {
        isRandomEnabled.value = !isRandomEnabled.value
    }

    private fun startProgressUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                currentPosition.value = mediaPlayer?.currentPosition?.toLong() ?: 0
                delay(250)
            }
        }
    }

    private fun stopProgressUpdate() {
        positionUpdateJob?.cancel()
    }

    fun release() {
        stopProgressUpdate()
        mediaPlayer?.release()
        mediaPlayer = null
        currentSong.value = null
    }

    fun getProgress(): Float {
        if (duration.value <= 0) return 0f
        return currentPosition.value.toFloat() / duration.value
    }

    fun setProgress(progress: Float) {
        val newPosition = (progress * duration.value).toInt()
        seekTo(newPosition)
    }
}