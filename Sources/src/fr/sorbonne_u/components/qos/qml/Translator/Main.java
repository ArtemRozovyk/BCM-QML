package fr.sorbonne_u.components.qos.qml.Translator;

import fr.sorbonne_u.components.qos.exemple.basic_cs.*;
import javassist.ClassPool;
import javassist.Loader;
import javassist.Translator;

public class Main {

	public static void main(String[] args) throws Throwable {
		 Translator dc = new DynamicConformity();
	     ClassPool pool = ClassPool.getDefault();
	     Loader cl = new Loader();
	     cl.addTranslator(pool, dc);
	     cl.run(CVM.class.getCanonicalName(), args);
	}

}
