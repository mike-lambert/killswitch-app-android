package com.github.mikelambert.killswitch.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class KillswitchArmedStatus extends BooleanState {
    public KillswitchArmedStatus(boolean value){
        this.value = value;
    }
}
