package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


//combines columnvalues and ModelVariable 
class Allrows {
	private List<String[]> rows = new ArrayList<String[]>();
	
	public Allrows(String[] colvalues) {
		this.add(colvalues);
	}
	
	public void add (String[] colvalues) {
		this.rows.add(colvalues);
	}
	
	public List<String[]> getAllRows () {
		return rows;
	}
}

/**
 * The Class Variable.
 * Used when building Variables from an inputfile(row) for a model, based on ModelFieldFilter   
 * Example:
 * 	Field: ICD_CODE
 * 	Stringposition: 1-3 (i.e. first 3 characters in icd)
 * 	Values: F32 (i.e. count only Depression icds)
 * 
 * There might be multiple filters on one field, see List in ModelField
 *  
 */
public class Variable {
	//private final static Logger LOGGER = Logger.getLogger(Variable.class.getName());
	
	private HashMap<ModelVariable,Allrows> rows = new  HashMap<ModelVariable,Allrows>();
	private boolean include = false; //if true: only include patients that have this
	private boolean exclude = false; //if true: exclude all patients that have this
	private boolean isTarget = false; //if true: is Target
	private boolean hideme = false; //if true: do not print var
	private boolean dependsOnOtherVars = false;
	
	private boolean isAllowed = true; //false, if all modelvariables or variable itself were filtered out
	
	private double profvalue = 1;
	
	private boolean profvalueIsCalulated = false;
	
	private boolean aggIsOccurence = true; //i.e. 1 (=occurence)


	public Variable() {
	}
	
	public void addRow (ModelVariable v,String[] values) {
		profvalueIsCalulated = false;
		if (!rows.containsKey(v)) {
			Allrows myrow = new Allrows(values);
			rows.put(v, myrow);
		} else rows.get(v).add(values);
		if (v.isInclude()) include=true; //i.e.: if true for one ModelVar -> true for all
		if (v.isExclude()) exclude=true; //i.e.: if true for one ModelVar -> true for all
		if (v.isTarget()) isTarget=true; //i.e.: if true for one ModelVar -> true for all
		if (v.hideme()) hideme=true; //i.e.: if true for one ModelVar -> true for all
		if (v.dependsOnOtherVar()) dependsOnOtherVars=true;  //if true for one -> must be calculated later
		if (!v.isOccAgg()) aggIsOccurence = false;
	}
	
	public boolean isInclude () {
		return include;
	}
	
	public boolean isExclude () {
		return exclude;
	}
	
	public boolean isTarget () {
		return isTarget;
	}
	
	public boolean hideme () {
		return hideme;
	}
	
	
	public boolean dependsOnOtherVars () {
		return dependsOnOtherVars;
	}
	
	public List<String> getOtherVarsDependent () {
		List<String> myVars = new ArrayList<String>();
		for (ModelVariable v : rows.keySet()) {
			if (v.dependsOnOtherVar()) myVars.addAll(v.getOtherVarsDependent());
		}
		return myVars;
	}
	
	public double getProfvalue () {
		if (!profvalueIsCalulated) this.calcProfvalue(); 
		return profvalue;
	}
	
	public double getCalcCoeff (double coeff) {
		if (!profvalueIsCalulated) this.calcProfvalue(); 
		return profvalue * coeff;
	}
	
	//calc w/o other know variables
	public void calcProfvalue() {
		this.calcProfvalue(null);
	}
	
	public void calcProfvalue (HashMap<String,Variable> vars) {
		if (profvalueIsCalulated) return; 
		profvalueIsCalulated=true;
		if (aggIsOccurence) this.profvalue=1;
		else {
			List<Double> allvalues = new ArrayList<Double>();
			//work through variables, filter and consolidate rows, aggregate
			ModelVariable aggV =null;
			double d;
			for (ModelVariable v : rows.keySet()) {
				//1: calculate values from single rows
				for (String[] singlerow : rows.get(v).getAllRows()) {
					d = v.getValue(singlerow, vars);
					allvalues.add(d);
				}
				aggV=v; //use last for aggregation & check
			}
			//2:filter, aggregate and filter again
			//1. filter before aggregation -> only for MAX, MIN
			if (aggV.filterBeforeAggregation()) {
				for (Iterator<Double> iterator = allvalues.iterator(); iterator.hasNext();) {
				    double x = iterator.next();
				    if (!aggV.varIsAllowed(x)) {
				        // Remove the current element from the iterator and the list.
				        iterator.remove();
				    }
				}
			}
			if (allvalues.size()>0) {
				this.profvalue=aggV.aggregateValues(allvalues);
				if (aggV.varIsAllowed(this.profvalue)) isAllowed=true; else isAllowed=false;
			} else isAllowed=false;
		}
	}
	
	public boolean isAllowed() {
		return isAllowed;
	}

}
