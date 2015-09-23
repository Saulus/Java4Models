package models;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.Days;

import au.com.bytecode.opencsv.CSVReader;

import configuration.Consts;
import configuration.Konfiguration;
import configuration.Utils;



class ModelVariableCalcPart {
	private boolean needsCol = false;
	private boolean needsVariable= false;
	private int refColnumber; //starting with 0 
	private String refVariable;
	private double value;
	private LocalDate mydate;
	private LocalDate myreferencedate;
	
	private boolean isConstant = false; //
	private boolean isDate;
	
	
	
	public ModelVariableCalcPart (String type, String value) throws Exception {
			if (Konfiguration.reference_date == null) 
				myreferencedate=Utils.parseDate(Consts.reference_date);
			else myreferencedate=Utils.parseDate(Konfiguration.reference_date);
			parseType(type);
			parseValue(value);
	}
	
	private void parseType (String type) {
		//isConstant = (type.equals(Consts.aggValue));
		isDate= (type.equals(Consts.aggDate));
	}
	
	private void parseValue (String value) throws Exception {
		if (value.startsWith(Consts.reference)) {
			try { 
				//Is Integer -> refers to column
				refColnumber = Integer.parseInt(value.substring(1))-1; 
				needsCol=true;
			} catch (Exception e) {
				//is no integer -> take as variable
				refVariable = value.substring(1);
				needsVariable=true;
			}
		} else {
			//test if value contains "," -> change to "."
			String newvalue = value.replace(",", ".");
			this.value=Double.parseDouble(newvalue); 
			isConstant = true;
		}
	}
	
	
	public boolean needsCol() {
		return needsCol;
	}
	
	public boolean needsVariable() {
		return needsVariable;
	}
	
	public int getColnumber() {
		return refColnumber;
	}
	
	public String getRefVariable() {
		return refVariable;
	}
	
	
	//used for column input -> parse
	public double getValue(String inputvalue) {
		double newval;
		//parse inputvalue & calc
		if (isDate) {
			mydate = Utils.parseDate(inputvalue);
			newval = Days.daysBetween(myreferencedate, mydate).getDays();
		} else 
			try {
				//test if value contains "," -> change to "."
				String myval = inputvalue.replace(",", ".");
				newval = Double.parseDouble(myval);
			} catch (Exception e) { newval = 1; } 
		return newval;
	}
	
	//used for input from other variable
	public double getValue(double inputvalue) {
		if (isConstant) return value;
		else return inputvalue;
	}
	
	public double getValue() {
		return value;
	}
	
	public boolean isConstant() {
		return isConstant;
	}
	
	public boolean isDate() {
		return isDate;
	}
	
	
	
}


class ModelVariableCalc {
	private List <ModelVariableCalcPart> plusParts= new ArrayList<ModelVariableCalcPart>();
	private List <ModelVariableCalcPart> minusParts= new ArrayList<ModelVariableCalcPart>();
	boolean isStd = false; //only 1
	private boolean dependsOnOtherVar = false;
	private List<String> otherVars = new ArrayList<String>();
	
	
	public ModelVariableCalc(String calculation) throws Exception {
		//Parse Otherfieldfilter
		
		if (!calculation.equals("")) {
			//Split "+", then split "-" 
			String[] plustokens = calculation.split("\\+");
			String[] minustokens;
			String[] parts;
			
			for (int i=0; i<plustokens.length; i++) {
				//split again by "-"
				minustokens = plustokens[i].split("\\-");
				//now: 1st is +, following are -
				for (int j=0; j<minustokens.length; j++) {
					//look for ()
					parts = minustokens[j].split(Consts.bracketEsc);
					ModelVariableCalcPart p = new ModelVariableCalcPart(parts[0],parts[1]);
					if (p.needsVariable()) {
						dependsOnOtherVar=true;
						otherVars.add(p.getRefVariable());
					}
					if (j==0) plusParts.add(p);
					else minusParts.add(p);
				}
			}
		} else isStd=true;
	}
	
	public boolean dependsOnOtherVar() {
		return dependsOnOtherVar;
	}
	
	public List<String> getOtherVarsDependent () {
		return otherVars;
	}
	
	public double getValue (String[] columnvalues, HashMap<String,Variable> vars) {
		if (isStd) return 1;
		else {
			double myval = 0;
			double addval = 0;
			for (ModelVariableCalcPart p : plusParts) {
				if (p.needsCol()) addval=p.getValue(columnvalues[p.getColnumber()]);
				else if (p.needsVariable()) {
					if (vars != null)
						addval=p.getValue(vars.get(p.getRefVariable()).getProfvalue());
					else addval=0;
				}
				else addval=p.getValue();
				myval = myval +addval;
			}
			for (ModelVariableCalcPart p : minusParts) {
				if (p.needsCol()) addval=p.getValue(columnvalues[p.getColnumber()]);
				else if (p.needsVariable()) {
					if (vars != null)
						addval=p.getValue(vars.get(p.getRefVariable()).getProfvalue());
					else addval=0;
				}
				else addval=p.getValue();
				myval = myval - addval;
			}
			return myval;
		}
	}
	
}

class ModelVariableCols {
	private String column; 
	private int pos_min = 0;
	private int pos_max = 10000; //dummy value
	private String filterstart = "";
	private HashSet<String> filtervalues = null; //if read in from file
	private String filterend = "";
	
	public ModelVariableCols (String col, String filter, Konfiguration config) throws Exception {
		parseCol(col);
		parseFilter(filter,config);
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
	
	private void parseFilter (String filter, Konfiguration config) throws Exception {
		if (!filter.equals("")) {
			//1: test whether File as in (File)
			if (filter.startsWith("(")) {
				String[] parts = filter.split(Consts.bracketEsc);
				CSVReader reader = new CSVReader(new FileReader(config.getModelpath() + Consts.filterfilelocation  + "\\" + parts[1]), ';', '"');
				List<String[]> myEntries = reader.readAll();
				reader.close();
				filtervalues = new HashSet<String>();
				for (String[] nextline : myEntries) {
					filtervalues.add(nextline[0]);
				}
			} else {
				String[] tokens = filter.split("-");
				filterstart=tokens[0];
				if (tokens.length>1) filterend=tokens[1]; else filterend = filterstart;
			}
		}
	}
	
	public String getColumn() {
		return column;
	}
	
	public int getMinPos () {
		return pos_min;
	}
	
	public int getMaxPos () {
		return pos_max;
	}
	
	public boolean isAllowed(String value) {
		if (filtervalues == null) //i.e. no list from file
			return ((filterstart.equals("") ||  value.compareTo(this.filterstart)>=0) // value >= firstvalue 
					&& (filterend.equals("") ||  value.compareTo(this.filterend)<=0)); //value <= filterend
		else //list from file
			return filtervalues.contains(value);
	}
}



class ModelVariableAgg {
	private boolean isSum;
	private boolean isCount;
	private boolean isMean;
	private boolean isMin;
	private boolean isMax;
	private boolean isMaxDistance;
	private boolean isOccurence;
	private boolean isStd;
	
	
	
	public ModelVariableAgg (String type) throws Exception {
		if (type.equals("")) this.isStd=true;
		else {
			this.parseType(type);
			this.isStd=this.isOccurence;
		}
	}
	
	private void parseType (String type) {
		isSum= (type.equals(Consts.aggSum));
		isCount= (type.equals(Consts.aggCount));
		isMean= (type.equals(Consts.aggMean));
		isMin= (type.equals(Consts.aggMin));
		isMax= (type.equals(Consts.aggMax));
		isMaxDistance= (type.equals(Consts.aggMaxDistance));
		isOccurence= (type.equals(Consts.aggOccurence));
	}
	
	public double aggregateValues (List<Double> values) {
		double myval=0;
		if (isOccurence || isStd) return 1;
		if (isSum) {
			for (double d: values) myval+=d;
			//needs rounding.... double not precise; gives not exactly correct numbers
			myval = myval*100;
			myval = Math.round(myval);
			myval = myval /100;
			return myval; 
		}
		if (isCount) return values.size();
		if (isMean) {
			for (double d: values) myval+=d;
			//needs rounding.... double not precise; gives not exactly correct numbers
			myval = myval*100/values.size();
			myval = Math.round(myval);
			myval = myval /100;
			return myval; 
		}
		if (isMin) {
			myval=1000000;
			for (double d: values) myval = Math.min(myval, d);
			return myval;
		}
		if (isMax) {
			myval=-1000000;
			for (double d: values) myval = Math.max(myval,d);
			return myval;
		}
		if (isMaxDistance) {
			//find min and max
			double myvalmin=1000000;
			double myvalmax=-1000000;
			for (double d: values) {
				myvalmin = Math.min(myvalmin, d);
				myvalmax = Math.max(myvalmax, d);
			}
			return myvalmax-myvalmin+1; //returns 1 if myvalmax=myvalmin (to separate from 0)
		}
		return myval;
	}
	
	public boolean isStd() {
		return isStd;
	}
}

/**
 * The Class ModelField.
 * Represents one field in an input file, e.g. ICD_CODE
 * Is valid for one model
 * 
 * Holds a list of filters for the field
 * Holds the aggregation type from Aggregation in Model.config 
 *  (e.g. empty, i.e. standard; see Consts for values allowed)
 * 	--> Only one aggregation type per field (per model per inputfile) allowed; last one is used 
 * 		(technical reason)
 *  
 */
public class ModelVariable {
	private String[] namePrefixes;
	private int[] nameColumnNumbers;
	private ModelVariableCols[] cols;
	private ModelVariableCalc calc;
	private ModelVariableAgg agg;
	private double filterstart = -1;
	private double filterend = -1;
	private boolean include; //if true: only include patients that have this
	private boolean exclude; //if true: exclude all patients that have this
	private boolean target; //if target: print separately, do not include in coeff-calculation
	private boolean hideme; //if true: do not print var
	
	public ModelVariable (ModelVariableReadIn readvar, Model mymodel, Konfiguration config) throws ModelConfigException{
		String[] tokens;
		//parse variable name
		try {
			tokens = readvar.getVariableCol().split(Consts.referenceEsc);
			namePrefixes = new String[tokens.length];
			nameColumnNumbers = new int[tokens.length-1];
			for (int i=0; i<tokens.length; i++) {
				if (i==0) namePrefixes[i]= tokens[i];
					else namePrefixes[i]= tokens[i].substring(1);
				if (i<tokens.length-1) nameColumnNumbers[i]=Integer.parseInt(tokens[i+1].substring(0,1))-1;
			}
		} catch (Exception e) {
			throw new ModelConfigException("Fehler bei Variable "+  readvar.getVariableCol() + ": "+ Consts.modVariableCol + " nicht lesbar",e); 
		}
		//parse var filter
		try {
			parseFilter(readvar.getFilterCol());
		} catch (Exception e) {
			throw new ModelConfigException("Fehler bei Variable "+  readvar.getVariableCol() + ": "+ Consts.modFilterCol + " nicht lesbar",e); 
		}
		//parse calculation
		try {
			this.calc = new ModelVariableCalc(readvar.getCalcCol());
		} catch (Exception e) {
			throw new ModelConfigException("Fehler bei Variable "+  readvar.getVariableCol() + ": "+ Consts.modCalcCol + " nicht lesbar",e); 
		}
		//parse aggregation
		try {
			this.agg = new ModelVariableAgg(readvar.getAggCol());
		} catch (Exception e) {
			throw new ModelConfigException("Fehler bei Variable "+  readvar.getVariableCol() + ": "+ Consts.modAggCol + " nicht lesbar",e); 
		}
		//parse rest
		this.include=Consts.wahr.equals(readvar.getIncludeCol()); if (this.include) mymodel.setInclusion(true);
		this.exclude=Consts.wahr.equals(readvar.getExcludeCol()); if (this.exclude) mymodel.setExclusion(true);
		this.target=Consts.wahr.equals(readvar.getTargetCol()); if (this.target) mymodel.setHasTargets(true);
		this.hideme=Consts.wahr.equals(readvar.getHideCol());
		//parse columns + their filters
		int number = 0;
		try {
			cols = new ModelVariableCols[readvar.getColumns().length]; 
			for (int i=0; i<readvar.getColumns().length; i++) {
				number=i+1;
				cols[i] = new ModelVariableCols(readvar.getColumns()[i],readvar.getFilters()[i],config);	
			}
		} catch (Exception e) {
			throw new ModelConfigException("Fehler bei Variable "+  readvar.getVariableCol() + ": " + Consts.modColumnCol + number + " bzw. " + Consts.modColfilterCol + number + " nicht lesbar",e); 
		}
	}
	
	private void parseFilter (String filter) throws Exception {
		if (!filter.equals("")) {
			String[] tokens = filter.split("-");

			if (!tokens[0].equals("")) filterstart=Double.parseDouble(tokens[0]);
				else filterstart=-1;
			if (tokens.length>1) {
				if (!tokens[1].equals("")) filterend=Double.parseDouble(tokens[1]);
				 else filterend=-1;
			} else filterend=filterstart;
		}
	}
	
	/*
	 * get column values, substrings
	 * returns null, if row is not allowed
	 */
	public String[] getColumnValues (InputFile inputrow) {
		String[] myCols = new String[cols.length];
		int mymax;
		String myval;
		for (int i=0; i<myCols.length;i++) {
			//column definition in config file might be empty
			if (cols[i].getColumn()!= null ) {
				//get value from inputrow
				myval = inputrow.getValue(cols[i].getColumn());
				//if value is empty -> return null as well
				if (myval.equals("")) return null;
				//if value is shorter than filter -> return null
				if (cols[i].getMinPos()>=myval.length()) return null;
				//substring (max length)
				mymax=Math.min(cols[i].getMaxPos(), myval.length());
				myval =myval.substring(cols[i].getMinPos(), mymax);
				//if value is not allowed due to filter -> return null
				if (!cols[i].isAllowed(myval)) return null; 
				myCols[i]=myval;
			}
		}
		return myCols;
	}
	
	public String getName (String[] columns) {
		String myname = "";
		for (int i=0; i<nameColumnNumbers.length; i++) {
			myname=myname + namePrefixes[i] + columns[nameColumnNumbers[i]];
		}
		myname=myname + namePrefixes[namePrefixes.length-1];
		return myname;
	}

	
	public boolean varIsAllowed (double value) {
		//simply parse to string 
		return ((filterstart==-1  ||  value >= filterstart ) // value >= filterstart 
				&& (filterend==-1  ||  value <= filterend)); //value <= filterend
	}
	
	public boolean isInclude () {
		return include;
	}
	
	public boolean isExclude () {
		return exclude;
	}
	
	public boolean isTarget () {
		return target;
	}
	
	
	public boolean hideme () {
		return hideme;
	}
	
	public boolean dependsOnOtherVar () {
		return calc.dependsOnOtherVar();
	}
	
	public List<String> getOtherVarsDependent () {
		return calc.getOtherVarsDependent();
	}
	
	public boolean isStdAgg () {
		return agg.isStd();
	}
	
	public double getValue (String[] columnvalues, HashMap<String,Variable> vars) {
		return this.calc.getValue(columnvalues, vars);
	}
	
	public double aggregateValues (List<Double> values) {
		return this.agg.aggregateValues(values);
	}
	
	
}