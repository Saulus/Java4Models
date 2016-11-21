# Java4Models

Extract profiles and apply model parameters to local data.

Input: csv or Satzart-Files
Output: patient profiles (if needed), Scores

start with: java -jar java4models3.jar konfiguration.xml

Base Configuration: see konfiguration.xml (-> inputfiles, model path)
Model configuration: see Models/occ.config (-> configures patient profiles)
OPTIONAL: Parameter configuration: see Models/occ.coeffs (-> model parameters to be applied to patient profiles)

Input files should all be sorted by patient in similar (ascending) order.
If not, files can be flagged in config, and are sorted before processing.

