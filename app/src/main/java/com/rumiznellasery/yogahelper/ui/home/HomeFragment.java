package com.rumiznellasery.yogahelper.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.AuthActivity;
import com.rumiznellasery.yogahelper.data.DbKeys;
import com.rumiznellasery.yogahelper.databinding.FragmentHomeBinding;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showLoggedOutState();
            return root;
        }

        // 1) Always reload to get up‐to‐date emailVerified flag
        user.reload()
                .addOnCompleteListener(task -> {
                    FirebaseUser fresh = FirebaseAuth.getInstance().getCurrentUser();
                    if (fresh == null) {
                        showLoggedOutState();
                        return;
                    }

                    // write the verified flag into Realtime DB
                    DbKeys keys = DbKeys.get(requireContext());
                    DatabaseReference userRef = FirebaseDatabase
                            .getInstance(keys.databaseUrl)
                            .getReference(keys.users)
                            .child(fresh.getUid());
                    boolean isVerified = fresh.isEmailVerified();
                    userRef.child(keys.emailVerified).setValue(isVerified);

                    // now update UI
                    updateStaticFields(fresh);
                    binding.textEmailVerified.setText("Verified: " + (isVerified ? "Yes" : "No"));
                    binding.buttonSendVerification.setVisibility(isVerified ? View.GONE : View.VISIBLE);

                    // load displayName → same as before
                });

        // 2) Send verification email
        binding.buttonSendVerification.setOnClickListener(v -> {
            FirebaseAuth.getInstance().getCurrentUser()
                    .sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Verification email sent to " + user.getEmail(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Failed to send verification email.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // 3) Edit name, logout… (unchanged)
        setupEditName();
        setupLogout();

        return root;
    }

    private void updateStaticFields(FirebaseUser user) {
        // displayName
        String name = user.getDisplayName();
        binding.textDisplayName.setText((name != null && !name.isEmpty()) ? name : "User");

        // UID
        binding.textUserId.setText("UID: " + user.getUid());

        // Email
        binding.textEmail.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "N/A"));

        // Provider
        List<? extends UserInfo> pd = user.getProviderData();
        String provider = pd.size() > 1
                ? pd.get(1).getProviderId()
                : (pd.isEmpty() ? "unknown" : pd.get(0).getProviderId());
        binding.textProvider.setText("Provider: " + provider);
    }

    private void showLoggedOutState() {
        binding.textDisplayName.setText("No account data");
        binding.textUserId.setText("");
        binding.textEmail.setText("");
        binding.textEmailVerified.setText("");
        binding.buttonSendVerification.setVisibility(View.GONE);
        binding.textProvider.setText("");
    }

    private void setupEditName() {
        binding.iconEditName.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder =
                    new android.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Edit Name");
            final android.widget.EditText input =
                    new android.widget.EditText(requireContext());
            input.setText(binding.textDisplayName.getText().toString());
            builder.setView(input);

            builder.setPositiveButton("Update", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (newName.isEmpty()) return;

                FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
                if (current == null) return;

                UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build();
                current.updateProfile(req).addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        binding.textDisplayName.setText(newName);
                        // update in DB
                        DbKeys keys = DbKeys.get(requireContext());
                        DatabaseReference ref = FirebaseDatabase
                                .getInstance(keys.databaseUrl)
                                .getReference(keys.users)
                                .child(current.getUid());
                        ref.child(keys.displayName).setValue(newName);
                    } else {
                        Toast.makeText(requireContext(),
                                "Could not update name.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            builder.setNegativeButton("Cancel", (d, w) -> d.cancel());
            builder.show();
        });
    }

    private void setupLogout() {
        binding.buttonLogout.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Logging out…", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(requireContext(), AuthActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
