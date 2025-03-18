package com.xdearboy.bookkeeper.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.model.Notification;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<Notification> notifications;
    private final Context context;
    private final OnNotificationClickListener listener;
    private final SimpleDateFormat dateFormat;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notifications, OnNotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateNotifications(List<Notification> newNotifications) {
        notifications.clear();
        notifications.addAll(newNotifications);
        notifyDataSetChanged();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView notificationIcon;
        private final TextView notificationTitle;
        private final TextView notificationMessage;
        private final TextView notificationDate;
        private final View unreadIndicator;
        private final MaterialButton markAsReadButton;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationIcon = itemView.findViewById(R.id.notification_icon);
            notificationTitle = itemView.findViewById(R.id.notification_title);
            notificationMessage = itemView.findViewById(R.id.notification_message);
            notificationDate = itemView.findViewById(R.id.notification_date);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
            markAsReadButton = itemView.findViewById(R.id.mark_as_read_button);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNotificationClick(notifications.get(position));
                }
            });

            markAsReadButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNotificationClick(notifications.get(position));
                }
            });
        }

        public void bind(Notification notification) {
            notificationTitle.setText(notification.getTitle());
            notificationMessage.setText(notification.getMessage());
            
            if (notification.getCreatedAt() != null) {
                notificationDate.setText(dateFormat.format(notification.getCreatedAt()));
            }

            // Устанавливаем иконку в зависимости от типа уведомления
            switch (notification.getType()) {
                case RETURN_REMINDER:
                    notificationIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                    break;
                case RECOMMENDATION:
                    notificationIcon.setImageResource(android.R.drawable.ic_dialog_info);
                    break;
                case NEW_BOOK:
                    notificationIcon.setImageResource(android.R.drawable.ic_dialog_email);
                    break;
            }

            // Показываем индикатор непрочитанного уведомления
            if (notification.isRead()) {
                unreadIndicator.setVisibility(View.INVISIBLE);
                markAsReadButton.setVisibility(View.GONE);
            } else {
                unreadIndicator.setVisibility(View.VISIBLE);
                markAsReadButton.setVisibility(View.VISIBLE);
            }
        }
    }
} 