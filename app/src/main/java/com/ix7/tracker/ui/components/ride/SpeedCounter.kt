// ui/components/ride/SpeedCounter.kt
@Composable
fun SpeedCounter(
    speed: Float,
    speedUnit: SpeedUnit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${speed.toInt()}",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = if (speed > 0) Color.Red else Color.Gray
            )
            Text(
                text = speedUnit.name.lowercase(),
                fontSize = 18.sp,
                color = Color.Gray
            )
        }
    }
}