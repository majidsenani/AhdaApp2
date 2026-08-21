package com.example.ahda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import java.util.Locale

data class Expense(val description:String, val amount:Double)
data class Person(val name:String, val allowance:Double, val expenses:List<Expense> = emptyList())

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AhdaApp() }
    }
}

@Composable
fun AhdaApp() {
    var people by remember { mutableStateOf(listOf<Person>()) }
    var selected by remember { mutableStateOf<Person?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    MaterialTheme {
        CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            if (selected != null) {
                PersonScreen(selected!!, onBack={selected=null}, onUpdate={p ->
                    people = people.map { if (it.name==p.name) p else it }
                    selected=p
                })
            } else {
                HomeScreen(people, onAdd={showAdd=true}, onSelect={selected=it})
                if(showAdd) AddPersonDialog(
                    onDismiss={showAdd=false},
                    onAdd={p -> people=people+p; showAdd=false}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(people:List<Person>, onAdd:()->Unit, onSelect:(Person)->Unit) {
    Scaffold(topBar={TopAppBar(title={Text("عُهَد")})},
        floatingActionButton={FloatingActionButton(onClick=onAdd){Text("+", style=MaterialTheme.typography.headlineMedium)}}) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
            val total = people.sumOf{it.allowance}; val spent=people.sumOf{p->p.expenses.sumOf{it.amount}}
            Text("ملخص العهد", style=MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("إجمالي العهد: ${money(total)} ريال")
            Text("إجمالي المصروف: ${money(spent)} ريال")
            Text("المتبقي: ${money(total-spent)} ريال")
            Spacer(Modifier.height(20.dp))
            if(people.isEmpty()) Text("لا توجد عهد مسجلة. اضغط + لإضافة شخص.")
            LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(people) { p ->
                    val spentP=p.expenses.sumOf{it.amount}; val remain=p.allowance-spentP
                    Card(onClick={onSelect(p)}, modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(p.name, style=MaterialTheme.typography.titleMedium)
                            Text("العهدة: ${money(p.allowance)} ريال")
                            Text("المصروف: ${money(spentP)} ريال")
                            Text("المتبقي: ${money(remain)} ريال")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddPersonDialog(onDismiss:()->Unit, onAdd:(Person)->Unit) {
    var name by remember{mutableStateOf("")}; var amount by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,
        title={Text("إضافة عهدة")},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            OutlinedTextField(name,{name=it},label={Text("اسم الشخص")})
            OutlinedTextField(amount,{amount=it},label={Text("مبلغ العهدة")})
        }},
        confirmButton={TextButton(onClick={if(name.isNotBlank() && amount.toDoubleOrNull()!=null) onAdd(Person(name,amount.toDouble()))}){Text("إضافة")}},
        dismissButton={TextButton(onClick=onDismiss){Text("إلغاء")}})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(person:Person,onBack:()->Unit,onUpdate:(Person)->Unit) {
    var desc by remember{mutableStateOf("")}; var amount by remember{mutableStateOf("")}
    val spent=person.expenses.sumOf{it.amount}; val remain=person.allowance-spent
    Scaffold(topBar={TopAppBar(title={Text(person.name)}, navigationIcon={TextButton(onClick=onBack){Text("رجوع")}})}){pad->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("العهدة: ${money(person.allowance)} ريال", style=MaterialTheme.typography.titleMedium)
            Text("المصروف: ${money(spent)} ريال")
            Text("المتبقي: ${money(remain)} ريال", style=MaterialTheme.typography.titleLarge)
            HorizontalDivider()
            OutlinedTextField(desc,{desc=it},label={Text("وصف المصروف")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(amount,{amount=it},label={Text("مبلغ المصروف")},modifier=Modifier.fillMaxWidth())
            Button(onClick={
                val a=amount.toDoubleOrNull()
                if(desc.isNotBlank() && a!=null){ onUpdate(person.copy(expenses=person.expenses+Expense(desc,a))); desc=""; amount=""}
            },modifier=Modifier.fillMaxWidth()){Text("تسجيل المصروف")}
            Text("المصروفات",style=MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){
                items(person.expenses){e-> Card(Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(e.description);Text("${money(e.amount)} ريال")}}}
            }
        }
    }
}
fun money(v:Double)=String.format(Locale.US,"%,.2f",v)
