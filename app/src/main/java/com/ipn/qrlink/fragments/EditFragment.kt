package com.ipn.qrlink.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.net.MailTo
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.ipn.qrlink.activities.HomeActivity
import com.ipn.qrlink.activities.PDFActivity
import com.ipn.qrlink.databinding.FragmentEditBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class EditFragment : Fragment() {
    private var _binding: FragmentEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    private lateinit var pdfName: String

    private var connected = false

    private lateinit var newPdfUri: Uri
    private lateinit var newPdfName: String

    // Referencia a la base de datos firestore
    var firebaseFirestore = FirebaseFirestore.getInstance()

    // ID del codigo qr
    var qrUUID: String? = null

    // Contenido del codigo
    var qrContent: String? = null

    // Variable para guardar la imagen en que se genera a partir del ID del codigo QR para escanear
    var qrImageBitmap: Bitmap? = null

    var lastSelectedQRContentView:View? = null
    var currentSelectedQRContentView:View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtenemos los argumentos de la pantalla anterior, en este caso el ID y contenido del QR seleccionado
        val bundle = arguments

        qrUUID = bundle!!.getString("id")
        qrContent = bundle.getString("contenido")

        lastSelectedQRContentView = binding.textInputLayout
        currentSelectedQRContentView = binding.textInputLayout

        decodeAndLoadQRContent(qrContent!!)

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm")

        // Si el ID del qr termina con una D, significa es dinamico
        if (qrUUID!!.last() == 'D') {
            qrImageBitmap = (activity as HomeActivity).CrearImagenQR(qrUUID!!)
        } else {
            qrImageBitmap = (activity as HomeActivity).CrearImagenQR(qrContent!!)
            setupStaticQRCode()
        }

        binding.imageViewQR.setImageBitmap(qrImageBitmap)
        binding.qrTypeSpinner.isEnabled = false

        // Evento al clickear el boton "Eliminar"
        binding.buttonDelete.setOnClickListener {
            val qrHashMap: MutableMap<String, Any> = HashMap()
            qrHashMap[qrUUID!!] = FieldValue.delete()

            if (qrContent!!.endsWith(".pdf",ignoreCase = true)) {
                val storageRef = Firebase.storage.reference
                val reference = storageRef.child("PDFs/"+(activity as HomeActivity).email!!+"/$pdfName")

                reference.delete().addOnCompleteListener {
                    firebaseFirestore.collection("Codigos").document((activity as HomeActivity).email!!).update(qrHashMap)
                    onBackPressed()
                }
            } else {
                firebaseFirestore.collection("Codigos").document((activity as HomeActivity).email!!).update(qrHashMap)
                onBackPressed()
            }
        }

        // Evento al clickear el boton "Guardar imagen"
        binding.buttonSaveImage.setOnClickListener {
            saveImage()
        }

        // Evento al clickear el boton "Actualizar"
        binding.buttonUpdate.setOnClickListener {
            if (updatedContentIsNotNull()) {
                val qrHashMap: MutableMap<String, Any> = HashMap()

                if (binding.qrContentSpinner.selectedItemPosition == 6) {
                    val dialog = ProgressDialog(context)
                    dialog.setMessage("Actualizando documento")
                    dialog.show()

                    val storageRef = Firebase.storage.reference
                    val reference = storageRef.child("PDFs/"+(activity as HomeActivity).email!!+"/$pdfName")

                    reference.delete().addOnSuccessListener {
                        val newReference = storageRef.child("PDFs/"+(activity as HomeActivity).email!!+"/$newPdfName")
                        val uploadTask: UploadTask = newReference.putFile(newPdfUri)

                        uploadTask.addOnFailureListener {  exception ->
                            Toast.makeText(context, "Ocurrio un error al subir el documento $exception", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }.addOnSuccessListener {
                            Toast.makeText(context, "Documento actualizado exitosamente", Toast.LENGTH_SHORT).show()
                            pdfName = newPdfName
                            // Actualizamos el contenido del UUID
                            qrHashMap[qrUUID!!] = getEncodedContent()
                            qrHashMap[qrUUID!!]

                            firebaseFirestore.collection("Codigos").document((activity as HomeActivity).email!!).update(qrHashMap)

                            Toast.makeText(context, "Cambios guardados", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    }.addOnFailureListener {
                        dialog.dismiss()
                        Toast.makeText(context, "Ocurrio un error al actualizar el documento", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Actualizamos el contenido del UUID
                    qrHashMap[qrUUID!!] = getEncodedContent()

                    if (::pdfName.isInitialized) {
                        val storageRef = Firebase.storage.reference
                        val reference = storageRef.child("PDFs/"+(activity as HomeActivity).email!!+"/$pdfName")

                        reference.delete()
                        pdfName = ""
                    }

                    firebaseFirestore.collection("Codigos").document((activity as HomeActivity).email!!).update(qrHashMap)

                    Toast.makeText(context, "Cambios guardados", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(
                    binding.root.context,
                    "Introduzca algunos datos para actualizar el código QR",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Spinner tipo de contenido
        binding.qrContentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadQRContentView(position)
            }
        }

        // Date picker para evento
        binding.editTextCalStart.setOnClickListener {
            startDate = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, monthOfYear, dayOfMonth ->
                    startDate!!.set(year, monthOfYear, dayOfMonth)
                    TimePickerDialog(context,
                        { _, hourOfDay, minute ->
                            startDate!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            startDate!!.set(Calendar.MINUTE, minute)
                            binding.editTextCalStart.setText(dateFormatter.format(startDate!!.time))
                        }, startDate!![Calendar.HOUR_OF_DAY], startDate!![Calendar.MINUTE], false
                    ).show()
                }, startDate!![Calendar.YEAR], startDate!![Calendar.MONTH], startDate!![Calendar.DATE]
            ).show()
        }

        binding.editTextCalEnd.setOnClickListener {
            endDate = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, monthOfYear, dayOfMonth ->
                    endDate!!.set(year, monthOfYear, dayOfMonth)
                    TimePickerDialog(context,
                        { _, hourOfDay, minute ->
                            endDate!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            endDate!!.set(Calendar.MINUTE, minute)
                            binding.editTextCalEnd.setText(dateFormatter.format(endDate!!.time))
                        }, endDate!![Calendar.HOUR_OF_DAY], endDate!![Calendar.MINUTE], false
                    ).show()
                }, endDate!![Calendar.YEAR], endDate!![Calendar.MONTH], endDate!![Calendar.DATE]
            ).show()
        }

        binding.buttonPreview.setOnClickListener {
            if (contentIsNotNull()) {
                val intent = Intent(requireContext(), PDFActivity::class.java)
                val documentURI = "hola:PDFs/"+(activity as HomeActivity).email!!+"/$pdfName"
                intent.putExtra("pdf", documentURI)
                startActivity(intent)
            } else Toast.makeText(context, "Primero sube un documento", Toast.LENGTH_SHORT).show()
        }

        binding.buttonDownload.setOnClickListener {
            val storageRef = Firebase.storage.reference
            val reference = storageRef.child("PDFs/"+(activity as HomeActivity).email!!+"/$pdfName")

            //  Variable para guardar el estado de si la imagen se guardo o no
            val guardada: Boolean
            val finalFile: File

            // OutputStream para escribir/guardar la imagen
            val writer: OutputStream?
            try {
                // Si el dispositivo es android 9 o superior
                writer =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < 30) {
                    val resolver = requireContext().contentResolver
                    val contentValues = ContentValues()

                    // Guardamos la imagen en el destino DCIM/QR, imagen de tipo .png con el nombre de "QR - " + las 5 primeras letras del QR
                    contentValues.put(
                        MediaStore.DownloadColumns.DISPLAY_NAME,
                        pdfName
                    )
                    contentValues.put(MediaStore.DownloadColumns.MIME_TYPE, "document/pdf")
                    contentValues.put(MediaStore.DownloadColumns.RELATIVE_PATH, "Documents")

                    // Insertamos la imagen en los archivos a escribir
                    val documentUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    finalFile = documentUri!!.toFile()
                    resolver.openOutputStream(documentUri)
                } else {
                    // El proceso a seguir es el mismo pero de diferente manera por la version de android
                    val directorioDocumento = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ).toString()
                    val documento = File(directorioDocumento, pdfName)
                    finalFile = documento
                    FileOutputStream(documento)
                }

                reference.getFile(finalFile).addOnSuccessListener {
                    Toast.makeText(context, "Documento descargado", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(context, "Ocurrio un error al descargar el documento", Toast.LENGTH_SHORT).show()
                }

                // Al finalizar limpiamos y cerramos el stream
                writer!!.flush()
                writer.close()

                // Informamos que se guardo la imagen correctamente
                Toast.makeText(context, "Imagen guardada", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                // Si tenemos algun error lo mostramos
                Toast.makeText(context, "Error al guardar: $e", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonUploadFile.setOnClickListener {
            val galleryIntent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT

            // We will be redirected to choose pdf
            galleryIntent.type = "application/pdf"
            startActivityForResult(galleryIntent, 1)
        }
    }

    private fun contentIsNotNull() : Boolean {
        return when (binding.qrContentSpinner.selectedItem) {
            "Texto" -> binding.editTextText.text!!.isNotEmpty()
            "Email" -> binding.editTextEmail.text!!.isNotEmpty() && binding.editTextEmailSubject.text!!.isNotEmpty() && binding.editTextEmailMessage.text!!.isNotEmpty()
            "Telefono" -> binding.editTextPhone.text!!.isNotEmpty()
            "SMS" -> binding.editTextSMSPhone.text!!.isNotEmpty() && binding.editTextSMSMessage.text!!.isNotEmpty()
            "Wifi" -> binding.editTextWifiSSID.text!!.isNotEmpty() && binding.editTextWifiPassword.text!!.isNotEmpty()
            "Evento" -> binding.editTextCalTitle.text!!.isNotEmpty() && binding.editTextCalLocation.text!!.isNotEmpty() && binding.editTextCalStart.text!!.isNotEmpty() && binding.editTextCalEnd.text!!.isNotEmpty()
            "PDF" -> ::pdfName.isInitialized
            else -> false
        }
    }

    private fun updatedContentIsNotNull() : Boolean {
        return when (binding.qrContentSpinner.selectedItem) {
            "Texto" -> binding.editTextText.text!!.isNotEmpty()
            "Email" -> binding.editTextEmail.text!!.isNotEmpty() && binding.editTextEmailSubject.text!!.isNotEmpty() && binding.editTextEmailMessage.text!!.isNotEmpty()
            "Telefono" -> binding.editTextPhone.text!!.isNotEmpty()
            "SMS" -> binding.editTextSMSPhone.text!!.isNotEmpty() && binding.editTextSMSMessage.text!!.isNotEmpty()
            "Wifi" -> binding.editTextWifiSSID.text!!.isNotEmpty() && binding.editTextWifiPassword.text!!.isNotEmpty()
            "Evento" -> binding.editTextCalTitle.text!!.isNotEmpty() && binding.editTextCalLocation.text!!.isNotEmpty() && binding.editTextCalStart.text!!.isNotEmpty() && binding.editTextCalEnd.text!!.isNotEmpty()
            "PDF" -> ::newPdfName.isInitialized
            else -> false
        }
    }

    private fun checkInternetConnection() {
        val connectedRef = Firebase.database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                connected = snapshot.getValue(Boolean::class.java) ?: false
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            newPdfUri = data?.data!!
            val uri: Uri = data.data!!
            val uriString: String = uri.toString()
            if (uriString.startsWith("content://")) {
                var myCursor: Cursor? = null
                try {
                    // Setting the PDF to the TextView
                    myCursor = requireContext().contentResolver.query(uri, null, null, null, null)
                    if (myCursor != null && myCursor.moveToFirst()) {
                        newPdfName = myCursor.getString(myCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        binding.textViewDocumentName.text = "Documento: $newPdfName"
                    }
                } finally {
                    myCursor?.close()
                }
            }
        }
    }

    private fun loadQRContentView(position: Int) {
        lastSelectedQRContentView = currentSelectedQRContentView
        lastSelectedQRContentView!!.visibility = View.GONE

        when (position) {
            0 -> {
                currentSelectedQRContentView = binding.textInputLayout
                binding.qrContentSpinner.setSelection(0)
            }
            1 -> {
                currentSelectedQRContentView = binding.emailHolder
                binding.qrContentSpinner.setSelection(1)
            }
            2 -> {
                currentSelectedQRContentView = binding.textInputPhone
                binding.qrContentSpinner.setSelection(2)
            }
            3 -> {
                currentSelectedQRContentView = binding.smsHolder
                binding.qrContentSpinner.setSelection(3)
            }
            4 -> {
                currentSelectedQRContentView = binding.wifiHolder
                binding.qrContentSpinner.setSelection(4)
            }
            5 -> {
                currentSelectedQRContentView = binding.calendarHolder
                binding.qrContentSpinner.setSelection(5)
            }
            6 -> {
                currentSelectedQRContentView = binding.pdfHolder
                binding.qrContentSpinner.setSelection(6)
            }
        }
        currentSelectedQRContentView!!.visibility = View.VISIBLE
    }

    private fun setupStaticQRCode() {
        binding.buttonUpdate.visibility = View.GONE

        binding.qrContentSpinner.isEnabled = false

        binding.qrTypeSpinner.setSelection(1)

        // Texto
        binding.editTextText.isEnabled = false

        // Email
        binding.editTextEmail.isEnabled = false
        binding.editTextEmailSubject.isEnabled = false
        binding.editTextEmailMessage.isEnabled = false

        // Telefono
        binding.editTextPhone.isEnabled = false

        // SMS
        binding.editTextSMSPhone.isEnabled = false
        binding.editTextSMSMessage.isEnabled = false

        // Wifi
        binding.editTextWifiSSID.isEnabled = false
        binding.editTextWifiPassword.isEnabled = false

        // Calendario
        binding.editTextCalTitle.isEnabled = false
        binding.textInputCalLocation.isEnabled = false
        binding.editTextCalStart.isEnabled = false
        binding.textInputCalEnd.isEnabled = false
    }

    private fun getEncodedContent() : String {
        var string = encodeQRContent()
        string = URLEncoder.encode(string, "UTF-8");
        string.replace(" ", "%20");
        return string
    }

    private fun encodeQRContent() : String {
        return when (binding.qrContentSpinner.selectedItem) {
            "Texto" -> binding.editTextText.text.toString()
            "Email" ->  {
                "mailto:"+binding.editTextEmail.text.toString()+
                        "?subject="+ binding.editTextEmailSubject.text.toString()+
                        "&body=" + binding.editTextEmailMessage.text.toString()
            }
            "Telefono" -> "tel:"+binding.editTextPhone.text.toString()
            "SMS" -> {
                "smsto:" + binding.editTextSMSPhone.text.toString() +
                        ":"+ binding.editTextSMSMessage.text.toString()
            }
            "Wifi" -> {
                "WIFI:S:"+binding.editTextWifiSSID.text.toString()+
                        ";T:WPA;P:"+
                        binding.editTextWifiPassword.text.toString()+";;"
            }
            "Evento" -> {
                val dateFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss")

                "BEGIN:VEVENT\nSUMMARY:"+ binding.editTextCalTitle.text.toString()+
                        "\nLOCATION:" + binding.editTextCalLocation.text.toString() +
                        "\nDTSTART:" + dateFormatter.format(startDate!!.time) +
                        "\nDTEND:" + dateFormatter.format(endDate!!.time) +
                        "\nEND:VEVENT"
            }
            "PDF" -> newPdfName
            else -> "ERROR"
        }
    }

    private fun decodeAndLoadQRContent(encodedContent: String) {
        val content = URLDecoder.decode(encodedContent)
        when  {
            content.startsWith("mailto:", ignoreCase = true) -> {
                val decodedMail = MailTo.parse(content)

                loadQRContentView(1)

                binding.editTextEmail.setText(decodedMail.to)
                binding.editTextEmailSubject.setText(decodedMail.subject)
                binding.editTextEmailMessage.setText(decodedMail.body)
            }

            content.startsWith("tel:", ignoreCase = true) -> {
                val decodedPhone = content.split(":")
                val phoneNumber = decodedPhone[1]

                loadQRContentView(2)

                binding.editTextPhone.setText(phoneNumber)
            }

            content.startsWith("smsto:", ignoreCase = true) -> {
                val decodedMessage = content.split(":")
                val phoneNumber = decodedMessage[1]
                val messageContent = decodedMessage[2]

                loadQRContentView(3)

                binding.editTextSMSPhone.setText(phoneNumber)
                binding.editTextSMSMessage.setText(messageContent)
            }

            content.startsWith("WIFI:S:", ignoreCase = true) -> {
                val decodedWIFI = content.split(":")
                val ssid = decodedWIFI[2].split(";")[0]
                val password = decodedWIFI[4].split(";")[0]

                loadQRContentView(4)

                binding.editTextWifiSSID.setText(ssid)
                binding.editTextWifiPassword.setText(password)
            }

            content.startsWith("BEGIN:VEVENT\nSUMMARY:", ignoreCase = true) -> {
                val decodedEvent = content.split("\n")

                val parseFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss")
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm")

                val title = decodedEvent[1].split(":")[1]
                val location = decodedEvent[2].split(":")[1]
                val start = dateFormatter.format(parseFormatter.parse(decodedEvent[3].split(":")[1]))
                val end = dateFormatter.format(parseFormatter.parse(decodedEvent[4].split(":")[1]))

                loadQRContentView(5)

                binding.editTextCalTitle.setText(title)
                binding.editTextCalLocation.setText(location)
                binding.editTextCalStart.setText(start)
                binding.editTextCalEnd.setText(end)
            }
            content.endsWith(".pdf", ignoreCase = true) -> {
                binding.textViewDocumentName.text = content
                pdfName = content
                loadQRContentView(6)
            }
            else -> {
                binding.editTextText.setText(content)
                loadQRContentView(0)
            }
        }
    }

    private fun saveImage() {
        if (qrImageBitmap == null) Toast.makeText(
            binding.root.context,
            "Genera un codigo QR primero",
            Toast.LENGTH_SHORT
        ).show() else (activity as HomeActivity).GuardarQR(getEncodedContent(), qrImageBitmap!!)
    }

    fun onBackPressed() {
        parentFragmentManager.popBackStackImmediate();
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}