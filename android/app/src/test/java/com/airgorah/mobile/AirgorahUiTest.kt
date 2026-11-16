package com.airgorah.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.airgorah.mobile.core.common.OperationState
import com.airgorah.mobile.core.permissions.PermissionStatusSnapshot
import com.airgorah.mobile.ui.screens.DashboardScreen
import com.airgorah.mobile.ui.theme.AirgorahTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AirgorahUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboardRendersAllCoreStatusCards() {
        var labClicked = false
        composeRule.setContent {
            AirgorahTheme {
                DashboardScreen(
                    scanState = OperationState.READY,
                    statusMessage = "Ready to scan",
                    permissionStatus = PermissionStatusSnapshot(
                        hasLocationPermission = true,
                        hasNearbyWifiPermission = true,
                        isLocationServiceEnabled = true
                    ),
                    adapters = emptyList(),
                    accessPoints = emptyList(),
                    clients = emptyList(),
                    artifacts = emptyList(),
                    findings = emptyList(),
                    nativeDiagnostic = "Rust Core v0.9.0",
                    onRequestPermissions = {},
                    onTriggerScan = {},
                    onOpenSecurityLab = { labClicked = true }
                )
            }
        }

        composeRule.onNodeWithText("Airgorah Mobile").assertIsDisplayed()
        composeRule.onNodeWithText("Networks").assertIsDisplayed()
        composeRule.onNodeWithText("Security Lab").performClick()
        assert(labClicked)
    }
}
