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

import fr.sorbonne_u.alasca.replication.components.ReplicationManager;
import fr.sorbonne_u.alasca.replication.connectors.ReplicableConnector;
import fr.sorbonne_u.alasca.replication.examples.components.Client;
import fr.sorbonne_u.alasca.replication.examples.components.ConstantServer;
import fr.sorbonne_u.alasca.replication.examples.components.RandomServer;
import fr.sorbonne_u.alasca.replication.examples.components.Server;
import fr.sorbonne_u.alasca.replication.interfaces.PortFactoryI;
import fr.sorbonne_u.alasca.replication.ports.ReplicableInboundPort;
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
public class			CVM
extends 	AbstractCVM
{
	public static final String[]		SERVER_INBOUND_PORT_URIS =
											new String[]{
												"server-service-1",
												"server-service-2",
												"server-service-3"
											} ;
	public static final String			MANAGER_INBOUND_PORT_URI = "manager" ;

	public static enum SelectorType {
		ROUND_ROBIN,
		RANDOM,
		WHOLE
	}
	protected final SelectorType	currentSelector = SelectorType.ROUND_ROBIN ;

	public static enum CombinatorType {
		FIXED,
		LONE,
		MAJORITY_VOTE,
		RANDOM
	}
	protected final CombinatorType	currentCombinator = CombinatorType.LONE ;

	public static final PortFactoryI PC =
			new PortFactoryI() {
				@Override
				public InboundPortI createInboundPort(ComponentI c)
						throws Exception
				{
					return new ReplicableInboundPort<String>(c) ;
				}

				@Override
				public InboundPortI createInboundPort(String uri, ComponentI c)
						throws Exception
				{
					return new ReplicableInboundPort<String>(uri, c) ;
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

	public				CVM() throws Exception
	{
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
										 0});
		
		AbstractComponent.createComponent(
			ReplicationManager.class.getCanonicalName(),
			new Object[]{
					currentSelector == SelectorType.WHOLE ?
						1
					:	SERVER_INBOUND_PORT_URIS.length,
					MANAGER_INBOUND_PORT_URI,
					(currentSelector == SelectorType.ROUND_ROBIN ?
						new RoundRobinSelector(
								SERVER_INBOUND_PORT_URIS.length)
					:	currentSelector == SelectorType.RANDOM ?
							new RandomSelector()
						:	new WholeSelector()
					),
					(currentCombinator == CombinatorType.FIXED) ?
						new FixedCombinator<String>(1)
					:	currentCombinator == CombinatorType.LONE ?
							new LoneCombinator<String>()
						:	currentCombinator == CombinatorType.MAJORITY_VOTE ?
							new MajorityVoteCombinator<String>(
								(o1,o2) -> o1.equals(o2),
								RuntimeException.class
							)
							:	new RandomCombinator<String>()
					,
					PC,
					SERVER_INBOUND_PORT_URIS
			}) ;

		for (int i = 1 ; i <= 5 ; i++) {
			AbstractComponent.createComponent(
							Client.class.getCanonicalName(),
							new Object[]{MANAGER_INBOUND_PORT_URI,
										 i*1000}) ;
		}

		super.deploy() ;
	}

	public static void	main(String[] args)
	{
		try {
			CVM cvm = new CVM() ;
			cvm.startStandardLifeCycle(10000L) ;
			Thread.sleep(10000L) ;
			System.exit(0) ;
		} catch (Exception e) {
			throw new RuntimeException(e) ;
		}
	}
}
// -----------------------------------------------------------------------------
