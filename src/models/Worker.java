package models;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import configuration.Consts;
import configuration.Datei;
import configuration.Konfiguration;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * The Class Worker.
 * This has three steps:
 * 1. init
 * 	Create & Open Inputfile-Objects, incl. sorting if necessary
 * 	Create and Import Models from Model.config and Model.coeff
 * 	create and open output Model Score file
 * 	create and open output ProfilDense file (if configured)
 * 	create and open output ProfilSparse files (if configured)
 * 
 * 2. Process
 * 	Move through all inputfiles, patient by patient
 * 	Create patient object with variable/model information
 * 	save patient score (and profile) to disk
 * 
 * 3. Finish
 *  Close files, delete temps
 */
public class Worker {
	
	/** The models. */
	private ArrayList<Model> models = new ArrayList<Model>();
	
	/** The inputfiles. */
	private ArrayList<InputFile> inputfiles = new ArrayList<InputFile>();
	
	/** The config. */
	private Konfiguration config;
	
	/** The outputfile. */
	private CSVWriter outputfile; 
	
	/** The profildensefile. */
	private HashMap<Model,CSVWriter> profildensefile = new HashMap<Model,CSVWriter>();
	
	/** The profilsparsefile. */
	private HashMap<Model,CSVWriter> profilsparsefile = new HashMap<Model,CSVWriter>();
	
	/** The profilsparsefile cols. */
	private HashMap<Model,CSVWriter> profilsparsefileCols = new HashMap<Model,CSVWriter>();
	
	/** The profilsparsefile rows. */
	private HashMap<Model,CSVWriter> profilsparsefileRows = new HashMap<Model,CSVWriter>();
	
	/** The known variables. */
	private HashMap<Model,ArrayList<String>> knownVariables = new HashMap<Model,ArrayList<String>>();//holds known variables per Model    
	
	/**
	 * Find next patient.
	 *
	 * @return the patient
	 */
	private Patient findNextPatient() {
		Patient newpatient= null;
		List<String> pidliste = new ArrayList<String>();
		for (InputFile infile : inputfiles) {
			if (infile.hasRow()) { pidliste.add(infile.getValue(config.getIdField())); }
		}
		if (!pidliste.isEmpty()) {
			Collections.sort(pidliste);
			newpatient= new Patient(pidliste.get(0));
		}
		return newpatient;
	}
	
	/**
	 * Next row all files.
	 */
	private void nextRowAllFiles () {
		for (InputFile infile : inputfiles) {
			infile.nextRow();
		}
	}
	
	/**
	 * Instantiates a new worker.
	 *
	 * @param config the config
	 */
	public Worker (Konfiguration config) {
		this.config=config;
	}
	
	/**
	 * Inits the.
	 *
	 * @return true, if successful
	 */
	public boolean init () {
		String timeStamp;
		//1. Create & Open Inputfiles
		boolean worked = false; 
		for (Datei nextfile : config.getInputfiles()) {
			//Sort if isSorted=false
			if (!nextfile.isSorted()) {
				try {
					Sorter sortfile = new Sorter(nextfile.getDatentyp(),nextfile.getPath(),nextfile.getFiletype(),config.getTmpPath());
					timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
					System.out.println(timeStamp + " Sortieren gestartet von " + nextfile.getPath());
					String newpath = sortfile.sortFile(config.getIdField());
					timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
					System.out.println(timeStamp + " Sortieren beendet von " + nextfile.getPath());
					nextfile.setPath(newpath);
					nextfile.setFiletype(Consts.csvFlag);
					nextfile.setIsSorted(true);
					worked = true; //if one files is ok, then start
				} catch (Exception e) {
					System.out.println("Fehler beim Sortieren von " + nextfile.getPath());
					e.printStackTrace();
					System.exit(1);
				}
			}
			try {
				timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
				System.out.println(timeStamp + " Einlesen der Inputdatei "+ nextfile.getPath() + " gestartet.");
				InputFile newfile = new InputFile(nextfile.getDatentyp(),nextfile.getPath(),nextfile.getFiletype());
				inputfiles.add(newfile);
				worked = true; //if one files is ok, then start
			} catch (Exception e) {
				System.out.println("Fehler gefunden beim Einlesen von " + nextfile.getPath());
				e.printStackTrace();
			}
		}
		//2. Create and Import Models
		List<String> processedModels = new ArrayList<String>();  
		File folder = new File(config.getModelpath());
		File[] listOfFiles = folder.listFiles();
		if (listOfFiles == null) {
			System.out.println("Keine Modellkonfiguration in " + config.getModelpath() + " gefunden");
			worked = false;
		} else {
			for (File file : listOfFiles) {
			    if (file.isFile()) {
			    	String modelname = file.getName().replaceFirst("[.][^.]+$", "");
			    	if (!processedModels.contains(modelname)) {
			    		try {
			    			Model newmodel = new Model(modelname, inputfiles, config.getModelpath() + "\\" + modelname+config.getModelConfigExt(), config.getModelpath() + "\\" + modelname+config.getModelCoeffExt());
			    			models.add(newmodel);
			    			ArrayList<String> knVars = new ArrayList<String>();
			    			knownVariables.put(newmodel, knVars);
			    			processedModels.add(modelname);
			    			timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
			    			System.out.println(timeStamp + " Modell "+ modelname + " konfiguriert.");
			    			worked = true; //if one files is ok, then start
			    		} catch (Exception e) {
							System.err.println("Fehler gefunden bei Konfiguration des Modells " + modelname + ". Das Modell wird nicht verwendet.");
							e.printStackTrace();
						}
			    	}
			    }
			}
			//create and open output Score file
			if (worked && config.createScores())
			try {
				outputfile = new CSVWriter(new FileWriter(config.getOutputfile()), ';', CSVWriter.NO_QUOTE_CHARACTER);
				List<String> newline = new ArrayList<String>();
				//write header
				newline.add(config.getIdField());
				for (Model model : models) {
					newline.add(model.getName());
				}
				outputfile.writeNext(newline.toArray(new String[newline.size()]));
				worked = true;
			} catch (Exception e) {
				System.err.println("Die Outputdatei " + config.getOutputfile() + " konnte nicht erstellt werden.");
				e.printStackTrace();
				worked = false;
			}
			//create and open output ProfilDense file (if needed)
			if (worked && config.createProfilDense()) {
				for (Model model : models) {
					try {
						profildensefile.put(model, new CSVWriter(new FileWriter(config.getProfilfileDenseTmp(model.getName())), ';', CSVWriter.NO_QUOTE_CHARACTER));
						//no header here
					} catch (Exception e) {
						System.err.println("Die Outputdatei " + config.getProfilfileDense(model.getName()) + " konnte nicht erstellt werden.");
						e.printStackTrace();
						worked = false;
					}
				}
			}
			//create and open output ProfilSparse files (if needed)
			if (worked && config.createProfilSparse()) {
				for (Model model : models) {
					try {
						profilsparsefile.put(model, new CSVWriter(new FileWriter(config.getProfilfileSparse(model.getName())), ';', CSVWriter.NO_QUOTE_CHARACTER));
						String[] newline = {"ROW","COL","VAL"};
						//write header
						profilsparsefile.get(model).writeNext(newline);
						//Col-&Row-Translation File
						profilsparsefileCols.put(model, new CSVWriter(new FileWriter(config.getProfilfileSparseCOLs(model.getName())), ';', CSVWriter.NO_QUOTE_CHARACTER));
						//write header
						String[] colline = {"COL_NO","VARIABLE"};
						profilsparsefileCols.get(model).writeNext(colline);
						//write header
						profilsparsefileRows.put(model, new CSVWriter(new FileWriter(config.getProfilfileSparseROWs(model.getName())), ';', CSVWriter.NO_QUOTE_CHARACTER));
						String[] rowline = {"ROW_NO","PID"};
						profilsparsefileRows.get(model).writeNext(rowline);
					} catch (Exception e) {
						System.err.println("Die Outputdatei " + config.getProfilfileSparse(model.getName()) + " konnte nicht erstellt werden.");
						e.printStackTrace();
						worked = false;
					}
				}
			}
		} //Ende test listOfFiles==null
		return  worked;
	}
	
	/**
	 * Process.
	 *
	 * @return true, if successful
	 */
	public boolean process() { 
		String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp + " Starte Profilbildung und Modellanwendung...");
		boolean worked = true;
		//add No. for rows and pids
		long pNo =0;
		long rowNo =0;
		//move to first real row for all inputfiles
		nextRowAllFiles();
		//loop through possible patients until no file has still rows
		for (Patient patient = findNextPatient(); patient != null; patient = findNextPatient()) {
			pNo++; //new patient, matrix starts from 1
			rowNo++;
		    //for each input file... 
			for (InputFile infile : inputfiles) {
				//..that still has a valid Row and the correct Patient  
				while ((infile.hasRow()) && patient.isPatient(infile.getValue(config.getIdField()))) {
					//loop though models
					for (Model model : models) {
						try {
							patient.processRow(model, infile);
						} catch (Exception e) {
							System.err.println(e.getMessage());
							worked=false;
							break;
						}
					}
					infile.nextRow();
				}
			}
			if (worked) {
				//save patient to file, if  includes and not excludes
				List<String> newline = new ArrayList<String>();
				// 1. Scores
				if (config.createScores()) {
					newline.add(patient.getPid());
					for (Model model : models) {
						if (model.hasCoeffs() && patient.areYouIncluded(model)) 
								newline.add(String.valueOf(patient.getCoeffSum(model)));
						else newline.add(Consts.navalue); //write navalue if model has no coefficients or patient is not included
					}
					try {
						outputfile.writeNext(newline.toArray(new String[newline.size()]));
					} catch (Exception e) {
						System.err.println("In die Outputdatei " + config.getOutputfile() + " konnte nicht geschrieben werden.");
						e.printStackTrace();
						worked = false;
					}
				}
				//2. Dense Profile
				if (config.createProfilDense()) {
					for (Model model : models) {
						if (patient.areYouIncluded(model)) {
							//update knownVariables
							ArrayList<String> knownVars = patient.addToKnownVariables(model,knownVariables.get(model));
							knownVariables.put(model,knownVars);
							//get ProfileValues
							ArrayList<String> profValues = patient.getProfvalues(model,knownVars);
							//output PID+Profilvalues only
							newline = new ArrayList<String>();
							newline.add(patient.getPid());
							newline.addAll(profValues);
							try {
								profildensefile.get(model).writeNext(newline.toArray(new String[newline.size()]));
							} catch (Exception e) {
								System.err.println("In die Outputdatei " + config.getProfilfileDense(model.getName()) + " konnte nicht geschrieben werden.");
								e.printStackTrace();
								worked = false;
							}
						}
					}
				}
				//3. Sparse Profile
				if (config.createProfilSparse()) {
					boolean isincluded = false;
					for (Model model : models) {
						if (patient.areYouIncluded(model)) {
							//update knownVariables
							ArrayList<String> knownVars = patient.addToKnownVariables(model,knownVariables.get(model));
							knownVariables.put(model,knownVars);
							//get ProfileValues
							ArrayList<String> profValues = patient.getProfvalues(model,knownVars);
							//output: row PID No, column (=Variable no.), value
							String[] sparseline = new String[3];
							for (int i =0; i<profValues.size(); i++) {
								if (!profValues.get(i).equals(Consts.navalue)) {
									sparseline[0]=Long.toString(rowNo); //Row
									sparseline[1]=Integer.toString(i+1); //Col; matrix starts from 1
									sparseline[2]=profValues.get(i); //Val
									if (!sparseline.equals(new String[3])) {
										try {
											profilsparsefile.get(model).writeNext(sparseline);
										} catch (Exception e) {
											System.out.println("In die Outputdatei " + config.getProfilfileSparse(model.getName()) + " konnte nicht geschrieben werden.");
											e.printStackTrace();
											worked = false;
										}	
										sparseline = new String[3];
									}
								}
							}
							isincluded = true;
						}
					}
					//write PID in row-translation.file
					if (isincluded) {
						String[] pidrow = {Long.toString(rowNo), patient.getPid()};
						for (Model model : models) {
							try {
								profilsparsefileRows.get(model).writeNext(pidrow);
							} catch (Exception e) {
								System.out.println("In die Outputdatei " + config.getProfilfileSparseROWs(model.getName()) + " konnte nicht geschrieben werden.");
								e.printStackTrace();
								worked = false;
							}
						}
					} else rowNo = rowNo-1; //if patient not included: reset no;
				} //end sparse matrix
				if (pNo % 50000 == 0) {
					timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
					System.out.println(timeStamp +" - "+ Long.toString(pNo) + " Patienten verarbeitet.");
				}
			} //end worked test
			else break;
		} // end for loop per patient
		if (worked) {
			timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
			System.out.println(timeStamp +" - "+ Long.toString(pNo) + " Patienten verarbeitet.");
		}
		return worked;
	}
	
	/**
	 * Finish.
	 *
	 * @return true, if successful
	 */
	public boolean finish() { 
		boolean worked = true;
		//close all files
		try {
			if (config.createScores()) {
				outputfile.close();
				System.out.println("Outputdatei " + config.getOutputfile() + " wurde erfolgreich geschrieben.");
			}
			for (InputFile infile : inputfiles) {
				infile.close();
			}
			for (Model model : models) {
				if (config.createProfilDense()) {
					//close file
					profildensefile.get(model).close();
					//add header by creating new file, and write all rows from tmpfile to new file
					CSVWriter newfile = new CSVWriter(new FileWriter(config.getProfilfileDense(model.getName())), ';', CSVWriter.NO_QUOTE_CHARACTER);
					ArrayList<String> header = new ArrayList<String>();
					header.add(config.getIdField());
					header.addAll(knownVariables.get(model));
					newfile.writeNext(header.toArray(new String[header.size()]));
					CSVReader tmpfile = new CSVReader(new FileReader(config.getProfilfileDenseTmp(model.getName())), ';');
					//now write line for line
					String[] nextLine;
					String[] tmpline;
				    while ((nextLine = tmpfile.readNext()) != null) {
				    	//add missing delimiters (i.e. empty columns) -> required for some csv parsers
				    	if (nextLine.length < header.size()) {
				    		tmpline = new String[header.size()];
				    		System.arraycopy(nextLine, 0, tmpline, 0, nextLine.length);
				    		for (int i=nextLine.length;i<header.size();i++) tmpline[i]=Consts.navalue; //set navalue
				    		newfile.writeNext(tmpline);
				    	} else newfile.writeNext(nextLine);
				    }
				    //close and delete tmpfile
				    newfile.close();
				    tmpfile.close();
				    File file = new File(config.getProfilfileDenseTmp(model.getName()));
				    file.delete();
				    System.out.println("Outputdatei " + config.getProfilfileDense(model.getName()) + " wurde erfolgreich geschrieben.");
				}
				if (config.createProfilSparse()) { 
					//write COL-translation file for sparse profile (vars)
					for (int i =0; i<knownVariables.get(model).size(); i++) {
						String[] varcol = {Integer.toString(i+1), knownVariables.get(model).get(i)};
						try {
							profilsparsefileCols.get(model).writeNext(varcol);
						} catch (Exception e) {
							System.out.println("In die Outputdatei " + config.getProfilfileSparseCOLs(model.getName()) + " konnte nicht geschrieben werden.");
							e.printStackTrace();
							worked = false;
						}	
					}
					//close files
					profilsparsefile.get(model).close();
					profilsparsefileRows.get(model).close();
					profilsparsefileCols.get(model).close();
					System.out.println("Outputdatei " + config.getProfilfileSparse(model.getName()) + " wurde erfolgreich geschrieben.");
				}
			}
		} catch (Exception e) {
			System.out.println("Fehler beim Schließen der Dateien.");
			e.printStackTrace();
			worked = false;
		}
		return worked;
	}
	

}
