package com.lockin.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.lockin.app.ui.navigation.LockInNavGraph
import com.lockin.app.ui.theme.LockInTheme
import com.lockin.app.ui.theme.SurfaceDark
import com.lockin.app.ui.theme.SurfaceDarkEnd
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LockInTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(SurfaceDark, SurfaceDarkEnd),
                            ),
                        ),
                ) {
                    LockInNavGraph()
                }
            }
        }
    }
}
