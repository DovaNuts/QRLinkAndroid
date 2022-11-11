package com.ipn.qrlink.fragments

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.ipn.qrlink.R
import com.ipn.qrlink.activities.HomeActivity
import com.ipn.qrlink.activities.PDFActivity
import com.ipn.qrlink.databinding.FragmentCreateBinding
import com.ipn.qrlink.utility.Utility
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

class CreateFragment : Fragment() {
    private var _binding: FragmentCreateBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    private lateinit var pdfUri: Uri
    private lateinit var pdfName: String

    // Referencia a la base de datos firestore
    var firebaseFirestore = FirebaseFirestore.getInstance()

    // Variable para guardar la imagen en que se genera a partir del ID del codigo QR para escanear
    var qrImageBitmap: Bitmap? = null

    private var userEmail: String = ""

    private var utility = Utility()

    var qrContentTypeList = arrayOf<String>()
    var qrTypeList = arrayOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userEmail = FirebaseAuth.getInstance().currentUser!!.email!!

        var lastSelectedQRContentView: View = binding.textInputLayout
        var currentSelectedQRContentView: View = binding.textInputLayout

        var lastSelectedPostion: Int = 0
        var currentSelectedPosition: Int = 0

        qrContentTypeList = resources.getStringArray(R.array.qrType)
        qrTypeList = resources.getStringArray(R.array.qrContent)

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm")

        // Spinner tipo de contenido
        binding.qrContentSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    lastSelectedQRContentView = currentSelectedQRContentView
                    lastSelectedQRContentView.visibility = View.GONE

                    lastSelectedPostion = currentSelectedPosition
                    currentSelectedPosition = position

                    when (position) {
                        0 -> currentSelectedQRContentView = binding.textInputLayout
                        1 -> currentSelectedQRContentView = binding.emailHolder
                        2 -> currentSelectedQRContentView = binding.textInputPhone
                        3 -> currentSelectedQRContentView = binding.smsHolder
                        4 -> currentSelectedQRContentView = binding.wifiHolder
                        5 -> currentSelectedQRContentView = binding.calendarHolder
                        6 -> {
                            if (binding.qrTypeSpinner.selectedItemPosition == 1) {
                                binding.qrContentSpinner.setSelection(lastSelectedPostion)
                                Toast.makeText(
                                    context,
                                    "PDF solo está disponible como código dinámico.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else currentSelectedQRContentView = binding.pdfHolder
                        }
                    }

                    currentSelectedQRContentView.visibility = View.VISIBLE
                }
            }

        // Spinner tipo de codigo
        binding.qrTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 1 && currentSelectedQRContentView == binding.pdfHolder) {
                    binding.qrContentSpinner.setSelection(0)

                    Toast.makeText(
                        context,
                        "PDF solo está disponible como código dinámico.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Evento "Guardar imagen"
        binding.buttonGuardarQR.setOnClickListener {
            saveImage()
        }

        // Evento al clickear el boton "Generar QR"
        binding.buttonGenerarQR.setOnClickListener {
            generateQRCode()
        }

        // Date picker para evento
        binding.editTextCalStart.setOnClickListener {
            startDate = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, monthOfYear, dayOfMonth ->
                    startDate!!.set(year, monthOfYear, dayOfMonth)
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            startDate!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            startDate!!.set(Calendar.MINUTE, minute)
                            binding.editTextCalStart.setText(dateFormatter.format(startDate!!.time))
                        }, startDate!![Calendar.HOUR_OF_DAY], startDate!![Calendar.MINUTE], false
                    ).show()
                },
                startDate!![Calendar.YEAR],
                startDate!![Calendar.MONTH],
                startDate!![Calendar.DATE]
            ).show()
        }

        binding.editTextCalEnd.setOnClickListener {
            endDate = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, monthOfYear, dayOfMonth ->
                    endDate!!.set(year, monthOfYear, dayOfMonth)
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            endDate!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            endDate!!.set(Calendar.MINUTE, minute)
                            binding.editTextCalEnd.setText(dateFormatter.format(endDate!!.time))
                        }, endDate!![Calendar.HOUR_OF_DAY], endDate!![Calendar.MINUTE], false
                    ).show()
                }, endDate!![Calendar.YEAR], endDate!![Calendar.MONTH], endDate!![Calendar.DATE]
            ).show()
        }

        binding.buttonUploadFile.setOnClickListener {
            val galleryIntent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT

            // We will be redirected to choose pdf
            galleryIntent.type = "application/pdf"
            startActivityForResult(galleryIntent, 1)
        }

        binding.buttonPreview.setOnClickListener {
            if (contentIsNotNull()) {
                val intent = Intent(requireContext(), PDFActivity::class.java)
                // Agregamos las "banderas" para limpiar el stack y asi no poder regresar a esta vista

                intent.putExtra("pdf", pdfUri.toString())
                startActivity(intent)
            } else Toast.makeText(context, "Sube un documento para continuar.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            pdfUri = data?.data!!
            val uri: Uri = data.data!!
            val uriString: String = uri.toString()
            if (uriString.startsWith("content://")) {
                var myCursor: Cursor? = null
                try {
                    // Setting the PDF to the TextView
                    myCursor = requireContext().contentResolver.query(uri, null, null, null, null)
                    if (myCursor != null && myCursor.moveToFirst()) {
                        pdfName =
                            myCursor.getString(myCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        binding.textViewDocumentName.text = "Documento: $pdfName"
                    }
                } finally {
                    myCursor?.close()
                }
            }
        }
    }

    private fun uploadDocument(): Boolean {
        val dialog = ProgressDialog(context)
        dialog.setMessage("Subiendo documento")
        dialog.show()

        val storageRef = Firebase.storage.reference
        val reference = storageRef.child("PDFs/" + userEmail + "/$pdfName")
        val uploadTask: UploadTask = reference.putFile(pdfUri)

        uploadTask.addOnFailureListener { exception ->
            Toast.makeText(
                context,
                "Error: No se pudo cargar el documento $exception",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }.addOnSuccessListener {
            Toast.makeText(context, "Documento cargado con éxito.", Toast.LENGTH_SHORT).show()
            utility.downloadDocument(pdfName, userEmail, requireContext())
            dialog.dismiss()
        }

        return true
    }

    private fun generateQRCode() {
        if (contentIsNotNull()) {
            val qrHashMap: MutableMap<String, Any> = HashMap()

            // Generamos un ID
            var codeUUID = UUID.randomUUID().toString()

            if (binding.qrTypeSpinner.selectedItem == qrTypeList[0]) {
                codeUUID += "D"

                if (binding.qrContentSpinner.selectedItemPosition == 6) {
                    val uploadResult = uploadDocument()

                    if (!uploadResult) {
                        Toast.makeText(
                            context,
                            "Error: No se pudo cargar el documento.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                }

                qrHashMap[codeUUID] = getEncodedContent()
                qrImageBitmap = (activity as HomeActivity).CrearImagenQR(codeUUID)
            } else {
                codeUUID += "E"
                qrHashMap[codeUUID] =  getEncodedContent()
                qrImageBitmap = (activity as HomeActivity).CrearImagenQR(URLDecoder.decode(qrHashMap[codeUUID] as String))
            }

            binding.imageViewQRCode.setImageBitmap(qrImageBitmap)

            firebaseFirestore.collection("Codigos")
                .document(userEmail)[qrHashMap] = SetOptions.merge()

            Toast.makeText(context, "Código creado.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                binding.root.context,
                "Introduce los datos para generar el código QR.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun contentIsNotNull(): Boolean {
        return when (binding.qrContentSpinner.selectedItem) {
            qrContentTypeList[0] -> binding.editTextText.text!!.isNotEmpty()
            qrContentTypeList[1] -> binding.editTextEmail.text!!.isNotEmpty() && binding.editTextEmailSubject.text!!.isNotEmpty() && binding.editTextEmailMessage.text!!.isNotEmpty()
            qrContentTypeList[2] -> binding.editTextPhone.text!!.isNotEmpty()
            qrContentTypeList[3] -> binding.editTextSMSPhone.text!!.isNotEmpty() && binding.editTextSMSMessage.text!!.isNotEmpty()
            qrContentTypeList[4] -> binding.editTextWifiSSID.text!!.isNotEmpty() && binding.editTextWifiPassword.text!!.isNotEmpty()
            qrContentTypeList[5] -> binding.editTextCalTitle.text!!.isNotEmpty() && binding.editTextCalLocation.text!!.isNotEmpty() && binding.editTextCalStart.text!!.isNotEmpty() && binding.editTextCalEnd.text!!.isNotEmpty()
            qrContentTypeList[6] -> ::pdfName.isInitialized
            else -> false
        }
    }

    private fun getEncodedContent(): String {
        var string = encodeQRContent()
        string = URLEncoder.encode(string, "UTF-8")
        string.replace(" ", "%20")
        return string
    }

    private fun encodeQRContent(): String {

        return when (binding.qrContentSpinner.selectedItem) {
            qrContentTypeList[0] -> binding.editTextText.text.toString()
            qrContentTypeList[1] -> {
                "mailto:" + binding.editTextEmail.text.toString() +
                        "?subject=" + binding.editTextEmailSubject.text.toString() +
                        "&body=" + binding.editTextEmailMessage.text.toString()
            }
            qrContentTypeList[2] -> "tel:" + binding.editTextPhone.text.toString()
            qrContentTypeList[3] -> {
                "smsto:" + binding.editTextSMSPhone.text.toString() +
                        ":" + binding.editTextSMSMessage.text.toString()
            }
            qrContentTypeList[4] -> {
                "WIFI:S:" + binding.editTextWifiSSID.text.toString() +
                        ";T:WPA;P:" +
                        binding.editTextWifiPassword.text.toString() + ";;"
            }
            qrContentTypeList[5] -> {
                val dateFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss")

                "BEGIN:VEVENT\nSUMMARY:" + binding.editTextCalTitle.text.toString() +
                        "\nLOCATION:" + binding.editTextCalLocation.text.toString() +
                        "\nDTSTART:" + dateFormatter.format(startDate!!.time) +
                        "\nDTEND:" + dateFormatter.format(endDate!!.time) +
                        "\nEND:VEVENT"
            }
            qrContentTypeList[6] -> pdfName
            else -> "ERROR"
        }
    }

    private fun saveImage() {
        if (qrImageBitmap == null) Toast.makeText(
            binding.root.context,
            "Crea un código QR primero.",
            Toast.LENGTH_SHORT
        ).show() else (activity as HomeActivity).GuardarQR(getEncodedContent(), qrImageBitmap!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}