package com.ipn.qrlink.fragments

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.net.MailTo
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.ipn.qrlink.databinding.FragmentScanBinding
import kotlin.time.Duration.Companion.milliseconds


class ScanFragment : Fragment() {
    private var _binding: FragmentScanBinding? = null

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
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initiateScan()

        binding.buttonScan.setOnClickListener {
            initiateScan()
        }
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(context, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            } else {
                codigoEscaneado(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun initiateScan() {
        IntentIntegrator.forSupportFragment(this)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt("Coloca el código dentro del marco")
            .initiateScan()
    }


    private fun decodeAndLoadQRContent(content: String) : String {
        when {
            content.startsWith("mailto:", ignoreCase = true) -> {
                val decodedMail = MailTo.parse(content)

                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:") // Solo las apps de email soportan esto

                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(decodedMail.to))
                intent.putExtra(Intent.EXTRA_SUBJECT, decodedMail.subject)
                intent.putExtra(Intent.EXTRA_TEXT, decodedMail.body)
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                }

                return "Para: " + decodedMail.to + "\nAsunto: "+decodedMail.subject+"\nMensaje: "+decodedMail.body
            }
            content.startsWith("tel:", ignoreCase = true) -> {
                val decodedPhone = content.split(":")
                val phoneNumber = decodedPhone[1]

                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:" + phoneNumber)

                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                }

                return "Llamada al: " + phoneNumber
            }
            content.startsWith("smsto:", ignoreCase = true) -> {
                val decodedMessage = content.split(":")
                val phoneNumber = decodedMessage[1]
                val messageContent = decodedMessage[2]

                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("smsto:" + phoneNumber) // Solo las apps de sms soportan esto

                intent.putExtra("sms_body", messageContent)
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                }

                return "Mensaje\nPara: $phoneNumber\nContenido: $messageContent"
            }
            content.startsWith("WIFI:S:", ignoreCase = true) -> {
                val decodedWIFI = content.split(":")
                val ssid = decodedWIFI[2].split(";")[0]
                val password = decodedWIFI[4].split(";")[0]

                val wifiConfig = WifiConfiguration()

                wifiConfig.SSID = String.format("\"%s\"", ssid)
                wifiConfig.preSharedKey = String.format("\"%s\"", password)

                val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
                val netId = wifiManager.addNetwork(wifiConfig)
                wifiManager.disconnect()
                wifiManager.enableNetwork(netId, true)
                wifiManager.reconnect()

                return "Credenciales para red Wifi\nNombre de red: $ssid\nContrasena: *******"
            }
            content.startsWith("BEGIN:VEVENT\nSUMMARY:", ignoreCase = true) -> {
                val decodedEvent = content.split("\n")
                val parseFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss")
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm")

                val title = decodedEvent[1].split(":")[1]
                val location = decodedEvent[2].split(":")[1]
                val start = dateFormatter.format(parseFormatter.parse(decodedEvent[3].split(":")[1]))
                val end = dateFormatter.format(parseFormatter.parse(decodedEvent[4].split(":")[1]))

                val startCalendar: Calendar = Calendar.getInstance()
                startCalendar.time = dateFormatter.parse(start)
                val startTime: Long = startCalendar.timeInMillis

                val endCalendar: Calendar = Calendar.getInstance()
                endCalendar.time = dateFormatter.parse(end)
                val endTime: Long = endCalendar.timeInMillis

                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                }

                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                }
                return "Evento\nNombre: $title\nUbicacion: $location\nFecha de inicio: $start\nFinal: $end"
            }
            URLUtil.isValidUrl(content) -> {
                val webpage: Uri = Uri.parse(content)
                val intent = Intent(Intent.ACTION_VIEW, webpage)

                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                }

                return "Sitio web: $content"
            }
            else -> {
                return content
            }
        }
    }

    fun codigoEscaneado(dataResult: String) {
        // Cuando escaneamos un codigo necesitamos comprobar si esta registrado en la base de datos para esto
        // Cargamos todos los correos dentro de la coleccion Codigos
        firebaseFirestore.collection("Codigos").get()
            .addOnCompleteListener { task -> // Una vez cargados, los metemos en una lista
                val correos = task.result.documents

                // Variable para saber si encontramos el codigo
                var encontrado = false

                // Para cada uno de los correos revisamos si contienen el codigo QR
                for (i in correos.indices) {
                    // Comprobamos si el correo contiene el codigo escaneado, si escaneamos un codigo registado entonces
                    // deberia contener el ID
                    if (correos[i][dataResult] != null) {
                        // Cuando encontramos el codigo mostramos el contenido
                        binding.textViewResult.text =
                            decodeAndLoadQRContent(correos[i][dataResult].toString())
                        encontrado = true
                        break
                    }
                }

                // Si no encontramos el codigo significa que escaneamos un codigo "normal" asi que mostramos
                // directamente la informacion escaneada
                if (!encontrado) binding.textViewResult.text = decodeAndLoadQRContent(dataResult)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}