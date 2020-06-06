package com.github.mikelambert.killswitch.persistence;

import lombok.Data;

@Data
public class PersistentState {
    private boolean armed;
    private boolean wipeSdCard;
    private boolean activateByMulticlick;
    private int clicksCount;

    public static PersistentState cloneState(PersistentState state) {
        PersistentState result = new PersistentState();
        result.armed = state.armed;
        result.wipeSdCard = state.wipeSdCard;
        result.activateByMulticlick = state.activateByMulticlick;
        result.clicksCount = state.clicksCount;
        return result;
    }
}
