package ddi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import configuration.Configuration;
import configuration.Consts;
import configuration.DDIConfiguration;
import configuration.Utils;
import models.InputFile;
import models.Patient;

public class DDIInputFile extends InputFile {
	
	private InputFile drugdata_file;
	private DDIMatrix ddimatrix;
	private String input_col_drug;
	private String input_col_date;
	
	private LocalDate myreferencedate;
	
	private boolean mustCreateStatistic=false;
	private DDIStats stats;
	
	private boolean mustSamplePatients=false;
	private HashMap<Integer,Integer> dates_indexes = new HashMap<Integer,Integer>(); //Day -> Number
	private HashMap<Integer,Integer> dates_sampled = new HashMap<Integer,Integer>(); //Day -> Number
	
	
	public DDIInputFile (DDIConfiguration ddiconfig, InputFile drugdata_file,DDIMatrix ddimatrix, boolean upcaseData) throws Exception {
		super(ddiconfig.getDatentyp(),"",Consts.ddiFlag,new String[]{"PID"},"",upcaseData,null);
		int colsize=5; 	//fields: PID, Interaction_ID, Meta_Id, Start, End
		if (ddiconfig.getDosefield() != null) colsize++;
		colnames = new String[colsize];
		colnames[0] = "PID";
		colnames[1] = ddiconfig.getInteractionfield();
		colnames[2] = ddiconfig.getMeta_interactionfield();
		colnames[3] = ddiconfig.getStartfield();
		colnames[4] = ddiconfig.getEndfield();
		if (ddiconfig.getDosefield() != null) colnames[5]=ddiconfig.getDosefield();
		this.drugdata_file=drugdata_file;
		this.ddimatrix=ddimatrix;
		this.input_col_drug=ddiconfig.getDrugfield().toUpperCase();
			if (!drugdata_file.hasField(this.input_col_drug)) throw new Exception("Field " +this.input_col_drug + " not found in DDI-Datasource + " + drugdata_file.getDatentyp());
		this.input_col_date=ddiconfig.getDatefield().toUpperCase();
			if (!drugdata_file.hasField(this.input_col_date)) throw new Exception("Field " +this.input_col_date + " not found in DDI-Datasource + " + drugdata_file.getDatentyp());
		
		this.setLeader(true);
		String[] keepcols=ddiconfig.getKeepFields();
		if (keepcols!=null) this.setLeaderColnames(keepcols);
		
		myreferencedate=Configuration.getReferenceDate();
	}
	
	
	public void mustCreateStatistic(DDIStats stats) {
		this.mustCreateStatistic=true;
		this.stats=stats;
	}
	
	public void mustSamplePatients() {
		this.mustSamplePatients=true;
	}
	
	
	/*
	 * Create new Set of rows with DDI -> start if rowcache has been used (or is empty), and search until rowcache is filled again
	 * add to statistics
	 * @see models.InputFile#nextRow(boolean, boolean)
	 */
	public boolean nextRow(boolean checkID, boolean allowWarpBack) throws Exception {
		while (currentcachepointer>=rowcache.size()) {
			this.clearcache(true);
			//find next id in base file
			while (this.getID().equals(drugdata_file.getID()) && drugdata_file.hasRow()) {
				drugdata_file.nextRow(true,true);
			}
			if (!drugdata_file.hasRow()) {
				this.hasRow = false;
				return false;
			}
			this.currentID=drugdata_file.getID();
			Patient patient = new Patient(this.currentID);
			ddimatrix.clearAll();
			while ((drugdata_file.hasRow()) && patient.isPatient(drugdata_file.getID())) {
				ddimatrix.addLiveDrug(drugdata_file.getValue(input_col_drug), drugdata_file.getValue(input_col_date));
				drugdata_file.nextRow(true,true);
			}
			//warp back input file
			drugdata_file.warpToCorrectID(patient.getPid());
			ArrayList<Index> indexes = ddimatrix.getLiveInteractionIndexes();
			boolean hasInteractions = false;
			if (indexes.size()>0) {
				hasInteractions=true;
			} else {
				indexes = sampleIndexes();
			}

			// add indexes to cache
			LinkedHashMap<String,String> newrow;
			for (Index index : indexes) {
				newrow = new LinkedHashMap<String,String>();
				newrow.put(colnames[0], patient.getPid());
				newrow.put(colnames[1], index.getInteraction());
				newrow.put(colnames[2], index.getMeta());
				newrow.put(colnames[3], index.getStartDate().toString()); //Integer.toString(Days.daysBetween(myreferencedate, index.getStartDate()).getDays()));
				newrow.put(colnames[4], index.getEndDate().toString()); //Integer.toString(Days.daysBetween(myreferencedate, index.getEndDate()).getDays()));
				if (colnames.length>5) newrow.put(colnames[5], index.getDose()); 
				rowcache.add(newrow);
				//add to days from which will be sampled
				if (mustSamplePatients && hasInteractions) {
					int day=Days.daysBetween(myreferencedate, index.getStartDate()).getDays();
					if (! dates_indexes.containsKey(day)) dates_indexes.put(day,1);
					else dates_indexes.put(day,dates_indexes.get(day)+1);
				}
				//add statistics for patients
				if (mustCreateStatistic) {
					stats.addOccurence(index.getMeta(), index.getInteraction(),patient.getPid());
				}
			}
			inWarpMode=true;
		}
		return super.nextRow(false,allowWarpBack);
	}
	
	/*
	 * samples one index date for patients that occur in drug datafile, but do not have any indexes
	 */
	private ArrayList<Index> sampleIndexes() {
		ArrayList<Index> indexes = new ArrayList<Index> ();
		LocalDate start=null;
		boolean canBeSampled = false;
		/* take the next available day by
		 * - first finding current max quotient from used / available counts
		 * - then using the first day where the quota is not reached
		 */
		double quota=0; //init very small value
		for (Integer day : dates_indexes.keySet()) {
			if (dates_sampled.containsKey(day))
				quota = Math.max(quota, dates_sampled.get(day)/dates_indexes.get(day));
			else dates_sampled.put(day,0);
		}
		for (Integer day : dates_indexes.keySet()) {
			if ((dates_sampled.get(day)/dates_indexes.get(day))<quota) {
				canBeSampled=true;
				start=myreferencedate.plusDays(day);
				dates_sampled.put(day,dates_sampled.get(day)+1);
				break;
			}
		}
		//if all days are filled equally: select just first 
		if (!canBeSampled && dates_indexes.size()>0) {
			canBeSampled=true;
			int day = dates_indexes.keySet().iterator().next();
			start=myreferencedate.plusDays(day);
			dates_sampled.put(day,dates_sampled.get(day)+1);
		}
		// if no index days yet: take first drug date
		if (!canBeSampled) {
			start=Utils.parseDate(drugdata_file.getValue(input_col_date));
			int day=Days.daysBetween(myreferencedate, start).getDays();
			if (dates_sampled.containsKey(day))
				dates_sampled.put(day,dates_sampled.get(day)+1);
			else dates_sampled.put(day,1);
		}
		LocalDate end = start.plusDays(ddimatrix.getDrugreach_standard());
		Index index= new Index(start,end,"","");
		index.setDose("");
		indexes.add(index);
		return indexes;
	}

}
