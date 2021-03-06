package models;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

import au.com.bytecode.opencsv.CSVWriter;
import configuration.Consts;
import configuration.Filter;

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
	
	private char separator = Consts.fieldcombineseparator.charAt(0);
	private char quote =  CSVWriter.NO_QUOTE_CHARACTER;
	
	
	/**
	 * Instantiates a new sorter.
	 *
	 * @param data_id the data_id
	 * @param path the path
	 * @param filetype the filetype
	 * @param tmppath the tmppath
	 * @throws Exception the exception
	 */
	public Sorter(String datentyp, String path, String filetype, String[] idfields,  char separator, char quote, String tmppath, ArrayList<Filter> filters) throws Exception {
		super(datentyp, path, filetype, idfields,separator,quote,false,filters);
		//use "in memory" if possible
		long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
		File me = new File(path);
        long filelength = me.length();
		if (presumableFreeMemory > (filelength+filelength/2)) { //memory must be filesize + ~50%
			dbfile = ":memory:";
		} else {
			dbfile = tmppath+"/sorter.db";
			File file = new File(dbfile);
			if (file.exists()) { file.delete(); };
		}
		Class.forName("org.sqlite.JDBC");
		sqldb = DriverManager.getConnection("jdbc:sqlite:"+dbfile);
		this.separator=separator;
		if (quote == Character.MIN_VALUE)
			this.quote=CSVWriter.NO_QUOTE_CHARACTER;
		else this.quote=quote;
	}

	/**
	 * reads CSV into database. Adds a new column if needed
	 *
	 * @param sortfield the sortfield
	 * @return String newFile (incl. path) (=csv)
	 * @throws Exception the exception
	 */
	public String sortFileByID(boolean addColumn, String sourcecol, String targetcols[], HashMap<String,String[]> translator,boolean makeInt2Cols) throws Exception {
		Statement stmt = sqldb.createStatement();
		//1. create table with headers and one hash information
		String createSQL = "create table sortdb (";
		String insertSQL = "insert into sortdb values(";
		String [] headerline = getColnames();
		int colcount = headerline.length;
		for (int i=0; i< colcount-1; i++) {
			createSQL +=  "'"+headerline[i];
			if (i<2 && makeInt2Cols) createSQL += "' INTEGER, ";
			else createSQL += "' TEXT, ";
			insertSQL += "?,";
		}
		if (addColumn) {
			for (int i=0;i<targetcols.length; i++) insertSQL += "?,";
			insertSQL += "?);";
			createSQL +=  "'" + headerline[colcount-1] + "' TEXT, ";
			for (int i=0;i<targetcols.length-1; i++) createSQL +=  "'"+targetcols[i] + "' TEXT, ";
			createSQL +=  "'"+targetcols[targetcols.length-1] + "' TEXT);";
		} else {
			createSQL +=  "'" + headerline[colcount-1] + "' TEXT);";
			insertSQL +=  "?);";
		}
		stmt.executeUpdate(createSQL);
		stmt.close();
		//2. read and import csv data
		PreparedStatement prep = sqldb.prepareStatement(insertSQL);
		String[] targetcolval = null;
		int max_buffer = 50000;
		while (this.nextRow()) {
			for (int i=1; i<=max_buffer;i++) {
				if (this.hasRow()) {
					for (int j=0; j<colcount; j++) {
							if (j<2 && makeInt2Cols)
								prep.setInt(j+1, Integer.parseInt(this.getValue(headerline[j]))); //i+1 as statements start with 1
							else prep.setString(j+1, this.getValue(headerline[j])); //i+1 as statements start with 1
				        	if (addColumn && headerline[j].equals(sourcecol)) {
				        		targetcolval=translator.get(this.getValue(headerline[j]));
				        	}
				    }
					if (addColumn) {
						for (int j=0;j<targetcols.length; j++) {
							if (targetcolval != null && targetcolval.length>j && targetcolval[j]!=null)
								prep.setString(colcount+j+1,targetcolval[j]);
							else prep.setString(colcount+j+1,"");
						}
					}
				    prep.addBatch();
				    if(i<max_buffer) this.nextRow();
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
		CSVWriter outputfile = new CSVWriter(new FileWriter(this.getPath()+sortedfileext), this.separator,this.quote);
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
	
	//without adding a new column, but do not make columns int
	public String sortFileByID() throws Exception {
		return this.sortFileByID(false, null, null, null, false);
	}
	
	public String sortFileByID(boolean makeInt2Cols ) throws Exception {
		return this.sortFileByID(false, null, null, null, makeInt2Cols);
	}
	
	public String sortFileByID(boolean addColumn, String sourcecol, String[] targetcols, HashMap<String,String[]> translator) throws Exception {
		return this.sortFileByID(addColumn,sourcecol,targetcols,translator,false);
	}

}
