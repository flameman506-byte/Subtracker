package com.subtracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager = BillingManager(applicationContext)
        billingManager.startConnection { }

        val db = AppDatabase.getInstance(applicationContext)

        setContent {
            SubTrackerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SubTrackerApp(dao = db.subscriptionDao(), billingManager = billingManager, authManager = authManager, activity = this)
                }
            }
        }
    }
}

private enum class Tab(val label: String) { HOME("Home"), SUBSCRIPTIONS("Subscriptions"), INSIGHTS("Insights"), ACCOUNT("Account") }

@Composable
fun SubTrackerApp(dao: SubscriptionDao, billingManager: BillingManager, authManager: AuthManager, activity: ComponentActivity) {
    val scope = rememberCoroutineScope()
    val subscriptions by dao.getAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val isPremium by billingManager.isPremium.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(Tab.HOME) }

    val monthlyTotal = subscriptions.sumOf {
        if (it.billingCycle == BillingCycle.MONTHLY) it.cost else it.cost / 12
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab == Tab.HOME || selectedTab == Tab.SUBSCRIPTIONS) {
                FloatingActionButton(
                    onClick = {
                        if (Entitlements.canAddSubscription(subscriptions.size, isPremium)) showAddDialog = true
                        else showPaywall = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Text("+", color = Color.White) }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == Tab.HOME,
                    onClick = { selectedTab = Tab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.SUBSCRIPTIONS,
                    onClick = { selectedTab = Tab.SUBSCRIPTIONS },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Subscriptions") },
                    label = { Text("Subscriptions") }
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.INSIGHTS,
                    onClick = { selectedTab = Tab.INSIGHTS },
                    icon = { Icon(Icons.Filled.Info, contentDescription = "Insights") },
                    label = { Text("Insights") }
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.ACCOUNT,
                    onClick = { selectedTab = Tab.ACCOUNT },
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Account") },
                    label = { Text("Account") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                Tab.HOME -> HomeScreen(subscriptions, monthlyTotal, isPremium)
                Tab.SUBSCRIPTIONS -> SubscriptionsScreen(subscriptions)
                Tab.INSIGHTS -> InsightsScreen(subscriptions)
                Tab.ACCOUNT -> AccountScreen(authManager)
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, cost, cycle, category ->
                scope.launch {
                    dao.insert(
                        Subscription(
                            name = name,
                            cost = cost,
                            billingCycle = cycle,
                            nextRenewalEpochDay = java.time.LocalDate.now().plusMonths(1).toEpochDay(),
                            category = category
                        )
                    )
                }
                showAddDialog = false
            }
        )
    }

    if (showPaywall) {
        AlertDialog(
            onDismissRequest = { showPaywall = false },
            title = { Text("Upgrade to Premium") },
            text = { Text("You've reached the free limit of ${Entitlements.FREE_SUBSCRIPTION_LIMIT} subscriptions. Go premium for unlimited tracking, spending charts, and CSV export.") },
            confirmButton = {
                Button(onClick = {
                    billingManager.launchPurchaseFlow(activity, "premium_monthly")
                    showPaywall = false
                }) { Text("Upgrade — $2.99/mo") }
            },
            dismissButton = { TextButton(onClick = { showPaywall = false }) { Text("Not now") } }
        )
    }
}

@Composable
fun HomeScreen(subscriptions: List<Subscription>, monthlyTotal: Double, isPremium: Boolean) {
    val upcoming = subscriptions.sortedBy { it.nextRenewalEpochDay }.take(3)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium, color = Color.White)
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Monthly Cost", color = SubTrackerTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$" + "%.2f".format(monthlyTotal), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Medium)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip("Active", subscriptions.size.toString(), SubTrackerBlue, Modifier.weight(1f))
                StatChip(
                    if (isPremium) "Plan" else "Free limit",
                    if (isPremium) "Premium" else subscriptions.size.toString() + "/" + Entitlements.FREE_SUBSCRIPTION_LIMIT.toString(),
                    SubTrackerPurple,
                    Modifier.weight(1f)
                )
            }
        }
        item {
            Text("Upcoming Renewals", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Medium)
        }
        if (upcoming.isEmpty()) {
            item {
                Text("No subscriptions yet — tap + to add one.", color = SubTrackerTextSecondary)
            }
        }
        items(upcoming) { sub ->
            SubscriptionRow(sub)
        }
    }
}

@Composable
fun StatChip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = SubTrackerTextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SubscriptionsScreen(subscriptions: List<Subscription>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Subscriptions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        if (subscriptions.isEmpty()) {
            Text("No subscriptions yet — tap + to add one.", color = SubTrackerTextSecondary)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(subscriptions) { sub -> SubscriptionRow(sub) }
            }
        }
    }
}

@Composable
fun SubscriptionRow(sub: Subscription) {
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
            Column {
                Text(sub.name, color = Color.White, fontWeight = FontWeight.Medium)
                Text(sub.billingCycle.name.lowercase().replaceFirstChar { it.uppercase() }, color = SubTrackerTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Text("$" + sub.cost.toString(), color = SubTrackerGreen, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = SubTrackerTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun AddSubscriptionDialog(onDismiss: () -> Unit, onAdd: (String, Double, BillingCycle, Category) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
    var category by remember { mutableStateOf(Category.OTHER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add subscription") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost") })
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = cycle == BillingCycle.MONTHLY, onClick = { cycle = BillingCycle.MONTHLY }, label = { Text("Monthly") })
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = cycle == BillingCycle.YEARLY, onClick = { cycle = BillingCycle.YEARLY }, label = { Text("Yearly") })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Category", style = MaterialTheme.typography.bodySmall, color = SubTrackerTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Category.values().forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val costValue = cost.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && costValue > 0) onAdd(name, costValue, cycle, category)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
