package com.buztrack.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.More
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buztrack.app.ui.theme.PrimaryGreen
import com.buztrack.app.ui.theme.TextSecondary

enum class NavTab(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Default.Home),
    TRANSACTIONS("transactions", "History", Icons.AutoMirrored.Filled.ReceiptLong),
    SCAN("scan", "Scan Bill", Icons.Default.QrCodeScanner),
    REPORTS("reports", "Reports", Icons.Default.BarChart),
    MORE("more", "More", Icons.AutoMirrored.Filled.More)
}

@Composable
fun BuztrackBottomNav(
    currentRoute: String,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp),
        color = Color.White,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.values().forEach { tab ->
                if (tab == NavTab.SCAN) {
                    // Center prominent Scan Bill FAB button
                    FloatingActionButton(
                        onClick = { onTabSelected(tab) },
                        shape = CircleShape,
                        containerColor = PrimaryGreen,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = "Scan Bill",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    val isSelected = currentRoute.startsWith(tab.route)
                    val contentColor = if (isSelected) PrimaryGreen else TextSecondary

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTabSelected(tab) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
