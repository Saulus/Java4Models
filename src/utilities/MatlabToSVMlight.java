package utilities;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import configuration.Consts;
import models.Sorter;



class Column {
	public String key;
	public Integer key_num;
	public String value;
	
	Column(String key, String value) throws NumberFormatException {
		this.key_num=Integer.parseInt(key); //test that key is numeric
		this.key = key;
		this.value=value;
	}
}

class ColComparator implements Comparator<Column> {

	   public int compare(Column first, Column second) {
		   return first.key_num.compareTo(second.key_num);
	}
}

public class MatlabToSVMlight {
	
	private static ColComparator colComparator = new ColComparator();

	public static void main(String[] args) {
		// 0. Get Configs
		// Inputfiles
		// modelpath
		// Outputfile
		boolean ignore1 = false;
		boolean sort = false;
		boolean set1 = false;
		boolean warn = false;
		boolean addpid = false;
		boolean sortColOnly = false;
		String[] set1_xy = new String[0];
		String[] warn_xy = new String[0];
		String source;
		String target;
		if (args.length == 1) {
			if (args[0].equals("--version")) {
				System.out.println("Java4Models / MatlabToSVMlight. Elsevier Health Analytics. Version: " + Consts.version);
				System.exit(1);
			}
			
			if (args[0].equals("--help")) {
				System.out.println("MatlabToSVMlight.");
				System.out.println("Erwartet: ROW;COL;VAL -> csv mit header, \";\" als Separator");
				System.out.println("-sort: Sortieren Quell-Datei (ins gleiche Verzeichnis, by ROW, COL -> müssen beide numerisch sein). Optional.");
				System.out.println("-ignore1: Ignoriere 1. Spalte (wenn dort eine Zeilen-ID z.B. aus R gespeichert ist). Optional.");
				System.out.println("-set1[x,y]: Setze alle Variablen AUßER x und y (und weitere) auf 1 (0 werden ignoriert). Optional.");
				System.out.println("-addpid: Füge Kommentar mit PID zum Ende der Zeile hinzu in der Form: # PIDa. Optional.");
				System.out.println("-sortcolonly: Sortiert nur COL-Spalte (falls ROWs bereits sortiert). Optional.");
				System.out.println("-warn[x,y]: Warnt, wenn aufeinanderfolgende ROWs in den COLs x,y (und weitere) gleiche Werte haben. Optional.");
				System.exit(1);
			}
		}
		if (args.length < 2) {
			System.err.println("Aufruf: java -jar MatlabToSVMlight.jar [-sort] [-ignore1] [-set1[x,y]] [-addpid] [-noindex] quelle ziel");
			System.exit(1);
		}
		int source_argNo = 0;
		
		//Parse
		for (int i=0; i<args.length-2; i++) {
			if (args[i].equals("-ignore1")) {
				ignore1=true;
				source_argNo++;
			}
			if (args[i].equals("-sort")) {
				sort=true;
				source_argNo++;
			}
			if (args[i].equals("-addpid")) {
				addpid=true;
				source_argNo++;
			}
			if (args[i].substring(0, 5).equals("-set1")) {
				set1=true;
				source_argNo++;
				if (args[i].length()>6)
				set1_xy = args[i].substring(6,args[i].length()-1).split(",");
			}
			if (args[i].equals("-sortcolonly")) {
				sortColOnly=true;
				source_argNo++;
			}
			if (args[i].substring(0, 5).equals("-warn")) {
				warn=true;
				source_argNo++;
				if (args[i].length()>6)
					warn_xy = args[i].substring(6,args[i].length()-1).split(",");
			}
		}
		
		source=args[source_argNo];
		target=args[source_argNo+1];
		String timeStamp;
		//Sort by X
		if (sort) {
				timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
				System.out.println(timeStamp + " Starte Sortieren von " + source);
				//get path for temp file
				Path p = Paths.get(source);
				Path folder = p.getParent();
				String path;
				if (folder == null) path=System.getProperty("user.dir");
				else path=folder.toString();
				//get id field
			try {
				CSVReader reader = new CSVReader(new FileReader(source), ';', '"');
				String [] firstLine;
				String[] idfield = {"ROW","COL"};
				if ((firstLine = reader.readNext()) != null) {
					if (ignore1) {
						idfield[0]=firstLine[1];
						idfield[1]=firstLine[2];
					}
					else {
						idfield[0]=firstLine[0];
						idfield[1]=firstLine[1];
					}
				}
				reader.close();
				
				Sorter sortfile = new Sorter("matlab",source,Consts.csvFlag,idfield,path);
				source = sortfile.sortFileByID(true);
			} catch (Exception e) {
				System.err.println("Fehler beim Sortieren von " + source + " im Ordner " + path);
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		//Transform
		timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp + " Starte Transformation von " + source + " nach " + target);
		try {
			transform(source,target,ignore1,set1,set1_xy, addpid,sortColOnly,warn,warn_xy);
		} catch (Exception e) {
			System.err.println("Fehler beim Schreiben oder Lesen von " + source + " oder " + target + ".");
			e.printStackTrace();
			System.exit(1);
		}
		timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp + " Transformation beendet von " + source + " nach " + target);
	}
	
	private static void transform(String readfile, String writefile, boolean ignore1, boolean set1, String[] set1_cols, boolean addpid, boolean sortColOnly, boolean warn, String[] warn_cols) throws Exception {
		String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
		CSVReader reader = null;
		CSVWriter writer = null;
		reader = new CSVReader(new FileReader(readfile), ';', '"');
		writer = new CSVWriter(new FileWriter(writefile), ' ', CSVWriter.NO_QUOTE_CHARACTER);
		String[] newline1 = {"#Profile features in svmlight format, see http://svmlight.joachims.org (targets always 1, pids in #info part, if selected)"};
		writer.writeNext(newline1);
		reader.readNext();  //header
		String [] row;
		String currentid= null;
		ArrayList<Column> profValues = new ArrayList<Column>();
		Column col;
		int col_x = 0;
		int col_y = 1;
		int col_z = 2;
		if (ignore1) {
			col_x = 1;
			col_y = 2;
			col_z = 3;
		}
		boolean doset1;
		boolean dowarn_foundcols;
		boolean dowarn_foundvalues;
		String[] warn_col_values = new String[warn_cols.length];
		ArrayList<String> errorRows = new ArrayList<String>();
		long rowid = 0;
		while ((row = reader.readNext()) != null) {
			rowid++;
			if (currentid != null && !row[col_x].equals(currentid)) {
				//warn
				if (warn) {
					dowarn_foundcols=false;
					dowarn_foundvalues=true;
					//per warn column
					for (int x=0;x<warn_cols.length; x++) {
						//find correct col
						for (Column mycol : profValues) {
							if (mycol.key.equals(warn_cols[x])) {
								dowarn_foundcols=true;
								//first: compare to earlier value
								if (dowarn_foundvalues && !mycol.value.equals(warn_col_values[x])) dowarn_foundvalues=false; 
								//last: set new value for later reference
								warn_col_values[x] = mycol.value; 
								break;
							}
						}
					}
					if (dowarn_foundcols && dowarn_foundvalues) {
						System.err.println("Warnung: Gleiche Werte in Spalten in Row " + currentid);
						errorRows.add(currentid);
					}
				}
				writeSvmlighRow(writer,profValues,currentid, addpid,sortColOnly);
				profValues = new ArrayList<Column>();
			}
			col = new Column(row[col_y],row[col_z]);
			if (set1) {
				doset1=true;
				for (int x=0;x<set1_cols.length; x++) {
					if (col.key.equals(set1_cols[x])) { doset1=false; break; }
				}
				if (doset1 && !col.value.equals("0")) col.value="1";
			}
			profValues.add(col);
			currentid=row[col_x];
			if (rowid % 100000000 == 0) {
				timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
				System.out.println(timeStamp +" - "+ Long.toString(rowid) + " Rows verarbeitet (RowNo: "+currentid+").");
			}
		}
		writeSvmlighRow(writer,profValues,currentid, addpid, sortColOnly);
		timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp +" - "+ Long.toString(rowid) + " Rows verarbeitet (RowNo: "+currentid+").");
		if (warn && errorRows.size()>0) {
			for (String errorrow : errorRows) System.err.println("Error in row: " + errorrow);
		}
		reader.close();
		writer.close();
	}
	
	
	private static void writeSvmlighRow(CSVWriter file, ArrayList<Column> profValues, String rowInfo, boolean addpid, boolean sortColOnly) {
		//output: "1 column(=VariableNo.):value #PID"
		ArrayList<String> newline = new ArrayList<String>();
		newline.add("1");
		//Sort profValues by key (numerically!)
		if (sortColOnly)
			Collections.sort(profValues, colComparator);
		for (Column col : profValues) {
			if (!col.key.equals(Consts.navalue))
				newline.add(col.key + ":" + col.value);
		}
		if (addpid) newline.add("# "+rowInfo);
		file.writeNext(newline.toArray(new String[newline.size()]));
	}
}
