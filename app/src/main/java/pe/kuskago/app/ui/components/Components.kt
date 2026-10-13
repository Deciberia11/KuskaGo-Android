package pe.kuskago.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.ui.theme.K
import pe.kuskago.app.ui.theme.SIZE_LABEL

/** Contenedor de pantalla: fondo, barras del sistema y teclado. */
@Composable
fun Screen(
    background: Color = K.Bg,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .systemBarsPadding()
            .imePadding(),
        content = content,
    )
}

@Composable
fun TopBar(
    onBack: () -> Unit,
    title: String? = null,
    titleSize: TextUnit = 17.sp,
    titleColor: Color = K.Ink,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val shape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shape)
                .background(K.White)
                .border(1.dp, K.Border, shape)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "Volver" },
            contentAlignment = Alignment.Center,
        ) {
            Text("←", fontSize = 18.sp, color = K.Ink, fontWeight = FontWeight.Bold)
        }
        if (title != null) {
            Text(
                title,
                fontSize = titleSize,
                fontWeight = FontWeight.ExtraBold,
                color = titleColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = K.Green,
    fontSize: TextUnit = 16.sp,
    minHeight: Dp = 56.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = K.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
    ) {
        Text(text, fontSize = fontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = K.Green,
    borderColor: Color = K.Green,
    fontSize: TextUnit = 16.sp,
    minHeight: Dp = 56.dp,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = RoundedCornerShape(if (minHeight < 50.dp) 12.dp else 16.dp),
        border = BorderStroke(1.5.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = K.White, contentColor = contentColor),
    ) {
        Text(text, fontSize = fontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, fontSize: TextUnit = 13.5.sp) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text, color = K.Green, fontWeight = FontWeight.Bold, fontSize = fontSize)
    }
}

@Composable
fun KCard(
    modifier: Modifier = Modifier,
    background: Color = K.White,
    borderColor: Color = K.Border,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

@Composable
fun ErrorCard(title: String, body: String, modifier: Modifier = Modifier) {
    KCard(
        modifier = modifier.fillMaxWidth(),
        background = K.ErrorBg,
        borderColor = K.ErrorBorder,
    ) {
        Text(
            "⚠ $title",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.5.sp,
            color = K.ErrorTitle,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(body, fontSize = 12.5.sp, color = K.ErrorBody, lineHeight = 17.sp)
    }
}

@Composable
fun FieldLabel(text: String, extra: String? = null) {
    Row(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Label)
        if (extra != null) {
            Text(" $extra", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.Muted2)
        }
    }
}

@Composable
fun KTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    textStyle: TextStyle = TextStyle(fontSize = 15.sp, color = K.Ink),
    placeholderAlign: TextAlign = TextAlign.Start,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        minLines = minLines,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        placeholder = {
            Text(
                placeholder,
                color = K.Muted2,
                fontSize = 15.sp,
                textAlign = placeholderAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else keyboardType,
        ),
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (password) {
            {
                IconButton(
                    onClick = { visible = !visible },
                    modifier = Modifier.semantics {
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                    },
                ) {
                    Text(if (visible) "🙈" else "👁", fontSize = 18.sp)
                }
            }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = K.Green,
            unfocusedBorderColor = K.Border,
            focusedContainerColor = K.White,
            unfocusedContainerColor = K.White,
            cursorColor = K.Green,
            focusedTextColor = K.Ink,
            unfocusedTextColor = K.Ink,
            disabledTextColor = K.Muted,
            disabledContainerColor = K.Bg,
            disabledBorderColor = K.Border,
        ),
    )
}

/** Tarjeta seleccionable (checkbox-card del prototipo). */
@Composable
fun CheckboxCard(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBox: Boolean = true,
    enabled: Boolean = true,
    role: Role = Role.Checkbox,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(if (checked) K.GreenSoft else K.White)
            .border(1.5.dp, if (checked) K.Green else K.Border, shape)
            .clickable(enabled = enabled, role = role, onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        if (showBox) {
            val boxShape = RoundedCornerShape(7.dp)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(boxShape)
                    .background(if (checked) K.Green else K.White)
                    .border(1.8.dp, if (checked) K.Green else K.Faint, boxShape),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) Text("✓", color = K.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        content()
    }
}

/** Selector de tamaño de texto 1–5. */
@Composable
fun TextSizeSelector(selected: Int, onSelect: (Int) -> Unit) {
    KCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            "Tamaño del texto",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = K.Label,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (n in 1..5) {
                val active = selected == n
                val shape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(shape)
                        .background(if (active) K.Green else K.White)
                        .border(1.5.dp, if (active) K.Green else K.Border, shape)
                        .clickable(role = Role.RadioButton) { onSelect(n) }
                        .semantics { contentDescription = "Tamaño $n" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$n",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (active) K.White else K.Label,
                    )
                }
            }
        }
        Text(
            SIZE_LABEL[selected] ?: "",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = K.Green,
        )
    }
}

/** Franja de colores andinos. */
@Composable
fun AndeanStripe(modifier: Modifier = Modifier) {
    val colors = listOf(K.Terracota, K.Mustard, K.Sky, K.Green)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp),
    ) {
        val w = 16.dp.toPx()
        var x = 0f
        var i = 0
        while (x < size.width) {
            drawRect(colors[i % colors.size], topLeft = Offset(x, 0f), size = Size(w, size.height))
            x += w
            i++
        }
    }
}

data class NavEntry(val icon: String, val label: String, val active: Boolean, val onClick: () -> Unit)

@Composable
fun BottomNav(items: List<NavEntry>) {
    Column(modifier = Modifier.background(K.White)) {
        HorizontalDivider(color = K.Border, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 9.dp, bottom = 14.dp),
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Tab, onClick = item.onClick)
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(item.icon, fontSize = 18.sp)
                    Text(
                        item.label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.active) K.Green else K.Muted2,
                    )
                }
            }
        }
    }
}

/** Botón cuadrado blanco con icono (campana, perfil) y contador opcional. */
@Composable
fun IconBox(icon: String, description: String, onClick: () -> Unit, badge: String? = null) {
    Box {
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(shape)
                .background(K.White)
                .border(1.dp, K.Border, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 17.sp)
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-5).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(K.Bg)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(K.Badge),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, color = K.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun Chip(text: String, background: Color, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

/** Círculo verde con check (pantallas de éxito). */
@Composable
fun SuccessCheck(size: Dp = 84.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(K.OkBg),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size / 2)) {
            val s = this.size.width / 24f
            val path = Path().apply {
                moveTo(5f * s, 13f * s)
                lineTo(9f * s, 17f * s)
                lineTo(19f * s, 7f * s)
            }
            drawPath(
                path,
                color = K.Green,
                style = Stroke(width = 3f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = K.Muted2,
        modifier = modifier,
    )
}
