package models;

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
	private String variable;
	private String value;
	private String aggregation;
	private boolean include; //if true: only include patients that have this
	private boolean exclude; //if true: exclude all patients that have this

	public Variable(String variable, String value, String aggregation, boolean include, boolean exclude) {
		this.setVariable(variable);
		this.setValue(value);
		this.setAggregation(aggregation);
		this.setInclude(include);
		this.setExclude(exclude);
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getAggregation() {
		return aggregation;
	}

	public void setAggregation(String aggregation) {
		this.aggregation = aggregation;
	}

	public boolean isInclude() {
		return include;
	}

	public void setInclude(boolean include) {
		this.include = include;
	}

	public boolean isExclude() {
		return exclude;
	}

	public void setExclude(boolean exclude) {
		this.exclude = exclude;
	}
	

}
