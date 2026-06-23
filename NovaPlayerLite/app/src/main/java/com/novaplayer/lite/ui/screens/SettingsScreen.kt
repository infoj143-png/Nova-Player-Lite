package com.novaplayer.lite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.novaplayer.lite.R
import com.novaplayer.lite.ui.components.GlassCard
import com.novaplayer.lite.ui.components.NeonButton
import com.novaplayer.lite.ui.theme.NeonBlue

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = NeonBlue)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    Text(text = stringResource(R.string.version), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Theme Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.theme_info), style = MaterialTheme.typography.bodyMedium)
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "About", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.about_desc), style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.weight(1f))

        NeonButton(
            text = stringResource(R.string.clear_recent),
            onClick = { },
            color = Color.Red,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}
