package fr.sorbonne_u.components.qos.exemple.basic_cs.interfaces;

//Copyright Jacques Malenfant, Sorbonne Universite.
//
//Jacques.Malenfant@lip6.fr
//
//This software is a computer program whose purpose is to provide a
//basic component programming model to program with components
//distributed applications in the Java programming language.
//
//This software is governed by the CeCILL-C license under French law and
//abiding by the rules of distribution of free software.  You can use,
//modify and/ or redistribute the software under the terms of the
//CeCILL-C license as circulated by CEA, CNRS and INRIA at the following
//URL "http://www.cecill.info".
//
//As a counterpart to the access to the source code and  rights to copy,
//modify and redistribute granted by the license, users are provided only
//with a limited warranty  and the software's author,  the holder of the
//economic rights,  and the successive licensors  have only  limited
//liability. 
//
//In this respect, the user's attention is drawn to the risks associated
//with loading,  using,  modifying and/or developing or reproducing the
//software by the user in light of its specific status of free software,
//that may mean  that it is complicated to manipulate,  and  that  also
//therefore means  that it is reserved for developers  and  experienced
//professionals having in-depth computer knowledge. Users are therefore
//encouraged to load and test the software's suitability as regards their
//requirements in conditions enabling the security of their systems and/or 
//data to be ensured and,  more generally, to use and operate it in the 
//same conditions as regards security. 
//
//The fact that you are presently reading this means that you have had
//knowledge of the CeCILL-C license and that you accept its terms.

import fr.sorbonne_u.components.interfaces.*;
import fr.sorbonne_u.components.qos.*;
import fr.sorbonne_u.components.qos.annotations.*;

//-----------------------------------------------------------------------------

@ContractDefinition(
		name = "systemReliability",
		type= Reliability.class,
		constraints=
				{"numberOfFailures < 5",
						"availability > 0.9"}
)
@ContractDefinition(
		name = "systemRepairability",
		type = Reliability.class,
		constraints=
				{"timeToRepair < 800"}
)
@Require(contractName = "systemReliability") //for each method.
public interface URIProviderI
		extends OfferedI
{
	@RequireContract(
			contractType=Performance.class,
			constraints= {"delay < 3000"}
	)
	@Post("ret.length() > 8 && ret.length() < 15")
	public String getURI() throws Exception ;

	@Pre(expression = "numberOfURIs > 1 && numberOfURIs < 5 ", args = {"a","b"})
	@RequireContract(
			contractType=Performance.class,
			constraints= {"delay < 7000", "throughput > 0.13"}
	) //troughput = number of uri per minute
	public String[]	getURIs(int numberOfURIs) throws Exception ;

	@Pre(expression = "x > 5 && y > 5  ", args = {"a","b"})
	@Post("ret < 100 && ret > 10")
	int doSomeOperation(int x, int y) throws Exception;

	@Require(contractName = "systemRepairability")
	void otherOperation() throws Exception;



}
//-----------------------------------------------------------------------------
