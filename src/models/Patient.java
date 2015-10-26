package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import configuration.Consts;


/**
 * The Class PatientModel.
 * Represents a full model with all variables for a patient, as read in when processing inputfiles
 * 
 * can return the coeff sum (i.e. risk score) for this patient and model
 */
class PatientModel {
	private HashMap<String,Variable> variables = new HashMap<String,Variable>(); //variablename -> PatientVariable
	private Model model;
	private boolean profValuesAreCalculated=false;
	private boolean amIincluded=true;
	private boolean amIexcluded=false;

	
	public PatientModel(Model model) {
		this.model = model;
		if (model.hasInclusion()) amIincluded=false;
	}
	
	
	public void updateVariables(InputFile inputfile) {
		profValuesAreCalculated=false;
		variables = this.model.updateVariables(inputfile, variables);
	}	
	

	private void calcProfValues () {
		if (!profValuesAreCalculated) {
			//1. Determine calculation sequence
			List<String> allVars = new ArrayList<String>(variables.keySet());
			@SuppressWarnings("unchecked")
			List<String>[] rounds = (ArrayList<String>[])new ArrayList[5];
			List<String> allIncludedVars = new ArrayList<String>();
			for (int i=0; i<rounds.length; i++) {
				rounds[i] = new ArrayList<String>();
				if (allVars.size()>0) {
					for (String var : allVars) {
						if (i==0 && !variables.get(var).dependsOnOtherVars()) rounds[i].add(var);
						else if (i>0 && rounds[i-1].containsAll(variables.get(var).getOtherVarsDependent())) rounds[i].add(var);
					}
					allVars.removeAll(rounds[i]);
					allIncludedVars.addAll(rounds[i]);
				}
			}
			rounds[rounds.length-1].addAll(allVars); // add all remaining; hopefully should be able to calc
			//2. Now round for round: calc values
			for (int i=0; i<rounds.length; i++) {
				for (String var : rounds[i]) {
					variables.get(var).calcProfvalue(variables);
					if (variables.get(var).isAllowed() && variables.get(var).isInclude()) amIincluded=true;
					if (variables.get(var).isAllowed() && variables.get(var).isExclude()) amIexcluded=true;
				}
			}
			profValuesAreCalculated = true;
		}
	}
	
	
	public double getCoeffSum () {
		calcProfValues();
		double mysum = model.getCoeffIntercept();
		for (String var : variables.keySet()) {
			if (!variables.get(var).hideme() && variables.get(var).isAllowed() && !variables.get(var).isTarget())
				mysum += variables.get(var).getCalcCoeff(model.getCoeff(var));
		} 
		if (this.model.getType().equals(Consts.logRegFlag)) {
			mysum = Math.exp(mysum) / (1+Math.exp(mysum));
		}
		return mysum;
	}
	
	/**
	 * adds Variables from that model to the list of knownVars
	 * @param knownVars, isTarget (find target vars?)
	 * @return
	 */
	public ArrayList<String> addToKnownVariables (ArrayList<String> knownVars, boolean isTarget) {
		ArrayList<String> newKnownVars = knownVars;
		for (String var : variables.keySet()) {
			if (!knownVars.contains(var) && !variables.get(var).hideme() && variables.get(var).isAllowed() && variables.get(var).isTarget()==isTarget) { //Variable ist bisher nicht bekannt Und ist gültig -> add
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
			if (variables.get(knownVars.get(i)) == null || !variables.get(knownVars.get(i)).isAllowed()) {
				profvalues.add(Consts.navalue);
			} else { 
				profvalues.add(String.valueOf(variables.get(knownVars.get(i)).getProfvalue())); //gets the Profil-value from PatientVariable, ordered by KnownVars order
			}
		}
		return profvalues;
	}
	
	public boolean amIincluded() {
		calcProfValues();
		return amIincluded && !amIexcluded;
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
	public void processRow (Model model, InputFile inputfile) throws Exception {
		//Patient does not have that model yet -> create
		if (models.get(model) == null) {
			PatientModel mypatientmodel = new PatientModel(model);
			this.models.put(model,mypatientmodel);
		}
		this.models.get(model).updateVariables(inputfile);
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
	 * Gets the profvalues.
	 *
	 * @param model the model
	 * @param knownVars the known vars
	 * @return the profvalues
	 */
	public  ArrayList<String> getProfvalues (Model model, ArrayList<String> knownVars) {
		return models.get(model).getProfvalues(knownVars);
	}
	
	public boolean areYouIncluded (Model model) {
		return models.get(model).amIincluded();
	}
	
	public ArrayList<String> addToKnownVariables (Model m, ArrayList<String> knownVars, boolean isTarget) {
		return models.get(m).addToKnownVariables(knownVars,isTarget);
	}
	
}