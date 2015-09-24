package models;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.util.HashMap;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Sorts using Sqlite.
 *
 * @author HellwigP
 */

public class Sorter extends InputFile {
	
	/** The sqldb. */
	private Connection sqldb;
	
	/** The sortedfileext. */
	private String sortedfileext = ".sorted.csv";
	
	/** The dbfile. */
	private String dbfile;
	
	
	/**
	 * Instantiates a new sorter.
	 *
	 * @param datentyp the datentyp
	 * @param path the path
	 * @param filetype the filetype
	 * @param tmppath the tmppath
	 * @throws Exception the exception
	 */
	public Sorter(String datentyp, String path, String filetype, String[] idfields, String tmppath) throws Exception {
		super(datentyp, path, filetype, idfields);
		//use "in memory" if possible
		long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
		File me = new File(path);
        long filelength = me.length();
		if (presumableFreeMemory > (filelength+filelength/5)) { //memory must be filesize + ~20%
			dbfile = ":memory:";
		} else {
			dbfile = tmppath+"/sorter.db";
			File file = new File(dbfile);
			if (file.exists()) { file.delete(); };
		}
		Class.forName("org.sqlite.JDBC");
		sqldb = DriverManager.getConnection("jdbc:sqlite:"+dbfile);
	}

	/**
	 * reads CSV into database. Adds a new column if needed
	 *
	 * @param sortfield the sortfield
	 * @return String newFile (incl. path) (=csv)
	 * @throws Exception the exception
	 */
	public String sortFileByID(boolean addColumn, String sourcecol, String targetcol, HashMap<String,String> translator) throws Exception {
		Statement stmt = sqldb.createStatement();
		//1. create table with headers and one hash information
		String createSQL = "create table sortdb (";
		String insertSQL = "insert into sortdb values(";
		String [] headerline = getColnames();
		int colcount = headerline.length;
		for (int i=0; i< colcount-1; i++) {
			createSQL +=  "'"+headerline[i] + "' TEXT, ";
			insertSQL += "?,";
		}
		if (addColumn) {
			insertSQL += "?,?);";
			createSQL +=  "'" + headerline[colcount-1] + "' TEXT, ";
			createSQL +=  "'"+targetcol + "' TEXT);";
		} else {
			createSQL +=  "'" + headerline[colcount-1] + "' TEXT);";
			insertSQL +=  "?);";
		}
		stmt.executeUpdate(createSQL);
		stmt.close();
		//2. read and import csv data
		PreparedStatement prep = sqldb.prepareStatement(insertSQL);
		String targetcolval = null;
		while (this.nextRow(false)) {
			for (int i=0; i<50000;i++) {
				if (this.hasRow()) {
					for (int j=0; j<colcount; j++) {
				        	prep.setString(j+1, this.getValue(headerline[j])); //i+1 as statements start with 1
				        	if (addColumn && headerline[j].equals(sourcecol)) {
				        		targetcolval=translator.get(this.getValue(headerline[j]));
				        	}
				    }
					if (addColumn) if (targetcolval != null) prep.setString(colcount+1,targetcolval); else prep.setString(colcount+1,"");
				    prep.addBatch();
				    this.nextRow(false);
				} else {
					break;
				}
			}
			sqldb.setAutoCommit(false);
			prep.executeBatch();
			sqldb.commit();
		}
		this.close();
		//create index -> helps sorting
		stmt = sqldb.createStatement();
		stmt.executeUpdate("create index sort_id on sortdb ("+ this.getIDFields() +");");
		stmt.close();   
		//3. dump db sorted
		CSVWriter outputfile = new CSVWriter(new FileWriter(this.getPath()+sortedfileext), ';', CSVWriter.NO_QUOTE_CHARACTER);
		stmt = sqldb.createStatement();
	    ResultSet orderedTable = stmt.executeQuery( "SELECT * FROM sortdb order by "+ this.getIDFields() + ";" );
	    outputfile.writeAll(orderedTable, true);
	    outputfile.close();
	    stmt.close();
	    sqldb.close();
	    //Cleanup: Delete file
	    if (!dbfile.equals(":memory:")) {
		    File file = new File(dbfile);
		    file.delete();
	    }
	    return this.getPath()+sortedfileext;
	}	
	
	//without adding a new column
	public String sortFileByID() throws Exception {
		return this.sortFileByID(false, null, null, null);
	}

}
