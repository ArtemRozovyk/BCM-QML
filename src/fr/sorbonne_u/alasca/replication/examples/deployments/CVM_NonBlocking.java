package fr.sorbonne_u.alasca.replication.examples.deployments;

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

import fr.sorbonne_u.alasca.replication.components.ReplicationManagerNonBlocking;
import fr.sorbonne_u.alasca.replication.components.ReplicationManagerNonBlocking.CallMode;
import fr.sorbonne_u.alasca.replication.connectors.ReplicableConnector;
import fr.sorbonne_u.alasca.replication.examples.components.Client;
import fr.sorbonne_u.alasca.replication.examples.components.ConstantServer;
import fr.sorbonne_u.alasca.replication.examples.components.RandomServer;
import fr.sorbonne_u.alasca.replication.examples.components.Server;
import fr.sorbonne_u.alasca.replication.interfaces.PortFactoryI;
import fr.sorbonne_u.alasca.replication.ports.ReplicableInboundPortNonBlocking;
import fr.sorbonne_u.alasca.replication.ports.ReplicableOutboundPort;
import fr.sorbonne_u.alasca.replication.selectors.WholeSelector;
import fr.sorbonne_u.alasca.replication.selectors.RandomSelector;
import fr.sorbonne_u.alasca.replication.selectors.RoundRobinSelector;
import fr.sorbonne_u.components.AbstractComponent;
import fr.sorbonne_u.components.ComponentI;
import fr.sorbonne_u.components.cvm.AbstractCVM;
import fr.sorbonne_u.components.ports.InboundPortI;
import fr.sorbonne_u.components.ports.OutboundPortI;
import fr.sorbonne_u.alasca.replication.combinators.FixedCombinator;
import fr.sorbonne_u.alasca.replication.combinators.LoneCombinator;
import fr.sorbonne_u.alasca.replication.combinators.MajorityVoteCombinator;
import fr.sorbonne_u.alasca.replication.combinators.RandomCombinator;

// -----------------------------------------------------------------------------
/**
 * The class <code>DispatcherCVM</code> implements an example of replication
 * to dispatch calls among servers.
 *
 * <p><strong>Description</strong></p>
 * 
 * <table>
 * <caption>Useful combinations</caption>
 * <tr><td>SINGLE_ROUND_ROBIN</td><td>SINGLE</td><td>LONE</td></tr>
 * <tr><td>SINGLE_RANDOM</td>     <td>SINGLE</td><td>LONE</td></tr>
 * <tr><td>MANY_SUBSET</td>       <td>ANY</td>   <td>LONE</td></tr>
 * <tr><td>MANY_SUBSET</td>       <td>FIRST</td> <td>LONE</td></tr>
 * <tr><td>MANY_SUBSET</td>       <td>ALL</td>   <td>FIXED</td></tr>
 * <tr><td>MANY_SUBSET</td>       <td>ALL</td>   <td>MAJORITY_VOTE</td></tr>
 * <tr><td>MANY_SUBSET</td>       <td>ALL</td>   <td>RANDOM</td></tr>
 * <tr><td>MANY_ALL</td>          <td>ANY</td>   <td>LONE</td></tr>
 * <tr><td>MANY_ALL</td>          <td>FIRST</td> <td>LONE</td></tr>
 * <tr><td>MANY_ALL</td>          <td>ALL</td>   <td>FIXED</td></tr>
 * <tr><td>MANY_ALL</td>          <td>ALL</td>   <td>MAJORITY_VOTE</td></tr>
 * <tr><td>MANY_ALL</td>          <td>ALL</td>    <td>RANDOM</td></tr>
 * </table>
 * 
 * <p><strong>Invariant</strong></p>
 * 
 * <pre>
 * invariant		true
 * </pre>
 * 
 * <p>Created on : 2020-02-28</p>
 * 
 * @author	<a href="mailto:Jacques.Malenfant@lip6.fr">Jacques Malenfant</a>
 */
public class			CVM_NonBlocking
extends 	AbstractCVM
{
	public static final String[]		SERVER_INBOUND_PORT_URIS =
											new String[]{
												"server-service-1",
												"server-service-2",
												"server-service-3"
											} ;
	public static final String			MANAGER_INBOUND_PORT_URI = "manager" ;
	public static final int				NUMBER_OF_CLIENTS = 10 ;

	public static enum SelectorType {
		SINGLE_ROUND_ROBIN,
		SINGLE_RANDOM,
		MANY_SUBSET,
		MANY_ALL
	}

	public static enum CombinatorType {
		LONE,
		FIXED,
		MAJORITY_VOTE,
		RANDOM
	}

	protected final SelectorType	currentSelector = SelectorType.MANY_ALL ;
	protected final int				fixedIndex = 0 ;
	protected final CallMode		currentCallMode = CallMode.ALL ;
	protected final CombinatorType	currentCombinator = CombinatorType.MAJORITY_VOTE ;

	public static final PortFactoryI PC =
			new PortFactoryI() {
				@Override
				public InboundPortI createInboundPort(ComponentI c)
						throws Exception
				{
					return new ReplicableInboundPortNonBlocking<String>(c) ;
				}

				@Override
				public InboundPortI createInboundPort(String uri, ComponentI c)
						throws Exception
				{
					return new ReplicableInboundPortNonBlocking<String>(uri, c) ;
				}

				@Override
				public OutboundPortI createOutboundPort(ComponentI c)
						throws Exception
				{
					return new ReplicableOutboundPort<String>(c) ;
				}

				@Override
				public OutboundPortI createOutboundPort(String uri, ComponentI c)
						throws Exception
				{
					return new ReplicableOutboundPort<String>(uri, c) ;
				}

				@Override
				public String getConnectorClassName() {
					return ReplicableConnector.class.getCanonicalName() ;
				}
			} ;

	public				CVM_NonBlocking() throws Exception
	{
		assert		this.currentSelector == SelectorType.SINGLE_ROUND_ROBIN &&
					this.currentCallMode == CallMode.SINGLE &&
					this.currentCombinator == CombinatorType.LONE
				||	this.currentSelector == SelectorType.SINGLE_RANDOM &&
					this.currentCallMode == CallMode.SINGLE &&
					this.currentCombinator == CombinatorType.LONE
				||	this.currentSelector == SelectorType.MANY_SUBSET &&
					this.currentCallMode == CallMode.ANY &&
					this.currentCombinator == CombinatorType.LONE
				||	this.currentSelector == SelectorType.MANY_SUBSET &&
					this.currentCallMode == CallMode.FIRST &&
					this.currentCombinator == CombinatorType.LONE
				||	this.currentSelector == SelectorType.MANY_SUBSET &&
					this.currentCallMode == CallMode.ALL &&
					this.currentCombinator == CombinatorType.FIXED
				||	this.currentSelector == SelectorType.MANY_SUBSET &&
					this.currentCallMode == CallMode.ALL &&
					this.currentCombinator == CombinatorType.MAJORITY_VOTE
				||	this.currentSelector == SelectorType.MANY_SUBSET &&
					this.currentCallMode == CallMode.ALL &&
					this.currentCombinator == CombinatorType.RANDOM
				||	this.currentSelector == SelectorType.MANY_ALL &&
					this.currentCallMode == CallMode.ANY &&
					this.currentCombinator == CombinatorType.LONE
				||	this.currentSelector == SelectorType.MANY_ALL &&
					this.currentCallMode == CallMode.FIRST &&
					this.currentCombinator == CombinatorType.LONE
				||	this.currentSelector == SelectorType.MANY_ALL &&
					this.currentCallMode == CallMode.ALL &&
					this.currentCombinator == CombinatorType.FIXED
				||	this.currentSelector == SelectorType.MANY_ALL &&
					this.currentCallMode == CallMode.ALL &&
					this.currentCombinator == CombinatorType.MAJORITY_VOTE
				||	this.currentSelector == SelectorType.MANY_ALL &&
					this.currentCallMode == CallMode.ALL &&
					this.currentCombinator == CombinatorType.RANDOM ;
	}

	/**
	 * @see AbstractCVM#deploy()
	 */
	@Override
	public void			deploy() throws Exception
	{
		for (int i = 1 ; i < SERVER_INBOUND_PORT_URIS.length ; i++) {
			AbstractComponent.createComponent(
					(currentCombinator != CombinatorType.MAJORITY_VOTE ?
						Server.class.getCanonicalName()
					:	ConstantServer.class.getCanonicalName()
					),
					new Object[]{"server" + i + "-",
								 SERVER_INBOUND_PORT_URIS[i],
								 i}) ;
		}
		AbstractComponent.createComponent(
							RandomServer.class.getCanonicalName(),
							new Object[]{"random-server-",
										 SERVER_INBOUND_PORT_URIS[0],
										 0}) ;
		
		this.createReplicationManager() ;

		for (int i = 1 ; i <= NUMBER_OF_CLIENTS ; i++) {
			AbstractComponent.createComponent(
							Client.class.getCanonicalName(),
							new Object[]{MANAGER_INBOUND_PORT_URI, i*1000}) ;
		}

		super.deploy() ;
	}

	protected void		createReplicationManager() throws Exception
	{
		AbstractComponent.createComponent(
			ReplicationManagerNonBlocking.class.getCanonicalName(),
			new Object[]{
					currentSelector == SelectorType.MANY_ALL ?
						10
					:	SERVER_INBOUND_PORT_URIS.length,
					MANAGER_INBOUND_PORT_URI,
					(currentSelector == SelectorType.SINGLE_ROUND_ROBIN ?
						new RoundRobinSelector(
									SERVER_INBOUND_PORT_URIS.length)
					:	currentSelector == SelectorType.SINGLE_RANDOM ?
							new RandomSelector()
						:	new WholeSelector()
					),
					this.currentCallMode,
					(currentCombinator == CombinatorType.FIXED) ?
						new FixedCombinator<String>(1)
					:	currentCombinator == CombinatorType.LONE ?
							new LoneCombinator<String>()
						:	currentCombinator == CombinatorType.MAJORITY_VOTE ?
							new MajorityVoteCombinator<String>(
												(o1,o2) -> o1.equals(o2),
												RuntimeException.class
												)
							:	new RandomCombinator<String>(),
					PC,
					SERVER_INBOUND_PORT_URIS
				}) ;
	}

	public static void	main(String[] args)
	{
		try {
			CVM_NonBlocking cvm = new CVM_NonBlocking() ;
			cvm.startStandardLifeCycle(10000L) ;
			Thread.sleep(100000L) ;
			System.exit(0) ;
		} catch (Exception e) {
			throw new RuntimeException(e) ;
		}
	}
}
// -----------------------------------------------------------------------------
