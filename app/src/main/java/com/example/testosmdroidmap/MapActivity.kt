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
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck
import org.osmdroid.tileprovider.modules.SqliteArchiveTileWriter
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
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

                    val filePath = g(selectedFileUri)!!


                    val g = File(filePath)

                    if (g.exists()) {
                        Toast.makeText(this, "file exists", Toast.LENGTH_LONG).show()
                        //copyMbtilesFileToCache(g)
                    } else {

                        Toast.makeText(this, "file not exists", Toast.LENGTH_LONG).show()
                    }


                    val cachedMbtilesFile = copyMbtilesFileToCache(g)
                    val mapView = MapView(this)

                    val tileSource = FileBasedTileSource(
                        "MBTiles",
                        1,
                        18,
                        256,
                        ".png",
                        arrayOf(cachedMbtilesFile!!.absolutePath)
                    )

                    val tileWriter = SqliteArchiveTileWriter(cachedMbtilesFile!!.absolutePath)

                    val tileProvider = CustomTileProvider(
                        SimpleRegisterReceiver(this),
                        NetworkAvailabliltyCheck(this),
                        tileSource,
                        tileWriter,
                        this)

                    val tilesOverlay = TilesOverlay(tileProvider, this)

                    mapView.setTileSource(tileSource)
                    //mapView.tileProvider = tileProvider
                    mapView.overlays.add(tilesOverlay)


                    val mapController = mapView.controller
                    mapController.setZoom(10.0)
                    mapController.setCenter(tilesOverlay.bounds.center)

                    setContentView(mapView)
                }
            }
        }

    private fun copyMbtilesFileToCache(selectedFilePath: File): File? {
        return try {
            val cacheDir = cacheDir
            val cachedMbtilesFile = File(cacheDir, "MBTiles")

            selectedFilePath.copyTo(cachedMbtilesFile, true)

            cachedMbtilesFile
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


    class CustomTileProvider(
        pRegisterReceiver: IRegisterReceiver?, aNetworkAvailablityCheck: INetworkAvailablityCheck?,
        pTileSource: ITileSource?, cacheWriter: IFilesystemCache, context: Context
    ) :
        MapTileProviderBasic(
            pRegisterReceiver,
            aNetworkAvailablityCheck, pTileSource,
            context, cacheWriter
        ) {

        init {
            mTileProviderList[0] = CustomMapTileFilesystemProvider(pRegisterReceiver, pTileSource)
        }

        override fun setTileSource(aTileSource: ITileSource) {
            super.setTileSource(aTileSource)
        }
    }

    class CustomMapTileFilesystemProvider(
        pRegisterReceiver: IRegisterReceiver?,
        pTileSource: ITileSource?
    ) :
        MapTileFilesystemProvider(
            pRegisterReceiver,
            pTileSource,
            OpenStreetMapTileProviderConstants.ONE_YEAR * 10
        )

}