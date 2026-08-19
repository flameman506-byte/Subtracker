package com.subtracker.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private fun colorForCategory(category: Category): Color = when (category) {
    Category.ENTERTAINMENT -> Color(0xFFE94560)
    Category.PRODUCTIVITY -> Color(0xFF4C6FFF)
    Category.UTILITIES -> Color(0xFF1DB954)
    Category.LIFESTYLE -> Color(0xFFF5A623)
    Category.OTHER -> Color(0xFF9CA3C0)
}

private fun labelForCategory(category: Category): String = when (category) {
    Category.ENTERTAINMENT -> "Entertainment"
    Category.PRODUCTIVITY -> "Productivity"
    Category.UTILITIES -> "Utilities"
    Category.LIFESTYLE -> "Lifestyle"
    Category.OTHER -> "Other"
}

private fun monthlyCost(sub: Subscription): Double =
    if (sub.billingCycle == BillingCycle.MONTHLY) sub.cost else sub.cost / 12

@Composable
fun InsightsScreen(subscriptions: List<Subscription>) {
    if (subscriptions.isEmpty()) {
        PlaceholderScreen("Insights", "Add a few subscriptions to see your spending breakdown here.")
        return
    }

    val byCategory = subscriptions.groupBy { it.category }
        .mapValues { (_, subs) -> subs.sumOf { monthlyCost(it) } }
        .filterValues { it > 0.0 }
        .toList()
        .sortedByDescending { it.second }

    val totalMonthly = subscriptions.sumOf { monthlyCost(it) }
    val priciest = subscriptions.maxByOrNull { monthlyCost(it) }
    val duplicateCategory = subscriptions.groupBy { it.category }
        .filter { (cat, subs) -> subs.size >= 2 && cat != Category.OTHER }
        .maxByOrNull { it.value.size }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium, color = Color.White)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(byCategory, totalMonthly, modifier = Modifier.size(140.dp))
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text("Spending Breakdown", color = SubTrackerTextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$" + "%.2f".format(totalMonthly) + "/mo", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        item {
            Text("By Category", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Medium)
        }

        items(byCategory) { (category, amount) ->
            CategoryRow(category, amount, totalMonthly)
        }

        item {
            Text("Calendar", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Medium)
        }

        item {
            CalendarMonthView(subscriptions)
        }

        if (priciest != null || duplicateCategory != null) {
            item {
                Text("Suggestions", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }

        if (duplicateCategory != null) {
            item {
                val (cat, subs) = duplicateCategory
                val cheapest = subs.minByOrNull { monthlyCost(it) }
                SuggestionCard(
                    "You have ${subs.size} ${labelForCategory(cat).lowercase()} subscriptions",
                    "Keeping just one instead of ${subs.joinToString(" and ") { it.name }} could save about $${"%.2f".format(subs.sumOf { monthlyCost(it) } - (cheapest?.let { monthlyCost(it) } ?: 0.0))}/mo."
                )
            }
        }

        if (priciest != null) {
            item {
                SuggestionCard(
                    "Your priciest subscription",
                    "${priciest.name} costs $${"%.2f".format(monthlyCost(priciest))}/mo \u2014 worth checking if you're still getting full use of it."
                )
            }
        }
    }
}

@Composable
private fun DonutChart(byCategory: List<Pair<Category, Double>>, total: Double, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.22f
        val diameter = size.minDimension - strokeWidth
        val topLeft = androidx.compose.ui.geometry.Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )
        var startAngle = -90f
        if (total <= 0.0) return@Canvas
        byCategory.forEach { (category, amount) ->
            val sweep = (amount / total * 360.0).toFloat()
            drawArc(
                color = colorForCategory(category),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun CategoryRow(category: Category, amount: Double, total: Double) {
    val percent = if (total > 0) (amount / total * 100).toInt() else 0
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(colorForCategory(category), shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(labelForCategory(category), color = Color.White, fontWeight = FontWeight.Medium)
            }
            Row {
                Text("$" + "%.2f".format(amount), color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Text("($percent%)", color = SubTrackerTextSecondary)
            }
        }
    }
}

@Composable
private fun SuggestionCard(title: String, message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = SubTrackerPurple, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, color = SubTrackerTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CalendarMonthView(subscriptions: List<Subscription>) {
    val today = java.time.LocalDate.now()
    val firstOfMonth = today.withDayOfMonth(1)
    val daysInMonth = firstOfMonth.lengthOfMonth()
    // Monday = 0 .. Sunday = 6, for a Monday-first grid
    val leadingBlanks = (firstOfMonth.dayOfWeek.value + 6) % 7

    val renewalDays = subscriptions
        .map { java.time.LocalDate.ofEpochDay(it.nextRenewalEpochDay) }
        .filter { it.month == today.month && it.year == today.year }
        .map { it.dayOfMonth }
        .toSet()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                firstOfMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + firstOfMonth.year,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(d, color = SubTrackerTextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            val totalCells = leadingBlanks + daysInMonth
            val rows = (totalCells + 6) / 7
            var dayCounter = 1
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cellIndex >= leadingBlanks && dayCounter <= daysInMonth) {
                                val day = dayCounter
                                val isToday = day == today.dayOfMonth
                                val hasRenewal = renewalDays.contains(day)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            when {
                                                hasRenewal -> SubTrackerPurple
                                                isToday -> SubTrackerSurfaceVariant
                                                else -> Color.Transparent
                                            },
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        day.toString(),
                                        color = if (hasRenewal) Color.White else if (isToday) Color.White else SubTrackerTextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                dayCounter++
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(SubTrackerPurple, shape = androidx.compose.foundation.shape.CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Renewal", color = SubTrackerTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
