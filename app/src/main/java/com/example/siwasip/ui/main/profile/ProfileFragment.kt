package com.example.siwasip.ui.main.profile

import android.Manifest
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.siwasip.R
import com.example.siwasip.data.local.Prefs
import com.example.siwasip.data.model.ProfileData
import com.example.siwasip.data.repository.ProfileRepository
import com.example.siwasip.ui.auth.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

class ProfileFragment : Fragment() {

    // container
    private lateinit var viewContainer: LinearLayout
    private lateinit var editContainer: LinearLayout

    // --- mode lihat ---
    private lateinit var txtUsernameValue: TextView
    private lateinit var txtEmailValue: TextView
    private lateinit var txtPasswordValue: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button

    // --- mode edit ---
    private lateinit var edtUsername: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnChangePhoto: Button

    private lateinit var imgAvatarView: ImageView

    private lateinit var imgAvatarEdit: ImageView

    private var selectedPhotoUri: Uri? = null

    private var currentProfile: ProfileData? = null
    private var isEditing: Boolean = false

    private val profileRepository: ProfileRepository by lazy {
        ProfileRepository { Prefs.authToken }
    }
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            imgAvatarEdit.setImageURI(it) // Tampilkan gambar yang dipilih
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pickImage.launch("image/*") // Lanjutkan memilih gambar jika izin diberikan
        } else {
            Toast.makeText(requireContext(), "Izin penyimpanan diperlukan untuk memilih foto.", Toast.LENGTH_SHORT).show()
        }
    }

    // ----- onCreateView -----
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // container
        viewContainer = view.findViewById(R.id.viewContainer)
        editContainer = view.findViewById(R.id.editContainer)

        // view mode
        txtUsernameValue = view.findViewById(R.id.txtUsernameValue)
        txtEmailValue = view.findViewById(R.id.txtEmailValue)
        txtPasswordValue = view.findViewById(R.id.txtPasswordValue)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnLogout = view.findViewById(R.id.btnLogout)
        imgAvatarView = view.findViewById(R.id.imgAvatarView)

        // edit mode
        edtUsername = view.findViewById(R.id.edtUsername)
        edtEmail = view.findViewById(R.id.edtEmail)
        edtPassword = view.findViewById(R.id.edtPassword)
        btnSave = view.findViewById(R.id.btnSaveProfile)
        btnCancel = view.findViewById(R.id.btnCancelProfile)
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)
        imgAvatarEdit = view.findViewById(R.id.imgAvatarEdit)

        setupListeners()
        setEditing(false) // mulai dari mode lihat

        loadProfile()

        return view
    }

    // ----- Listener tombol -----
    private fun setupListeners() {
        btnChangePhoto.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Android 13 (API 33) ke atas
                pickImage.launch("image/*")
            } else if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Android < 13 dengan izin sudah diberikan
                pickImage.launch("image/*")
            } else {
                // Android < 13, minta izin
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        btnEditProfile.setOnClickListener {
            // Masuk mode edit, isi field dari currentProfile
            val profile = currentProfile
            if (profile != null) {
                edtUsername.setText(profile.username ?: "")
                edtEmail.setText(profile.email ?: "")
            } else {
                edtUsername.setText(txtUsernameValue.text.toString().removePrefix("@"))
                edtEmail.setText(txtEmailValue.text.toString())
            }
            edtPassword.setText("")
            setEditing(true)
        }

        btnLogout.setOnClickListener {
            confirmLogout()
        }

        btnSave.setOnClickListener {
            confirmSave()
        }

        btnCancel.setOnClickListener {
            // Balik ke data awal
            currentProfile?.let { bindToEdit(it) }
            setEditing(false)
        }
    }

    // ----- Toggle mode -----
    private fun setEditing(edit: Boolean) {
        isEditing = edit
        if (edit) {
            viewContainer.visibility = View.GONE
            editContainer.visibility = View.VISIBLE
            selectedPhotoUri = null
        } else {
            viewContainer.visibility = View.VISIBLE
            editContainer.visibility = View.GONE
            edtPassword.setText("")
        }
    }

    //   Load profile
    private fun loadProfile() {
        val token = Prefs.authToken
        if (token.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                "Session habis, silakan login ulang.",
                Toast.LENGTH_LONG
            ).show()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    profileRepository.getProfile()
                } catch (e: Exception) {
                    null
                }
            }

            if (result == null) {
                Toast.makeText(
                    requireContext(),
                    "Gagal memuat profil.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                currentProfile = result
                bindProfile(result)
            }
        }
    }

    // Update tampilan view-mode dan edit-mode
    private fun bindProfile(profile: ProfileData) {
        // view mode
        val username = profile.username ?: ""
        val email = profile.email ?: ""

        txtUsernameValue.text =
            if (username.isNotBlank()) "@$username" else "@username"
        txtEmailValue.text =
            if (email.isNotBlank()) email else "username@gmail.com"
        txtPasswordValue.text = "********************"

        profile.photo_url?.let { url ->
            Glide.with(this).load(url).placeholder(R.drawable.ic_profile_active).error(R.drawable.ic_profile_active).circleCrop().into(imgAvatarView)
            Glide.with(this).load(url).placeholder(R.drawable.ic_profile_active).error(R.drawable.ic_profile_active).circleCrop().into(imgAvatarEdit)
        } ?: run {
            imgAvatarView.setImageResource(R.drawable.ic_profile_active)
            imgAvatarEdit.setImageResource(R.drawable.ic_profile_active)
        }

        // edit mode
        bindToEdit(profile)
    }

    private fun bindToEdit(profile: ProfileData) {
        edtUsername.setText(profile.username ?: "")
        edtEmail.setText(profile.email ?: "")
        edtPassword.setText("")
        edtPassword.hint = "********************"

        profile.photo_url?.let { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_profile_active)
                .error(R.drawable.ic_profile_active)
                .circleCrop()
                .into(imgAvatarEdit)
        } ?: run {
            imgAvatarEdit.setImageResource(R.drawable.ic_profile_active)
        }
    }

    //   Simpan profile

    private fun confirmSave() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Simpan perubahan?")
            .setNegativeButton("Tidak", null)
            .setPositiveButton("Ya") { _, _ ->
                performSave()
            }
            .show()
    }

    private fun performSave() {
        val username = edtUsername.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString()

        if (username.isBlank() || email.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Username dan email wajib diisi.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Menyimpan..."

        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    // BARU: Dapatkan file dari selectedPhotoUri
                    val imageFile: File? = selectedPhotoUri?.let { uri ->
                        uri.getPath(requireContext())?.let { File(it) }
                    }

                    val res = profileRepository.updateProfile(
                        username = username,
                        email = email,
                        password = password.takeIf { it.isNotBlank() },
                        imageFile = imageFile // BARU: Kirim file avatar
                    )
                    res.success == true
                } catch (e: Exception) {
                    e.printStackTrace() // Log error untuk debugging
                    false
                }
            }

            btnSave.isEnabled = true
            btnSave.text = "Simpan"

            if (ok) {
                Toast.makeText(
                    requireContext(),
                    "Profil berhasil diperbarui.",
                    Toast.LENGTH_SHORT
                ).show()
                // refresh dari server biar sinkron
                loadProfile()
                setEditing(false)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Gagal memperbarui profil.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Logout
    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Yakin ingin logout dari aplikasi?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Ya") { _, _ ->
                doLogout()
            }
            .show()
    }

    private fun doLogout() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    profileRepository.logout()
                } catch (_: Exception) {

                }
            }

            // clear token lokal
            Prefs.authToken = null

            Toast.makeText(
                requireContext(),
                "Berhasil logout.",
                Toast.LENGTH_SHORT
            ).show()

            // arahkan ke LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    fun Uri.getPath(context: Context): String? {
        var result: String? = null
        if (ContentResolver.SCHEME_FILE == scheme) {
            result = path
        } else if (ContentResolver.SCHEME_CONTENT == scheme) {
            val cursor = context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    // Anda mungkin perlu logika yang lebih canggih untuk mendapatkan path absolut
                    // Jika ini tidak cukup, Anda bisa menyimpan file ke cache dan mendapatkan path-nya
                    val cacheDir = context.cacheDir
                    val file = File(cacheDir, fileName)
                    try {
                        context.contentResolver.openInputStream(this)?.use { inputStream ->
                            file.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        result = file.absolutePath
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return result
    }
}