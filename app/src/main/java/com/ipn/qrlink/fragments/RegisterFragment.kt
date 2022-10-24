package com.ipn.qrlink.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.ipn.qrlink.activities.AuthActivity
import com.ipn.qrlink.databinding.FragmentRegisterBinding


class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Referencia a la base de datos firestore
    var firebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonReturn.setOnClickListener {
            onBackPressed()
        }

        // Evento al clickear el boton "Registrarse"
        binding.buttonRegister.setOnClickListener (View.OnClickListener{
            val email = binding.editTextEmail.text.toString()
            val contrasena = binding.editTextContrasena.text.toString()
            val contrasenados = binding.editTextRepiteContrasena.text.toString()

            // Comprobamos que el email y contrasena no esten vacios, si estan vacios mostramos una alerta y regresamos
            if (email == "" || contrasena == "" || contrasenados == "") {
                Toast.makeText(
                    requireContext(),
                    "Introduce los datos necesarios para registrarte.",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
            } else if (contrasena != contrasenados) {
                Toast.makeText(
                    requireContext(),
                    "Las contraseñas no coinciden.",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
            }

            // Mandamos a firebase el email y contrasena ingresados para comprobar si existen y si no, registrarlos
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, contrasena)
                .addOnCompleteListener { task ->
                    // Si existen cargamos la siguiente pantalla y enviamos como extra el correo del usuario
                    if (task.isSuccessful) {
                        Toast.makeText((activity as AuthActivity),"Cuenta creada, revisa tu email", Toast.LENGTH_LONG)
                            .show()
                        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
                        user!!.sendEmailVerification()
                        onBackPressed()
                    } else
                        Toast.makeText((activity as AuthActivity), "Email ya registrado", Toast.LENGTH_SHORT)
                            .show()
                }
        })
    }

    fun onBackPressed() {
        parentFragmentManager.popBackStackImmediate();
    }
}