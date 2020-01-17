package org.who.owl.export;

import java.util.ArrayList;
import java.util.Collection;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import edu.stanford.bmir.whofic.WHOFICContentModelConstants;
import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ClassExporter {

	private OWLModel sourceOnt;
	
	private OWLOntologyManager manager;
	private OWLDataFactory df;
    private OWLOntology targetOnt;
    
    private ICDContentModel cm;
    private ICDAPIModel icdapiModel;
    private LogicalDefinitionCreator logDefCreator;
    
    private RDFSNamedClass sourceCls;
    
    private boolean isICTM = false;
    

	public ClassExporter(RDFSNamedClass cls, OWLModel sourceOnt, OWLOntologyManager manager, 
			OWLOntology targetOnt, ICDContentModel cm, ICDAPIModel icdapiModel, boolean isICTM) {
		this.sourceCls = cls;
		this.sourceOnt = sourceOnt;
		this.manager = manager;
		this.df = manager.getOWLDataFactory();
		this.targetOnt = targetOnt;
		this.cm = cm;
		this.icdapiModel = icdapiModel;
		this.isICTM = isICTM;
		this.logDefCreator = new LogicalDefinitionCreator(cm, icdapiModel, manager, targetOnt);
	}
    
	public OWLClass export() {
		OWLClass cls = createCls(PublicIdCache.getPublicId(cm, sourceCls));
		
		addTitle(cls);
		addDefinition(cls);
		addLongDefinition(cls);
		
		addFullySpecifiedTitle(cls);
		addCodingHint(cls);
		addNote(cls);
		
		addPublicBrowserLink(cls);
		
		addSyns(cls);
		addNarrower(cls);
		
		addInclusions(cls);
		addExclusions(cls);
		
		addIsObosolte(cls);
		
		addLogicalDefinition(cls);
		
		return cls;
	}




	@SuppressWarnings("deprecation")
	private void addExclusions(OWLClass cls) {
		if (isICTM == true) {
			addStringAnnotations(cls, icdapiModel.getExclusionProp(), cm.getExclusionProperty());
		} else {
			addReferenceAnnotations(cls, icdapiModel.getExclusionProp(), cm.getBaseExclusionProperty());
		}
	}

	/**
	 * Putting together here the indexBaseInclusions and the subclassBaseInclusions, for 
	 * consistency purpose. The value of the annotation will be the text,
	 * either the actual text for the indexBaseInclusions, or the title for the
	 * subclassBaseInclusions. For the latter, we will also add an annotation on the
	 * annotation with the foundationReference as a link to the actual subclass, as
	 * is in the API.
	 * 
	 * The two types of inclusions can be easily split.
	 * 
	 * @param cls
	 */
	private void addInclusions(OWLClass cls) {
		if (isICTM == true) {
			addSimpleInclusions(cls);
		} else {
			addIndexBaseInclusions(cls);
			addSubclassBaseInclusions(cls);
		}
	}

	@SuppressWarnings("deprecation")
	private void addSimpleInclusions(OWLClass cls) {
		addStringAnnotations(cls, icdapiModel.getInclusionProp(), cm.getInclusionProperty());
	}

	private void addSubclassBaseInclusions(OWLClass cls) {
		addReferenceAnnotations(cls, icdapiModel.getInclusionProp(), cm.getSubclassBaseInclusionProperty());
	}

	private void addIndexBaseInclusions(OWLClass cls) {
		addStringAnnotations(cls, icdapiModel.getInclusionProp(), cm.getIndexBaseInclusionProperty());
	}

	private void addNarrower(OWLClass cls) {
		addStringAnnotations(cls, icdapiModel.getNarrowerProp(), cm.getNarrowerProperty());
	}

	private void addSyns(OWLClass cls) {
		addStringAnnotations(cls, icdapiModel.getSynProp(), cm.getSynonymProperty());
	}

	private void addPublicBrowserLink(OWLClass cls) {
		OWLAnnotation ann = df.getOWLAnnotation(icdapiModel.getBrowserUrlProp(), IRI.create(StringUtils.getSimplePublicBrowserLink(cls.getIRI().toString())));
		manager.addAxiom(targetOnt, df.getOWLAnnotationAssertionAxiom(cls.getIRI(), ann));
	}

	private void addNote(OWLClass cls) {
		addStringAnnotation(cls, icdapiModel.getNoteProp(), cm.getNoteProperty());
	}

	private void addCodingHint(OWLClass cls) {
		addStringAnnotation(cls, icdapiModel.getCodingHintProp(), cm.getCodingHintProperty());
	}

	private void addFullySpecifiedTitle(OWLClass cls) {
		addStringAnnotation(cls, icdapiModel.getFullNameProp(), sourceOnt.getRDFProperty(WHOFICContentModelConstants.FULLY_SPECIFIED_NAME_PROP));
	}

	private void addTitle(OWLClass cls) {
		addStringAnnotations(cls, icdapiModel.getTitleProp(), cm.getIcdTitleProperty());
	}
	
	private void addDefinition(OWLClass cls) {
		addStringAnnotations(cls, icdapiModel.getDefProp(), cm.getDefinitionProperty());
	}

	private void addLongDefinition(OWLClass cls) {
		addStringAnnotation(cls, icdapiModel.getLongDefProp(), cm.getLongDefinitionProperty());
	}
	
	
	private void addIsObosolte(OWLClass cls) {
		RDFProperty isObsoleteProp = cm.getIsObsoleteProperty();
		Object isObsoleteObj = sourceCls.getPropertyValue(isObsoleteProp);
		if (isObsoleteObj != null && isObsoleteObj instanceof Boolean) {
			boolean isObsolete = (Boolean) isObsoleteObj;
			addBooleanAnnotation(cls, icdapiModel.getIsObsoleteProp(), isObsolete);
			if (isObsolete == true) {
				deprecateCls(cls);
			}
		}
	}
	

	private void addLogicalDefinition(OWLClass cls) {
		logDefCreator.createLogicalAxioms(sourceCls, cls);
	}

	
	/******************* Generic methods ********************/
	
	private OWLClass createCls(String iri) {
		OWLClass cls = df.getOWLClass(iri);
		manager.addAxiom(targetOnt, df.getOWLDeclarationAxiom(cls));
		return cls;
	}
 
	private void addStringAnnotations(OWLClass cls, OWLAnnotationProperty targetProp, RDFProperty sourceProp) {
		Collection<RDFResource> terms = cm.getTerms(sourceCls, sourceProp);
		for (RDFResource term : terms) {
			addStringAnnotationFromTerm(cls, targetProp, sourceProp, term);
		}
	}
	
	private void addStringAnnotation(OWLClass cls, OWLAnnotationProperty targetProp, RDFProperty sourceProp) {
		RDFResource termInst = cm.getTerm(sourceCls, sourceProp);
		if (termInst == null) {
			return;
		}
		
		if (isAppropriateTerm (termInst) == true) {
			addStringAnnotationFromTerm(cls, targetProp, sourceProp, termInst);
		}
	}
	

	private void addStringAnnotationFromTerm(OWLClass cls, OWLAnnotationProperty targetProp, RDFProperty sourceProp, RDFResource termInst) {
		String label = (String) termInst.getPropertyValue(cm.getLabelProperty());
		if (label == null) {
			return;
		}
		String lang = (String) termInst.getPropertyValue(cm.getLangProperty());
		
		if (isAppropriateTerm (termInst) == true) {
			addStringAnnotation(cls, targetProp, label, lang);
		}
	}
		
	//default language is en
	private OWLAnnotationAssertionAxiom addStringAnnotation(OWLClass cls, OWLAnnotationProperty targetProp, String value, String lang) {
		if (value == null) {
			return null;
		}
		lang = lang == null ? "en" : lang;
		OWLAnnotation ann = df.getOWLAnnotation(targetProp, df.getOWLLiteral(value,lang));
		OWLAnnotationAssertionAxiom annotationAssertionAxiom = df.getOWLAnnotationAssertionAxiom(cls.getIRI(), ann);
		manager.addAxiom(targetOnt, annotationAssertionAxiom);
		
		return annotationAssertionAxiom;
	}
	
	private void addReferenceAnnotations(OWLClass cls, OWLAnnotationProperty targetProp, RDFProperty sourceProp) {
		Collection<RDFResource> terms = cm.getTerms(sourceCls, sourceProp);
		for (RDFResource term : terms) {
			addReferenceAnnotationFromTerm(cls, targetProp, sourceProp, term);
		}
	}

	private void addReferenceAnnotationFromTerm(OWLClass cls, OWLAnnotationProperty targetProp, RDFProperty sourceProp,
			RDFResource term) {
		RDFProperty refCatProp = sourceOnt.getRDFProperty(WHOFICContentModelConstants.REFERENCED_CATEGORY_PROP);
		if (refCatProp == null) { //happens in ICTM
			return;
		}
		RDFSNamedClass refCls = (RDFSNamedClass) term.getPropertyValue(sourceOnt.getRDFProperty(WHOFICContentModelConstants.REFERENCED_CATEGORY_PROP));
		if (refCls == null) {
			return;
		}
		RDFResource refTitleTerm = cm.getTerm(refCls, cm.getIcdTitleProperty());
		String label = (String) refTitleTerm.getPropertyValue(cm.getLabelProperty());
		if (label == null) {
			return;
		}
		String lang = (String) refTitleTerm.getPropertyValue(cm.getLangProperty());
		OWLAnnotation mainAnn = df.getOWLAnnotation(targetProp, df.getOWLLiteral(label,lang));
		
		IRI refIri = IRI.create(PublicIdCache.getPublicId(cm, refCls));
		OWLAnnotation refAnn = df.getOWLAnnotation(icdapiModel.getFoundRefProp(), refIri);
		Collection<OWLAnnotation> refAnnCol = new ArrayList<OWLAnnotation>();
		refAnnCol.add(refAnn);
		
		OWLAnnotationAssertionAxiom annotationAssertionAxiom = df.getOWLAnnotationAssertionAxiom(cls.getIRI(), mainAnn, refAnnCol);
		manager.addAxiom(targetOnt, annotationAssertionAxiom);
	}
	
	private OWLAnnotationAssertionAxiom addBooleanAnnotation(OWLClass cls, OWLAnnotationProperty targetProp, boolean value) {
		OWLAnnotation ann = df.getOWLAnnotation(targetProp, df.getOWLLiteral(value));
		OWLAnnotationAssertionAxiom annotationAssertionAxiom = df.getOWLAnnotationAssertionAxiom(cls.getIRI(), ann);
		manager.addAxiom(targetOnt, annotationAssertionAxiom);
		
		return annotationAssertionAxiom;
	}
	
	private void deprecateCls(OWLClass cls) {
		addBooleanAnnotation(cls, df.getOWLAnnotationProperty(OWLRDFVocabulary.OWL_DEPRECATED.getIRI()), true);
	}
	
	/**
	 * Only terms with language "en" should be exported.
	 * The rest of the translations will come from the translation tool.
	 * 
	 * @param termInst
	 * @return
	 */
	private boolean isAppropriateTerm(RDFResource termInst) {
		RDFProperty langProp = cm.getLangProperty();
		
		String lang = (String) termInst.getPropertyValue(langProp);
		
		return lang == null || "en".equals(lang);
	}
	
}
