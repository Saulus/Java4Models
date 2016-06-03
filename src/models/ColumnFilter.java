package models;

import java.io.FileReader;
import java.util.HashSet;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import configuration.Consts;

class Criteria {
	public String start  = "";
	public HashSet<String> values = null; //if read in from file or comma-separated-list
	public String end = "";
	
	public Criteria() {
		
	}
	
	public boolean valueFits (String testvalue) {
		boolean fits=true;
		//test values
		if (values != null) {
			fits = values.contains(testvalue);
		}
		fits = fits && (start.equals("") ||  testvalue.compareTo(this.start)>=0); // value >= firstvalue
		fits = fits && (end.equals("") ||  testvalue.compareTo(this.end)<=0); // value <= filterend
		
		return fits;
	}
}

public class ColumnFilter {
	//private final static Logger LOGGER = Logger.getLogger(ColumnFilter.class.getName());
	
	private String column; 
	public int pos_min = 0;
	public int pos_max = 10000; //dummy value
	private Criteria inclusion = null;
	private Criteria exclusion = null;
	
	public ColumnFilter (String col, String inclusion, String exclusion) throws Exception {
		parseCol(col);
		if (inclusion!=null && !inclusion.isEmpty()) this.inclusion = parseFilter(inclusion);
		if (exclusion!=null && !exclusion.isEmpty()) this.exclusion = parseFilter(exclusion);
	}
	
	private void parseCol (String col) throws Exception {
		if (!col.equals("")) {
			String[] tokens = col.split(Consts.bracketEsc);
			
			this.column = tokens[0];
			//Parse Stringposition
			if (tokens.length>1) {
				if (tokens[1].contains("-")) {
					String [] positions = tokens[1].split("-");
					if (positions[0].equals("")) pos_min=0; //allow "-4"
					else pos_min = Integer.parseInt(positions[0])-1; //In Java Strings start from 0, but substring end counts +1
					if (positions[1].equals("")) pos_max=15000; //allow "4-"
					else pos_max = Integer.parseInt(positions[1]); //In Java Strings start from 0, but substring end counts +1
				} else {
					pos_min = Integer.parseInt(tokens[1])-1;
					pos_max = pos_min+1;
				}
			}
		}
	}
	
	private Criteria parseFilter (String filter) throws Exception {
		Criteria filtercrit = new Criteria();
		if (!filter.equals("")) {
			//1: test whether File as in (File)
			if (filter.startsWith("(")) {
				String[] parts = filter.split(Consts.bracketEsc);
				CSVReader reader = new CSVReader(new FileReader(parts[1]), ';', '"');
				List<String[]> myEntries = reader.readAll();
				reader.close();
				filtercrit.values = new HashSet<String>();
				for (String[] nextline : myEntries) {
					filtercrit.values.add(nextline[0]);
				}
			} else {
				//now: comma-separated or range?
				if (filter.contains(Consts.idfieldseparator)) {
					String[] tokens = filter.split(",");
					filtercrit.values = new HashSet<String>();
					for (String token : tokens) {
						filtercrit.values.add(token);
					}
				} else {
					String[] tokens = filter.split("-");
					filtercrit.start=tokens[0];
					if (tokens.length>1) filtercrit.end=tokens[1]; else filtercrit.end = filtercrit.start;
				}
			}
		}
		
		return filtercrit;
	}
	
	public String getColumn() {
		return column;
	}
	
	public String getValueSubstring (String value) {
		//if value is empty -> return null as well
		if (value.equals("")) return null;
		//if value is shorter than filter -> return null
		if (pos_min>=value.length()) return null;
		//substring (max length)
		int mymax=Math.min(pos_max, value.length());
		return value.substring(pos_min, mymax);
	}
	
	
	
	public boolean isAllowed(String value) {
		if (value==null) return false;
		boolean isAllowed = (inclusion==null) || inclusion.valueFits(value);
		isAllowed = isAllowed && ((exclusion==null) || !exclusion.valueFits(value));
		return isAllowed;
	}
}
