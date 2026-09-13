package ir.behzad.roozhayeman.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Study : Screen("study")
    data object StudyHome : Screen("study-home")
    data object Chat : Screen("chat")
    data object Heart : Screen("heart")
    data object More : Screen("more")
    data object Cycle : Screen("cycle")
    data object Mood : Screen("mood")
    data object Mindfulness : Screen("mindfulness")
    data object ScreenTime : Screen("screentime")
    data object Focus : Screen("focus")
    data object Calm : Screen("calm")
    data object Journal : Screen("journal")
    data object Breath : Screen("breath")
    data object Routine : Screen("routine")
    data object SafeSpace : Screen("safespace")
    data object Album : Screen("album")
    data object Writing : Screen("writing")
    data object Helplines : Screen("helplines")
    data object Library : Screen("library")
    data object Audiobook : Screen("audiobook")
    data object School : Screen("school")
    data object HealthHub : Screen("health")
    data object AwarenessHub : Screen("awareness")
    data object Academy : Screen("academy")
    data object Book : Screen("study-book/{bookCode}") {
        fun of(bookCode: String) = "study-book/${Uri.encode(bookCode)}"
    }
    data object LessonTeach : Screen("study-teach/{packId}") {
        fun of(packId: String) = "study-teach/${Uri.encode(packId)}"
    }
    data object WeeklySchedule : Screen("weekly-schedule")
    data object Meds : Screen("meds")
    data object SleepLog : Screen("sleep-log")
    data object ReadingCorner : Screen("reading-corner")
    data object Appearance : Screen("appearance")
    /** آزمون. `lessonId` اختیاری است تا از صفحه‌ی درس فقط سؤال‌های همان درس بیاید. */
    data object Quiz : Screen("quiz?lessonId={lessonId}") {
        fun of(lessonId: String? = null) =
            if (lessonId.isNullOrBlank()) "quiz" else "quiz?lessonId=${Uri.encode(lessonId)}"
    }
    data object QuizReview : Screen("quizreview")

    /** صفحه‌ی مطالعه‌ی عمیق یک درس (فلش‌کارت/آزمون/حل) — پک با `packId` مثل C905_E01-L01. */
    data object LessonStudy : Screen("study-lesson/{packId}") {
        fun of(packId: String) = "study-lesson/" + Uri.encode(packId)
    }

    /** نمایشگر PDF کتاب درس (از باکت Appwrite — کش فقط روی گوشی). */
    data object LessonPdf : Screen("study-lesson-pdf/{packId}") {
        fun of(packId: String) = "study-lesson-pdf/" + Uri.encode(packId)
    }
    data object Pdf : Screen("pdf")
    data object Charts : Screen("charts")

    /** مدیریت دانلود صوت/PDF کتاب‌ها (v1.14). */
    data object Downloads : Screen("study-downloads")
    data object HealthProgress : Screen("health-progress")
    data object Art : Screen("art")
    data object Gallery : Screen("gallery")
    data object Learning : Screen("learning")
    data object Lesson : Screen("lesson/{id}") {
        fun of(id: String) = "lesson/${Uri.encode(id)}"
    }
    data object Placement : Screen("placement")
    /** نقشه‌ی راه. `track` اختیاری است تا ماژول هوش مصنوعی فقط گره‌های خودش را ببیند. */
    data object Roadmap : Screen("roadmap?track={track}") {
        fun of(track: String? = null) = if (track.isNullOrBlank()) "roadmap" else "roadmap?track=${Uri.encode(track)}"
    }
    data object AiLearning : Screen("ailearning")
    data object AiAssessment : Screen("aiassessment")
    data object Recipes : Screen("recipes")
    data object RecipeDetail : Screen("recipedetail/{id}") {
        fun of(id: String) = "recipedetail/${Uri.encode(id)}"
    }
    data object Exercise : Screen("exercise")
    data object ExerciseDetail : Screen("exercise/{id}") { fun of(id: String) = "exercise/${Uri.encode(id)}" }
    data object Water : Screen("water")
    data object Pairing : Screen("pairing")
    data object Call : Screen("call")
    data object Settings : Screen("settings")
    data object Privacy : Screen("privacy")
    data object ChatSettings : Screen("chatsettings")
    data object Badges : Screen("badges")
    data object Lock : Screen("lock")
    data object Reminders : Screen("reminders")
    data object Sync : Screen("sync")
    /** پرامپت ۰۲: ماژول سلامتی (یوگا/ورزش/تنفس/یادگیری). */
    data object Wellness : Screen("wellness")
    /** پرامپت ۰۲: گالری مرجع‌های نقاشی سیاه‌قلم. */
    data object SketchGallery : Screen("sketch-gallery")
}

data class Tab(val route: String, val icon: ImageVector, val label: String)
val Tabs = listOf(
    Tab(Screen.Home.route, Icons.Filled.Home, "داشبورد"),
    Tab(Screen.Study.route, Icons.Filled.School, "مدرسه"),
    Tab(Screen.Academy.route, Icons.Filled.LocalLibrary, "آموزشگاه"),
    Tab(Screen.HealthHub.route, Icons.Filled.FitnessCenter, "سلامتی"),
    Tab(Screen.Chat.route, Icons.Filled.SmartToy, "همراه من"),
    Tab(Screen.More.route, Icons.Filled.MoreHoriz, "بیشتر"),
)
val TopRoutes = Tabs.map { it.route }.toSet()
