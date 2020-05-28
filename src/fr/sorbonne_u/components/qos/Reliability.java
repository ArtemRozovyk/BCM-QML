package fr.sorbonne_u.components.qos;

import fr.sorbonne_u.components.qos.dimensions.*;
import fr.sorbonne_u.components.qos.interfaces.*;

import java.util.*;

public class Reliability implements ContractTypeI {
    public NumberOfFailures numberOfFailures;
    public TimeToRepair timeToRepair;
    public Availability availability;




    @Override
    public List<String> getAssociatedOperations() {
        return null; //?
    }
}
