package com.ipn.qrlink.activities

import android.Manifest
import android.R.attr.bitmap
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toFile
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.ipn.qrlink.R
import com.ipn.qrlink.databinding.ActivityHomeBinding
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    var navController: NavController? = null

    // Referencia al email con el que ingreso el usuario
    // Nos servira a la hora de guardar los QR generados
    var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Obtenemos el email de la pantalla anterior de ingreso
        email = intent.extras!!.getString("email")

        val navView = findViewById<BottomNavigationView>(R.id.nav_view)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_home) as NavHostFragment?
        navController = navHostFragment!!.navController
        setupWithNavController(navView, navController!!)

        hasWriteStoragePermission()
    }

    // Metodo para cargar nuevos fragmentos
    fun CargarFragmento(fragmento: Int, bundle: Bundle?) {
        navController!!.navigate(fragmento, bundle)
    }

    // Metodo para guardar QR localmente
    fun GuardarQR(qrID: String, qrImagenBitmap: Bitmap) {
        //  Variable para guardar el estado de si la imagen se guardo o no
        val guardada: Boolean
        val finalFile: File

        var qrName = ""
        qrName = if (qrID.length < 12) qrID
        else qrID.substring(0,14)

        // OutputStream para escribir/guardar la imagen
        val writer: OutputStream?
        try {
            // Si el dispositivo es android 9 o superior
            writer =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < 30) {
                val resolver = contentResolver
                val contentValues = ContentValues()

                // Guardamos la imagen en el destino DCIM/QR, imagen de tipo .png con el nombre de "QR - " + las 5 primeras letras del QR
                contentValues.put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    "QR-$qrName"
                )
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + "QR")

                // Insertamos la imagen en los archivos a escribir
                val imageUri =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                finalFile = imageUri!!.toFile()
                resolver.openOutputStream(imageUri)
            } else {
                // El proceso a seguir es el mismo pero de diferente manera por la version de android
                val directorioImagen = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM
                ).toString() + File.separator + "QR"
                val archivoImagen = File(directorioImagen)
                if (!archivoImagen.exists()) archivoImagen.mkdir()

                val imagen = File(directorioImagen, "QR-$qrName.jpeg")
                finalFile = imagen
                FileOutputStream(imagen)
            }
            guardada = qrImagenBitmap.compress(Bitmap.CompressFormat.JPEG, 100, writer)

            // Al finalizar limpiamos y cerramos el stream
            writer!!.flush()
            writer.close()

            // Informamos que se guardo la imagen correctamente
            Toast.makeText(this, "Imagen guardada", Toast.LENGTH_SHORT).show()

            sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(finalFile)
                )
            )

        } catch (e: IOException) {
            // Si tenemos algun error lo mostramos
            Toast.makeText(this, "Error al guardar: $e", Toast.LENGTH_SHORT).show()
        }
    }

    // Metodo para crear un bitmap(bits) de un String
    // Lo usaremos para generar las imagenes de los codigos qr
    fun CrearImagenQR(contenido: String): Bitmap {
        val escritor = MultiFormatWriter()
        var matriz: BitMatrix? = null
        try {
            // Convertimos la cadena de texto del ID a una matriz, especificamos que se trata de un codigo QR
            matriz = escritor.encode(contenido, BarcodeFormat.QR_CODE, 500, 500)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        val codificadorQR = BarcodeEncoder()

        // Regresamos el conjunto de bits para crear la iamgen
        return codificadorQR.createBitmap(matriz)
    }

    private fun hasWriteStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val androidElevenPermission =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    with(binding.root) {
                        when {
                            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                            }
                        }
                    }
                }

            androidElevenPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    101
                )
                return false
            }
        }
        return true
    }
}