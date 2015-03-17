package configuration;

import java.util.HashMap;

/**
 * The Class FileDefinitions.
 * Defines Satzart-Files (fixed length) in flatpack xml format, for later reference by column.
 * Used columns only.
 * Format Grouper 2014
 */
public class FileDefinitions {
	
	/** The sa100. */
	private String sa100 = "<?xml version='1.0'?>" +
			"<!DOCTYPE PZMAP SYSTEM 'flatpack.dtd' >" +
			"<PZMAP>" +
			"<COLUMN name='KENNZEICHEN' length='3' />" +
			"<COLUMN name='BERICHTSJAHR' length='4' />" +
			"<COLUMN name='BETRIEBSNUMMER' length='8' />" +
			"<COLUMN name='PID' length='38' />" +
			"<COLUMN name='KV_NR_KENNZEICHEN' length='1' />" +
			"<COLUMN name='GEBURTSJAHR' length='4' />" +
			"<COLUMN name='GESCHLECHT' length='1' />" +
			"<COLUMN name='VERSICHERTENTAGE' length='3' />" +
			"<COLUMN name='VERSTORBEN' length='1' />" +
			"</PZMAP>";
	
	/** The sa400. */
	private String sa400 = "<?xml version='1.0'?>" +
			"<!DOCTYPE PZMAP SYSTEM 'flatpack.dtd' >" +
			"<PZMAP>" +
			"<COLUMN name='KENNZEICHEN' length='3' />" +
			"<COLUMN name='BERICHTSJAHR' length='4' />" +
			"<COLUMN name='BETRIEBSNUMMER' length='8' />" +
			"<COLUMN name='PID' length='38' />" +
			"<COLUMN name='VERORDNUNGSDATUM' length='8' />" +
			"<COLUMN name='PZN' length='8' />" + //War: 7
			"<COLUMN name='ANZAHL_DER_PACKUNGEN' length='3' />" +
			//"<COLUMN name='FAKTOR' length='10' />" + //erst in neueren Satzarten
			"</PZMAP>";
	
	/** The sa500. */
	private String sa500 = "<?xml version='1.0'?>" +
			"<!DOCTYPE PZMAP SYSTEM 'flatpack.dtd' >" +
			"<PZMAP>" +
			"<COLUMN name='KENNZEICHEN' length='3' />" +
			"<COLUMN name='BERICHTSJAHR' length='4' />" +
			"<COLUMN name='BETRIEBSNUMMER' length='8' />" +
			"<COLUMN name='PID' length='38' />" +
			"<COLUMN name='ENTLASSUNGSMONAT' length='6' />" +
			"<COLUMN name='FALLZAEHLER' length='2' />" +
			"<COLUMN name='ICD' length='7' />" +
			"<COLUMN name='LOKALISATION' length='1' />" +
			"<COLUMN name='ART_DER_DIAGNOSE' length='1' />" +
			"<COLUMN name='ART_DER_BEHANDLUNG' length='1' />" +
			"</PZMAP>";
	
	/** The sa600. */
	private String sa600 = "<?xml version='1.0'?>" +
			"<!DOCTYPE PZMAP SYSTEM 'flatpack.dtd' >" +
			"<PZMAP>" +
			"<COLUMN name='KENNZEICHEN' length='3' />" +
			"<COLUMN name='BERICHTSJAHR' length='4' />" +
			"<COLUMN name='BETRIEBSNUMMER' length='8' />" +
			"<COLUMN name='PID' length='38' />" +
			"<COLUMN name='LQ' length='1' />" +
			"<COLUMN name='ICD' length='7' />" +
			"<COLUMN name='QUALIFIZIERUNG' length='1' />" +
			"<COLUMN name='LOKALISATION' length='1' />" +
			"<COLUMN name='ABRECHNUNGSWEG' length='1' />" +
			"</PZMAP>";
	
	/** The datentypen. */
	private HashMap<String,String> datentypen = new HashMap<String,String>(); 
	
	/**
	 * Instantiates a new file definitions.
	 */
	public FileDefinitions() {
		datentypen.put("STAMM", sa100); //Stamm
		datentypen.put("ARZNEIMITTEL", sa400); //Arzneimittel
		datentypen.put("STATIONAER", sa500); //Stationaer
		datentypen.put("AMBULANT", sa600); //Ambulant
	}
	
	/**
	 * Gets the definition.
	 *
	 * @param datentyp the datentyp
	 * @return the definition
	 */
	public String getDefinition (String datentyp) {
		return datentypen.get(datentyp.toUpperCase());
	}

}
