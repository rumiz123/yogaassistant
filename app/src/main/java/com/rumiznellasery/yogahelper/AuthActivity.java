package com.rumiznellasery.yogahelper;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.AuthCredential;

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
    private EditText nameEditText;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("AIzaSyD4Zsoiqv36JxRKVbMif8rlVCSRchntaGY")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        com.google.android.gms.tasks.Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignIn(task);
                    }
                }
        );

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        nameEditText = findViewById(R.id.editTextDisplayName);
        Button signInButton = findViewById(R.id.buttonSignIn);
        Button signUpButton = findViewById(R.id.buttonSignUp);
        SignInButton googleButton = findViewById(R.id.buttonGoogleSignIn);

        signInButton.setOnClickListener(v -> signIn());
        signUpButton.setOnClickListener(v -> signUp());
        googleButton.setOnClickListener(v -> googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
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

    private void handleGoogleSignIn(com.google.android.gms.tasks.Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null) {
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
                return;
            }
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
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
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (ApiException e) {
            Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void signUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            return;
        }
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            if (!name.isEmpty()) {
                                UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();
                                user.updateProfile(req);
                                DbKeys keys = DbKeys.get(this);
                                DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                                        .getReference(keys.users)
                                        .child(user.getUid());
                                ref.child(keys.displayName).setValue(name);
                                ref.child(keys.workouts).setValue(0);
                                ref.child(keys.totalWorkouts).setValue(0);
                                ref.child(keys.calories).setValue(0);
                                ref.child(keys.streak).setValue(0);
                                ref.child(keys.score).setValue(0);
                                ref.child(keys.level).setValue(1);
                            }

                            user.sendEmailVerification().addOnCompleteListener(vt -> {
                                if (vt.isSuccessful()) {
                                    Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        startMain();
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
