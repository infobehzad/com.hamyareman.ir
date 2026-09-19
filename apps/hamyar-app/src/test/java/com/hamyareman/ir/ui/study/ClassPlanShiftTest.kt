package com.hamyareman.ir.ui.study

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * منطقِ تازهٔ شیفت: شیفتِ هفته‌ی جاری (بخش اول) لنگر است و شیفتِ هفته‌های
 * چرخه از آن به‌صورتِ متناوبِ هفتگی مشتق می‌شود. این تست‌ها روی JVM خالص اجرا می‌شوند.
 */
class ClassPlanShiftTest {

    private val saturday = LocalDate.of(2026, 9, 5) // شنبه

    private fun snap(
        cycleWeeks: Int,
        anchor: LocalDate = saturday,
        base: Shift = Shift.MORNING,
        offset: Int = 1,
        weekPattern: List<String> = emptyList(),
    ) = ClassPlanStore.Snapshot(
        locked = false,
        days = (1..5).associateWith { emptyList<String>() },
        cycleWeeks = cycleWeeks,
        anchorIso = anchor.toString(),
        fixedEvening = base == Shift.EVENING,
        lunarOffset = 0,
        morningHour = 7,
        morningMinute = 30,
        wakeLeadMin = 60,
        noonHour = 12,
        noonMinute = 0,
        sleepMorning = "21:30",
        sleepEvening = "23:00",
        weekPattern = weekPattern,
        virtualMorningHour = 8,
        virtualMorningMinute = 0,
        virtualNoonHour = 14,
        virtualNoonMinute = 0,
        thisWeekShift = base,
        cycleWeekOffset = offset,
    )

    @Test
    fun `derived shift alternates every week from the anchor base`() {
        assertEquals(Shift.MORNING, derivedShift(Shift.MORNING, 0))
        assertEquals(Shift.EVENING, derivedShift(Shift.MORNING, 1))
        assertEquals(Shift.MORNING, derivedShift(Shift.MORNING, 2))
        assertEquals(Shift.EVENING, derivedShift(Shift.MORNING, 3))
        assertEquals(Shift.EVENING, derivedShift(Shift.MORNING, -1))
        assertEquals(Shift.EVENING, derivedShift(Shift.EVENING, 0))
        assertEquals(Shift.MORNING, derivedShift(Shift.EVENING, 1))
        assertEquals(Shift.EVENING, derivedShift(Shift.EVENING, 2))
    }

    @Test
    fun `two week cycle alternates from this week shift`() {
        val s = snap(2, base = Shift.MORNING)
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(s, saturday))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday.plusDays(7)))
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(s, saturday.plusDays(14)))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday.minusDays(7)))

        val sEven = snap(2, base = Shift.EVENING)
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(sEven, saturday))
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(sEven, saturday.plusDays(7)))
    }

    @Test
    fun `fixed cycle always uses this week shift`() {
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(snap(1, base = Shift.MORNING), saturday.plusDays(21)))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(snap(1, base = Shift.EVENING), saturday.plusDays(21)))
    }

    @Test
    fun `cycle week position follows the selected offset`() {
        val s1 = snap(2, offset = 1)
        assertEquals(1, ClassPlanStore.cycleWeekPos(s1, saturday))
        assertEquals(2, ClassPlanStore.cycleWeekPos(s1, saturday.plusDays(7)))
        assertEquals(1, ClassPlanStore.cycleWeekPos(s1, saturday.plusDays(14)))

        val s2 = snap(4, offset = 3, base = Shift.EVENING)
        assertEquals(3, ClassPlanStore.cycleWeekPos(s2, saturday))
        assertEquals(4, ClassPlanStore.cycleWeekPos(s2, saturday.plusDays(7)))
        assertEquals(1, ClassPlanStore.cycleWeekPos(s2, saturday.plusDays(14)))
        assertEquals(2, ClassPlanStore.cycleWeekPos(s2, saturday.plusDays(21)))
    }

    @Test
    fun `four week cycle still alternates weekly`() {
        val s = snap(4, offset = 3, base = Shift.EVENING)
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday))
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(s, saturday.plusDays(7)))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday.plusDays(14)))
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(s, saturday.plusDays(21)))
    }

    @Test
    fun `legacy manual pattern still wins until re-saved`() {
        val s = snap(2, base = Shift.MORNING, weekPattern = listOf("evening", "evening"))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday.plusDays(7)))
    }

    @Test
    fun `captions use the stored wording`() {
        assertEquals("همیشه شیفت صبح", ClassPlanStore.captionOf(snap(1, base = Shift.MORNING), saturday))
        assertEquals("همیشه شیفت ظهر", ClassPlanStore.captionOf(snap(1, base = Shift.EVENING), saturday))
        assertEquals(
            "هفته‌ی ۱ از ۲ هفته · شیفت صبح",
            ClassPlanStore.captionOf(snap(2, offset = 1), saturday),
        )
        assertEquals(
            "هفته‌ی ۳ از ۴ هفته · شیفت ظهر",
            ClassPlanStore.captionOf(snap(4, offset = 3, base = Shift.EVENING), saturday),
        )
        assertEquals(
            "هفته‌ی ۴ از ۴ هفته · شیفت صبح",
            ClassPlanStore.captionOf(snap(4, offset = 3, base = Shift.EVENING), saturday.plusDays(7)),
        )
    }
}
