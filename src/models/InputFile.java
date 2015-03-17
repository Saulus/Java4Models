package models;

import java.util.Arrays;
import java.io.FileReader;
import java.io.StringReader;

import configuration.Consts;
import configuration.FileDefinitions;
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
	
	/** The fixparse. */
	private BuffReaderFixedParser fixparse = null;
	
	/** The csvparse. */
	private BuffReaderDelimParser csvparse = null;
	
	/** The my dataset. */
	private DataSet myDataset;
	
	/** The col names. */
	private String[] colNames = null; 
	
	/** The has row. */
	private boolean hasRow = false; 
	
	/**
	 * Instantiates a new input file.
	 *
	 * @param datentyp the datentyp
	 * @param path the path
	 * @param filetype the filetype
	 * @throws Exception the exception
	 */
	public InputFile (String datentyp, String path, String filetype) throws Exception {
		//ToDo: Eigene Reader-Classe als Wrapper für z.B. flatpack, csvreader usw.
		this.datentyp = datentyp;
		this.filetype = filetype;
		this.path = path;
		if (filetype.equals(Consts.satzartFlag)) {
				FileDefinitions filedef = new FileDefinitions();
				String myDef = filedef.getDefinition(datentyp);
				fixparse = (BuffReaderFixedParser) BuffReaderParseFactory.getInstance().newFixedLengthParser(new StringReader(myDef), new FileReader(path));
				fixparse.setIgnoreExtraColumns(true); //ignores extra characters in lines that are too long; lines that are too short are ignored (i.e. first line)
				//fixparse.setStoreRawDataToDataSet(true); //for Testing only
				myDataset = fixparse.parse();
				colNames = myDataset.getColumns();
		} else {
				//Issue: Flatpack will not give back CSV column name correctly, when BuffReaderDelimParser is used (getValue works fine)
				//Workaround: Open csv beforehand and read first line
					CSVReader reader = new CSVReader(new FileReader(path), ';', '"');
					String [] firstLine;
					if ((firstLine = reader.readNext()) != null) {
						colNames = firstLine;  
					}
					reader.close();
					//make Uppercase
					for(int i=0; i<colNames.length; i++) {
						colNames[i]=colNames[i].toUpperCase();
					}
				csvparse = (BuffReaderDelimParser) BuffReaderParseFactory.getInstance().newDelimitedParser(new FileReader(path),';','"');
				//csvparse.setStoreRawDataToDataSet(true);//for Testing only
				myDataset = csvparse.parse();
				//not working:
				//colNames = myDataset.getColumns();
		}
		
		if (myDataset.getErrors() != null && !myDataset.getErrors().isEmpty()) {
	            System.out.println("Fehler gefunden beim Einlesen von " + path);
	            for (int i = 0; i < myDataset.getErrors().size(); i++) {
	                final DataError de = (DataError) myDataset.getErrors().get(i);
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
		return Arrays.asList(colNames).contains(field);
	}
	
	/**
	 * Next row.
	 *
	 * @return true, if successful
	 */
	public boolean nextRow () {
		hasRow = myDataset.next();
		return hasRow;
	}
	
	/**
	 * Gets the value.
	 *
	 * @param field the field
	 * @return the value
	 */
	public String getValue (String field) {
		return myDataset.getString(field);
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
		return this.colNames;
	}
	
	/**
	 * Checks for row.
	 *
	 * @return true, if successful
	 */
	public boolean hasRow () {
		return hasRow;
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