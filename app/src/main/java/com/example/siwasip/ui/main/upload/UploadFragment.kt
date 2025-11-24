package com.example.siwasip.ui.main.upload

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.siwasip.R
import com.example.siwasip.data.local.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadFragment : Fragment() {

    private lateinit var btnChooseFile: Button
    private lateinit var txtFileName: TextView
    private lateinit var edtTitle: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnUpload: Button
    private lateinit var btnCancel: Button

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    // --- Retrofit ---
    private interface UploadApi {
        @Multipart
        @POST("documents")
        suspend fun uploadDocument(
            @Part file: MultipartBody.Part,
            @Part("title") title: RequestBody,
            @Part("description") description: RequestBody?,
            @Part("uploaded_at") uploadedAt: RequestBody
        ): Response<ResponseBody>
    }

    private val uploadApi: UploadApi by lazy {
        createUploadApi()
    }

    private fun createUploadApi(): UploadApi {
        val token = Prefs.authToken

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("Accept", "application/json")

                if (!token.isNullOrBlank()) {
                    builder.header("Authorization", "Bearer $token")
                }

                chain.proceed(builder.build())
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://siwasis.novarentech.web.id/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(UploadApi::class.java)
    }

    // File picker
    private val pickDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedFileUri = uri
                selectedFileName = queryFileName(uri) ?: "dokumen.pdf"
                btnChooseFile.text = selectedFileName
                btnChooseFile.setTextColor(resources.getColor(android.R.color.black, null))
                txtFileName.text = ""
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_upload, container, false)

        btnChooseFile = view.findViewById(R.id.btnChooseFile)
        txtFileName = view.findViewById(R.id.txtFileName)
        edtTitle = view.findViewById(R.id.edtTitle)
        edtDescription = view.findViewById(R.id.edtDescription)
        btnUpload = view.findViewById(R.id.btnUpload)
        btnCancel = view.findViewById(R.id.btnCancel)

        setupListeners()

        return view
    }

    private fun setupListeners() {
        btnChooseFile.setOnClickListener {
            // Hanya izinkan PDF dan dokumen office
            pickDocumentLauncher.launch(
                arrayOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
        }

        btnUpload.setOnClickListener {
            handleUpload()
        }

        btnCancel.setOnClickListener {
            // reset form saja
            resetForm()
        }
    }

    private fun handleUpload() {
        val uri = selectedFileUri
        val title = edtTitle.text.toString().trim()
        val desc = edtDescription.text.toString().trim()

        if (uri == null) {
            Toast.makeText(requireContext(), "Silakan pilih dokumen terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Judul dokumen wajib diisi.", Toast.LENGTH_SHORT).show()
            return
        }

        val token = Prefs.authToken
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Token login tidak ditemukan. Silakan login ulang.", Toast.LENGTH_SHORT).show()
            return
        }

        btnUpload.isEnabled = false
        btnUpload.text = "Mengunggah..."

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    uploadDocumentToServer(uri, title, desc)
                } catch (e: Exception) {
                    e
                }
            }

            btnUpload.isEnabled = true
            btnUpload.text = "Unggah Dokumen"

            when (result) {
                is Exception -> {
                    Toast.makeText(
                        requireContext(),
                        "Gagal mengunggah: ${result.message ?: "Terjadi kesalahan"}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is Boolean -> {
                    if (result) {
                        Toast.makeText(requireContext(), "Dokumen berhasil diunggah.", Toast.LENGTH_SHORT).show()
                        resetForm()
                    } else {
                        Toast.makeText(requireContext(), "Upload gagal. Coba lagi.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private suspend fun uploadDocumentToServer(
        uri: Uri,
        title: String,
        description: String
    ): Boolean {
        // Salin konten URI ke file sementara
        val cacheFile = copyUriToCache(uri) ?: return false

        val mimeType = requireContext().contentResolver.getType(uri)
            ?: "application/octet-stream"

        val requestFile = cacheFile
            .asRequestBody(mimeType.toMediaTypeOrNull())

        val filePart = MultipartBody.Part.createFormData(
            "file_path",
            selectedFileName ?: cacheFile.name,
            requestFile
        )

        val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val descBody =
            if (description.isNotBlank()) description.toRequestBody("text/plain".toMediaTypeOrNull())
            else null

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val uploadedAtBody = today.toRequestBody("text/plain".toMediaTypeOrNull())

        val response = uploadApi.uploadDocument(
            file = filePart,
            title = titleBody,
            description = descBody,
            uploadedAt = uploadedAtBody
        )

        return response.isSuccessful
    }

    private fun copyUriToCache(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val fileName = selectedFileName ?: "dokumen_upload.tmp"
            val tempFile = File(requireContext().cacheDir, fileName)

            FileOutputStream(tempFile).use { out ->
                inputStream.use { input ->
                    input.copyTo(out)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun queryFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        ) ?: return null

        cursor.use {
            val nameIndex = it.getColumnIndex("_display_name")
            if (nameIndex >= 0 && it.moveToFirst()) {
                return it.getString(nameIndex)
            }
        }
        return null
    }

    private fun resetForm() {
        selectedFileUri = null
        selectedFileName = null
        btnChooseFile.text = "Pilih"
        btnChooseFile.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        txtFileName.text = ""
        edtTitle.text?.clear()
        edtDescription.text?.clear()
    }
}