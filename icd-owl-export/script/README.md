This script exports the ICD and ICTM (optional) projects from the iCAT database dump into an OWL 2.0 file.

To run the script:

(1) Edit export.properties to match your configuration (e.g., project paths, output path, max heap size, etc.). Each property has a short description for the expected value.

(2) Copy icd-base.owl from template folder into the script folder. Rename it, if wanted. 

This OWL file contains the basic metadata about the ICD ontology. Edit the ontology metadata in this file, either in a text editor, or in an ontology editor, like Protege (http://protege.stanford.edu).

(3) Run the script via the command:

ant run



The output will be in the {output.owl.file} as configured in export.properties.


Note: If ICTM should not be included in the OWL export, please delete or comment the ictm properties in export.properties: {ictm.pprj.file} and {ictm.topclass}.

The script may take 2-3 hours to run, so it is recommended to be run in a screen session, if run on a Linux server. See more: https://linux.die.net/man/1/screen