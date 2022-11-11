package com.ipn.qrlink

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.MailTo
import java.net.URLDecoder

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

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View? {
        val rowView =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.item_qr, parent, false)

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

    private fun decodeQRContent(encodedContent: String): String {
        val content = URLDecoder.decode(encodedContent)
        when {
            content.startsWith("mailto:", ignoreCase = true) -> {
                val decodedMail = MailTo.parse(content)

                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:") // Solo las apps de email soportan esto

                return "Para: " + decodedMail.to + "\nAsunto: " + decodedMail.subject + "\nMensaje: " + decodedMail.body
            }
            content.startsWith("tel:", ignoreCase = true) -> {
                val decodedPhone = content.split(":")
                val phoneNumber = decodedPhone[1]

                return "Numero: $phoneNumber"
            }
            content.startsWith("smsto:", ignoreCase = true) -> {
                val decodedMessage = content.split(":")
                val phoneNumber = decodedMessage[1]
                val messageContent = decodedMessage[2]

                return "Numero: $phoneNumber\nMensaje: $messageContent"
            }
            content.startsWith("WIFI:S:", ignoreCase = true) -> {
                val decodedWIFI = content.split(":")
                val ssid = decodedWIFI[2].split(";")[0]
                val password = decodedWIFI[4].split(";")[0]

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

                return "Titulo: $title\nUbicación: $location\nFecha de inicio: $start\nFinal: $end"
            }
            else -> {
                return content
            }
        }
    }

    private fun getQRContentType(encodedContent: String): String {
        val content = URLDecoder.decode(encodedContent)
        return when {
            content.startsWith("mailto:", ignoreCase = true) -> "Correo eletrónico"
            content.startsWith("tel:", ignoreCase = true) -> "Numero telefónico"
            content.startsWith("smsto:", ignoreCase = true) -> "Mensaje de texto"
            content.startsWith("WIFI:S:", ignoreCase = true) -> "Red Wi-Fi"
            content.startsWith("BEGIN:VEVENT\nSUMMARY:", ignoreCase = true) -> "Evento"
            content.endsWith(".pdf", ignoreCase = true) -> "PDF"
            URLUtil.isValidUrl(content) -> "Sitio Web"
            else -> "Texto"
        }
    }

    private fun getQRContentImage(encodedContent: String): Int {
        val content = URLDecoder.decode(encodedContent)
        return when {
            content.startsWith("mailto:", ignoreCase = true) -> R.drawable.maillogo
            content.startsWith("tel:", ignoreCase = true) -> R.drawable.phonelogo
            content.startsWith("smsto:", ignoreCase = true) -> R.drawable.smslogo
            content.startsWith("WIFI:S:", ignoreCase = true) -> R.drawable.wifilogo
            content.startsWith("BEGIN:VEVENT\nSUMMARY:", ignoreCase = true) -> R.drawable.eventlogo
            content.endsWith(".pdf", ignoreCase = true) -> R.drawable.pdflogo
            URLUtil.isValidUrl(content) -> R.drawable.networklogo
            else -> R.drawable.textlogo
        }
    }

    private fun getQRTypeImage(content: String): Int {
        return when {
            content.endsWith("D", ignoreCase = true) -> R.drawable.dynamiclogo
            else -> R.drawable.staticlogo
        }
    }
}