package org.who.owl.export;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.exception.OntologyLoadException;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.util.SystemUtilities;
import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICD2OWLExporter {

	private final static Logger log = Logger.getLogger(ICD2OWLExporter.class);
	
	private static String TEMPLATE_FILE_PATH = "template/icd-base.owl";

	private OWLModel sourceOnt;

	private OWLOntologyManager manager;
	private OWLOntology targetOnt;

	private RDFSNamedClass sourceTopClass;

	private ICDContentModel cm;
	private ExcludedClasses excludedClasses;
	private ICDAPIModel icdapiModel;

	private Set<RDFSNamedClass> traversed = new HashSet<RDFSNamedClass>();
	private Map<RDFSNamedClass, OWLClass> source2targetCls = new HashMap<RDFSNamedClass, OWLClass>();

	private int exportedClassesCount = 0;
	private boolean isICTM = false;

	public ICD2OWLExporter(OWLModel sourceOnt, OWLOntologyManager manager, ICDAPIModel icdapiModel,
			OWLOntology targetOnt, RDFSNamedClass sourceTopClass) {
		this.sourceOnt = sourceOnt;
		this.manager = manager;
		this.targetOnt = targetOnt;
		this.sourceTopClass = sourceTopClass;
		this.cm = new ICDContentModel(sourceOnt);
		this.excludedClasses = new ExcludedClasses(sourceOnt, cm);
		this.icdapiModel = icdapiModel;
		this.isICTM = ICTMUtil.isICTMOntology(sourceOnt);
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			log.error("Requires 3 parameters: (1) ICD pprj or OWL file, " + "(2) top ICD class to export,  "
					+ "(3) output OWL file ");
			return;
		}
		
		PropertyConfigurator.configure("log4j.properties");

		String sourceICDPrjFile = args[0];

		OWLModel sourceICDOnt = openOWLFile(sourceICDPrjFile);
		if (sourceICDOnt == null) {
			log.error("Could not open ICD project " + sourceICDPrjFile);
			System.exit(1);
		}

		RDFSNamedClass sourceICDTopClass = sourceICDOnt.getRDFSNamedClass(args[1]);
		if (sourceICDTopClass == null) {
			log.error("Could not find ICD top class " + args[1]);
			System.exit(1);
		}

		String outputOWLFile = args[2];

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology targetOnt = initTargetOnt(manager, outputOWLFile);
		
		ICDAPIModel icdapiModel = new ICDAPIModel(manager, targetOnt);
		
		initTargetOntMetadata(manager,targetOnt, icdapiModel);


		SystemUtilities.logSystemInfo();

		exportOntology("ICD", sourceICDOnt, manager, icdapiModel, targetOnt, sourceICDTopClass, outputOWLFile);
		
		log.info("Starting post-processing ..");
		try {
			ExportPostProcessor postProcessor = new ExportPostProcessor(targetOnt, manager, icdapiModel);
			postProcessor.postprocess();
		} catch (Throwable e) {
			log.warn("Error at postprocessing", e);
		}
		log.info("Ended post-processing");

		
		log.info("Saving ontology..");
		try {
			manager.saveOntology(targetOnt, IRI.create(new File(outputOWLFile)));
		} catch (OWLOntologyStorageException e) {
			log.error("Error at saving ontology", e);
		}

		log.info("\n===== End export at " + new Date());
	}


	private static void exportOntology(String ontShortName, OWLModel sourceOnt, OWLOntologyManager manager,
			ICDAPIModel icdapiModel, OWLOntology targetOnt, RDFSNamedClass sourceTopClass, String outputOWLFile) {

		log.info("Started the " + ontShortName + " export");
		log.info("Top class: " + sourceTopClass.getBrowserText());
		log.info("Output file: " + outputOWLFile);

		ICD2OWLExporter icdConv = new ICD2OWLExporter(sourceOnt, manager, icdapiModel, targetOnt, sourceTopClass);

		try {
			icdConv.export();
			icdConv.saveTargetOntology(outputOWLFile);
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}

		log.info("Ended " + ontShortName + " export");
	}

	public void export() {
		export(sourceTopClass, sourceOnt.getOWLThingClass());
	}

	private void export(RDFSNamedClass sourceCls, RDFSNamedClass sourceParent) {
		if (excludedClasses.isExcludedTopClass(sourceCls) == true) {
			return;
		}

		if (traversed.contains(sourceCls) == true) {
			addSuperCls(sourceCls, sourceParent);
			return;
		}

		traversed.add(sourceCls);

		try {
			ClassExporter clsExporter = new ClassExporter(sourceCls, sourceOnt, manager, targetOnt, cm, icdapiModel,
					isICTM);
			OWLClass targetCls = clsExporter.export();

			source2targetCls.put(sourceCls, targetCls);
			PublicIdCache.getPublicId(cm, sourceCls); // just fill the cache

			addChildren(sourceCls);
		} catch (Throwable t) {
			log.error("Error at adding class: " + sourceCls, t);
		}

		exportedClassesCount++;

		if (exportedClassesCount % 1000 == 0) {
			log.info("Exported " + exportedClassesCount + " classes.");
		}
	}

	private void addSuperCls(RDFSNamedClass sourceCls, RDFSNamedClass sourceParent) {
		OWLClass targetCls = getTargetCls(sourceCls);
		OWLClass targetParent = getTargetCls(sourceParent);
		OWLSubClassOfAxiom subclsAxiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom(targetCls, targetParent);
		manager.addAxiom(targetOnt, subclsAxiom);

		// may add later
		// addSkosChildParentRels(targetCls, targetParent); //redundant with is-a rel
	}

	private void addSkosChildParentRels(OWLClass targetCls, OWLClass targetParent) {
		OWLDataFactory df = manager.getOWLDataFactory();

		OWLAnnotation annChild = df.getOWLAnnotation(icdapiModel.getParentProp(), targetParent.getIRI());
		OWLAnnotationAssertionAxiom annChildAx = df.getOWLAnnotationAssertionAxiom(targetCls.getIRI(), annChild);
		manager.addAxiom(targetOnt, annChildAx);

		OWLAnnotation annParent = df.getOWLAnnotation(icdapiModel.getChildProp(), targetCls.getIRI());
		OWLAnnotationAssertionAxiom annParentAx = df.getOWLAnnotationAssertionAxiom(targetParent.getIRI(), annParent);
		manager.addAxiom(targetOnt, annParentAx);
	}

	private void addChildren(RDFSNamedClass sourceCls) {
		Set<RDFSNamedClass> subclses = getNamedSubclasses(sourceCls);
		for (RDFSNamedClass subcls : subclses) {
			if (excludedClasses.isExcludedTopClass(subcls) == false) {
				addSuperCls(subcls, sourceCls);
				export(subcls, sourceCls);
			}
		}
	}

	// *************** Generic methods *************/

	private Set<RDFSNamedClass> getNamedSubclasses(RDFSNamedClass sourceCls) {
		Set<RDFSNamedClass> namedSubclses = new HashSet<RDFSNamedClass>();
		for (Object subcls : sourceCls.getSubclasses(false)) {
			if (subcls instanceof RDFSNamedClass) {
				namedSubclses.add((RDFSNamedClass) subcls);
			}
		}
		return namedSubclses;
	}

	private void saveTargetOntology(String targetPath) throws OWLOntologyStorageException {
		manager.saveOntology(targetOnt, IRI.create(new File(targetPath)));
	}

	private OWLClass getTargetCls(RDFSNamedClass cls) {
		OWLClass targetCls = source2targetCls.get(cls);

		if (targetCls == null) {
			targetCls = manager.getOWLDataFactory().getOWLClass(IRI.create(PublicIdCache.getPublicId(cm, cls)));
			source2targetCls.put(cls, targetCls);
		}

		return targetCls;
	}

	private static OWLModel openOWLFile(String fileName) {
		OWLModel owlModel = null;

		if (fileName.endsWith(".pprj")) { // pprj file
			@SuppressWarnings("rawtypes")
			List errors = new ArrayList();
			Project prj = Project.loadProjectFromFile(fileName, errors);
			if (errors.size() > 0) {
				log.error("There were errors at loading project: " + fileName);
				return null;
			}
			owlModel = (OWLModel) prj.getKnowledgeBase();
		} else { // Assume OWL file
			try {
				owlModel = ProtegeOWL.createJenaOWLModelFromURI(fileName);
			} catch (OntologyLoadException e) {
				log.error(e.getMessage(), e);
			}
		}
		return owlModel;
	}

	private static OWLOntology initTargetOnt(OWLOntologyManager manager, String outputOWLFile) {
		File outputOntFile = new File(outputOWLFile);
		OWLOntology targetOnt = null;

		if (outputOntFile.exists() == true) { // target ont file already exists
			log.info("Loading existing target ontology from " + outputOntFile.getAbsolutePath());
			targetOnt = loadFromExistingFile(manager, outputOntFile);
		} else { // target ont file does not exist, try to use template file
			File templateOntFile = new File(TEMPLATE_FILE_PATH);
			
			if (templateOntFile.exists() == true) { // start with template ont file
				log.info("Initializing target ontology with template from: " + templateOntFile.getAbsolutePath());
				targetOnt = loadFromExistingFile(manager, templateOntFile);
			} else { // template file does not exist, initialize empty target ont
				try {
					log.info("Creating new target ontology");
					targetOnt = manager.createOntology(IRI.create(ICDAPIConstants.TARGET_ONT_NAME));
				} catch (OWLOntologyCreationException e) {
					log.error("Could not create target OWL ontology", e);
					System.exit(1);
				}
			}
		}

		return targetOnt;
	}
	
	private static OWLOntology loadFromExistingFile(OWLOntologyManager manager, File outputOntFile) {
		OWLOntology targetOnt = null;
		
		try {
			targetOnt = manager.loadOntologyFromOntologyDocument(outputOntFile);
		} catch (OWLOntologyCreationException e) {
			log.error("Could not load target OWL ontology from " + outputOntFile.getAbsolutePath(), e);
		}
		
		return targetOnt;
	}

	private static void initTargetOntMetadata(OWLOntologyManager manager, OWLOntology targetOnt, ICDAPIModel icdapiModel) {
		OWLDataFactory df = manager.getOWLDataFactory();
		
		//adding the release date
		String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		
		OWLAnnotation releaseDateAnn = df.getOWLAnnotation(icdapiModel.getReleaseDate(), df.getOWLLiteral(dateStr));
		manager.applyChange(new AddOntologyAnnotation(targetOnt, releaseDateAnn));
		
		OWLAnnotation owlVersionAnn = df.getOWLAnnotation(df.getOWLVersionInfo(), df.getOWLLiteral(dateStr));
		manager.applyChange(new AddOntologyAnnotation(targetOnt, owlVersionAnn));
	}
	
}
