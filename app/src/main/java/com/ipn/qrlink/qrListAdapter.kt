package com.ipn.qrlink

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.net.MailTo
import org.w3c.dom.Text

class qrListAdapter(private val context: Context, private val dataSource: ArrayList<qrCode>) :
    BaseAdapter() {

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @NonNull
    override fun getView(
        position: Int,
        @Nullable convertView: View?,
        @NonNull parent: ViewGroup?
    ): View? {
        val rowView = LayoutInflater.from(context).inflate(R.layout.item_qr, parent, false)

        val qrCode = getItem(position) as qrCode

        val textViewQRContentTypeTitle = rowView.findViewById<TextView>(R.id.textViewQRContentType)
        val positionNumberTextView = rowView.findViewById<TextView>(R.id.positionTextView)
        val contentTextView = rowView.findViewById<TextView>(R.id.contentTextView)

        val imageViewQRContent = rowView.findViewById<ImageView>(R.id.imageViewQRContent)
        val imageViewQRType = rowView.findViewById<ImageView>(R.id.imageViewQRType)

        textViewQRContentTypeTitle.text = getQRContentType(qrCode.content)
        positionNumberTextView.text = "#" + (position + 1)
        contentTextView.text = decodeQRContent(qrCode.content)
        imageViewQRContent.setImageResource(getQRContentImage(qrCode.content))
        imageViewQRType.setImageResource(getQRTypeImage(qrCode.id))

        return rowView
    }

    private fun decodeQRContent(content: String) : String {
        when {
            content.startsWith("mailto:", ignoreCase = true) -> {
                val decodedMail = MailTo.parse(content)

                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:") // Solo las apps de email soportan esto

                return "Para: " + decodedMail.to + "\nAsunto: "+decodedMail.subject+"\nMensaje: "+decodedMail.body
            }
            content.startsWith("tel:", ignoreCase = true) -> {
                val decodedPhone = content.split(":")
                val phoneNumber = decodedPhone[1]

                return "Para: $phoneNumber"
            }
            content.startsWith("smsto:", ignoreCase = true) -> {
                val decodedMessage = content.split(":")
                val phoneNumber = decodedMessage[1]
                val messageContent = decodedMessage[2]

                return "Para: $phoneNumber\nContenido: $messageContent"
            }
            content.startsWith("WIFI:S:", ignoreCase = true) -> {
                val decodedWIFI = content.split(":")
                val ssid = decodedWIFI[2].split(";")[0]
                val password = decodedWIFI[4].split(";")[0]

                return "Nombre de red: $ssid\nContrasena: *******"
            }
            content.startsWith("BEGIN:VEVENT\nSUMMARY:", ignoreCase = true) -> {
                val decodedEvent = content.split("\n")
                val parseFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss")
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm")

                val title = decodedEvent[1].split(":")[1]
                val location = decodedEvent[2].split(":")[1]
                val start = dateFormatter.format(parseFormatter.parse(decodedEvent[3].split(":")[1]))
                val end = dateFormatter.format(parseFormatter.parse(decodedEvent[4].split(":")[1]))

                return "Nombre: $title\nUbicacion: $location\nFecha de inicio: $start\nFinal: $end"
            }
            else -> {
                return content
            }
        }
    }

    private fun getQRContentType(content: String) : String {
        return when {
            content.startsWith("mailto:", ignoreCase = true) -> "Correo eletronico"
            content.startsWith("tel:", ignoreCase = true) -> "Llamada"
            content.startsWith("smsto:", ignoreCase = true) -> "Mensaje de texto"
            content.startsWith("WIFI:S:", ignoreCase = true) -> "Red Wifi"
            content.startsWith("BEGIN:VEVENT\nSUMMARY:", ignoreCase = true) -> "Evento"
            content.endsWith(".pdf",ignoreCase = true) -> "PDF"
            URLUtil.isValidUrl(content) -> "Sitio Web"
            else -> "Contenido"
        }
    }

    private fun getQRContentImage(content: String) : Int {
       return when  {
            content.startsWith("mailto:", ignoreCase = true) -> R.drawable.maillogo
            content.startsWith("tel:", ignoreCase = true) -> R.drawable.phonelogo
            content.startsWith("smsto:", ignoreCase = true) -> R.drawable.smslogo
            content.startsWith("WIFI:S:", ignoreCase = true) -> R.drawable.wifilogo
            content.startsWith("BEGIN:VEVENT\nSUMMARY:", ignoreCase = true) -> R.drawable.eventlogo
           content.endsWith(".pdf",ignoreCase = true) -> R.drawable.pdflogo
            URLUtil.isValidUrl(content) -> R.drawable.networklogo
            else -> R.drawable.textlogo
        }
    }

    private fun getQRTypeImage(content: String) : Int {
        return when  {
            content.endsWith("D", ignoreCase = true) -> R.drawable.dynamiclogo
            else -> R.drawable.staticlogo
        }
    }
}