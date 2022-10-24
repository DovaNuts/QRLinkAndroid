package com.ipn.qrlink.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.FragmentNavigator
import com.google.firebase.auth.FirebaseAuth
import com.ipn.qrlink.R
import com.ipn.qrlink.databinding.ActivityAuthBinding
import com.ipn.qrlink.fragments.ForgotFragment


class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Revisamos si el usuario ya se ha logeado en este dispositivo antes, en caso posivito continuamos
        if (FirebaseAuth.getInstance().currentUser != null) {
            val intent = Intent(this@AuthActivity, HomeActivity::class.java)
            intent.putExtra("email", FirebaseAuth.getInstance().currentUser?.email)

            // Agregamos las "banderas" para limpiar el stack y asi no poder regresar a esta vista
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)

            finish()
        }
    }
}