package com.github.mikelambert.killswitch.event;

import lombok.Data;

@Data
public class KillswitchAdminStatus extends BooleanState {
    public KillswitchAdminStatus(boolean value){
        this.value = value;
    }
}
