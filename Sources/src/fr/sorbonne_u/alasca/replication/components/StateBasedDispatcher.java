package fr.sorbonne_u.alasca.replication.components;

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

import fr.sorbonne_u.alasca.replication.interfaces.ReplicableCI;
import fr.sorbonne_u.components.annotations.OfferedInterfaces;
import fr.sorbonne_u.components.annotations.RequiredInterfaces;
import fr.sorbonne_u.components.exceptions.ComponentShutdownException;
import fr.sorbonne_u.components.exceptions.ComponentStartException;
import fr.sorbonne_u.components.ports.OutboundPortI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import fr.sorbonne_u.alasca.replication.interfaces.NotificationsCI.StateI;
import fr.sorbonne_u.alasca.replication.ports.NotificationsInboundPort;
import fr.sorbonne_u.alasca.replication.interfaces.PortFactoryI;
import fr.sorbonne_u.alasca.replication.examples.components.MonitoredServer.ServerState;
import fr.sorbonne_u.alasca.replication.interfaces.NotificationsCI;

// -----------------------------------------------------------------------------
/**
 * The class <code>StateBasedDispatcher</code> implements a request dispatcher
 * that selects for each request the server which has the shortest waiting
 * queue.
 *
 * <p><strong>Description</strong></p>
 * 
 * <p>
 * To select the dispatcher with the shortest queue, this component will need
 * to get the information from its attached servers. Doing this by calling
 * each server each time a request must be dispatched would impose an
 * unreasonable delay to the dispatching going against the very idea of
 * dispatching by making the dispatcher a bottleneck.
 * </p>
 * <p>
 * Here, the component implements an asynchronous notification for the queue
 * sizes. Each server will transmit regularly their queue size using an
 * asynchronous task executed at fixed rate. These tasks call the method
 * <code>acceptNewServerState</code> passing their current state as
 * parameter. With this information, this method updates the currently
 * selected server, the one with the shortest queue. The, the <code>call</code>
 * method has only to call this server when it processes a request.
 * </p>
 * <p>
 * In order to allow an unlimited parallel processing of requests, the
 * <code>call</code> method is not executed by threads own by this component
 * but rather by the threads of the caller component. To achieve this, the
 * inbound port simply calls the method instead of submitting it to a pool of
 * threads own by this component). Of course, this imposes that the
 * <code>call</code> method be thread safe.
 * </p>
 * <p>
 * In order to make some comparisons, it is possible to run this dispatcher
 * in a round-robin mode <i>i.e.</i>, the servers will be called using a
 * round-robin dispatching policy. When all servers are of the same processing
 * power, the results are similar to the shortest-queue policy, but when the
 * servers are not of the same power, the shortest-queue policy will make the
 * average queue size be more equal by sending less requests to the slower
 * servers (hence, faster ones process more requests).
 * </p>
 * 
 * <p><strong>Invariant</strong></p>
 * 
 * <pre>
 * invariant		true
 * </pre>
 * 
 * <p>Created on : 2020-03-20</p>
 * 
 * @author	<a href="mailto:Jacques.Malenfant@lip6.fr">Jacques Malenfant</a>
 */
// -----------------------------------------------------------------------------
@OfferedInterfaces(offered = {ReplicableCI.class,NotificationsCI.class})
@RequiredInterfaces(required = {ReplicableCI.class})
// -----------------------------------------------------------------------------
public class			StateBasedDispatcher<T>
extends		ReplicationManagerNonBlocking<T>
{
	// -------------------------------------------------------------------------
	// Constants and variables
	// -------------------------------------------------------------------------

	/** URI of the notification inbound port to receive state (queue sizes)
	 *  information from the servers.										*/
	public static final String				NOTIFICATION_INBOUNDPORT_URI =
													"notification ipURI" ;
	/** URI of the pool of threads used to process the notifications.		*/
	public static final String				NOTIFICATIONS_POOL_URI =
													"notification pool URI" ;
	/** the number of threads used by the notification processing pool of
	 *  threads.															*/
	protected static final int				NOTIFICATIONS_POOL_SIZE = 3 ;

	/** the notification inbound port to receive state (queue sizes)
	 *  information from the servers.										*/
	protected NotificationsInboundPort		notificationsInboundPort ;
	/** map from server inbound port URIs to the outbound port that must
	 *  be used to call this server.									 	*/
	protected Map<String,OutboundPortI>		inboundPortURI2outboundPort ;

	/** the outbound port of the currently selected server, the one which
	 *  queue is the shortest.												*/
	protected OutboundPortI					selectedOut ;
	/** the URI of the inbound port of the currently selected server.		*/
	protected String						selectedInURI ;
	/** the size of the queue of the currently selected server.				*/
	protected int							selectedSize ;
	/** map from server's inbound port URI to their latest reported queue
	 *  size; an invariant connect queuSizes, selectedOut, selectedInURI
	 *  and selectedSize: the three latter represents the server which has
	 *  the shortest queue in queueSizes so the intrinsic lock of queueSizes
	 *  will be used to enforce the mutual exclusion in the access to these
	 *  four pieces of information.											*/
	protected HashMap<String,Integer>		queueSizes = new HashMap<>() ;

	/** true to force a run with round robin dispatching policy and false
	 *  to have a shortest-queue dispatching policy.						*/
	protected static final boolean			TEST_ROUND_ROBIN = true ;
	/** number of available servers.										*/
	protected final int						numberOfServers ;
	/** in a round robin run, gives the index of the next server to be
	 *  called.																*/
	protected AtomicInteger					next ;

	/**
	 * return a string representing the content of {@code queueSizes}.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	queueSizes != null
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @param queueSizes	the map of inbound port URIs to queue sizes.
	 * @return				a string representing the content of {@code queueSizes}.
	 */
	protected static String		printCurrentStates(
		HashMap<String,Integer> queueSizes
		)
	{
		StringBuffer sb = new StringBuffer("[") ;
		int i = 0 ;
		for (Entry<String,Integer> e : queueSizes.entrySet()) {
			sb.append("(") ;
			sb.append(e.getKey()) ;
			sb.append(" -> ") ;
			sb.append(e.getValue()) ;
			sb.append(")") ;
			i++ ;
			if (i < queueSizes.size() - 1) {
				sb.append(", ") ;
			}
 		}
		sb.append("]") ;
		return sb.toString() ;
	}

	/**
	 * creating a dispatcher.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	{@code ownInboundPortURI != null}
	 * pre	{@code portCreator != null}
	 * pre	{@code serverInboundPortURIs != null && serverInboundPortURIs.length > 0}
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @param ownInboundPortURI			URI of the inbound port of this component.
	 * @param portCreator				a port factory to create inbound and outbound ports.
	 * @param serverInboundPortURIs		URIs of the inbound ports of the server to connect this component.
	 * @throws Exception				<i>to do</i>.
	 */
	@SuppressWarnings("unchecked")
	protected			StateBasedDispatcher(
		String ownInboundPortURI,
		PortFactoryI portCreator,
		String[] serverInboundPortURIs
		) throws Exception
	{
		super(1, ownInboundPortURI, (o -> o), CallMode.SINGLE,
			  (o -> (T)new Object[] {o}), portCreator, serverInboundPortURIs) ;
		this.numberOfServers = serverInboundPortURIs.length ;
		this.initialise() ;
	}

	/**
	 * creating a dispatcher.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	{@code ownInboundPortURI != null}
	 * pre	{@code portCreator != null}
	 * pre	{@code serverInboundPortURIs != null && serverInboundPortURIs.length > 0}
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @param reflectionInboundPortURI	URI of the reflection inbound port of this component.
	 * @param ownInboundPortURI			URI of the inbound port of this component.
	 * @param portCreator				a port factory to create inbound and outbound ports.
	 * @param serverInboundPortURIs		URIs of the inbound ports of the server to connect this component.
	 * @throws Exception				<i>to do</i>.
	 */
	@SuppressWarnings("unchecked")
	protected			StateBasedDispatcher(
		String reflectionInboundPortURI,
		String ownInboundPortURI,
		PortFactoryI portCreator,
		String[] serverInboundPortURIs
		) throws Exception
	{
		super(reflectionInboundPortURI, 1, ownInboundPortURI,
			  (o -> o), CallMode.SINGLE, (o -> (T)new Object[] {o}),
			  portCreator, serverInboundPortURIs) ;
		this.numberOfServers = serverInboundPortURIs.length ;
		this.initialise() ;
	}

	/**
	 * initialise the dispatcher.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true			// no precondition.
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @throws Exception	<i>to do</i>.
	 */
	protected void		initialise() throws Exception
	{
		this.createNewExecutorService(NOTIFICATIONS_POOL_URI,
									  NOTIFICATIONS_POOL_SIZE,
									  false) ;
		this.notificationsInboundPort =
			new NotificationsInboundPort(NOTIFICATION_INBOUNDPORT_URI, this) ;
		this.notificationsInboundPort.publishPort() ;

	}

	/**
	 * @see ReplicationManager#start()
	 */
	@Override
	public void			start() throws ComponentStartException
	{
		super.start() ;

		this.next = new AtomicInteger(0) ;

		// Initialise the map from server inbound port URI to the outbound ports
		// used to call them.
		HashMap<String,OutboundPortI> temp =
										new HashMap<String,OutboundPortI>() ;
		for (int i = 0 ; i < this.outboundPorts.length ; i++) {
			try {
				temp.put(this.outboundPorts[i].getServerPortURI(),
						 this.outboundPorts[i]) ;
			} catch (Exception e) {
				throw new ComponentStartException(e) ;
			}
		}
		// As this information will not change during the execution, make it
		// immutable.
		this.inboundPortURI2outboundPort = Collections.unmodifiableMap(temp) ;

		// Initialise the queue sizes to MAX_VALUE, so the selected server will
		// change with the first sizes transmitted by servers.
		for (int i = 0 ; i < this.serverInboundPortURIs.length; i++) {
			this.queueSizes.put(this.serverInboundPortURIs[i],
								Integer.MAX_VALUE) ;
		}
		// Arbitrarily choose the first server to send the first requests.
		// As the execution of servers have not begun, there is no need to
		// enforce a mutual exclusion here.
		this.selectedOut = this.outboundPorts[0] ;
		try {
			this.selectedInURI = this.selectedOut.getServerPortURI() ;
		} catch (Exception e) {
			throw new ComponentStartException(e) ;
		}
		this.selectedSize = Integer.MAX_VALUE ;
	}

	/**
	 * @see ReplicationManager#shutdown()
	 */
	@Override
	public void			shutdown() throws ComponentShutdownException
	{
		try {
			this.notificationsInboundPort.unpublishPort() ;
		} catch (Exception e) {
			throw new ComponentShutdownException(e) ;
		}
		super.shutdown();
	}

	/**
	 * receive the current state (size of the queue) from the servers and
	 * update the selected one (the one with the shortest queue).
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	s != null ;
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @param s		the current state of a server.
	 */
	public void			acceptNewServerState(StateI s)
	{
		assert	s != null ;

		ServerState serverState = (ServerState) s ;

		// For tracing purposes, in debug mode, uncomment to see what is going
		// on during small execution scenarios, but for large ones, it generates
		// too much trace, which modifies the behaviour of the system.
//		StringBuffer sb = new StringBuffer() ;
//		sb.append("new server state ") ;
//		sb.append(serverState.getInboundPortURI()) ;
//		sb.append(" with new size ") ;
//		sb.append(serverState.getQueueSize()) ;
//		sb.append(".\n") ;
//		synchronized (this.tracer) {
//			this.traceMessage(sb.toString()) ;
//		}

		// Extract the information from the state object.
		String uri = serverState.getInboundPortURI() ;
		OutboundPortI p = this.inboundPortURI2outboundPort.get(uri) ;
		int size = serverState.getQueueSize() ;

		// For tracing purposes.
//		String sizes = null ;
//		String theSelectedInURI = null ;
		// the intrinsic lock of this.queueSizes is used to enforce the
		// access to the information about the server selection in mutual
		// exclusion (queueSizes, selectedOut, selectedInURI, selectedSize.
		synchronized (this.queueSizes) {
			// update the size of the queue of the server which state has just
			// been received.
			int old = this.queueSizes.put(uri, size) ;
			if (uri.equals(this.selectedInURI)) {
				// the server is already the selected one so update the size.
				this.selectedSize = size ;
				if (size > old) {
					// the current best may not be the best anymore, look up
					// for a possibly new champion.
					for(Entry<String,Integer> entry :
												this.queueSizes.entrySet()) {
						if (entry.getValue() < this.selectedSize) {
							this.selectedInURI = entry.getKey() ;
							this.selectedOut =
									this.inboundPortURI2outboundPort.
													get(this.selectedInURI) ;
							this.selectedSize = entry.getValue() ;
						}
					}

				}
			} else {
				// !uri.equals(this.selectedInURI)
				if (size < this.selectedSize) {
					// we have a new best, update the information
					this.selectedOut = p ;
					this.selectedInURI = uri ;
					this.selectedSize = size ;
				}   // otherwise the current champion remains the best.
			}

			// For tracing purposes
//			sizes = printCurrentStates(this.queueSizes) ;
//			theSelectedInURI = this.selectedInURI ;
		}

		// For tracing purposes.
//		StringBuffer mes = new StringBuffer("Current queue sizes: ") ;
//		mes.append(sizes) ;
//		mes.append(" ") ;
//		mes.append(theSelectedInURI) ;
//		mes.append(".\n") ;
//		String smes = mes.toString() ;
//		synchronized (this.tracer) {
//			this.traceMessage(smes) ;
//		}
	}

	/**
	 * @see ReplicationManagerNonBlocking#call(Object[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T			call(Object... parameters) throws Exception
	{
		// This method is meant to be executed by the thread of the caller
		// component (the inbound port does not call handleRequest by directly
		// this method.

		// For tracing purposes.
//		StringBuffer mes = new StringBuffer() ;
//		mes.append("sending request [") ;
//		for (int i = 0 ; i < parameters.length ; i++) {
//			mes.append(parameters[i]) ;
//			if (i < parameters.length - 1) {
//				mes.append(", ") ;
//			}
//		}
//		mes.append(" to ") ;

		// The next lines get in mutual exclusion the currently selected server. 
		ReplicableCI<T> p =  null ;

		// For tracing purposes.
//		String inboundportURI = null ;
//		int size = Integer.MIN_VALUE ;

		if (!TEST_ROUND_ROBIN) {
			synchronized (this.queueSizes) {
				p = (ReplicableCI<T>) this.selectedOut ;

				// For tracing purposes.
//				inboundportURI =  this.selectedInURI ;
//				size = this.selectedSize ;
			}
		} else {
			int n = this.next.updateAndGet(a -> (a + 1) % numberOfServers) ;
			this.traceMessage("selected = " + n + "\n") ;
			p = (ReplicableCI<T>)this.outboundPorts[n] ;
		}

		// For tracing purposes.
//		mes.append(inboundportURI) ;
//		mes.append(" having queue size ") ;
//		mes.append(size) ;
//		mes.append(".\n") ;
//		synchronized (this.tracer) {
//			this.traceMessage(mes.toString()) ;
//		}
//		System.out.print(mes.toString()) ;

		// The call, made by the caller thread.
		T result = p.call(parameters) ;

		// For tracing purposes.
//		synchronized (this.tracer) {
//			this.traceMessage(result.toString() + "\n") ;
//		}

		return result ;
	}
}
// -----------------------------------------------------------------------------
