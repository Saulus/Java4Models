package models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import configuration.Consts;
import configuration.FileDefinitions;
import configuration.Utils;
import au.com.bytecode.opencsv.CSVReader;
import net.sf.flatpack.DataError;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.brparse.BuffReaderDelimParser;
import net.sf.flatpack.brparse.BuffReaderFixedParser;
import net.sf.flatpack.brparse.BuffReaderParseFactory;


/**
 * The Class InputFile.
 * Represents one inputfile as defined in konfiguration.xml (tag datei)
 * uses flatpack for fixed length, i.e. Satzart-Files
 * uses CSVReader for csv files
 * 
 * returns value by column name
 */
public class InputFile {
	
	/** The datentyp, e.g. STAMM */
	private String datentyp; 
	
	/** The filetype, e.g. CSV */
	private String filetype;
	
	/** The path. */
	private String path; 
	
	/** The fixparse file. */
	private BuffReaderFixedParser fixparse = null;
	
	/** The csvparse file. */
	private BuffReaderDelimParser csvparse = null;
	
	private DataSet flatpackDataset;
	
	/** colnames **/
	private String[] colnames;
	
	/** cached rows if needed */
	private ArrayList<LinkedHashMap<String,String>> rowcache = new ArrayList<LinkedHashMap<String,String>>();
	
	/** points to last element in case that is to be used **/
	private int currentcachepointer=0;
	private String currentCachedID = "";
	
	private boolean inWarpMode = false;
	
	/** do I still have a row? */
	private boolean hasRow = false; 
	
	private IDfield[] idfields;
	private String currentID = "";
	
	private boolean isLeader = false; //leadingtable: diese Tabelle wird um Features erweitert, dh. alle Zeilen bleiben erhalten
	
	private boolean hasLeaderCols = false; //returns true if columns from leaderfile are to be added  
	private String[] leaderColnames;
	private boolean hasLeaderNumfield = false; //returns true if there is a specific rowno from leaderdile
	private String leaderNumfield;
	
	private boolean upcaseData=false;
	
	
	/**
	 * Instantiates a new input file.
	 *
	 * @param datentyp the datentyp
	 * @param path the path
	 * @param filetype the filetype
	 * @throws Exception the exception
	 */
	public InputFile (String datentyp, String path, String filetype, String[] idfields, boolean upcaseData) throws Exception {
		//ToDo: Eigene Reader-Classe als Wrapper für z.B. flatpack, csvreader usw.
		this.datentyp = datentyp;
		this.filetype = filetype;
		this.path = path;
		this.idfields = new IDfield[idfields.length];
		this.upcaseData=upcaseData;
		for (int i=0;i<idfields.length;i++) this.idfields[i] = new IDfield(idfields[i]);
		
		//check encoding first
		String encoding = Utils.checkEncoding(path);
		if (encoding==null) encoding="UTF-8";
		
		//new InputStreamReader(new FileInputStream(myFile), encoding)
		
		//new FileReader(path)
		if (filetype.equals(Consts.satzartFlag)) {
				FileDefinitions filedef = new FileDefinitions();
				String myDef = filedef.getDefinition(datentyp);
				//old: new FileReader(path));
				fixparse = (BuffReaderFixedParser) BuffReaderParseFactory.getInstance().newFixedLengthParser(new StringReader(myDef), new InputStreamReader(new FileInputStream(path), encoding));
				fixparse.setIgnoreExtraColumns(true); //ignores extra characters in lines that are too long; lines that are too short are ignored (i.e. first line)
				//fixparse.setStoreRawDataToDataSet(true); //for Testing only
				flatpackDataset = fixparse.parse();
				colnames=flatpackDataset.getColumns();
		} else {
				//Issue: Flatpack will not give back CSV column name correctly, when BuffReaderDelimParser is used (getValue works fine)
				//Workaround: Open csv beforehand and read first line
				CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(path), encoding), ';', '"');
				String [] firstLine;
				if ((firstLine = reader.readNext()) != null) {
					colnames = firstLine;  
				}
				reader.close();
				//make Uppercase
				for(int i=0; i<colnames.length; i++) {
					colnames[i]=colnames[i].toUpperCase();
				}
				csvparse = (BuffReaderDelimParser) BuffReaderParseFactory.getInstance().newDelimitedParser(new InputStreamReader(new FileInputStream(path), encoding),';','"');
				//csvparse.setStoreRawDataToDataSet(true);//for Testing only
				flatpackDataset = csvparse.parse();
		}
		
		if (flatpackDataset.getErrors() != null && !flatpackDataset.getErrors().isEmpty()) {
	            System.out.println("Fehler gefunden beim Einlesen von " + path);
	            for (int i = 0; i < flatpackDataset.getErrors().size(); i++) {
	                final DataError de = (DataError) flatpackDataset.getErrors().get(i);
	                System.out.println("Fehler: " + de.getErrorDesc() + " Zeile: " + de.getLineNo());
	            }
	    }
	}
	
	/**
	 * Checks for field.
	 *
	 * @param field the field
	 * @return true, if successful
	 */
	public boolean hasField (String field) {
		return Arrays.asList(colnames).contains(field);
	}
	
	/**
	 * Next row.
	 *
	 * @return true, if successful
	 */
	public boolean nextRow(boolean checkID, boolean allowWarpBack) throws Exception {
		if (this.inWarpMode) {
			//i.e. use current cache only!
			currentcachepointer++;
			//checkID only if last entry
			if (currentcachepointer==rowcache.size()) checkID=true;
			//stop warp mode if overshooting
			if (currentcachepointer>rowcache.size()) {
				//stop warp
				this.inWarpMode= false;
				currentcachepointer--;
			} else hasRow=true;
		}
		if (!this.inWarpMode) {
			hasRow = flatpackDataset.next();
			if (!hasRow) return false;
			if (!allowWarpBack) this.clearcache();
			//add current row to rowcache
			LinkedHashMap<String,String> nextrow = new LinkedHashMap<String,String>();
			for (int i=0; i<colnames.length; i++) {
				if (upcaseData)
					nextrow.put(colnames[i],flatpackDataset.getString(colnames[i]).toUpperCase());
				else 
					nextrow.put(colnames[i],flatpackDataset.getString(colnames[i]));
			}
			rowcache.add(nextrow);
			currentcachepointer++;
		}
		if (checkID) {
			String newID = "";
			try {
				for (int i=0; i<idfields.length; i++) 
					newID += idfields[i].getFinalValue(this.getValue(idfields[i].getField()));
			} catch (Exception e) {
				newID="";
			}
			if (newID=="") {
				hasRow = false;
				throw new Exception("Keine ID in File " + this.datentyp + ", Zeile "+flatpackDataset.getRowNo()+". Zeile wird ignoriert.");
			}
			if (currentcachepointer>1) {
				//refresh cache, if
				// a) newID <> currentID <> currentCachedID 
				// b) newID == currentID && newID <> currentCachedID 
				// -> i.e. flush all but last entries
				if (!currentCachedID.isEmpty() && !newID.equals(currentCachedID) && !currentID.equals(currentCachedID)) {
					LinkedHashMap<String,String> last = rowcache.get(rowcache.size()-1);
					LinkedHashMap<String,String> lastbutone = rowcache.get(rowcache.size()-2);
					this.clearcache();
					rowcache.add(lastbutone);
					rowcache.add(last);
					currentcachepointer=2;
				}
			}
			currentCachedID=currentID;
			currentID=newID;
		}
		return hasRow;
	}
	
	public boolean nextRow() throws Exception {
		return this.nextRow(false, false);
	}
	
	/**
	 * Warps back to beginning of last ID, to repeat readin
	 * alternatively warps forward until correct ID is reached
	 *
	 * @return true, if successful
	 */
	public void warpToCorrectID (String warpID) throws Exception {
		if (warpID.equals(currentCachedID)) {
			currentcachepointer=0;
			inWarpMode=true;
			currentID=currentCachedID;
			//set pointer to correct row, no checkid required
			this.nextRow(false,true); 
		} else {
			//warp forward until ID is equal or bigger (i.e. later), check ID but do not cache
			while (this.currentID.compareTo(warpID)<0 && this.nextRow(true,false)) {}
		}
	}
	
	/**
	 * Gets the value.
	 *
	 * @param field the field
	 * @return the value
	 */
	public String getValue (String field) {
		return rowcache.get(currentcachepointer-1).get(field);
	}
	
	public String getValueLastCached (String field) {
		return rowcache.get(currentcachepointer-2).get(field);
	}
	
	public String getID() {
		return currentID;
	}
	
	//returns fields in one String, Komma-separated
	public String getIDFields() {
		String fields="";
		for (int i=0; i<idfields.length; i++) {
			if (i==0) fields=idfields[i].getField();
			else fields+=","+idfields[i].getField();
		}
		return fields;
	}
	
	/**
	 * Checks if is datentyp.
	 *
	 * @param datentyp the datentyp
	 * @return true, if is datentyp
	 */
	public boolean isDatentyp (String datentyp) {
		return this.datentyp.equals(datentyp);
	}
	
	/**
	 * Gets the datentyp.
	 *
	 * @return the datentyp
	 */
	public String getDatentyp () {
		return this.datentyp;
	}
	
	/**
	 * Gets the path.
	 *
	 * @return the path
	 */
	public String getPath () {
		return this.path;
	}
	
	/**
	 * Gets the colnames.
	 *
	 * @return the colnames
	 */
	public String[] getColnames () {
		return this.colnames;
	}
	
	/**
	 * Checks for row.
	 *
	 * @return true, if successful
	 */
	public boolean hasRow () {
		return hasRow;
	}
	
	
	public boolean isLeader() {
		return isLeader;
	}

	public void setLeader(boolean isLeader) {
		this.isLeader = isLeader;
		this.hasLeaderCols = true;
		this.leaderColnames = colnames;
		this.hasLeaderNumfield = false;
	}
	
	public void setLeaderNumfield (String numfield) {
		boolean exists = false;
		numfield = numfield.toUpperCase();
		for(int i=0; i<colnames.length; i++) {
			if (colnames[i].equals(numfield)) { exists=true; break; }
		}
		if (exists) {
			hasLeaderNumfield=true;
			leaderNumfield=numfield;
		}
	}
	
	public void setLeaderColnames (String[] cols) {
		this.leaderColnames = null; //reset leadercolnames
		this.hasLeaderCols=false;
		if (cols.length > 0 && !cols[0].equals("")) {
			ArrayList<String> addcols= new ArrayList<String>();
			for(int i=0; i<cols.length; i++) {
				cols[i]=cols[i].toUpperCase();
				for(int j=0; j<this.colnames.length; j++) {
					if (this.colnames[j].equals(cols[i])) { addcols.add(cols[i]); break; }
				}
			}
			if (addcols.size()>0) {
				this.leaderColnames = addcols.toArray(new String[addcols.size()]);
				this.hasLeaderCols=true;
			}
		}
	}
	
	public boolean hasLeaderCols () {
		return hasLeaderCols;
	}
	
	public String[] getLeaderColnames () {
		return leaderColnames;
	}
	
	public boolean hasLeaderNumfield () {
		return hasLeaderNumfield;
	}
	
	public String getLeaderNumfield () {
		return leaderNumfield;
	}
	
	
	public void clearcache () {
		rowcache.clear();
		currentcachepointer=0;
	}

	/**
	 * Close.
	 *
	 * @throws Exception the exception
	 */
	public void close () throws Exception {
		if (filetype.equals(Consts.satzartFlag)) {
			fixparse.close();
		} else {
			csvparse.close();
		}
	}
	
}