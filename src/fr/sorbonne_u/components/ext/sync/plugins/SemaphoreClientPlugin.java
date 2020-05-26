package fr.sorbonne_u.components.ext.sync.plugins;

// Copyright Jacques Malenfant, Sorbonne Universite.
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

import fr.sorbonne_u.components.AbstractPlugin;
import fr.sorbonne_u.components.ComponentI;
import fr.sorbonne_u.components.ext.sync.components.SemaphoreI;
import fr.sorbonne_u.components.ext.sync.connectors.SemaphoreServicesConnector;
import fr.sorbonne_u.components.ext.sync.interfaces.SemaphoreServicesCI;
import fr.sorbonne_u.components.ext.sync.ports.SemaphoreServicesOutboundPort;
import fr.sorbonne_u.components.reflection.connectors.ReflectionConnector;
import fr.sorbonne_u.components.reflection.interfaces.ReflectionI;
import fr.sorbonne_u.components.reflection.ports.ReflectionOutboundPort;

// -----------------------------------------------------------------------------
/**
 * The class <code>SemaphoreClientPlugin</code> implements a client-side plug-in
 * to be used by components that need to use a semaphore component.
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
public class			SemaphoreClientPlugin
extends		AbstractPlugin
implements	SemaphoreI
{
	// -------------------------------------------------------------------------
	// Plug-in variables and constants
	// -------------------------------------------------------------------------

	private static final long				serialVersionUID = 1L;
	/** the URI of the reflection inbound port of the semaphore component.	*/
	protected String						semaphoreReflectionInboundPortURI ;
	protected SemaphoreServicesOutboundPort	ssop ;

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * create a plug-in instance.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	pluginURI != null
	 * pre	semaphoreReflectionInboundPortURI != null
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @param pluginURI							URI of this plug-in instance.
	 * @param semaphoreReflectionInboundPortURI	URI of the semaphore component inbound port URI.
	 * @throws Exception						<i>to do.</i>
	 */
	public					SemaphoreClientPlugin(
		String pluginURI,
		String semaphoreReflectionInboundPortURI
		) throws Exception
	{
		super() ;

		assert	pluginURI != null ;
		assert	semaphoreReflectionInboundPortURI != null ;

		this.setPluginURI(pluginURI) ;
		this.semaphoreReflectionInboundPortURI =
										semaphoreReflectionInboundPortURI ;
	}

	// -------------------------------------------------------------------------
	// Life cycle
	// -------------------------------------------------------------------------

	/**
	 * @see AbstractPlugin#installOn(ComponentI)
	 */
	@Override
	public void				installOn(ComponentI owner)
	throws Exception
	{
		super.installOn(owner) ;

		this.addRequiredInterface(SemaphoreServicesCI.class) ;
		this.ssop = new SemaphoreServicesOutboundPort(owner) ;
		this.ssop.publishPort() ;
	}

	/**
	 * @see AbstractPlugin#initialise()
	 */
	@Override
	public void				initialise() throws Exception
	{
		this.addRequiredInterface(ReflectionI.class) ;
		ReflectionOutboundPort rop = new ReflectionOutboundPort(this.owner) ;
		rop.publishPort() ;
		this.owner.doPortConnection(
				rop.getPortURI(),
				this.semaphoreReflectionInboundPortURI,
				ReflectionConnector.class.getCanonicalName());

		String[] uris =
			rop.findInboundPortURIsFromInterface(SemaphoreServicesCI.class) ;
		assert	uris != null && uris.length == 1 ;
		this.owner.doPortDisconnection(rop.getPortURI()) ;
		rop.unpublishPort() ;
		rop.destroyPort() ;
		this.removeRequiredInterface(ReflectionI.class) ;

		this.owner.doPortConnection(
				this.ssop.getPortURI(),
				uris[0],
				SemaphoreServicesConnector.class.getCanonicalName());
	}

	/**
	 * @see AbstractPlugin#finalise()
	 */
	@Override
	public void				finalise() throws Exception
	{
		this.owner.doPortDisconnection(this.ssop.getPortURI()) ;
	}

	/**
	 * @see AbstractPlugin#uninstall()
	 */
	@Override
	public void				uninstall() throws Exception
	{
		this.ssop.unpublishPort() ;
		this.ssop.destroyPort() ;
		this.removeRequiredInterface(SemaphoreServicesCI.class) ;
	}

	// -------------------------------------------------------------------------
	// Plug-in services implementation
	// -------------------------------------------------------------------------

	/**
	 * @see SemaphoreI#acquire()
	 */
	@Override
	public void				acquire() throws Exception {
		this.ssop.acquire() ;
	}

	/**
	 * @see SemaphoreI#acquire(int)
	 */
	@Override
	public void				acquire(int permits) throws Exception {
		this.ssop.acquire(permits) ;
	}

	/**
	 * @see SemaphoreI#availablePermits()
	 */
	@Override
	public int				availablePermits() throws Exception {
		return this.ssop.availablePermits() ;
	}

	/**
	 * @see SemaphoreI#hasQueuedThreads()
	 */
	@Override
	public boolean			hasQueuedThreads() throws Exception {
		return this.ssop.hasQueuedThreads() ;
	}

	/**
	 * @see SemaphoreI#release()
	 */
	@Override
	public void				release() throws Exception {
		this.ssop.release() ;
	}

	/**
	 * @see SemaphoreI#release(int)
	 */
	@Override
	public void				release(int permits) throws Exception {
		this.ssop.release(permits) ;
	}

	/**
	 * @see SemaphoreI#tryAcquire()
	 */
	@Override
	public void				tryAcquire() throws Exception {
		this.ssop.tryAcquire() ;
	}

	/**
	 * @see SemaphoreI#tryAcquire(int)
	 */
	@Override
	public void				tryAcquire(int permits) throws Exception {
		this.ssop.tryAcquire(permits) ;
	}
}
// -----------------------------------------------------------------------------
