/**
 * 
 */
package ddi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import models.Model;

class TargetStat {
	public int num=0;
	public String lastid="";
	
	public TargetStat (int num, String id) {
		this.num=num;
		this.lastid=id;
	}
	
	public void addNum(String newid) {
		if (!lastid.equals(newid)) {
			lastid=newid;
			num ++;
		}
	}
	
}

class InteractionStat {
	public String id;
	public int num_interactions=0;
	public int num_substances=0;
	public int num_patients=0;
	public int num_occurence=0;
	public HashMap<Model,HashMap<String,Integer>> targets_occ = new HashMap<Model,HashMap<String,Integer>> ();
	public HashMap<Model,HashMap<String,TargetStat>> targets_patients = new HashMap<Model,HashMap<String,TargetStat>> ();
	private String lastPatientId="";
	
	public InteractionStat(String id) {
		this.id=id;
	}
	
	public void setBaseInfo(int num_interactions,int num_substances) {
		this.num_interactions=num_interactions;
		this.num_substances=num_substances;
	}

	
	
	public void addOccurence(String patient_id) {
		this.num_occurence++;
		if (!patient_id.equals(lastPatientId)) {
			lastPatientId=patient_id;
			this.num_patients++;
		}
	}
	
	public void addTarget(Model model, String patient_id, String target_id) {
		//count occurence
		if (!targets_occ.containsKey(model)) {
			targets_occ.put(model, new HashMap<String,Integer>());
		}
		if (!targets_occ.get(model).containsKey(target_id)) {
			this.targets_occ.get(model).put(target_id, 1);
		} else 
			this.targets_occ.get(model).put(target_id,targets_occ.get(model).get(target_id)+1);
		//count patients
		if (!targets_patients.containsKey(model)) {
			targets_patients.put(model, new HashMap<String,TargetStat>());
		}
		if (!targets_patients.get(model).containsKey(target_id)) {
			this.targets_patients.get(model).put(target_id, new TargetStat(1, patient_id));
		} else targets_patients.get(model).get(target_id).addNum(patient_id);
			
	}
	
	public int getNumTargetsPatients (Model model, String target_id) {
		if (!targets_patients.containsKey(model) || !targets_patients.get(model).containsKey(target_id)) return 0;
		return targets_patients.get(model).get(target_id).num;
	}
	
	public int getNumTargetsOcc (Model model, String target_id) {
		if (!targets_occ.containsKey(model) || !targets_occ.get(model).containsKey(target_id)) return 0;
		return targets_occ.get(model).get(target_id);
	}
}



/**
 * @author HellwigP
 *
 */
public class DDIStats {
	private int num_patients=0;
	private int num_occurences=0;
	public HashMap<Model,HashMap<String,TargetStat>> targets_allpatients = new HashMap<Model,HashMap<String,TargetStat>> ();
	private String lastPatientId="";
	
	HashMap<String,InteractionStat> metas = new HashMap<String,InteractionStat> ();
	HashMap<String,InteractionStat> interactions = new HashMap<String,InteractionStat> ();

	public DDIStats() {
		
	}
	
	public void addMeta(String meta_id, int num_interactions,int num_substances) {
		if (!metas.containsKey(meta_id)) {
			InteractionStat m = new InteractionStat (meta_id);
			this.metas.put(meta_id, m);
		}
		this.metas.get(meta_id).setBaseInfo(num_interactions, num_substances);
	}
	
	public void addInteraction(String interaction_id,int num_substances) {
		if (!interactions.containsKey(interaction_id)) {
			InteractionStat m = new InteractionStat (interaction_id);
			this.interactions.put(interaction_id, m);
		}
		this.interactions.get(interaction_id).setBaseInfo(1,num_substances);
	}
	
	/*
	 * add statistic for 1 occurence of interaction
	 * 
	 * and also maintains patient stats (by last patient index)
	 */
	public void addOccurence(String meta_id, String interaction_id, String patient_id) {
		if (meta_id!= null && interaction_id!=null && !meta_id.isEmpty() && !interaction_id.isEmpty()) {
			if (!metas.containsKey(meta_id)) {
				InteractionStat m = new InteractionStat (meta_id);
				this.metas.put(meta_id, m);
			}
			this.metas.get(meta_id).addOccurence(patient_id);
			if (!interactions.containsKey(interaction_id)) {
				InteractionStat m = new InteractionStat (interaction_id);
				this.interactions.put(interaction_id, m);
			}
			this.interactions.get(interaction_id).addOccurence(patient_id);
			this.num_occurences++;
		}
		if (!patient_id.equals(lastPatientId)) {
			lastPatientId=patient_id;
			this.num_patients++;
		}
	}
	
	
	public void addTarget(Model model, String patient_id, String meta_id, String interaction_id, String target_id) {
		this.addTargetAll(model,patient_id, target_id);
		if (meta_id!= null && interaction_id!=null && !meta_id.isEmpty() && !interaction_id.isEmpty()) {
			this.metas.get(meta_id).addTarget(model,patient_id,target_id);
			this.interactions.get(interaction_id).addTarget(model,patient_id,target_id);
		}
	}
	
	private void addTargetAll(Model model, String patient_id, String target_id) {
		if (!targets_allpatients.containsKey(model)) {
			targets_allpatients.put(model, new HashMap<String,TargetStat>());
		}
		if (!targets_allpatients.get(model).containsKey(target_id)) {
			this.targets_allpatients.get(model).put(target_id, new TargetStat(1, patient_id));
		} else targets_allpatients.get(model).get(target_id).addNum(patient_id);
	}
	
	
	public Set<String> getMetaIds() {
		return this.metas.keySet();
	}
	
	public Set<String> getInteractionIds() {
		return this.interactions.keySet();
	}
	
	
	public int getNumberTargetKeys(Model model) {
		if (!targets_allpatients.containsKey(model)) return 0;
		return this.targets_allpatients.get(model).size();
	}
	
	public Set<String> getTargetNames(Model model) {
		if (!targets_allpatients.containsKey(model)) return Collections.<String>emptySet();;
		return this.targets_allpatients.get(model).keySet();
	}
	
	
	public int getNumInteractions (String id, boolean ismeta) {
		if (ismeta) return metas.get(id).num_interactions;
		else return interactions.get(id).num_interactions;
	}
	
	public int getNumSubs (String id, boolean ismeta) {
		if (ismeta) return metas.get(id).num_substances;
		else return interactions.get(id).num_substances;
	}
	

	public int getNumPatients (String id, boolean ismeta) {
		if (ismeta) return metas.get(id).num_patients;
		else return interactions.get(id).num_patients;
	}
	
	public int getNumPatientsAll () {
		return this.num_patients;
	}
	
	public int getNumOccurences (String id, boolean ismeta) {
		if (ismeta) return metas.get(id).num_occurence;
		else return interactions.get(id).num_occurence;
	}
	
	public int getNumOccurencesAll () {
		return this.num_occurences;
	}
	
	public int getNumTargetsPatients (Model model,String id, boolean ismeta, String target) {
		if (ismeta) return metas.get(id).getNumTargetsPatients(model,target);
		else return interactions.get(id).getNumTargetsPatients(model,target);
	}
	
	public int getNumTargetsOcc (Model model,String id, boolean ismeta, String target) {
		if (ismeta) return metas.get(id).getNumTargetsOcc(model,target);
		else return interactions.get(id).getNumTargetsOcc(model,target);
	}
	
	public int getNumTargetsAllPatients (Model model,String target) {
		if (!targets_allpatients.containsKey(model)) return 0;
		return targets_allpatients.get(model).get(target).num;
	}
	
}
