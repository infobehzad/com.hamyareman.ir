package ir.behzad.roozhayeman.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.behzad.platform.feature.calls.CallScreen
import ir.behzad.platform.feature.hearttoheart.HeartToHeartScreen
import ir.behzad.platform.feature.hearttoheart.MessageDirection
import ir.behzad.platform.feature.pairing.ZahraPairingScreen
import ir.behzad.roozhayeman.LocalAppContainer
import ir.behzad.roozhayeman.ui.ailearning.AiAssessmentScreen
import ir.behzad.roozhayeman.ui.ailearning.AiLearningHomeScreen
import ir.behzad.roozhayeman.ui.art.ArtGalleryScreen
import ir.behzad.roozhayeman.ui.art.DailyArtPromptScreen
import ir.behzad.roozhayeman.ui.calmdown.BreathingScreen
import ir.behzad.roozhayeman.ui.calmdown.CalmMenuScreen
import ir.behzad.roozhayeman.ui.calmdown.JournalScreen
import ir.behzad.roozhayeman.ui.chatbot.ChatScreen
import ir.behzad.roozhayeman.ui.chatbot.ChatSettingsScreen
import ir.behzad.roozhayeman.ui.cycle.CycleCalendarScreen
import ir.behzad.roozhayeman.ui.cycle.MindfulnessScreen
import ir.behzad.roozhayeman.ui.cycle.MoodCheckInScreen
import ir.behzad.roozhayeman.ui.exercise.ExerciseDetailScreen
import ir.behzad.roozhayeman.ui.exercise.ExerciseScreen
import ir.behzad.roozhayeman.ui.gamification.BadgesScreen
import ir.behzad.roozhayeman.ui.appearance.AppearanceScreen
import ir.behzad.roozhayeman.ui.hub.AwarenessHubScreen
import ir.behzad.roozhayeman.ui.hub.HealthHubScreen
import ir.behzad.roozhayeman.ui.hub.MedsScreen
import ir.behzad.roozhayeman.ui.hub.ReadingCornerScreen
import ir.behzad.roozhayeman.ui.hub.SchoolHubScreen
import ir.behzad.roozhayeman.ui.hub.SleepLogScreen
import ir.behzad.roozhayeman.ui.hub.WeeklyScheduleScreen
import ir.behzad.roozhayeman.ui.home.HomeScreen
import ir.behzad.roozhayeman.ui.learning.LearningHomeScreen
import ir.behzad.roozhayeman.ui.learning.LessonScreen
import ir.behzad.roozhayeman.ui.learning.PlacementTestScreen
import ir.behzad.roozhayeman.ui.learning.RoadmapScreen
import ir.behzad.roozhayeman.ui.more.MoreScreen
import ir.behzad.roozhayeman.ui.recipes.RecipeDetailScreen
import ir.behzad.roozhayeman.ui.recipes.RecipesScreen
import ir.behzad.roozhayeman.ui.routine.RoutineScreen
import ir.behzad.roozhayeman.ui.safespace.AlbumScreen
import ir.behzad.roozhayeman.ui.safespace.HelplinesScreen
import ir.behzad.roozhayeman.ui.safespace.SafeSpaceScreen
import ir.behzad.roozhayeman.ui.safespace.WritingPromptScreen
import ir.behzad.roozhayeman.ui.screentime.FocusModeScreen
import ir.behzad.roozhayeman.ui.screentime.ScreenTimeScreen
import ir.behzad.roozhayeman.ui.settings.PrivacySettingsScreen
import ir.behzad.roozhayeman.ui.settings.AppLockScreen
import ir.behzad.roozhayeman.ui.settings.RemindersScreen
import ir.behzad.roozhayeman.ui.settings.SettingsScreen
import ir.behzad.roozhayeman.ui.settings.SyncScreen
import ir.behzad.roozhayeman.ui.study.AudiobookScreen
import ir.behzad.roozhayeman.ui.wellness.SketchGalleryScreen
import ir.behzad.roozhayeman.ui.wellness.WellnessScreen
import ir.behzad.roozhayeman.ui.study.LibraryScreen
import ir.behzad.roozhayeman.ui.study.PdfUploadScreen
import ir.behzad.roozhayeman.ui.study.ProgressChartsScreen
import ir.behzad.roozhayeman.ui.study.QuizReviewScreen
import ir.behzad.roozhayeman.ui.study.QuizScreen
import ir.behzad.roozhayeman.ui.study.SchoolScheduleScreen
import ir.behzad.roozhayeman.ui.study.StudyHomeScreen
import ir.behzad.roozhayeman.ui.water.WaterScreen

@Composable
fun ZahraNavHost() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val c = LocalAppContainer.current
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
            composable(Screen.AwarenessHub.route) { AwarenessHubScreen(nav) }
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
            composable(Screen.QuizReview.route) { QuizReviewScreen { nav.popBackStack() } }
            composable(Screen.Pdf.route) { PdfUploadScreen { nav.popBackStack() } }
            composable(Screen.Charts.route) { ProgressChartsScreen { nav.popBackStack() } }
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
