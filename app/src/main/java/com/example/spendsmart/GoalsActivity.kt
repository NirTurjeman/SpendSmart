package com.example.spendsmart

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GoalsActivity : AppCompatActivity() {

    private lateinit var goalsContainer: LinearLayout
    private lateinit var addGoalButton: MaterialButton
    private lateinit var goalAmountInput: EditText
    private lateinit var goalDescriptionInput: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)
        loadGoalsFromFirebase()
        goalsContainer = findViewById(R.id.goals_container)
        addGoalButton = findViewById(R.id.add_goal_button)
        goalAmountInput = findViewById(R.id.goal_amount_input)
        goalDescriptionInput = findViewById(R.id.goal_description_input)
        val returnButton = findViewById<ImageView>(R.id.goal_return_icon)
        addGoalButton.setOnClickListener {
            addNewGoal()
        }
        returnButton.setOnClickListener {
            val intent = Intent(this, DashBoardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun addNewGoal() {
        if (goalAmountInput.text.toString().isEmpty() || goalDescriptionInput.text.toString().isEmpty()) {
            Toast.makeText(this, "Please fill in both the amount and description.", Toast.LENGTH_SHORT).show()
            return
        }

        val goalData = hashMapOf(
            "amount" to goalAmountInput.text.toString(),
            "description" to goalDescriptionInput.text.toString()
        )

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("goals")
                .add(goalData)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Goal added!", Toast.LENGTH_SHORT).show()
                    displayGoal(goalData["amount"] as String, goalData["description"] as String, documentReference.id)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add goal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadGoalsFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        if (userId != null) {
            // שליפת יעדים של המשתמש
            db.collection("users")
                .document(userId)
                .collection("goals")
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val amount = document.getString("amount") ?: ""
                        val description = document.getString("description") ?: ""
                        val documentId = document.id
                        displayGoal(amount, description, documentId)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load user goals: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            // שליפת פרטי הפרטנרים של המשתמש
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { userDocument ->
                    val partnerIds = userDocument.get("partners") as? List<String> // קבלת רשימת פרטנרים

                    partnerIds?.forEach { partnerId ->
                        db.collection("users")
                            .document(partnerId)
                            .get()
                            .addOnSuccessListener { partnerDocument ->
                                val partnerName = partnerDocument.getString("username") ?: "Partner"
                                val partnerAvatarName = partnerDocument.getString("avatarName")
                                val partnerAvatarDrawable = avatarsMap[partnerAvatarName] ?: R.drawable.ic_launcher_background

                                // שליפת יעדים של הפרטנר
                                db.collection("users")
                                    .document(partnerId)
                                    .collection("goals")
                                    .get()
                                    .addOnSuccessListener { partnerGoals ->
                                        for (goal in partnerGoals) {
                                            val amount = goal.getString("amount") ?: ""
                                            val description = goal.getString("description") ?: ""
                                            val documentId = goal.id

                                            // הצגת היעד של הפרטנר עם הפרטים שלו
                                            displayGoalWithPartnerDetails(amount, description, documentId, partnerName, partnerAvatarDrawable)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Failed to load partner goals: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun displayGoalWithPartnerDetails(amount: String, description: String, documentId: String, partnerName: String, partnerAvatar: Int) {

        displayGoal(amount, description, documentId)
    }


    private fun displayGoal(amount: String, description: String,documentId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val goalLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            setBackgroundResource(R.drawable.goal_background)
            setPadding(16, 16, 16, 16)
        }

        val goalAmount = TextView(this).apply {
            text = "Amount: $amount ₪"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }

        val goalDescription = TextView(this).apply {
            text = "Description: $description"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }

        val deleteButton = ImageButton(this).apply {
            setImageResource(R.drawable.delete_icon)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("goals")
                    .document(documentId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this@GoalsActivity, "Goal deleted!", Toast.LENGTH_SHORT).show()
                        goalsContainer.removeView(goalLayout)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@GoalsActivity, "Failed to delete goal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        goalLayout.addView(goalAmount)
        goalLayout.addView(goalDescription)
        goalLayout.addView(deleteButton)
        goalsContainer.addView(goalLayout, 0)
    }
}
