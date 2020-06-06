package com.github.mikelambert.killswitch.event;

import lombok.Data;

@Data
public abstract class BooleanState {
    protected boolean value;
}
