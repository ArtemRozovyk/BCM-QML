package fr.sorbonne_u.components.qos;

import fr.sorbonne_u.components.interfaces.*;
import fr.sorbonne_u.components.qos.annotations.*;
import fr.sorbonne_u.components.qos.interfaces.*;
import javafx.util.*;

import javax.script.*;
import java.lang.reflect.*;
import java.util.*;

public class ConformanceChecker {


    public static boolean conformanceVerification(Class<?>[] clientI, Class<?>[] serverI) throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, ConformanceException {

        //TODO multiple interfaces implemented by port or connector
        Class<? extends RequiredI> client = (Class<? extends RequiredI>) clientI[0];
        Class<? extends RequiredI> server = (Class<? extends RequiredI>) serverI[0];

        //keep trace of all contracts that have been defined in both interfaces
        Map<String, ContractI> serverDefinedContracts = new HashMap<>(getContractDefinitionsFromClass(server));
        Map<String, ContractI> clientDefinedContracts = new HashMap<>(getContractDefinitionsFromClass(client));
        //IMPORTANT: Key should always represent the server contracts and value those defined by client
        List<Pair<ContractI, ContractI>> conractCouplesToBeTested = new ArrayList<>();

        for (Require cr : getRequireFromClass(client)) {
            //try to find matching global @Require in the server
            boolean found = false;
            for (Require sr : getRequireFromClass(server)) {
                if (sr.contractName().equals(cr.contractName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ConformanceException("Server does not specify global @Require constraint named: " + cr.contractName());
            }
            //both server and client have a constraint with corresponding name,
            // add to contract couples to be verified.
            conractCouplesToBeTested.add(new Pair<>(
                    serverDefinedContracts.get(cr.contractName()),
                    clientDefinedContracts.get(cr.contractName())));

        }
        //once again server is key, value is client.
        List<Pair<String, String>> methodPreconditions = new ArrayList<>();
        List<Pair<String, String>> methodPostconditions = new ArrayList<>();

        //checking only methods with having the same signature.
        Method[] methodsServer = server.getMethods();

        for (Method serverMethod : methodsServer) {
            //Axioms (pre post)
            try {
                Method clientMethod = client.getMethod(serverMethod.getName(), serverMethod.getParameterTypes());
                //TODO factorise couple checking annotation : see annotation superclass to make a method.
                Pre cliPre = clientMethod.getAnnotation(Pre.class);
                Post cliPost = clientMethod.getAnnotation(Post.class);
                if (cliPre != null) {
                    Pre servPre = serverMethod.getAnnotation(Pre.class);
                    if (servPre == null) {
                        throw new ConformanceException("Server method " + serverMethod.getName() + "" +
                                " does not define corresponding client precondition");
                    }
                    methodPreconditions.add(new Pair<>(servPre.expression(),cliPre.expression()));
                }
                if (cliPost != null) {
                    Post servPost = serverMethod.getAnnotation(Post.class);
                    if (servPost == null) {
                        throw new ConformanceException("Server method " + serverMethod.getName() + "" +
                                " does not define corresponding client postCondition");
                    }
                    methodPostconditions.add(new Pair<>(servPost.value(),cliPost.value()));
                }
                //get all the @RequireContract
                RequireContract requireContractClient = clientMethod.getAnnotation(RequireContract.class);
                if(requireContractClient!=null){
                    RequireContract requireContractServer = serverMethod.getAnnotation(RequireContract.class);
                    if(requireContractServer==null){
                        throw new ConformanceException("Server method " + serverMethod.getName() + "" +
                                " does not define corresponding @ClientContract");
                    }
                    conractCouplesToBeTested.add(new Pair<>(
                            contractFromAnnotation(serverMethod.getName(),requireContractServer),
                            contractFromAnnotation(serverMethod.getName(),requireContractClient)));
                }

                //get all the @Require (has to be defined contract)
                Require requireClient = clientMethod.getAnnotation(Require.class);
                if(requireClient!=null){
                    Require requireServer = serverMethod.getAnnotation(Require.class);
                    if(requireServer==null){
                        throw new ConformanceException("Server method " + serverMethod.getName() + "" +
                                " does not define corresponding @Require");
                    }

                    ContractI serverContract = serverDefinedContracts.get(requireServer.contractName());
                    ContractI clientContract = clientDefinedContracts.get(requireServer.contractName());
                    if(serverContract==null || clientContract == null){
                        throw new ConformanceException("The contract "+requireServer.contractName()+" is undefined");
                    }
                    conractCouplesToBeTested.add(new Pair<>(serverContract,clientContract));
                }

            } catch (NoSuchMethodException ignored) {
            }
            //QML


        }

        return verifyAxiomsConformance(methodPostconditions) && verifyAxiomsConformance(methodPostconditions) && verifyQoSConformance(conractCouplesToBeTested);
        //try matching the constraints of corresponding global contracts defined by @Require


    }

    private static boolean verifyQoSConformance(List<Pair<ContractI, ContractI>> conractCouplesToBeTested) {
        /*
            if (serverContract != null) {
                Class<? extends ContractTypeI> contractTypeI = clientContract.getType();
                for (Field f : contractTypeI.getDeclaredFields()) {
                    Field isIncreasingField = f.getType().getField("IS_INCREASING");
                    Class<?> bres = isIncreasingField.getType();
                    boolean isIncreasing;
                    if (bres == boolean.class) {
                        isIncreasing = isIncreasingField.getBoolean(null);
                    }
                }
                /*
                Field isIncrFidl  = contractTypeI.getField("IS_INCREASING");
                Class<?> bres = isIncrFidl.getType();
                if(bres == boolean.class){
                    System.out.println(field.getBoolean(null));
                }
            } else {
                throw new ConformanceException("The server doesnt not define " + clientContract.getName());
            }
        }

*/

        return false;
    }

    public static class Implication {

    }

    private static boolean verifyAxiomsConformance(List<Pair<String, String>> methodPostconditions) {
        //build the list of constrainst and solve it;

        System.out.println("hello");
        try {
            ScriptEngineManager sem = new ScriptEngineManager();
            ScriptEngine se = sem.getEngineByName("JavaScript");
            String myExpression = "(x > 5 && y <55) || 5 % 2 == 1";
            System.out.println(se.eval(myExpression));
        } catch (ScriptException e) {
            System.out.println("Invalid Expression");
            e.printStackTrace();
        }
    return false;
    }

    private static ContractI contractFromAnnotation(String name, RequireContract requireContractServer) {
        return new ContractI() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<? extends ContractTypeI> getType() {
                return requireContractServer.contractType();
            }

            @Override
            public String[] getConstraints() {
                return requireContractServer.constraints();
            }
        };
    }


    static Require[] getRequireFromClass(Class<? extends RequiredI> itClass) {
        return itClass.getAnnotation(Require.class) == null ?
                itClass.getAnnotation(Require.List.class).value() :
                new Require[]{itClass.getAnnotation(Require.class)};
    }


    private static Map<? extends String, ? extends ContractI> getContractDefinitionsFromClass(Class<? extends RequiredI> clazz) {
        Map<String, ContractI> res = new HashMap<String, ContractI>();
        ContractDefinition[] clientContractDefs =
                clazz.getAnnotation(ContractDefinition.class) == null ?
                        clazz.getAnnotation(ContractDefinition.List.class).value() :
                        new ContractDefinition[]{clazz.getAnnotation(ContractDefinition.class)};

        for (ContractDefinition ccd : clientContractDefs) {
            res.put(ccd.name(), new ContractI() {
                @Override
                public String getName() {
                    return ccd.name();
                }

                @Override
                public Class<? extends ContractTypeI> getType() {
                    return ccd.type();
                }

                @Override
                public String[] getConstraints() {
                    return ccd.constraints();
                }
            });
        }
        return res;
    }

}
