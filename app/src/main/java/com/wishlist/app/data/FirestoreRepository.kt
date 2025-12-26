package com.wishlist.app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore를 사용한 클라우드 데이터 저장소
 * 실시간 동기화 및 공유 기능 지원
 */
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "FirestoreRepository"
        private const val COLLECTION_WISHLISTS = "wishlists"
        private const val COLLECTION_USERS = "users"
    }

    /**
     * 현재 사용자의 위시리스트를 Firestore에 저장
     */
    suspend fun saveWishlist(wishes: List<WishItem>): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            // 각 위시 아이템을 개별 문서로 저장
            val batch = db.batch()
            val userWishlistRef = db.collection(COLLECTION_WISHLISTS)
                .document(userId)
                .collection("items")

            // 기존 아이템들 삭제 (전체 교체)
            val existingDocs = userWishlistRef.get().await()
            existingDocs.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            // 새로운 아이템들 추가
            wishes.forEach { wish ->
                val docRef = userWishlistRef.document(wish.id)
                batch.set(docRef, wish)
            }

            batch.commit().await()
            Log.d(TAG, "Wishlist saved to Firestore: ${wishes.size} items")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving wishlist to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * 특정 위시 아이템 저장/업데이트
     */
    suspend fun saveWishItem(wish: WishItem): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            db.collection(COLLECTION_WISHLISTS)
                .document(userId)
                .collection("items")
                .document(wish.id)
                .set(wish)
                .await()

            Log.d(TAG, "WishItem saved: ${wish.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving wish item", e)
            Result.failure(e)
        }
    }

    /**
     * 특정 위시 아이템 삭제
     */
    suspend fun deleteWishItem(wishId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            db.collection(COLLECTION_WISHLISTS)
                .document(userId)
                .collection("items")
                .document(wishId)
                .delete()
                .await()

            Log.d(TAG, "WishItem deleted: $wishId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting wish item", e)
            Result.failure(e)
        }
    }

    /**
     * 현재 사용자의 위시리스트 가져오기
     */
    suspend fun getMyWishlist(): Result<List<WishItem>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            val snapshot = db.collection(COLLECTION_WISHLISTS)
                .document(userId)
                .collection("items")
                .get()
                .await()

            val wishes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(WishItem::class.java)
            }

            Log.d(TAG, "Wishlist loaded from Firestore: ${wishes.size} items")
            Result.success(wishes)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading wishlist from Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * 다른 사용자의 위시리스트 가져오기 (공유된 위시리스트 보기)
     */
    suspend fun getUserWishlist(userId: String): Result<List<WishItem>> {
        return try {
            val snapshot = db.collection(COLLECTION_WISHLISTS)
                .document(userId)
                .collection("items")
                .get()
                .await()

            val wishes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(WishItem::class.java)
            }

            Log.d(TAG, "User wishlist loaded: ${wishes.size} items for user $userId")
            Result.success(wishes)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user wishlist", e)
            Result.failure(e)
        }
    }

    /**
     * 실시간 위시리스트 동기화 (Flow 사용)
     */
    fun observeMyWishlist(): Flow<List<WishItem>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            close(Exception("User not logged in"))
            return@callbackFlow
        }

        val listenerRegistration: ListenerRegistration = db.collection(COLLECTION_WISHLISTS)
            .document(userId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing wishlist", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val wishes = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(WishItem::class.java)
                    }
                    trySend(wishes)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * 다른 사용자의 위시리스트 실시간 관찰
     */
    fun observeUserWishlist(userId: String): Flow<List<WishItem>> = callbackFlow {
        val listenerRegistration: ListenerRegistration = db.collection(COLLECTION_WISHLISTS)
            .document(userId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing user wishlist", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val wishes = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(WishItem::class.java)
                    }
                    trySend(wishes)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * 위시 아이템에 선물 완료 표시
     */
    suspend fun markAsGifted(
        ownerId: String,
        wishId: String,
        giftedByName: String,
        message: String? = null
    ): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid

            val giftInfo = GiftInfo(
                giftedBy = currentUserId,
                giftedByName = giftedByName,
                giftedAt = System.currentTimeMillis(),
                message = message
            )

            db.collection(COLLECTION_WISHLISTS)
                .document(ownerId)
                .collection("items")
                .document(wishId)
                .update("giftInfo", giftInfo)
                .await()

            Log.d(TAG, "WishItem marked as gifted: $wishId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking wish as gifted", e)
            Result.failure(e)
        }
    }

    /**
     * 선물 완료 취소
     */
    suspend fun unmarkAsGifted(ownerId: String, wishId: String): Result<Unit> {
        return try {
            db.collection(COLLECTION_WISHLISTS)
                .document(ownerId)
                .collection("items")
                .document(wishId)
                .update("giftInfo", null)
                .await()

            Log.d(TAG, "WishItem gift unmarked: $wishId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unmarking wish as gifted", e)
            Result.failure(e)
        }
    }

    /**
     * 사용자 정보 가져오기
     */
    suspend fun getUserInfo(userId: String): Result<User?> {
        return try {
            val snapshot = db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user info", e)
            Result.failure(e)
        }
    }
}
