package com.ipn.qrlink.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ipn.qrlink.databinding.FragmentForgotBinding

class ForgotFragment : Fragment() {
    private var _binding: FragmentForgotBinding? = null

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
        _binding = FragmentForgotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonReturn.setOnClickListener {
            onBackPressed()
        }

        // Evento al clickear el boton "Registrarse"
        binding.buttonRecover.setOnClickListener (View.OnClickListener{
            val email = binding.editTextEmail.text.toString()

            // Comprobamos que el email y contrasena no esten vacios, si estan vacios mostramos una alerta y regresamos
            if (email == "") {
                Toast.makeText(
                    requireContext(),
                    "Introduce un email registrado para continuar.",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
            }

            // Mandamos a firebase el email y contrasena ingresados para comprobar si existen y si no, registrarlos
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    // Si existen cargamos la siguiente pantalla y enviamos como extra el correo del usuario
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Correo enviado, revisa tu email", Toast.LENGTH_LONG)
                            .show()
                        onBackPressed()
                    } else
                        Toast.makeText(requireContext(), "Este email no esta registrado", Toast.LENGTH_SHORT)
                            .show()
                }
        })
    }

    fun onBackPressed() {
        parentFragmentManager.popBackStackImmediate();
    }
}