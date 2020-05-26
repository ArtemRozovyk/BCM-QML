package fr.sorbonne_u.components.qos;

import fr.sorbonne_u.components.qos.interfaces.*;

public class TimeToRepair implements DimensionI {

    long secondsToRepair;

    public static final boolean IS_INCREASING = false;


    public TimeToRepair(long secToRepair){
        this.secondsToRepair=secToRepair;
    }

    @Override
    public Object getValue() {
        return secondsToRepair;
    }

    @Override
    public boolean isIncreasig() {
        return false;
    }

    @Override
    public boolean isDecreasing() {
        return true;
    }
}
