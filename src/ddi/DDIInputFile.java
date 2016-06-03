package ddi;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import au.com.bytecode.opencsv.CSVWriter;
import configuration.Configuration;
import configuration.Consts;
import configuration.DDIConfiguration;
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
				//add statistics for patients with interactions
				if (mustCreateStatistic) {
					stats.addOccurence(index.getMeta(), index.getInteraction(),patient.getPid());
				}
			}
			inWarpMode=true;
		}
		return super.nextRow(false,allowWarpBack);
	}


}
