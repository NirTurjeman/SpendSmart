package com.example.spendsmart

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private var selectedAvatar: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val registerButton = findViewById<MaterialButton>(R.id.register_Button)
        val emailTextInput = findViewById<EditText>(R.id.register_Input_Email)
        val passwordTextInput = findViewById<EditText>(R.id.register_Input_Password)
        val confirmPassInput = findViewById<EditText>(R.id.register_Input_ConfirmPassword)
        val usernameInput = findViewById<EditText>(R.id.register_Input_UserName)
        val signInButton = findViewById<TextView>(R.id.register_Login_Button)

        val avatarMale1 = findViewById<ImageView>(R.id.avatar_male_1)
        val avatarMale2 = findViewById<ImageView>(R.id.avatar_male_2)
        val avatarFemale1 = findViewById<ImageView>(R.id.avatar_female_1)
        val avatarFemale2 = findViewById<ImageView>(R.id.avatar_female_2)

        val avatarsMap = mapOf(
            avatarMale1 to "avatar_male_1",
            avatarMale2 to "avatar_male_2",
            avatarFemale1 to "avatar_female_1",
            avatarFemale2 to "avatar_female_2"
        )

        avatarsMap.keys.forEach { avatarView ->
            avatarView.setOnClickListener {
                avatarsMap.keys.forEach { it.setBackgroundResource(R.drawable.avatar_background) }
                avatarView.setBackgroundResource(R.drawable.avatar_selected_background)
                selectedAvatar = avatarsMap[avatarView]
            }
        }

        signInButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        registerButton.setOnClickListener {
            val userName = usernameInput.text.toString()
            val email = emailTextInput.text.toString()
            val pass = passwordTextInput.text.toString()
            val confirmPass = confirmPassInput.text.toString()
            val avatarName = selectedAvatar ?: ""

            when {
                userName.isEmpty() -> {
                    Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                }
                email.isEmpty() -> {
                    Toast.makeText(this, "Please enter an email address", Toast.LENGTH_SHORT).show()
                }
                pass.isEmpty() -> {
                    Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
                }
                confirmPass.isEmpty() -> {
                    Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
                }
                avatarName.isEmpty() -> {
                    Toast.makeText(this, "Please select an avatar", Toast.LENGTH_SHORT).show()
                }
                pass != confirmPass -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                pass.length < 6 -> {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = task.result?.user?.uid ?: return@addOnCompleteListener
                                val db = FirebaseFirestore.getInstance()
                                val userData = hashMapOf(
                                    "username" to userName,
                                    "email" to email,
                                    "avatarName" to avatarName
                                )

                                db.collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Registration complete", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, DashBoardActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                val error = task.exception?.message ?: "Sign-up error"
                                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }
}
