package fr.sorbonne_u.components.ext.sync.examples.semaphore;

// Copyright Jacques Malenfant, Sorbonne Universite.
//
// Jacques.Malenfant@lip6.fr
//
// This software is a computer program whose purpose is to provide a
// basic component programming model to program with components
// distributed applications in the Java programming language.
//
// This software is governed by the CeCILL-C license under French law and
// abiding by the rules of distribution of free software.  You can use,
// modify and/ or redistribute the software under the terms of the
// CeCILL-C license as circulated by CEA, CNRS and INRIA at the following
// URL "http://www.cecill.info".
//
// As a counterpart to the access to the source code and  rights to copy,
// modify and redistribute granted by the license, users are provided only
// with a limited warranty  and the software's author,  the holder of the
// economic rights,  and the successive licensors  have only  limited
// liability. 
//
// In this respect, the user's attention is drawn to the risks associated
// with loading,  using,  modifying and/or developing or reproducing the
// software by the user in light of its specific status of free software,
// that may mean  that it is complicated to manipulate,  and  that  also
// therefore means  that it is reserved for developers  and  experienced
// professionals having in-depth computer knowledge. Users are therefore
// encouraged to load and test the software's suitability as regards their
// requirements in conditions enabling the security of their systems and/or 
// data to be ensured and,  more generally, to use and operate it in the 
// same conditions as regards security. 
//
// The fact that you are presently reading this means that you have had
// knowledge of the CeCILL-C license and that you accept its terms.

import fr.sorbonne_u.components.AbstractComponent;
import fr.sorbonne_u.components.cvm.AbstractDistributedCVM;
import fr.sorbonne_u.components.ext.sync.components.SemaphoreComponent;

// -----------------------------------------------------------------------------
/**
 * The class <code>DistributedCVM</code> implements a distributed component
 * deployment for an example showing how to use a semaphore component.
 *
 * <p><strong>Description</strong></p>
 * 
 * <p><strong>Invariant</strong></p>
 * 
 * <pre>
 * invariant		true
 * </pre>
 * 
 * <p>Created on : 2019-04-03</p>
 * 
 * @author	<a href="mailto:Jacques.Malenfant@lip6.fr">Jacques Malenfant</a>
 */
public class					DistributedCVM
extends		AbstractDistributedCVM
{
	public static final String	SEMC_URI = "semaphore" ;

	public					DistributedCVM(String[] args)
	throws Exception
	{
		super(args);
	}

	/**
	 * @see AbstractDistributedCVM#instantiateAndPublish()
	 */
	@Override
	public void				instantiateAndPublish() throws Exception
	{
		if (thisJVMURI.equals("semaphore")) {
			AbstractComponent.createComponent(
				SemaphoreComponent.class.getCanonicalName(),
				new Object[]{SEMC_URI,	// imposed reflection inbound port URI
							 1			// number of permits of the semaphore
							}) ;
		} else if (thisJVMURI.equals("client1")) {
			AbstractComponent.createComponent(
				ClientComponent.class.getCanonicalName(),
				new Object[]{SEMC_URI,	// URI of the reflection inbound port
										// of the semaphore component.
							 0			// number uniquely identifying the
							 			// client component.
							}) ;
		} else if (thisJVMURI.equals("client2")) {
			AbstractComponent.createComponent(
				ClientComponent.class.getCanonicalName(),
				new Object[]{SEMC_URI,	// URI of the reflection inbound port
										// of the semaphore component.
							 1			// number uniquely identifying the
							 			// client component.
							}) ;
		} else {
			System.out.println("Unknown JVM URI: " + thisJVMURI) ;
		}

		super.instantiateAndPublish();
	}

	public static void		main(String[] args)
	{
		try {
			DistributedCVM dcvm = new DistributedCVM(args) ;
			dcvm.startStandardLifeCycle(15000L) ;
			Thread.sleep(10000L) ;
			System.exit(0) ;
		} catch (Exception e) {
			throw new RuntimeException(e) ;
		}
	}
}
// -----------------------------------------------------------------------------
