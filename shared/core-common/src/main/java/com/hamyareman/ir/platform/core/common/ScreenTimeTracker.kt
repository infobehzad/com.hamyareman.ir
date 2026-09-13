package com.hamyareman.ir.platform.core.common

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.provider.Settings
import java.util.Calendar

/** مصرف یک اپ در یک بازه. */
data class AppUsage(val packageName: String, val label: String, val minutes: Long)

/** مصرف یک روز. */
data class DayUsage(
    val dateIso: String,
    val totalMinutes: Long,
    val byApp: List<AppUsage>,
) {
    val isEmpty: Boolean get() = totalMinutes == 0L
}

/** نتیجه‌ی کامل برای صفحه‌ی «زمان صفحه». */
data class ScreenTimeReport(
    val today: DayUsage,
    val week: List<DayUsage>,
    val permissionGranted: Boolean,
) {
    val weekTotalMinutes: Long get() = week.sumOf { it.totalMinutes }
    val dailyAverageMinutes: Long get() = if (week.isEmpty()) 0 else weekTotalMinutes / week.size
}

/**
 * زمان صفحه با `UsageStatsManager` خود اندروید.
 *
 * سه نکته‌ی مهم:
 *  1) مجوز `PACKAGE_USAGE_STATS` یک مجوز ویژه است: کاربر باید **دستی** در تنظیمات
 *     دستگاه «دسترسی به مصرف» را بدهد. تا وقتی نداده، هیچ عددی ساخته نمی‌شود و
 *     اپ هم عدد جعلی نشان نمی‌دهد.
 *  2) این داده در [PrivacyPolicy.neverSyncTables] است (`screen_time_logs`)، یعنی
 *     هرگز به سرور نمی‌رود؛ نه برای پدر، نه برای هیچ‌کس.
 *  3) هدف و حالت تمرکز را خود نوجوان انتخاب می‌کند؛ اپ گوشی را قفل نمی‌کند و
 *     نمی‌تواند بکند (بدون Device Admin).
 */
class ScreenTimeTracker(
    private val context: Context,
    private val store: LocalStore? = null,
) {

    /** آیا مجوز دسترسی به آمار مصرف داده شده؟ */
    fun hasUsageAccess(): Boolean = runCatching {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return@runCatching false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /** بازکردن تنظیمات دسترسی مصرف (مسیر دقیق در هر دستگاه کمی فرق می‌کند). */
    fun openUsageAccessSettings(): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    fun today(): DayUsage = dayUsage(startOfToday(), System.currentTimeMillis(), JalaliDate.todayIso())

    /** مصرف روزهای گذشته، از امروز به عقب. */
    fun week(days: Int = 7): List<DayUsage> {
        val result = mutableListOf<DayUsage>()
        for (offset in 0 until days.coerceIn(1, 14)) {
            val end = if (offset == 0) System.currentTimeMillis() else startOfDay(offset - 1)
            val start = startOfDay(offset)
            val calendar = Calendar.getInstance().apply { timeInMillis = start }
            val iso = "%04d-%02d-%02d".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
            )
            result += dayUsage(start, end, iso)
        }
        return result
    }

    fun report(): ScreenTimeReport = ScreenTimeReport(
        today = today(),
        week = week(),
        permissionGranted = hasUsageAccess(),
    )

    // --- حالت تمرکز --------------------------------------------------------

    fun isFocusOn(): Boolean = store?.getLong(KEY_FOCUS_SINCE, 0L)?.let { it > 0L } ?: false

    fun focusStartedAt(): Long = store?.getLong(KEY_FOCUS_SINCE, 0L) ?: 0L

    fun focusMinutes(): Long {
        val since = focusStartedAt()
        if (since <= 0L) return 0L
        return ((System.currentTimeMillis() - since) / 60_000L).coerceAtLeast(0L)
    }

    fun startFocus() {
        store?.putLong(KEY_FOCUS_SINCE, System.currentTimeMillis())
    }

    fun stopFocus() {
        store?.remove(KEY_FOCUS_SINCE)
    }

    fun goalMinutes(): Int = store?.getInt(KEY_GOAL, DEFAULT_GOAL_MINUTES)?.coerceIn(15, 480) ?: DEFAULT_GOAL_MINUTES

    fun setGoalMinutes(minutes: Int) {
        store?.putInt(KEY_GOAL, minutes.coerceIn(15, 480))
    }

    // --- محاسبه ------------------------------------------------------------

    private fun dayUsage(startMs: Long, endMs: Long, dateIso: String): DayUsage {
        if (!hasUsageAccess()) return DayUsage(dateIso, 0L, emptyList())
        val manager = context.getSystemService(UsageStatsManager::class.java)
            ?: return DayUsage(dateIso, 0L, emptyList())

        val events = runCatching { manager.queryEvents(startMs, endMs) }.getOrNull()
            ?: return DayUsage(dateIso, 0L, emptyList())

        val openForeground = HashMap<String, Long>()
        val totals = HashMap<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> openForeground[pkg] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.KEYGUARD_SHOWN,
                -> {
                    val from = openForeground.remove(pkg) ?: continue
                    val delta = (event.timeStamp - from).coerceAtLeast(0L)
                    totals[pkg] = (totals[pkg] ?: 0L) + delta
                }

                else -> Unit
            }
        }
        // برنامه‌هایی که تا پایان بازه باز مانده‌اند
        openForeground.forEach { (pkg, from) ->
            val delta = (endMs - from).coerceAtLeast(0L)
            totals[pkg] = (totals[pkg] ?: 0L) + delta
        }

        val byApp = totals
            .filterValues { it >= MIN_SESSION_MS }
            .map { (pkg, ms) -> AppUsage(pkg, label(pkg), ms / 60_000L) }
            .filter { it.minutes > 0 }
            .sortedByDescending { it.minutes }
            .take(MAX_APPS)

        return DayUsage(dateIso, byApp.sumOf { it.minutes }, byApp)
    }

    private fun label(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName.substringAfterLast('.'))

    private fun startOfToday(): Long = startOfDay(0)

    private fun startOfDay(daysAgo: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        const val KEY_GOAL = "st_goal"
        const val KEY_FOCUS_SINCE = "focus_since"
        const val DEFAULT_GOAL_MINUTES = 90

        /** کمتر از ده ثانیه، نویز محسوب می‌شود (لانچر، کیبورد، …). */
        private const val MIN_SESSION_MS = 10_000L
        private const val MAX_APPS = 12
    }
}
