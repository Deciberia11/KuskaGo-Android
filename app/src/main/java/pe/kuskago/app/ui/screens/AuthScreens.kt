package pe.kuskago.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.R
import pe.kuskago.app.data.CurrentUser
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.ui.components.AndeanStripe
import pe.kuskago.app.ui.components.CheckboxCard
import pe.kuskago.app.ui.components.ErrorCard
import pe.kuskago.app.ui.components.FieldLabel
import pe.kuskago.app.ui.components.KCard
import pe.kuskago.app.ui.components.KTextField
import pe.kuskago.app.ui.components.PrimaryButton
import pe.kuskago.app.ui.components.Screen
import pe.kuskago.app.ui.components.SecondaryButton
import pe.kuskago.app.ui.components.SuccessCheck
import pe.kuskago.app.ui.components.TopBar
import pe.kuskago.app.ui.theme.K
import pe.kuskago.app.ui.theme.KFont
import pe.kuskago.app.ui.components.IconLabel
import pe.kuskago.app.ui.components.KIcon

private val DNI_REGEX = Regex("^\\d{8}$")
private val PHONE_REGEX = Regex("^9\\d{8}$")
private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun onlyDigits(text: String, max: Int) = text.filter { it.isDigit() }.take(max)

// =============================================================
// 1. BIENVENIDA
// =============================================================
@Composable
fun WelcomeScreen(onStart: () -> Unit, onHaveAccount: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to K.GreenSoft,
                    0.55f to K.Bg,
                    1f to K.Bg,
                )
            )
            .systemBarsPadding()
            .padding(bottom = 32.dp),
    ) {
        AndeanStripe()
        Spacer(Modifier.weight(0.6f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = "Logo de KuskaGo",
                modifier = Modifier.size(152.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "KuskaGo",
                    fontFamily = KFont.Title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = K.GreenDark,
                    letterSpacing = (-0.5).sp,
                )
            }
            Text(
                "Tu aliado en cada paso, más cerca de ti.",
                fontSize = 13.5.sp,
                color = K.Muted,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Trámites más simples.\nMenos viajes. Más oportunidades.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp),
            fontFamily = KFont.Title,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = K.Ink,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
        )
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton("Comenzar", onStart)
            SecondaryButton("Ya tengo una cuenta", onHaveAccount)
        }
        Spacer(Modifier.weight(0.6f))
        AndeanStripe()
    }
}

// =============================================================
// 2. INICIAR SESIÓN
// =============================================================
private enum class LoginError { EMPTY, FORMAT, NOT_FOUND }

@Composable
fun LoginScreen(
    store: KuskaStore,
    onBack: () -> Unit,
    onForgot: () -> Unit,
    onSignUp: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    var dni by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<LoginError?>(null) }

    fun attemptLogin() {
        val d = dni.trim()
        if (d.isEmpty() || pass.isBlank()) { error = LoginError.EMPTY; return }
        if (!DNI_REGEX.matches(d)) { error = LoginError.FORMAT; return }
        if (d == KuskaStore.DEMO_DNI) {
            store.setCurrentUser(CurrentUser(d, KuskaStore.DEMO_NAME))
            store.setRememberSession(rememberMe); onLoggedIn(); return
        }
        if (store.checkPassword(d, pass)) {
            store.setCurrentUser(CurrentUser(d, store.getUser(d)?.name.orEmpty()))
            store.setRememberSession(rememberMe); onLoggedIn(); return
        }
        error = LoginError.NOT_FOUND
    }

    Screen {
        TopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Image(painterResource(R.drawable.ic_logo), "Logo de KuskaGo", Modifier.size(64.dp))
                Text("KuskaGo", fontFamily = KFont.Title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.GreenDark)
            }
            Text(
                "Inicia sesión",
                fontFamily = KFont.Title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = K.Ink,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            error?.let {
                val (title, body) = when (it) {
                    LoginError.EMPTY -> "Completa tus datos" to "Ingresa tu DNI y contraseña para continuar."
                    LoginError.FORMAT -> "DNI inválido" to "El DNI debe tener exactamente 8 números, sin letras ni caracteres especiales."
                    LoginError.NOT_FOUND -> "Datos no válidos" to "Ese DNI no está registrado o los datos son incorrectos. Prueba con 12345678 o crea una cuenta nueva abajo."
                }
                ErrorCard(title, body, Modifier.padding(bottom = 16.dp))
            }

            FieldLabel("DNI")
            KTextField(
                value = dni,
                onValueChange = { dni = onlyDigits(it, 8); error = null },
                placeholder = "Ingresa tu DNI (prueba: 12345678)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            FieldLabel("Contraseña")
            KTextField(
                value = pass,
                onValueChange = { pass = it; error = null },
                placeholder = "Ingresa tu contraseña",
                password = true,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Checkbox) { rememberMe = !rememberMe },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = K.Green),
                        modifier = Modifier.padding(12.dp),
                    )
                    Text("Recordarme", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Label)
                }
                Text(
                    "¿Olvidaste tu contraseña?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = K.Green,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onForgot)
                        .padding(vertical = 12.dp),
                )
            }
            if (dni.length != 8 || pass.isBlank()) {
                Hint("Para continuar, ingresa los 8 dígitos del DNI y tu contraseña.")
            }
            PrimaryButton("Iniciar sesión", ::attemptLogin, enabled = DNI_REGEX.matches(dni.trim()) && pass.isNotBlank())

            KCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp),
                background = K.MustardBg,
                borderColor = K.MustardBorder,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KIcon(":editnote:", size = 34.dp, tint = K.WarnText)
                Text("¿Primera vez en KuskaGo?", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = K.Ink)
                Text(
                    "Crea tu cuenta gratis en 1 minuto para guardar tus trámites y documentos.",
                    fontSize = 12.5.sp,
                    color = K.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                PrimaryButton("Crear cuenta nueva", onSignUp, containerColor = K.MustardText)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// =============================================================
// 3. CREAR CUENTA
// =============================================================
private enum class SignUpError(val title: String, val body: String) {
    EXISTS("Este DNI ya tiene una cuenta", "Inicia sesión con esa cuenta o usa otro DNI para crear una nueva."),
    NAME("Falta tu nombre", "Ingresa tus nombres completos para crear la cuenta."),
    DNI("DNI inválido", "El DNI debe tener exactamente 8 números, sin letras."),
    PHONE("Celular inválido", "El celular debe tener 9 números, empezar con 9 y no contener letras, espacios ni caracteres especiales."),
    PASS("Falta tu contraseña", "Crea una contraseña para continuar."),
    MISMATCH("Las contraseñas no coinciden", "Revisa \"Contraseña\" y \"Confirmar contraseña\": deben ser iguales."),
    EMAIL("Correo inválido", "Ingresa un correo con formato válido (ej. usuario@correo.com) o déjalo vacío."),
}

@Composable
fun SignUpScreen(store: KuskaStore, onBack: () -> Unit, onGoToLogin: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var dni by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var pass2 by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("") }
    var done by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<SignUpError?>(null) }

    fun create() {
        val n = name.trim()
        val e = email.trim()
        error = when {
            n.isEmpty() -> SignUpError.NAME
            !DNI_REGEX.matches(dni.trim()) -> SignUpError.DNI
            store.accountExists(dni.trim()) -> SignUpError.EXISTS
            !PHONE_REGEX.matches(phone.trim()) -> SignUpError.PHONE
            pass.isBlank() -> SignUpError.PASS
            pass != pass2 -> SignUpError.MISMATCH
            e.isNotEmpty() && !EMAIL_REGEX.matches(e) -> SignUpError.EMAIL
            else -> null
        }
        if (error == null) {
            store.saveUser(dni.trim(), n, phone.trim(), pass, e)
            store.saveAddressFor(dni.trim(), address, district)
            done = true
        }
    }

    Screen {
        if (done) {
            val first = name.trim().split(" ").firstOrNull().orEmpty().ifEmpty { "vecino" }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            ) {
                SuccessCheck()
                Text(
                    "¡Cuenta creada, $first!",
                    fontFamily = KFont.Title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = K.Ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Cuenta creada correctamente. Ahora inicia sesión con tu DNI y contraseña.",
                    fontSize = 14.5.sp,
                    color = K.Muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )
            }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
                PrimaryButton("Ir a iniciar sesión", onGoToLogin)
            }
        } else {
            SignUpForm(
                error = error,
                name = name, onName = { name = it; error = null },
                dni = dni, onDni = { dni = onlyDigits(it, 8); error = null },
                phone = phone, onPhone = { phone = onlyDigits(it, 9); error = null },
                pass = pass, onPass = { pass = it; error = null },
                pass2 = pass2, onPass2 = { pass2 = it; error = null },
                email = email, onEmail = { email = it; error = null },
                address = address, onAddress = { address = it },
                district = district, onDistrict = { district = it },
                onScanned = { d ->
                    d.fullName?.let { name = it }
                    d.dni?.let { dni = it }
                    d.address?.let { address = it }
                    d.district?.let { district = it }
                    error = null
                },
                onLocated = { g ->
                    g.address?.let { address = it }
                    g.district?.let { district = it }
                },
                onBack = onBack,
                onCreate = ::create,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.SignUpForm(
    error: SignUpError?,
    name: String, onName: (String) -> Unit,
    dni: String, onDni: (String) -> Unit,
    phone: String, onPhone: (String) -> Unit,
    pass: String, onPass: (String) -> Unit,
    pass2: String, onPass2: (String) -> Unit,
    email: String, onEmail: (String) -> Unit,
    address: String, onAddress: (String) -> Unit,
    district: String, onDistrict: (String) -> Unit,
    onScanned: (pe.kuskago.app.data.identity.DniData) -> Unit,
    onLocated: (pe.kuskago.app.data.identity.AddressGuess) -> Unit,
    onBack: () -> Unit,
    onCreate: () -> Unit,
) {
    TopBar(onBack = onBack, title = "Crear cuenta")
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        error?.let { ErrorCard(it.title, it.body, Modifier.padding(bottom = 16.dp)) }

        pe.kuskago.app.ui.components.ScanDniCard(onResult = onScanned, modifier = Modifier.padding(bottom = 18.dp))

        FieldLabel("Nombres completos")
        KTextField(name, onName, "Ej. Juan Quispe", Modifier.padding(bottom = 16.dp))
        FieldLabel("DNI")
        KTextField(dni, onDni, "8 dígitos", Modifier.padding(bottom = 16.dp), keyboardType = KeyboardType.Number)
        FieldLabel("Celular")
        KTextField(phone, onPhone, "9xx xxx xxx", Modifier.padding(bottom = 16.dp), keyboardType = KeyboardType.Phone)
        FieldLabel("Contraseña")
        KTextField(pass, onPass, "Crea una contraseña", Modifier.padding(bottom = 16.dp), password = true)
        FieldLabel("Confirmar contraseña")
        KTextField(pass2, onPass2, "Repite tu contraseña", Modifier.padding(bottom = 16.dp), password = true)
        FieldLabel("Correo electrónico", extra = "(opcional)")
        KTextField(email, onEmail, "usuario@correo.com", Modifier.padding(bottom = 16.dp), keyboardType = KeyboardType.Email)
        FieldLabel("Dirección de tu casa", extra = "(opcional)")
        KTextField(address, onAddress, "Jr./Av., número, barrio", Modifier.padding(bottom = 8.dp))
        pe.kuskago.app.ui.components.UseMyLocationButton(onResult = onLocated, modifier = Modifier.padding(bottom = 16.dp))
        FieldLabel("Distrito", extra = "(opcional)")
        KTextField(district, onDistrict, "Ej. Puno", Modifier.padding(bottom = 6.dp))
        Text(
            "Con tu dirección y distrito completamos tus trámites y te mostramos las oficinas más cercanas.",
            fontSize = 12.sp, color = K.Muted2, modifier = Modifier.padding(bottom = 22.dp),
        )
        PrimaryButton("Crear cuenta", onCreate)
        Spacer(Modifier.height(20.dp))
    }
}

// =============================================================
// 4. RECUPERAR CONTRASEÑA
// =============================================================
private enum class ForgotStep { DNI, CHANNEL, CODE, NEW_PASS, CONFIRM }

@Composable
fun ForgotPasswordScreen(store: KuskaStore, onExit: () -> Unit, onGoToLogin: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(ForgotStep.DNI) }
    var dni by rememberSaveable { mutableStateOf("") }
    var channelEmail by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var newPass by rememberSaveable { mutableStateOf("") }
    var newPass2 by rememberSaveable { mutableStateOf("") }
    var recoveryError by rememberSaveable { mutableStateOf<String?>(null) }

    val account = remember(dni) { store.getUser(dni.trim()) }
    val hasEmail = !account?.email.isNullOrBlank()

    fun back() {
        if (step.ordinal > 0) step = ForgotStep.entries[step.ordinal - 1] else onExit()
    }
    BackHandler(enabled = step.ordinal > 0) { back() }

    Screen {
        TopBar(onBack = ::back, title = "Recuperar contraseña")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 6.dp),
        ) {
            recoveryError?.let { ErrorCard("No se pudo continuar", it, Modifier.padding(bottom = 16.dp)) }
            when (step) {
                ForgotStep.DNI -> {
                    Hint("Ingresa tu DNI para verificar tu identidad.")
                    FieldLabel("DNI")
                    KTextField(
                        dni, { dni = onlyDigits(it, 8) }, "Ingresa tu DNI",
                        Modifier.padding(bottom = 26.dp), keyboardType = KeyboardType.Number,
                    )
                    PrimaryButton("Continuar", { channelEmail = false; step = ForgotStep.CHANNEL }, enabled = DNI_REGEX.matches(dni.trim()))
                }

                ForgotStep.CHANNEL -> {
                    Hint("¿Cómo quieres recibir tu código de verificación?")
                    Column(
                        modifier = Modifier.padding(bottom = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CheckboxCard(
                            checked = !channelEmail,
                            onClick = { channelEmail = false },
                            showBox = false,
                            role = Role.RadioButton,
                        ) {
                            IconLabel(":smartphone: SMS al 9xx xxx x21", fontWeight = FontWeight.Bold, color = K.Ink, modifier = Modifier.weight(1f))
                        }
                        if (hasEmail) {
                            CheckboxCard(
                                checked = channelEmail,
                                onClick = { channelEmail = true },
                                showBox = false,
                                role = Role.RadioButton,
                            ) {
                                IconLabel(
                                    ":email: Correo ${maskEmail(account!!.email)}",
                                    fontWeight = FontWeight.Bold,
                                    color = K.Ink,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            CheckboxCard(checked = false, onClick = {}, showBox = false, enabled = false) {
                                Column(Modifier.weight(1f)) {
                                    IconLabel(":email: Correo electrónico", fontWeight = FontWeight.Bold, color = K.Ink)
                                    Text("No registraste un correo", fontSize = 12.sp, color = K.Muted2, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    PrimaryButton("Enviar código", { step = ForgotStep.CODE })
                }

                ForgotStep.CODE -> {
                    Hint("Ingresa el código de 6 dígitos que enviamos.")
                    KTextField(
                        value = code,
                        onValueChange = { code = onlyDigits(it, 6) },
                        placeholder = "• • • • • •",
                        keyboardType = KeyboardType.NumberPassword,
                        textStyle = TextStyle(
                            fontFamily = KFont.Body,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 8.sp,
                            textAlign = TextAlign.Center,
                            color = K.Ink,
                        ),
                        placeholderAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Text(
                        "Reenviar código",
                        fontSize = 13.sp,
                        color = K.Green,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(role = Role.Button) { code = "" }
                            .padding(vertical = 6.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton("Verificar", { step = ForgotStep.NEW_PASS }, enabled = code.length == 6)
                }

                ForgotStep.NEW_PASS -> {
                    FieldLabel("Nueva contraseña")
                    KTextField(newPass, { newPass = it }, "Crea una nueva contraseña", Modifier.padding(bottom = 16.dp), password = true)
                    FieldLabel("Confirmar contraseña")
                    KTextField(newPass2, { newPass2 = it }, "Repite la contraseña", Modifier.padding(bottom = 26.dp), password = true)
                    PrimaryButton("Guardar contraseña", {
                        recoveryError = null
                        if (store.updatePassword(dni.trim(), newPass)) {
                            step = ForgotStep.CONFIRM
                        } else {
                            recoveryError = "No encontramos una cuenta registrada con ese DNI. Revisa el número antes de continuar."
                        }
                    }, enabled = newPass.isNotBlank() && newPass == newPass2)
                    if (newPass2.isNotEmpty() && newPass != newPass2) {
                        Hint("Las contraseñas deben coincidir para poder guardarlas.")
                    }
                }

                ForgotStep.CONFIRM -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SuccessCheck(80.dp)
                        Text("Contraseña actualizada", fontFamily = KFont.Title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                        Text(
                            "Ya puedes iniciar sesión con tu nueva contraseña.",
                            fontSize = 14.sp,
                            color = K.Muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        if (step == ForgotStep.CONFIRM) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
                PrimaryButton("Volver a iniciar sesión", onGoToLogin)
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 15.sp, color = K.Muted, modifier = Modifier.padding(bottom = 20.dp))
}

private fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 0) return email
    return email.take(1) + "***" + email.substring(at)
}
