package id.p2kd.kalisalak.coklit.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.p2kd.kalisalak.coklit.data.models.VoterItem

class LocalVoterCacheManager(context: Context) {

    private val prefs = context.getSharedPreferences("p2kd_voter_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveVoters(stageKey: String, voters: List<VoterItem>, total: Int) {
        prefs.edit()
            .putString("cached_voters_$stageKey", gson.toJson(voters))
            .putInt("cached_total_$stageKey", total)
            .putLong("cached_time_$stageKey", System.currentTimeMillis())
            .apply()
    }

    fun getCachedVoters(stageKey: String): Pair<List<VoterItem>, Int>? {
        val json = prefs.getString("cached_voters_$stageKey", null) ?: return null
        val total = prefs.getInt("cached_total_$stageKey", 0)
        return try {
            val type = object : TypeToken<List<VoterItem>>() {}.type
            val list: List<VoterItem> = gson.fromJson(json, type)
            Pair(list, total)
        } catch (_: Exception) {
            null
        }
    }

    fun searchCached(stageKey: String, query: String): List<VoterItem> {
        val cached = getCachedVoters(stageKey)?.first ?: return emptyList()
        val clean = query.trim().lowercase()
        val digits = clean.replace(Regex("[^0-9]"), "")
        return cached.filter {
            it.nama.lowercase().contains(clean) ||
            (digits.isNotEmpty() && it.nik.contains(digits)) ||
            (it.alamat?.lowercase()?.contains(clean) == true)
        }
    }
}
