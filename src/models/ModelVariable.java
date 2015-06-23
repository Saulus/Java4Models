package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.Days;

import configuration.Consts;
import configuration.Utils;

/**
 * The Class ModelVariableFilter.
 * Helper class, holds filter on other field from Model.config
 * Notation example: Bezugsjahr$>=$2012$Bezugsjahr$<=$2014
 * 
 * Operatins allowed (as String): =, >, >=, <, <=
 * 
 * Their might be multiple constraints/filters for a field, see List in class Modelfieldfilter 
 */
class ModelVariableFilterPart {
	private int refColnumber; //0=current variable; columns starting from 1
	private boolean isEquals =false; 
	private boolean isBigger=false;
	private boolean isSmaller=false;
	private String testvalue;
	
	public ModelVariableFilterPart (String field, String operation, String value) throws Exception {
		isEquals = (operation.contains("="));
		isBigger = (operation.contains(">"));
		isSmaller = (operation.contains("<"));
		this.refColnumber = Integer.parseInt(field.substring(1));
		this.testvalue = value;
	}
	
	public int getRefColnumber () {
		return refColnumber;	
	}
	
	public boolean valueIsAllowed (double value) {
		//simply parse to string -> easier
		return valueIsAllowed(Double.toString(value));
	}
	
	public boolean valueIsAllowed (String value) {
		if (isEquals&isBigger) return value.compareTo(this.testvalue)!=-1;
		else if (isEquals&isSmaller) return value.compareTo(this.testvalue)!=1;
		else if (isEquals) return value.equals(this.testvalue);
		else if (isBigger) return value.compareTo(this.testvalue)==1;
		else if (isSmaller) return value.compareTo(this.testvalue)==-1;
	    else return true;
	}
}

/**
 * The Class ModelFieldFilter.
 * Helper class, holds filter on field from Model.config, columns: Field, Stringposition, Values, Aggregation
 * Example:
 * 	Field: ICD_CODE
 * 	Stringposition: 1-3 (i.e. first 3 characters in icd)
 * 	Values: F32 (i.e. count only Depression icds)
 * 
 * There might be multiple filters on one field, see List in ModelField
 *  
 */
class ModelVariableFilter {
	private List <ModelVariableFilterPart> othercolumns = new ArrayList<ModelVariableFilterPart>();
	private List <ModelVariableFilterPart> ownvar = new ArrayList<ModelVariableFilterPart>();
	
	
	public ModelVariableFilter (String filters) throws Exception {
		if (!filters.equals("")) {
			String[] tokens = filters.split(Consts.seperatorEsc);
			
			for (int i=0; i<tokens.length; i = i+3) {
				//1.ModelVariableAggPart
				ModelVariableFilterPart p = new ModelVariableFilterPart(tokens[0],tokens[1],tokens[2]);
				if (p.getRefColnumber()==0) ownvar.add(p);
				else othercolumns.add(p);
			}
		}
	}
	
	public boolean rowIsAllowed (String[] columns) {
		boolean isAllowed = true;
		if (!othercolumns.isEmpty()) {
			for(ModelVariableFilterPart testfilter : othercolumns) {
				if (!testfilter.valueIsAllowed(columns[testfilter.getRefColnumber()])) {
					isAllowed=false;
					break;
				}
			}
		} 
		return isAllowed;
	}
	
	public boolean varIsAllowed (double value) {
		boolean isAllowed = true;
		if (!ownvar.isEmpty()) {
			for(ModelVariableFilterPart testfilter : ownvar) {
				if (!testfilter.valueIsAllowed(value)) {
					isAllowed=false;
					break;
				}
			}
		} 
		return isAllowed;
	}
	
}

class ModelVariableCalcPart {
	private boolean needsCol = false;
	private boolean needsVariable= false;
	private int refColnumber; //starting with 1 (0 is reserved for variable-> needed only for filter)
	private String refVariable;
	private double value;
	private LocalDate mydate;
	private LocalDate myreferencedate = Utils.parseDate(Consts.reference_date);
	
	private boolean isValue;
	private boolean isDate;
	
	
	
	public ModelVariableCalcPart (String type, String value) throws Exception {
			parseType(type);
			parseValue(value);
	}
	
	private void parseType (String type) {
		isValue = (type.equals(Consts.aggValue));
		isDate= (type.equals(Consts.aggDate));
	}
	
	private void parseValue (String value) throws Exception {
		if (value.startsWith(Consts.reference)) {
			refColnumber = Integer.parseInt(value.substring(1));
			needsCol=true;
		} else if (value.startsWith(Consts.varreferenceEsc)) {
			refVariable = value.split(Consts.bracketEsc)[1];
			needsVariable=true;
		} else {
			//test if value contains "," -> change to "."
			String newvalue = value.replace(",", ".");
			this.value=Double.parseDouble(newvalue); 
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
				inputvalue.replace(",", ".");
				newval = Double.parseDouble(inputvalue);
			} catch (Exception e) { newval = 1; } 
		return getValue(newval);
	}
	
	//used for variable input
	public double getValue(double inputvalue) {
		if (isValue) return value;
		else return inputvalue;
	}
	
	public double getValue() {
		return value;
	}
	
	public boolean isValue() {
		return isValue;
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
			//if not starting with"+" or "-" -> add "+"
			if (!calculation.startsWith("+") &&  !calculation.startsWith("-")) {
				calculation = "+!" + calculation;
			}
			String[] tokens = calculation.split(Consts.seperatorEsc);
			
			for (int i=0; i<tokens.length; i = i+2) {
				//1.ModelVariableAggPart
				String[] parts = tokens[i+1].split(Consts.bracketEsc);
				ModelVariableCalcPart p = new ModelVariableCalcPart(parts[0],parts[1]);
				if (p.needsVariable()) {
					dependsOnOtherVar=true;
					otherVars.add(p.getRefVariable());
				}
				if (tokens[i].equals("-")) minusParts.add(p);
					else plusParts.add(p);
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
	private int pos_max = 0;
	
	public ModelVariableCols (String col) throws Exception {
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
	
	public String getColumn() {
		return column;
	}
	
	public int getMinPos () {
		return pos_min;
	}
	
	public int getMaxPos () {
		return pos_max;
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
			return myval;
		}
		if (isCount) return values.size();
		if (isMean) {
			for (double d: values) myval+=d;
			return myval/values.size();
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
			return myvalmax-myvalmin;
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
	private ModelVariableFilter filter;
	private boolean include; //if true: only include patients that have this
	private boolean exclude; //if true: exclude all patients that have this
	private boolean hideme; //if true: do not print var
	
	public ModelVariable (String variable, String columns, String filter, String calculation, String aggregation, String include, String exclude, String hideme, Model mymodel) throws ModelConfigException{
		String[] tokens;
		//parse name
		try {
			tokens = variable.split(Consts.seperatorEsc);
			namePrefixes = new String[tokens.length];
			nameColumnNumbers = new int[tokens.length];
			for (int i=0; i<tokens.length; i++) {
				if (i==0) namePrefixes[i]= tokens[i];
					else namePrefixes[i]= tokens[i].substring(1);
				if (i<tokens.length-1) nameColumnNumbers[i]=Integer.parseInt(tokens[i+1].substring(0,1));
			}
		} catch (Exception e) {
			throw new ModelConfigException("Fehler bei Variable "+ variable + ", Spalten "+ columns + " (Variable)",e); 
		}
		//parse columns
		try {
			tokens = columns.split(Consts.seperatorEsc);
			cols = new ModelVariableCols[tokens.length]; 
			ModelVariableCols c;
			for (int i=0; i<tokens.length; i++) {
				c = new ModelVariableCols(tokens[i]);
				cols[i]=c;
			}
		} catch (Exception e) {
			throw new ModelConfigException("Fehler bei Variable "+ variable + ", Spalten "+ columns + " (Spalten)",e); 
		}
		//parse filter
		try {
			this.filter = new ModelVariableFilter(filter);
		} catch (Exception e) {
			throw new ModelConfigException("Fehler bei Variable "+ variable + ", Spalten "+ columns + " (Filter)",e); 
		}
		//parse calculation
		try {
			this.calc = new ModelVariableCalc(calculation);
		} catch (Exception e) {
			throw new ModelConfigException("Fehler bei Variable "+ variable + ", Spalten "+ columns + " (Berechnung)",e); 
		}
		//parse aggregation
		try {
			this.agg = new ModelVariableAgg(aggregation);
		} catch (Exception e) {
			throw new ModelConfigException("Fehler bei Variable "+ variable + ", Spalten "+ columns + " (Aggregation)",e); 
		}
		//parse rest
		this.include=Consts.wahr.equals(include); if (this.include) mymodel.setInclusion(true);
		this.exclude=Consts.wahr.equals(exclude); if (this.exclude) mymodel.setExclusion(true);
		this.hideme=Consts.wahr.equals(hideme);
	}
	
	/*
	 * get column values, substrings
	 */
	public String[] getColumnValues (InputFile inputrow) {
		String[] myCols = new String[cols.length-1];
		int mymax;
		String myval;
		for (int i=0; i<myCols.length;i++) {
			myval = inputrow.getValue(cols[i].getColumn());
			mymax=Math.min(cols[i].getMaxPos(), myval.length());
			myCols[i]=myval.substring(cols[i].getMinPos(), mymax);
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
	
	public boolean rowIsAllowed (String[] columns) {
		boolean isallowed = true;
		//test if name columns are not empty
		for (int i=0; i<nameColumnNumbers.length; i++) {
			isallowed = !columns[nameColumnNumbers[i]].equals("");
		}
		return isallowed && filter.rowIsAllowed(columns);
	}
	
	public boolean varIsAllowed (double value) {
		return filter.varIsAllowed(value);
	}
	
	public boolean isInclude () {
		return include;
	}
	
	public boolean isExclude () {
		return exclude;
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