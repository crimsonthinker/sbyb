package com.sbyb;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class CameraPreference extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.camera_preference);
    }
}
