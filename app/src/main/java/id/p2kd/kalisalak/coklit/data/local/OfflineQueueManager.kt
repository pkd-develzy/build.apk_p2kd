package id.p2kd.kalisalak.coklit.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.p2kd.kalisalak.coklit.data.api.ApiService
import id.p2kd.kalisalak.coklit.data.models.OfflineQueueItem
import id.p2kd.kalisalak.coklit.data.models.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OfflineQueueManager(context: Context) {

    private val prefs = context.getSharedPreferences("p2kd_offline_queue", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val keyQueue = "queue_items_json"

    private val _itemsFlow = MutableStateFlow<List<OfflineQueueItem>>(emptyList())
    val itemsFlow: StateFlow<List<OfflineQueueItem>> = _itemsFlow.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        val json = prefs.getString(keyQueue, null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<OfflineQueueItem>>() {}.type
                val loaded: List<OfflineQueueItem> = gson.fromJson(json, type)
                _itemsFlow.value = loaded
            } catch (_: Exception) {
                _itemsFlow.value = emptyList()
            }
        } else {
            _itemsFlow.value = emptyList()
        }
    }

    private fun saveItems(items: List<OfflineQueueItem>) {
        _itemsFlow.value = items
        prefs.edit().putString(keyQueue, gson.toJson(items)).apply()
    }

    fun enqueue(item: OfflineQueueItem) {
        val current = _itemsFlow.value.toMutableList()
        // Deduplicate by idempotency key
        current.removeAll { it.idempotencyKey == item.idempotencyKey }
        current.add(0, item)
        saveItems(current)
    }

    fun updateState(localId: String, newState: SyncState, errorMsg: String? = null) {
        val current = _itemsFlow.value.map { item ->
            if (item.localId == localId) {
                item.copy(syncState = newState, errorMessage = errorMsg)
            } else {
                item
            }
        }
        saveItems(current)
    }

    fun remove(localId: String) {
        val current = _itemsFlow.value.filterNot { it.localId == localId }
        saveItems(current)
    }

    fun clearSynced() {
        val current = _itemsFlow.value.filterNot { it.syncState == SyncState.SYNCED }
        saveItems(current)
    }

    suspend fun syncAll(apiService: ApiService): Int {
        val pending = _itemsFlow.value.filter {
            it.syncState == SyncState.SAVED_LOCAL ||
            it.syncState == SyncState.PENDING_SYNC ||
            it.syncState == SyncState.ERROR
        }

        var successCount = 0

        for (item in pending) {
            updateState(item.localId, SyncState.SYNCING)
            try {
                // 1. Daftarkan / perbarui rumah
                val rumahRes = apiService.registerRumah(item.rumahData)
                if (!rumahRes.isSuccessful || rumahRes.body()?.success != true) {
                    val errMsg = rumahRes.body()?.message ?: "Gagal sinkronisasi data rumah (${rumahRes.code()})"
                    updateState(item.localId, SyncState.ERROR, errMsg)
                    continue
                }

                val rumahId = rumahRes.body()!!.rumah?.id ?: item.localId

                // 2. Hubungkan KK
                for (kkReq in item.kks) {
                    apiService.linkKk(rumahId, kkReq)
                }

                // 3. Simpan Kunjungan & Verifikasi Anggota
                val visitRes = apiService.submitVisit(rumahId, item.visitData)
                if (visitRes.isSuccessful && visitRes.body()?.success == true) {
                    updateState(item.localId, SyncState.SYNCED)
                    successCount++
                } else {
                    val errMsg = visitRes.body()?.message ?: "Gagal sinkronisasi kunjungan (${visitRes.code()})"
                    updateState(item.localId, SyncState.ERROR, errMsg)
                }
            } catch (e: Exception) {
                updateState(item.localId, SyncState.ERROR, e.message ?: "Koneksi jaringan terputus.")
            }
        }

        return successCount
    }
}
