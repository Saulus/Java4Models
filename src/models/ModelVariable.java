package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.Years;
import org.joda.time.Days;

import configuration.Consts;
import configuration.Configuration;
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
	private boolean isAgeFromDate;
	
	
	
	public ModelVariableCalcPart (String type, String value) throws Exception {
			myreferencedate=Configuration.getReferenceDate();
			parseType(type);
			parseValue(value);
	}
	
	private void parseType (String type) {
		//isConstant = (type.equals(Consts.aggValue));
		isDate= (type.equals(Consts.aggDate));
		isAgeFromDate= (type.equals(Consts.aggAge));
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
	
	
	//used for column inputfiles -> parse
	public double getValue(String inputvalue) {
		double newval;
		//parse inputvalue & calc
		if (isDate) {
			mydate = Utils.parseDate(inputvalue);
			newval = Days.daysBetween(myreferencedate, mydate).getDays();
		} else if (isAgeFromDate) {
			mydate = Utils.parseDate(inputvalue);
			newval = Years.yearsBetween(mydate,myreferencedate).getYears();
		} else 
			try {
				//test if value contains "," -> change to "."
				String myval = inputvalue.replace(",", ".");
				newval = Double.parseDouble(myval);
			} catch (Exception e) { newval = 1; } 
		return newval;
	}
	
	//used for inputfiles from other variable
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
				if (p.needsCol() && columnvalues!=null) addval=p.getValue(columnvalues[p.getColnumber()]);
				else if (p.needsVariable()) {
					if (vars != null && vars.containsKey(p.getRefVariable())) {
						addval=p.getValue(vars.get(p.getRefVariable()).getProfvalue());
						if (!vars.get(p.getRefVariable()).isAllowed()) addval=0;
					}
					else addval=0;
				}
				else addval=p.getValue();
				myval = myval +addval;
			}
			for (ModelVariableCalcPart p : minusParts) {
				if (p.needsCol() && columnvalues!=null) addval=p.getValue(columnvalues[p.getColnumber()]);
				else if (p.needsVariable()) {
					if (vars != null && vars.containsKey(p.getRefVariable())) {
						addval=p.getValue(vars.get(p.getRefVariable()).getProfvalue());
						if (!vars.get(p.getRefVariable()).isAllowed()) addval=0;
					}
					else addval=0;
				}
				else addval=p.getValue();
				myval = myval - addval;
			}
			return myval;
		}
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
	
	
	
	public ModelVariableAgg (String type) throws Exception {
		if (type.equals("")) this.isMax=true; //Default: MAX
		else {
			this.parseType(type);
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
		if (isOccurence) return 1;
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
	
	public boolean isOccurence() {
		return isOccurence;
	}
	
	public boolean isMax() {
		return isMax;
	}
	
	public boolean isMin() {
		return isMin;
	}
}

/**
 * The Class ModelField.
 * Represents one field in an inputfiles file, e.g. ICD_CODE
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
	
	//private final static Logger LOGGER = Logger.getLogger(ModelVariable.class.getName());
	private String[] namePrefixes;
	private int[] nameColumnNumbers;
	private ColumnFilter[] cols;
	private ModelVariableCalc calc;
	private ModelVariableAgg agg;
	private double filterstart = -1;
	private double filterend = -1;
	private boolean set1; //if true: set var to 1 if >0 (after filter)
	private boolean include; //if true: only include patients that have this
	private boolean exclude; //if true: exclude all patients that have this
	private boolean target; //if target: print separately, do not include in coeff-calculation
	private boolean hideme; //if true: do not print var
	
	public ModelVariable (ModelVariableReadIn readvar, Model mymodel, Configuration config) throws ModelConfigException{
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
		this.set1=Consts.wahr.equals(readvar.getSet1Col());
		this.include=Consts.wahr.equals(readvar.getIncludeCol()); if (this.include) mymodel.setInclusion(true);
		this.exclude=Consts.wahr.equals(readvar.getExcludeCol()); if (this.exclude) mymodel.setExclusion(true);
		this.target=Consts.wahr.equals(readvar.getTargetCol()); if (this.target) mymodel.setHasTargets(true);
		this.hideme=Consts.wahr.equals(readvar.getHideCol());
		//parse columns + their filters
		int number = 0;
		try {
			cols = new ColumnFilter[readvar.getColumns().length]; 
			for (int i=0; i<readvar.getColumns().length; i++) {
				number=i+1;
				cols[i] = new ColumnFilter(readvar.getColumns()[i],readvar.getFilters()[i],null);	
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
		String myval;
		for (int i=0; i<myCols.length;i++) {
			//column definition in config file might be empty
			if (cols[i].getColumn()!= null ) {
				//get value from inputrow, and substring it
				myval = cols[i].getValueSubstring(inputrow.getValue(cols[i].getColumn()));
				if (!cols[i].isAllowed(myval)) return null; 
				myCols[i]=myval;
			}
		}
		return myCols;
	}
	
	public String getName (String[] columns) {
		String myname = "";
		for (int i=0; i<nameColumnNumbers.length; i++) {
			if (columns!=null && columns[nameColumnNumbers[i]] != null) 
				myname=myname + namePrefixes[i] + columns[nameColumnNumbers[i]];
			else myname=myname + namePrefixes[i] + "null";
		}
		myname=myname + namePrefixes[namePrefixes.length-1];
		return myname;
	}

	
	public boolean varIsAllowed (double value) {
		//simply parse to string 
		return ((filterstart==-1  ||  value >= filterstart ) // value >= filterstart 
				&& (filterend==-1  ||  value <= filterend)); //value <= filterend
	}
	
	public boolean set1 () {
		return set1;
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
	
	public boolean isOccAgg () {
		return agg.isOccurence();
	}
	
	public double getValue (String[] columnvalues, HashMap<String,Variable> vars) {
		return this.calc.getValue(columnvalues, vars);
	}
	
	public double aggregateValues (List<Double> values) {
		return this.agg.aggregateValues(values);
	}
	
	public boolean filterBeforeAggregation() {
		return this.agg.isMax() ||  this.agg.isMin();
	}
	
	
}