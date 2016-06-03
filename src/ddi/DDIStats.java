/**
 * 
 */
package ddi;

import java.util.HashMap;
import java.util.Set;

class InteractionStat {
	public String id;
	public int num_interactions=0;
	public int num_substances=0;
	public int num_patients=0;
	public int num_occurence=0;
	public HashMap<String,Integer> targets = new HashMap<String,Integer> ();
	private String lastPatientId="";
	
	public InteractionStat(String id) {
		this.id=id;
	}
	
	public void setBaseInfo(int num_interactions,int num_substances) {
		this.num_interactions=num_interactions;
		this.num_substances=num_substances;
	}

	
	private void addPatient() {
		this.num_patients++;
	}
	
	public void addOccurence(String patient_id) {
		this.num_occurence++;
		if (!patient_id.equals(lastPatientId)) {
			lastPatientId=patient_id;
			this.addPatient();
		}
	}
	
	public void addTarget(String id) {
		if (!targets.containsKey(id)) {
			this.targets.put(id, 1);
		} else 
			this.targets.put(id, this.targets.get(id)+1);
	}
	
	public int getTargetNum(String id) {
		if (!targets.containsKey(id)) return 0;
		return this.targets.get(id);
	}
}



/**
 * @author HellwigP
 *
 */
public class DDIStats {
	private int num_patients=0;
	private int num_occurences=0;
	public HashMap<String,Integer> targets_all = new HashMap<String,Integer> ();
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
		if (!patient_id.equals(lastPatientId)) {
			lastPatientId=patient_id;
			this.num_patients++;
		}
	}
	
	public void addPatientNoInteractions () {
		this.num_patients++;
	}
	
	public void addTarget(String meta_id, String interaction_id, String target_id) {
		this.addTargetAll(target_id);
		if (meta_id!= null && interaction_id!=null && !meta_id.isEmpty() && !interaction_id.isEmpty()) {
			this.metas.get(meta_id).addTarget(target_id);
			this.interactions.get(interaction_id).addTarget(target_id);
		}
	}
	
	private void addTargetAll(String id) {
		if (!targets_all.containsKey(id)) {
			this.targets_all.put(id, 1);
		} else 
			this.targets_all.put(id, this.targets_all.get(id)+1);
	}
	
	
	public Set<String> getMetaIds() {
		return this.metas.keySet();
	}
	
	public Set<String> getInteractionIds() {
		return this.interactions.keySet();
	}
	
	
	public int getNumberTargetKeys() {
		return this.targets_all.size();
	}
	
	public Set<String> getTargetNames() {
		return this.targets_all.keySet();
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
	
	public int getNumTargets (String id, boolean ismeta, String target) {
		if (ismeta) return metas.get(id).getTargetNum(target);
		else return interactions.get(id).getTargetNum(target);
	}
	
	public int getNumTargetsAll (String target) {
		return targets_all.get(target);
	}
	
}
