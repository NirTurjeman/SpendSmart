package com.example.spendsmart

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private var selectedAvatar: String? = null
private var avatarDrawable: Int = R.drawable.ic_launcher_background
private val avatarsMapStringToDrawable = mapOf(
    "avatar_male_1" to R.drawable.avatar_male_1,
    "avatar_male_2" to R.drawable.avatar_male_2,
    "avatar_female_1" to R.drawable.avatar_female_1,
    "avatar_female_2" to R.drawable.avatar_female_2
)
private lateinit var username: String

class SettingsActivity : AppCompatActivity() {
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val scannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            lifecycleScope.launch {
                linkWithPartner(result.contents)
            }
        } else {
            Toast.makeText(this, "Scan canceled", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val returnButton = findViewById<ImageView>(R.id.settings_return_icon)
        val usernameInput = findViewById<EditText>(R.id.settings_edit_username)
        val avatarMale1 = findViewById<ImageView>(R.id.avatar_male_1)
        val avatarMale2 = findViewById<ImageView>(R.id.avatar_male_2)
        val avatarFemale1 = findViewById<ImageView>(R.id.avatar_female_1)
        val avatarFemale2 = findViewById<ImageView>(R.id.avatar_female_2)
        val saveButton = findViewById<Button>(R.id.settings_save_button)
        val linkWithPartnerButton = findViewById<Button>(R.id.settings_link_partner_button)

        returnButton.setOnClickListener {
            val intent = Intent(this, DashBoardActivity::class.java)
            startActivity(intent)
            finish()
        }

        val avatarsMapImageViewToString = mapOf(
            avatarMale1 to "avatar_male_1",
            avatarMale2 to "avatar_male_2",
            avatarFemale1 to "avatar_female_1",
            avatarFemale2 to "avatar_female_2"
        )

        avatarsMapImageViewToString.keys.forEach { avatarView ->
            avatarView.setOnClickListener {
                if (selectedAvatar == avatarsMapImageViewToString[avatarView]) {
                    avatarView.setBackgroundResource(R.drawable.avatar_background)
                    selectedAvatar = null
                } else {
                    avatarsMapImageViewToString.keys.forEach { it.setBackgroundResource(R.drawable.avatar_background) }
                    avatarView.setBackgroundResource(R.drawable.avatar_selected_background)
                    selectedAvatar = avatarsMapImageViewToString[avatarView]
                }
            }
        }

        lifecycleScope.launch {
            loadUserInfo(usernameInput, avatarsMapImageViewToString)
        }

        saveButton.setOnClickListener {
            val newUsername = usernameInput.text.toString().trim()
            if (newUsername.isNotEmpty() && selectedAvatar != null) {
                lifecycleScope.launch {
                    if (isUsernameAvailable(newUsername)) {
                        saveChanges(newUsername, selectedAvatar!!)
                    } else {
                        Toast.makeText(this@SettingsActivity, "Username is already taken", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@SettingsActivity, "Please enter a username and select an avatar", Toast.LENGTH_SHORT).show()
            }
        }

        linkWithPartnerButton.setOnClickListener {
            showLinkOptionsDialog()
        }
    }

    private fun showLinkOptionsDialog() {
        val options = arrayOf("Show QR Code", "Scan QR Code")
        AlertDialog.Builder(this)
            .setTitle("Link with Partner")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showQRCode()
                    1 -> startQRCodeScanner()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showQRCode() {
        userId?.let {
            val qrCodeBitmap = generateQRCode(it)
            val dialogBuilder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_qr_code, null)
            dialogBuilder.setView(dialogView)

            val qrImageView = dialogView.findViewById<ImageView>(R.id.qr_code_image)
            qrImageView.setImageBitmap(qrCodeBitmap)

            dialogBuilder.setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }

            val alertDialog = dialogBuilder.create()
            alertDialog.show()
        }
    }


    private fun startQRCodeScanner() {
        val options = ScanOptions().apply {
            setPrompt("Scan the QR code")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setBarcodeImageEnabled(true)
        }
        scannerLauncher.launch(options)
    }

    private suspend fun linkWithPartner(partnerId: String) {
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            try {
                db.collection("users").document(userId)
                    .update("partners", FieldValue.arrayUnion(partnerId))
                    .await()

                db.collection("users").document(partnerId)
                    .update("partners", FieldValue.arrayUnion(userId))
                    .await()

                Toast.makeText(this, "Partner linked successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w("Firebase", "Error linking partner", e)
                Toast.makeText(this, "Failed to link partner", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun generateQRCode(userId: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(userId, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private suspend fun isUsernameAvailable(newUsername: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        val querySnapshot = db.collection("users")
            .whereEqualTo("username", newUsername)
            .get()
            .await()

        return querySnapshot.isEmpty || (querySnapshot.documents.firstOrNull()?.id == userId)
    }

    private suspend fun saveChanges(newUsername: String, avatarName: String) {
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            val userDocument = db.collection("users").document(userId)
            try {
                userDocument.update("username", newUsername, "avatarName", avatarName).await()
                Toast.makeText(this, "Changes saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w("Firebase", "Error saving changes", e)
                Toast.makeText(this, "Failed to save changes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loadUserInfo(usernameInput: EditText, avatarsMapImageViewToString: Map<ImageView, String>) {
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            val userDocument = db.collection("users").document(userId)
            try {
                val documentSnapshot = userDocument.get().await()
                val avatarName = documentSnapshot.getString("avatarName")
                val userName = documentSnapshot.getString("username").orEmpty()

                avatarDrawable = avatarsMapStringToDrawable[avatarName] ?: R.drawable.ic_launcher_background
                username = userName
                usernameInput.setText(username)

                avatarsMapImageViewToString.forEach { (avatarView, name) ->
                    if (name == avatarName) {
                        avatarView.setBackgroundResource(R.drawable.avatar_selected_background)
                        selectedAvatar = avatarName
                    } else {
                        avatarView.setBackgroundResource(R.drawable.avatar_background)
                    }
                }

                Log.d("Avatar Load", "Loaded avatarDrawable: $avatarDrawable, username: $username")
            } catch (e: Exception) {
                Log.w("Firebase", "Error loading avatar", e)
            }
        }
    }
}
