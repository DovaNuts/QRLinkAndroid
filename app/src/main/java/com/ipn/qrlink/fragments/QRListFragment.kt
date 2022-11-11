package com.ipn.qrlink.fragments

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.widget.SearchView
import androidx.core.net.MailTo
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ipn.qrlink.R
import com.ipn.qrlink.activities.HomeActivity
import com.ipn.qrlink.databinding.FragmentQrlistBinding
import com.ipn.qrlink.qrCode
import com.ipn.qrlink.qrListAdapter
import java.net.URLDecoder

class QRListFragment : Fragment() {
    private var _binding: FragmentQrlistBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Referencia a la base de datos firestore
    var firebaseFirestore = FirebaseFirestore.getInstance()

    private var userEmail: String = ""

    var listaQRCompleta = ArrayList<qrCode>()
    var listaFiltro = ArrayList<qrCode>()

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

        binding.listaQR.visibility = View.GONE

        // Referencia al elemento de lista de la interfaz
        val listView = binding.listaQR

        // Lista de codigos QR registrados
        val adapter = qrListAdapter(requireContext(), listaFiltro)
        listView.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                    listaFiltro = ArrayList<qrCode>()

                    listaQRCompleta.forEach {
                        val content = URLDecoder.decode(it.content)
                        val id = it.id
                        val description: Array<String>  = when {
                            content.startsWith("mailto:", ignoreCase = true) -> arrayOf("mail","email","correo","electronico","electrónico","@","para","asunto","contenido")
                            content.startsWith("tel:", ignoreCase = true) -> arrayOf("telefono","llamada","numero","tel","cel","telefónico","telefonico","para")
                            content.startsWith("smsto:", ignoreCase = true) -> arrayOf("mensaje","sms","mensaje de texto","para")
                            content.startsWith("WIFI:S:", ignoreCase = true) -> arrayOf("wifi","wi-fi","red","internet")
                            content.startsWith("BEGIN:VEVENT\nSUMMARY:", ignoreCase = true) -> arrayOf("evento","event","calendario","fecha","ubicacion","ubicación","titulo")
                            content.endsWith(".pdf",ignoreCase = true) -> arrayOf("pdf",".pdf","documento")
                            else -> arrayOf("texto")
                        }
                        val type: Array<String>  = when {
                            id.endsWith("D", ignoreCase = true) -> arrayOf("dinamico","dinámico","dinámicos","dinamicos")
                            else -> arrayOf("estático","estatico","estáticos","estaticos")
                        }
                        if (content.contains(query, ignoreCase = true) || query.lowercase() in description || query.lowercase() in type)
                            listaFiltro.add(it)
                    }
                    listView.adapter = qrListAdapter(requireContext(), listaFiltro)

                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    listaFiltro = listaQRCompleta
                    listView.adapter = qrListAdapter(requireContext(), listaFiltro)
                    return true
                }
                return false
            }
        })

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
                        listaQRCompleta = ArrayList<qrCode>()
                        for ((key, value) in codigos)
                            listaQRCompleta.add(
                                qrCode(key, value.toString().replace("[\\[\\](){}]".toRegex(), ""))
                            )
                    }
                }

                listaFiltro = listaQRCompleta
                val adapter = qrListAdapter(requireContext(), listaQRCompleta)
                listView.adapter = adapter

                if (adapter.count == 0) binding.textViewEmpty.visibility = View.VISIBLE
                    binding.progressCircular.visibility = View.GONE
                if (adapter.count == 0) binding.textViewEmpty.visibility = View.VISIBLE
                    else binding.listaQR.visibility = View.VISIBLE

                // Evento que se ejecuta al hacer click en algun elemento de la lista
                listView.onItemClickListener =
                    OnItemClickListener { adapterView, view, i, l -> // Creamos un nuevo contenedor para la informacion del QR seleccionado
                        val codigoQR: qrCode = listaFiltro[i]

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