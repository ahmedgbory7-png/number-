package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Support full screen bleed edge-to-edge correctly
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showSplash) {
                        SplashScreen {
                            showSplash = false
                        }
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var pulseState by remember { mutableStateOf(false) }
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (pulseState) 1.04f else 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        visible = true
        pulseState = true
        delay(2000) // Delightful 2.0s loader duration
        visible = false
        delay(300) // Transition buffer
        onTimeout()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1BD8C1),
                            Color(0xFF006DAE),
                            Color(0xFF013A63)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Adaptive layout overlaying Foreground on top of Background
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(scaleAnim)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF26D4CE), Color(0xFF08689C))
                            ),
                            shape = RoundedCornerShape(36.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Load foreground pigeon vector
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Icon Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = "حاسبة العملات الذكية",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        letterSpacing = 0.5.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.25f),
                            blurRadius = 6f
                        )
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "محول العملات للمسافرين • Real-time Guide",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(44.dp))
                
                // Minimalist glowing circle progress load indicator
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "Developed by Abu Watan",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(letterSpacing = 0.5.sp)
                )
            }
        }
    }
}
