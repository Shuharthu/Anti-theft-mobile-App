package com.mobilechor.guardian

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilechor.guardian.service.GuardianService
import com.mobilechor.guardian.ui.screens.*
import com.mobilechor.guardian.ui.theme.*
import com.mobilechor.guardian.ui.viewmodel.GuardianViewModel

enum class NavigationTab(val titleRes: Int, val icon: ImageVector) {
    DASHBOARD(R.string.nav_dashboard, Icons.Default.Shield),
    EVIDENCE(R.string.nav_evidence, Icons.Default.PhotoCamera),
    RADAR(R.string.nav_radar, Icons.Default.Radar),
    REPORT(R.string.nav_report, Icons.Default.Article),
    SETTINGS(R.string.nav_settings, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: GuardianViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start Guardian foreground service safely
        try {
            val serviceIntent = Intent(this, GuardianService::class.java).apply {
                action = GuardianService.ACTION_START_MONITORING
            }
            startForegroundService(serviceIntent)
        } catch (ignored: Exception) {}

        setContent {
            MobileChorTheme {
                val ownerProfile by viewModel.ownerProfile.collectAsState()
                var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
                val context = LocalContext.current

                if (!ownerProfile.isOnboardingComplete) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onComplete = {
                            selectedTab = NavigationTab.DASHBOARD
                        }
                    )
                } else {
                    Scaffold(
                        containerColor = CleanBg,
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = CleanBg,
                                    titleContentColor = TextPrimary
                                ),
                                title = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(RoyalBlueLight, CircleShape)
                                                    .border(1.dp, RoyalBlue.copy(alpha = 0.3f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Shield,
                                                    contentDescription = null,
                                                    tint = RoyalBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.app_name),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RoyalBlue
                                                )
                                                Text(
                                                    text = stringResource(R.string.device_owner_label),
                                                    fontSize = 11.sp,
                                                    color = TextMutedClean
                                                )
                                            }
                                        }

                                        // Bilingual language toggle (EN / বাং)
                                        Row(
                                            modifier = Modifier
                                                .background(CleanSurface, RoundedCornerShape(20.dp))
                                                .border(1.dp, CleanCardBorder, RoundedCornerShape(20.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "EN",
                                                color = if (ownerProfile.language == "en") RoyalBlue else TextMutedClean,
                                                fontWeight = if (ownerProfile.language == "en") FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                modifier = Modifier
                                                    .clickable { viewModel.setLanguage(context, "en") }
                                                    .padding(horizontal = 4.dp)
                                            )
                                            Text(text = "|", color = CleanCardBorder, fontSize = 12.sp)
                                            Text(
                                                text = "বাং",
                                                color = if (ownerProfile.language == "bn") IconOrangeTint else TextMutedClean,
                                                fontWeight = if (ownerProfile.language == "bn") FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                modifier = Modifier
                                                    .clickable { viewModel.setLanguage(context, "bn") }
                                                    .padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = CleanSurface,
                                tonalElevation = 10.dp
                            ) {
                                val evidenceList by viewModel.evidenceList.collectAsState()

                                NavigationTab.values().forEach { tab ->
                                    val isSelected = selectedTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { selectedTab = tab },
                                        icon = {
                                            if (tab == NavigationTab.EVIDENCE && evidenceList.isNotEmpty()) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(containerColor = IconOrangeTint) {
                                                            Text("${evidenceList.size}", color = CleanSurface, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                ) {
                                                    Icon(tab.icon, contentDescription = stringResource(tab.titleRes))
                                                }
                                            } else {
                                                Icon(tab.icon, contentDescription = stringResource(tab.titleRes))
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(tab.titleRes),
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) RoyalBlue else androidx.compose.ui.graphics.Color(0xFF1E293B)
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = RoyalBlue,
                                            selectedTextColor = RoyalBlue,
                                            unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF475569),
                                            unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                                            indicatorColor = RoyalBlueLight
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                NavigationTab.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToEvidence = { selectedTab = NavigationTab.EVIDENCE }
                                )
                                NavigationTab.EVIDENCE -> EvidenceGalleryScreen(viewModel = viewModel)
                                NavigationTab.RADAR -> BleFinderScreen(viewModel = viewModel)
                                NavigationTab.REPORT -> PoliceReportScreen(viewModel = viewModel)
                                NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
