package com.github.mikelambert.killswitch.common;

import android.content.Intent;

public class Intents {
    public static final String EVENT_KILLSWITCH_TRIGGER = "com.github.mikelambert.killswitch.ACTION_TRIGGER";
    public static final String EVENT_KILLSWITCH_ARMED = "com.github.mikelambert.killswitch.ACTION_ARMED";
    public static final String EVENT_KILLSWITCH_DISARMED = "com.github.mikelambert.killswitch.ACTION_DISARMED";

    public static final String TRIGGER_ACTION_WIPE = "WIPE";

    public static final int FLAG_KILLSWITCH_TRIGGER_RED_BUTTON = 1;
    public static final int FLAG_KILLSWITCH_TRIGGER_TAMPER = 2;
    public static final int FLAG_KILLSWITCH_TRIGGER_KEYGUARD_PIN_ENTRY = 4;
    public static final int FLAG_KILLSWITCH_TRIGGER_KEYGUARD_PIN_FAILURE = 8;

    public static Intent createKillswitchTriggerIntent(int flags) {
        return new Intent(EVENT_KILLSWITCH_TRIGGER)
                .addFlags(flags);
    }

    public static Intent createKillswitchArmedIntent() {
        return new Intent(EVENT_KILLSWITCH_ARMED);
    }

    public static Intent createKillswitchDisarmedIntent() {
        return new Intent(EVENT_KILLSWITCH_DISARMED);
    }
}
