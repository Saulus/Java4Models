package utilities;

import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;



import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import configuration.Consts;



class Column {
	public String key;
	public String value;
	
	Column(String key, String value) throws NumberFormatException {
		int test = Integer.parseInt(key); //test that key is numeric
		this.key=key;
		this.value=value;
	}
}

public class MatlabToSVMlight {

	public static void main(String[] args) {
		// 0. Get Configs
		// Inputfiles
		// modelpath
		// Outputfile
		if (args.length != 2) {
			System.err.println("Aufruf: java -jar MatlabToSVMlight.jar quelle ziel");
			System.exit(1);
		}
		if (args[0].equals("--version")) {
			System.out.println("Java4Models. Elsevier Health Analytics. Version: " + Consts.version);
			System.exit(1);
		}
		String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp + " Starte Transformation von " + args[0] + " nach " + args[1]);
		try {
			transform(args[0],args[1]);
		} catch (Exception e) {
			System.err.println("Fehler beim Schreiben oder Lesen von " + args[0] + " oder " + args[1] + ".");
			e.printStackTrace();
			System.exit(1);
		}
		timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp + " Transformation beendet von " + args[0] + " nach " + args[1]);
	}
	
	private static void transform(String readfile, String writefile) throws Exception {
		CSVReader reader = null;
		CSVWriter writer = null;
		reader = new CSVReader(new FileReader(readfile), ';', '"');
		writer = new CSVWriter(new FileWriter(writefile), ' ', CSVWriter.NO_QUOTE_CHARACTER);
		String[] newline1 = {"#Profile features in svmlight format, see http://svmlight.joachims.org (targets always 1, pids in #info part)"};
		writer.writeNext(newline1);
		reader.readNext();  //header
		String [] row;
		String currentid= null;
		ArrayList<Column> profValues = new ArrayList<Column>();
		Column col;
		while ((row = reader.readNext()) != null) {
			if (currentid == null || row[0].equals(currentid)) {
				col = new Column(row[1],row[2]);
				profValues.add(col);
			} else {
				writeSvmlighRow(writer,profValues,currentid);
				profValues = new ArrayList<Column>();
				col = new Column(row[1],row[2]);
				profValues.add(col);
			}
			currentid=row[0];
		}
		reader.close();
		writer.close();
	}
	
	private static void writeSvmlighRow(CSVWriter file, ArrayList<Column> profValues, String rowInfo) {
		//output: "1 column(=VariableNo.):value #PID"
		ArrayList<String> newline = new ArrayList<String>();
		newline.add("1");
		for (Column col : profValues) {
			if (!col.key.equals(Consts.navalue))
				newline.add(col.key + ":" + col.value);
		}
		newline.add("# "+rowInfo);
		file.writeNext(newline.toArray(new String[newline.size()]));
	}
}
