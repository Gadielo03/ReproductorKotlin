package com.example.reproductor

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.example.reproductor.repositories.MusicFilesRepository
import com.example.reproductor.utils.PermissionManager


class MainActivity : AppCompatActivity(), PermissionManager.PermissionCallback {
    private lateinit var permissionManager: PermissionManager
    private val musicRepository = MusicFilesRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        permissionManager = PermissionManager(this)
        permissionManager.setPermissionCallback(this)

        if (permissionManager.checkStoragePermission()) {
            loadMusic()
        }
    }

    private fun loadMusic() {
        val musicFiles = musicRepository.loadMusicFiles("/storage/emulated/0/Music/Samsung/")
        Log.d("MainActivity", "Se encontraron ${musicFiles.size} archivos de música")
    }


    override fun onPermissionGranted() {
        Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
    }

    override fun onPermissionDenied() {
        Toast.makeText(this, "Favor de Conceder Permiso", Toast.LENGTH_SHORT).show()
        finish()
    }
}

