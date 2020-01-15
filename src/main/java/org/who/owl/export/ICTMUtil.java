package org.who.owl.export;

import edu.stanford.smi.protegex.owl.model.OWLModel;

public class ICTMUtil {
	
	public static final String ICTM_TOP_CLASS = "http://who.int/ictm#ICTMCategory";
	

	public static boolean isICTMOntology(OWLModel owlModel) {
		return owlModel.getRDFSNamedClass(ICTM_TOP_CLASS) != null;
	}
	
}
