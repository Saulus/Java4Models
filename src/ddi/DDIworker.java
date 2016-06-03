package ddi;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import au.com.bytecode.opencsv.CSVWriter;
import configuration.DDIConfiguration;
import models.InputFile;

public class DDIworker {
	private final static Logger LOGGER = Logger.getLogger(DDIworker.class.getName());
	
	private DDIMatrix ddimatrix;
	private DDIInputFile ddifile=null;
	private boolean createStats = false;
	private DDIStats stats;
	
	
	public DDIworker () {
		
	}
	
	/*
	 * initializes
	 * a) the interaction matrix
	 * b) new inputfile as interaction-leaderfile, and output files for statistics (if needed)
	 * 
	 * must be called 
	 * - after InputFiles are created (and sorted, if needed)
	 * - before Models are created  
	 */
	public boolean init(DDIConfiguration ddiconfig,ArrayList<InputFile> inputfiles,boolean upcase) {
		//String timeStamp;
		boolean worked = true;
		//1. Open Inputfiles, create matrix, remove inputfile from list
		ddimatrix = new DDIMatrix(ddiconfig.getDrugreachStandard());
		
		ListIterator<InputFile> itFile = inputfiles.listIterator();
		InputFile infile;
		while (itFile.hasNext()) {
			infile = itFile.next();
			if (infile.getDatentyp().equals(ddiconfig.getFilePatient2drug())) {
				try {
					//create new InputFile and add
					ddifile = new DDIInputFile(ddiconfig,infile,ddimatrix,upcase);
					itFile.add(ddifile);
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,"Fehler gefunden beim Einlesen der DDI Konfiguration " + infile.getPath(), e);
					worked = false;
				}
			} else if (infile.getDatentyp().equals(ddiconfig.getFileDrug2substance())) {
				try {
					String drug;
					String substance;
					int i=0;
					while (infile.nextRow()) {
						drug = infile.getValue(ddiconfig.getDrugfield());
						substance = infile.getValue(ddiconfig.getSubstancefield());
						if (!drug.isEmpty() && !substance.isEmpty())
							ddimatrix.addDrug2Substance(drug, substance);
						i++;
					}
					LOGGER.log(Level.INFO,Integer.toString(i) + " Zeilen aus File " + infile.getDatentyp() +" eingelesen");
					infile.close();
					itFile.remove();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,"Fehler gefunden beim Einlesen der DDI Konfiguration " + infile.getPath() , e);
					worked = false;
				}
			} else if (infile.getDatentyp().equals(ddiconfig.getFileSubstance2interaction())) {
				try {
					String substance;
					String interaction;
					String[] columns;
					int i=0;
					while (infile.nextRow()) {
						interaction = infile.getValue(ddiconfig.getInteractionfield());
						//format might be different, contains multiple columns with Substance-IDs
						columns=infile.getColnames();
						for (int k=0; k<columns.length;k++) {
							if (columns[k].contains(ddiconfig.getSubstancefield())) {
								substance = infile.getValue(columns[k]);
								if (!substance.isEmpty() && !interaction.isEmpty())
									ddimatrix.addSubstance2Interaction(substance, interaction);
							}
						}
						i++;
					}
					LOGGER.log(Level.INFO,Integer.toString(i) + " Zeilen aus File " + infile.getDatentyp() +" eingelesen");
					infile.close();
					itFile.remove();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,"Fehler gefunden beim Einlesen der DDI Konfiguration " + infile.getPath() , e);
					worked = false;
				}
			} else if (infile.getDatentyp().equals(ddiconfig.getFileInteraction2meta())) {
				try {
					String interaction;
					String meta;
					int i=0;
					while (infile.nextRow()) {
						interaction = infile.getValue(ddiconfig.getInteractionfield());
						meta = infile.getValue(ddiconfig.getMeta_interactionfield());
						if (!interaction.isEmpty() && !meta.isEmpty())
							ddimatrix.addInteraction2Meta(interaction, meta);
						i++;
					}
					LOGGER.log(Level.INFO,Integer.toString(i) + " Zeilen aus File " + infile.getDatentyp() +" eingelesen");
					infile.close();
					itFile.remove();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,"Fehler gefunden beim Einlesen der DDI Konfiguration " + infile.getPath() , e);
					worked = false;
				}
			} else if (infile.getDatentyp().equals(ddiconfig.getFileInteractioninfo())) {
				try {
					String interaction;
					int i=0;
					while (infile.nextRow()) {
						interaction = infile.getValue(ddiconfig.getInteractionfield());
						if (!interaction.isEmpty())
							ddimatrix.verifyInteraction(interaction); //Erwartung: Falsche interaktionen werden durch den Filter bereits exkludiert
						i++;
					}
					LOGGER.log(Level.INFO,Integer.toString(i) + " Zeilen aus File " + infile.getDatentyp() +" eingelesen");
					infile.close();
					itFile.remove();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,"Fehler gefunden beim Einlesen der DDI Konfiguration " + infile.getPath() , e);
					worked = false;
				}
			} else if (infile.getDatentyp().equals(ddiconfig.getFileDruginfo())) {
				try {
					String drug;
					String drugreach;
					String dose;
					int i=0;
					while (infile.nextRow()) {
						drug = infile.getValue(ddiconfig.getDrugfield());
						drugreach = infile.getValue(ddiconfig.getDrugreachfield());
						dose = infile.getValue(ddiconfig.getDosefield());
						if (!drug.isEmpty())
							ddimatrix.addDrugInformation(drug, drugreach, dose);
						i++;
					}
					LOGGER.log(Level.INFO,Integer.toString(i) + " Zeilen aus File " + infile.getDatentyp() +" eingelesen");
					infile.close();
					itFile.remove();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,"Fehler gefunden beim Einlesen der DDI Konfiguration " + infile.getPath() , e);
					worked = false;
				}
			}
		}
		ddimatrix.clearAllUnverifiedInteractions();
		LOGGER.log(Level.INFO,"Die DDI Matrix besteht aus: " 
				+ Integer.toString(ddimatrix.getNumberDrugs()) + " Drugs, "
				+ Integer.toString(ddimatrix.getNumberSubstances()) + " Substances, "
				+ Integer.toString(ddimatrix.getNumberInteractions()) + " Interactions, "
				+ Integer.toString(ddimatrix.getNumberMetas()) + " Meta-Interactions.");

		if (ddifile==null) {
			LOGGER.log(Level.SEVERE,"Kein <patient2drug> File gefunden. Bitte DDI Konfiguration prüfen.");
			worked=false;
		}
		//base matrix is now final
		//create new output for statistics... 
		if (worked && ddiconfig.createStatistics) {
			stats = new DDIStats();
			for (String meta :ddimatrix.getMetaIds() ) {
				stats.addMeta(meta, ddimatrix.getNumInteractions4Meta(meta),ddimatrix.getNumSubstances4Meta(meta));
			}
			for (String interact :ddimatrix.getInteractionIds() ) {
				stats.addInteraction(interact, ddimatrix.getNumSubstances4Interaction(interact));
			}
			this.createStats=true;
			ddifile.mustCreateStatistic(stats);
		}
		return worked;
		
	}
	
	public InputFile getDDIInputFile() {
		return ddifile;
	}
	
	/*
	 * writes 2 statistics files (ddi_stats_meta + ddi_stats_interactions)
	 * 
	 * columns: id, num_interactions, num_substances, num_patients, num_all_patients, percent_patients, num_occurences, num_all_occurences, percent_occurences, num_target_XXX, percent_target_XXX, num_targetAll_XXX
	 */
	public boolean finish(String path) { 
		boolean worked = true;
		String[] header = new String[9+(this.stats.getNumberTargetKeys()*3)];
		header[0]="id";
		header[1]="num_interactions";
		header[2]="num_substances";
		header[3]="num_patients";
		header[4]="num_all_patients";
		header[5]="percent_patients";
		header[6]="num_occurences";
		
		int i=9;
		for (String target : this.stats.getTargetNames() ) {
			header[i]="num_target_"+target;
			header[i+1]="percent_target_"+target;
			header[i+2]="num_targetAll_"+target;
			i+=3;
		}
		if (this.createStats) {
			String filename_int = path+ "\\ddi_stats_interactions.csv"; CSVWriter stat_file_int = null;
			String filename_met = path+ "\\ddi_stats_metas.csv"; CSVWriter stat_file_met = null;
			try {
				stat_file_int = new CSVWriter(new FileWriter(filename_int), ';', CSVWriter.NO_QUOTE_CHARACTER);
				//write header
				stat_file_int.writeNext(header);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE,"Die Outputdatei " + filename_int + " konnte nicht erstellt werden.", e);
				worked = false;
			}
			try {
				stat_file_met = new CSVWriter(new FileWriter(filename_met), ';', CSVWriter.NO_QUOTE_CHARACTER);
				//write header
				stat_file_met.writeNext(header);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE,"Die Outputdatei " + filename_met + " konnte nicht erstellt werden.", e);
				worked = false;
			}
			if (!worked) return worked;
			//write all metas
			for (String id : this.stats.getMetaIds()) {
				stat_file_met.writeNext(newRow(header,id,true));
			}
			//write stats per interaction
			for (String id : this.stats.getInteractionIds()) {
				stat_file_int.writeNext(newRow(header,id,false));
			}
			//close
			try {
				stat_file_met.close();
				stat_file_int.close();
			} catch (Exception e) {
				LOGGER.log(Level.WARNING,"Fehler beimn Schließen der Dateien.", e);
				worked = false;
			}
		}
		return worked;
	}

	
	private String[] newRow (String[] header, String id, boolean ismeta) {
		String[] row= new String[header.length];
		row[0]=id;
		row[1]=Integer.toString(stats.getNumInteractions(id, ismeta)); 
		row[2]=Integer.toString(stats.getNumSubs(id, ismeta));
		row[3]=Integer.toString(stats.getNumPatients(id, ismeta));
		row[4]=Integer.toString(stats.getNumPatientsAll());
		row[5]=Double.toString(Math.round(stats.getNumPatients(id, ismeta)/stats.getNumPatientsAll()*100/100)*100);
		row[6]=Integer.toString(stats.getNumOccurences(id, ismeta));
		row[7]=Integer.toString(stats.getNumOccurencesAll());
		row[8]=Double.toString(Math.round(stats.getNumOccurences(id, ismeta)/stats.getNumOccurencesAll()*100/100));
		int i=9;
		for (String target : this.stats.getTargetNames() ) {
			row[i]=Integer.toString(stats.getNumTargets(id, ismeta, target));
			row[i+1]=Double.toString(Math.round(stats.getNumTargets(id, ismeta, target)/stats.getNumOccurences(id, ismeta)*100/100));
			row[i+2]=Integer.toString(stats.getNumTargetsAll(target));
			i+=3;
		}
		return row;
	}
	

}
