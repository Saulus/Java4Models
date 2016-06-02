package models;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import configuration.Consts;
import configuration.Configuration;
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
	public void addVariable (ModelVariableReadIn readvar, Model mymodel,Configuration config) throws ModelConfigException {
		if (!readvar.getVariableCol().equals("") && !readvar.getColumns()[0].isEmpty()) {
			ModelVariable v = new ModelVariable(readvar, mymodel,config);
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
		String[] colvalues;
		String myname;
		for(ModelVariable variable : filevars) {
			//1. create column values array -> returns null if not allowed by rowfilter
			colvalues = variable.getColumnValues(inputrow);
			//1b: Test for filters
			if (colvalues!=null) {
				//create variable name
				myname = variable.getName(colvalues);
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
	
	private final static Logger LOGGER = Logger.getLogger(Model.class.getName());
	
	/** The coeffs. */
	private HashMap<String,Double> coeffs = new HashMap<String,Double>();
	
	/** The modelfiles. */
	private HashMap<InputFile,ModelFile> modelfiles = new HashMap<InputFile,ModelFile>();
	
	/** The name. */
	private String name;
	
	/** The type. */
	private String type = Consts.logRegFlag;
	
	/** The i have coeffs. */
	private boolean iHaveCoeffs = false;
	
	/** The i have inclusion criteria. */
	private boolean iHaveInclusion = false;
	
	/** The i have exclusion criteria. */
	private boolean iHaveExclusion = false;
	
	private boolean iHaveTargets = false;
	
	private String interceptname = Consts.interceptname;

	
	/**
	 * Instantiates a new model.
	 *
	 * @param name the name
	 * @param inputfiles the inputfiles
	 * @param configfile the configfile
	 * @param coefffile the coefffile
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public Model (String name, List<InputFile> inputfiles, Configuration config) throws Exception {
		this.name = name;
		String configfile = config.getModelpath() + "\\" + name+config.getModelConfigExt();
		//read fields-data and add per modelfile, if present in corresponding inputfile
		HashMap<String,String> fields_data = new HashMap<String,String>(); //(header -> value)
		CSVReader reader = new CSVReader(new FileReader(configfile), ';', '"');
		List<String[]> myEntries = reader.readAll();
		reader.close();
		//first line = header-line
		String[] headerline = myEntries.get(0);
		//make uppercase and count number of column/filters
		int columnnumber=0;
		for (int j=0; j<headerline.length; j++) {
			headerline[j] = headerline[j].toUpperCase();
			if (headerline[j].startsWith(Consts.modColumnCol)) columnnumber++; 
		}
		myEntries.remove(0);
		ModelVariableReadIn newvar;
		for (String[] nextline : myEntries) {
			//ignore comments and empty lines
			if (!nextline[0].isEmpty() && !nextline[0].substring(0, 1).equals(Consts.comment_indicator)) {
				//add current line values to Hashmap (header -> value)
				//this prohibits errors from wrong column order in config file
				for (int j=0; j<headerline.length; j++) {
					if (j<nextline.length)
						fields_data.put(headerline[j], nextline[j]);
				}
				//find inputfile(s) for this line / fields_data
				for (InputFile myinputfile : inputfiles) {
					if (myinputfile.isDatentyp(fields_data.get(Consts.modInputfileCol).toUpperCase())) {
						//relevant inputfile is found -> create ModelFile if not present
						if (!this.modelfiles.containsKey(myinputfile)) this.modelfiles.put(myinputfile,new ModelFile());
						//add vars
						newvar = new ModelVariableReadIn(fields_data,columnnumber,myinputfile);
						this.modelfiles.get(myinputfile).addVariable(newvar,this,config);
					}
				}
			}
		}
		
		if (config.createScores()) {
		   //read Coeffs, if available
			String coefffile=config.getModelpath() + "\\" + name+config.getModelCoeffExt();
			try {
				reader = new CSVReader(new FileReader(coefffile), ';', '"', 1);
				myEntries = reader.readAll();
				reader.close();
			    for (String[] nextline1 : myEntries) {
			    	this.coeffs.put(nextline1[0].toUpperCase(), Double.parseDouble(nextline1[1].replace(",", ".")));
			    }
			    this.iHaveCoeffs = true;
			} catch (IOException e) {
				LOGGER.log(Level.WARNING,"Fehler beim Einlesen der Koeffizienten für Modell "+ name + ". Es werden nur Profile für das Modell gebildet.");
				this.iHaveCoeffs = false;
			}
		}
		//set Interceptname
		if (Configuration.interceptname != null) this.interceptname=Configuration.interceptname;
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
	
	public boolean inputfileIsRelevant (InputFile inputfile) {
		return modelfiles.containsKey(inputfile);
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
		if (inputfileIsRelevant(inputfile))
			return modelfiles.get(inputfile).updateVariables(inputfile,existingVars);
		else return existingVars;
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
	
	public double getCoeffIntercept () {
		return getCoeff(this.interceptname);
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
	
	public boolean hasTargets () {
		return iHaveTargets;
	}
	
	public void setHasTargets (boolean b) {
		iHaveTargets = b;
	}
	
}
