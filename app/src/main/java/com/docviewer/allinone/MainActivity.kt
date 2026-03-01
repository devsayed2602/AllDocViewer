package com.docviewer.allinone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docviewer.allinone.ui.navigation.AppNavigation
import com.docviewer.allinone.ui.screen.PermissionScreen
import com.docviewer.allinone.ui.screen.checkAllFilesPermission
import com.docviewer.allinone.ui.theme.DocViewerTheme
import com.docviewer.allinone.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            var hasPermission by remember { mutableStateOf(checkAllFilesPermission()) }

            DocViewerTheme(darkTheme = isDarkMode) {
                if (hasPermission) {
                    AppNavigation()
                } else {
                    PermissionScreen(
                        onPermissionGranted = {
                            hasPermission = true
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission when returning from Settings
    }
}
