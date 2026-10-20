package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.AppTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onOpenAiAdvisor: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Pockethome") },
        navigationIcon = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AppTab.values().forEach { tab ->
                    DropdownMenuItem(
                        text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            expanded = false
                            onTabSelected(tab)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("AI Advisor") },
                    onClick = {
                        expanded = false
                        onOpenAiAdvisor()
                    }
                )
            }
        }
    )
}
