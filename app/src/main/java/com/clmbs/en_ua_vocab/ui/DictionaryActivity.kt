package com.clmbs.en_ua_vocab.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clmbs.en_ua_vocab.R
import com.clmbs.en_ua_vocab.data.DictionaryInfo
import com.clmbs.en_ua_vocab.data.WordRepository
import com.clmbs.en_ua_vocab.service.VocabService
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DictionaryActivity : AppCompatActivity() {

    private lateinit var repository: WordRepository
    private lateinit var adapter: DictionaryAdapter
    private lateinit var rvDictionaries: RecyclerView

    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                importFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictionary)

        repository = WordRepository(this)
        rvDictionaries = findViewById(R.id.rvDictionaries)
        rvDictionaries.layoutManager = LinearLayoutManager(this)
        
        adapter = DictionaryAdapter(
            repository.getDictionaries(),
            onToggle = { dict, isEnabled -> handleToggle(dict, isEnabled) },
            onDelete = { dict -> showDeleteConfirmation(dict) }
        )
        rvDictionaries.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabImport).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            importLauncher.launch(intent)
        }
    }

    private fun handleToggle(dict: DictionaryInfo, isEnabled: Boolean) {
        val enabledDicts = adapter.items.filter { it.isEnabled }
        if (!isEnabled && enabledDicts.size <= 1 && dict.isEnabled) {
            Toast.makeText(this, "Повинен бути увімкнений принаймні один словник", Toast.LENGTH_SHORT).show()
            adapter.refresh(repository.getDictionaries())
            return
        }
        repository.toggleDictionary(dict.id, isEnabled)
        adapter.refresh(repository.getDictionaries())
        notifyService()
    }

    private fun showDeleteConfirmation(dict: DictionaryInfo) {
        AlertDialog.Builder(this)
            .setTitle("Видалити словник?")
            .setMessage("Ви впевнені, що хочете видалити '${dict.name}'?")
            .setPositiveButton("Видалити") { _, _ ->
                repository.deleteDictionary(dict.id)
                adapter.refresh(repository.getDictionaries())
                notifyService()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun importFile(uri: Uri) {
        var fileName = "imported_${System.currentTimeMillis()}.json"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        if (repository.importDictionary(uri, fileName)) {
            Toast.makeText(this, "Імпортовано: $fileName", Toast.LENGTH_SHORT).show()
            adapter.refresh(repository.getDictionaries())
            notifyService()
        } else {
            Toast.makeText(this, "Помилка імпорту", Toast.LENGTH_SHORT).show()
        }
    }

    private fun notifyService() {
        val intent = Intent(this, VocabService::class.java).apply {
            action = VocabService.ACTION_RELOAD
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

class DictionaryAdapter(
    var items: List<DictionaryInfo>,
    private val onToggle: (DictionaryInfo, Boolean) -> Unit,
    private val onDelete: (DictionaryInfo) -> Unit
) : RecyclerView.Adapter<DictionaryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DictionaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dictionary, parent, false)
        return DictionaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DictionaryViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvType.text = if (item.isBuiltIn) "Вбудований" else "Локальний файл"
        
        holder.btnDelete.visibility = if (item.isBuiltIn) View.GONE else View.VISIBLE
        holder.btnDelete.setOnClickListener { onDelete(item) }

        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = item.isEnabled
        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            onToggle(item, isChecked)
        }
    }

    override fun getItemCount() = items.size

    fun refresh(newItems: List<DictionaryInfo>) {
        items = newItems
        notifyDataSetChanged()
    }
}

class DictionaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tvName: TextView = view.findViewById(R.id.tvDictName)
    val tvType: TextView = view.findViewById(R.id.tvDictType)
    val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    val switch: SwitchCompat = view.findViewById(R.id.switchEnable)
}
