package com.leonardovictormuller.napstop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leonardovictormuller.napstop.ui.home.HomeScreen
import com.leonardovictormuller.napstop.ui.home.HomeViewModel
import com.leonardovictormuller.napstop.ui.theme.NapStopTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as NapStopApplication).container

        setContent {
            NapStopTheme {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(appContainer)
                )
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
