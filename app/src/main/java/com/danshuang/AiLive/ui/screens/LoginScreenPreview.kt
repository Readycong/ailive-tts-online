package com.danshuang.AiLive.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.danshuang.AiLive.ui.theme.AiLiveTheme

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AiLiveTheme {
        LoginScreen(onLoginSuccess = {})
    }
}

@Preview(showBackground = true, device = "id:pixel_4")
@Composable
fun LoginScreenPreviewDevice() {
    AiLiveTheme {
        LoginScreen(onLoginSuccess = {})
    }
}