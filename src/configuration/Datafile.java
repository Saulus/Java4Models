package configuration;

import org.xmappr.Element;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * The Class Datafile.
 * Elements from konfiguration.xml are loaded into here, into variables defined acc. to xml tag.
 * Class for Tags konfiguration->inputfiles->datafile
 */
public class Datafile {
	
	/** The data_id. */
	@Element
	public String data_id; 
	
	/** The path. */
	@Element
	public String path;
	
	/** The type. */
	@Element
	public String type;
	
	/** The is_sorted. */
	@Element(defaultValue="false")
	public boolean is_sorted;
	
	/** The id field. */
	@Element(defaultValue="PID")
	public String idfield;
	
	@Element(defaultValue="false")
	public boolean leadingtable;
	
	@Element
	public String leadingtable_columns;
	
	@Element
	public String leadingtable_numfield;
	
	@Element
	public String addinfo;
	
	@Element
	public String separator;
	
	@Element
	public String quote;
	
	
	
	/**
	 * Checks if is sorted.
	 *
	 * @return true, if is sorted
	 */
	public boolean isSorted() {
		return is_sorted;
	}
	
	/**
	 * Sets the checks if is sorted.
	 *
	 * @param b the new checks if is sorted
	 */
	public void setIsSorted(boolean b) {
		is_sorted=b;
	}
	
	/**
	 * Gets the data_id.
	 *
	 * @return the data_id
	 */
	public String getDatentyp() {
		return data_id.toUpperCase();
	}
	
	/**
	 * Gets the path.
	 *
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * Sets the path.
	 *
	 * @param p the new path
	 */
	public void setPath(String p) {
		path = p;
	}

	/**
	 * Gets the filetype.
	 *
	 * @return the filetype
	 */
	public String getFiletype() {
		return type.toUpperCase();
	}
	
	/**
	 * Sets the filetype.
	 *
	 * @param f the new filetype
	 */
	public void setFiletype(String f) {
		type = f;
	}
	
	public String[] getIdfeld() {
		if (idfield==null) idfield = "PID";
		String[] tokens = idfield.toUpperCase().split(Consts.fieldcombineseparator);
		return tokens;
	}
	
	public boolean hasZusatzinfo() {
		return (addinfo != null && !addinfo.equals(""));
	}
	
	public String getZusatzinfo() {
		if (addinfo != null) return addinfo;
		return "";
	}
	
	public boolean isLeadingTable() {
		return leadingtable;
	}
	
	public boolean hasSpecificColumns() {
		if (leadingtable_columns==null) return false;
		else return true;
	}
	
	public String[] getSpecificColumns() {
		String[] tokens = null;
		if (leadingtable_columns==null) return tokens;
		tokens = leadingtable_columns.split(Consts.fieldcombineseparator);
		return tokens;
	}
	
	public boolean hasNumfield() {
		if (leadingtable_numfield==null) return false;
		else return true;
	}
	
	public String getNumfield() {
		return leadingtable_numfield;
	}
	
	public char getSeparator(){
		if (separator == null) return Consts.csvfieldseparator.charAt(0);
		return separator.charAt(0);
	}
	
	public char getQuote(){
		//if (quote == null) return '"';
		if (quote == null || quote.length()==0) return CSVWriter.NO_QUOTE_CHARACTER;
		return quote.charAt(0);
	}
	
	

}
