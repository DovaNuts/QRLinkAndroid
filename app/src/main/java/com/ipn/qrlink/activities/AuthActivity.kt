package com.ipn.qrlink.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.ipn.qrlink.databinding.ActivityAuthBinding
import com.ipn.qrlink.utility.Utility
import java.net.URLDecoder


class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    private val firebase = FirebaseFirestore.getInstance()
    private var utility = Utility()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setup()
    }

    private fun setup() {

// Creamos los ajutes para la base de datos
        // Especificamos un tamano de cache ilimitado para poder guardar los datos sin problema
        val firebaseSettings = FirebaseFirestoreSettings.Builder()
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()

        // Aplicamos los ajustes
        firebase.firestoreSettings = firebaseSettings
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        if (isOnline(this) && hasWriteStoragePermission()) {
            Thread(Runnable {
                // Intentamos actualizar la base de datos local
                firebase.collection("Codigos").get().addOnCompleteListener { task ->
                    val correos = task.result.documents

                    for (i in correos.indices) {
                        firebase.collection("Codigos").document(correos[i].id).get().addOnCompleteListener { taska ->
                            val codigos = taska.result.data
                            if (codigos != null) {
                                for (codigo in codigos.entries) {
                                    if (codigo.value.toString().endsWith(".pdf",ignoreCase = true)) {
                                        val content = URLDecoder.decode(codigo.value.toString())
                                        utility.downloadDocument(content,correos[i].id,this)
                                    }
                                }
                            }
                        }
                    }
                }
            }).start()
        }

        // Revisamos si el usuario ya se ha logeado en este dispositivo antes, en caso posivito continuamos
        if (FirebaseAuth.getInstance().currentUser != null) {
            val intent = Intent(this@AuthActivity, HomeActivity::class.java)

            // Agregamos las "banderas" para limpiar el stack y asi no poder regresar a esta vista
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)

            finish()
        }
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

            return if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                androidElevenPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                false
            }
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    101
                )
                false
            } else true
        }

        return false
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }
}