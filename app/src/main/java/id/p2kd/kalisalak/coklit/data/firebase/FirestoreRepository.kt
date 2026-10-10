package id.p2kd.kalisalak.coklit.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import id.p2kd.kalisalak.coklit.data.models.VoterItem
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // Collections
    private const val COL_VOTERS = "pemilih"
    private const val COL_HOUSES = "rumah"
    private const val COL_VISITS = "kunjungan"
    private const val COL_COMPLAINTS = "aduan"
    private const val COL_ANNOUNCEMENTS = "pengumuman"

    /**
     * Save or update voter in Firestore
     */
    suspend fun saveVoter(voter: VoterItem) {
        val docRef = db.collection(COL_VOTERS).document(voter.id)
        val data = mapOf(
            "id" to voter.id,
            "nik" to voter.nik,
            "nama" to voter.nama,
            "jenisKelamin" to voter.jenisKelamin,
            "usia" to voter.displayAge,
            "alamat" to voter.alamat,
            "rt" to voter.rt,
            "rw" to voter.rw,
            "tps" to voter.tps,
            "tahap" to voter.tahap,
            "status" to voter.status,
            "keterangan" to voter.keterangan,
            "updatedAt" to (voter.updatedAt ?: com.google.firebase.Timestamp.now().toDate().toString())
        )
        docRef.set(data, SetOptions.merge()).await()
    }

    /**
     * Query voters by stage
     */
    suspend fun getVotersByStage(tahap: String): List<Map<String, Any>> {
        val snapshot = db.collection(COL_VOTERS)
            .whereEqualTo("tahap", tahap)
            .limit(100)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.data }
    }

    /**
     * Record field visit
     */
    suspend fun recordVisit(visitId: String, payload: Map<String, Any>) {
        db.collection(COL_VISITS).document(visitId).set(payload, SetOptions.merge()).await()
    }

    /**
     * Listen to announcements real-time
     */
    suspend fun getAnnouncements(): List<Map<String, Any>> {
        val snapshot = db.collection(COL_ANNOUNCEMENTS).limit(20).get().await()
        return snapshot.documents.mapNotNull { it.data }
    }
}