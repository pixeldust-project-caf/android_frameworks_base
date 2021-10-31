/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.tuner;

import android.content.Context;
import android.provider.Settings;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.systemui.R;

public class StatusbarItems extends PreferenceFragment {

    private static final String HIDE_QS_CALL_STRENGTH = "hide_qs_call_strength";

    private SwitchPreference mCallStrengthIcon;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.statusbar_items);

        mCallStrengthIcon = (SwitchPreference) findPreference(HIDE_QS_CALL_STRENGTH);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mCallStrengthIcon) {
            boolean checked = ((SwitchPreference)preference).isChecked();
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    HIDE_QS_CALL_STRENGTH, checked ? 0 : 1);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
