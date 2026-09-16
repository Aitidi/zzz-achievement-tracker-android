package com.aitidi.zzztracker.viewmodel

import android.content.SharedPreferences
import com.aitidi.zzztracker.model.AchievementCategories

/** Persist the alias migration as soon as preferences are loaded, including combined selections. */
internal fun loadCategorySelection(prefs: SharedPreferences): Set<String> {
    val raw = prefs.getString("selectedCategories", "").orEmpty()
    val stored = raw.split("\u001F").filter { it.isNotBlank() }.toSet()
    val migrated = AchievementCategories.canonicalSelection(stored)
    if (migrated != stored) {
        prefs.edit().putString("selectedCategories", migrated.joinToString("\u001F")).apply()
    }
    return migrated
}
