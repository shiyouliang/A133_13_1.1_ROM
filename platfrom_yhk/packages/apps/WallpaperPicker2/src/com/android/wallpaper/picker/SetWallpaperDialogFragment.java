/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.wallpaper.picker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;

import com.android.wallpaper.R;
import com.android.wallpaper.module.WallpaperPersister;

/**
 * Dialog fragment which shows the "Set wallpaper" destination dialog for N+ devices. Lets user
 * choose whether to set the wallpaper on the home screen, lock screen, or both.
 */
public class SetWallpaperDialogFragment extends DialogFragment {

    private Button mSetHomeWallpaperButton;
    private Button mSetLockWallpaperButton;
    private Button mSetBothWallpaperButton;

    private boolean mHomeAvailable = true;
    private boolean mLockAvailable = true;
    private Listener mListener;
    private int mTitleResId;
    private boolean mWithItemSelected;

    public SetWallpaperDialogFragment() {
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        Context context = getContext();

        @SuppressWarnings("RestrictTo")
        View layout =
                View.inflate(
                        new ContextThemeWrapper(getActivity(), R.style.LightDialogTheme),
                        R.layout.dialog_set_wallpaper,
                        null);

        View options = layout.findViewById(R.id.dialog_set_wallpaper_options);
        options.setClipToOutline(true);

        View customTitleView = View.inflate(context, R.layout.dialog_set_wallpaper_title,  null);
        TextView title = customTitleView.findViewById(R.id.dialog_set_wallpaper_title);
        title.setText(mTitleResId);
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.LightDialogTheme)
                .setCustomTitle(customTitleView)
                .setView(layout)
                .create();

        mSetHomeWallpaperButton = layout.findViewById(R.id.set_home_wallpaper_button);
        mSetHomeWallpaperButton.setOnClickListener(
                v -> onSetWallpaperButtonClick(WallpaperPersister.DEST_HOME_SCREEN));

        mSetLockWallpaperButton = layout.findViewById(R.id.set_lock_wallpaper_button);
        mSetLockWallpaperButton.setOnClickListener(
                v -> onSetWallpaperButtonClick(WallpaperPersister.DEST_LOCK_SCREEN));

        mSetBothWallpaperButton = layout.findViewById(R.id.set_both_wallpaper_button);
        mSetBothWallpaperButton.setOnClickListener(
                v -> onSetWallpaperButtonClick(WallpaperPersister.DEST_BOTH));

        updateButtonsVisibility();

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) {
            mListener.onDialogDismissed(mWithItemSelected);
        }
    }

    public void setHomeOptionAvailable(boolean homeAvailable) {
        mHomeAvailable = homeAvailable;
        updateButtonsVisibility();
    }

    public void setLockOptionAvailable(boolean lockAvailable) {
        mLockAvailable = lockAvailable;
        updateButtonsVisibility();
    }

    public void setTitleResId(@StringRes int titleResId) {
        mTitleResId = titleResId;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private void updateButtonsVisibility() {
        if (mSetHomeWallpaperButton != null) {
            mSetHomeWallpaperButton.setVisibility(mHomeAvailable ? View.VISIBLE : View.VISIBLE);
        }
        if (mSetLockWallpaperButton != null) {
            mSetLockWallpaperButton.setVisibility(mLockAvailable ? View.VISIBLE : View.VISIBLE);
        }
    }

    private void onSetWallpaperButtonClick(int destination) {
        mWithItemSelected = true;
        mListener.onSet(destination);
        dismiss();
    }

    /**
     * Interface which clients of this DialogFragment should implement in order to handle user
     * actions on the dialog's clickable elements.
     */
    public interface Listener {
        void onSet(int destination);

        /**
         * Called when the dialog is closed, both because of dismissal and for a selection
         * being set, so it'll be called even after onSet is called.
         *
         * @param withItemSelected true if the dialog is dismissed with item selected
         */
        default void onDialogDismissed(boolean withItemSelected) {}
    }
}
