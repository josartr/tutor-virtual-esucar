@file:OptIn(ExperimentalMaterial3Api::class)

package cl.esucar.tutorvirtual.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.esucar.tutorvirtual.R
import cl.esucar.tutorvirtual.domain.model.InstitutionType
import cl.esucar.tutorvirtual.domain.model.UserRole
import cl.esucar.tutorvirtual.ui.state.AuthUiState
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors

@Composable
fun LoginScreen(
    state: AuthUiState,
    onModeChanged: (Boolean) -> Unit,
    onUserRoleSelected: (UserRole) -> Unit,
    onRankSelected: (String) -> Unit,
    onTutorSelected: (String) -> Unit,
    onFirstNameChanged: (String) -> Unit,
    onFirstLastNameChanged: (String) -> Unit,
    onSecondLastNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onRegenerateEmail: () -> Unit,
    onApplyDomainSuggestion: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onTopicChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onQuickDemoLogin: () -> Unit
) {
    var rankExpanded by remember { mutableStateOf(false) }
    var tutorExpanded by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = InstitutionalColors.Background) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                    border = BorderStroke(1.dp, InstitutionalColors.Green.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.carabineros_banner),
                            contentDescription = "Carabineros de Chile · Orden y Patria",
                            colorFilter = ColorFilter.colorMatrix(
                                ColorMatrix(
                                    floatArrayOf(
                                        -0.9702f, 0f, 0f, 0f, 269.55f,
                                        0f, -0.6851f, 0f, 0f, 265.28f,
                                        0f, 0f, -0.8170f, 0f, 267.26f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "ESCUELA DE SUBOFICIALES · ESUCAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = InstitutionalColors.Green
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Tutor Virtual ESUCAR",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = InstitutionalColors.GreenDark
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Acceso unificado @esucar.cl y @carabineros.cl",
                    fontSize = 13.sp,
                    color = InstitutionalColors.MutedText,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                    border = BorderStroke(1.dp, InstitutionalColors.Border),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = InstitutionalColors.Background,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(4.dp)) {
                                listOf(false to "Iniciar sesión", true to "Crear cuenta").forEach { (isReg, label) ->
                                    val selected = state.registrationMode == isReg
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onModeChanged(isReg) },
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (selected) InstitutionalColors.Green else Color.Transparent
                                    ) {
                                        Box(
                                            Modifier.padding(vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                label,
                                                fontSize = 13.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) InstitutionalColors.White else InstitutionalColors.Graphite
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.registrationMode) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "¿Ingresas como estudiante o funcionario?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InstitutionalColors.GreenDark
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        Triple(UserRole.ESTUDIANTE, Icons.Default.School, "@esucar.cl"),
                                        Triple(UserRole.FUNCIONARIO, Icons.Default.Badge, "@carabineros.cl")
                                    ).forEach { (roleOption, icon, roleDomain) ->
                                        val selected = state.userRole == roleOption
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) InstitutionalColors.GreenSoft else InstitutionalColors.Background,
                                            border = BorderStroke(
                                                if (selected) 1.5.dp else 1.dp,
                                                if (selected) InstitutionalColors.Green else InstitutionalColors.Border
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onUserRoleSelected(roleOption) }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        icon,
                                                        contentDescription = null,
                                                        tint = if (selected) InstitutionalColors.GreenDark else InstitutionalColors.MutedText,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(5.dp))
                                                    Text(
                                                        roleOption.label,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (selected) InstitutionalColors.GreenDark else InstitutionalColors.Graphite
                                                    )
                                                }
                                                Text(
                                                    roleDomain,
                                                    fontSize = 10.sp,
                                                    color = if (selected) InstitutionalColors.Green else InstitutionalColors.MutedText
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = state.isCarabinerosDomain) {
                                ExposedDropdownMenuBox(
                                    expanded = rankExpanded,
                                    onExpandedChange = { rankExpanded = !rankExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = state.rank,
                                        onValueChange = {},
                                        readOnly = true,
                                        singleLine = true,
                                        label = { Text("Grado policial") },
                                        leadingIcon = { Icon(Icons.Default.Security, null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rankExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth(),
                                        isError = state.submitted && !state.validRank
                                    )
                                    ExposedDropdownMenu(
                                        expanded = rankExpanded,
                                        onDismissRequest = { rankExpanded = false }
                                    ) {
                                        state.availableRanks.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    onRankSelected(option)
                                                    rankExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = state.firstName,
                                onValueChange = onFirstNameChanged,
                                label = { Text("Nombres") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                isError = state.submitted && !state.validFirstName,
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                supportingText = {
                                    if (state.submitted && !state.validFirstName) {
                                        Text("Ingresa al menos 2 caracteres")
                                    }
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.firstLastName,
                                    onValueChange = onFirstLastNameChanged,
                                    label = { Text("1er Apellido") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    isError = state.submitted && !state.validFirstLastName,
                                    supportingText = {
                                        if (state.submitted && !state.validFirstLastName) Text("Obligatorio")
                                    }
                                )
                                OutlinedTextField(
                                    value = state.secondLastName,
                                    onValueChange = onSecondLastNameChanged,
                                    label = { Text("2do Apellido") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    isError = state.submitted && !state.validSecondLastName,
                                    supportingText = {
                                        if (state.submitted && !state.validSecondLastName) Text("Obligatorio")
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.email,
                            onValueChange = onEmailChanged,
                            label = { Text("Correo institucional") },
                            placeholder = {
                                Text(
                                    if (!state.registrationMode) {
                                        "usuario@esucar.cl o @carabineros.cl"
                                    } else if (state.userRole == UserRole.FUNCIONARIO) {
                                        "nombre.apellido@carabineros.cl"
                                    } else {
                                        "nombre.apellido@esucar.cl"
                                    }
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.submitted && !state.validEmail,
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.canRegenerateEmail) {
                                        IconButton(
                                            onClick = onRegenerateEmail,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AutoFixHigh,
                                                contentDescription = "Sugerir correo automático",
                                                tint = InstitutionalColors.Navy,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (state.validEmail) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Dominio válido",
                                            tint = InstitutionalColors.Green,
                                            modifier = Modifier.padding(end = 10.dp)
                                        )
                                    }
                                }
                            },
                            supportingText = {
                                when {
                                    state.submitted && !state.validEmail ->
                                        Text("Usa un correo @esucar.cl o @carabineros.cl")
                                    state.registrationMode && state.emailAutoManaged && state.validEmail ->
                                        Text(
                                            "Generado automáticamente según tus datos (${state.userRole.label})",
                                            color = InstitutionalColors.GreenDark
                                        )
                                    state.detectedInstitution == InstitutionType.ESUCAR ->
                                        Text(
                                            "Dominio detectado: ESUCAR (@esucar.cl)",
                                            color = InstitutionalColors.GreenDark
                                        )
                                    state.detectedInstitution == InstitutionType.CARABINEROS ->
                                        Text(
                                            "Dominio detectado: Carabineros de Chile (@carabineros.cl)",
                                            color = InstitutionalColors.GreenDark
                                        )
                                    else -> Unit
                                }
                            }
                        )

                        if (!state.registrationMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "esucar.cl" to Icons.Default.School,
                                    "carabineros.cl" to Icons.Default.Security
                                ).forEach { (domain, icon) ->
                                    val active = state.email.trim().lowercase().endsWith("@$domain")
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (active) InstitutionalColors.GreenSoft else InstitutionalColors.Background,
                                        border = BorderStroke(
                                            1.dp,
                                            if (active) InstitutionalColors.Green else InstitutionalColors.Border
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onApplyDomainSuggestion(domain) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = if (active) InstitutionalColors.GreenDark else InstitutionalColors.MutedText,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                "@$domain",
                                                fontSize = 11.sp,
                                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                                color = if (active) InstitutionalColors.GreenDark else InstitutionalColors.Graphite,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = onPasswordChanged,
                            label = { Text("Contraseña") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.submitted && !state.validPassword,
                            visualTransformation = if (state.passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = onTogglePasswordVisibility) {
                                    Icon(
                                        if (state.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (state.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            supportingText = {
                                if (state.submitted && !state.validPassword) {
                                    Text("Debe tener al menos 8 caracteres")
                                }
                            }
                        )

                        if (state.registrationMode) {
                            OutlinedTextField(
                                value = state.confirmPassword,
                                onValueChange = onConfirmPasswordChanged,
                                label = { Text("Repetir contraseña") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                isError = state.submitted && !state.passwordsMatch,
                                visualTransformation = if (state.confirmPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                trailingIcon = {
                                    IconButton(onClick = onToggleConfirmPasswordVisibility) {
                                        Icon(
                                            if (state.confirmPasswordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = if (state.confirmPasswordVisible) {
                                                "Ocultar contraseña"
                                            } else {
                                                "Mostrar contraseña"
                                            }
                                        )
                                    }
                                },
                                supportingText = {
                                    if (state.submitted && !state.passwordsMatch) {
                                        Text("Las contraseñas no coinciden")
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = state.researchTopic,
                                onValueChange = onTopicChanged,
                                label = { Text("Tema inicial del proyecto") },
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                                isError = state.submitted && !state.validTopic,
                                leadingIcon = { Icon(Icons.Default.Book, null) },
                                supportingText = {
                                    if (state.submitted && !state.validTopic) {
                                        Text("Ingresa el tema de tu investigación")
                                    }
                                }
                            )

                            ExposedDropdownMenuBox(
                                expanded = tutorExpanded,
                                onExpandedChange = { tutorExpanded = !tutorExpanded }
                            ) {
                                OutlinedTextField(
                                    value = state.tutor,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Profesor tutor") },
                                    leadingIcon = { Icon(Icons.Default.SupervisedUserCircle, null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tutorExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = tutorExpanded,
                                    onDismissRequest = { tutorExpanded = false }
                                ) {
                                    state.availableTutors.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, fontSize = 12.sp) },
                                            onClick = {
                                                onTutorSelected(option)
                                                tutorExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (state.feedbackMessage != null) {
                            Surface(
                                color = if (state.feedbackIsError) Color(0xFFFFEBEE) else InstitutionalColors.GreenSoft,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (state.feedbackIsError) Icons.Default.ErrorOutline else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (state.feedbackIsError) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            InstitutionalColors.GreenDark
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        state.feedbackMessage,
                                        color = if (state.feedbackIsError) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            InstitutionalColors.GreenDark
                                        },
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onSubmit,
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalColors.Green)
                        ) {
                            Icon(
                                if (state.registrationMode) Icons.Default.PersonAdd else Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (state.registrationMode) "Crear cuenta institucional" else "Iniciar sesión",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onQuickDemoLogin,
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, InstitutionalColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InstitutionalColors.Navy),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Vista previa rápida (Modo Demo)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = InstitutionalColors.MutedText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Acceso seguro · Base de datos local cifrada PBKDF2",
                            fontSize = 11.sp,
                            color = InstitutionalColors.MutedText
                        )
                    }
                }
            }
        }
    }
}
