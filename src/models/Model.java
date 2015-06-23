package models;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import configuration.Consts;
import au.com.bytecode.opencsv.CSVReader;


/**
 * The Class ModelFile.
 * Represents the configuration for an inputfile for a model (all fields and their filters needed) 
 * 
 *  
 */
class ModelFile {
	private List<ModelVariable> filevars= new ArrayList<ModelVariable>();
	
	public ModelFile () {
	}
	
	/**
	 * Adds a field to the model/inputfile relationship
	 * 
	 * @param field the field
	 * @param position the position from Model.config
	 * @param values the allowed values from Model.config
	 * @param aggregation the aggregation type from Model.config
	 * @param otherfieldfilter the otherfieldfilter String from Model.config
	 * @param variable the variablefrom Model.config
	 */
	public void addVariable (String variable, String columns, String filter, String calculation, String aggregation, String include, String exclude, String hideme, Model mymodel) throws ModelConfigException {
		if (!variable.equals("") && !columns.equals("")) {
			ModelVariable v = new ModelVariable(variable, columns, filter, calculation,aggregation, include, exclude, hideme, mymodel);
			filevars.add(v);
		}
	}
	
	
	
	/**
	 * Gets the Variables for a model on a field in an inputfile.
	 * 	 *
	* @param inputrow the full inputrow from the inputfile; to test filters in other fields
	 * @return a list of Variables incl. values
	 */
	public HashMap<String,Variable> updateVariables(InputFile inputrow, HashMap<String,Variable> existingVars) {
		Variable myVar;
		for(ModelVariable variable : filevars) {
			//1. create column values array;
			String[] colvalues = variable.getColumnValues(inputrow);
			//1b: Test for filters
			if (variable.rowIsAllowed(colvalues)) {
				//create variable name
				String myname = variable.getName(colvalues);
				//test filters
				if (!existingVars.containsKey(myname)) {
					myVar = new Variable();
					existingVars.put(myname, myVar);
				}
				existingVars.get(myname).addRow(variable, colvalues);
			}
		}
		return existingVars;
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
	public Model (String name, List<InputFile> inputfiles, String configfile, String coefffile) throws Exception {
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
			if (!nextline[0].substring(0, 1).equals(Consts.comment_indicator)) {
				//add current line values to Hashmap (header -> value)
				//this prohibits errors from wrong column order in config file
				for (int j=0; j<nextline.length; j++) {
					fields_data.put(headerline[j].toUpperCase(), nextline[j].toUpperCase());
				}
				//find inputfile(s) for this line / fields_data
				for (InputFile myinputfile : inputfiles) {
					if ((myinputfile.isDatentyp(fields_data.get(Consts.modInputfileCol))) 
							&& (myinputfile.hasField(fields_data.get(Consts.modVariableCol)))) {
								this.modelfiles.get(myinputfile).addVariable(
									fields_data.get(Consts.modVariableCol),
									fields_data.get(Consts.modColumnCol),
									fields_data.get(Consts.modFilterCol),
									fields_data.get(Consts.modCalcCol),
									fields_data.get(Consts.modAggCol),
									fields_data.get(Consts.modIncludeCol),
									fields_data.get(Consts.modExcludeCol),
									fields_data.get(Consts.modHideCol),
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
	public HashMap<String,Variable> updateVariables(InputFile inputfile, HashMap<String,Variable> existingVars ) {
		return modelfiles.get(inputfile).updateVariables(inputfile,existingVars);
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
