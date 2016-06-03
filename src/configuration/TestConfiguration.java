package configuration;


public class TestConfiguration {
	private Configuration config;
	
	public TestConfiguration (Configuration config) {
		this.config=config;
	}
	
	public boolean test() {
		boolean ok = true;
		
		//check required parameters
		ok = ok && config.getModelpath()!=null;
		ok = ok && config.getOutputPath()!=null;
		
		if (!ok) {
			System.err.println("Missing configuration options. See example.xml for all required options.");
			return ok;
		}
		
		//check datafiles
		for (Datafile df : config.getInputfiles()) {
			ok = ok && df.getDatentyp()!=null;
			ok = ok && df.getPath()!=null;
			ok = ok && df.getFiletype()!=null && (df.getFiletype().equals(Consts.csvFlag) || df.getFiletype().equals(Consts.satzartFlag));
			if (!ok) {
				System.err.println("Error with configuration: Datafile "+ df.getDatentyp());
				return ok;
			}
		}
		
		//check DDI
		DDIConfiguration ddiconfig = config.ddiconfiguration;
		if (ddiconfig!= null) {
			ok = ok && ddiconfig.getDatentyp()!=null;
			ok = ok && ddiconfig.getFilePatient2drug()!=null;
			ok = ok && ddiconfig.getFileDruginfo()!=null;
			ok = ok && ddiconfig.getFileDrug2substance()!=null;
			ok = ok && ddiconfig.getFileSubstance2interaction()!=null;
			ok = ok && ddiconfig.getFileInteractioninfo()!=null;
			ok = ok && ddiconfig.getFileInteraction2meta()!=null;
			ok = ok && ddiconfig.getDrugfield()!=null;
			ok = ok && ddiconfig.getDatefield()!=null;
			ok = ok && ddiconfig.getDrugreachfield()!=null;
			ok = ok && ddiconfig.getDrugreachStandard()!=null;
			ok = ok && ddiconfig.getSubstancefield()!=null;
			ok = ok && ddiconfig.getInteractionfield()!=null;
			ok = ok && ddiconfig.getMeta_interactionfield()!=null;
			ok = ok && ddiconfig.getStartfield()!=null;
			ok = ok && ddiconfig.getEndfield()!=null;
			if (!ok) {
				System.err.println("Error with DDI configuration. See example.xml for all required options.");
				return ok;
			}
		}		
		
		return ok;
	}

}
