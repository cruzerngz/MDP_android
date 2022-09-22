package com.example.mdp_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class FullscreenFragment extends DialogFragment
{
    private int margin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        margin = 10;
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        super.show(manager, tag);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fullscreen, container, false);
    }

    @Override
    public void onResume() {
        int dialogHeight =10 - (margin * 2) - 20;
        int dialogWidth = 200 - (margin * 2);
        getDialog().getWindow().setLayout(dialogWidth, dialogHeight);
        super.onResume();
    }
}