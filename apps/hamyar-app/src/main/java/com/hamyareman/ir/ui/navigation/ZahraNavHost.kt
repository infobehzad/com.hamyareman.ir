package com.hamyareman.ir.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.feature.calls.CallScreen
import com.hamyareman.ir.platform.feature.hearttoheart.HeartToHeartScreen
import com.hamyareman.ir.platform.feature.hearttoheart.MessageDirection
import com.hamyareman.ir.platform.feature.pairing.ZahraPairingScreen
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.ui.ailearning.AiAssessmentScreen
import com.hamyareman.ir.ui.ailearning.AiLearningHomeScreen
import com.hamyareman.ir.ui.appearance.AppearanceScreen
import com.hamyareman.ir.ui.art.ArtGalleryScreen
import com.hamyareman.ir.ui.art.DailyArtPromptScreen
import com.hamyareman.ir.ui.calmdown.BreathingScreen
import com.hamyareman.ir.ui.calmdown.CalmMenuScreen
import com.hamyareman.ir.ui.calmdown.JournalScreen
import com.hamyareman.ir.ui.chatbot.ChatScreen
import com.hamyareman.ir.ui.chatbot.ChatSettingsScreen
import com.hamyareman.ir.ui.cycle.CycleCalendarScreen
import com.hamyareman.ir.ui.cycle.MindfulnessScreen
import com.hamyareman.ir.ui.cycle.MoodCheckInScreen
import com.hamyareman.ir.ui.exercise.ExerciseDetailScreen
import com.hamyareman.ir.ui.exercise.ExerciseScreen
import com.hamyareman.ir.ui.gamification.BadgesScreen
import com.hamyareman.ir.ui.home.HomeScreen
import com.hamyareman.ir.ui.hub.AwarenessHubScreen
import com.hamyareman.ir.ui.hub.HealthHubScreen
import com.hamyareman.ir.ui.hub.MedsScreen
import com.hamyareman.ir.ui.hub.ReadingCornerScreen
import com.hamyareman.ir.ui.hub.SchoolHubScreen
import com.hamyareman.ir.ui.hub.SleepLogScreen
import com.hamyareman.ir.ui.hub.WeeklyScheduleScreen
import com.hamyareman.ir.ui.learning.LearningHomeScreen
import com.hamyareman.ir.ui.learning.LessonScreen
import com.hamyareman.ir.ui.learning.PlacementTestScreen
import com.hamyareman.ir.ui.learning.RoadmapScreen
import com.hamyareman.ir.ui.more.MoreScreen
import com.hamyareman.ir.ui.recipes.RecipeDetailScreen
import com.hamyareman.ir.ui.recipes.RecipesScreen
import com.hamyareman.ir.ui.routine.RoutineScreen
import com.hamyareman.ir.ui.safespace.AlbumScreen
import com.hamyareman.ir.ui.safespace.HelplinesScreen
import com.hamyareman.ir.ui.safespace.SafeSpaceScreen
import com.hamyareman.ir.ui.safespace.WritingPromptScreen
import com.hamyareman.ir.ui.screentime.FocusModeScreen
import com.hamyareman.ir.ui.screentime.ScreenTimeScreen
import com.hamyareman.ir.ui.settings.AppLockScreen
import com.hamyareman.ir.ui.settings.PrivacySettingsScreen
import com.hamyareman.ir.ui.settings.RemindersScreen
import com.hamyareman.ir.ui.settings.SettingsScreen
import com.hamyareman.ir.ui.settings.SyncScreen
import com.hamyareman.ir.ui.study.AcademyHubScreen
import com.hamyareman.ir.ui.study.AudiobookScreen
import com.hamyareman.ir.ui.study.BookDetailScreen
import com.hamyareman.ir.ui.study.DownloadsScreen
import com.hamyareman.ir.ui.study.HealthProgressScreen
import com.hamyareman.ir.ui.study.LessonPdfScreen
import com.hamyareman.ir.ui.study.LessonStudyScreen
import com.hamyareman.ir.ui.study.LessonTeachScreen
import com.hamyareman.ir.ui.study.LibraryScreen
import com.hamyareman.ir.ui.study.LockedStudyScreen
import com.hamyareman.ir.ui.study.PdfUploadScreen
import com.hamyareman.ir.ui.study.ProgressChartsScreen
import com.hamyareman.ir.ui.study.QuizReviewScreen
import com.hamyareman.ir.ui.study.QuizScreen
import com.hamyareman.ir.ui.study.SchoolScheduleScreen
import com.hamyareman.ir.ui.study.StudyHomeScreen
import com.hamyareman.ir.ui.study.StudyMedia
import com.hamyareman.ir.ui.study.TeachStats
import com.hamyareman.ir.ui.study.VideoTeachScreen
import com.hamyareman.ir.ui.study.expectedTeachMedia
import com.hamyareman.ir.ui.study.teachTracksOf
import com.hamyareman.ir.ui.water.WaterScreen
import com.hamyareman.ir.ui.wellness.SketchGalleryScreen
import com.hamyareman.ir.ui.wellness.WellnessScreen
import kotlinx.coroutines.launch

@Composable
fun ZahraNavHost() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val c = LocalAppContainer.current
    // لمس اعلان پخش → صفحه‌ی تدریس همان درس (قانون: صوت فقط در صفحه‌ی تدریس پخش می‌شود؛
    // پس بعد از لود شدن همان صفحه، پخش خودکار از TeachAudioBar شروع می‌شود).
    LaunchedEffect(com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack) {
        val p = com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack ?: return@LaunchedEffect
        if (com.hamyareman.ir.platform.feature.study.BookModuleRegistry.pack(p) != null) {
            com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack = null
            nav.navigate(Screen.LessonTeach.of(p)) {
                popUpTo(Screen.Home.route)
                launchSingleTop = true
            }
        } else {
            com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack = null
        }
    }
    Scaffold(bottomBar = {
        if (route in TopRoutes) {
            NavigationBar {
                Tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = { nav.navigate(tab.route) { popUpTo(Screen.Home.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = Screen.Home.route, modifier = Modifier.padding(pad)) {
            composable(Screen.Home.route) { HomeScreen(nav) }
            composable(Screen.Study.route) { SchoolHubScreen(nav) }
            composable(Screen.StudyHome.route) { StudyHomeScreen(nav) }
            composable(Screen.Chat.route) {
                ChatScreen(
                    onSettings = { nav.navigate(Screen.ChatSettings.route) },
                    onHelplines = { nav.navigate(Screen.Helplines.route) },
                )
            }
            composable(Screen.Heart.route) {
                HeartToHeartScreen(c.heart, MessageDirection.TO_FATHER, onStartCall = { nav.navigate(Screen.Call.route) }, onDialTel = { c.calls.dialTel(c.fatherTel) })
            }
            composable(Screen.More.route) { MoreScreen(nav) }
            composable(Screen.HealthHub.route) { HealthHubScreen(nav) }
            composable(Screen.Academy.route) { AcademyHubScreen(nav) }
            composable(
                Screen.Book.route,
                listOf(navArgument("bookCode") { type = NavType.StringType }),
            ) { entry ->
                BookDetailScreen(
                    bookCode = entry.arguments?.getString("bookCode").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onTeach = { packId -> nav.navigate(Screen.LessonTeach.of(packId)) },
                    onStudy = { packId -> nav.navigate(Screen.LessonStudy.of(packId)) },
                    onVideoTeach = { packId -> nav.navigate(Screen.VideoTeach.of(packId)) },
                    onCharts = { nav.navigate(Screen.Charts.of(entry.arguments?.getString("bookCode"))) },
                )
            }
            composable(
                Screen.LessonTeach.route,
                listOf(navArgument("packId") { type = NavType.StringType }),
            ) { entry ->
                LessonTeachScreen(
                    packId = entry.arguments?.getString("packId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onStudy = { packId -> nav.navigate(Screen.LessonStudy.of(packId)) },
                    onPdf = { packId -> nav.navigate(Screen.LessonPdf.of(packId)) },
                )
            }
            composable(Screen.AwarenessHub.route) { AwarenessHubScreen(nav) }
            composable(Screen.HealthProgress.route) { HealthProgressScreen(onBack = { nav.popBackStack() }) }
            composable(Screen.WeeklySchedule.route) { WeeklyScheduleScreen { nav.popBackStack() } }
            composable(Screen.Meds.route) { MedsScreen { nav.popBackStack() } }
            composable(Screen.SleepLog.route) { SleepLogScreen { nav.popBackStack() } }
            composable(Screen.ReadingCorner.route) { ReadingCornerScreen { nav.popBackStack() } }
            composable(Screen.Appearance.route) { AppearanceScreen { nav.popBackStack() } }
            composable(Screen.Cycle.route) { CycleCalendarScreen({ nav.popBackStack() }, { nav.navigate(Screen.Mood.route) }, { nav.navigate(Screen.Mindfulness.route) }) }
            composable(Screen.Mood.route) { MoodCheckInScreen { nav.popBackStack() } }
            composable(Screen.Mindfulness.route) { MindfulnessScreen { nav.popBackStack() } }
            composable(Screen.ScreenTime.route) { ScreenTimeScreen({ nav.popBackStack() }, { nav.navigate(Screen.Focus.route) }) }
            composable(Screen.Focus.route) { FocusModeScreen { nav.popBackStack() } }
            composable(Screen.Calm.route) { CalmMenuScreen(nav) }
            composable(Screen.Journal.route) { JournalScreen { nav.popBackStack() } }
            composable(Screen.Breath.route) { BreathingScreen { nav.popBackStack() } }
            composable(Screen.Routine.route) { RoutineScreen { nav.popBackStack() } }
            composable(Screen.SafeSpace.route) { SafeSpaceScreen(nav) }
            composable(Screen.Album.route) { AlbumScreen { nav.popBackStack() } }
            composable(Screen.Writing.route) { WritingPromptScreen { nav.popBackStack() } }
            composable(Screen.Helplines.route) { HelplinesScreen { nav.popBackStack() } }
            composable(Screen.Library.route) { LibraryScreen { nav.popBackStack() } }
            composable(Screen.Audiobook.route) { AudiobookScreen { nav.popBackStack() } }
            composable(Screen.School.route) { SchoolScheduleScreen { nav.popBackStack() } }
            composable(
                Screen.Quiz.route,
                listOf(navArgument("lessonId") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                QuizScreen(
                    lessonId = entry.arguments?.getString("lessonId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onReview = { nav.navigate(Screen.QuizReview.route) },
                )
            }

            composable(
                Screen.LessonStudy.route,
                listOf(navArgument("packId") { type = NavType.StringType }),
            ) { entry ->
                val packId = entry.arguments?.getString("packId").orEmpty()
                val pack = remember(packId) { BookModuleRegistry.pack(packId) }
                // قفل سراسری: مطالعه فقط پس از اتمام اولین دوره‌ی تدریس باز می‌شود
                // (هیچ ورودیِ دیگری به این بخش راه ندارد).
                val expected = if (pack == null) 1 else expectedTeachMedia(pack)
                val done = pack != null && run {
                    TeachStats.expectMedia(LocalContext.current, packId, expected)
                    TeachStats.isDone(LocalContext.current, packId)
                }
                if (done) {
                    LessonStudyScreen(
                        packId = packId,
                        onBack = { nav.popBackStack() },
                    )
                } else {
                    LockedStudyScreen(onBack = { nav.popBackStack() })
                }
            }
            composable(
                Screen.LessonPdf.route,
                listOf(navArgument("packId") { type = NavType.StringType }),
            ) { entry ->
                LessonPdfScreen(
                    packId = entry.arguments?.getString("packId").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Screen.QuizReview.route) { QuizReviewScreen { nav.popBackStack() } }
            composable(Screen.Pdf.route) { PdfUploadScreen { nav.popBackStack() } }
            composable(
                Screen.Charts.route,
                listOf(navArgument("bookCode") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                ProgressChartsScreen(
                    bookCode = entry.arguments?.getString("bookCode").orEmpty().ifBlank { null },
                    onBack = { nav.popBackStack() },
                    onPickBook = { code -> nav.navigate(Screen.Charts.of(code)) },
                )
            }
            composable(
                Screen.VideoTeach.route,
                listOf(navArgument("packId") { type = NavType.StringType }),
            ) { entry ->
                VideoTeachScreen(
                    packId = entry.arguments?.getString("packId").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Screen.Downloads.route) { DownloadsScreen { nav.popBackStack() } }
            composable(Screen.Art.route) { DailyArtPromptScreen({ nav.popBackStack() }, { nav.navigate(Screen.Gallery.route) }) }
            composable(Screen.Gallery.route) { ArtGalleryScreen { nav.popBackStack() } }
            composable(Screen.Learning.route) { LearningHomeScreen(nav) }
            composable(
                Screen.Lesson.route,
                listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                LessonScreen(
                    lessonId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onQuiz = { lessonId -> nav.navigate(Screen.Quiz.of(lessonId)) },
                )
            }
            composable(Screen.Placement.route) { PlacementTestScreen { nav.popBackStack() } }
            composable(
                Screen.Roadmap.route,
                listOf(navArgument("track") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                RoadmapScreen(
                    onBack = { nav.popBackStack() },
                    trackFilter = entry.arguments?.getString("track").orEmpty(),
                )
            }
            composable(Screen.AiLearning.route) { AiLearningHomeScreen(nav) }
            composable(Screen.AiAssessment.route) { AiAssessmentScreen { nav.popBackStack() } }
            composable(Screen.Recipes.route) {
                RecipesScreen(
                    onBack = { nav.popBackStack() },
                    onDetail = { recipeId -> nav.navigate(Screen.RecipeDetail.of(recipeId)) },
                )
            }
            composable(
                Screen.RecipeDetail.route,
                listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                RecipeDetailScreen(
                    recipeId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Screen.Exercise.route) {
                ExerciseScreen(onExerciseClick = { id -> nav.navigate(Screen.ExerciseDetail.of(id)) })
            }
            composable(Screen.ExerciseDetail.route, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                ExerciseDetailScreen(
                    exerciseId = e.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Screen.Water.route) { WaterScreen() }
            composable(Screen.Pairing.route) { ZahraPairingScreen(c.pairing) { nav.popBackStack() } }
            composable(Screen.Call.route) {
                CallScreen(
                    engine = c.calls,
                    onClose = { nav.popBackStack() },
                    fatherTel = c.fatherTel,
                    remoteUserId = c.partnerId,
                    remoteLabel = "بابا",
                )
            }
            composable(Screen.Settings.route) { SettingsScreen(nav) }
            composable(Screen.UserProfile.route) {
                val container = LocalAppContainer.current
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val scopeUp = rememberCoroutineScope()
                var profile by remember { mutableStateOf<com.hamyareman.ir.ui.profile.StudentProfile?>(null) }
                var loading by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    com.hamyareman.ir.ui.profile.StudentProfileState.loadAvatarMirror(ctx)
                    val uid = runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                    profile = if (uid.isBlank()) null else
                        runCatching { com.hamyareman.ir.ui.profile.StudentProfileRepo.fetch(container.tables, uid) }.getOrNull()
                    loading = false
                }
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else -> com.hamyareman.ir.ui.profile.UserProfileScreen(
                        profile = profile,
                        onBack = { nav.popBackStack() },
                        onLogout = {
                            scopeUp.launch {
                                runCatching { container.auth.logout() }
                                com.hamyareman.ir.ui.profile.StudentProfileState.clearMirror(ctx)
                                (ctx as? com.hamyareman.ir.MainActivity)?.onLoggedOut()
                            }
                        },
                        onSave = { p ->
                            scopeUp.launch {
                                val uid = runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                                val toSave = p.copy(userId = uid, grade = profile?.grade ?: com.hamyareman.ir.ui.profile.StudentProfileState.grade)
                                val email = toSave.email.ifBlank { runCatching { container.auth.currentUser() }.getOrNull()?.email.orEmpty() }
                                val ok = runCatching {
                                    com.hamyareman.ir.ui.profile.StudentProfileRepo.save(container.tables, email, toSave)
                                }.getOrDefault(false)
                                if (ok) {
                                    com.hamyareman.ir.ui.profile.StudentProfileState.writeMirror(
                                        ctx, toSave.grade, true, toSave.firstName,
                                        profile?.subscription ?: "free",
                                    )
                                    profile = toSave.copy(subscription = profile?.subscription ?: "free")
                                }
                            }
                        },
                    )
                }
            }
            composable(Screen.Privacy.route) { PrivacySettingsScreen { nav.popBackStack() } }
            composable(Screen.ChatSettings.route) { ChatSettingsScreen { nav.popBackStack() } }
            composable(Screen.Badges.route) { BadgesScreen { nav.popBackStack() } }
            composable(Screen.Lock.route) { AppLockScreen { nav.popBackStack() } }
            composable(Screen.Reminders.route) { RemindersScreen { nav.popBackStack() } }
            composable(Screen.Sync.route) { SyncScreen { nav.popBackStack() } }
            composable(Screen.Wellness.route) {
                WellnessScreen(
                    onBack = { nav.popBackStack() },
                    onSketchGallery = { nav.navigate(Screen.SketchGallery.route) },
                )
            }
            composable(Screen.SketchGallery.route) {
                SketchGalleryScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
