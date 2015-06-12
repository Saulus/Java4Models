package models;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import configuration.Consts;
import au.com.bytecode.opencsv.CSVReader;

/**
 * The Class Otherfieldfilter.
 * Helper class, holds filter on other field from Model.config, column Otherfieldfilter
 * Notation example: Bezugsjahr$>=$2012$Bezugsjahr$<=$2014
 * 
 * Operatins allowed (as String): =, >, >=, <, <=
 * 
 * Their might be multiple constraints/filters for a field, see List in class Modelfieldfilter 
 */
class Otherfieldfilter {
	private String field;
	private String operation;
	private String testvalue;
	
	public Otherfieldfilter (String field, String operation, String value) {
		this.field = field;
		this.operation = operation;
		this.testvalue = value;
	}
	
	public String getField () {
		return field;	
	}
	
	public boolean valueIsAllowed (String value) {
		if (operation.equals("="))  
	        return value.equals(this.testvalue);  
	      else if (operation.equals(">"))  
		    return value.compareTo(this.testvalue)==1;
	      else if (operation.equals(">="))  
			return value.compareTo(this.testvalue)!=-1;
	      else if (operation.equals("<"))  
	    	  return value.compareTo(this.testvalue)==-1;
	      else if (operation.equals("<="))  
	    	  return value.compareTo(this.testvalue)!=1;
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
class ModelFieldFilter {
	private Integer fields_pos_min;
	private Integer fields_pos_max;
	private String fields_value_min;
	private String fields_value_max;
	private ArrayList<Otherfieldfilter> otherfieldfilters = new ArrayList<Otherfieldfilter>();
	private String variable;
	private String aggregation;
	private boolean include; //if true: only include patients that have this
	private boolean exclude; //if true: exclude all patients that have this
	
	public ModelFieldFilter (Integer pos_min, Integer pos_max, String value_min, String value_max, ArrayList<Otherfieldfilter> filters, String variable, String aggregation, boolean include, boolean exclude) {
		this.fields_pos_min=pos_min;
		this.fields_pos_max=pos_max;
		this.fields_value_min=value_min;
		this.fields_value_max=value_max;
		this.variable=variable;
		this.otherfieldfilters.addAll(filters);
		this.include=include;
		this.exclude=exclude;
		this.aggregation=aggregation;
	}
	
	public Integer getFields_pos_min() {
		return fields_pos_min;
	}
	public Integer getFields_pos_max() {
		return fields_pos_max;
	}
	public String getFields_value_min() {
		return fields_value_min;
	}
	public String getFields_value_max() {
		return fields_value_max;
	}
	public String getVariable() {
		return variable;
	}
	public ArrayList<Otherfieldfilter> getOtherfieldfilters() {
		return otherfieldfilters;
	}
	public String getAggregation() {
		return aggregation;
	}
	public boolean getInclude() {
		return include;
	}
	public boolean getExclude() {
		return exclude;
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
class ModelField { 
	private List<ModelFieldFilter> filters = new ArrayList<ModelFieldFilter>();
	
	public ModelField () {
	}
	
	/**
	 * adds a filter for a field
	 */
	public void addFilter (Integer pos_min, Integer pos_max, String value_min, String value_max, String aggregation, ArrayList<Otherfieldfilter> filters, String variable, boolean include, boolean exclude) {
		ModelFieldFilter newfilter = new ModelFieldFilter (pos_min,pos_max,value_min,value_max,filters,variable,aggregation,include,exclude);
		this.filters.add(newfilter);
	}
	
	
	/**
	 * Gets the Variables for a model on a field in an inputfile.
	 * 	 *
	 * @param value the current field value from the inputfile that is to be processed
	 * @param inputrow the full inputrow from the inputfile; to test filters in other fields
	 * @return a hashmap of Variable, Value fields (e.g. ICD_F32 -> 5)
	 */
	public List<Variable> getVariables(String value, InputFile inputrow) {
		HashMap<String,Variable> targetvars = new HashMap<String,Variable>(); //hashmap to keep one variable(typ) per row only
		for(ModelFieldFilter filter : filters) {
			//First thing: Test if Otherfieldfilters are true
			boolean isAllowed = false;
			if (filter.getOtherfieldfilters().isEmpty()) {
				isAllowed = true;
			} else {
				for(Otherfieldfilter otherfieldfilter : filter.getOtherfieldfilters()) {
					if (otherfieldfilter.valueIsAllowed(inputrow.getValue(otherfieldfilter.getField()))) {
						isAllowed=true;
						break;
					}
				}
			}
			if (!isAllowed) continue;
			//String positions filled? -> reduce value to substring
			String myValue = value;
			if ((filter.getFields_pos_min()>0) ||
				(filter.getFields_pos_max()>0)) {
				int mymin = filter.getFields_pos_min();
				int mymax = filter.getFields_pos_max();
				if ((mymin > (myValue.length()-1)) || (mymin < 0))  { mymin=0; }
				if ((mymax > (myValue.length()-1)) || (mymax <= 0)) { mymax = myValue.length()-1; }
				myValue = myValue.substring(mymin,mymax+1);
			}
			//In Range? or Range not filled? -> take coeff 
			if ( (filter.getFields_value_min().equals("") || myValue.compareTo(filter.getFields_value_min())>=0)
				&& (filter.getFields_value_max().equals("") || myValue.compareTo(filter.getFields_value_max())<=0)) {
				String myvariable = filter.getVariable();
				//deal with $ place holder
				int placeholder_pos = myvariable.indexOf(Consts.placeholder);
				if (placeholder_pos > -1) {
					StringBuilder sb = new StringBuilder(myvariable);
					sb.deleteCharAt(placeholder_pos);
					sb.insert(placeholder_pos, myValue);
					myvariable = sb.toString();
				}
				targetvars.put(myvariable,new Variable(myvariable,myValue,filter.getAggregation(),filter.getInclude(),filter.getExclude()));// i.e. if 1 var is filled multi times from 1 row -> keep last value only
			}
		}
		return new ArrayList<Variable>(targetvars.values());
	}
}

/**
 * The Class ModelFile.
 * Represents the configuration for an inputfile for a model (all fields and their filters needed) 
 * 
 *  
 */
class ModelFile {
	private HashMap<String,ModelField> filefields = new HashMap<String,ModelField>();
	
	public ModelFile () {
	}
	
	/**
	 * Adds a field to the model/inputfile relationship
	 * parses strings
	 * 
	 * @param field the field
	 * @param position the position from Model.config
	 * @param values the allowed values from Model.config
	 * @param aggregation the aggregation type from Model.config
	 * @param otherfieldfilter the otherfieldfilter String from Model.config
	 * @param variable the variablefrom Model.config
	 */
	public void addField (String field, String position, String values, String aggregation, String otherfieldfilter, String variable, String include, String exclude, Model mymodel) {
		if ((field != "") && (variable != "")) {
			//field already there?
			ModelField myfield = this.filefields.get(field);
			if (myfield == null) {
				myfield = new ModelField();
			}
			//Parse Stringposition
			int min_pos = 0;
			int max_pos = 0; 
			if (position.contains("-")) {
				try { min_pos = Integer.parseInt(position.split("-")[0])-1; } //In Java Strings start from 0
					catch (Exception e) {} //ignore Parsing.Exception, as "0" as init-value is fine 
				try { max_pos = Integer.parseInt(position.split("-")[1])-1; } //In Java Strings start from 0
					catch (Exception e) {} //ignore Parsing.Exception, as "0" as init-value is fine 
			}
			//Parse Values
			String min_value = "";
			String max_value = ""; 
			if (values.contains("-")) {
				min_value = values.split("-")[0];
				max_value = values.split("-")[1];
			} else {
				min_value=values;
				max_value=values;
			}
			//Parse Otherfieldfilter
			ArrayList<Otherfieldfilter> filters = null;
			if (otherfieldfilter != "") {
				filters = new ArrayList<Otherfieldfilter>();
				String[]tokens = otherfieldfilter.split(Consts.placeholderEsc);
				for (int i=0; i<tokens.length; i = i+3) {
					try {
						Otherfieldfilter newfilter = new Otherfieldfilter(tokens[i],tokens[i+1], tokens[i+2]);
						filters.add(newfilter);
					} catch (Exception e) {}
				}
			}
			//Parse include and exclude
			boolean b_include="TRUE".equals(include); if (b_include) mymodel.setInclusion(true);
			boolean b_exclude="TRUE".equals(exclude); if (b_exclude) mymodel.setExclusion(true);
			myfield.addFilter(min_pos, max_pos, min_value, max_value, aggregation, filters, variable,b_include,b_exclude);
			this.filefields.put(field,myfield);
		}
	}
	
	/**
	 * Gets all used fields in an inputfile for this model
	 */
	public Set<String> getFields() {
		return filefields.keySet();
	}
	
	/**
	 * Gets the Variables for a model on a field in an inputfile.
	 * 
	 * @param field the field the value was read from
	 * @param value the current field value from the inputfile that is to be processed
	 * @param inputrow the full inputrow from the inputfile; to test filters in other fields
	 * @return a hashmap of Variable, Value fields (e.g. ICD_F32 -> 5)
	 */
	public List<Variable> getVariables(String field, String value, InputFile inputrow) {
		return filefields.get(field).getVariables(value,inputrow);
	}
}


/**
 * The Class Model.
 * Represents a model as configured by 
 * Model.config ,i.e. which fields from inputfiles are relevant and how are they to be processed into model variables, and 
 * Model.coeff ,i.e. which coefficients are assigned to which variables (incl. intercept) 
 */
public class Model {
	
	/** The coeffs. */
	private HashMap<String,Double> coeffs = new HashMap<String,Double>();
	
	/** The modelfiles. */
	private HashMap<InputFile,ModelFile> modelfiles = new HashMap<InputFile,ModelFile>();
	
	/** The name. */
	private String name;
	
	/** The type. */
	private String type = Consts.logRegFlag;
	
	/** The i have coeffs. */
	private boolean iHaveCoeffs = true;
	
	/** The i have inclusion criteria. */
	private boolean iHaveInclusion = false;
	
	/** The i have exclusion criteria. */
	private boolean iHaveExclusion = false;
	
	/**
	 * Instantiates a new model.
	 *
	 * @param name the name
	 * @param inputfiles the inputfiles
	 * @param configfile the configfile
	 * @param coefffile the coefffile
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public Model (String name, List<InputFile> inputfiles, String configfile, String coefffile) throws IOException {
		this.name = name;
		//Create one modelfile object per inputfile
		for (InputFile myinputfile : inputfiles) {
			ModelFile mymodelfile = new ModelFile();
			this.modelfiles.put(myinputfile,mymodelfile);
		}
		//read fields-data and add per modelfile, if present in corresponding inputfile
		HashMap<String,String> fields_data = new HashMap<String,String>(); //(header -> value)
		CSVReader reader = new CSVReader(new FileReader(configfile), ';', '"');
		List<String[]> myEntries = reader.readAll();
		//first line = header-line
		String[] headerline = myEntries.get(0);
		myEntries.remove(0);
		for (String[] nextline : myEntries) {
			//ignore comments
			if (!nextline[0].substring(0, 0).equals(Consts.comment_indicator)) {
				//add current line values to Hashmap (header -> value)
				//this prohibits errors from wrong column order in config file
				for (int j=0; j<nextline.length; j++) {
					fields_data.put(headerline[j].toUpperCase(), nextline[j].toUpperCase());
				}
				//find inputfile(s) for this line / fields_data
				for (InputFile myinputfile : inputfiles) {
					if ((myinputfile.isDatentyp(fields_data.get(Consts.modInputfileCol))) 
							&& (myinputfile.hasField(fields_data.get(Consts.modFieldCol)))) {
						//save aggregation type per variable found 
						String aggType = fields_data.get(Consts.modAggCol);
						//ToDO: Config-Check (z.B. Wrong Aggregation Type?)
						if (aggType.equals("")) { aggType = Consts.aggStd; }
						this.modelfiles.get(myinputfile).addField(
								fields_data.get(Consts.modFieldCol),
								fields_data.get(Consts.modPositionCol),
								fields_data.get(Consts.modValueCol),
								aggType,
								fields_data.get(Consts.modOtrherfieldCol),
								fields_data.get(Consts.modVarCol),
								fields_data.get(Consts.modIncludeCol),
								fields_data.get(Consts.modExcludeCol),
								this);
					}
				}
			}
		}
		reader.close();
	   //read Coeffs -> if error model creates profile only
		try {
			reader = new CSVReader(new FileReader(coefffile), ';', '"', 1);
			myEntries = reader.readAll();
		    for (String[] nextline1 : myEntries) {
		    	this.coeffs.put(nextline1[0].toUpperCase(), Double.parseDouble(nextline1[1].replace(",", ".")));
		    }
		    reader.close();
		} catch (IOException e) {
			System.out.println("Fehler beim Einlesen der Koeffizienten für Modell "+ name + ". Es werden nur Profile für das Modell gebildet.");
			this.iHaveCoeffs = false;
		}
	}
	
	/**
	 * Gets the fields.
	 *
	 * @param inputfile the inputfile
	 * @return the fields
	 */
	public Set<String> getFields (InputFile inputfile) {
		return this.modelfiles.get(inputfile).getFields();
	}
	
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName () {
		return this.name;
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public String getType () {
		return this.type;
	}
	
	/**
	 * Gets the variables.
	 *
	 * @param inputfile the inputfile
	 * @param field the field
	 * @param value the value
	 * @return the variables
	 */
	public List<Variable> getVariables(InputFile inputfile, String field, String value) {
		return modelfiles.get(inputfile).getVariables(field, value, inputfile);
	}
	
	
	/**
	 * Gets the coeff.
	 *
	 * @param variable the variable
	 * @return the coeff
	 */
	public double getCoeff (String variable) {
		double mycoeff;
		if (this.coeffs.get(variable) == null) { //modell does not contain coeff -> assume it is zero
			mycoeff = 0;
		} else {
			mycoeff = this.coeffs.get(variable);
		}
		return mycoeff;
	}
	
	/**
	 * Checks for coeffs.
	 *
	 * @return true, if successful
	 */
	public boolean hasCoeffs () {
		return iHaveCoeffs;
	}
	
	public boolean hasInclusion () {
		return iHaveInclusion;
	}
	
	public void setInclusion (boolean b) {
		iHaveInclusion = b;
	}
	
	public boolean hasExclusion () {
		return iHaveExclusion;
	}
	
	public void setExclusion (boolean b) {
		iHaveExclusion = b;
	}
	
}
