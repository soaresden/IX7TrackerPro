// ui/components/ride/RideModeSelector.kt
@Composable
fun RideModeSelector(
    currentMode: com.ix7.tracker.ui.screens.RideMode,
    onModeChange: (com.ix7.tracker.ui.screens.RideMode) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Mode de conduite",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                com.ix7.tracker.ui.screens.RideMode.values().forEach { mode ->
                    Button(
                        onClick = { onModeChange(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == mode) Color.Yellow else Color.Gray
                        ),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = mode.name.take(3),
                            fontSize = 12.sp,
                            fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}