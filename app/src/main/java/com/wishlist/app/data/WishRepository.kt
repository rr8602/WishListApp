package com.wishlist.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 위시리스트 데이터를 관리하는 Repository
 * SharedPreferences를 사용하여 데이터를 영구적으로 저장
 */
class WishRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("wish_prefs", Context.MODE_PRIVATE)
    
    private val gson = Gson()
    private val WISH_LIST_KEY = "wish_list"
    
    /**
     * 모든 위시리스트 아이템 가져오기
     */
    fun getAllWishes(): List<WishItem> {
        val json = sharedPreferences.getString(WISH_LIST_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<WishItem>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * 위시리스트 아이템 추가
     */
    fun addWish(wish: WishItem) {
        val wishes = getAllWishes().toMutableList()
        wishes.add(wish)
        saveWishes(wishes)
    }
    
    /**
     * 위시리스트 아이템 업데이트
     */
    fun updateWish(updatedWish: WishItem) {
        val wishes = getAllWishes().toMutableList()
        val index = wishes.indexOfFirst { it.id == updatedWish.id }
        if (index != -1) {
            wishes[index] = updatedWish
            saveWishes(wishes)
        }
    }
    
    /**
     * 위시리스트 아이템 삭제
     */
    fun deleteWish(wishId: String) {
        val wishes = getAllWishes().toMutableList()
        wishes.removeAll { it.id == wishId }
        saveWishes(wishes)
    }

    /**
     * 모든 위시리스트 아이템 삭제
     */
    fun deleteAllWishes() {
        sharedPreferences.edit().remove(WISH_LIST_KEY).apply()
    }

    /**
     * 위시리스트 전체 저장
     */
    private fun saveWishes(wishes: List<WishItem>) {
        val json = gson.toJson(wishes)
        sharedPreferences.edit().putString(WISH_LIST_KEY, json).apply()
    }
}
