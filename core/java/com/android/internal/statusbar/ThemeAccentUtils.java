/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.statusbar;

import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.util.Log;

public class ThemeAccentUtils {

    public static final String TAG = "ThemeAccentUtils";

    // Vendor overlays to ignore
    public static final String[] BLACKLIST_VENDOR_OVERLAYS = {
        "SysuiDarkTheme",
        "Pixel",
        "DisplayCutoutEmulationCorner",
        "DisplayCutoutEmulationDouble",
        "DisplayCutoutEmulationNarrow",
        "DisplayCutoutEmulationWide",
    };

    // Stock dark theme package
    private static final String STOCK_DARK_THEME = "com.android.systemui.theme.dark";

    // Dark themes
    private static final String[] DARK_THEMES = {
        "com.accents.pink", // 0
        "com.android.system.theme.dark", // 1
        "com.android.settings.theme.dark", // 2
        "com.android.settings.intelligence.theme.dark", // 3
        "com.android.gboard.theme.dark", // 4
        "com.android.systemui.theme.dark", // 5
        "com.android.wellbeing.theme.dark", // 6
        "com.google.intelligence.sense.theme.dark", // 7
    };

    private static final String[] BLACK_THEMES = {
        "com.accents.deeppurple", // 0
        "com.android.system.theme.black", // 1
        "com.android.settings.theme.black", // 2
        "com.android.settings.intelligence.theme.black", // 3
        "com.android.gboard.theme.black", // 4
        "com.android.documentsui.theme.black", // 5
        "com.android.systemui.theme.black", // 6
        "com.android.wellbeing.theme.black", // 7
        "com.google.intelligence.sense.theme.dark", // 8
    };

    private static final String[] SHISHUNIGHTS_THEMES = {
        "com.accents.userone", // 0
        "com.android.system.theme.shishunights", // 1
        "com.android.settings.theme.shishunights", // 2
        "com.android.settings.intelligence.theme.black", // 3
        "com.android.gboard.theme.shishunights", // 4
        "com.google.android.gms.theme.shishunights", // 5
        "com.android.documentsui.theme.shishunights", // 6
        "com.android.wellbeing.theme.shishunights", // 7
        "com.android.systemui.theme.shishunights", // 8
        "com.google.intelligence.sense.theme.dark", // 9
    };

    private static final String[] CHOCOLATE_THEMES = {
        "com.accents.candyred", // 0
        "com.android.system.theme.chocolate", // 1
        "com.android.settings.theme.chocolate", // 2
        "com.android.settings.intelligence.theme.black", // 3
        "com.android.gboard.theme.chocolate", // 4
        "com.android.documentsui.theme.chocolate", // 5
        "com.android.wellbeing.theme.chocolate", // 6
        "com.android.systemui.theme.chocolate", // 7
        "com.google.intelligence.sense.theme.dark", // 8
    };

    private static final String[] LIGHT_THEMES = {
        "com.google.intelligence.sense.theme.light", // 0
        "com.android.gboard.theme.light", // 1
    };

    // Accents
    private static final String[] ACCENTS = {
        "default_accent", // 0
        "com.accents.red", // 1
        "com.accents.pink", // 2
        "com.accents.purple", // 3
        "com.accents.deeppurple", // 4
        "com.accents.indigo", // 5
        "com.accents.blue", // 6
        "com.accents.lightblue", // 7
        "com.accents.cyan", // 8
        "com.accents.teal", // 9
        "com.accents.green", // 10
        "com.accents.lightgreen", // 11
        "com.accents.lime", // 12
        "com.accents.yellow", // 13
        "com.accents.amber", // 14
        "com.accents.orange", // 15
        "com.accents.deeporange", // 16
        "com.accents.brown", // 17
        "com.accents.grey", // 18
        "com.accents.bluegrey", // 19
        "com.accents.black", // 20
        "com.accents.white", // 21
        "com.accents.userone", // 22
        "com.accents.usertwo", // 23
        "com.accents.userthree", // 24
        "com.accents.userfour", // 25
        "com.accents.userfive", // 26
        "com.accents.usersix", // 27
        "com.accents.userseven", // 28
        "com.accents.maniaamber", // 29
        "com.accents.coldyellow", // 30
        "com.accents.newhouseorange", // 31
        "com.accents.warmthorange", // 32
        "com.accents.burningred", // 33
        "com.accents.candyred", // 34
        "com.accents.palered", // 35
        "com.accents.hazedpink", // 36
        "com.accents.bubblegumpink", // 37
        "com.accents.trufilpink", // 38
        "com.accents.duskpurple", // 39
        "com.accents.illusionspurple", // 40
        "com.accents.spookedpurple", // 41
        "com.accents.notimppurple", // 42
        "com.accents.dreamypurple", // 43
        "com.accents.footprintpurple", // 44
        "com.accents.obfusbleu", // 45
        "com.accents.frenchbleu", // 46
        "com.accents.coldbleu", // 47
        "com.accents.heirloombleu", // 48
        "com.accents.paleblue", // 49
        "com.accents.holillusion", // 50
        "com.accents.stock", // 51
        "com.accents.seasidemint", // 52
        "com.accents.movemint", // 53
        "com.accents.extendedgreen", // 54
        "com.accents.diffdaygreen", // 55
        "com.accents.jadegreen", // 56
    };

    private static final String[] QS_TILE_THEMES = {
        "default", // 0
        "com.android.systemui.qstile.square", // 1
        "com.android.systemui.qstile.roundedsquare", // 2
        "com.android.systemui.qstile.squircle", // 3
        "com.android.systemui.qstile.teardrop", // 4
        "com.android.systemui.qstile.circlegradient", // 5
        "com.android.systemui.qstile.circletrim", // 6
        "com.android.systemui.qstile.dottedcircle", // 7
        "com.android.systemui.qstile.dualtonecircle", // 8
        "com.android.systemui.qstile.dualtonecircletrim", // 9
        "com.android.systemui.qstile.mountain", // 10
        "com.android.systemui.qstile.ninja", // 11
        "com.android.systemui.qstile.pokesign", // 12
        "com.android.systemui.qstile.wavey", // 13
        "com.android.systemui.qstile.squircletrim", // 14
        "com.android.systemui.qstile.cookie", // 15
        "com.android.systemui.qstile.oreo", // 16
        "com.android.systemui.qstile.oreocircletrim", // 17
        "com.android.systemui.qstile.oreosquircletrim", // 18
    };

    // Unloads the stock dark theme
    public static void unloadStockDarkTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(STOCK_DARK_THEME,
                    userId);
            if (themeInfo != null && themeInfo.isEnabled()) {
                om.setEnabled(STOCK_DARK_THEME,
                        false /*disable*/, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Check for the dark system theme
    public static boolean isUsingDarkTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(DARK_THEMES[0],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Check for the black system theme
    public static boolean isUsingBlackTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(BLACK_THEMES[0],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
     }

    // Check for the shishunights system theme
    public static boolean isUsingShishuNightsTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(SHISHUNIGHTS_THEMES[0],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
     }

    // Check for the chocolate system theme
    public static boolean isUsingChocolateTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(CHOCOLATE_THEMES[0],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
     }

    // Set light / dark theme
    public static void setLightDarkTheme(IOverlayManager om, int userId, boolean useDarkTheme) {
        for (String theme : DARK_THEMES) {
            try {
                om.setEnabled(theme,
                        useDarkTheme, userId);
                if (useDarkTheme) {
                    unloadStockDarkTheme(om, userId);
                }
            } catch (RemoteException e) {
            }
        }
        for (String theme : LIGHT_THEMES) {
            try {
                om.setEnabled(theme,
                        !useDarkTheme, userId);
            } catch (RemoteException e) {
            }
        }
        unfuckBlackWhiteAccent(om, userId);
    }

    // Set black theme
    public static void setBlackTheme(IOverlayManager om, int userId, boolean useBlackTheme) {
        for (String theme : BLACK_THEMES) {
            try {
                om.setEnabled(theme,
                        useBlackTheme, userId);
                unfuckBlackWhiteAccent(om, userId);
                if (useBlackTheme) {
                    unloadStockDarkTheme(om, userId);
                }
            } catch (RemoteException e) {
            }
        }
    }

    // Set shishunights theme
    public static void setShishuNightsTheme(IOverlayManager om, int userId, boolean useShishuNightsTheme) {
        for (String theme : SHISHUNIGHTS_THEMES) {
            try {
                om.setEnabled(theme,
                        useShishuNightsTheme, userId);
                unfuckBlackWhiteAccent(om, userId);
                if (useShishuNightsTheme) {
                    unloadStockDarkTheme(om, userId);
                }
            } catch (RemoteException e) {
            }
        }
    }

    // Set chocolate theme
    public static void setChocolateTheme(IOverlayManager om, int userId, boolean useChocolateTheme) {
        for (String theme : CHOCOLATE_THEMES) {
            try {
                om.setEnabled(theme,
                        useChocolateTheme, userId);
                unfuckBlackWhiteAccent(om, userId);
                if (useChocolateTheme) {
                    unloadStockDarkTheme(om, userId);
                }
            } catch (RemoteException e) {
            }
        }
    }

    // Check for black and white accent overlays
    public static void unfuckBlackWhiteAccent(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            if (isUsingDarkTheme(om, userId) || isUsingBlackTheme(om, userId) || isUsingShishuNightsTheme(om, userId) || isUsingChocolateTheme(om, userId)) {
                themeInfo = om.getOverlayInfo(ACCENTS[20],
                        userId);
                if (themeInfo != null && themeInfo.isEnabled()) {
                    om.setEnabled(ACCENTS[20],
                            false /*disable*/, userId);
                    om.setEnabled(ACCENTS[21],
                            true, userId);
                }
            } else {
                themeInfo = om.getOverlayInfo(ACCENTS[21],
                        userId);
                if (themeInfo != null && themeInfo.isEnabled()) {
                    om.setEnabled(ACCENTS[21],
                            false /*disable*/, userId);
                    om.setEnabled(ACCENTS[20],
                            true, userId);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Check for the white accent overlay
    public static boolean isUsingWhiteAccent(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(ACCENTS[21],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Switches theme accent from one to another or back to stock
    public static void updateAccents(IOverlayManager om, int userId, int accentSetting) {
        if (accentSetting == 0) {
            //On selecting default accent, set accent to pink if Dark Theme is being used
            if (isUsingDarkTheme(om, userId)) {
                try {
                    om.setEnabled(DARK_THEMES[0],
                        true, userId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Can't change theme", e);
                }
            //On selecting default accent, set accent to deep purple if Black Theme is being used
            } else if (isUsingBlackTheme(om, userId)) {
                try {
                    om.setEnabled(BLACK_THEMES[0],
                        true, userId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Can't change theme", e);
                }
            //On selecting default accent, set accent to dirty red if ShishuNights Theme is being used
            } else if (isUsingShishuNightsTheme(om, userId)) {
                try {
                    om.setEnabled(SHISHUNIGHTS_THEMES[0],
                        true, userId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Can't change theme", e);
                }
            //On selecting default accent, set accent to candy red if Chocolate Theme is being used
            } else if (isUsingChocolateTheme(om, userId)) {
                try {
                    om.setEnabled(CHOCOLATE_THEMES[0],
                        true, userId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Can't change theme", e);
                }
            } else {
                unloadAccents(om, userId);
            }
        } else if ((accentSetting < 20) || (accentSetting > 21)) {
            try {
                om.setEnabled(ACCENTS[accentSetting],
                        true, userId);
            } catch (RemoteException e) {
            }
        } else if (accentSetting == 20) {
            try {
                // If using a dark theme we use the white accent, otherwise use the black accent
                if (isUsingDarkTheme(om, userId) || isUsingBlackTheme(om, userId) || isUsingShishuNightsTheme(om, userId) || isUsingChocolateTheme(om, userId)) {
                    om.setEnabled(ACCENTS[21],
                            true, userId);
                } else {
                    om.setEnabled(ACCENTS[20],
                            true, userId);
                }
            } catch (RemoteException e) {
            }
        }
    }

    // Unload all the theme accents
    public static void unloadAccents(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 1; i < ACCENTS.length; i++) {
            String accent = ACCENTS[i];
            try {
                om.setEnabled(accent,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches qs tile style to user selected.
    public static void updateTileStyle(IOverlayManager om, int userId, int qsTileStyle) {
        if (qsTileStyle == 0) {
            unlockQsTileStyles(om, userId);
        } else {
            try {
                om.setEnabled(QS_TILE_THEMES[qsTileStyle],
                        true, userId);
            } catch (RemoteException e) {
            }
        }
    }

    // Unload all the qs tile styles
    public static void unlockQsTileStyles(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 1; i < QS_TILE_THEMES.length; i++) {
            String qstiletheme = QS_TILE_THEMES[i];
            try {
                om.setEnabled(qstiletheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Check for any QS tile styles overlay
    public static boolean isUsingQsTileStyles(IOverlayManager om, int userId, int qsstyle) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(QS_TILE_THEMES[qsstyle],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }
}
