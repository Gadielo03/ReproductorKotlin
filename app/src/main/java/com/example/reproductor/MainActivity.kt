package com.example.reproductor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reproductor.repositories.MusicFilesRepository
import com.example.reproductor.ui.theme.MusicPlayerTheme
import com.example.reproductor.utils.PermissionManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.reproductor.models.MusicFile
import com.example.reproductor.player.MusicPlayerManager
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.ColorFilter


class MainActivity : AppCompatActivity(), PermissionManager.PermissionCallback {
    private lateinit var permissionManager: PermissionManager
    private val musicRepository = MusicFilesRepository()
    private lateinit var musicPlayerManager: MusicPlayerManager
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val maxVolume get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private val currentVolume get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    private var musicFiles by mutableStateOf<List<MusicFile>>(emptyList())

    private val REQUEST_CODE_PICK_FOLDER = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(this)
        permissionManager.setPermissionCallback(this)
        musicPlayerManager = MusicPlayerManager(application)
        loadFiles()


        if (!permissionManager.checkStoragePermission()) {
            finish()
        }
        setContent {
            MusicPlayerTheme {
                Surface {
                    MusicPlayerObserver()
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Menú desplegable
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 8.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Título a la izquierda
                            Text(
                                text = "Reproductor Kotlin",
                                style = TextStyle(
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )

                            // Menú desplegable alineado a la derecha
                            MinimalDropdownMenu()
                        }

                        // Lista scrolleable (ocupa el espacio disponible)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                )
                        ) {
                            if (musicFiles.isNotEmpty()) {
                                MusicFilesList(
                                    musicFiles = musicFiles,
                                    onMusicFileClick = { index ->
                                        musicPlayerManager.loadSong(index)
                                        musicPlayerManager.play()
                                    }
                                )
                            }
                        }

                        // ControllersCard con DataCard integrado
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val isPlaying by musicPlayerManager.isPlaying.collectAsState()
                            val currentSong by musicPlayerManager.currentSong
                            val currentPosition by musicPlayerManager.currentPosition
                            val duration by musicPlayerManager.duration
                            val isRandom by musicPlayerManager.isRandomEnabled
                            val isRepeat by musicPlayerManager.isRepeatEnabled
                            val (volume, setVolume) = rememberVolumeState()

                            ControllersCard(
                                currentTime = formatDuration(currentPosition),
                                totalTime = formatDuration(duration),
                                isPlaying = isPlaying,
                                isRandom = isRandom,
                                isRepeat = isRepeat,
                                bitmap = currentSong?.coverBitmap,
                                title = currentSong?.name ?: "Selecciona una canción",
                                artist = currentSong?.artist ?: "Artista desconocido",
                                onPlayPause = { musicPlayerManager.togglePlayPause() },
                                onNext = { musicPlayerManager.playNext() },
                                onPrevious = { musicPlayerManager.playPrevious() },
                                onRandom = { musicPlayerManager.toggleRandom() },
                                onRepeat = { musicPlayerManager.toggleRepeat() },
                                onSeek = { musicPlayerManager.setProgress(it) },
                                volume = volume,
                                onVolumeChange = setVolume
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadFiles() {
        try {
            musicFiles = musicRepository.loadMusicFiles(getMusicFolderPath())
            musicPlayerManager.setPlaylist(musicFiles)
            Log.d("MainActivity", "Archivos de música cargados: ${musicFiles.size}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al cargar archivos de música: ${e.message}")
            e.printStackTrace()
            musicFiles = emptyList()
        }
    }

    override fun onPermissionGranted() {
        Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
    }

    override fun onPermissionDenied() {
        Toast.makeText(this, "Favor de Conceder Permiso", Toast.LENGTH_SHORT).show()
        finish()
    }

    @Composable
    fun PrimaryColorText(texto: String,fontSize:Int) {
        Text(
            text = texto,
            color = MaterialTheme.colorScheme.primary,
            style = TextStyle (
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }

    @Composable
    fun SecondaryColorText(texto: String,fontSize:Int) {
        Text(
            text = texto,
            color = MaterialTheme.colorScheme.secondary,
            style = TextStyle (
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Normal
            )
        )
    }

    @Composable
    fun MinimalDropdownMenu() {
        var expanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .padding(16.dp)
        ) {
            IconButton (onClick = { expanded = !expanded }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu (
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_folder_24),
                                contentDescription = "Carpeta",
                                modifier = Modifier.padding(end = 8.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            PrimaryColorText("Cambiar Carpeta de música", 16)
                        }
                    },
                    onClick = {openFolderPicker() }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable

    fun ControllersCard(
        // Elimina la evaluación directa en los valores por defecto
        currentTime: String = "0:00",
        totalTime: String = "0:00",
        isPlaying: Boolean = false,
        isRandom: Boolean = false,
        isRepeat: Boolean = false,
        bitmap: android.graphics.Bitmap? = null,
        title: String = "Selecciona una canción",
        artist: String = "Artista desconocido",
        onPlayPause: () -> Unit = {},
        onNext: () -> Unit = {},
        onPrevious: () -> Unit = {},
        onRandom: () -> Unit = {},
        onRepeat: () -> Unit = {},
        onSeek: (Float) -> Unit = {},
        volume: Float,
        onVolumeChange: (Float) -> Unit,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp) // Aumentar un poco la altura para acomodar ambos elementos
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Parte superior: MiniDataCard
                MiniDataCard(
                    bitmap = bitmap,
                    title = title,
                    artist = artist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f) // 40% del espacio para la miniatura
                )

                // Parte media: Slider con tiempos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.2f)
                        .padding(
                            start = 0.dp,
                            end = 0.dp,
                            top = 10.dp,
                            bottom = 0.dp
                        ), // 20% para el slider
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tiempo actual
                    Text(
                        text = currentTime,
                        style = TextStyle(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )

                    // Slider
                    var sliderPosition by remember { mutableStateOf(0f) }
                    Slider(
                        value = musicPlayerManager.getProgress(),  // Esto puede ser un problema
                        onValueChange = {
                            onSeek(it)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                        )
                    )

                    // Tiempo total
                    Text(
                        text = totalTime,
                        style = TextStyle(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Parte inferior: Botones de control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                        .padding(
                            start = 0.dp,
                            end = 0.dp,
                            top = 10.dp,
                            bottom = 0.dp
                        ), // 40% para los botones
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Random
                    IconButton(onClick = onRandom) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_auto_awesome_24),
                            contentDescription = "Reproducción aleatoria",
                            tint = if (isRandom) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Botón Previous
                    IconButton(onClick = onPrevious) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_skip_previous_24),
                            contentDescription = "Anterior",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Botón Play/Pause
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary,
                                CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
                            ),
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }

                    // Botón Next
                    IconButton(onClick = onNext) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_skip_next_24),
                            contentDescription = "Siguiente",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Botón Repeat
                    IconButton(onClick = onRepeat) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_repeat_24),
                            contentDescription = "Repetir",
                            tint = if (isRepeat) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_volume_up_24),
                        contentDescription = "Volumen",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                        ),
                        thumb = {
                            androidx.compose.material3.SliderDefaults.Thumb(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                thumbSize = androidx.compose.ui.unit.DpSize(12.dp, 12.dp)
                            )
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun MiniDataCard(
        bitmap: android.graphics.Bitmap? = null,
        title: String,
        artist: String,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen miniatura
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Portada de música",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.baseline_library_music_24),
                        contentDescription = "Música",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)

                    )
                }
            }

            // Información de la canción
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                // Título
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Artista
                Text(
                    text = artist,
                    style = TextStyle(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    fun MusicFilesList(
        musicFiles: List<MusicFile>,
        onMusicFileClick: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            items(
                items = musicFiles,
                key = { it.path }
            ) { musicFile ->
                val index = musicFiles.indexOf(musicFile)
                MusicFileItem(
                    musicFile = musicFile,
                    onClick = { onMusicFileClick(index) }
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    @Composable
    fun MusicFileItem(musicFile: MusicFile,onClick: () -> Unit = {}) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen de portada
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (musicFile.coverBitmap != null) {
                    Image(
                        bitmap = musicFile.coverBitmap.asImageBitmap(),
                        contentDescription = "Portada de ${musicFile.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Imagen por defecto si no hay portada
                    Image(
                        painter = painterResource(id = R.drawable.baseline_library_music_24),
                        contentDescription = "Sin portada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)

                    )
                }
            }

            // Información de la canción (nombre y artista)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = musicFile.name,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = musicFile.artist ?: "Artista desconocido",
                    style = TextStyle(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duración de la canción
            Text(
                text = formatDuration(musicFile.duration),
                style = TextStyle(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    // Función para formatear la duración en milisegundos a formato MM:SS
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    @Composable
    fun MusicPlayerObserver() {
        val _currentPosition by musicPlayerManager.currentPosition
        val _isPlaying by musicPlayerManager.isPlaying.collectAsState()
        val _currentSong by musicPlayerManager.currentSong
    }

    @Composable
    fun rememberVolumeState(): Pair<Float, (Float) -> Unit> {
        val volume = remember { mutableStateOf(currentVolume.toFloat() / maxVolume) }
        val setVolume: (Float) -> Unit = { v ->
            val newVol = (v * maxVolume).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
            volume.value = newVol.toFloat() / maxVolume
        }
        return Pair(volume.value, setVolume)
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayerManager.release()
    }

    private fun getMusicFolderPath(): String {
        val sharedPrefs = getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("musicFolderPath", "/storage/emulated/0/Music/Samsung/") ?: "/storage/emulated/0/Music/Samsung/"
    }

    fun saveMusicFolderPath(path: String) {
        val sharedPrefs = getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("musicFolderPath", path).apply()
    }

    fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER)
    }

    // Maneja el resultado del selector
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val path = getFullPathFromTreeUri(uri) ?: return
            saveMusicFolderPath(path)
            loadFiles()
        }
    }

    fun getFullPathFromTreeUri(uri: android.net.Uri): String? {
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        val parts = docId.split(":")
        if (parts.size == 2 && parts[0] == "primary") {
            return "/storage/emulated/0/" + parts[1]
        }
        return null // Para SD o almacenamiento externo, se requiere lógica adicional
    }
}



