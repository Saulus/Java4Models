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
	@Element(defaultValue="Ja")
	public String istsortiert;
	
	/**
	 * Checks if is sorted.
	 *
	 * @return true, if is sorted
	 */
	public boolean isSorted() {
		return istsortiert.toUpperCase().equals(Consts.isSortedFlag);
	}
	
	/**
	 * Sets the checks if is sorted.
	 *
	 * @param b the new checks if is sorted
	 */
	public void setIsSorted(boolean b) {
		if (b) {
			istsortiert=Consts.isSortedFlag;
		} else {
			istsortiert="Nein";
		}
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

}
