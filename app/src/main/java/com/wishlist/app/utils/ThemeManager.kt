package com.wishlist.app.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * 테마 관리 클래스
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_SHOW_MESSAGE = "show_theme_message"
    private const val KEY_THEME_NAME = "theme_name"

    /**
     * 테마 모드
     */
    enum class ThemeMode(val value: Int) {
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
        DARK(AppCompatDelegate.MODE_NIGHT_YES)
    }

    /**
     * 현재 테마 모드 가져오기
     */
    fun getCurrentTheme(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getInt(KEY_THEME_MODE, ThemeMode.LIGHT.value)
        return ThemeMode.values().find { it.value == value } ?: ThemeMode.LIGHT
    }

    /**
     * 테마 모드 설정
     */
    fun setTheme(context: Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode.value).apply()
        AppCompatDelegate.setDefaultNightMode(mode.value)
    }

    /**
     * 테마 변경 메시지 저장
     */
    fun setThemeChangeMessage(context: Context, themeName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SHOW_MESSAGE, true)
            .putString(KEY_THEME_NAME, themeName)
            .apply()
    }

    /**
     * 테마 변경 메시지 가져오기 (한 번만 반환)
     */
    fun getAndClearThemeChangeMessage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val showMessage = prefs.getBoolean(KEY_SHOW_MESSAGE, false)
        return if (showMessage) {
            val themeName = prefs.getString(KEY_THEME_NAME, null)
            prefs.edit()
                .putBoolean(KEY_SHOW_MESSAGE, false)
                .remove(KEY_THEME_NAME)
                .apply()
            themeName
        } else {
            null
        }
    }

    /**
     * 테마 적용
     */
    fun applyTheme(context: Context) {
        val currentTheme = getCurrentTheme(context)
        AppCompatDelegate.setDefaultNightMode(currentTheme.value)
    }

    /**
     * 다음 테마로 전환 (Light <-> Dark)
     */
    fun toggleTheme(context: Context): ThemeMode {
        val current = getCurrentTheme(context)
        val next = when (current) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
        }
        setTheme(context, next)
        return next
    }

    /**
     * 테마 모드 이름 가져오기
     */
    fun getThemeName(mode: ThemeMode): String {
        return when (mode) {
            ThemeMode.LIGHT -> "라이트 모드"
            ThemeMode.DARK -> "다크 모드"
        }
    }
}
