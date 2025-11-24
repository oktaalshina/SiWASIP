package com.example.siwasip.ui.main.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.siwasip.R
import com.example.siwasip.data.local.Prefs
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

// ----- Data dari /profile -----
data class ProfileData(
    val id: Int?,
    val name: String?,
    val username: String?,
    val email: String?
)

// ----- Retrofit API -----
private interface ProfileApi {
    @GET("profile")
    suspend fun getProfile(): Response<ProfileData>

    @Multipart
    @POST("profile")
    suspend fun updateProfile(
        @Part("_method") method: RequestBody,
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody?,
        @Part("password_confirmation") passwordConfirmation: RequestBody?
    ): Response<ResponseBody>

    @POST("logout")
    suspend fun logout(): Response<ResponseBody>
}

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

    private var currentProfile: ProfileData? = null
    private var isEditing: Boolean = false

    private val api: ProfileApi by lazy { createApi() }

    // Retrofit
    private fun createApi(): ProfileApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("Accept", "application/json")

                val token = Prefs.authToken
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

        return retrofit.create(ProfileApi::class.java)
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

        // edit mode
        edtUsername = view.findViewById(R.id.edtUsername)
        edtEmail = view.findViewById(R.id.edtEmail)
        edtPassword = view.findViewById(R.id.edtPassword)
        btnSave = view.findViewById(R.id.btnSaveProfile)
        btnCancel = view.findViewById(R.id.btnCancelProfile)
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)

        setupListeners()
        setEditing(false) // mulai dari mode lihat

        loadProfile()

        return view
    }

    // ----- Listener tombol -----
    private fun setupListeners() {
        btnChangePhoto.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Ganti foto belum diimplementasikan.",
                Toast.LENGTH_SHORT
            ).show()
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
            return
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val res = api.getProfile()
                    if (res.isSuccessful) res.body() else null
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

        // edit mode
        bindToEdit(profile)
    }

    private fun bindToEdit(profile: ProfileData) {
        edtUsername.setText(profile.username ?: "")
        edtEmail.setText(profile.email ?: "")
        edtPassword.setText("")
        edtPassword.hint = "********************"
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
                    val textPlain = "text/plain".toMediaTypeOrNull()

                    val methodBody = "PUT".toRequestBody(textPlain)
                    val usernameBody = username.toRequestBody(textPlain)
                    val emailBody = email.toRequestBody(textPlain)

                    val passwordBody: RequestBody?
                    val passwordConfBody: RequestBody?
                    if (password.isNotBlank()) {
                        val pw = password.toRequestBody(textPlain)
                        passwordBody = pw
                        passwordConfBody = pw
                    } else {
                        passwordBody = null
                        passwordConfBody = null
                    }

                    val res = api.updateProfile(
                        method = methodBody,
                        username = usernameBody,
                        email = emailBody,
                        password = passwordBody,
                        passwordConfirmation = passwordConfBody
                    )

                    res.isSuccessful
                } catch (e: Exception) {
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
                    api.logout()
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
}