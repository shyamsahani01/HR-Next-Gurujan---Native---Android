package com.example.hrnext.ui.screens.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hrnext.model.Session

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (Session) -> Unit,
) {
    val state = viewModel.uiState
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val entranceState = remember { MutableTransitionState(false).apply { targetState = true } }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 500f),
                    ),
                ),
        ) {
            // Decorative blobs behind the hero content — a slow continuous drift so the whole
            // screen feels alive rather than static, same language as the splash screen.
            val blobDrift = rememberInfiniteTransition(label = "blobDrift")
            val drift1 by blobDrift.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
                label = "drift1",
            )
            val drift2 by blobDrift.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(5600, easing = LinearEasing), RepeatMode.Reverse),
                label = "drift2",
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(x = (-60).dp + (drift1 * 24).dp, y = (-40).dp + (drift1 * 30).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.10f)),
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp - (drift2 * 26).dp, y = 40.dp + (drift2 * 22).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.10f)),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(56.dp))

                AnimatedVisibility(
                    visibleState = entranceState,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it / 3 } +
                        scaleIn(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            initialScale = 0.6f,
                        ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("HR", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                        }

                        Spacer(Modifier.height(18.dp))
                        Text(
                            "HR Next Gurujan",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            "Your Frappe HRMS, on the go",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                AnimatedVisibility(
                    visibleState = entranceState,
                    enter = fadeIn(tween(500, delayMillis = 120)) + slideInVertically(tween(500, delayMillis = 120)) { it / 3 },
                ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Welcome back",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "Connect to your site to continue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(22.dp))

                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        )

                        OutlinedTextField(
                            value = state.siteUrl,
                            onValueChange = viewModel::onSiteUrlChange,
                            label = { Text("Site URL") },
                            placeholder = { Text("https://your-site.frappe.cloud") },
                            leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = state.username,
                            onValueChange = viewModel::onUsernameChange,
                            label = { Text("Username or Email") },
                            placeholder = { Text("you@company.com") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        AnimatedVisibility(visible = state.error != null) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            state.error.orEmpty(),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        val buttonEnabled = !state.isLoading
                        val gradientAlpha = if (buttonEnabled) 1f else 0.5f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = gradientAlpha),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = gradientAlpha),
                                        ),
                                    ),
                                )
                                .clickable(enabled = buttonEnabled) { viewModel.login(onLoginSuccess) },
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(
                                targetState = state.isLoading,
                                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                                label = "loginButtonContent",
                            ) { isLoading ->
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Text(
                                        "Log In",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "Works with any ERPNext/Frappe site — local, self-hosted or Frappe Cloud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}
