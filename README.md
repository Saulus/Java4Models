# Java4Models

Extract profiles and apply model parameters to local data.

Input: csv or Satzart-Files
Output: patient profiles (if needed), Scores

Base Configuration: see konfiguration.xml
Model configuration: see Models/occ.config (-> configures patient profiles)
Parameter configuration: see Models/occ.coeffs (-> model parameters to be applied to patient profiles)

Input files should all be sorted by patient in similar (ascending) order.
If not, files can be flagged in config, and are sorted before processing.



Load data in python, and to H2O using svmlight:
https://groups.google.com/forum/#!topic/h2ostream/VvWWA7ZjVfc


Open issues:
- empty ;; row in matlab matrix 