package models;

import java.util.HashMap;

import configuration.Consts;

/**
 * The Class ModelVariableReadIn.
 * Just for reading in Strings from csv-row, and returning as needed (e.e. uppercase if needed) 
 * Non-Uppercase for filters only!
 * 
 *  
 */
public class ModelVariableReadIn {
	//private final static Logger LOGGER = Logger.getLogger(ModelVariableReadIn.class.getName());
	
	private HashMap<String,String> myrow;
	private String[] columns;
	private String[] filters;

	public ModelVariableReadIn(HashMap<String,String> myrow, int filtercolumns, InputFile myinputfile) throws ModelConfigException {
		this.myrow = myrow; 
		columns = new String[filtercolumns];
		filters = new String[filtercolumns];
		for (int j=0; j<filtercolumns; j++) {
			columns[j]=myrow.get(Consts.modColumnCol + (j+1)).toUpperCase();
			filters[j]=myrow.get(Consts.modColfilterCol + (j+1));
			if (!columns[j].isEmpty() && !myinputfile.hasField(columns[j].split(Consts.bracketEsc)[0]))
					throw new ModelConfigException("Fehler bei Variable "+ myrow.get(Consts.modVariableCol) + ": "+ columns[j] + " existiert nicht in File "+ myinputfile.getDatentyp());
		}
	}
	

	
	public String getVariableCol() {
		return this.myrow.get(Consts.modVariableCol).toUpperCase();
	}
	public String getCalcCol() {
		return this.myrow.get(Consts.modCalcCol).toUpperCase();
	}
	public String getAggCol() {
		return this.myrow.get(Consts.modAggCol).toUpperCase();
	}
	public String getFilterCol() {
		return this.myrow.get(Consts.modFilterCol);
	}
	public String getIncludeCol() {
		return this.myrow.get(Consts.modIncludeCol).toUpperCase();
	}
	public String getExcludeCol() {
		return this.myrow.get(Consts.modExcludeCol).toUpperCase();
	}
	public String getTargetCol() {
		return this.myrow.get(Consts.modTargetCol).toUpperCase();
	}
	public String getHideCol() {
		return this.myrow.get(Consts.modHideCol).toUpperCase();
	}
	public String[] getColumns() {		
		return this.columns;
	}
	public String[] getFilters() {		
		return this.filters;
	}

}
