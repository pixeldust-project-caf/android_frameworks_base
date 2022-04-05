/*
 * Copyright (C) 2021 The Pixel Experience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pixelexperience.systemui.qs.tileimpl;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSFactoryImpl;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.AlarmTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.CameraToggleTile;
import com.android.systemui.qs.tiles.CastTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.ColorInversionTile;
import com.android.systemui.qs.tiles.DataSaverTile;
import com.android.systemui.qs.tiles.DeviceControlsTile;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.FlashlightTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.qs.tiles.InternetTile;
import com.android.systemui.qs.tiles.LocationTile;
import com.android.systemui.qs.tiles.MicrophoneToggleTile;
import com.android.systemui.qs.tiles.NfcTile;
import com.android.systemui.qs.tiles.NightDisplayTile;
import com.android.systemui.qs.tiles.QuickAccessWalletTile;
import com.android.systemui.qs.tiles.ReduceBrightColorsTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.qs.tiles.ScreenRecordTile;
import com.android.systemui.qs.tiles.UiModeNightTile;
import com.android.systemui.qs.tiles.UserTile;
import com.android.systemui.qs.tiles.WifiTile;
import com.android.systemui.qs.tiles.WorkModeTile;
import com.android.systemui.util.leak.GarbageMonitor;

// Custom tiles
import com.pixeldust.android.systemui.qs.tiles.AODTile;
import com.pixeldust.android.systemui.qs.tiles.CaffeineTile;
import com.pixeldust.android.systemui.qs.tiles.DataSwitchTile;
import com.pixeldust.android.systemui.qs.tiles.LocaleTile;
import com.pixeldust.android.systemui.qs.tiles.PDSettingsTile;
import com.pixeldust.android.systemui.qs.tiles.SyncTile;
import com.pixeldust.android.systemui.qs.tiles.VpnTile;

import org.pixelexperience.systemui.qs.tiles.BatterySaverTileGoogle;
import org.pixelexperience.systemui.qs.tiles.ReverseChargingTile;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.Lazy;

@SysUISingleton
public class QSFactoryImplGoogle extends QSFactoryImpl {

    private final Provider<CaffeineTile> mCaffeineTileProvider;
    private final Provider<SyncTile> mSyncTileProvider;
    private final Provider<VpnTile> mVpnTileProvider;
    private final Provider<AODTile> mAODTileProvider;
    private final Provider<DataSwitchTile> mDataSwitchTileProvider;
    private final Provider<PDSettingsTile> mPDSettingsTileProvider;
    private final Provider<LocaleTile> mLocaleTileProvider;
    private final Provider<BatterySaverTileGoogle> mBatterySaverTileGoogleProvider;
    private final Provider<ReverseChargingTile> mReverseChargingTileProvider;

    @Inject
    public QSFactoryImplGoogle(
            Lazy<QSHost> qsHostLazy,
            Provider<CustomTile.Builder> customTileBuilderProvider,
            Provider<WifiTile> wifiTileProvider,
            Provider<InternetTile> internetTileProvider,
            Provider<BluetoothTile> bluetoothTileProvider,
            Provider<CellularTile> cellularTileProvider,
            Provider<DndTile> dndTileProvider,
            Provider<ColorInversionTile> colorInversionTileProvider,
            Provider<AirplaneModeTile> airplaneModeTileProvider,
            Provider<WorkModeTile> workModeTileProvider,
            Provider<RotationLockTile> rotationLockTileProvider,
            Provider<FlashlightTile> flashlightTileProvider,
            Provider<LocationTile> locationTileProvider,
            Provider<CastTile> castTileProvider,
            Provider<HotspotTile> hotspotTileProvider,
            Provider<UserTile> userTileProvider,
            Provider<BatterySaverTileGoogle> batterySaverTileGoogleProvider,
            Provider<DataSaverTile> dataSaverTileProvider,
            Provider<NightDisplayTile> nightDisplayTileProvider,
            Provider<NfcTile> nfcTileProvider,
            Provider<GarbageMonitor.MemoryTile> memoryTileProvider,
            Provider<UiModeNightTile> uiModeNightTileProvider,
            Provider<ScreenRecordTile> screenRecordTileProvider,
            Provider<ReduceBrightColorsTile> reduceBrightColorsTileProvider,
            Provider<CameraToggleTile> cameraToggleTileProvider,
            Provider<MicrophoneToggleTile> microphoneToggleTileProvider,
            Provider<DeviceControlsTile> deviceControlsTileProvider,
            Provider<AlarmTile> alarmTileProvider,
            Provider<QuickAccessWalletTile> quickAccessWalletTileProvider,
            Provider<CaffeineTile> caffeineTileProvider,
            Provider<SyncTile> syncTileProvider,
            Provider<VpnTile> vpnTileProvider,
            Provider<AODTile> aodTileProvider,
            Provider<DataSwitchTile> dataSwitchTileProvider,
            Provider<PDSettingsTile> pdSettingsTileProvider,
            Provider<LocaleTile> localeTileProvider,
            Provider<ReverseChargingTile> reverseChargingTileProvider) {
        super(qsHostLazy,
                customTileBuilderProvider,
                wifiTileProvider,
                internetTileProvider,
                bluetoothTileProvider,
                cellularTileProvider,
                dndTileProvider,
                colorInversionTileProvider,
                airplaneModeTileProvider,
                workModeTileProvider,
                rotationLockTileProvider,
                flashlightTileProvider,
                locationTileProvider,
                castTileProvider,
                hotspotTileProvider,
                userTileProvider,
                () -> batterySaverTileGoogleProvider.get(),
                dataSaverTileProvider,
                nightDisplayTileProvider,
                nfcTileProvider,
                memoryTileProvider,
                uiModeNightTileProvider,
                screenRecordTileProvider,
                reduceBrightColorsTileProvider,
                cameraToggleTileProvider,
                microphoneToggleTileProvider,
                deviceControlsTileProvider,
                alarmTileProvider,
                quickAccessWalletTileProvider);
        // custom tiles
        mCaffeineTileProvider = caffeineTileProvider;
        mSyncTileProvider = syncTileProvider;
        mVpnTileProvider = vpnTileProvider;
        mAODTileProvider = aodTileProvider;
        mDataSwitchTileProvider = dataSwitchTileProvider;
        mPDSettingsTileProvider = pdSettingsTileProvider;
        mLocaleTileProvider = localeTileProvider;
        mReverseChargingTileProvider = reverseChargingTileProvider;
        mBatterySaverTileGoogleProvider = batterySaverTileGoogleProvider;
    }

    @Override
    public QSTile createTile(String str) {
        QSTileImpl createTileInternal = createTileInternal(str);
        if (createTileInternal != null) {
            return createTileInternal;
        }
        return super.createTile(str);
    }

    private QSTileImpl createTileInternal(String str) {
        switch(str) {
            case "caffeine":
                return mCaffeineTileProvider.get();
            case "sync":
                return mSyncTileProvider.get();
            case "vpn":
                return mVpnTileProvider.get();
            case "aod":
                return mAODTileProvider.get();
            case "dataswitch":
                return mDataSwitchTileProvider.get();
            case "pixeldust_settings":
                return mPDSettingsTileProvider.get();
            case "locale":
                return mLocaleTileProvider.get();
            case "reverse":
                return mReverseChargingTileProvider.get();
            case "battery":
                return mBatterySaverTileGoogleProvider.get();
            default:
                return null;
        }
    }
}
