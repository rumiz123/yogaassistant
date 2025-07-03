package com.rumiznellasery.yogahelper.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.DbKeys;

public class SettingsFragment extends Fragment {
    
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // prepare the image-picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) updateProfilePicture(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // Back button
        ImageView backButton = view.findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        
        // Profile picture
        ImageView profilePic = view.findViewById(R.id.settings_profile_pic);
        
        // Load existing profile picture
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Uri photoUri = user.getPhotoUrl();
            if (photoUri != null) {
                profilePic.setImageURI(photoUri);
            }
        }
        
        // Profile picture click to change
        profilePic.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.setType("image/*");
            pickImageLauncher.launch(pick);
        });
        
        return view;
    }
    
    private void updateProfilePicture(Uri uri) {
        FirebaseUser curr = FirebaseAuth.getInstance().getCurrentUser();
        if (curr == null) return;

        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();

        curr.updateProfile(req).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ImageView profilePic = getView().findViewById(R.id.settings_profile_pic);
                if (profilePic != null) {
                    profilePic.setImageURI(uri);
                }
                // save URL in Realtime DB if you like:
                DbKeys keys = DbKeys.get(requireContext());
                DatabaseReference ref = FirebaseDatabase
                        .getInstance(keys.databaseUrl)
                        .getReference(keys.users)
                        .child(curr.getUid());
                ref.child("photoUrl").setValue(uri.toString());

                Toast.makeText(requireContext(),
                        "Profile picture updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),
                        "Failed to update profile picture", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 