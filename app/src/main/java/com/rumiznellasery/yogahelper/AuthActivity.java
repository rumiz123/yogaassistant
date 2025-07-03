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

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText emailEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        Button signInButton = findViewById(R.id.buttonSignIn);
        Button signUpButton = findViewById(R.id.buttonSignUp);

        signInButton.setOnClickListener(v -> signIn());
        signUpButton.setOnClickListener(v -> signUp());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) {
            startMain();
        }
    }

    private void signIn() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            DbKeys keys = DbKeys.get(this);
                            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                                    .getReference(keys.users)
                                    .child(user.getUid());
                            ref.child(keys.displayName).get().addOnCompleteListener(r -> {
                                if (!r.isSuccessful() || r.getResult() == null || !r.getResult().exists()) {
                                    String display = user.getDisplayName() == null ? "" : user.getDisplayName();
                                    ref.child(keys.displayName).setValue(display);
                                    ref.child(keys.workouts).setValue(0);
                                    ref.child(keys.totalWorkouts).setValue(0);
                                    ref.child(keys.calories).setValue(0);
                                    ref.child(keys.streak).setValue(0);
                                    ref.child(keys.score).setValue(0);
                                    ref.child(keys.level).setValue(1);
                                }
                            });
                        }
                        startMain();
                    } else {
                        Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void signUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            return;
        }
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
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

                            user.sendEmailVerification().addOnCompleteListener(vt -> {
                                if (vt.isSuccessful()) {
                                    Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        // Navigate to onboarding instead of main activity
                        startActivity(new Intent(AuthActivity.this, OnboardingActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Sign up failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
