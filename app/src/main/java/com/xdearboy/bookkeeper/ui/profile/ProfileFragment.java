package com.xdearboy.bookkeeper.ui.profile;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.repository.UserRepository;
public class ProfileFragment extends Fragment {
    private TextView nameTextView, emailTextView;
    private Button editProfileButton;
    private UserRepository userRepository;
    private User currentUser;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_profile_card, container, false);
        nameTextView = view.findViewById(R.id.profile_name);
        emailTextView = view.findViewById(R.id.profile_email);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        userRepository = UserRepository.getInstance(requireActivity().getApplication());
        // TODO: Replace with actual user id from session/auth
        String userId = userRepository.getCurrentUser() != null ? userRepository.getCurrentUser().getId() : null;
        if (userId != null) {
            userRepository.getUserById(userId).observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    currentUser = user;
                    nameTextView.setText(user.getName());
                    emailTextView.setText(user.getEmail());
                }
            });
        }
        editProfileButton.setOnClickListener(v -> {
            if (currentUser != null) {
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                intent.putExtra("USER_ID", currentUser.getId());
                startActivity(intent);
            }
        });
        return view;
    }
}