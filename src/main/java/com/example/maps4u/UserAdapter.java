package com.example.maps4u;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> userList;
    private boolean showPasswords; // Flag pentru a afișa sau ascunde parolele

    public UserAdapter(List<User> userList, boolean showPasswords) {
        this.userList = userList;
        this.showPasswords = showPasswords;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textUsername.setText("Username: " + user.getUsername());

        if (showPasswords) {
            holder.textPassword.setText("Password: " + user.getPassword());
        } else {
            holder.textPassword.setText("Password: " + user.getPassword().replaceAll(".", "•")); // Cenzurare
        }

        holder.textRole.setText("Role: " + user.getRole());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textUsername, textPassword, textRole;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.textUsername);
            textPassword = itemView.findViewById(R.id.textPassword);
            textRole = itemView.findViewById(R.id.textRole);
        }
    }
}