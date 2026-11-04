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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import pe.kuskago.app.ui.components.HomeTutorial
import pe.kuskago.app.data.AccessPrefs
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.ui.screens.AccessibilityScreen
import pe.kuskago.app.ui.screens.AlertsScreen
import pe.kuskago.app.ui.screens.CitizenRouteScreen
import pe.kuskago.app.ui.screens.DocumentsScreen
import pe.kuskago.app.ui.screens.EditProfileScreen
import pe.kuskago.app.ui.screens.FamilyScreen
import pe.kuskago.app.ui.screens.HelpScreen
import pe.kuskago.app.ui.screens.MyTramitesScreen
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
import pe.kuskago.app.ui.theme.textScale

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
    MyTramites,
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
    val nav = rememberSaveable(saver = Navigator.saver) {
        // Si el usuario marcó "Recordarme" y su sesión sigue abierta, entra directo al inicio.
        val user = store.currentUser()
        when {
            user != null && store.rememberSession() -> {
                val guided = store.getPrefs(user.dni)?.guided == true
                Navigator(listOf(if (guided) Route.HomeSimple else Route.Home))
            }
            else -> {
                if (user != null) store.setCurrentUser(null)   // sesión sin "Recordarme": se cierra
                Navigator(listOf(Route.Welcome))
            }
        }
    }
    var wizardType by rememberSaveable { mutableStateOf("terreno") }
    var detailTab by rememberSaveable { mutableStateOf("resumen") }
    var profileUpdated by rememberSaveable { mutableStateOf(false) }

    fun selectedHome(): Route = if (store.currentUser()?.dni?.let { store.getPrefs(it) }?.guided == true) Route.HomeSimple else Route.Home
    fun selectNormalMode() {
        store.currentUser()?.dni?.let { dni ->
            store.savePrefs(dni, (store.getPrefs(dni) ?: AccessPrefs()).copy(guided = false))
        }
        nav.reset(Route.Home)
    }
    val currentRoute = if (nav.current == Route.Home || nav.current == Route.HomeSimple) selectedHome() else nav.current
    val go: (Route) -> Unit = { r ->
        if (r == Route.TramiteDetail) detailTab = "resumen"
        nav.go(if (r == Route.Home || r == Route.HomeSimple) selectedHome() else r)
    }
    val openEntity: () -> Unit = { detailTab = "entidad"; nav.go(Route.TramiteDetail) }
    val openWizard: (String) -> Unit = { t -> wizardType = t; nav.go(Route.TramiteWizard) }

    LaunchedEffect(nav.current) {
        if (nav.current != Route.Profile) profileUpdated = false
    }

    BackHandler(enabled = nav.canGoBack) { nav.back() }

    // "Texto grande" de las preferencias se aplica a TODA la app (se vuelve a leer al cambiar de pantalla).
    val appTextScale = remember(nav.current) {
        store.currentUser()?.dni?.let { store.getPrefs(it) }.textScale()
    }
    val baseDensity = LocalDensity.current

    Crossfade(targetState = currentRoute, label = "pantalla") { route ->
        // Preferencias y Accesibilidad ya muestran su propia vista previa del tamaño.
        val scale = if (route == Route.Preferences || route == Route.Accessibility) 1f else appTextScale
        CompositionLocalProvider(
            LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * scale),
        ) {
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
                onContinue = { _ -> nav.reset(selectedHome()) },
                onSkip = { selectNormalMode() },
            )

            Route.Home -> HomeTutorial(store, simple = false, active = route == currentRoute && selectedHome() == Route.Home) { target ->
                HomeScreen(store = store, go = go, target = target)
            }

            Route.HomeSimple -> HomeTutorial(store, simple = true, active = route == currentRoute && selectedHome() == Route.HomeSimple) { target -> HomeSimpleScreen(
                target = target,
                go = go,
                onNormalMode = { selectNormalMode() },
            ) }

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

            Route.MyTramites -> MyTramitesScreen(
                store = store,
                onBack = { nav.back() },
                go = go,
                onOpenWizard = openWizard,
            )

            Route.CitizenRoute -> CitizenRouteScreen(
                store = store,
                onExit = { nav.back() },
                go = go,
                onEntity = openEntity,
            )

            Route.Alerts -> AlertsScreen(store = store, onExit = { nav.back() }, go = go)

            Route.SimpleFlow -> SimpleFlowScreen(onExit = { nav.back() }, go = go)

            Route.Profile -> ProfileScreen(
                store = store,
                showUpdated = profileUpdated,
                onBack = { nav.back() },
                go = go,
                onLogout = {
                    store.setRememberSession(false)
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
}
