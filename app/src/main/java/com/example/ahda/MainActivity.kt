package com.example.ahda

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

data class Expense(val id: String = UUID.randomUUID().toString(), val description: String, val amount: Double)
data class Person(val id: String = UUID.randomUUID().toString(), val name: String, val allowance: Double, val expenses: List<Expense> = emptyList())

object Storage {
    private const val PREFS = "ahda_prefs"
    private const val KEY = "people_data"

    fun save(context: Context, people: List<Person>) {
        val arr = JSONArray()
        for (p in people) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("allowance", p.allowance)
            val expArr = JSONArray()
            for (e in p.expenses) {
                val eObj = JSONObject()
                eObj.put("id", e.id)
                eObj.put("description", e.description)
                eObj.put("amount", e.amount)
                expArr.put(eObj)
            }
            obj.put("expenses", expArr)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun load(context: Context): List<Person> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Person>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val expArr = obj.getJSONArray("expenses")
                val expenses = mutableListOf<Expense>()
                for (j in 0 until expArr.length()) {
                    val eObj = expArr.getJSONObject(j)
                    expenses.add(
                        Expense(
                            id = eObj.optString("id", UUID.randomUUID().toString()),
                            description = eObj.getString("description"),
                            amount = eObj.getDouble("amount")
                        )
                    )
                }
                list.add(
                    Person(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        allowance = obj.getDouble("allowance"),
                        expenses = expenses
                    )
                )
            }
            list
        } catch (ex: Exception) {
            emptyList()
        }
    }
}

private val TealPrimary = Color(0xFF00695C)
private val TealDark = Color(0xFF004D40)
private val Gold = Color(0xFFFFA000)
private val BgLight = Color(0xFFF3F6F5)
private val DangerRed = Color(0xFFD32F2F)
private val SuccessGreen = Color(0xFF2E7D32)

private val AhdaColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = TealDark,
    secondary = Gold,
    onSecondary = Color.White,
    background = BgLight,
    onBackground = Color(0xFF1B1B1B),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE0F2F1),
    error = DangerRed
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AhdaApp() }
    }
}

@Composable
fun AhdaApp() {
    val context = LocalContext.current
    var people by remember { mutableStateOf(Storage.load(context)) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    fun updatePeople(newList: List<Person>) {
        people = newList
        Storage.save(context, newList)
    }

    MaterialTheme(colorScheme = AhdaColorScheme) {
        CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            val selectedPerson = people.find { it.id == selectedId }
            if (selectedPerson != null) {
                PersonScreen(
                    person = selectedPerson,
                    onBack = { selectedId = null },
                    onUpdate = { updated ->
                        updatePeople(people.map { if (it.id == updated.id) updated else it })
                    }
                )
            } else {
                HomeScreen(
                    people = people,
                    onAdd = { showAdd = true },
                    onSelect = { selectedId = it.id }
                )
                if (showAdd) {
                    AddPersonDialog(
                        onDismiss = { showAdd = false },
                        onAdd = { p ->
                            updatePeople(people + p)
                            showAdd = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(people: List<Person>, onAdd: () -> Unit, onSelect: (Person) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("عُهَد", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.secondary) {
                Text("+", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
            val total = people.sumOf { it.allowance }
            val spent = people.sumOf { p -> p.expenses.sumOf { it.amount } }
            val remaining = total - spent

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("ملخص العهد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    SummaryRow("إجمالي العهد", total)
                    SummaryRow("إجمالي المصروف", spent)
                    SummaryRow("المتبقي", remaining, highlight = true)
                }
            }

            Spacer(Modifier.height(20.dp))

            if (people.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد عهد مسجلة. اضغط + لإضافة شخص.", color = Color.Gray)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(people) { p ->
                    val spentP = p.expenses.sumOf { it.amount }
                    val remain = p.allowance - spentP
                    Card(
                        onClick = { onSelect(p) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("العهدة: ${money(p.allowance)} ريال", color = Color.Gray)
                            Text("المصروف: ${money(spentP)} ريال", color = Color.Gray)
                            Text(
                                "المتبقي: ${money(remain)} ريال",
                                color = if (remain >= 0) SuccessGreen else DangerRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: Double, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TealDark)
        Text(
            "${money(value)} ريال",
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) { if (value >= 0) SuccessGreen else DangerRed } else TealDark
        )
    }
}

@Composable
fun AddPersonDialog(onDismiss: () -> Unit, onAdd: (Person) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عهدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم الشخص") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it }, label = { Text("مبلغ العهدة") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && amount.toDoubleOrNull() != null) {
                    onAdd(Person(name = name, allowance = amount.toDouble()))
                }
            }) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(person: Person, onBack: () -> Unit, onUpdate: (Person) -> Unit) {
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    val spent = person.expenses.sumOf { it.amount }
    val remain = person.allowance - spent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person.name, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("رجوع", color = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    SummaryRow("العهدة", person.allowance)
                    SummaryRow("المصروف", spent)
                    SummaryRow("المتبقي", remain, highlight = true)
                }
            }

            HorizontalDivider()

            OutlinedTextField(desc, { desc = it }, label = { Text("وصف المصروف") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(amount, { amount = it }, label = { Text("مبلغ المصروف") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    val a = amount.toDoubleOrNull()
                    if (desc.isNotBlank() && a != null) {
                        onUpdate(person.copy(expenses = person.expenses + Expense(description = desc, amount = a)))
                        desc = ""; amount = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("تسجيل المصروف") }

            Text("المصروفات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(person.expenses) { e ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(e.description, fontWeight = FontWeight.Medium)
                                Text("${money(e.amount)} ريال", color = Color.Gray)
                            }
                            Row {
                                IconButton(onClick = { editingExpense = e }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = TealDark)
                                }
                                IconButton(onClick = {
                                    onUpdate(person.copy(expenses = person.expenses.filter { it.id != e.id }))
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (editingExpense != null) {
            EditExpenseDialog(
                expense = editingExpense!!,
                onDismiss = { editingExpense = null },
                onSave = { updated ->
                    onUpdate(person.copy(expenses = person.expenses.map { if (it.id == updated.id) updated else it }))
                    editingExpense = null
                }
            )
        }
    }
}

@Composable
fun EditExpenseDialog(expense: Expense, onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    var desc by remember { mutableStateOf(expense.description) }
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل المصروف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(desc, { desc = it }, label = { Text("وصف المصروف") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it }, label = { Text("مبلغ المصروف") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toDoubleOrNull()
                if (desc.isNotBlank() && a != null) {
                    onSave(expense.copy(description = desc, amount = a))
                }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

fun money(v: Double) = String.format(Locale.US, "%,.2f", v)
