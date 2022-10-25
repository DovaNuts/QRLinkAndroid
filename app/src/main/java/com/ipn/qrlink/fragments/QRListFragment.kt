package com.ipn.qrlink.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ipn.qrlink.R
import com.ipn.qrlink.activities.HomeActivity
import com.ipn.qrlink.databinding.FragmentQrlistBinding
import com.ipn.qrlink.qrCode
import com.ipn.qrlink.qrListAdapter

class QRListFragment : Fragment() {
    private var _binding: FragmentQrlistBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Referencia a la base de datos firestore
    var firebaseFirestore = FirebaseFirestore.getInstance()

    private var userEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity: HomeActivity? = activity as HomeActivity?

        userEmail = FirebaseAuth.getInstance().currentUser!!.email!!

        // Referencia al elemento de lista de la interfaz
        val listView = binding.listaQR

        // Lista de codigos QR registrados
        val listaQR = ArrayList<qrCode>()

        // Entramos a la coleccion Codigos y de ahi al documento con el nombre del correo del usuario actual
        firebaseFirestore.collection("Codigos").document(userEmail).get()
            .addOnCompleteListener { task ->
                // Obtenemos el documento de los codigos registrados por el usuario
                val documento = task.result

                // Si existe el documento, o sea no esta vacio
                if (documento.exists()) {
                    // Guardamos el contenido en un Hashmap, ID/Contenido
                    val codigos = documento.data

                    // Si el hashmap existe y no esta vacio
                    if (codigos != null) {
                        // Agregamos todos los codigos a la lista
                        // Usamos replaceAll para remover cualquier formato extra que firebase incluya y asi obtener el contenido original
                        for ((key, value) in codigos)
                            listaQR.add(
                                qrCode(key, value.toString().replace("[\\[\\](){}]".toRegex(), ""))
                            )
                    }
                }
                val adapter = qrListAdapter(requireContext(), listaQR)
                listView.adapter = adapter

                if (adapter.count == 0) binding.textViewEmpty.visibility = View.VISIBLE
                binding.progressCircular.visibility = View.GONE

                // Evento que se ejecuta al hacer click en algun elemento de la lista
                listView.onItemClickListener =
                    OnItemClickListener { adapterView, view, i, l -> // Creamos un nuevo contenedor para la informacion del QR seleccionado
                        val codigoQR: qrCode = listaQR[i]

                        // Agregamos los datos para la siguiente pantalla, el ID del qr y su contenido
                        val bundle = Bundle()
                        bundle.putString("id", codigoQR.id)
                        bundle.putString("contenido", codigoQR.content)

                        // Cargamos la pantalla
                        activity!!.CargarFragmento(R.id.navigation_edit, bundle)
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}