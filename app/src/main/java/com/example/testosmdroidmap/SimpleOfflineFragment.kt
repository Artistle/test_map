import com.example.testosmdroidmap.R
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver

Солнце, [24.10.2023 20:33]
The error you're encountering is due to the fact that the TileSource in OSMDroid is trying to treat a local file path as a URL, which is causing a MalformedURLException. To fix this, you should create a custom TileSource and TileProvider that can work with local MBTiles files.

Here's the modified code that should resolve this issue:

kotlin
package com.example.testosmdroidmap

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

class MapActivity : AppCompatActivity() {

    private val requestCodeStoragePermission = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestStoragePermission()
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                requestCodeStoragePermission
            )
        } else {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            selectFileResult.launch(intent)
        }
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
        selectFileResult.launch(intent)
    }

    private val selectFileResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val data = result.data
                val selectedFileUri = data?.data

                if (selectedFileUri != null) {
                    val selectedFilePath = File(g(selectedFileUri)!!)

                    val cachedMbtilesFilePath = copyMbtilesFileToCache(selectedFilePath)

                    if (cachedMbtilesFilePath != null) {
                        val mapView = MapView(this)

                        val tileSource = CustomFileBasedTileSource(cachedMbtilesFilePath)

                        val tileProvider = CustomMapTileFilesystemProvider(
                            SimpleRegisterReceiver(this),
                            tileSource
                        )
                        val tilesOverlay = TilesOverlay(tileProvider, this)

                        mapView.setTileSource(tileSource)
                        mapView.overlays.add(tilesOverlay)

                        val mapController = mapView.controller
                        mapController.setZoom(10.0)
                        mapController.setCenter(tilesOverlay.bounds.center)

                        setContentView(mapView)
                    }
                }
            }
        }

    private fun copyMbtilesFileToCache(selectedFilePath: File): String? {
        return try {
            val cacheDir = cacheDir
            val cachedMbtilesFile = File(cacheDir, "MBTiles")

            selectedFilePath.copyTo(cachedMbtilesFile, true)

            cachedMbtilesFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun g(uri: Uri): String? {
        var filePath: String? = null
        Log.d("", "URI = $uri")
        if ("content" == uri.scheme) {
            val cursor = this.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.ImageColumns.DATA),
                null,
                null,
                null
            )
            cursor!!.moveToFirst()
            filePath = cursor!!.getString(0)
            cursor!!.close()
        } else {
            filePath = uri!!.path
        }
        Log.d("", "Chosen path = $filePath")
        return filePath
    }

    class CustomFileBasedTileSource(mbtilesFilePath: String) :
        FileBasedTileSource("custom", 1, 18, 256, ".png", arrayOf(mbtilesFilePath))

    class CustomMapTileFilesystemProvider(
        pRegisterReceiver: IRegisterReceiver?,
        pTileSource: CustomFileBasedTileSource
    ) : MapTileFilesystemProvider(
        pRegisterReceiver,
        pTileSource,
        OpenStreetMapTileProviderConstants.ONE_YEAR * 10
    )
}