package com.mobilechor.guardian.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mobilechor.guardian.data.model.EvidenceRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class EvidenceRepository(private val context: Context) {

    private val gson = Gson()
    private val prefs = context.getSharedPreferences("mobile_chor_evidence", Context.MODE_PRIVATE)
    private val _evidenceList = MutableStateFlow<List<EvidenceRecord>>(emptyList())
    val evidenceList: StateFlow<List<EvidenceRecord>> = _evidenceList.asStateFlow()

    init {
        loadEvidence()
    }

    private fun loadEvidence() {
        val json = prefs.getString("evidence_records", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<EvidenceRecord>>() {}.type
            try {
                val list: List<EvidenceRecord> = gson.fromJson(json, type)
                _evidenceList.value = list.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                _evidenceList.value = emptyList()
            }
        }
    }

    @Synchronized
    fun addEvidence(record: EvidenceRecord) {
        val current = _evidenceList.value.toMutableList()
        current.add(0, record)
        _evidenceList.value = current
        saveEvidence(current)
    }

    @Synchronized
    fun deleteEvidence(recordId: String) {
        val current = _evidenceList.value.toMutableList()
        val record = current.find { it.id == recordId }
        if (record != null) {
            // Delete photo file if present
            if (record.photoPath.isNotEmpty()) {
                val file = File(record.photoPath)
                if (file.exists()) {
                    file.delete()
                }
            }
            current.remove(record)
            _evidenceList.value = current
            saveEvidence(current)
        }
    }

    private fun saveEvidence(list: List<EvidenceRecord>) {
        val json = gson.toJson(list)
        prefs.edit().putString("evidence_records", json).apply()
    }

    fun getEvidenceDirectory(): File {
        val dir = File(context.filesDir, "evidence_vault")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
