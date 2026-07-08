package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FireAssetViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FireAssetViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            Screen.values().forEach { screen ->
                                val isSelected = currentScreen == screen
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = { Text(screen.title) },
                                    modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    when (currentScreen) {
                        Screen.DASHBOARD -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToInspection = { assetId ->
                                    viewModel.selectAsset(assetId)
                                    currentScreen = Screen.INSPECTION
                                },
                                modifier = modifier
                            )
                        }
                        Screen.ASSETS -> {
                            AssetRegisterScreen(
                                viewModel = viewModel,
                                onNavigateToInspection = { assetId ->
                                    viewModel.selectAsset(assetId)
                                    currentScreen = Screen.INSPECTION
                                },
                                modifier = modifier
                            )
                        }
                        Screen.INSPECTION -> {
                            InspectionScreen(
                                viewModel = viewModel,
                                modifier = modifier
                            )
                        }
                        Screen.GEMINI -> {
                            GeminiChatScreen(
                                viewModel = viewModel,
                                modifier = modifier
                            )
                        }
                        Screen.REPORTS -> {
                            ReportsScreen(
                                viewModel = viewModel,
                                modifier = modifier
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    ASSETS("Assets", Icons.Default.Inventory2),
    INSPECTION("Inspect", Icons.Default.QrCodeScanner),
    GEMINI("Safety AI", Icons.Default.AutoAwesome),
    REPORTS("Reports", Icons.Default.Assessment)
}
