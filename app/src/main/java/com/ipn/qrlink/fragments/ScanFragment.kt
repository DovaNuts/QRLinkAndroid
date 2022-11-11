package com.ipn.qrlink.fragments

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings.ACTION_WIFI_ADD_NETWORKS
import android.provider.Settings.EXTRA_WIFI_NETWORK_LIST
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.net.MailTo
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.ipn.qrlink.R
import com.ipn.qrlink.activities.PDFActivity
import com.ipn.qrlink.databinding.FragmentScanBinding
import com.ipn.qrlink.utility.Utility
import java.net.URLDecoder


class ScanFragment : Fragment() {
    private var _binding: FragmentScanBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Referencia a la base de datos firestore
    var firebaseFirestore = FirebaseFirestore.getInstance()

    private var utility = Utility()

    var qrContentTypeList = arrayOf<String>()

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

        qrContentTypeList = resources.getStringArray(R.array.qrType)

        initiateScan()

        binding.buttonScan.setOnClickListener {
            initiateScan()
        }
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result.contents == null) Toast.makeText(
            context,
            "Escaneo cancelado.",
            Toast.LENGTH_SHORT
        ).show()
        else codigoEscaneado(result.contents)
    }

    fun initiateScan() {
        IntentIntegrator.forSupportFragment(this)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt("Coloca el código dentro del marco")
            .setBeepEnabled(false)
            .initiateScan()
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        return false
    }

    private fun decodeAndLoadQRContent(encodedContent: String, userEmail: String): String {
        val content = URLDecoder.decode(encodedContent)
        when {
            content.startsWith("mailto:", ignoreCase = true) -> {
                val decodedMail = MailTo.parse(content)

                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:") // Solo las apps de email soportan esto
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(decodedMail.to))
                    putExtra(Intent.EXTRA_SUBJECT, decodedMail.subject)
                    putExtra(Intent.EXTRA_TEXT, decodedMail.body)
                }

                startActivity(intent)

                binding.textViewResultType.text = qrContentTypeList[1] +":"
                binding.imageViewScan.setImageResource(R.drawable.maillogo)
                return "Para: " + decodedMail.to + "\nAsunto: " + decodedMail.subject + "\nMensaje: " + decodedMail.body
            }
            content.startsWith("tel:", ignoreCase = true) -> {
                val decodedPhone = content.split(":")
                val phoneNumber = decodedPhone[1]

                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:" + phoneNumber)
                }

                startActivity(intent)

                binding.textViewResultType.text = qrContentTypeList[2] +":"
                binding.imageViewScan.setImageResource(R.drawable.phonelogo)
                return "Numero: $phoneNumber"
            }
            content.startsWith("smsto:", ignoreCase = true) -> {
                val decodedMessage = content.split(":")
                val phoneNumber = decodedMessage[1]
                val messageContent = decodedMessage[2]

                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:" + phoneNumber) // Solo las apps de sms soportan esto
                    putExtra("sms_body", messageContent)
                }

                startActivity(intent)

                binding.textViewResultType.text = qrContentTypeList[3] +":"
                binding.imageViewScan.setImageResource(R.drawable.smslogo)
                return "Numero: $phoneNumber\nMensaje: $messageContent"
            }
            content.startsWith("WIFI:S:", ignoreCase = true) -> {
                val decodedWIFI = content.split(":")
                val ssid = decodedWIFI[2].split(";")[0]
                val password = decodedWIFI[4].split(";")[0]

                val wifiManager =
                    requireContext().applicationContext.getSystemService(WIFI_SERVICE) as WifiManager?
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    try {
                        val wifiConfig = WifiConfiguration()
                        wifiConfig.SSID = "\"" + ssid + "\""
                        wifiConfig.preSharedKey = "\"" + password + "\""
                        val netId = wifiManager!!.addNetwork(wifiConfig)
                        wifiManager.disconnect()
                        wifiManager.enableNetwork(netId, true)
                        wifiManager.reconnect()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val wifiSuggestionBuilder = WifiNetworkSuggestion.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(password)
                        .build()

                    val suggestionsList = arrayListOf<WifiNetworkSuggestion>(wifiSuggestionBuilder)
                    val intent = Intent(ACTION_WIFI_ADD_NETWORKS)
                    intent.putParcelableArrayListExtra(EXTRA_WIFI_NETWORK_LIST, suggestionsList);
                    requireActivity().startActivityForResult(intent, 1000)
                }

                binding.textViewResultType.text = qrContentTypeList[4] +":"
                binding.imageViewScan.setImageResource(R.drawable.wifilogo)
                return "Red: $ssid\nContraseña: *******"
            }
            content.startsWith("BEGIN:VEVENT\nSUMMARY:", ignoreCase = true) -> {
                val decodedEvent = content.split("\n")
                val parseFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss")
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm")

                val title = decodedEvent[1].split(":")[1]
                val location = decodedEvent[2].split(":")[1]
                val start =
                    dateFormatter.format(parseFormatter.parse(decodedEvent[3].split(":")[1]))
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

                startActivity(intent)

                binding.textViewResultType.text = qrContentTypeList[5] +":"
                binding.imageViewScan.setImageResource(R.drawable.eventlogo)
                return "Titulo: $title\nUbicación: $location\nFecha de inicio: $start\nFinal: $end"
            }
            content.endsWith(".pdf", ignoreCase = true) -> {
                if (isOnline(requireContext())) {
                    val intent = Intent(requireContext(), PDFActivity::class.java)
                    val documentURI = "hola:PDFs/$userEmail/$content"
                    utility.downloadDocument(content, userEmail, requireContext())
                    intent.putExtra("pdf", documentURI)
                    startActivity(intent)
                } else {
                    val intent = Intent(requireContext(), PDFActivity::class.java)
                    val documentURI =
                        "offline:" + utility.getDocumentURI(content, userEmail, requireContext())
                    intent.putExtra("pdf", documentURI)
                    startActivity(intent)
                }

                binding.textViewResultType.text = qrContentTypeList[6] +":"
                binding.imageViewScan.setImageResource(R.drawable.pdflogo)
                return content
            }
            URLUtil.isValidUrl(content) -> {
                val webpage: Uri = Uri.parse(content)

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = webpage
                }

                startActivity(intent)

                binding.textViewResultType.text = "Sitio web:"
                binding.imageViewScan.setImageResource(R.drawable.networklogo)
                return "Enlace: $content"
            }
            else -> {
                binding.textViewResultType.text = qrContentTypeList[0] +":"
                binding.imageViewScan.setImageResource(R.drawable.textlogo)
                return content
            }
        }
    }

    fun codigoEscaneado(dataResult: String) {
        if (dataResult.length == 37 && dataResult.endsWith("D", ignoreCase = false)) {
            firebaseFirestore.collection("Codigos").get()
                .addOnCompleteListener { task -> // Una vez cargados, los metemos en una lista
                    val correos = task.result.documents
                    var encontrado = false

                    for (i in correos.indices) {
                        // Comprobamos si el correo contiene el codigo escaneado
                        if (correos[i][dataResult] != null) {
                            // Cuando encontramos el codigo mostramos el contenido
                            binding.textViewResult.text = decodeAndLoadQRContent(
                                correos[i][dataResult].toString(),
                                correos[i].id
                            )
                            encontrado = true
                            break
                        }
                    }

                    if (!encontrado) binding.textViewResult.text =
                        decodeAndLoadQRContent(dataResult, "")
                }
        } else binding.textViewResult.text = decodeAndLoadQRContent(dataResult, "")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}