package com.xdearboy.bookkeeper.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.ui.auth.LoginActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Активность для онбординга с ViewPager2, индикаторами и кнопками
 */
public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 onboardingViewPager;
    private MaterialButton nextButton;
    private TextView skipButton;
    private ImageView[] indicators;
    private OnboardingAdapter onboardingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверяем, нужно ли показывать онбординг
        if (isOnboardingCompleted()) {
            startLoginActivity();
            return;
        }

        setContentView(R.layout.activity_onboarding);
        
        // Инициализация UI компонентов
        onboardingViewPager = findViewById(R.id.onboarding_view_pager);
        nextButton = findViewById(R.id.next_button);
        skipButton = findViewById(R.id.skip_button);
        
        // Инициализация индикаторов
        indicators = new ImageView[3];
        indicators[0] = findViewById(R.id.indicator_1);
        indicators[1] = findViewById(R.id.indicator_2);
        indicators[2] = findViewById(R.id.indicator_3);
        
        // Настройка адаптера для ViewPager2
        setupOnboardingItems();
        
        // Настройка слушателя для ViewPager2
        onboardingViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                updateNextButtonText(position);
            }
        });
        
        // Настройка слушателя для кнопки "Далее"
        nextButton.setOnClickListener(v -> {
            if (onboardingViewPager.getCurrentItem() < onboardingAdapter.getItemCount() - 1) {
                onboardingViewPager.setCurrentItem(onboardingViewPager.getCurrentItem() + 1);
            } else {
                completeOnboarding();
                startLoginActivity();
            }
        });
        
        // Настройка слушателя для кнопки "Пропустить"
        skipButton.setOnClickListener(v -> {
            completeOnboarding();
            startLoginActivity();
        });
    }
    
    /**
     * Настройка элементов онбординга
     */
    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();
        
        // Создаем элементы онбординга
        OnboardingItem item1 = new OnboardingItem(
                R.drawable.onboarding_image_1,
                R.string.onboarding_quote_1,
                R.string.onboarding_desc_1
        );
        
        OnboardingItem item2 = new OnboardingItem(
                R.drawable.onboarding_image_2,
                R.string.onboarding_quote_2,
                R.string.onboarding_desc_2
        );
        
        OnboardingItem item3 = new OnboardingItem(
                R.drawable.onboarding_image_3,
                R.string.onboarding_quote_3,
                R.string.onboarding_desc_3
        );
        
        // Добавляем элементы в список
        onboardingItems.add(item1);
        onboardingItems.add(item2);
        onboardingItems.add(item3);
        
        // Создаем адаптер и устанавливаем его для ViewPager2
        onboardingAdapter = new OnboardingAdapter(onboardingItems);
        onboardingViewPager.setAdapter(onboardingAdapter);
    }
    
    /**
     * Обновление индикаторов в зависимости от текущей страницы
     */
    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            if (i == position) {
                indicators[i].setImageResource(R.drawable.indicator_active);
            } else {
                indicators[i].setImageResource(R.drawable.indicator_inactive);
            }
        }
    }
    
    /**
     * Обновление текста кнопки "Далее" в зависимости от текущей страницы
     */
    private void updateNextButtonText(int position) {
        if (position == onboardingAdapter.getItemCount() - 1) {
            nextButton.setText(R.string.get_started);
        } else {
            nextButton.setText(R.string.next);
        }
    }
    
    /**
     * Проверка, был ли уже показан онбординг
     */
    private boolean isOnboardingCompleted() {
        SharedPreferences sharedPreferences = getSharedPreferences("onboarding", MODE_PRIVATE);
        return sharedPreferences.getBoolean("completed", false);
    }
    
    /**
     * Сохранение информации о том, что онбординг был показан
     */
    private void completeOnboarding() {
        SharedPreferences sharedPreferences = getSharedPreferences("onboarding", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("completed", true);
        editor.apply();
    }
    
    /**
     * Запуск активности входа
     */
    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
} 