package com.securevault.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securevault.domain.model.Note
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle(MainUiState())
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "SecureVault")
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color.Green),
            )
        },
        content = { innerPadding ->
            MainScreenContent(
                modifier = Modifier.padding(innerPadding),
                onClick = viewModel::addSampleNote,
                state = state,
            )
        },
    )
}

@Composable
private fun MainScreenContent(
    modifier: Modifier,
    state: MainUiState,
    onClick: () -> Unit,
) {
    Column(modifier = modifier.padding(16.dp)) {
        SecurityBanner(state.securityStatus)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClick) {
            Text("Add Encrypted Note")
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(state.notes) { note ->
                Text(text = note.title, style = MaterialTheme.typography.titleMedium)
                Text(text = note.content, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
private fun MainScreenContentPreview() {
    MainScreenContent(
        modifier = Modifier.fillMaxSize().padding(16.dp).background(Color.White),
        state =
            MainUiState(
                notes =
                    listOf(
                        Note(
                            title = "Secure Note",
                            content = "Stored encrypted using SQLCipher + Keystore",
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                        ),
                    ),
            ),
        onClick = {},
    )
}
