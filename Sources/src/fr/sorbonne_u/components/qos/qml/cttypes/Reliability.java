package fr.sorbonne_u.components.qos.qml.cttypes;

import fr.sorbonne_u.components.qos.qml.dimensions.*;
import fr.sorbonne_u.components.qos.qml.interfaces.*;

import java.util.*;

public class Reliability implements ContractTypeI {
    public NumberOfFailures numberOfFailures;
    public TimeToRepair timeToRepair;
    public Availability availability;

    public Reliability(NumberOfFailures numberOfFailures, TimeToRepair timeToRepair, Availability availability) {
        this.numberOfFailures = numberOfFailures;
        this.timeToRepair = timeToRepair;
        this.availability = availability;
    }

    public Reliability() {
    }
}
