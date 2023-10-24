package com.example.testosmdroidmap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.SqliteArchiveTileWriter
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

class MainActivity : AppCompatActivity() {


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                openMap()
            } else {
                Toast.makeText(this, "no permission", Toast.LENGTH_LONG).show()
            }
        }


    private val selectFileResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            //Toast.makeText(this, "${it?.data?.data}", Toast.LENGTH_LONG).show()
            val path = it.data?.data?.path
            Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

            val fff = File(path)

            val fi = File(Environment.getExternalStorageDirectory(), fff.name)
            val mapView = MapView(this)
            val tileSource = FileBasedTileSource(
                "MBTiles",
                1,
                18,
                256,
                ".png",
                arrayOf(fi.absolutePath)
            )

            val tileWriter = SqliteArchiveTileWriter(fi.absolutePath)
            val tileProvider = MapTileProviderBasic(this, tileSource, tileWriter)
            val tilesOverlay = TilesOverlay(tileProvider, this)
            mapView.overlays.add(tilesOverlay)

            val mapController = mapView.controller
            mapController.setZoom(10.0)
            mapController.setCenter(tilesOverlay.bounds.center)

            setContentView(mapView)
        }

    private fun openMap() {
        startActivity(Intent(this, MapActivity::class.java))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MapActivity::class.java))
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)
        //selectFileResult.launch(intent)
    }
}