package fr.sorbonne_u.components.qos.qml.Translator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import fr.sorbonne_u.components.qos.annotations.ContractDefinition;
import fr.sorbonne_u.components.qos.annotations.Post;
import fr.sorbonne_u.components.qos.annotations.Pre;
import fr.sorbonne_u.components.qos.annotations.Require;
import fr.sorbonne_u.components.qos.annotations.RequireContract;
import fr.sorbonne_u.components.qos.qml.interfaces.*;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

public class DynamicConformance {

	/**
	 * add the dynamic conformance verification code for pre and post conditions
	 * in the appropriat port method
	 *
	 *
	 * @param implementedInterface the implemented interface by the post class
	 * @param port represent the port class
	 * @throws Exception
	 */
	public static void AddDynamicConformityCode(CtClass implementedInterface, CtClass port) throws Exception {

		//get all the methods of the interface
		CtMethod[] interfaceMethods = implementedInterface.getDeclaredMethods();

		//for each method of the interface get its annotations and add the proper verification code in the matching class method
		for (CtMethod interfaceMethod : interfaceMethods) {

			Object[] methodAnnotations = interfaceMethod.getAnnotations();
			CtMethod cm = port.getDeclaredMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());

			//for each annotation add the proper code verification
			for(Object annotation : methodAnnotations ){

				if(annotation instanceof Pre){
					String expression;
					expression = ((Pre)annotation).expression();
					cm.insertBefore("if (!(" + expression + "))" + "throw new fr.sorbonne_u.components.exceptions.PreconditionException(\""+expression+"\");");
				}
				else if(annotation instanceof Post){
					String expression = ((Post)annotation).value();
					String modifiedExpression = expression.replaceAll("\\b" + "ret" + "\\b", "\\$_"); 
					cm.insertAfter("if (!(" + modifiedExpression + "))" + "throw new fr.sorbonne_u.components.exceptions.PostconditionException(\""+expression+"\");");
				}
			}
		}

		//add the map <Method,<List<ContractI>> to the outboundPort constructors
		for(CtConstructor constructor : port.getDeclaredConstructors()){
			constructor.insertAfter("this.owner.getContractTypeMap().putAll((java.util.Map)"+DynamicConformance.class.getCanonicalName()+".getInterfaceContractMap(this.getImplementedInterface()));");
		}

		port.writeFile();

	}

	/**
	 * this method generates a map of method as key and their contract as value from an interface
	 * @param IR
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static Map<Method, List<ContractI>> getInterfaceContractMap(Class<?> implementedInterface) throws NoSuchMethodException, SecurityException {
		//QoS type of contracts in order to manipulate dimensions
		Map<Method, List<ContractI>> contractTypeMap = new HashMap<Method, List<ContractI>>();

		//get the interface Annotations
		Object[] interfaceAnnotations = implementedInterface.getAnnotations();

		//get all the methods of the interface
		Method[] methodsIR = implementedInterface.getDeclaredMethods();

		//store the contract definitions and the required contract names (the once which are required for all the methods of the interface)
		ContractDefinition[] contractDefinitionList = null;
		List<String> require = new ArrayList<String>();

		//for each interface annotation do whatever ...
		for(Object annotation : interfaceAnnotations ){

			if(annotation instanceof ContractDefinition.List){
				contractDefinitionList = ((ContractDefinition.List)annotation).value();
			}
			else if(annotation instanceof Require){
				require.add(((Require)annotation).contractName());
			}
		}

		//for each method of the interface get its annotations
		for (Method method : methodsIR) {

			Object[] methodAnnotations = method.getAnnotations();

			//listContrat
			List<ContractI> list = new ArrayList<ContractI>();

			for(Object annotation : methodAnnotations ){
				if (annotation instanceof Require){
					String contractName = ((Require)annotation).contractName();
					ContractI contract = getContractTypeFromContractDefinitionList(contractDefinitionList,contractName);
					list.add(contract);
				} else if (annotation instanceof RequireContract){
					ContractI contract = new ContractI() {
						@Override
						public String getName() {
							return "noName";
						}

						@Override
						public Class<? extends ContractTypeI> getType() {
							return ((RequireContract)annotation).contractType();
						}

						@Override
						public String[] getConstraints() {
							return ((RequireContract)annotation).constraints();
						}
					};
					list.add(contract);
				}
			}

			//add the contract required for all methods
			for(String contractName : require){
				ContractI contract = getContractTypeFromContractDefinitionList(contractDefinitionList,contractName);
				list.add(contract);
			}
			contractTypeMap.put(method, list);
		}

		return contractTypeMap;
	}

	/**
	 * get the appropriat <code>ContractI</code> from a list of contract definitions
	 *
	 * @param contractDefinitionList
	 * @param contractName
	 * @return
	 */
	private static ContractI getContractTypeFromContractDefinitionList(ContractDefinition[] contractDefinitionList, String contractName) {

		for (ContractDefinition ccd : contractDefinitionList) {
			if(ccd.name().equals(contractName)){
				return new ContractI() {
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
				};
			}
		}
		return null;

	}
}