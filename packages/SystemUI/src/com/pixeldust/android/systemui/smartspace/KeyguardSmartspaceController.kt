/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.pixeldust.android.systemui.smartspace

import android.content.ComponentName
import android.content.Context

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.FeatureFlags

import com.google.android.systemui.smartspace.KeyguardMediaViewController
import com.google.android.systemui.smartspace.KeyguardZenAlarmViewController

import javax.inject.Inject

@SysUISingleton
class KeyguardSmartspaceController @Inject constructor(
    private val context: Context,
    private val featureFlags: FeatureFlags,
    private val zenController: KeyguardZenAlarmViewController,
    private val mediaController: KeyguardMediaViewController,
) {
    init {
        if (!featureFlags.isSmartspaceEnabled()) {
            context.packageManager.setComponentEnabledSetting(ComponentName("com.android.systemui", "com.pixeldust.android.systemui.keyguard.KeyguardSliceProviderPixeldust"), 1, 1)
        } else {
            mediaController.init()
            zenController.init()
        }
    }
}
