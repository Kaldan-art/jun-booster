
package com.junbooster

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

class MainActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var status: TextView
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var layout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        showLoginScreen()
    }

    private fun createLayout() {
        layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(12, 20, 17))
        }
    }

    private fun showLoginScreen() {
        createLayout()

        val title = TextView(this).apply {
            text = "JUN BOOSTER"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        email = EditText(this).apply {
            hint = "Email address"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine(true)
        }

        password = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }

        status = TextView(this).apply {
            text = "Welcome to JUN BOOSTER"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val login = Button(this).apply {
            text = "LOGIN"
            setOnClickListener { loginUser() }
        }

        val register = Button(this).apply {
            text = "CREATE ACCOUNT"
            setOnClickListener { registerUser() }
        }

        val forgot = Button(this).apply {
            text = "FORGOT PASSWORD"
            setOnClickListener { resetPassword() }
        }

        layout.addView(title)
        layout.addView(email, fieldParams())
        layout.addView(password, fieldParams())
        layout.addView(login)
        layout.addView(register)
        layout.addView(forgot)
        layout.addView(status)

        setContentView(layout)
    }

    private fun fieldParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 12, 0, 12)
        }
    }

    private fun loginUser() {
        val userEmail = email.text.toString().trim()
        val userPassword = password.text.toString()

        if (userEmail.isEmpty() || userPassword.isEmpty()) {
            status.text = "Enter your email and password."
            return
        }

        status.text = "Logging in..."

        auth.signInWithEmailAndPassword(userEmail, userPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user != null && user.isEmailVerified) {
                        showWelcomeScreen()
                    } else {
                        user?.sendEmailVerification()
                        auth.signOut()
                        status.text =
                            "Please verify your email. Check your inbox."
                    }
                } else {
                    status.text =
                        task.exception?.localizedMessage ?: "Login failed."
                }
            }
    }

    private fun registerUser() {
        val userEmail = email.text.toString().trim()
        val userPassword = password.text.toString()

        if (userEmail.isEmpty() || userPassword.length < 6) {
            status.text =
                "Enter a valid email and a password of at least 6 characters."
            return
        }

        status.text = "Creating account..."

        auth.createUserWithEmailAndPassword(userEmail, userPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.sendEmailVerification()
                        ?.addOnCompleteListener(this) {
                            auth.signOut()
                            status.text =
                                "Account created! Verify your email before logging in."
                        }
                } else {
                    status.text =
                        task.exception?.localizedMessage ?: "Registration failed."
                }
            }
    }

    private fun resetPassword() {
        val userEmail = email.text.toString().trim()

        if (userEmail.isEmpty()) {
            status.text = "Enter your email address first."
            return
        }

        status.text = "Sending password reset email..."

        auth.sendPasswordResetEmail(userEmail)
            .addOnCompleteListener(this) { task ->
                status.text = if (task.isSuccessful) {
                    "Password reset email sent. Check your inbox."
                } else {
                    task.exception?.localizedMessage ?: "Could not send email."
                }
            }
    }

    private fun showWelcomeScreen() {
        val welcomeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(12, 20, 17))
        }

        val welcome = TextView(this).apply {
            text = "WELCOME TO JUN BOOSTER"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val logout = Button(this).apply {
            text = "LOGOUT"
            setOnClickListener {
                auth.signOut()
                showLoginScreen()
            }
        }

        welcomeLayout.addView(welcome)
        welcomeLayout.addView(logout)

        setContentView(welcomeLayout)
    }
}
