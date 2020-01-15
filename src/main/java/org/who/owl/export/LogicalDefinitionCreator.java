package org.who.owl.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protegex.owl.model.OWLHasValue;
import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLIntersectionClass;
import edu.stanford.smi.protegex.owl.model.OWLSomeValuesFrom;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSClass;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class LogicalDefinitionCreator {
	
	private final static Logger log = Logger.getLogger(LogicalDefinitionCreator.class);
	
	private ICDContentModel cm;
	private OWLDataFactory df;
	private ICDAPIModel icdapiModel;
	private OWLOntologyManager manager;
	private OWLOntology targetOnt;
	private ICDPostCoordinationMaps pcMaps;
	
	
	public LogicalDefinitionCreator(ICDContentModel cm, ICDAPIModel icdapiModel,
			OWLOntologyManager manager, OWLOntology targetOnt) {
		this.cm = cm;
		this.icdapiModel = icdapiModel;
		this.manager = manager;
		this.df = manager.getOWLDataFactory();
		this.targetOnt = targetOnt;
	}

	
	public ChangeApplied createLogicalAxioms(RDFSNamedClass sourceCls, OWLClass targetCls) {
		Collection<OWLAxiom> logDefAxs = getLogicalDefinitionAxioms(sourceCls, targetCls);
		return manager.addAxioms(targetOnt, logDefAxs.stream());
	}
	
	
	public Collection<OWLAxiom> getLogicalDefinitionAxioms(RDFSNamedClass sourceCls, OWLClass targetCls) {
		Collection<OWLAxiom> logDefAxs = new ArrayList<OWLAxiom>();
		
		try {
			Collection<OWLAxiom> eqClses = getLogicalDefinitionAxioms(sourceCls.getEquivalentClasses(), targetCls, true);
			logDefAxs.addAll(eqClses);
		
			Collection<OWLAxiom> superClses = getLogicalDefinitionAxioms(sourceCls.getSuperclasses(false), targetCls, false);
			logDefAxs.addAll(superClses);
		} catch (Exception e) {
			log.error("Error at exporting the logical definition for class " + sourceCls.getBrowserText() + ", " + targetCls, e);
		}

		return logDefAxs;
	}
	
	private Collection<OWLAxiom> getLogicalDefinitionAxioms(Collection<RDFSClass> superOrEqClses, OWLClass targetCls, boolean isEquivalent) {
		List<OWLAxiom> axList = new ArrayList<OWLAxiom>();
		if (superOrEqClses == null) {
			return axList;
		}
		for (RDFSClass superOrEqCls : superOrEqClses) {
			if (superOrEqCls instanceof OWLIntersectionClass) {
				OWLObjectIntersectionOf targetInt = getIntersection((OWLIntersectionClass) superOrEqCls);
				OWLAxiom ax = null;
				if (isEquivalent == true) {
					ax = df.getOWLEquivalentClassesAxiom(targetCls, targetInt);
				} else {
					ax = df.getOWLSubClassOfAxiom(targetCls, targetInt);
				}
				axList.add(ax);
				
			}
		}
		return axList;
	}
	
	
	private OWLObjectIntersectionOf getIntersection(OWLIntersectionClass intCls) {
		Collection<OWLClassExpression> targetOps = new ArrayList<OWLClassExpression>();

		for (RDFSClass op : intCls.getOperands()) {
			if (op instanceof RDFSNamedClass) { //maybe check if this is the preCoordParent
				OWLClass cls = df.getOWLClass(PublicIdCache.getPublicId(cm, (RDFSNamedClass) op));
				targetOps.add(cls);
				
			} else if (op instanceof OWLSomeValuesFrom) {
				RDFProperty prop = ((OWLSomeValuesFrom) op).getOnProperty();
				RDFResource filler = ((OWLSomeValuesFrom) op).getFiller();
				OWLClass targetFiller = getClassFiller(filler);
				if (targetFiller != null) {
					OWLObjectProperty targetProp = icdapiModel.getPostCoordinationProp(prop.getName());
					targetOps.add(df.getOWLObjectSomeValuesFrom(targetProp, targetFiller));
				}
				
			} else if (op instanceof OWLHasValue) {
				RDFProperty prop = ((OWLHasValue) op).getOnProperty();
				Object filler = ((OWLHasValue) op).getHasValue();
				boolean isFixedScaleProp = ICDPostCoordinationMaps.isFixedScalePCProp(prop.getName());
				
				org.semanticweb.owlapi.model.OWLIndividual targetFiller = getIndividualFiller(filler, isFixedScaleProp);
				if (targetFiller != null) {
					OWLObjectProperty targetProp = icdapiModel.getPostCoordinationProp(prop.getName());
					targetOps.add(df.getOWLObjectHasValue(targetProp, targetFiller));
				}
				
			} else {
				log.warn("Logical definition operand: " + op.getBrowserText() + " was not converted.");
			}
		}
		
		return df.getOWLObjectIntersectionOf(targetOps);
	}
	
	private OWLClass getClassFiller(Object filler) {
		if (filler instanceof RDFSNamedClass) {
			return df.getOWLClass(PublicIdCache.getPublicId(cm, (RDFSNamedClass) filler));
		} else {
			log.warn("Logical definition filler: " + filler + " was not converted.");
		}
		return null;
	}
	
	private org.semanticweb.owlapi.model.OWLIndividual getIndividualFiller(Object filler, boolean uniqueInd) {
		if (filler instanceof OWLIndividual) {
			RDFSNamedClass refCls = (RDFSNamedClass) ((OWLIndividual)filler).getPropertyValue(cm.getReferencedValueProperty());
			if (refCls == null) {
				log.warn("Logical definition has value was not converted. No referenced class of term " + filler);
				return null;
			}
			String targetFillerClsName = PublicIdCache.getPublicId(cm, (RDFSNamedClass) refCls);
			OWLClass targetFillerCls = df.getOWLClass(targetFillerClsName);
			
			return createFillerIndividual(targetFillerCls, uniqueInd);
		} else {
			log.warn("Logical definition has value filler: " + filler + " was not converted, because it is not an individual.");
		}
		return null;
	}
	
	private org.semanticweb.owlapi.model.OWLIndividual createFillerIndividual(OWLClass targetFillerCls, boolean uniqueInd) {
		org.semanticweb.owlapi.model.OWLIndividual anInd = null;
		
		if (uniqueInd == true) { //for fixed scales, always return the same individual
			anInd = getIndividual(targetFillerCls);
			if (anInd != null) {
				return anInd;
			}
		}
		
		String indName = ICDAPIConstants.TARGET_ONT_NS + UUID.randomUUID();
		anInd = df.getOWLNamedIndividual(indName);
		
		OWLClassAssertionAxiom ax = df.getOWLClassAssertionAxiom(targetFillerCls, anInd);

		manager.addAxiom(targetOnt, ax);
		return anInd;
	}

	private org.semanticweb.owlapi.model.OWLIndividual getIndividual(OWLClass cls) {
		Optional<OWLClassAssertionAxiom> axOpt = targetOnt.classAssertionAxioms(cls).findFirst();
		if (axOpt.isEmpty()) {
			return null;
		}
		return axOpt.get().getIndividual();
	}
	
	public boolean hasLogicalDefinition(RDFSNamedClass sourceCls) {
		RDFProperty precoodParentProp = cm.getPrecoordinationSuperclassProperty();
		if (precoodParentProp == null) {
			return false;
		}
		
		RDFSNamedClass precoordParent = (RDFSNamedClass) sourceCls.getPropertyValue(precoodParentProp); 
		return precoordParent != null;
	}
	
}
