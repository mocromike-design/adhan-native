package com.astronomy.quranicprayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.astronomy.quranicprayer.ui.screens.MainPrayerScreen
import com.astronomy.quranicprayer.ui.theme.QuranicPrayerTheme
import com.astronomy.quranicprayer.ui.viewmodel.PrayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PrayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel.loadSchedule()

        setContent {
            QuranicPrayerTheme {
                val uiState by viewModel.uiState.collectAsState()
                MainPrayerScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.loadSchedule() }
                )
            }
        }
    }
}
