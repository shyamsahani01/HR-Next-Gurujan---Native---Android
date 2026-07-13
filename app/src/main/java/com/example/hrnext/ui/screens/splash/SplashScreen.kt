package com.example.hrnext.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen() {
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
                    end = Offset(1200f, 1400f),
                ),
            ),
    ) {
        // Soft decorative blobs for visual depth.
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-70).dp, y = (-60).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.08f)),
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.10f)),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "HR",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "HR Next Gurujan",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your Frappe HRMS, on the go",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )

            Spacer(Modifier.weight(1f))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}
