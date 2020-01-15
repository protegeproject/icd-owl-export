package org.who.owl.export;

import java.util.Optional;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.search.Searcher;

public class ExportPostProcessor {
	
	private final static Logger log = Logger.getLogger(ExportPostProcessor.class);
	
	public final static String ICD_CATEGORIES_CLS = "http://who.int/icd#ICDCategory";
	public final static String ERROR_CLS = "http://who.int/icd#Z_ErrorClasses";
	
	private OWLOntology targetOnt;
	private OWLOntologyManager manager;
	private OWLDataFactory df;
	private ICDAPIModel icdapiModel;
	
	
	public ExportPostProcessor(OWLOntology targetOnt, OWLOntologyManager manager, ICDAPIModel icdapiModel) {
		this.targetOnt = targetOnt;
		this.manager = manager;
		this.icdapiModel = icdapiModel;
		this.df = manager.getOWLDataFactory();
	}

	public void postprocess() {
		log.info("Post-process: Adding missing title for logical definition fillers");
		addMissingTitlesForLogDefFillers();
		
		log.info("Post-process: Move erroneous classes under Error class");
		moveErrorClasses();
		
		log.info("Post-process: Removing ICD Categories class");
		removeICDCatClass();
	}


	private void addMissingTitlesForLogDefFillers() {
		targetOnt.individualsInSignature().filter(i -> hasNoTitle(i)).forEach(i -> addTitle(i));
	}

	private void addTitle(OWLNamedIndividual ind) {
		try {
			Optional<OWLClassAssertionAxiom> clsOpt = getType(ind);

			if (clsOpt.isPresent() == false) {
				log.warn("Did not add title for " + ind + " because did not find individual type.");
				return;
			}
			OWLClass cls = (OWLClass) clsOpt.get().getClassExpression();

			OWLLiteral titleLit = getAnnotationValue(targetOnt, cls, icdapiModel.getTitleProp());
			if (titleLit == null) {
				log.warn("Did not add title for " + ind + " because did cls has no title. Cls = " + cls);
				return;
			}

			OWLAnnotation ann = manager.getOWLDataFactory().getOWLAnnotation(icdapiModel.getTitleProp(), titleLit);
			manager.addAxiom(targetOnt, manager.getOWLDataFactory().getOWLAnnotationAssertionAxiom(ind.getIRI(), ann));
		} catch (Exception e) {
			log.warn("Error at adding title for " + ind, e);
		}
	}
	
	
	private void removeICDCatClass() {
		OWLClass icdCatCls = df.getOWLClass(ICD_CATEGORIES_CLS);
		if (targetOnt.containsClassInSignature(icdCatCls.getIRI()) == false) {
			log.warn(targetOnt + " does not contain cls " + ICD_CATEGORIES_CLS + ". Cannot remove." );
			return;
		}
		
		targetOnt.subClassAxiomsForSuperClass(icdCatCls).forEach(s -> moveToTop(icdCatCls, s.getSubClass()));
	}
	
	
	private void moveToTop(OWLClass icdCatCls, OWLClassExpression subclsEx) {
		if (subclsEx.isOWLClass() == false) {
			return;
		}
		
		OWLClass subcls = subclsEx.asOWLClass();
		
		try {
			targetOnt.addAxiom(df.getOWLSubClassOfAxiom(subcls, df.getOWLThing()));
			targetOnt.removeAxiom(df.getOWLSubClassOfAxiom(subcls, icdCatCls));
		}
		catch (Exception e) {
			log.warn("Error at moving class " + subcls + " to top.", e);
		}
	}

	
	private void moveErrorClasses() {
		targetOnt.subClassAxiomsForSuperClass(df.getOWLThing()).
			filter(s -> s.getSubClass().isOWLClass()).
			map(s -> s.getSubClass().asOWLClass()).
			filter(c -> hasNoTitle(c)).
			forEach(e -> moveErrorCls(e));
	}
	
	
	private void moveErrorCls(OWLClass errorCls) {
		OWLClass topErrorCls = df.getOWLClass(ERROR_CLS);
		try {
			targetOnt.addAxiom(df.getOWLSubClassOfAxiom(errorCls, topErrorCls));
			targetOnt.remove(df.getOWLSubClassOfAxiom(errorCls, df.getOWLThing()));
		} catch (Exception e) {
			log.warn("Error at moving error class " + errorCls + " under " + ERROR_CLS, e);
		}
	}

	// ***************** Generic methods *********************** //



	private Optional<OWLClassAssertionAxiom> getType(OWLNamedIndividual ind) {
		return targetOnt.classAssertionAxioms(ind).findFirst();
	}

	private boolean hasNoTitle(OWLEntity ind) {
		return getAnnotationValue(targetOnt, ind, icdapiModel.getTitleProp()) == null;
	}

	private OWLLiteral getAnnotationValue(OWLOntology ont, OWLEntity cls, OWLAnnotationProperty prop) {
		Optional<OWLAnnotationValue> ann = Searcher
				.values(Searcher.annotationObjects(ont.annotationAssertionAxioms(cls.getIRI()), prop)).findFirst();
		return ann.isPresent() ? ann.get().asLiteral().get() : null;
	}
}
