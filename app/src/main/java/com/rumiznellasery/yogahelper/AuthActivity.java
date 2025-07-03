package com.rumiznellasery.yogahelper;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.data.DbKeys;
import com.rumiznellasery.yogahelper.utils.Logger;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText emailEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("AuthActivity onCreate started");
        
        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        Logger.info("Firebase Auth initialized");

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        Button signInButton = findViewById(R.id.buttonSignIn);
        Button signUpButton = findViewById(R.id.buttonSignUp);

        signInButton.setOnClickListener(v -> signIn());
        signUpButton.setOnClickListener(v -> signUp());
        Logger.info("AuthActivity onCreate completed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) {
            startMain();
        }
    }

    private void signIn() {
        Logger.info("Sign in attempt started");
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Logger.warn("Sign in failed: empty email or password");
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Logger.info("Sign in successful");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            Logger.info("User authenticated: " + user.getUid());
                            DbKeys keys = DbKeys.get(this);
                            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                                    .getReference(keys.users)
                                    .child(user.getUid());
                            ref.child(keys.displayName).get().addOnCompleteListener(r -> {
                                if (!r.isSuccessful() || r.getResult() == null || !r.getResult().exists()) {
                                    Logger.info("Setting up new user data");
                                    String display = user.getDisplayName() == null ? "" : user.getDisplayName();
                                    ref.child(keys.displayName).setValue(display);
                                    ref.child(keys.workouts).setValue(0);
                                    ref.child(keys.totalWorkouts).setValue(0);
                                    ref.child(keys.calories).setValue(0);
                                    ref.child(keys.streak).setValue(0);
                                    ref.child(keys.score).setValue(0);
                                    ref.child(keys.level).setValue(1);
                                } else {
                                    Logger.info("User data already exists");
                                }
                            });
                        }
                        Logger.info("Navigating to MainActivity");
                        startMain();
                    } else {
                        Logger.error("Sign in failed", task.getException());
                        Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void signUp() {
        Logger.info("Sign up attempt started");
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Logger.warn("Sign up failed: empty email or password");
            return;
        }
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Logger.info("Sign up successful");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            Logger.info("New user created: " + user.getUid());
                            DbKeys keys = DbKeys.get(this);
                            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                                    .getReference(keys.users)
                                    .child(user.getUid());
                            ref.child(keys.workouts).setValue(0);
                            ref.child(keys.totalWorkouts).setValue(0);
                            ref.child(keys.calories).setValue(0);
                            ref.child(keys.streak).setValue(0);
                            ref.child(keys.score).setValue(0);
                            ref.child(keys.level).setValue(1);
                            ref.child("developer").setValue(false);

                            user.sendEmailVerification().addOnCompleteListener(vt -> {
                                if (vt.isSuccessful()) {
                                    Logger.info("Verification email sent");
                                    Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show();
                                } else {
                                    Logger.warn("Failed to send verification email", vt.getException());
                                }
                            });
                        }
                        // Navigate to onboarding instead of main activity
                        Logger.info("Navigating to OnboardingActivity");
                        startActivity(new Intent(AuthActivity.this, OnboardingActivity.class));
                        finish();
                    } else {
                        Logger.error("Sign up failed", task.getException());
                        Toast.makeText(this, "Sign up failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
