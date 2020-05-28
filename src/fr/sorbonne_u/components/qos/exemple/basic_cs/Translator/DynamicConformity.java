package fr.sorbonne_u.components.qos.exemple.basic_cs.Translator;

import fr.sorbonne_u.components.ports.AbstractOutboundPort;
import fr.sorbonne_u.components.qos.ConformanceChecker;
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
		//CtClass abstractInboundPort = pool.getCtClass(AbstractInboundPort.class.getCanonicalName());
		
		//if the currentClazz is an outboundPort check conformity add DynamicConformityCode with its interfaces
		if(CurrentClazz.getSuperclass() == abstractOutboundPort){
			CtClass[]interfaces = CurrentClazz.getInterfaces();
			for(CtClass IR : interfaces ){
				try {
					CurrentClazz.defrost();
					ConformanceChecker.AddDynamicConformityCode(IR, CurrentClazz);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.out.println(classname);
		}
	}
	
	//The method start() is called when this event listener is added to a javassist.Loader object by addTranslator()
	@Override
	public void start(ClassPool arg0) throws NotFoundException, CannotCompileException {
		// TODO Auto-generated method stub
	}
	
}
