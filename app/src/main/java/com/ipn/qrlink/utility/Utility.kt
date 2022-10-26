package com.ipn.qrlink.utility

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class Utility {
    fun getDocumentURI(pdfName: String, userEmail: String, context: Context): String {
        //  Variable para guardar el estado de si la imagen se guardo o no
        val guardada: Boolean
        val finalFile: File

        val resolver = context.contentResolver
        try {
            // Si el dispositivo es android 9 o superior
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < 30) {

                val contentValues = ContentValues()

                // Guardamos la imagen en el destino DCIM/QR, imagen de tipo .png con el nombre de "QR - " + las 5 primeras letras del QR
                contentValues.put(
                    MediaStore.DownloadColumns.DISPLAY_NAME,
                    pdfName
                )
                contentValues.put(MediaStore.DownloadColumns.MIME_TYPE, "document/pdf")
                contentValues.put(MediaStore.DownloadColumns.RELATIVE_PATH, "Documents/QRLink/Cache/PDFs/$userEmail")

                // Insertamos la imagen en los archivos a escribir
                val documentUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                finalFile = documentUri!!.toFile()
                return finalFile.toString()
            } else {
                // El proceso a seguir es el mismo pero de diferente manera por la version de android
                val directorioURI = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS +"/QRLink/Cache/PDFs/$userEmail"
                )

                val directorioDocumento = directorioURI.toString()
                val documento = File(directorioDocumento, pdfName)
                finalFile = documento
                return finalFile.toString()
            }
        } catch (e: IOException) {
            // Si tenemos algun error lo mostramos
            Toast.makeText(context, "Error al leer: $e", Toast.LENGTH_SHORT).show()
            return ""
        }
    }
    fun deleteDocument(pdfName: String, userEmail: String, context: Context) {
        //  Variable para guardar el estado de si la imagen se guardo o no
        val guardada: Boolean
        val finalFile: File

        val resolver = context.contentResolver
        try {
            // Si el dispositivo es android 9 o superior
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < 30) {

                val contentValues = ContentValues()

                // Guardamos la imagen en el destino DCIM/QR, imagen de tipo .png con el nombre de "QR - " + las 5 primeras letras del QR
                contentValues.put(
                    MediaStore.DownloadColumns.DISPLAY_NAME,
                    pdfName
                )
                contentValues.put(MediaStore.DownloadColumns.MIME_TYPE, "document/pdf")
                contentValues.put(MediaStore.DownloadColumns.RELATIVE_PATH, "Documents/QRLink/Cache/PDFs/$userEmail")

                // Insertamos la imagen en los archivos a escribir
                val documentUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                finalFile = documentUri!!.toFile()
               finalFile.delete()
            } else {
                // El proceso a seguir es el mismo pero de diferente manera por la version de android
                val directorioURI = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS +"/QRLink/Cache/PDFs/$userEmail"
                )

                val directorioDocumento = directorioURI.toString()
                val documento = File(directorioDocumento, pdfName)
                finalFile = documento
                finalFile.delete()
            }
        } catch (e: IOException) {
            // Si tenemos algun error lo mostramos
            Toast.makeText(context, "Error al eliminar: $e", Toast.LENGTH_SHORT).show()
        }
    }
    fun downloadDocument(pdfName: String, userEmail: String,context: Context) {
        val storageRef = Firebase.storage.reference
        val reference = storageRef.child("PDFs/"+ userEmail+"/$pdfName")

        //  Variable para guardar el estado de si la imagen se guardo o no
        val guardada: Boolean
        val finalFile: File

        // OutputStream para escribir/guardar la imagen
        val writer: OutputStream?
        try {
            // Si el dispositivo es android 9 o superior
            writer =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < 30) {
                val resolver = context.contentResolver
                val contentValues = ContentValues()

                // Guardamos la imagen en el destino DCIM/QR, imagen de tipo .png con el nombre de "QR - " + las 5 primeras letras del QR
                contentValues.put(
                    MediaStore.DownloadColumns.DISPLAY_NAME,
                    pdfName
                )
                contentValues.put(MediaStore.DownloadColumns.MIME_TYPE, "document/pdf")
                contentValues.put(MediaStore.DownloadColumns.RELATIVE_PATH, "Documents/QRLink/Cache/PDFs/$userEmail")

                // Insertamos la imagen en los archivos a escribir
                    val documentUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                finalFile = documentUri!!.toFile()

                if (!finalFile.exists()) {
                    finalFile.mkdirs();
                }

                resolver.openOutputStream(documentUri)
            } else {
                // El proceso a seguir es el mismo pero de diferente manera por la version de android
                val directorioURI = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS +"/QRLink/Cache/PDFs/$userEmail"
                )

                if (!directorioURI.exists()) {
                    directorioURI.mkdirs();
                }

                val directorioDocumento = directorioURI.toString()
                val documento = File(directorioDocumento, pdfName)
                finalFile = documento
                FileOutputStream(documento)
            }

            reference.getFile(finalFile).addOnFailureListener {
                Toast.makeText(context, "Ocurrio un error al cachear el documento $pdfName", Toast.LENGTH_SHORT).show()
            }

            // Al finalizar limpiamos y cerramos el stream
            writer!!.flush()
            writer.close()
        } catch (e: IOException) {
            // Si tenemos algun error lo mostramos
            Toast.makeText(context, "Error al guardar: $e", Toast.LENGTH_SHORT).show()
        }
    }
}