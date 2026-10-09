package id.p2kd.kalisalak.coklit.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.p2kd.kalisalak.coklit.data.api.ApiService
import id.p2kd.kalisalak.coklit.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

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

    fun getQueue(): List<OfflineQueueItem> {
        return _itemsFlow.value
    }

    fun enqueue(item: OfflineQueueItem) {
        val current = _itemsFlow.value.toMutableList()
        current.removeAll { it.idempotencyKey == item.idempotencyKey }
        current.add(0, item)
        saveItems(current)
    }

    fun enqueue(rumahId: String, payload: SubmitVisitRequest) {
        val item = OfflineQueueItem(
            localId = rumahId,
            idempotencyKey = payload.idempotencyKey ?: UUID.randomUUID().toString(),
            qrToken = payload.qrToken,
            rumahData = RumahRequest(
                qrToken = payload.qrToken,
                alamat = "Offline Record",
                rt = "01",
                rw = "01"
            ),
            kks = emptyList(),
            visitData = payload,
            syncState = SyncState.PENDING_SYNC
        )
        enqueue(item)
    }

    fun updateStatus(id: String, statusStr: String, errorMsg: String? = null) {
        val syncState = try {
            SyncState.valueOf(statusStr)
        } catch (_: Exception) {
            SyncState.ERROR
        }
        updateState(id, syncState, errorMsg)
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
                val rumahRes = apiService.registerRumah(item.rumahData)
                if (!rumahRes.isSuccessful || rumahRes.body()?.success != true) {
                    val errMsg = rumahRes.body()?.message ?: "Gagal sinkronisasi data rumah ()"
                    updateState(item.localId, SyncState.ERROR, errMsg)
                    continue
                }

                val rumahId = rumahRes.body()!!.rumah?.id ?: item.localId

                for (kkReq in item.kks) {
                    apiService.linkKk(rumahId, kkReq)
                }

                val visitRes = apiService.submitVisit(rumahId, item.visitData)
                if (visitRes.isSuccessful && visitRes.body()?.success == true) {
                    updateState(item.localId, SyncState.SYNCED)
                    successCount++
                } else {
                    val errMsg = visitRes.body()?.message ?: "Gagal sinkronisasi kunjungan ()"
                    updateState(item.localId, SyncState.ERROR, errMsg)
                }
            } catch (e: Exception) {
                updateState(item.localId, SyncState.ERROR, e.message ?: "Koneksi jaringan terputus.")
            }
        }

        return successCount
    }
}
