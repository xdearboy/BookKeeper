package com.xdearboy.bookkeeper.ui.onboarding;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * Класс для хранения данных элемента онбординга
 */
public class OnboardingItem {
    private final @DrawableRes int imageResId;
    private final @StringRes int quoteResId;
    private final @StringRes int descriptionResId;

    public OnboardingItem(@DrawableRes int imageResId, @StringRes int quoteResId, @StringRes int descriptionResId) {
        this.imageResId = imageResId;
        this.quoteResId = quoteResId;
        this.descriptionResId = descriptionResId;
    }

    @DrawableRes
    public int getImageResId() {
        return imageResId;
    }

    @StringRes
    public int getQuoteResId() {
        return quoteResId;
    }

    @StringRes
    public int getDescriptionResId() {
        return descriptionResId;
    }
} 