package com.ipn.qrlink.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.ipn.qrlink.R
import com.ipn.qrlink.activities.HomeActivity
import com.ipn.qrlink.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Evento al clickear el boton "Entrar"
        binding.buttonEntrar.setOnClickListener(View.OnClickListener {
            val email = binding.editTextEmail.text.toString()
            val contrasena = binding.editTextContrasena.text.toString()

            // Comprobamos que el email y contrasena no esten vacios, si estan vacios mostramos una alerta y regresamos
            if (email == "" || contrasena == "") {
                Toast.makeText(
                    requireContext(),
                    "Introduce tu correo electrónico  y contraseña registrados para continuar.",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
            }

            // Mandamos a firebase el email y contrasena ingresados para comprobar si existen
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, contrasena)
                .addOnCompleteListener { task ->
                    // Si existen cargamos la siguiente pantalla y enviamos como extra el correo del usuario
                    if (task.isSuccessful) {
                        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

                        if (user!!.isEmailVerified) {
                            val intent = Intent(requireContext(), HomeActivity::class.java)
                            intent.putExtra("email", email)
                            // Agregamos las "banderas" para limpiar el stack y asi no poder regresar a esta vista
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            requireActivity().finish()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Verifica tu cuenta para acceder, revisa tu correo electrónico.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else Toast.makeText(
                        requireContext(),
                        "Correo electrónico y/o contraseña incorrectos.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        })

        binding.buttonRecover.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_activity_auth, ForgotFragment::class.java, null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit()
        }

        binding.buttonRegister.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_activity_auth, RegisterFragment::class.java, null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit()
        }
    }
}