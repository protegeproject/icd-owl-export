package org.who.owl.export;

import java.util.Optional;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.search.Searcher;

public class MissingTitleAdder {
	
	private final static Logger log = Logger.getLogger(MissingTitleAdder.class);
	
	private OWLOntology targetOnt;
	private OWLOntologyManager manager;
	private ICDAPIModel icdapiModel;
	
	
	public MissingTitleAdder(OWLOntology targetOnt, OWLOntologyManager manager, ICDAPIModel icdapiModel) {
		this.targetOnt = targetOnt;
		this.manager = manager;
		this.icdapiModel = icdapiModel;
	}

	public void addMissingTitlesForLogDefFillers() {
		targetOnt.individualsInSignature().filter(i -> hasNoTitle(i)).forEach(i -> addTitle(i));
	}

	private void addTitle(OWLNamedIndividual ind) {
		try {
			Optional<OWLClassAssertionAxiom> clsOpt = getType(ind);

			if (clsOpt.isEmpty()) {
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
