package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val animateAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "AlphaAnimation"
    )
    val animateScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ScaleAnimation"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2000) // 2 second delay requirement
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .alpha(animateAlpha)
                .scale(animateScale)
        ) {
            // Elegant Graduate Logo Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = BluePrimary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Text(
                    text = "🎓", 
                    fontSize = 58.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Smart Student Manager",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BluePrimary,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Kelola Akademik dan Keuangan Mahasiswa dalam Satu Aplikasi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Tiny aesthetic tagline loading dot
            Row {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(BluePrimary, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(GreenSecondary, CircleShape)
                )
            }
        }
    }
}
