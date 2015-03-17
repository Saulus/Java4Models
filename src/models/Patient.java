package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import configuration.Consts;

/**
 * The Class PatientVariable.
 * Represents an actual profile/model-variable for a patient, as read in when processing inputfiles
 * Holds count and sum, values are increased each time the variable turns up for the patient 
 */
class PatientVariable {
	private int count = 1;
	private double sum;
	private String aggregationType;
	private double profvalue = 1;
	private double coeff = 0;
	
	
	public PatientVariable (String aggregationType, String value, double coeffVal) {
		if ((aggregationType.equals(Consts.aggSum)) ||		//for SUm or Mean-type: calc sum
			(aggregationType.equals(Consts.aggMean))) {
			//test if value contains "," -> change to "."
			String newvalue = value.replace(",", ".");
			this.sum=Double.parseDouble(newvalue);
		} else if ((aggregationType.equals(Consts.aggMin)) ||
			(aggregationType.equals(Consts.aggMax))) { //for Min/Max type: calc profvalue
				//test if value contains "," -> change to "."
				String newvalue = value.replace(",", ".");
				this.profvalue=Double.parseDouble(newvalue);
		}
		this.coeff=coeffVal;
		this.aggregationType=aggregationType;
	}
	
	public void add (String value) {
		if ((aggregationType.equals(Consts.aggSum)) ||		//for SUm or Mean-type: calc sum
				(aggregationType.equals(Consts.aggMean))) {
				//test if value contains "," -> change to "."
				String newvalue = value.replace(",", ".");
				this.sum+=Double.parseDouble(newvalue);
		} else if (aggregationType.equals(Consts.aggMin)) { //for Min/Max type: calc profvalue
					//test if value contains "," -> change to "."
					String newvalue = value.replace(",", ".");
					this.profvalue=Math.min(this.profvalue,Double.parseDouble(newvalue));
		} else if (aggregationType.equals(Consts.aggMax)) { //for Min/Max type: calc profvalue
			//test if value contains "," -> change to "."
			String newvalue = value.replace(",", ".");
			this.profvalue=Math.max(this.profvalue,Double.parseDouble(newvalue));
		}
		this.count +=1;
	}
	
	public void calcProfvalue () {
		if (aggregationType.equals(Consts.aggSum)) {
			this.profvalue = this.sum; 
		} else if (aggregationType.equals(Consts.aggCount)) {
			this.profvalue = this.count; 
		} else if (aggregationType.equals(Consts.aggMean)) {
			this.profvalue = this.sum / this.count; 
		}
	}
	
	public double getProfvalue () {
		return profvalue;
	}
	
	public double getCalcCoeff () {
		return profvalue * coeff;
	}
	
}

/**
 * The Class PatientModel.
 * Represents a full model with all variables for a patient, as read in when processing inputfiles
 * 
 * can return the coeff sum (i.e. risk score) for this patient and model
 */
class PatientModel {
	private HashMap<String,PatientVariable> variables = new HashMap<String,PatientVariable>(); //variablename -> PatientVariable
	private Model model;
	private boolean profValuesAreCalculated=false;

	
	public PatientModel(Model model) {
		this.model = model;
	}
	
	public void addVariable (String variable, String value, String aggregation, double coeff ) {
		profValuesAreCalculated=false;
		if (variables.get(variable) == null) {
			PatientVariable pvar = new PatientVariable(aggregation,value,coeff);
			variables.put(variable,pvar);
		} else {
			variables.get(variable).add(value);
		}
	}
	
	private void calcProfValues () {
		if (!profValuesAreCalculated) {
			for (PatientVariable variable : variables.values()) {
				variable.calcProfvalue();
			}
			profValuesAreCalculated = true;
		}
	}
	
	
	public double getCoeffSum () {
		calcProfValues();
		double mysum = model.getCoeff(Consts.interceptname);
		for (PatientVariable variable : variables.values()) {
			mysum += variable.getCalcCoeff();
		} 
		if (this.model.getType().equals(Consts.logRegFlag)) {
			mysum = Math.exp(mysum) / (1+Math.exp(mysum));
		}
		return mysum;
	}
	
	/**
	 * adds Variables from that model to the list of knownVars
	 * @param knownVars
	 * @return
	 */
	public ArrayList<String> addToKnownVariables (ArrayList<String> knownVars) {
		ArrayList<String> newKnownVars = knownVars;
		for (String var : variables.keySet()) {
			if (!knownVars.contains(var)) { //Variable ist bisher nicht bekannt -> add
				newKnownVars.add(var);	
			}
		}
		return newKnownVars;
	}
	
	/**
	 * returns Profvalues in same order as knownVars (as String)
	 * @param knownVars
	 * @return
	 */
	public ArrayList<String> getProfvalues (ArrayList<String> knownVars) {
		calcProfValues();
		ArrayList<String> profvalues = new ArrayList<String>();
		for (int i = 0; i < knownVars.size(); i++) {
			//has Patient this Variable? If not, return ""
			if (variables.get(knownVars.get(i)) == null) {
				profvalues.add("");
			} else { 
				profvalues.add(String.valueOf(variables.get(knownVars.get(i)).getProfvalue())); //gets the Profil-value from PatientVariable, ordered by KnownVars order
			}
		}
		return profvalues;
	}
}
	


/**
 * The Class Patient.
 * Represents a patient with information / variables for all models that are currently processed
 * 
 * One patient is processed at a time (inputfiles are sorted)
 */
public class Patient {
	
	/** The models. */
	private HashMap<Model,PatientModel> models = new HashMap<Model,PatientModel>();
	
	/** The pid. */
	private String pid;
	
	/**
	 * Instantiates a new patient.
	 *
	 * @param pid the pid
	 */
	public Patient (String pid) {
		this.pid = pid;
	}
	
	/**
	 * Checks if is patient.
	 *
	 * @param newpid the newpid
	 * @return true, if is patient
	 */
	public boolean isPatient (String newpid) {
		return pid.equals(newpid);
	}
	
	/**
	 * Process row.
	 *
	 * @param model the model
	 * @param inputfile the inputfile
	 */
	public void processRow (Model model, InputFile inputfile) {
		//Patient does not have that model yet -> create
		if (models.get(model) == null) {
			PatientModel mypatientmodel = new PatientModel(model);
			this.models.put(model,mypatientmodel);
		}
		//Get fields that need to be looked at for that model/inputfile
		Set<String> modelFields = model.getFields(inputfile);
		for (String nextfield : modelFields) {
			//get value for the field
			String value = inputfile.getValue(nextfield);
			//new Var only if value is not empty
			if (!value.equals("")) {
				//get aggTyp for the field
				String aggType = model.getAgg4ModelField(inputfile,nextfield);
				//Identify real variables based on inputfile, field, value)
				//could be more than one
				HashMap<String,String> realVariables = model.getVariables(inputfile, nextfield, value); 
				for (Entry<String, String> nextvar : realVariables.entrySet()) {
					this.models.get(model).addVariable(nextvar.getKey(),nextvar.getValue(),aggType,model.getCoeff(nextvar.getKey()));
				}
			}
		}
	}
	
	/**
	 * Gets the coeff sum.
	 *
	 * @param model the model
	 * @return the coeff sum
	 */
	public double getCoeffSum (Model model) {
		return this.models.get(model).getCoeffSum();
	}
	
	/**
	 * Gets the pid.
	 *
	 * @return the pid
	 */
	public String getPid() {
		return this.pid;
	}
	
	/**
	 * Gets the.
	 *
	 * @return the string
	 */
	public String get() {
		return this.pid;
	}
	
	/**
	 * adds Variables from that model to the list of knownVars
	 *
	 * @param model the model
	 * @param knownVars the known vars
	 * @return the array list
	 */
	public ArrayList<String> addToKnownVariables (Model model, ArrayList<String> knownVars) {
		return models.get(model).addToKnownVariables(knownVars);
	}
	
	/**
	 * Gets the profvalues.
	 *
	 * @param model the model
	 * @param knownVars the known vars
	 * @return the profvalues
	 */
	public  ArrayList<String> getProfvalues (Model model, ArrayList<String> knownVars) {
		return models.get(model).getProfvalues(knownVars);
	}
	
}