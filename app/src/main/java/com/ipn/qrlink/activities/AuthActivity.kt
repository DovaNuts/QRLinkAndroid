package com.ipn.qrlink.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import com.ipn.qrlink.databinding.ActivityAuthBinding

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

        // Evento al clickear el boton "Entrar"
        binding.buttonEntrar.setOnClickListener(View.OnClickListener {
            val email = binding.editTextEmail.text.toString()
            val contrasena = binding.editTextContrasena.text.toString()

            // Comprobamos que el email y contrasena no esten vacios, si estan vacios mostramos una alerta y regresamos
            if (email == "" || contrasena == "") {
                Toast.makeText(
                    this,
                    "Ingresa un usuario y contrasena para continuar",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
            }

            // Mandamos a firebase el email y contrasena ingresados para comprobar si existen
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, contrasena)
                .addOnCompleteListener { task ->
                    // Si existen cargamos la siguiente pantalla y enviamos como extra el correo del usuario
                    if (task.isSuccessful) {
                        val intent = Intent(this@AuthActivity, HomeActivity::class.java)
                        intent.putExtra("email", email)
                        // Agregamos las "banderas" para limpiar el stack y asi no poder regresar a esta vista
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    } else Toast.makeText(
                        this,
                        "Email y/o contrasena incorrectos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        })

        // Evento al clickear el boton "Registrarse"
        binding.buttonRegistrarse.setOnClickListener(View.OnClickListener {
            val email = binding.editTextEmail.text.toString()
            val contrasena = binding.editTextContrasena.text.toString()

            // Comprobamos que el email y contrasena no esten vacios, si estan vacios mostramos una alerta y regresamos
            if (email == "" || contrasena == "") {
                Toast.makeText(
                    this,
                    "Ingresa un usuario y contrasena para continuar",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
            }

            // Mandamos a firebase el email y contrasena ingresados para comprobar si existen y si no, registrarlos
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, contrasena)
                .addOnCompleteListener { task ->
                    // Si existen cargamos la siguiente pantalla y enviamos como extra el correo del usuario
                    if (task.isSuccessful) {
                        val intent = Intent(this@AuthActivity, HomeActivity::class.java)
                        intent.putExtra("email", email)
                        // Agregamos las "banderas" para limpiar el stack y asi no poder regresar a esta vista
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    } else
                        Toast.makeText(this, "Email ya registrado", Toast.LENGTH_SHORT)
                            .show()
                }
        })
    }
}