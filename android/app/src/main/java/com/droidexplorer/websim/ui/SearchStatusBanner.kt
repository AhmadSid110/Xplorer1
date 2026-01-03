package com.droidexplorer.websim.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidexplorer.websim.search.SearchRoot

@Composable
fun SearchStatusBanner(skippedRoots: List<SearchRoot>) {
    if (skippedRoots.isEmpty()) return

    Surface(
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = "Some locations were not searched due to missing permissions",
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}
