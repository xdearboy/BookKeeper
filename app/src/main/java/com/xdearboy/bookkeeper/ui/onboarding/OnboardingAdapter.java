package com.xdearboy.bookkeeper.ui.onboarding;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.xdearboy.bookkeeper.R;
import java.util.List;
public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {
    private final List<OnboardingItem> onboardingItems;
    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }
    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OnboardingViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_onboarding, parent, false
                )
        );
    }
    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(onboardingItems.get(position));
    }
    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }
    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView quoteTextView;
        private final TextView descriptionTextView;
        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.onboarding_image);
            quoteTextView = itemView.findViewById(R.id.onboarding_quote);
            descriptionTextView = itemView.findViewById(R.id.onboarding_description);
        }
        void bind(OnboardingItem onboardingItem) {
            imageView.setImageResource(onboardingItem.getImageResId());
            quoteTextView.setText(onboardingItem.getQuoteResId());
            descriptionTextView.setText(onboardingItem.getDescriptionResId());
        }
    }
} 