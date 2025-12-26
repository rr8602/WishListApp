package com.wishlist.app.utils

import android.content.Context
import com.wishlist.app.data.PriceHistory
import com.wishlist.app.data.WishCategory
import com.wishlist.app.data.WishItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 위시리스트 백업 및 복원 관리 클래스
 */
object BackupManager {

    private const val VERSION = 1
    private const val KEY_VERSION = "version"
    private const val KEY_BACKUP_DATE = "backupDate"
    private const val KEY_WISHES = "wishes"

    /**
     * 위시리스트를 JSON 형식으로 백업
     */
    fun exportToJson(wishes: List<WishItem>): String {
        val jsonObject = JSONObject()
        jsonObject.put(KEY_VERSION, VERSION)
        jsonObject.put(KEY_BACKUP_DATE, System.currentTimeMillis())

        val wishesArray = JSONArray()
        wishes.forEach { wish ->
            val wishJson = JSONObject().apply {
                put("id", wish.id)
                put("title", wish.title)
                put("description", wish.description)
                put("imageUri", wish.imageUri ?: "")
                put("price", wish.price ?: 0.0)
                put("category", wish.category.name)
                put("priority", wish.priority)
                put("targetDate", wish.targetDate ?: 0L)
                put("shoppingUrl", wish.shoppingUrl ?: "")
                put("priceTrackingEnabled", wish.priceTrackingEnabled)
                put("lowestPrice", wish.lowestPrice ?: 0.0)
                put("createdAt", wish.createdAt)
                put("isCompleted", wish.isCompleted)

                // 가격 이력
                val priceHistoryArray = JSONArray()
                wish.priceHistory.forEach { history ->
                    val historyJson = JSONObject().apply {
                        put("price", history.price)
                        put("timestamp", history.timestamp)
                        put("source", history.source)
                    }
                    priceHistoryArray.put(historyJson)
                }
                put("priceHistory", priceHistoryArray)
            }
            wishesArray.put(wishJson)
        }

        jsonObject.put(KEY_WISHES, wishesArray)
        return jsonObject.toString(2) // 들여쓰기 2칸으로 보기 좋게 포맷
    }

    /**
     * JSON에서 위시리스트 복원
     */
    fun importFromJson(jsonString: String): List<WishItem> {
        val wishes = mutableListOf<WishItem>()

        try {
            val jsonObject = JSONObject(jsonString)
            val version = jsonObject.optInt(KEY_VERSION, 1)

            if (version > VERSION) {
                throw IllegalArgumentException("백업 파일의 버전이 너무 높습니다. 앱을 업데이트해주세요.")
            }

            val wishesArray = jsonObject.getJSONArray(KEY_WISHES)
            for (i in 0 until wishesArray.length()) {
                val wishJson = wishesArray.getJSONObject(i)

                // 가격 이력 복원
                val priceHistory = mutableListOf<PriceHistory>()
                val priceHistoryArray = wishJson.optJSONArray("priceHistory")
                if (priceHistoryArray != null) {
                    for (j in 0 until priceHistoryArray.length()) {
                        val historyJson = priceHistoryArray.getJSONObject(j)
                        priceHistory.add(
                            PriceHistory(
                                price = historyJson.optDouble("price"),
                                timestamp = historyJson.optLong("timestamp"),
                                source = historyJson.optString("source", "")
                            )
                        )
                    }
                }

                val wish = WishItem(
                    id = wishJson.optString("id", UUID.randomUUID().toString()),
                    title = wishJson.getString("title"),
                    description = wishJson.optString("description", ""),
                    imageUri = wishJson.optString("imageUri").takeIf { it.isNotEmpty() },
                    price = wishJson.optDouble("price").takeIf { it != 0.0 },
                    category = try {
                        WishCategory.valueOf(wishJson.optString("category", "OTHER"))
                    } catch (e: IllegalArgumentException) {
                        WishCategory.OTHER
                    },
                    priority = wishJson.optInt("priority", 2),
                    targetDate = wishJson.optLong("targetDate").takeIf { it != 0L },
                    shoppingUrl = wishJson.optString("shoppingUrl").takeIf { it.isNotEmpty() },
                    priceTrackingEnabled = wishJson.optBoolean("priceTrackingEnabled", false),
                    lowestPrice = wishJson.optDouble("lowestPrice").takeIf { it != 0.0 },
                    priceHistory = priceHistory,
                    createdAt = wishJson.optLong("createdAt", System.currentTimeMillis()),
                    isCompleted = wishJson.optBoolean("isCompleted", false)
                )

                wishes.add(wish)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("백업 파일을 읽을 수 없습니다: ${e.message}")
        }

        return wishes
    }

    /**
     * 백업 파일을 OutputStream에 쓰기
     */
    fun writeBackupToStream(wishes: List<WishItem>, outputStream: OutputStream) {
        val json = exportToJson(wishes)
        outputStream.use { stream ->
            stream.write(json.toByteArray())
        }
    }

    /**
     * InputStream에서 백업 파일 읽기
     */
    fun readBackupFromStream(inputStream: InputStream): List<WishItem> {
        val json = inputStream.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
        return importFromJson(json)
    }

    /**
     * 백업 파일 이름 생성
     */
    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "wishlist_backup_$timestamp.json"
    }

    /**
     * 백업 파일 정보 가져오기
     */
    fun getBackupInfo(jsonString: String): BackupInfo? {
        return try {
            val jsonObject = JSONObject(jsonString)
            val backupDate = jsonObject.optLong(KEY_BACKUP_DATE, 0L)
            val wishCount = jsonObject.getJSONArray(KEY_WISHES).length()

            BackupInfo(
                backupDate = backupDate,
                wishCount = wishCount,
                version = jsonObject.optInt(KEY_VERSION, 1)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 백업 파일 정보 데이터 클래스
     */
    data class BackupInfo(
        val backupDate: Long,
        val wishCount: Int,
        val version: Int
    )
}
