package org.who.owl.export;


public class ICDAPIConstants {

	public final static String TARGET_ONT_NAME = "http://who.int/icd";
	public static final String TARGET_ONT_NS = "http://id.who.int/icd/schema/";
	public static final String SOURCE_ONT_NS = "http://who.int/icd#";
	
	public final static String ERROR_CLS = "http://who.int/icd#Z_RetiredClasses";
	
	public final static String RELEASE_DATE = TARGET_ONT_NS + "releaseDate";
	public final static String RELEASE_ID = TARGET_ONT_NS + "releaseId";
	public final static String RELEASE_ID_BETA = "beta";
	
	public final static String TITLE = "http://www.w3.org/2004/02/skos/core#prefLabel";
	public final static String DEFINITION = "http://www.w3.org/2004/02/skos/core#definition";
	public final static String LONG_DEFINITION = "http://id.who.int/icd/schema/longDefinition";
	public final static String PARENT = "http://www.w3.org/2004/02/skos/core#broaderTransitive";
	public final static String CHILD = "http://www.w3.org/2004/02/skos/core#narrowerTransitive";
	public final static String SYNONYM = "http://www.w3.org/2004/02/skos/core#altLabel";
	
	public final static String FULLY_SPECIFIED_NAME = TARGET_ONT_NS + "fullySpecifiedName";
	public final static String NARROWER_TERM = TARGET_ONT_NS + "narrowerTerm";
	public final static String EXCLUSION = TARGET_ONT_NS + "exclusion";
	public final static String INCLUSION = TARGET_ONT_NS + "inclusion";
	public final static String BROWSER_URL = TARGET_ONT_NS + "browserUrl";
	public final static String FOUNDATION_REFERENCE = TARGET_ONT_NS + "foundationReference";
	
	public final static String NOTE = TARGET_ONT_NS + "note"; //ICD-10
	public final static String CODING_HINT = TARGET_ONT_NS + "codingHint"; //ICD-10
	public final static String CODING_NOTE = TARGET_ONT_NS + "codingNote"; //ICD-11 per linearization
	
	public final static String ICD10CODE = TARGET_ONT_NS + "icd10code";
	
	public final static String IS_OBSOLETE = TARGET_ONT_NS + "isObsolote";

}
