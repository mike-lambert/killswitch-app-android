package com.github.mikelambert.killswitch.event;

import lombok.Data;

@Data
public class KillswitchArmedStatus extends BooleanState {
    public KillswitchArmedStatus(boolean value){
        this.value = value;
    }
}
