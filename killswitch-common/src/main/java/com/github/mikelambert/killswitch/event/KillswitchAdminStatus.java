package com.github.mikelambert.killswitch.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class KillswitchAdminStatus extends BooleanState {
    public KillswitchAdminStatus(boolean value){
        this.value = value;
    }
}
