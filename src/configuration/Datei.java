package configuration;

import org.xmappr.Element;

/**
 * The Class Datei.
 * Elements from konfiguration.xml are loaded into here, into variables defined acc. to xml tag.
 * Class for Tags konfiguration->input->datei
 */
public class Datei {
	
	/** The datentyp. */
	@Element
	public String datentyp; 
	
	/** The pfad. */
	@Element
	public String pfad;
	
	/** The dateityp. */
	@Element(defaultValue="csv")
	public String dateityp;
	
	/** The istsortiert. */
	@Element(defaultValue="false")
	public boolean istsortiert;
	
	/** The id field. */
	@Element(defaultValue="PID")
	public String idfeld;
	
	/** The istsortiert. */
	@Element(defaultValue="false")
	public boolean leadingtable;
	
	@Element
	public String zusatzinfo;
	
	/**
	 * Checks if is sorted.
	 *
	 * @return true, if is sorted
	 */
	public boolean isSorted() {
		return istsortiert;
	}
	
	/**
	 * Sets the checks if is sorted.
	 *
	 * @param b the new checks if is sorted
	 */
	public void setIsSorted(boolean b) {
		istsortiert=b;
	}
	
	/**
	 * Gets the datentyp.
	 *
	 * @return the datentyp
	 */
	public String getDatentyp() {
		return datentyp.toUpperCase();
	}
	
	/**
	 * Gets the path.
	 *
	 * @return the path
	 */
	public String getPath() {
		return pfad;
	}
	
	/**
	 * Sets the path.
	 *
	 * @param p the new path
	 */
	public void setPath(String p) {
		pfad = p;
	}

	/**
	 * Gets the filetype.
	 *
	 * @return the filetype
	 */
	public String getFiletype() {
		return dateityp.toUpperCase();
	}
	
	/**
	 * Sets the filetype.
	 *
	 * @param f the new filetype
	 */
	public void setFiletype(String f) {
		dateityp = f;
	}
	
	public String[] getIdfeld() {
		if (idfeld==null) idfeld = "PID";
		String[] tokens = idfeld.split(Consts.idfieldseparator);
		return tokens;
	}
	
	public boolean hasZusatzinfo() {
		return (zusatzinfo != null && !zusatzinfo.equals(""));
	}
	
	public String getZusatzinfo() {
		if (zusatzinfo != null) return zusatzinfo;
		return "";
	}
	
	public boolean isLeadingTable() {
		return leadingtable;
	}
	

}
