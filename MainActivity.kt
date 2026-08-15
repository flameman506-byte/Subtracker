package com.subtracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager = BillingManager(applicationContext)
        billingManager.startConnection { }

        val db = AppDatabase.getInstance(applicationContext)

        setContent {
            MaterialTheme {
                SubTrackerApp(dao = db.subscriptionDao(), billingManager = billingManager, activity = this)
            }
        }
    }
}

@Composable
fun SubTrackerApp(dao: SubscriptionDao, billingManager: BillingManager, activity: ComponentActivity) {
    val scope = rememberCoroutineScope()
    val subscriptions by dao.getAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val isPremium by billingManager.isPremium.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val monthlyTotal = subscriptions.sumOf {
        if (it.billingCycle == BillingCycle.MONTHLY) it.cost else it.cost / 12
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (Entitlements.canAddSubscription(subscriptions.size, isPremium)) {
                    showAddDialog = true
                } else {
                    // Hit the free tier limit — this is the paywall moment.
                    showPaywall = true
                }
            }) { Text("+") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Monthly spend: $${"%.2f".format(monthlyTotal)}", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (isPremium) "Premium — unlimited subscriptions"
                else "${subscriptions.size}/${Entitlements.FREE_SUBSCRIPTION_LIMIT} subscriptions (free tier)",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(subscriptions) { sub ->
                    ListItem(
                        headlineContent = { Text(sub.name) },
                        supportingContent = { Text("$${sub.cost} / ${sub.billingCycle.name.lowercase()}") }
                    )
                    Divider()
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, cost, cycle ->
                scope.launch {
                    dao.insert(
                        Subscription(
                            name = name,
                            cost = cost,
                            billingCycle = cycle,
                            nextRenewalEpochDay = java.time.LocalDate.now().plusMonths(1).toEpochDay()
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
            dismissButton = {
                TextButton(onClick = { showPaywall = false }) { Text("Not now") }
            }
        )
    }
}

@Composable
fun AddSubscriptionDialog(onDismiss: () -> Unit, onAdd: (String, Double, BillingCycle) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf(BillingCycle.MONTHLY) }

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
            }
        },
        confirmButton = {
            Button(onClick = {
                val costValue = cost.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && costValue > 0) onAdd(name, costValue, cycle)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
