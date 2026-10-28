package com.matrixmarket.app.ui.navigation

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.matrixmarket.app.R
import com.matrixmarket.app.auth.BiometricHelper
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.BottomNavItem
import com.matrixmarket.app.ui.components.MatrixBottomNavBar
import com.matrixmarket.app.ui.components.OfflineBanner
import com.matrixmarket.app.ui.screens.createlisting.CreateListingScreen
import com.matrixmarket.app.ui.screens.home.HomeScreen
import com.matrixmarket.app.ui.screens.listingdetail.ListingDetailScreen
import com.matrixmarket.app.ui.screens.lock.BiometricLockScreen
import com.matrixmarket.app.ui.screens.login.LoginScreen
import com.matrixmarket.app.ui.screens.login.RegisterScreen
import com.matrixmarket.app.ui.screens.notifications.NotificationsScreen
import com.matrixmarket.app.ui.screens.onboarding.OnboardingScreen
import com.matrixmarket.app.ui.screens.profile.ProfileScreen
import com.matrixmarket.app.ui.screens.scanner.BarcodeScannerScreen
import com.matrixmarket.app.ui.screens.settings.SettingsScreen
import com.matrixmarket.app.ui.screens.trademeeting.MyMeetingsScreen
import com.matrixmarket.app.ui.screens.trademeeting.ScheduleMeetingScreen
import com.matrixmarket.app.ui.screens.watchlist.WatchlistScreen
import com.matrixmarket.app.ui.theme.MatrixBlack
import kotlinx.coroutines.launch

private const val TAG = "NavGraph"

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val LOCK = "biometric_lock"
    const val MAIN = "main"
}

private object MainRoutes {
    const val LISTINGS = "listings"
    const val LISTING_DETAIL = "listing_detail/{listingId}"
    fun listingDetail(id: Int) = "listing_detail/$id"
    const val CREATE_LISTING = "create_listing"
    const val SCANNER = "scanner"
    const val MEET = "meet"
    const val SCHEDULE = "schedule_meeting/{offerId}"
    fun schedule(offerId: Int) = "schedule_meeting/$offerId"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val WATCHLIST = "watchlist"
}

// Top-level graph: onboarding/login, biometric lock, or straight into the app.
@Composable
fun MatrixMarketNavGraph() {
    val navController = rememberNavController()
    // Saved so a language change (which recreates the activity) doesn't re-run this check.
    var startDestination by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (startDestination == null) {
            val session = AppContainer.sessionManager
            startDestination = when {
                !session.isLoggedIn() -> Routes.ONBOARDING
                session.isBiometricEnabled() -> Routes.LOCK
                else -> Routes.MAIN
            }
            Log.i(TAG, "Start destination: $startDestination")
        }
    }

    val start = startDestination
    if (start == null) {
        // Brief dark splash while the stored session is read.
        Box(Modifier.fillMaxSize().background(MatrixBlack))
        return
    }

    fun goToMainClearingStack() {
        navController.navigate(Routes.MAIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    fun goToOnboardingClearingStack() {
        navController.navigate(Routes.ONBOARDING) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onLogin = { navController.navigate(Routes.LOGIN) },
                onRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { goToMainClearingStack() },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { goToMainClearingStack() },
                onBackToLogin = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.REGISTER) { inclusive = true } }
                }
            )
        }
        composable(Routes.LOCK) {
            BiometricLockScreen(
                onUnlocked = { goToMainClearingStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            MainScreen(onLoggedOut = { goToOnboardingClearingStack() })
        }
    }
}

// Hosts the 5-tab bottom navigation once the user is signed in.
@Composable
private fun MainScreen(onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val isOnline by AppContainer.connectivity.isOnline.collectAsState()
    val pendingCount by AppContainer.listingRepository.pendingCount.collectAsState(initial = 0)

    // The API rejected our token (expired / revoked) -> back to the start.
    LaunchedEffect(Unit) {
        AppContainer.sessionExpired.collect {
            Log.w(TAG, "Session expired - returning to onboarding")
            onLoggedOut()
        }
    }

    // Ask for notification permission (Android 13+) once, if notifications are on.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.i(TAG, "Notification permission granted=$granted")
        if (granted) scope.launch { AppContainer.pushTokenManager.syncToken() }
    }
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!askedNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            AppContainer.sessionManager.getNotificationsEnabled()
        ) {
            askedNotificationPermission = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // One-time offer to turn on fingerprint / face unlock after the first login.
    var showBiometricOffer by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val session = AppContainer.sessionManager
        if (!session.wasBiometricOfferShown() && !session.isBiometricEnabled() &&
            BiometricHelper.availability(context) == BiometricHelper.Availability.AVAILABLE
        ) {
            showBiometricOffer = true
        }
    }
    if (showBiometricOffer) {
        val title = stringResource(R.string.biometric_enable_title)
        val subtitle = stringResource(R.string.biometric_enable_subtitle)
        val cancel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = {
                showBiometricOffer = false
                scope.launch { AppContainer.sessionManager.setBiometricOfferShown() }
            },
            title = { Text(stringResource(R.string.biometric_offer_title)) },
            text = { Text(stringResource(R.string.biometric_offer_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showBiometricOffer = false
                    scope.launch { AppContainer.sessionManager.setBiometricOfferShown() }
                    val activity = BiometricHelper.findActivity(context) ?: return@TextButton
                    BiometricHelper.authenticate(
                        activity, title, subtitle, cancel,
                        onSuccess = { scope.launch { AppContainer.sessionManager.setBiometricEnabled(true) } },
                        onNegative = {},
                        onError = {}
                    )
                }) { Text(stringResource(R.string.enable)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBiometricOffer = false
                    scope.launch { AppContainer.sessionManager.setBiometricOfferShown() }
                }) { Text(stringResource(R.string.not_now)) }
            }
        )
    }

    val selectedTab = when {
        route == null -> null
        route == MainRoutes.PROFILE || route == MainRoutes.WATCHLIST -> BottomNavItem.Profile
        route == MainRoutes.MEET || route.startsWith("schedule_meeting") -> BottomNavItem.Meet
        route == MainRoutes.CREATE_LISTING || route == MainRoutes.SCANNER -> BottomNavItem.Sell
        route == MainRoutes.SETTINGS -> BottomNavItem.Settings
        else -> BottomNavItem.Listings
    }

    Scaffold(
        bottomBar = {
            if (route != MainRoutes.SCANNER) {
                MatrixBottomNavBar(selected = selectedTab, onNavigate = { item -> innerNav.navigateToTab(item.route) })
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!isOnline) OfflineBanner(pendingCount)
            NavHost(
                navController = innerNav,
                startDestination = MainRoutes.LISTINGS,
                modifier = Modifier.weight(1f)
            ) {
                composable(MainRoutes.LISTINGS) {
                    HomeScreen(
                        onListingClick = { id -> innerNav.navigate(MainRoutes.listingDetail(id)) },
                        onOpenNotifications = { innerNav.navigate(MainRoutes.NOTIFICATIONS) },
                        onOpenWatchlist = { innerNav.navigate(MainRoutes.WATCHLIST) },
                        onCreateListing = { innerNav.navigateToTab(MainRoutes.CREATE_LISTING) }
                    )
                }
                composable(
                    MainRoutes.LISTING_DETAIL,
                    arguments = listOf(navArgument("listingId") { type = NavType.IntType })
                ) { entry ->
                    ListingDetailScreen(
                        listingId = entry.arguments?.getInt("listingId") ?: 0,
                        onBack = { innerNav.popBackStack() },
                        onOfferCreated = { offerId -> innerNav.navigate(MainRoutes.schedule(offerId)) },
                        onDeleted = { innerNav.popBackStack() }
                    )
                }
                composable(MainRoutes.CREATE_LISTING) { entry ->
                    val scannedIsbn by entry.savedStateHandle.getStateFlow<String?>("scanned_isbn", null).collectAsState()
                    CreateListingScreen(
                        scannedIsbn = scannedIsbn,
                        onScannedIsbnConsumed = { entry.savedStateHandle["scanned_isbn"] = null },
                        onScanIsbnClick = { innerNav.navigate(MainRoutes.SCANNER) },
                        onFinished = { innerNav.navigateToTab(MainRoutes.LISTINGS) }
                    )
                }
                composable(MainRoutes.SCANNER) {
                    BarcodeScannerScreen(
                        onIsbnDetected = { isbn ->
                            innerNav.previousBackStackEntry?.savedStateHandle?.set("scanned_isbn", isbn)
                            innerNav.popBackStack()
                        },
                        onBack = { innerNav.popBackStack() }
                    )
                }
                composable(MainRoutes.MEET) {
                    MyMeetingsScreen()
                }
                composable(
                    MainRoutes.SCHEDULE,
                    arguments = listOf(navArgument("offerId") { type = NavType.IntType })
                ) { entry ->
                    ScheduleMeetingScreen(
                        offerId = entry.arguments?.getInt("offerId") ?: 0,
                        onBack = { innerNav.popBackStack() },
                        onDone = { innerNav.navigateToTab(MainRoutes.MEET) }
                    )
                }
                composable(MainRoutes.PROFILE) {
                    ProfileScreen(
                        onOpenWatchlist = { innerNav.navigate(MainRoutes.WATCHLIST) },
                        onOpenNotifications = { innerNav.navigate(MainRoutes.NOTIFICATIONS) }
                    )
                }
                composable(MainRoutes.SETTINGS) {
                    SettingsScreen(onLoggedOut = onLoggedOut)
                }
                composable(MainRoutes.NOTIFICATIONS) {
                    NotificationsScreen(
                        onBack = { innerNav.popBackStack() },
                        onOpenListing = { id -> innerNav.navigate(MainRoutes.listingDetail(id)) },
                        onOpenMeetings = { innerNav.navigateToTab(MainRoutes.MEET) }
                    )
                }
                composable(MainRoutes.WATCHLIST) {
                    WatchlistScreen(
                        onBack = { innerNav.popBackStack() },
                        onListingClick = { id -> innerNav.navigate(MainRoutes.listingDetail(id)) }
                    )
                }
            }
        }
    }
}

// Standard bottom-navigation behaviour: one copy of each tab, state kept per tab.
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
