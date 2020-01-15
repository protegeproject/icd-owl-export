package org.who.owl.export;


import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class ICDAPIModel {
	
	private OWLOntologyManager manager;
	private OWLDataFactory df;
    private OWLOntology targetOnt;
	
	private OWLAnnotationProperty titleProp;
	private OWLAnnotationProperty defProp;
	private OWLAnnotationProperty longDefProp;
	private OWLAnnotationProperty parentProp;
	private OWLAnnotationProperty childProp;
	private OWLAnnotationProperty synProp;
	
	private OWLAnnotationProperty noteProp;
	private OWLAnnotationProperty codingHintProp;
	
	private OWLAnnotationProperty fullNameProp;
	private OWLAnnotationProperty narrowerProp;
	private OWLAnnotationProperty exclusionProp;
	private OWLAnnotationProperty inclusionProp;
	private OWLAnnotationProperty browserUrlProp;
	private OWLAnnotationProperty foundRefProp;
	private OWLAnnotationProperty isObsoleteProp;
	
	private OWLAnnotationProperty icd10codeProp;
	
	
	public ICDAPIModel(OWLOntologyManager manager, OWLOntology targetOnt) {
		this.manager = manager;
		this.targetOnt = targetOnt;
		this.df = manager.getOWLDataFactory();
		initModel();
	}

	private void initModel() {
		titleProp = createAnnotationProperty(ICDAPIConstants.TITLE);
		defProp = createAnnotationProperty(ICDAPIConstants.DEFINITION);
		longDefProp = createAnnotationProperty(ICDAPIConstants.LONG_DEFINITION);
		parentProp = createAnnotationProperty(ICDAPIConstants.PARENT);
		childProp = createAnnotationProperty(ICDAPIConstants.CHILD);
		synProp = createAnnotationProperty(ICDAPIConstants.SYNONYM);
		
		fullNameProp = createAnnotationProperty(ICDAPIConstants.FULLY_SPECIFIED_NAME);
		narrowerProp = createAnnotationProperty(ICDAPIConstants.NARROWER_TERM);
		exclusionProp = createAnnotationProperty(ICDAPIConstants.EXCLUSION);
		inclusionProp = createAnnotationProperty(ICDAPIConstants.INCLUSION);
		browserUrlProp = createAnnotationProperty(ICDAPIConstants.BROWSER_URL);
		foundRefProp = createAnnotationProperty(ICDAPIConstants.FOUNDATION_REFERENCE);
		
		noteProp = createAnnotationProperty(ICDAPIConstants.NOTE);
		codingHintProp = createAnnotationProperty(ICDAPIConstants.CODING_HINT);
		
		isObsoleteProp = createAnnotationProperty(ICDAPIConstants.IS_OBSOLETE);
		icd10codeProp = createAnnotationProperty(ICDAPIConstants.ICD10CODE);
	}
	
	private OWLAnnotationProperty createAnnotationProperty(String propIRI) {
		OWLAnnotationProperty p = df.getOWLAnnotationProperty(propIRI);
		manager.addAxiom(targetOnt, df.getOWLDeclarationAxiom(p)); //TODO: check if it exists already?
		return p;
	}

	public OWLOntology getTargetOnt() {
		return targetOnt;
	}

	public OWLAnnotationProperty getTitleProp() {
		return titleProp;
	}

	public OWLAnnotationProperty getDefProp() {
		return defProp;
	}

	public OWLAnnotationProperty getLongDefProp() {
		return longDefProp;
	}

	public OWLAnnotationProperty getParentProp() {
		return parentProp;
	}

	public OWLAnnotationProperty getChildProp() {
		return childProp;
	}

	public OWLAnnotationProperty getSynProp() {
		return synProp;
	}
	
	public OWLAnnotationProperty getNoteProp() {
		return noteProp;
	}
	
	public OWLAnnotationProperty getCodingHintProp() {
		return codingHintProp;
	}

	public OWLAnnotationProperty getFullNameProp() {
		return fullNameProp;
	}

	public OWLAnnotationProperty getNarrowerProp() {
		return narrowerProp;
	}

	public OWLAnnotationProperty getExclusionProp() {
		return exclusionProp;
	}

	public OWLAnnotationProperty getInclusionProp() {
		return inclusionProp;
	}

	public OWLAnnotationProperty getBrowserUrlProp() {
		return browserUrlProp;
	}

	public OWLAnnotationProperty getFoundRefProp() {
		return foundRefProp;
	}
	
	public OWLAnnotationProperty getIsObsoleteProp() {
		return isObsoleteProp;
	}
	
	public OWLAnnotationProperty getICD10CodeProp() {
		return icd10codeProp;
	}
	
	public OWLObjectProperty getPostCoordinationProp(String sourceProp) {
		String targetName = sourceProp.replace(ICDAPIConstants.SOURCE_ONT_NS, ICDAPIConstants.TARGET_ONT_NS);
		return df.getOWLObjectProperty(targetName);
	}

}
