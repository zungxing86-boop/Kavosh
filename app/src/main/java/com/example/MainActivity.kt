package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.core.common.t
import com.example.core.design.MyJourneyTheme
import com.example.core.navigation.Routes
import com.example.domain.model.AppLanguage
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.achievements.AchievementsAndReportScreen
import com.example.ui.adventure.AdventureMapScreen
import com.example.ui.companion.CompanionScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.journal.PrivateJournalScreen
import com.example.ui.missions.MissionDetailScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.parent.FamilyRoomScreen
import com.example.ui.parent.ParentRoomViewModel
import com.example.ui.projects.ProjectDetailScreen
import com.example.ui.projects.ProjectLabListScreen
import com.example.ui.projects.ProjectLabViewModel
import com.example.ui.settings.SettingsBackupViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.skills.SkillsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MyJourneyApp).container

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(container))
            val projectViewModel: ProjectLabViewModel = viewModel(factory = ProjectLabViewModel.Factory(container))
            val parentViewModel: ParentRoomViewModel = viewModel(factory = ParentRoomViewModel.Factory(container))
            val settingsViewModel: SettingsBackupViewModel = viewModel(factory = SettingsBackupViewModel.Factory(container))

            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

            MyJourneyTheme(
                themeMode = uiState.settings.themeMode,
                language = uiState.settings.language
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.userProfile?.onboardingCompleted != true) {
                        OnboardingScreen(
                            language = uiState.settings.language,
                            interests = uiState.interests,
                            onToggleLanguage = { mainViewModel.toggleLanguage() },
                            onComplete = { name, avatarId, colorHex, accessory, uri, interests, prefs ->
                                mainViewModel.completeOnboarding(name, avatarId, colorHex, accessory, uri, interests, prefs)
                            }
                        )
                    } else {
                        MyJourneyMainNavHost(
                            uiState = uiState,
                            mainViewModel = mainViewModel,
                            projectViewModel = projectViewModel,
                            parentViewModel = parentViewModel,
                            settingsViewModel = settingsViewModel
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyJourneyMainNavHost(
    uiState: MainUiState,
    mainViewModel: MainViewModel,
    projectViewModel: ProjectLabViewModel,
    parentViewModel: ParentRoomViewModel,
    settingsViewModel: SettingsBackupViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.DASHBOARD
    val lang = uiState.settings.language
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarEvent) {
        uiState.snackbarEvent?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearSnackbar()
        }
    }

    val topLevelRoutes = listOf(
        Routes.DASHBOARD,
        Routes.ADVENTURE_MAP,
        Routes.PROJECTS,
        Routes.JOURNAL,
        Routes.COMPANION
    )
    val isTopLevel = currentRoute in topLevelRoutes

    val navItems = listOf(
        Triple(Routes.DASHBOARD, t(lang, "خانه", "Home"), Icons.Filled.Home),
        Triple(Routes.ADVENTURE_MAP, t(lang, "نقشه", "Map"), Icons.Filled.Map),
        Triple(Routes.PROJECTS, t(lang, "پروژه‌ها", "Projects"), Icons.Filled.Build),
        Triple(Routes.JOURNAL, t(lang, "دفترچه", "Journal"), Icons.Filled.Lock),
        Triple(Routes.COMPANION, t(lang, "همراه من", "Companion"), Icons.Filled.Psychology)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && isTopLevel) {
                NavigationRail(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    navItems.forEach { (route, label, icon) ->
                        NavigationRailItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(Routes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (isTopLevel || currentRoute in listOf(Routes.SKILLS, Routes.ACHIEVEMENTS, Routes.FAMILY_ROOM, Routes.SETTINGS)) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (currentRoute) {
                                        Routes.DASHBOARD -> t(lang, "مسیر من", "My Journey")
                                        Routes.ADVENTURE_MAP -> t(lang, "نقشه ماجراجویی", "Adventure Map")
                                        Routes.PROJECTS -> t(lang, "آزمایشگاه پروژه‌ها", "Project Lab")
                                        Routes.JOURNAL -> t(lang, "دفترچه خصوصی", "Private Journal")
                                        Routes.COMPANION -> t(lang, "همراه من", "My Companion")
                                        Routes.SKILLS -> t(lang, "مهارت‌ها", "Skills")
                                        Routes.ACHIEVEMENTS -> t(lang, "دستاوردها و گزارش", "Badges & Report")
                                        Routes.FAMILY_ROOM -> t(lang, "اتاق خانواده", "Family Room")
                                        Routes.SETTINGS -> t(lang, "تنظیمات", "Settings")
                                        else -> t(lang, "مسیر من", "My Journey")
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                if (!isTopLevel) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(lang, "بازگشت", "Back"))
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { mainViewModel.toggleLanguage() },
                                    modifier = Modifier.testTag("topbar_language_toggle")
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) "EN" else "فا",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (currentRoute != Routes.SETTINGS) {
                                    IconButton(
                                        onClick = { navController.navigate(Routes.SETTINGS) },
                                        modifier = Modifier.testTag("topbar_settings_button")
                                    ) {
                                        Icon(Icons.Filled.Settings, contentDescription = t(lang, "تنظیمات", "Settings"))
                                    }
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    if (!useRail && isTopLevel) {
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            navItems.forEach { (route, label, icon) ->
                                NavigationBarItem(
                                    selected = currentRoute == route,
                                    onClick = {
                                        navController.navigate(route) {
                                            popUpTo(Routes.DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("bottom_nav_$route")
                                )
                            }
                        }
                    }
                },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val constrainedModifier = Modifier.widthIn(max = 840.dp).fillMaxSize()

                    NavHost(
                        navController = navController,
                        startDestination = Routes.DASHBOARD,
                        modifier = constrainedModifier
                    ) {
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                uiState = uiState,
                                onOpenMission = { id ->
                                    mainViewModel.selectMission(id)
                                    navController.navigate(Routes.missionDetail(id))
                                },
                                onNavigate = { route -> navController.navigate(route) },
                                onMarkMessagesRead = { mainViewModel.markSupportMessagesRead() }
                            )
                        }
                        composable(Routes.ADVENTURE_MAP) {
                            AdventureMapScreen(
                                uiState = uiState,
                                onOpenMission = { id ->
                                    mainViewModel.selectMission(id)
                                    navController.navigate(Routes.missionDetail(id))
                                }
                            )
                        }
                        composable(
                            route = Routes.MISSION_DETAIL,
                            arguments = listOf(navArgument("missionId") { type = NavType.StringType })
                        ) { entry ->
                            val missionId = entry.arguments?.getString("missionId") ?: "mission_01"
                            MissionDetailScreen(
                                missionId = missionId,
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onStartMission = { m, short -> mainViewModel.startMission(m, short) },
                                onToggleStep = { m, idx -> mainViewModel.toggleMissionStep(m, idx) },
                                onToggleShortVersion = { id, short -> mainViewModel.toggleMissionShortVersion(id, short) },
                                onSaveDraft = { id, note, ans -> mainViewModel.saveMissionDraft(id, note, ans) },
                                onCompleteMission = { m, note, ans, fb -> mainViewModel.completeMission(m, note, ans, fb) },
                                onPauseOrSave = { id, st, reason, note -> mainViewModel.pauseOrSaveMission(id, st, reason, note) }
                            )
                        }
                        composable(Routes.PROJECTS) {
                            val projects by projectViewModel.projects.collectAsStateWithLifecycle()
                            ProjectLabListScreen(
                                language = lang,
                                projects = projects,
                                onOpenProject = { pid -> navController.navigate(Routes.projectDetail(pid)) },
                                onCreateProject = { proj -> projectViewModel.saveProject(proj, lang == AppLanguage.PERSIAN) },
                                onDeleteProject = { pid -> projectViewModel.deleteProject(pid) }
                            )
                        }
                        composable(
                            route = Routes.PROJECT_DETAIL,
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                        ) { entry ->
                            val pid = entry.arguments?.getLong("projectId") ?: 0L
                            val projects by projectViewModel.projects.collectAsStateWithLifecycle()
                            ProjectDetailScreen(
                                projectId = pid,
                                language = lang,
                                projects = projects,
                                viewModel = projectViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Routes.SKILLS) {
                            SkillsScreen(
                                uiState = uiState,
                                onOpenMission = { id -> navController.navigate(Routes.missionDetail(id)) }
                            )
                        }
                        composable(Routes.ACHIEVEMENTS) {
                            AchievementsAndReportScreen(uiState = uiState)
                        }
                        composable(Routes.JOURNAL) {
                            PrivateJournalScreen(
                                language = lang,
                                entries = uiState.journalEntries,
                                onSaveEntry = { entry -> mainViewModel.saveJournalEntry(entry) },
                                onDeleteEntry = { id -> mainViewModel.deleteJournalEntry(id) }
                            )
                        }
                        composable(Routes.COMPANION) {
                            CompanionScreen(
                                language = lang,
                                modules = uiState.companionModules,
                                onSaveToJournal = { title, body -> mainViewModel.saveCompanionNotesToJournal(title, body) },
                                onCreateProject = { title, goal, steps -> mainViewModel.createProjectFromCompanion(title, goal, steps) }
                            )
                        }
                        composable(Routes.FAMILY_ROOM) {
                            val parentState by parentViewModel.uiState.collectAsStateWithLifecycle()
                            FamilyRoomScreen(
                                uiState = uiState,
                                parentState = parentState,
                                onSetupPin = { pin, q, a -> parentViewModel.setupPin(pin, q, a, lang == AppLanguage.PERSIAN) },
                                onVerifyPin = { pin -> parentViewModel.verifyPin(pin, lang == AppLanguage.PERSIAN) },
                                onRecoverPin = { ans, newPin -> parentViewModel.recoverAndResetPin(ans, newPin, lang == AppLanguage.PERSIAN) },
                                onLockRoom = { parentViewModel.lockRoom() },
                                onSendEncouragement = { sender, msg -> parentViewModel.sendEncouragement(sender, msg, lang == AppLanguage.PERSIAN) }
                            )
                        }
                        composable(Routes.SETTINGS) {
                            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                            SettingsScreen(
                                uiState = uiState,
                                settingsState = settingsState,
                                viewModel = settingsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
