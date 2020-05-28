package fr.sorbonne_u.components.qos;

import fr.sorbonne_u.components.interfaces.*;
import fr.sorbonne_u.components.qos.annotations.*;
import fr.sorbonne_u.components.qos.interfaces.*;
import fr.sorbonne_u.components.qos.solver.*;
import fr.sorbonne_u.components.qos.solver.booleval.*;
import javafx.util.*;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javax.script.*;
import java.lang.annotation.Annotation;
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

        return verifyAxiomsConformance(true,methodPreconditions) && verifyAxiomsConformance(false,methodPostconditions) && verifyQoSConformance(conractCouplesToBeTested);
        //try matching the constraints of corresponding global contracts defined by @Require


    }

    private static boolean verifyQoSConformance(List<Pair<ContractI, ContractI>> conractCouplesToBeTested) {
        //FIXME come on, do it
        return true;
    }

    public static class Implication {

    }

    private static boolean verifyAxiomsConformance(boolean pre,List<Pair<String, String>> axiomCouples) {
        //build the list of constrainst and solve it;
        for(Pair<String,String> p : axiomCouples){
            if (pre){
                if(!ChocoSolver.verifyAll(p.getValue(),p.getKey())){
                    return false;
                }
            }else{
                if(!ChocoSolver.verifyAll(p.getKey(),p.getValue())){
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
    
    public static void AddDynamicConformityCode(Class<?> IR, Class<?> outboundPort, ClassPool pool) throws Exception {
    	
    	//get the interface Annotations
		Annotation[] interfaceAnnotations = IR.getAnnotations(); 
		
		//for each interface annotation do whatever ... 
		for(Annotation annotation : interfaceAnnotations ){
			if(annotation instanceof ContractDefinition){
				//to do ...
			}
			else if(annotation instanceof Require){
				//to do ...
			}
		}
    	
		//get all the methods of the interface
		Method[] methodsIR = IR.getDeclaredMethods();
		
		//for each method get its annotation and add the proper verification code in the class method
		for (Method mIR : methodsIR) {
			
			Method mOutboundPort = outboundPort.getMethod(mIR.getName(), mIR.getParameterTypes());
			Annotation[] methodAnnotations = mIR.getAnnotations();
			
			//for each annotation add the proper code verification  
			for(Annotation annotation : methodAnnotations ){
				if(annotation instanceof Pre){
					
					String expression = ((Pre)annotation).expression();
					//to do ...
				}
				else if(annotation instanceof Post){
					//to do ...
				} else if (annotation instanceof Require){
					//to do ...
				} else if (annotation instanceof RequireContract){
					//to do ...
				}
			}
			
			
		/*
		 * 	
		 * 
			
			//String[] args = annotation.args();

			//for (int i = 0; i < args.length; i++) {
			//	expression = expression.replaceAll("\\b" + args[i] + "\\b", "\\$" + (i + 1));
			//}

			// Partie javassist

			CtClass cc = pool.get(outboundPort.getCanonicalName());
			cc.defrost();

			Class<?>[] paramTypes = mOutboundPort.getParameterTypes();
			CtClass[] params = new CtClass[paramTypes.length];

			for (int i = 0; i < params.length; i++) {
				params[i] = pool.get(paramTypes[i].getName());
			}

			CtMethod cm = cc.getDeclaredMethod(mOutboundPort.getName(), params);
			
			// insérer le code au debut de la methode
			cm.insertBefore("if (!(" + expression + "))" + "throw new IllegalArgumentException();");
			cc.writeFile();
			
			//byte[] classFile = cc.toBytecode();
			//HotSwapper hs = new HotSwapper(8000);
			//hs.reload(cc.getName(), classFile);
			
		}
    */}
	}

}
