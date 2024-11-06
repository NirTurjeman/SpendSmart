package com.example.spendsmart

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

var items: MutableList<MyAdapter.Item> = mutableListOf()
lateinit var adapter: MyAdapter
private val userId = FirebaseAuth.getInstance().currentUser?.uid
private var avatarDrawable: Int = R.drawable.ic_launcher_background
private var username : String = ""
val avatarsMap = mapOf(
    "avatar_male_1" to R.drawable.avatar_male_1,
    "avatar_male_2" to R.drawable.avatar_male_2,
    "avatar_female_1" to R.drawable.avatar_female_1,
    "avatar_female_2" to R.drawable.avatar_female_2
)
class DashBoardActivity : AppCompatActivity() {
    @SuppressLint("UseCompatLoadingForDrawables", "MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        adapter = MyAdapter(mutableListOf())
        val incomeText = findViewById<TextView>(R.id.income_text)
        val expenseText = findViewById<TextView>(R.id.expense_text)
        val balanceText = findViewById<TextView>(R.id.balance_text)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            loadUserInfo()
            items = fetchRecentDataFromFirestore()
            Log.d("items", "$items")

            val totalIncome = items.filter { it.transactionType == TransactionType.INCOME }
                .sumOf { it.price.replace("₪", "").replace("+", "").toInt() }

            val totalExpense = items.filter { it.transactionType == TransactionType.EXPENSE }
                .sumOf { it.price.replace("₪", "").replace("-", "").toInt() }

            val balance = totalIncome - totalExpense

            incomeText.text = "Total Income: $totalIncome₪"
            expenseText.text = "Total Expenses: $totalExpense₪"
            balanceText.text = "Remaining Balance: $balance₪"
            adapter.setItems(items)
        }

        val addNewTrans = findViewById<MaterialButton>(R.id.dashboard_button_new_trans)
        val addNewGoal = findViewById<MaterialButton>(R.id.dashboard_button_new_goal)
        buttonClicked(addNewTrans, addNewGoal)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.overflowIcon = getDrawable(R.drawable.menu_icon)
    }
    private suspend fun loadUserInfo() {
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            val userDocument = db.collection("users").document(userId)
            try {
                val documentSnapshot = userDocument.get().await()
                val avatarName = documentSnapshot.getString("avatarName")
                val userName = documentSnapshot.getString("username").toString()
                Log.d("userName","${userName}")
                avatarDrawable = avatarsMap[avatarName] ?: R.drawable.ic_launcher_background
                username = userName
                Log.d("Avatar Load", "Loaded avatarDrawable: $avatarDrawable")
            } catch (e: Exception) {
                Log.w("Firebase", "Error loading avatar", e)
            }
        }
    }

    private fun getCurrentUserId(): String? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d("userID", "UserID: ${currentUser?.uid}")
        return currentUser?.uid
    }

    private suspend fun fetchRecentDataFromFirestore(): MutableList<MyAdapter.Item> {
        val userId = getCurrentUserId()
        Log.d("fetchData", "UserID: $userId")
        val db = FirebaseFirestore.getInstance()
        val items = mutableListOf<MyAdapter.Item>()

        if (userId != null) {
            try {
                val userTransactions = db.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()

                items.addAll(userTransactions.map { document ->
                    val item = document.toObject(MyAdapter.Item::class.java)
                    MyAdapter.Item(
                        avatar = avatarDrawable,
                        userName = username,
                        category = item.category,
                        description = item.description,
                        price = item.price,
                        date = item.date,
                        transactionType = item.transactionType
                    )
                })

                val userDocument = db.collection("users").document(userId).get().await()
                val partnerIds = userDocument.get("partners") as? List<String>

                partnerIds?.forEach { partnerId ->
                    val partnerDoc = db.collection("users").document(partnerId).get().await()
                    val partnerName = partnerDoc.getString("username") ?: "Partner"
                    val partnerAvatarName = partnerDoc.getString("avatarName")
                    val partnerAvatarDrawable = avatarsMap[partnerAvatarName] ?: R.drawable.ic_launcher_background

                    val partnerTransactions = db.collection("users")
                        .document(partnerId)
                        .collection("transactions")
                        .orderBy("date", Query.Direction.DESCENDING)
                        .limit(5)
                        .get()
                        .await()

                    items.addAll(partnerTransactions.map { document ->
                        val item = document.toObject(MyAdapter.Item::class.java)
                        MyAdapter.Item(
                            avatar = partnerAvatarDrawable,
                            userName = partnerName,
                            category = item.category,
                            description = item.description,
                            price = item.price,
                            date = item.date,
                            transactionType = item.transactionType
                        )
                    })
                }

            } catch (e: Exception) {
                Log.w("Firebase", "Error fetching transactions", e)
            }
        } else {
            Log.w("Firebase", "User is not logged in")
        }

        return items
    }

    private fun buttonClicked(addNewTrans: MaterialButton, addNewGoal: MaterialButton) {
        addNewTrans.setOnClickListener {
            val intent = Intent(this, TransactionsActivity::class.java)
            startActivity(intent)
            finish()
        }
        addNewGoal.setOnClickListener {
            val intent = Intent(this, GoalsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                startActivity(Intent(this,AboutActivity::class.java))
                true
            }
            R.id.action_transactions -> {
                startActivity(Intent(this, TransactionsActivity::class.java))
                true
            }
            R.id.action_goals -> {
                startActivity(Intent(this, GoalsActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
