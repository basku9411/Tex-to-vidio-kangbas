package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.remote.FirestoreVideoHistory
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

class VideoHistoryRepository(
    context: Context,
    private val userId: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    ),
    private val auth: FirebaseAuth = Firebase.auth,
    private val dao: VideoHistoryDao = AppDatabase.getInstance(context).videoHistoryDao(),
) {
    private val appContext = context.applicationContext
    private val createdRemoteIds: MutableSet<String> = Collections.synchronizedSet(HashSet())

    private fun userHistoryCollection() =
        db.collection("users").document(userId).collection("videoHistory")

    fun observeHistory(): Flow<List<VideoHistoryEntity>> = channelFlow {
        // 1. Adopt any pre-account local rows cleanly into the active account
        launch(Dispatchers.IO) {
            runCatching {
                val legacy = dao.getLegacyUnassignedHistory()
                if (legacy.isNotEmpty()) {
                    dao.claimLegacyUnassignedHistory(userId)
                    legacy.forEach { legacyItem ->
                        syncItemToFirestore(
                            legacyItem.copy(
                                userId = userId,
                                draftJson = legacyItem.draftJson
                                    ?: VideoDraftStore.readRawDraftJson(appContext, legacyItem.id),
                            )
                        )
                    }
                }
            }
        }

        // 2. Live Firestore listener for this user's persistent cloud history
        launch {
            observeRemoteSnapshots().collect { /* synced into Room */ }
        }

        // 3. Emit Room state for this userId so UI stays reactive and offline-ready
        dao.observeHistory(userId).collectLatest { items ->
            send(items)
        }
    }

    private fun observeRemoteSnapshots(): Flow<Unit> = callbackFlow {
        val path = "users/$userId/videoHistory"
        val query = userHistoryCollection()
            .orderBy("createdAtMs", Query.Direction.DESCENDING)
            .limit(50)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Firestore listener error at $path: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val remoteEntities = snapshot.documents.mapNotNull { doc ->
                val model = doc.toObject(
                    FirestoreVideoHistory::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ) ?: return@mapNotNull null
                createdRemoteIds.add(doc.id)
                model.toEntity().copy(userId = userId)
            }

            val removedIds = snapshot.documentChanges
                .filter { it.type == DocumentChange.Type.REMOVED }
                .map { it.document.id }

            launch(Dispatchers.IO) {
                runCatching {
                    val existingById = dao.getHistoryForUser(userId).associateBy { it.id }
                    val merged = remoteEntities.map { remote ->
                        val local = existingById[remote.id]
                        val chosenThumb = when {
                            local?.thumbPath != null && File(local.thumbPath).exists() -> local.thumbPath
                            remote.thumbPath != null && File(remote.thumbPath).exists() -> remote.thumbPath
                            else -> remote.thumbPath
                        }
                        val chosenVideoUrl = when {
                            remote.videoUrl?.startsWith("http") == true -> remote.videoUrl
                            local?.videoUrl != null && isExistingLocalFileUri(local.videoUrl) -> local.videoUrl
                            else -> remote.videoUrl ?: local?.videoUrl
                        }
                        val resolvedDraft = remote.draftJson ?: local?.draftJson
                        if (!resolvedDraft.isNullOrBlank()) {
                            VideoDraftStore.saveRawDraftJsonIfAbsent(appContext, remote.id, resolvedDraft)
                        }
                        remote.copy(
                            thumbPath = chosenThumb,
                            videoUrl = chosenVideoUrl,
                            draftJson = resolvedDraft,
                        )
                    }
                    if (merged.isNotEmpty()) {
                        dao.upsertAll(merged)
                    }
                    removedIds.forEach { removedId ->
                        dao.deleteById(userId, removedId)
                    }
                    dao.pruneOlderThan(userId, 50)
                }.onFailure { err ->
                    Log.w(TAG, "Failed merging snapshot into Room", err)
                }
            }
            trySend(Unit)
        }

        awaitClose { registration.remove() }
    }

    suspend fun upsert(item: VideoHistoryEntity): Result<Unit> {
        val draftJson = item.draftJson ?: withContext(Dispatchers.IO) {
            VideoDraftStore.readRawDraftJson(appContext, item.id)
        }
        val scopedItem = item.copy(
            userId = userId,
            draftJson = draftJson,
        )
        dao.upsert(scopedItem)
        dao.pruneOlderThan(userId, 50)

        return syncItemToFirestore(scopedItem)
    }

    suspend fun attachDraftJson(historyId: String) {
        val raw = withContext(Dispatchers.IO) {
            VideoDraftStore.readRawDraftJson(appContext, historyId)
        } ?: return
        val current = withContext(Dispatchers.IO) {
            dao.getHistoryForUser(userId).firstOrNull { it.id == historyId }
        } ?: return
        val updated = current.copy(draftJson = raw)
        dao.upsert(updated)
        syncItemToFirestore(updated)
    }

    private suspend fun syncItemToFirestore(item: VideoHistoryEntity): Result<Unit> {
        val docPath = "users/$userId/videoHistory/${item.id}"
        val docRef = userHistoryCollection().document(item.id)
        val record = FirestoreVideoHistory.fromEntity(item, userId)
        val isKnownCreated = createdRemoteIds.contains(item.id)
        return try {
            if (!isKnownCreated) {
                val snap = docRef.get().await()
                if (!snap.exists()) {
                    docRef.set(record.toCreateMap()).await()
                } else {
                    docRef.update(record.toUpdateMap()).await()
                }
                createdRemoteIds.add(item.id)
            } else {
                docRef.update(record.toUpdateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching {
                handleFirestoreError(
                    error = e,
                    operationType = if (isKnownCreated) OperationType.UPDATE else OperationType.WRITE,
                    path = docPath,
                    auth = auth,
                )
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { err -> Result.failure(err) },
            )
        }
    }

    suspend fun deleteItem(item: VideoHistoryEntity): Result<Unit> {
        val docPath = "users/$userId/videoHistory/${item.id}"
        dao.deleteById(userId, item.id)
        createdRemoteIds.remove(item.id)
        item.thumbPath?.let { path -> runCatching { File(path).delete() } }
        VideoDraftStore.deleteDraft(appContext, item.id)

        return try {
            userHistoryCollection().document(item.id).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching {
                handleFirestoreError(
                    error = e,
                    operationType = OperationType.DELETE,
                    path = docPath,
                    auth = auth,
                )
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { err -> Result.failure(err) },
            )
        }
    }

    suspend fun clearAll(): Result<Unit> {
        val existing = withContext(Dispatchers.IO) {
            dao.getHistoryForUser(userId)
        }
        dao.clearAllForUser(userId)
        existing.forEach { item ->
            createdRemoteIds.remove(item.id)
            item.thumbPath?.let { path -> runCatching { File(path).delete() } }
            VideoDraftStore.deleteDraft(appContext, item.id)
        }

        val collectionPath = "users/$userId/videoHistory"
        return try {
            val snap = userHistoryCollection().get().await()
            if (!snap.isEmpty) {
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching {
                handleFirestoreError(
                    error = e,
                    operationType = OperationType.DELETE,
                    path = collectionPath,
                    auth = auth,
                )
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { err -> Result.failure(err) },
            )
        }
    }

    private fun isExistingLocalFileUri(uriStr: String): Boolean {
        val path = when {
            uriStr.startsWith("file://") -> uriStr.removePrefix("file://")
            uriStr.startsWith("/") -> uriStr
            else -> return false
        }
        return File(path).exists()
    }

    companion object {
        private const val TAG = "VideoHistoryRepo"
    }
}
