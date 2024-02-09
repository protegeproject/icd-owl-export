This script exports the WHO-FIC Foundation project from the iCAT database dump into an OWL 2.0 file.

Edit export.properties to match your configuration (e.g., project paths, output path, max heap size, etc.). Each property has a short description for the expected value.

The script will copy icd-base.owl from template folder to initialize the target ontology, which will be saved in {output.owl.file} as configured in export.properties.

The icd-base.owl file contains the basic metadata about the WHO-FIC Foundation ontology. Edit the ontology metadata in this file, either in a text editor, or in an ontology editor, like Protege (http://protege.stanford.edu).
The script will append the OWL content to this file.

Run the script via the command:

ant run

The output will be in the {output.owl.file} as configured in export.properties.


The script may take 30 mins to run, so it is recommended to be run in a screen session, if run on a Linux server. See more: https://linux.die.net/man/1/screen