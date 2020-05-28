package fr.sorbonne_u.components.qos;

import com.sun.tools.javac.util.*;
import com.sun.xml.internal.ws.util.StringUtils;
import fr.sorbonne_u.components.interfaces.*;
import fr.sorbonne_u.components.qos.annotations.*;
import fr.sorbonne_u.components.qos.interfaces.*;
import fr.sorbonne_u.components.qos.solver.*;
import fr.sorbonne_u.components.qos.solver.booleval.*;
import javafx.util.*;
import javafx.util.Pair;
import javassist.*;

import javax.script.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

import static fr.sorbonne_u.components.qos.solver.ChocoSolver.WITH_DELIMITER;

public class ConformanceChecker {


    public static boolean conformanceVerification(Class<?>[] clientI, Class<?>[] serverI) throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, ConformanceException, IllegalStrengthException {

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
                    methodPreconditions.add(new Pair<>(servPre.expression(), cliPre.expression()));
                }
                if (cliPost != null) {
                    Post servPost = serverMethod.getAnnotation(Post.class);
                    if (servPost == null) {
                        throw new ConformanceException("Server method " + serverMethod.getName() + "" +
                                " does not define corresponding client postCondition");
                    }
                    methodPostconditions.add(new Pair<>(servPost.value(), cliPost.value()));
                }
                //get all the @RequireContract
                RequireContract requireContractClient = clientMethod.getAnnotation(RequireContract.class);
                if (requireContractClient != null) {
                    RequireContract requireContractServer = serverMethod.getAnnotation(RequireContract.class);
                    if (requireContractServer == null) {
                        throw new ConformanceException("Server method " + serverMethod.getName() + "" +
                                " does not define corresponding @ClientContract");
                    }
                    conractCouplesToBeTested.add(new Pair<>(
                            contractFromAnnotation(serverMethod.getName(), requireContractServer),
                            contractFromAnnotation(serverMethod.getName(), requireContractClient)));
                }
                //get all the @Require (has to be defined contract)
                Require requireClient = clientMethod.getAnnotation(Require.class);
                if (requireClient != null) {
                    Require requireServer = serverMethod.getAnnotation(Require.class);
                    if (requireServer == null) {
                        throw new ConformanceException("Server method " + serverMethod.getName() + "" +
                                " does not define corresponding @Require");
                    }
                    ContractI serverContract = serverDefinedContracts.get(requireServer.contractName());
                    ContractI clientContract = clientDefinedContracts.get(requireServer.contractName());
                    if (serverContract == null || clientContract == null) {
                        throw new ConformanceException("The contract " + requireServer.contractName() + " is undefined");
                    }
                    conractCouplesToBeTested.add(new Pair<>(serverContract, clientContract));
                }

            } catch (NoSuchMethodException ignored) {
            }
            //QML
        }
        return verifyAxiomsConformance(true, methodPreconditions) && verifyAxiomsConformance(false, methodPostconditions) && verifyQoSConformance(conractCouplesToBeTested);
        //try matching the constraints of corresponding global contracts defined by @Require
    }


    private static boolean verifyQoSConformance(List<Pair<ContractI, ContractI>> conractCouplesToBeTested) throws IllegalStrengthException, ConformanceException {
        //FIXME come on, do it
        List<Pair<String,String >> cplsToBeTested = new ArrayList<>();
        for (Pair<ContractI,ContractI> contractIPair : conractCouplesToBeTested){
            ContractI serverContract = contractIPair.getKey();
            ContractI clientContract = contractIPair.getValue();
            StringBuilder sc= new StringBuilder();
            StringBuilder cc= new StringBuilder();
            for(String constraint : serverContract.getConstraints()){
                verifyStrength(serverContract.getType(),constraint);
                if(sc.length() > 0){
                    sc.append(" && ").append(constraint);
                }else{
                    sc.append(constraint);
                }
            }
            for(String constraint : clientContract.getConstraints()){
                verifyStrength(clientContract.getType(),constraint);
                if(cc.length() > 0){
                    cc.append(" && ").append(constraint);
                }else{
                    cc.append(constraint);
                }
            }
            cplsToBeTested.add(new Pair<>(sc.toString(),cc.toString()));




            System.out.println(contractIPair.getKey().getName());
            System.out.println(contractIPair.getValue().getName());
        }
        verifyAxiomsConformance(false,cplsToBeTested);
        return true;
    }

    /**
     * To achieve the property
     * that conformance corresponds to constraint satisfaction,
     * we allow only the operators {==, <=, <} for decreasing
     * domains, and we allow only the operators {==, >=, >}
     * for increasing domains.
     * @param type Contract type allowing accessing dimensions
     * @param constraint Constraint to be tested
     * @throws IllegalStrengthException
     * @throws ConformanceException
     */
    private static void  verifyStrength(Class<? extends ContractTypeI> type, String constraint) throws IllegalStrengthException, ConformanceException {
        String []elements = Arrays.stream(constraint.split(
                String.format(WITH_DELIMITER, "<|>|<=|>=|==|!="))).map(String::trim).toArray(String[]::new);

        if(elements.length!=3 || ! ">,<,<=,>=,==,!=".contains(elements[1]))
            throw new ConformanceException("Malformed expression in "+constraint);

        String dim = isNumeric(elements[0])?elements[1]:elements[0];
        boolean isIncreasing;
        try {
            Field dimField = type.getDeclaredField(dim);
            Class<?> dimClass = dimField.getType();
            Field incrField = dimClass.getDeclaredField("IS_INCREASING");
            isIncreasing = incrField.getBoolean(null);
            if(isIncreasing){
                if(!"==,>=,>".contains(elements[1]))
                    throw new IllegalStrengthException(elements[1]+" " +
                            "is not an allowed operator for increasing dimmension");
            }else{
                if(!"==,<=,<".contains(elements[1]))
                    throw new IllegalStrengthException(elements[1]+" " +
                            "is not an allowed operator for decreasing dimmension");
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new ConformanceException("No such field "+dim+" in "+type.getCanonicalName());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }



    private static boolean verifyAxiomsConformance(boolean pre, List<Pair<String, String>> axiomCouples) {
        //build the list of constrainst and solve it;
        for (Pair<String, String> p : axiomCouples) {
            if (pre) {
                if (!ChocoSolver.verifyAll(p.getValue(), p.getKey())) {
                    return false;
                }
            } else {
                if (!ChocoSolver.verifyAll(p.getKey(), p.getValue())) {
                    return false;
                }
            }
        }

        return true;
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


    public static void AddDynamicConformityCode(CtClass IR, CtClass outboundPort) throws Exception {

        //get the interface super classes
        ArrayList<CtClass> superClasses = getAllSuperClasses(IR);

        //get the interface Annotations
        Object[] interfaceAnnotations = IR.getAnnotations();

        //get all the methods of the interface
        CtMethod[] methodsIR = IR.getDeclaredMethods();

        //store the contract definitions and the required contract names (the once which are required for all the methods of the interface)
        ContractDefinition[] contractDefinitionList ;
        List<String> require = new ArrayList<String>();

        //for each interface annotation do whatever ...
        for(Object annotation : interfaceAnnotations ){

            if(annotation instanceof ContractDefinition.List){
                contractDefinitionList = ((ContractDefinition.List)annotation).value();
            }
            else if(annotation instanceof Require){
                for (CtMethod mIR : methodsIR) {
                    require.add(((Require)annotation).contractName());
                }
            }
        }

        //for each method of the interface get its annotations and add the proper verification code in the matching class method
        for (CtMethod mIR : methodsIR) {

            Object[] methodAnnotations = mIR.getAnnotations();
            CtMethod cm = outboundPort.getDeclaredMethod(mIR.getName(), mIR.getParameterTypes());

            //for each annotation add the proper code verification
            for(Object annotation : methodAnnotations ){

                if(annotation instanceof Pre){
                    String expression;
                    //interface super classes code injection
                    for(int i = superClasses.size() -1 ; 0 > superClasses.size(); i--){
                        try {
                            CtMethod scm = superClasses.get(i).getDeclaredMethod(mIR.getName(), mIR.getParameterTypes());
                            Pre anno = (Pre) scm.getAnnotation(Pre.class);
                            expression = anno.expression();
                        } catch (NotFoundException e) {
                            continue;
                        }
                        cm.insertBefore("if (!(" + expression + "))" + "throw new IllegalArgumentException();");
                    }
                    //interface code injection
                    expression = ((Pre)annotation).expression();
                    cm.insertBefore("if (!(" + expression + "))" + "throw new IllegalArgumentException();");
                }
                else if(annotation instanceof Post){
                    //interface code injection
                    String expression = ((Post)annotation).value();
                    expression = expression.replaceAll("\\b" + "ret" + "\\b", "\\$_"); //needs to be done properly (java parser ?? ...)
                    cm.insertAfter("if (!(" + expression + "))" + "throw new IllegalArgumentException();");
                    //interface super classes code injection
                    for(CtClass superClass : superClasses){
                        CtMethod scm;
                        try {
                            scm = superClass.getDeclaredMethod(mIR.getName(), mIR.getParameterTypes());
                            Post anno = (Post) scm.getAnnotation(Post.class);
                            expression = anno.value();
                        } catch (NotFoundException e) {
                            continue;
                        }
                        expression = expression.replaceAll("\\b" + "ret" + "\\b", "\\$_"); //needs to be done properly (java parser ?? ...)
                        cm.insertAfter("if (!(" + expression + "))" + "throw new IllegalArgumentException();");
                    }
                } else if (annotation instanceof Require){
                    String contractName = ((Require)annotation).contractName();
                    //to do ...
                } else if (annotation instanceof RequireContract){
                    //to do ...
                }
            }

        }
        outboundPort.writeFile();
    }

    public static ArrayList<CtClass> getAllSuperClasses(CtClass clazz) throws NotFoundException {

        ArrayList<CtClass> res = new ArrayList<CtClass>();

        while (!"java.lang.Object".equals(clazz.getName())) {

            // Get the super class
            CtClass superClass = clazz.getSuperclass();

            // Add the super class
            res.add(superClass);

            // Now inspect the superclass
            clazz = superClass;

        }

        return res;
    }
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
