package fr.sorbonne_u.components.qos.qml.Translator;

import fr.sorbonne_u.components.ports.AbstractInboundPort;
import fr.sorbonne_u.components.ports.AbstractOutboundPort;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.Translator;

public class DynamicConformity implements Translator {

	//The method onLoad() is called before javassist.Loader loads a class, onLoad() can modify the definition of the loaded class
	@Override
	public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {

		CtClass CurrentClazz = pool.getCtClass(classname);
		CtClass abstractOutboundPort = pool.getCtClass(AbstractOutboundPort.class.getCanonicalName());
		CtClass abstractInboundPort = pool.getCtClass(AbstractInboundPort.class.getCanonicalName());

		//if the currentClazz is an outboundPort or inboundPort check conformity add DynamicConformityCode with its interfaces
		if(CurrentClazz.getSuperclass() == abstractOutboundPort || CurrentClazz.getSuperclass() == abstractInboundPort){
			CtClass[]interfaces = CurrentClazz.getInterfaces();
			for(CtClass implementedInterface : interfaces ){
				try {
					CurrentClazz.defrost();
					DynamicConformance.AddDynamicConformityCode(implementedInterface, CurrentClazz);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	//The method start() is called when this event listener is added to a javassist.Loader object by addTranslator()
	@Override
	public void start(ClassPool arg0) throws NotFoundException, CannotCompileException {
		// TODO Auto-generated method stub
	}

}