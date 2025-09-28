// ui/components/ride/RideGraph.kt
@Composable
fun RideGraph(isRiding: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isRiding) {
                Text(
                    text = "📈 Graphique temps réel\n(en cours de trajet)",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Blue
                )
            } else {
                Text(
                    text = "📈 Graphique temps réel\n(démarrez un trajet)",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    }
}