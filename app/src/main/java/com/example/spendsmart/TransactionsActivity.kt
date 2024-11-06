package com.example.spendsmart

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.PopupWindow
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class TransactionsActivity : AppCompatActivity() {
    private lateinit var transactionsAdapter: MyAdapter
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var avatarDrawable: Int = R.drawable.ic_launcher_background
    private val avatarsMap = mapOf(
        "avatar_male_1" to R.drawable.avatar_male_1,
        "avatar_male_2" to R.drawable.avatar_male_2,
        "avatar_female_1" to R.drawable.avatar_female_1,
        "avatar_female_2" to R.drawable.avatar_female_2
    )

    @SuppressLint("MissingInflatedId", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { userDocument ->
                    val avatarName: String? = userDocument.getString("avatarName")
                    avatarDrawable = avatarsMap[avatarName] ?: R.drawable.ic_launcher_background
                }
        }
        loadTransactionsFromFirebase()
        setupUIComponents()
    }

    private fun setupUIComponents() {
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_transaction_options, null)
        val popupWindow = PopupWindow(popupView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = false
            isFocusable = true
        }
        val categorySpinner = popupView.findViewById<Spinner>(R.id.transactions_category_spinner)
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // Handle selection if needed
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val recyclerView = findViewById<RecyclerView>(R.id.transactions_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        transactionsAdapter = MyAdapter(mutableListOf())
        recyclerView.adapter = transactionsAdapter

        val returnButton = findViewById<ImageView>(R.id.transactions_return_icon)
        returnButton.setOnClickListener {
            val intent = Intent(this, DashBoardActivity::class.java)
            startActivity(intent)
            finish()
        }

        val addNewTransactionButton = findViewById<MaterialButton>(R.id.transactions_addNewTrans_Button)
        val cancelTransactionButton = popupView.findViewById<MaterialButton>(R.id.transactions_Cancel_Button)
        val saveTransactionButton = popupView.findViewById<MaterialButton>(R.id.transactions_Submit_Button)
        val blurOverlay = findViewById<FrameLayout>(R.id.blur_overlay)

        addNewTransactionButton.setOnClickListener {
            popupWindow.showAtLocation(it, Gravity.CENTER, 0, 0)
            blurOverlay.visibility = View.VISIBLE
            adjustPopupDim(popupWindow)
        }

        saveTransactionButton.setOnClickListener {
            val category = categorySpinner.selectedItem.toString()
            val amount = popupView.findViewById<EditText>(R.id.transactions_Price_Input).text.toString()
            val description = popupView.findViewById<EditText>(R.id.transactions_Description_Input).text.toString()
            val isIncome = popupView.findViewById<Switch>(R.id.transactions_type_switch).isChecked
            val transactionType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

            if (category.isNotEmpty() && amount.isNotEmpty() && description.isNotEmpty()) {
                addTransactionToFirebase(category, amount, description, transactionType, Date())
                popupWindow.dismiss()
                blurOverlay.visibility = View.GONE
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        cancelTransactionButton.setOnClickListener {
            popupWindow.dismiss()
            blurOverlay.visibility = View.GONE
        }

        val pageToolbar = findViewById<Toolbar>(R.id.transactions_toolbar)
        pageToolbar.inflateMenu(R.menu.menu_transactions)
        pageToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sort -> showSortMenu(findViewById(R.id.action_sort))
                else -> false
            }
        }
    }

    private fun adjustPopupDim(popupWindow: PopupWindow) {
        val container = popupWindow.contentView.parent as View
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutParams = container.layoutParams as WindowManager.LayoutParams
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        layoutParams.dimAmount = 0.8f
        windowManager.updateViewLayout(container, layoutParams)
    }

    private fun loadTransactionsFromFirebase() {
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { userDocument ->
                    val userName = userDocument.getString("username") ?: ""
                    val partnerIds = userDocument.get("partners") as? List<String> // מקבל את מערך הפרטנרים

                    val items = mutableListOf<MyAdapter.Item>()

                    // שליפת פעולות המשתמש
                    loadUserTransactions(db, userId, userName, avatarDrawable, items) {
                        transactionsAdapter.setItems(items)
                    }

                    // שליפת פעולות לכל פרטנר במערך
                    partnerIds?.forEach { partnerId ->
                        db.collection("users")
                            .document(partnerId)
                            .get()
                            .addOnSuccessListener { partnerDocument ->
                                val partnerName = partnerDocument.getString("username") ?: "Partner"
                                val partnerAvatarName = partnerDocument.getString("avatarName")
                                val partnerAvatarDrawable = avatarsMap[partnerAvatarName] ?: R.drawable.ic_launcher_background

                                loadUserTransactions(db, partnerId, partnerName, partnerAvatarDrawable, items) {
                                    transactionsAdapter.setItems(items)
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to load partner data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun loadUserTransactions(
        db: FirebaseFirestore,
        userId: String,
        userName: String,
        avatar: Int,
        items: MutableList<MyAdapter.Item>,
        onComplete: () -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .collection("transactions")
            .orderBy("date")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val category = document.getString("category") ?: ""
                    val price = document.getString("price") ?: ""
                    val description = document.getString("description") ?: ""
                    val transactionType = TransactionType.valueOf(document.getString("transactionType") ?: "EXPENSE")
                    val transDate = document.getDate("date") ?: Date()

                    val item = MyAdapter.Item(
                        userName = userName,
                        avatar = avatar,
                        category = category,
                        price = price,
                        description = description,
                        transactionType = transactionType,
                        date = transDate
                    )
                    items.add(item)
                }
                onComplete()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load transactions: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addTransactionToFirebase(category: String, amount: String, description: String, transactionType: TransactionType, date: Date) {
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { _ ->
                    val transactionData = hashMapOf(
                        "category" to category,
                        "price" to amount,
                        "description" to description,
                        "transactionType" to transactionType.name,
                        "date" to date,
                    )

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("transactions")
                        .add(transactionData)
                        .addOnSuccessListener {
                            val newItem = MyAdapter.Item(
                                avatar = avatarDrawable,
                                category = category,
                                price = amount,
                                description = description,
                                transactionType = transactionType,
                                date = date
                            )
                            transactionsAdapter.addItem(newItem)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to add transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load avatar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showSortMenu(anchor: View): Boolean {
        val sortMenu = PopupMenu(this, anchor)
        sortMenu.menuInflater.inflate(R.menu.menu_sort, sortMenu.menu)
        sortMenu.setOnMenuItemClickListener { sortItem ->
            when (sortItem.itemId) {
                R.id.sort_by_date -> {
                    items.sortBy { it.date }
                    transactionsAdapter.setItems(items)
                    true
                }
                R.id.sort_by_name -> {
                    items.sortBy { it.userName }
                    transactionsAdapter.setItems(items)
                    true
                }
                R.id.sort_by_amount -> {
                    items.sortedWith(compareBy<MyAdapter.Item> {
                        it.transactionType == TransactionType.INCOME
                    }.thenByDescending {
                        it.price.replace("₪", "").toDoubleOrNull() ?: 0.0
                    }).toMutableList().also {
                        transactionsAdapter.setItems(it)
                    }
                    true
                }

                else -> false
            }
        }
        sortMenu.show()
        return true
    }
}
