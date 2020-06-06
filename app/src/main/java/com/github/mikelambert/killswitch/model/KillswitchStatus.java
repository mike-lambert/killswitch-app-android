package com.github.mikelambert.killswitch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KillswitchStatus {
    private boolean adminActive;
    private boolean killswitchArmed;
}
