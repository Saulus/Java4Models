package models;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import configuration.Consts;
import configuration.Datei;
import configuration.Konfiguration;
import configuration.Utils;
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
	
	private InputFile ourLeaderfile = null;
	
	/** The outputfile. */
	private CSVWriter outputfile; 
	
	/** The profildensefiles */
	private HashMap<Model,CSVWriter> profildensefile = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profildensefile_targets = new HashMap<Model,CSVWriter>();
	
	/** Matlab format */
	private HashMap<Model,CSVWriter> profilsparsefile = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profilsparsefileRows = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profilsparsefile_targets = new HashMap<Model,CSVWriter>();
	
	/** Svmlight format */
	private HashMap<Model,CSVWriter> profilsvmlightfile = new HashMap<Model,CSVWriter>();
	private HashMap<Model,CSVWriter> profilsvmlightfile_targets = new HashMap<Model,CSVWriter>();
	
	/** The known variables. */
	private HashMap<Model,ArrayList<String>> knownVariables = new HashMap<Model,ArrayList<String>>();//holds known variables per Model    
	private HashMap<Model,ArrayList<String>> knownVariables_targets = new HashMap<Model,ArrayList<String>>();//holds known variables per Model (targets only)
	
	/**
	 * Find next patient. Uses either the first available ID from all files (sorted by ID),
	 * or in case of leaderfile the next id from that... warps back all other files where last id fits
	 *
	 * @return the patient
	 */
	private Patient findNextPatient() {
		Patient newpatient= null;
		if (ourLeaderfile == null) { 
			// traditional work through; pid by pid
			List<String> pidliste = new ArrayList<String>();
			for (InputFile infile : inputfiles) {
				if (infile.hasRow()) 
					pidliste.add(infile.getID());
			}
			if (!pidliste.isEmpty()) {
				Collections.sort(pidliste);
				newpatient= new Patient(pidliste.get(0));
			}
		} else {
			//leaderfile: row by row in that file
			//warp back all others
			if (ourLeaderfile.hasRow()) {
				newpatient= new Patient(ourLeaderfile.getID());
				for (InputFile infile : inputfiles) {
					try {
						if (infile != ourLeaderfile) infile.warpToCorrectID(newpatient.getPid());
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
			}
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
				infile.nextRow(true,ourLeaderfile != null);
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
					String newpath;
					//add new column from zusatzinfo?
					if (nextfile.hasZusatzinfo()) {
						CSVReader reader = new CSVReader(new FileReader(nextfile.getZusatzinfo()), ';', '"');
						List<String[]> myEntries = reader.readAll();
						reader.close();
						//first line = header-line
						String[] headerline = myEntries.get(0); myEntries.remove(0);
						for (int j=0; j<headerline.length; j++) { headerline[j] = headerline[j].toUpperCase(); }
						//create HashMap for easy translation
						HashMap<String,String> translator = new HashMap<String,String>(); 
						for (String[] nextline : myEntries) {
							translator.put(nextline[0], nextline[1]);
						}
						newpath = sortfile.sortFileByID(true,headerline[0],headerline[1],translator);
					} else newpath = sortfile.sortFileByID();
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
				newfile.setLeader(nextfile.isLeadingTable());
				if (newfile.isLeader()) ourLeaderfile=newfile;
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
			Model newmodel;
			for (File file : listOfFiles) {
			    if (file.isFile()) {
			    	String modelname = file.getName().replaceFirst("[.][^.]+$", "");
			    	if (!processedModels.contains(modelname)) {
			    		try {
			    			newmodel = new Model(modelname, inputfiles, config);
			    			models.add(newmodel);
			    			knownVariables.put(newmodel,  new ArrayList<String>()); //init
			    			knownVariables_targets.put(newmodel,  new ArrayList<String>()); //init
			    			processedModels.add(modelname);
			    			timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
			    			System.out.println(timeStamp + " Modell "+ modelname + " konfiguriert.");
			    			worked = true; //if one files is ok, then start
			    		} catch (Exception e) {
							System.err.println("Fehler gefunden bei Konfiguration des Modells " + modelname + ". Das Modell wird nicht verwendet.");
							System.err.println("Ort: "+ e.getMessage());
							System.err.println("Ursache: "+ e.getCause());
							//e.printStackTrace();
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
						
						if (model.hasTargets()) {
							profilsvmlightfile_targets.put(model, new CSVWriter(new FileWriter(config.getProfilfileSvmlight(model.getName(),true)), ' ', CSVWriter.NO_QUOTE_CHARACTER));
							String[] newline3 = {"#Profile targets in svmlight format, see http://svmlight.joachims.org (targets always 1, pids in #info part)"};
							profilsvmlightfile_targets.get(model).writeNext(newline3);
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
	
	private void workThroughInfile (InputFile infile, Patient patient) throws Exception {
		//loop though models and process row for the patient
		for (Model model : models) {
			patient.processRow(model, infile);
		}
		infile.nextRow(true,ourLeaderfile != null);
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
				//it its the leaderfile: process only once
				if (infile == ourLeaderfile) {
					try {
						workThroughInfile(infile,patient);
					} catch (Exception e) {
						System.err.println(e.getMessage());
						worked=false;
						break;
					}
				} else {
					//if no leaderfile: ..loop...that still has a valid Row and the correct Patient  
					while ((infile.hasRow()) && patient.isPatient(infile.getID())) {
						try {
							workThroughInfile(infile,patient);
						} catch (Exception e) {
							System.err.println(e.getMessage());
							worked=false;
							break;
						}
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
					//if leaderfile: get all leaderfile_columns
					String[] leaderrow = null;
					if (this.ourLeaderfile!=null) {
						leaderrow = new String[this.ourLeaderfile.getColnames().length];
						//get Value nor from current but from last chached row 
						for (int i=0;i<leaderrow.length;i++) leaderrow[i]=this.ourLeaderfile.getValueLastCached(this.ourLeaderfile.getColnames()[i]);
					}
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
								//output PID+Profilvalues
								worked = writeDenseProfileRow(profildensefile.get(model), config.getProfilfileDense(model.getName(),false), patient, profValues, leaderrow);
								//same for targets, w/o leaderrow
								if (worked && model.hasTargets()) 
									worked = writeDenseProfileRow(profildensefile_targets.get(model), config.getProfilfileDense(model.getName(),true), patient, profValues_targets, null);								
							} //end dense profile
							if (config.createProfilSparse()) {
								worked = writeMatlabProfileRow(profilsparsefile.get(model), config.getProfilfileSparse(model.getName(),false), patient, profValues, leaderrow, rowNo);
								//same for targets, w/o leaderrow
								if (worked && model.hasTargets()) 
									worked = writeMatlabProfileRow(profilsparsefile_targets.get(model), config.getProfilfileSparse(model.getName(),true), patient, profValues_targets, null,rowNo);
							} //end sparse profile
							if (config.createProfilSvmlight()) {
								//output: "1 column(=VariableNo.):value #PID"
								worked = writeSvmlightProfileRow(profilsvmlightfile.get(model), config.getProfilfileSparse(model.getName(),false), patient, profValues, leaderrow);
								//same for targets, w/o leaderrow
								if (worked && model.hasTargets()) 
									worked = writeSvmlightProfileRow(profilsvmlightfile_targets.get(model), config.getProfilfileSparse(model.getName(),true), patient, profValues_targets, null);
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
		String[] leaderheader = null;
		if (this.ourLeaderfile!=null) leaderheader = this.ourLeaderfile.getColnames();
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
					worked = writeDenseHeader(config.getProfilfileDenseTmp(model.getName(),false),config.getProfilfileDense(model.getName(),false),knownVariables.get(model),leaderheader);
				    //same for targets
				    if (worked && model.hasTargets()) {
					  	//close file
				    	profildensefile_targets.get(model).close();
						//add header by creating new file, and write all rows from tmpfile to new file
				    	worked = writeDenseHeader(config.getProfilfileDenseTmp(model.getName(),true),config.getProfilfileDense(model.getName(),true),knownVariables_targets.get(model),null);
				    }
				}
				if (config.createProfilSparse()) { 
					//close files
					profilsparsefile.get(model).close();
					System.out.println("Outputdatei " + config.getProfilfileSparse(model.getName(),false) + " wurde erfolgreich geschrieben.");
					profilsparsefileRows.get(model).close();
					if (model.hasTargets()) {
				    	//close file
						profilsparsefile_targets.get(model).close();
					    System.out.println("Outputdatei " + config.getProfilfileSparse(model.getName(),true) + " wurde erfolgreich geschrieben.");
				    }
					//add header by creating new file, and write all rows from tmpfile to new file
					worked = writeMatlabHeader(config.getProfilfileSparseCOLs(model.getName(),false),knownVariables.get(model),leaderheader);
				    //same for targets
				    if (worked && model.hasTargets()) {
				    	//add header by creating new file, and write all rows from tmpfile to new file
					  	worked = writeMatlabHeader(config.getProfilfileSparseCOLs(model.getName(),true),knownVariables_targets.get(model),null);
				    }
				}
				if (config.createProfilSvmlight()) { 
					//close file
					profilsvmlightfile.get(model).close();
					if (model.hasTargets()) {
				    	//close file
						profilsvmlightfile_targets.get(model).close();
					    System.out.println("Outputdatei " + config.getProfilfileSvmlight(model.getName(),true) + " wurde erfolgreich geschrieben.");
				    }
					//write header
					worked = writeSvmlightHeader(config.getProfilfileSvmlightHeader(model.getName(),false),knownVariables.get(model),leaderheader);
				    //same for targets
				    if (worked && model.hasTargets()) {
						//add header by creating new file, and write all rows from tmpfile to new file
				    	worked = writeSvmlightHeader(config.getProfilfileSvmlightHeader(model.getName(),true),knownVariables_targets.get(model),null);
				    }
				}
			}
		} catch (Exception e) {
			System.out.println("Fehler beim Schlieﬂen der Dateien.");
			e.printStackTrace();
			worked = false;
		}
		return worked;
	}
	
	
	//helper functions for profile output
	private boolean writeDenseProfileRow(CSVWriter file, String filename, Patient patient, ArrayList<String> profValues, String[] leaderrow) {
		//output PID+Profilvalues only
		ArrayList<String> newline = new ArrayList<String>();
		newline.add(patient.getPid());
		newline.addAll(profValues);
		try {
			//first write leaderfile columns (only for profiles)
			if (leaderrow!=null) file.writeNext(Utils.concatArrays(leaderrow,newline.toArray(new String[newline.size()])));
			//else write rest
			else file.writeNext(newline.toArray(new String[newline.size()]));
		} catch (Exception e) {
			System.err.println("In die Outputdatei " + filename + " konnte nicht geschrieben werden.");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private boolean writeDenseHeader(String filename_old, String filename_new, ArrayList<String> variables, String[] leaderheader) {
		ArrayList<String> header;
		//first:add leadercolumns
		if (leaderheader != null) header = new ArrayList<String>(Arrays.asList(leaderheader));
		else header = new ArrayList<String>();
		header.add(Consts.idfieldheader);
		header.addAll(variables);
		try {
			Utils.addHeaderToCsv(filename_old,header, filename_new);
		} catch (Exception e) {
			System.err.println("In die Outputdatei " + filename_new + " konnte nicht geschrieben werden.");
			e.printStackTrace();
			return false;
		}
	    System.out.println("Outputdatei " + filename_new + " wurde erfolgreich geschrieben.");
	    return true;
	}

	
	private boolean writeMatlabProfileRow(CSVWriter file, String filename, Patient patient, ArrayList<String> profValues, String[] leaderrow, long rowNo) {
		//output: row PID No, column (=Variable no.), value
		String[] sparseline = new String[3];
		int starterno=0;
		//first write leaderrow
		if (leaderrow!=null) {
			for (int i =0; i<leaderrow.length; i++) {
				if (!leaderrow[i].equals(Consts.navalue) && !leaderrow[i].isEmpty()) {
					sparseline[0]=Long.toString(rowNo); //Row
					sparseline[1]=Integer.toString(i+1); //Col; matrix starts from 1
					sparseline[2]=leaderrow[i]; //Val
					try {
						file.writeNext(sparseline);
					} catch (Exception e) {
						System.out.println("In die Outputdatei " + filename + " konnte nicht geschrieben werden.");
						e.printStackTrace();
						return false;
					}
					sparseline = new String[3];
				}
			}
			starterno = leaderrow.length;
		}
		//Next: profvalues
		for (int i =0; i<profValues.size(); i++) {
			if (!profValues.get(i).equals(Consts.navalue)) {
				sparseline[0]=Long.toString(rowNo); //Row
				sparseline[1]=Integer.toString(i+1+starterno); //Col; matrix starts from 1
				sparseline[2]=profValues.get(i); //Val
				if (!sparseline.equals(new String[3])) {
					try {
						file.writeNext(sparseline);
					} catch (Exception e) {
						System.out.println("In die Outputdatei " + filename + " konnte nicht geschrieben werden.");
						e.printStackTrace();
						return false;
					}	
					sparseline = new String[3];
				}
			}
		}
		return true;
	}
	
	private boolean writeMatlabHeader(String filename, ArrayList<String> variables, String[] leaderrow) {
		//Col-&Row-Translation File
		int starterno=0;
		try {
			CSVWriter file= new CSVWriter(new FileWriter(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
			//write header
			String[] colline = {"COL_NO","VARIABLE"};
			file.writeNext(colline);
			if (leaderrow!=null) {
				for (int i =0; i<leaderrow.length; i++) {
					String[] varcol = {Integer.toString(i+1),leaderrow[i]};
					file.writeNext(varcol);
				}
				starterno=leaderrow.length;
			}
			for (int i =0; i<variables.size(); i++) {
				String[] varcol = {Integer.toString(i+1+starterno), variables.get(i)};
				file.writeNext(varcol);
			}
			file.close();
		} catch (Exception e) {
			System.err.println("In die Outputdatei " + filename + " konnte nicht geschrieben werden.");
			e.printStackTrace();
			return false;
		}
	    System.out.println("Outputdatei " + filename + " wurde erfolgreich geschrieben.");
	    return true;
	}
	
	
	
	private boolean writeSvmlightProfileRow(CSVWriter file, String filename, Patient patient, ArrayList<String> profValues, String[] leaderrow) {
		//output: "1 column(=VariableNo.):value #PID"
		ArrayList<String> newline = new ArrayList<String>();
		newline.add("1");
		int starterno=0;
		//first write leaderrow
		if (leaderrow!=null) {
			for (int i =0; i<leaderrow.length; i++) {
				if (!leaderrow[i].equals(Consts.navalue)) 
					newline.add(Integer.toString(i+1) + ":" + leaderrow[i]);
			}
			starterno = leaderrow.length;
		}
		//Next: profvalues
		for (int i =0; i<profValues.size(); i++) {
			if (!profValues.get(i).equals(Consts.navalue))
				newline.add(Integer.toString(i+1+starterno) + ":" + profValues.get(i));
		}
		newline.add("# "+patient.getPid());
		try {
			file.writeNext(newline.toArray(new String[newline.size()]));
		} catch (Exception e) {
			System.err.println("In die Outputdatei " + filename + " konnte nicht geschrieben werden.");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private boolean writeSvmlightHeader(String filename, ArrayList<String> variables, String[] leaderrow) {
		int starterno=0;
		try {
			CSVWriter file= new CSVWriter(new FileWriter(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
			//write header
			String[] newline = {"#Header to features, separated by linespace"};
			file.writeNext(newline);
			if (leaderrow!=null) {
				for (int i =0; i<leaderrow.length; i++) {
					String[] varhead = {Integer.toString(i+1),leaderrow[i]};
					file.writeNext(varhead);
				}
				starterno=leaderrow.length;
			}
			for (int i =0; i<variables.size(); i++) {
				String[] varhead = {Integer.toString(i+1+starterno),variables.get(i)};
				file.writeNext(varhead);
			}
			file.close();
		} catch (Exception e) {
			System.err.println("In die Outputdatei " + filename + " konnte nicht geschrieben werden.");
			e.printStackTrace();
			return false;
		}
	    System.out.println("Outputdatei " + filename + " wurde erfolgreich geschrieben.");
	    return true;
	}
	
	
}
