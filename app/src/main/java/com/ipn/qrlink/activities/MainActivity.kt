package com.ipn.qrlink.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.ktx.Firebase
import com.ipn.qrlink.databinding.ActivityMainBinding
import com.ipn.qrlink.utility.Utility
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val firebase = FirebaseFirestore.getInstance()

    private var utility = Utility()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hacemos que se muestre en pantalla completa la pantalla de carga
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Creamos los ajutes para la base de datos
        // Especificamos un tamano de cache ilimitado para poder guardar los datos sin problema
        val firebaseSettings = FirebaseFirestoreSettings.Builder()
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()

        // Aplicamos los ajustes
        firebase.firestoreSettings = firebaseSettings
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        if (isOnline(this)) {
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


        loadAuthActivity()
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

    fun loadAuthActivity() {
        val intent = Intent(this, AuthActivity::class.java)
        // Agregamos las "banderas" para limpiar el stack y asi no poder regresar a esta vista
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}