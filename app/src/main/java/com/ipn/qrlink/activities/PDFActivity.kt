package com.ipn.qrlink.activities

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.ipn.qrlink.databinding.ActivityPdfBinding

class PDFActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfBinding
    private lateinit var pdfView : PDFView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pdf = intent.extras!!.getString("pdf")

        if (pdf?.startsWith("hola:") == true) {
            val data = pdf.split(":")
            val storageRef = Firebase.storage.reference
            val reference = storageRef.child(data[1])

            val ONE_MEGABYTE: Long = 1024 * 1024 * 5
            reference.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
                val inputData = bytes

                pdfView = binding.pdfView
                pdfView.fromBytes(inputData).load()
            }.addOnFailureListener {
                // Handle any errors
            }
        } else {
            val pdfUri = Uri.parse(pdf)
            val inputData = contentResolver.openInputStream(pdfUri)?.readBytes()

            pdfView = binding.pdfView
            pdfView.fromBytes(inputData).load()
        }
    }
}