package com.example.siwasip.ui.main.home

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.siwasip.R
import com.example.siwasip.data.local.Prefs
import com.example.siwasip.data.model.Document
import com.example.siwasip.data.repository.DocumentRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var txtTotalValue: TextView
    private lateinit var txtLastValue: TextView
    private lateinit var layoutRows: LinearLayout
    private lateinit var txtEmpty: TextView

    private val repo by lazy {
        DocumentRepository { Prefs.authToken }
    }

    private val apiStorageBase = "https://siwasis.novarentech.web.id/storage/"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtTotalValue = view.findViewById(R.id.txtTotalValue)
        txtLastValue = view.findViewById(R.id.txtLastValue)
        layoutRows = view.findViewById(R.id.layoutRows)
        txtEmpty = view.findViewById(R.id.txtEmpty)

        loadDocuments()
    }

    private fun loadDocuments() {
        lifecycleScope.launch {
            try {
                // page 1, 50 dok max
                val resp = repo.getDocuments(page = 1, perPage = 50)
                val docs = resp.data

                // KPI total
                val total = resp.pagination?.total ?: docs.size
                txtTotalValue.text = "$total dokumen"

                // KPI terakhir diunggah
                val last = docs.maxByOrNull { it.uploaded_at.orEmpty() }
                txtLastValue.text = last?.title ?: "-"

                renderRows(docs)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal memuat dokumen: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderRows(docs: List<Document>) {
        layoutRows.removeAllViews()

        if (docs.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            return
        } else {
            txtEmpty.visibility = View.GONE
        }

        val inflater = LayoutInflater.from(requireContext())
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))

        docs.forEachIndexed { index, doc ->
            val row = inflater.inflate(R.layout.item_document_row, layoutRows, false)

            val txtNumber = row.findViewById<TextView>(R.id.txtRowNumber)
            val txtTitle = row.findViewById<TextView>(R.id.txtRowTitle)
            val txtDate = row.findViewById<TextView>(R.id.txtRowDate)
            val txtDesc = row.findViewById<TextView>(R.id.txtRowDescription)
            val btnView = row.findViewById<View>(R.id.btnView)
            val btnDelete = row.findViewById<View>(R.id.btnDelete)
            val btnDownload = row.findViewById<View>(R.id.btnDownload)

            // No. baris
            txtNumber.text = (index + 1).toString()

            txtTitle.text = doc.title ?: doc.filename ?: "(Tanpa judul)"
            txtDesc.text = doc.description?.ifBlank { "-" } ?: "-"

            val dateRaw = doc.uploaded_at
            val prettyDate = try {
                if (!dateRaw.isNullOrBlank()) {
                    val parsed = inputFormat.parse(dateRaw)
                    if (parsed != null) outputFormat.format(parsed) else "-"
                } else {
                    "-"
                }
            } catch (_: Exception) {
                "-"
            }
            txtDate.text = prettyDate

            // Action view
            btnView.setOnClickListener {
                val url = buildFileUrl(doc.file_path)
                if (url == null) {
                    Toast.makeText(requireContext(), "File tidak tersedia", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val intent = Intent(requireContext(), DocumentDetailActivity::class.java).apply {
                    putExtra(DocumentDetailActivity.EXTRA_TITLE, txtTitle.text.toString())
                    putExtra(DocumentDetailActivity.EXTRA_DATE, prettyDate)
                    putExtra(DocumentDetailActivity.EXTRA_URL, url)
                }
                startActivity(intent)
            }

            // Action delete
            btnDelete.setOnClickListener {
                if (doc.id == null) {
                    Toast.makeText(requireContext(), "ID dokumen tidak valid", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Konfirmasi")
                    .setMessage("Yakin ingin hapus dokumen?")
                    .setNegativeButton("Tidak", null)
                    .setPositiveButton("Ya") { _, _ ->
                        deleteDocument(doc.id)
                    }
                    .show()
            }

            // Action download
            btnDownload.setOnClickListener {
                val url = buildFileUrl(doc.file_path)
                if (url == null) {
                    Toast.makeText(requireContext(), "File tidak tersedia", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Konfirmasi")
                    .setMessage("Ingin mengunduh dokumen?")
                    .setNegativeButton("Tidak", null)
                    .setPositiveButton("Ya") { _, _ ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(url)
                        }
                        startActivity(intent)
                    }
                    .show()
            }

            layoutRows.addView(row)
        }
    }

    private fun deleteDocument(id: Int) {
        lifecycleScope.launch {
            try {
                repo.deleteDocument(id)
                Toast.makeText(requireContext(), "Dokumen dihapus.", Toast.LENGTH_SHORT).show()
                loadDocuments()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildFileUrl(filePath: String?): String? {
        if (filePath.isNullOrBlank()) return null
        val encoded = filePath
            .split("/")
            .joinToString("/") { Uri.encode(it) }
        return apiStorageBase + encoded
    }
}