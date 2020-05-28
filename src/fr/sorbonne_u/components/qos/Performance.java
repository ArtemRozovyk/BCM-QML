package fr.sorbonne_u.components.qos;

import fr.sorbonne_u.components.qos.dimensions.*;
import fr.sorbonne_u.components.qos.interfaces.*;

import java.util.*;

public class Performance implements ContractTypeI {
    Delay delay;
    Throughput throughput;



    @Override
    public List<String> getAssociatedOperations() {
        return null;
    }
}
