package pe.kuskago.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.ui.screens.AccessibilityScreen
import pe.kuskago.app.ui.screens.AlertsScreen
import pe.kuskago.app.ui.screens.CitizenRouteScreen
import pe.kuskago.app.ui.screens.DocumentsScreen
import pe.kuskago.app.ui.screens.EditProfileScreen
import pe.kuskago.app.ui.screens.FamilyScreen
import pe.kuskago.app.ui.screens.HelpScreen
import pe.kuskago.app.ui.screens.RequisitoDetailScreen
import pe.kuskago.app.ui.screens.SimpleFlowScreen
import pe.kuskago.app.ui.screens.TemplatesScreen
import pe.kuskago.app.ui.screens.TramiteDetailScreen
import pe.kuskago.app.ui.screens.TramiteWizardScreen
import pe.kuskago.app.ui.screens.TranslatorScreen
import pe.kuskago.app.ui.screens.ForgotPasswordScreen
import pe.kuskago.app.ui.screens.HomeScreen
import pe.kuskago.app.ui.screens.HomeSimpleScreen
import pe.kuskago.app.ui.screens.LoginScreen
import pe.kuskago.app.ui.screens.PreferencesScreen
import pe.kuskago.app.ui.screens.ProfileScreen
import pe.kuskago.app.ui.screens.SearchScreen
import pe.kuskago.app.ui.screens.SignUpScreen
import pe.kuskago.app.ui.screens.WelcomeScreen
import pe.kuskago.app.ui.theme.KuskaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KuskaTheme {
                KuskaGoApp()
            }
        }
    }
}

enum class Route {
    Welcome, Login, SignUp, ForgotPassword, Preferences,
    Home, HomeSimple, Search, Profile, Accessibility,
    TramiteDetail, RequisitoDetail, Templates, Translator, TramiteWizard,
    Documents, Family, Help, CitizenRoute, Alerts, EditProfile, SimpleFlow,
}

/** Pila de navegación simple: ir a una pantalla que ya está en la pila vuelve a ella. */
class Navigator(initial: List<Route>) {
    val stack = mutableStateListOf<Route>().apply { addAll(initial) }
    val current: Route get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun go(route: Route) {
        val i = stack.lastIndexOf(route)
        if (i >= 0) {
            while (stack.size > i + 1) stack.removeAt(stack.lastIndex)
        } else {
            stack.add(route)
        }
    }

    fun replace(route: Route) {
        stack.removeAt(stack.lastIndex)
        go(route)
    }

    fun reset(vararg routes: Route) {
        stack.clear()
        stack.addAll(routes)
    }

    fun back() {
        if (canGoBack) stack.removeAt(stack.lastIndex)
    }

    companion object {
        val saver = Saver<Navigator, List<String>>(
            save = { nav -> nav.stack.map { it.name } },
            restore = { names -> Navigator(names.map { Route.valueOf(it) }) },
        )
    }
}

@Composable
fun KuskaGoApp() {
    val context = LocalContext.current
    val store = remember { KuskaStore(context) }
    val nav = rememberSaveable(saver = Navigator.saver) { Navigator(listOf(Route.Welcome)) }
    var wizardType by rememberSaveable { mutableStateOf("terreno") }
    var detailTab by rememberSaveable { mutableStateOf("resumen") }
    var profileUpdated by rememberSaveable { mutableStateOf(false) }

    val go: (Route) -> Unit = { r ->
        if (r == Route.TramiteDetail) detailTab = "resumen"
        nav.go(r)
    }
    val openEntity: () -> Unit = { detailTab = "entidad"; nav.go(Route.TramiteDetail) }
    val openWizard: (String) -> Unit = { t -> wizardType = t; nav.go(Route.TramiteWizard) }

    LaunchedEffect(nav.current) {
        if (nav.current != Route.Profile) profileUpdated = false
    }

    BackHandler(enabled = nav.canGoBack) { nav.back() }

    Crossfade(targetState = nav.current, label = "pantalla") { route ->
        when (route) {
            Route.Welcome -> WelcomeScreen(
                onStart = { nav.go(Route.Login) },
                onHaveAccount = { nav.go(Route.Login) },
            )

            Route.Login -> LoginScreen(
                store = store,
                onBack = { nav.go(Route.Welcome) },
                onForgot = { nav.go(Route.ForgotPassword) },
                onSignUp = { nav.go(Route.SignUp) },
                onLoggedIn = { nav.go(Route.Preferences) },
            )

            Route.SignUp -> SignUpScreen(
                store = store,
                onBack = { nav.go(Route.Login) },
                onGoToLogin = { nav.go(Route.Login) },
            )

            Route.ForgotPassword -> ForgotPasswordScreen(
                store = store,
                onExit = { nav.back() },
                onGoToLogin = { nav.go(Route.Login) },
            )

            Route.Preferences -> PreferencesScreen(
                store = store,
                onBack = { nav.go(Route.Login) },
                onContinue = { guided -> nav.go(if (guided) Route.HomeSimple else Route.Home) },
                onSkip = { nav.go(Route.Home) },
            )

            Route.Home -> HomeScreen(store = store, go = go)

            Route.HomeSimple -> HomeSimpleScreen(
                go = go,
                onNormalMode = { nav.replace(Route.Home) },
            )

            Route.Search -> SearchScreen(
                onBack = { nav.back() },
                go = go,
                onPick = openWizard,
            )

            Route.TramiteWizard -> TramiteWizardScreen(
                type = wizardType,
                store = store,
                onExit = { nav.back() },
                go = go,
            )

            Route.TramiteDetail -> TramiteDetailScreen(
                store = store,
                initialTab = detailTab,
                onBack = { nav.back() },
                go = { r -> if (r == Route.TramiteWizard) openWizard("terreno") else go(r) },
            )

            Route.RequisitoDetail -> RequisitoDetailScreen(
                onBack = { nav.back() },
                go = go,
                onEntity = openEntity,
            )

            Route.Templates -> TemplatesScreen(store = store, onExit = { nav.back() }, go = go)

            Route.Translator -> TranslatorScreen(onBack = { nav.back() })

            Route.Documents -> DocumentsScreen(store = store, onExit = { nav.back() }, go = go)

            Route.Family -> FamilyScreen(
                store = store,
                onExit = { nav.back() },
                go = go,
                onFamilyWizard = { openWizard("herencia") },
            )

            Route.Help -> HelpScreen(onExit = { nav.back() }, go = go)

            Route.CitizenRoute -> CitizenRouteScreen(
                store = store,
                onExit = { nav.back() },
                go = go,
                onEntity = openEntity,
            )

            Route.Alerts -> AlertsScreen(onExit = { nav.back() }, go = go)

            Route.SimpleFlow -> SimpleFlowScreen(onExit = { nav.back() }, go = go)

            Route.Profile -> ProfileScreen(
                store = store,
                showUpdated = profileUpdated,
                onBack = { nav.back() },
                go = go,
                onLogout = {
                    store.setCurrentUser(null)
                    nav.reset(Route.Welcome, Route.Login)
                },
            )

            Route.EditProfile -> EditProfileScreen(
                store = store,
                onBack = { nav.back() },
                onSaved = {
                    nav.back()
                    if (nav.current != Route.Profile) nav.go(Route.Profile)
                    profileUpdated = true
                },
            )

            Route.Accessibility -> AccessibilityScreen(
                store = store,
                onBack = { nav.go(Route.Profile) },
            )
        }
    }
}
