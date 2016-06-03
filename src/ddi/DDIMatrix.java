package ddi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.joda.time.LocalDate;

import configuration.Utils;


class Drug {
	private String id;
	private int days_reach;
	private String dose ="";
	private HashSet<Substance> substances = new HashSet<Substance>();
	
	public Drug(String id,int standardreach) {
		this.id=id;
		this.days_reach=standardreach;
	}
	
	public void setInformation (int reach, String dose) {
		this.days_reach=reach;
		this.dose=dose;
	}
	
	public void addSubstance (Substance subs) {
		substances.add(subs);
	}
	
	public void traverseLiveDrug (LocalDate date) {
		for (Substance s : substances) {
			s.traverseLiveSubstance (date, days_reach,dose);
		}
	}
	
	public int getReach() {
		return days_reach;
	}
	
	
	public String getDose () {
		return dose;
	}
	
}


class Substance {
	private String id;
	private HashSet<Interaction> interactions = new HashSet<Interaction>();
	
	public Substance(String id) {
		this.id=id;
	}
	
	public void addInteraction(Interaction in) {
		interactions.add(in);
	}
	public void traverseLiveSubstance (LocalDate date, int days_reach,String dose) {
		for (Interaction in : interactions) {
			in.addSubstanceHave(this, date, days_reach,dose);
		}
	}
	
	public void clearUnverifiedInteractions () {
		Iterator<Interaction> iterator = interactions.iterator();
		while (iterator.hasNext()) {
			Interaction in = iterator.next();
			if (!in.isVerified()) iterator.remove();
		}
	}
}

class HaveSubstance {
	public Substance sub;
	public LocalDate date;
	public int days_reach =0;
	public String dose;
	
	public HaveSubstance(Substance sub, LocalDate date, int days_reach, String dose) {
		this.sub = sub;
		this.date=date;
		this.days_reach=days_reach;
		this.dose=dose;
	}
}

class SubComp implements Comparator<HaveSubstance>{
    @Override
    public int compare(HaveSubstance e1, HaveSubstance e2) {
    	return e1.date.compareTo(e2.date);
    	/*if(e2.date.compareTo(arg0) > e1.date) return 1;
    	if(e2.date < e1.date) return -1;
        return 0;*/
    }
}


class Interaction {
	private String id;
	private HashSet<Substance> subs_must = new HashSet<Substance>();
	private ArrayList<HaveSubstance> subs_have = new ArrayList<HaveSubstance>();
	private MetaInteraction meta;
	private ArrayList<Index> indexes= new ArrayList<Index>();
	private boolean indexesAreCalculated = false;
	
	private boolean isVerified = false;
	
	
	
	public Interaction(String id) {
		this.id=id;
	}
	

	public void addSubstanceMust(Substance sub) {
		this.subs_must.add(sub);
	}
	

	public void setMeta(MetaInteraction meta) {
		this.meta = meta;
	}
	
	public MetaInteraction getMeta() {
		return meta;
	}
	
	
	public void addSubstanceHave(Substance sub,LocalDate date, int days_reach,String dose) {
		if (subs_must.contains(sub)) {
			HaveSubstance haveSub = new HaveSubstance(sub,date,days_reach,dose);
			subs_have.add(haveSub);
			indexesAreCalculated = false;
		}
	}
	
	
	public void clear() {
		subs_have.clear();
		indexes.clear();
		indexesAreCalculated=false;
	}
	
	class IndexOpportunity {
		public LocalDate start;
		public LocalDate end;
		public HashSet<Substance> subs_have = new HashSet<Substance>();
		public String max_dose = "";
		
		public IndexOpportunity(Substance currentsub, LocalDate start,LocalDate end) {
			subs_have.add(currentsub);
			this.start = start;
			this.end = end;
		}
		
		public void addDose (String dose) {
			if (dose.compareTo(max_dose)>0) max_dose=dose;
		}
		
		public String getDose() {
			return max_dose;
		}
	}
	
	
	public void calcIndexes() {
		/*Does:
		 *... check if all subs_must are there
		 *... include start and reach-times 
		 *...create indexes, might be multiple
		 */
		if (subs_must.size()>subs_have.size()) return;
		//sort ArrayList by date
		Collections.sort(subs_have, new SubComp());
		//create Array of IndexOpportunities
		ArrayList<IndexOpportunity> indexOpps = new ArrayList<IndexOpportunity>();
		IndexOpportunity newindex;
		for (HaveSubstance sub : subs_have) {
			//add substance to all previous IndexOpportunities, if within timeframe
			for (IndexOpportunity io : indexOpps) {
				if (sub.date.compareTo(io.end) <= 0) io.subs_have.add(sub.sub);
			}
			//create new IndexOpportunity
			newindex = new IndexOpportunity(sub.sub,sub.date,sub.date.plusDays(sub.days_reach));
			newindex.addDose(sub.dose);
			indexOpps.add(newindex);
		}
		//now find all real index dates -> if all substances are included (simple size comparison)
		Index realindex;
		for (IndexOpportunity io : indexOpps) {
			if (io.subs_have.size()==this.subs_must.size()) {
				realindex = new Index(io.start, io.end, this.id,this.meta.getId());
				realindex.setDose(io.getDose());
				indexes.add(realindex);
			}
		}
		indexesAreCalculated = true;
	}
	
	public ArrayList<Index> getIndexes() {
		if (!indexesAreCalculated) calcIndexes();
		return indexes;
	}
	
	public void verify() {
		this.isVerified=true;
	}
	
	public boolean isVerified() {
		return this.isVerified;
	}
	
	public String getID() {
		return this.id;
	}
	
	public int getNumSubstances() {
		return this.subs_must.size();
	}
	
	public HashSet<Substance> getSubsMust() {
		return this.subs_must;
	}

	
}

class MetaInteraction {
	private String id;
	private HashSet<Interaction> interactions= new HashSet<Interaction>();
	
	public MetaInteraction( String id) {
		this.id=id;
	}
	
	public String getId() {
		return id;
	}
	
	public void addInteraction(Interaction i) {
		this.interactions.add(i);
	}
	
	public int getNumInteractions() {
		return this.interactions.size();
	}
	
	public int getNumSubstances() {
		HashSet<Substance> subs = new HashSet<Substance>();
		for (Interaction i : interactions) {
			subs.addAll(i.getSubsMust());
		}
		return subs.size();
	}
}



class IndexComp implements Comparator<Index>{
    @Override
    public int compare(Index e1, Index e2) {
    	return e1.getStartDate().compareTo(e2.getStartDate());
    	/*if(e2.getStart() > e1.getStart()) return 1;
    	if(e2.getStart() < e1.getStart()) return -1;
        return 0;*/
    }
}



public class DDIMatrix {
	private final static Logger LOGGER = Logger.getLogger(DDIMatrix.class.getName());
	
	HashMap<String,Drug> drugs = new HashMap<String,Drug> ();
	HashMap<String,Substance> substances = new HashMap<String,Substance> ();
	HashMap<String,Interaction> interactions = new HashMap<String,Interaction> ();
	HashMap<String,MetaInteraction> metas = new HashMap<String,MetaInteraction> ();
	
	private int drugreach_standard;
	
	
	
	public  DDIMatrix(String drugreach_standard) {
		int reach = Integer.parseInt(drugreach_standard);
		this.drugreach_standard=reach;
	}
	
	//init
	
	public void addDrug2Substance (String drug_id, String sub_id) {
		if (!drugs.containsKey(drug_id)) {
			Drug drug = new Drug (drug_id,drugreach_standard);
			this.drugs.put(drug_id, drug);
		}
		if (!substances.containsKey(sub_id)) {
			Substance sub = new Substance (sub_id);
			this.substances.put(sub_id, sub);
		}
		this.drugs.get(drug_id).addSubstance(this.substances.get(sub_id));
	}
	
	
	public void addDrugInformation(String drug_id, String reach, String dose) {
		//parse reach ... from Double to int
		int reachint;
		if (reach.isEmpty()) reachint=drugreach_standard;
		else {
			Double reachdouble = Double.parseDouble(reach);
			reachint = reachdouble.intValue();
			if (reachint<=0) reachint=drugreach_standard;
		}
		if (!drugs.containsKey(drug_id)) {
			Drug drug = new Drug (drug_id,drugreach_standard);
			this.drugs.put(drug_id, drug);
		}
		if (dose==null) dose="";
		this.drugs.get(drug_id).setInformation(reachint,dose);
	}
	
	
	
	public void addSubstance2Interaction (String sub_id, String in_id) {
		if (!substances.containsKey(sub_id)) {
			Substance sub = new Substance (sub_id);
			this.substances.put(sub_id, sub);
		}
		if (!interactions.containsKey(in_id)) {
			Interaction in = new Interaction (in_id);
			this.interactions.put(in_id, in);
		}
		this.substances.get(sub_id).addInteraction(this.interactions.get(in_id));
		this.interactions.get(in_id).addSubstanceMust(this.substances.get(sub_id));
	}
	
	public void addInteraction2Meta (String in_id, String meta_id) {
		if (!interactions.containsKey(in_id)) {
			Interaction in = new Interaction (in_id);
			this.interactions.put(in_id, in);
		}
		if (!metas.containsKey(meta_id)) {
			MetaInteraction met = new MetaInteraction (meta_id);
			this.metas.put(meta_id, met);
		}
		this.interactions.get(in_id).setMeta(this.metas.get(meta_id));
		this.metas.get(meta_id).addInteraction(this.interactions.get(in_id));
	}
	
	//live data
	
	public void clearAll() {
		for (Interaction in : interactions.values()) {
			in.clear();
		}
	}
	
	
	public void addLiveDrug (String id, String date) {
		//add only if part of matrix
		if (this.drugs.containsKey(id)) {
			//parse date
			LocalDate startdate = Utils.parseDate(date);
			this.drugs.get(id).traverseLiveDrug(startdate);
		}
	}
	
	public ArrayList<Index> getLiveInteractionIndexes() {
		ArrayList<Index> indexes = new ArrayList<Index>();
		//get all Index for all Interactions
		for (Interaction in : interactions.values()) {
			//double-check for verified interaction (verification and remove happens during read-in of base-matrix, however better safe than sorry)
			if (in.isVerified()) indexes.addAll(in.getIndexes());
		}
		//now adjust overlapping end dates (cut index end)
		Collections.sort(indexes,new IndexComp());
		for (int i=1; i<indexes.size(); i++) {
			if (indexes.get(i).getStartDate().compareTo(indexes.get(i-1).getEndDate()) < 0) indexes.get(i-1).setEnd(indexes.get(i).getStartDate().minusDays(1));
		}
		
		return indexes;
	}
	
	public void verifyInteraction (String in_id) {
		if (!interactions.containsKey(in_id)) {
			Interaction in = new Interaction (in_id);
			this.interactions.put(in_id, in);
		}
		this.interactions.get(in_id).verify();
	}
	
	
	public void clearAllUnverifiedInteractions() {
		//remove from all lists (first from here)
		Iterator<HashMap.Entry<String, Interaction>> iterator = interactions.entrySet().iterator();
		while (iterator.hasNext()) {
			HashMap.Entry<String, Interaction> entry = iterator.next();
			if (!entry.getValue().isVerified()) iterator.remove();
		}
		//second from substances
		for (Substance sub : substances.values()) {
			sub.clearUnverifiedInteractions();
		}
	}
	
	public int getNumberDrugs() {
		return drugs.size();
	}
	
	public int getNumberSubstances() {
		return substances.size();
	}
	
	public int getNumberInteractions() {
		return interactions.size();
	}
	
	public int getNumberMetas() {
		return metas.size();
	}
	
	public Set<String> getMetaIds() {
		return this.metas.keySet();
	}
	
	public Set<String> getInteractionIds() {
		return this.interactions.keySet();
	}
	
	public int getNumInteractions4Meta (String id) {
		return this.metas.get(id).getNumInteractions();
	}
	
	public int getNumSubstances4Meta (String id) {
		//must be calculated
		return this.metas.get(id).getNumSubstances();
	}
	
	public int getNumSubstances4Interaction (String id) {
		return this.interactions.get(id).getNumSubstances();
	}
	
	public int getDrugreach_standard() {
		return drugreach_standard;
	}
	
	
}
