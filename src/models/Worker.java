package models;

import java.io.File;
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
import configuration.Utils;
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
	
	/** The profildensefiles */
	private HashMap<Model,CSVWriter> profildensefile = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profildensefile_targets = new HashMap<Model,CSVWriter>();
	
	/** Matlab format */
	private HashMap<Model,CSVWriter> profilsparsefile = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profilsparsefileCols = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profilsparsefileRows = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profilsparsefile_targets = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profilsparsefileCols_targets = new HashMap<Model,CSVWriter>();
	
	/** Svmlight format */
	private HashMap<Model,CSVWriter> profilsvmlightfile = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profilsvmlightfileHeader = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profilsvmlightfile_targets = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profilsvmlightfileHeader_targets = new HashMap<Model,CSVWriter>();
	
	/** The known variables. */
	private HashMap<Model,ArrayList<String>> knownVariables = new HashMap<Model,ArrayList<String>>();//holds known variables per Model    
	private HashMap<Model,ArrayList<String>> knownVariables_targets = new HashMap<Model,ArrayList<String>>();//holds known variables per Model (targets only)
	
	/**
	 * Find next patient.
	 *
	 * @return the patient
	 */
	private Patient findNextPatient() {
		Patient newpatient= null;
		List<String> pidliste = new ArrayList<String>();
		for (InputFile infile : inputfiles) {
			if (infile.hasRow()) 
				pidliste.add(infile.getID());
		}
		if (!pidliste.isEmpty()) {
			Collections.sort(pidliste);
			newpatient= new Patient(pidliste.get(0));
		}
		return newpatient;
	}
	
	/**
	 * Next row all files.
	 * @throws Exception 
	 */
	private void nextRowAllFiles () {
		for (InputFile infile : inputfiles) {
			try {
				infile.nextRow();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
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
					Sorter sortfile = new Sorter(nextfile.getDatentyp(),nextfile.getPath(),nextfile.getFiletype(),nextfile.getIdfeld(),config.getTmpPath());
					timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
					System.out.println(timeStamp + " Sortieren gestartet von " + nextfile.getPath());
					String newpath = sortfile.sortFileByID();
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
				InputFile newfile = new InputFile(nextfile.getDatentyp(),nextfile.getPath(),nextfile.getFiletype(),nextfile.getIdfeld());
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
			    			Model newmodel;
			    			if (config.createScores()) {
			    				newmodel = new Model(modelname, inputfiles, config.getModelpath() + "\\" + modelname+config.getModelConfigExt(), config.getModelpath() + "\\" + modelname+config.getModelCoeffExt());
			    			}
			    			else { 
			    				newmodel = new Model(modelname, inputfiles, config.getModelpath() + "\\" + modelname+config.getModelConfigExt());
			    			}
			    			models.add(newmodel);
			    			knownVariables.put(newmodel,  new ArrayList<String>()); //init
			    			knownVariables_targets.put(newmodel,  new ArrayList<String>()); //init
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
				newline.add(Consts.idfieldheader);
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
						profildensefile.put(model, new CSVWriter(new FileWriter(config.getProfilfileDenseTmp(model.getName(),false)), ';', CSVWriter.NO_QUOTE_CHARACTER));
						if (model.hasTargets()) profildensefile_targets.put(model, new CSVWriter(new FileWriter(config.getProfilfileDenseTmp(model.getName(),true)), ';', CSVWriter.NO_QUOTE_CHARACTER));
						//no header here
					} catch (Exception e) {
						System.err.println("Die Outputdatei " + config.getProfilfileDense(model.getName(),false) + " konnte nicht erstellt werden.");
						e.printStackTrace();
						worked = false;
					}
				}
			}
			//create and open output ProfilSparse files (if needed)
			if (worked && config.createProfilSparse()) {
				for (Model model : models) {
					try {
						profilsparsefile.put(model, new CSVWriter(new FileWriter(config.getProfilfileSparse(model.getName(),false)), ';', CSVWriter.NO_QUOTE_CHARACTER));
						if (model.hasTargets()) profilsparsefile_targets.put(model, new CSVWriter(new FileWriter(config.getProfilfileSparse(model.getName(),true)), ';', CSVWriter.NO_QUOTE_CHARACTER));
						String[] newline = {"ROW","COL","VAL"};
						//write header
						profilsparsefile.get(model).writeNext(newline);
						if (model.hasTargets()) profilsparsefile_targets.get(model).writeNext(newline);
						//Col-&Row-Translation File
						profilsparsefileCols.put(model, new CSVWriter(new FileWriter(config.getProfilfileSparseCOLs(model.getName(),false)), ';', CSVWriter.NO_QUOTE_CHARACTER));
						if (model.hasTargets()) profilsparsefileCols_targets.put(model, new CSVWriter(new FileWriter(config.getProfilfileSparseCOLs(model.getName(),true)), ';', CSVWriter.NO_QUOTE_CHARACTER));
						//write header
						String[] colline = {"COL_NO","VARIABLE"};
						profilsparsefileCols.get(model).writeNext(colline);
						if (model.hasTargets()) profilsparsefileCols_targets.get(model).writeNext(colline);
						//write header
						profilsparsefileRows.put(model, new CSVWriter(new FileWriter(config.getProfilfileSparseROWs(model.getName())), ';', CSVWriter.NO_QUOTE_CHARACTER));
						String[] rowline = {"ROW_NO",Consts.idfieldheader};
						profilsparsefileRows.get(model).writeNext(rowline);
					} catch (Exception e) {
						System.err.println("Die Outputdatei " + config.getProfilfileSparse(model.getName(),false) + " konnte nicht erstellt werden.");
						e.printStackTrace();
						worked = false;
					}
				}
			}
			//create and open output ProfilSvmlight files (if needed)
			if (worked && config.createProfilSvmlight()) {
				for (Model model : models) {
					try {
						profilsvmlightfile.put(model, new CSVWriter(new FileWriter(config.getProfilfileSvmlight(model.getName(),false)), ' ', CSVWriter.NO_QUOTE_CHARACTER));
						String[] newline1 = {"#Profile features in svmlight format, see http://svmlight.joachims.org (targets always 1, pids in #info part)"};
						profilsvmlightfile.get(model).writeNext(newline1);
						
						profilsvmlightfileHeader.put(model, new CSVWriter(new FileWriter(config.getProfilfileSvmlight(model.getName(),false)), ' ', CSVWriter.NO_QUOTE_CHARACTER));
						String[] newline2 = {"#Header to features, separated by linespace"};
						profilsvmlightfileHeader.get(model).writeNext(newline2);
						
						if (model.hasTargets()) {
							profilsvmlightfile_targets.put(model, new CSVWriter(new FileWriter(config.getProfilfileSvmlight(model.getName(),true)), ' ', CSVWriter.NO_QUOTE_CHARACTER));
							String[] newline3 = {"#Profile targets in svmlight format, see http://svmlight.joachims.org (targets always 1, pids in #info part)"};
							profilsvmlightfile_targets.get(model).writeNext(newline3);
							
							profilsvmlightfileHeader_targets.put(model, new CSVWriter(new FileWriter(config.getProfilfileSvmlight(model.getName(),true)), ' ', CSVWriter.NO_QUOTE_CHARACTER));
							String[] newline4 = {"#Header to targets, separated by linespace"};
							profilsvmlightfileHeader_targets.get(model).writeNext(newline4);
						}
					} catch (Exception e) {
						System.err.println("Die Outputdatei " + config.getProfilfileSvmlight(model.getName(),false) + " konnte nicht erstellt werden.");
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
				while ((infile.hasRow()) && patient.isPatient(infile.getID())) {
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
					try {
						infile.nextRow();
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
			}
			if (worked) {
				//save patient to file, if  includes and not excluded (per model)
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
				//2. Profiles
				if (config.createProfilDense() || config.createProfilSparse() || config.createProfilSvmlight()) {
					boolean isincluded = false;
					for (Model model : models) {
						if (patient.areYouIncluded(model)) {
							isincluded = true;
							//update knownVariables
							ArrayList<String> knownVars = patient.addToKnownVariables(model,knownVariables.get(model),false);
							knownVariables.put(model,knownVars);
							//get ProfileValues
							ArrayList<String> profValues = patient.getProfvalues(model,knownVars);
							//same for targets 
							ArrayList<String> profValues_targets = null;
							if (model.hasTargets()) {
								//update knownVariables
								ArrayList<String> knownVars_targets = patient.addToKnownVariables(model,knownVariables_targets.get(model),true);
								knownVariables_targets.put(model,knownVars_targets);
								//get ProfileValues
								profValues_targets = patient.getProfvalues(model,knownVars_targets);
							}
							if (config.createProfilDense()) {
								//output PID+Profilvalues only
								newline = new ArrayList<String>();
								newline.add(patient.getPid());
								newline.addAll(profValues);
								try {
									profildensefile.get(model).writeNext(newline.toArray(new String[newline.size()]));
								} catch (Exception e) {
									System.err.println("In die Outputdatei " + config.getProfilfileDense(model.getName(),false) + " konnte nicht geschrieben werden.");
									e.printStackTrace();
									worked = false;
								}
								//same for targets
								if (model.hasTargets()) {
									newline = new ArrayList<String>();
									newline.add(patient.getPid());
									newline.addAll(profValues_targets);
									try {
										profildensefile_targets.get(model).writeNext(newline.toArray(new String[newline.size()]));
									} catch (Exception e) {
										System.err.println("In die Outputdatei " + config.getProfilfileDense(model.getName(),true) + " konnte nicht geschrieben werden.");
										e.printStackTrace();
										worked = false;
									}
								}
							} //end dense profile
							if (config.createProfilSparse()) {
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
												System.out.println("In die Outputdatei " + config.getProfilfileSparse(model.getName(),false) + " konnte nicht geschrieben werden.");
												e.printStackTrace();
												worked = false;
											}	
											sparseline = new String[3];
										}
									}
								}	
								//same for targets
								if (model.hasTargets()) {
									for (int i =0; i<profValues_targets.size(); i++) {
										if (!profValues_targets.get(i).equals(Consts.navalue)) {
											sparseline[0]=Long.toString(rowNo); //Row
											sparseline[1]=Integer.toString(i+1); //Col; matrix starts from 1
											sparseline[2]=profValues_targets.get(i); //Val
											if (!sparseline.equals(new String[3])) {
												try {
													profilsparsefile_targets.get(model).writeNext(sparseline);
												} catch (Exception e) {
													System.out.println("In die Outputdatei " + config.getProfilfileSparse(model.getName(),true) + " konnte nicht geschrieben werden.");
													e.printStackTrace();
													worked = false;
												}	
												sparseline = new String[3];
											}
										}
									}
								}
							} //end sparse profile
							if (config.createProfilSvmlight()) {
								//output: "1 column(=VariableNo.):value #PID"
								newline = new ArrayList<String>();
								newline.add("1");
								for (int i =0; i<profValues.size(); i++) {
									if (!profValues.get(i).equals(Consts.navalue)) {
										newline.add(Integer.toString(i+1) + ":" + profValues.get(i));
									}
								}
								newline.add("#"+patient.getPid());
								try {
									profilsvmlightfile.get(model).writeNext(newline.toArray(new String[newline.size()]));
								} catch (Exception e) {
									System.err.println("In die Outputdatei " + config.getProfilfileSvmlight(model.getName(),false) + " konnte nicht geschrieben werden.");
									e.printStackTrace();
									worked = false;
								}
								//same for targets
								if (model.hasTargets()) {
									//output: "1 column(=VariableNo.):value #PID"
									newline = new ArrayList<String>();
									newline.add("1");
									for (int i =0; i<profValues_targets.size(); i++) {
										if (!profValues_targets.get(i).equals(Consts.navalue)) {
											newline.add(Integer.toString(i+1) + ":" + profValues_targets.get(i));
										}
									}
									newline.add("#"+patient.getPid());
									try {
										profilsvmlightfile_targets.get(model).writeNext(newline.toArray(new String[newline.size()]));
									} catch (Exception e) {
										System.err.println("In die Outputdatei " + config.getProfilfileSvmlight(model.getName(),true) + " konnte nicht geschrieben werden.");
										e.printStackTrace();
										worked = false;
									}
								}
							} //end svmlight
						}
					} //end rolling through models
					//for sparse: write PID in row-translation.file
					if (config.createProfilSparse() && isincluded) {
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
				} //end if profile needs creation
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
					ArrayList<String> header = new ArrayList<String>();
					header.add(Consts.idfieldheader);
					header.addAll(knownVariables.get(model));
					Utils.addHeaderToCsv(config.getProfilfileDenseTmp(model.getName(),false),header, config.getProfilfileDense(model.getName(),false));
				    System.out.println("Outputdatei " + config.getProfilfileDense(model.getName(),false) + " wurde erfolgreich geschrieben.");
				    //same for targets
				    if (model.hasTargets()) {
					  	//close file
				    	profildensefile_targets.get(model).close();
						//add header by creating new file, and write all rows from tmpfile to new file
						header = new ArrayList<String>();
						header.add(Consts.idfieldheader);
						header.addAll(knownVariables_targets.get(model));
						Utils.addHeaderToCsv(config.getProfilfileDenseTmp(model.getName(),true),header, config.getProfilfileDense(model.getName(),true));
					    System.out.println("Outputdatei " + config.getProfilfileDense(model.getName(),true) + " wurde erfolgreich geschrieben.");
				    }
				}
				if (config.createProfilSparse()) { 
					//write COL-translation file for sparse profile (vars)
					for (int i =0; i<knownVariables.get(model).size(); i++) {
						String[] varcol = {Integer.toString(i+1), knownVariables.get(model).get(i)};
						try {
							profilsparsefileCols.get(model).writeNext(varcol);
						} catch (Exception e) {
							System.out.println("In die Outputdatei " + config.getProfilfileSparseCOLs(model.getName(),false) + " konnte nicht geschrieben werden.");
							e.printStackTrace();
							worked = false;
						}	
					}
					//same for targets
					 if (model.hasTargets()) {
						//write COL-translation file for sparse profile (vars)
						for (int i =0; i<knownVariables_targets.get(model).size(); i++) {
							String[] varcol = {Integer.toString(i+1), knownVariables_targets.get(model).get(i)};
							try {
								profilsparsefileCols_targets.get(model).writeNext(varcol);
							} catch (Exception e) {
								System.out.println("In die Outputdatei " + config.getProfilfileSparseCOLs(model.getName(),true) + " konnte nicht geschrieben werden.");
								e.printStackTrace();
								worked = false;
							}	
						}
					 }
					//close files
					profilsparsefile.get(model).close();
					profilsparsefileRows.get(model).close();
					profilsparsefileCols.get(model).close();
					if (model.hasTargets()) profilsparsefileCols_targets.get(model).close();
					System.out.println("Outputdatei " + config.getProfilfileSparse(model.getName(),false) + " wurde erfolgreich geschrieben.");
				}
				if (config.createProfilSvmlight()) { 
					//close file
					profilsvmlightfile.get(model).close();
				    System.out.println("Outputdatei " + config.getProfilfileSvmlight(model.getName(),false) + " wurde erfolgreich geschrieben.");
				    //same for targets
				    if (model.hasTargets()) {
				    	//close file
						profilsvmlightfile_targets.get(model).close();
					    System.out.println("Outputdatei " + config.getProfilfileSvmlight(model.getName(),true) + " wurde erfolgreich geschrieben.");
				    }
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
