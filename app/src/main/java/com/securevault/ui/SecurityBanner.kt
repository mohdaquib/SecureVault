package com.securevault.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SecurityBanner(status: SecurityStatus) {
    val text =
        when (status) {
            SecurityStatus.Loading -> "Verifying Secure connection..."
            SecurityStatus.Secure -> "\uD83D\uDD12 Secure connection verified"
            SecurityStatus.Blocked -> "❌ Connection blocked (TLS validation failed)"
        }

    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}

@Preview
@Composable
private fun SecurityBannerPreview() {
    SecurityBanner(status = SecurityStatus.Secure)
}