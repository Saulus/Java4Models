package configuration;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.xmappr.Element;
import org.xmappr.RootElement;


/**
 * The Class Configuration.
 * Elements from konfiguration.xml are loaded into here, into variables defined acc. to xml tag.
 * Class for Tags konfiguration
 */
@RootElement
public class Configuration {
	
	/** The model coeff ext. */
	private String modelCoeffExt = ".coeff";
	
	/** The model config ext. */
	private String modelConfigExt = ".config";
	
	/** The profilfile dense. */
	private String profilfileDense = "profil_dense";
	
	/** The profilfile sparse. */
	private String profilfileSparse = "profil_mm";
	
	/** The profilfile sparse col. */
	private String profilfileSparseCol = "profil_mm_cols";
	
	/** The profilfile sparse row. */
	private String profilfileSparseRow = "profil_mm_rows";
	
	private String profilfileSvmlight = "profil_svmlight";
	private String profilfileSvmlightHeadExt = "_head";
	
	/** The inputfiles. */
	@Element
	public Inputfiles inputfiles;
	
	
	@Element
	public Inputfilter inputfilter;
	
	
	@Element
	public DDIConfiguration ddiconfiguration;
	
	
	/** The outputpath. */
	@Element
	public String outputpath;
	
	/** The modelpath. */
	@Element
	public String modelpath;
	
	/** The create profil dense. */
	@Element(defaultValue="false")
	public boolean createProfilDense;
	
	/** The create profil sparse. */
	@Element(defaultValue="false")
	public boolean createProfilMatrixMarket;
	
	@Element(defaultValue="false")
	public boolean createProfilSvmlight;
	
	/** The createScores. */
	@Element(defaultValue="true")
	public boolean createScores;
	
	/** The outputfile. */
	@Element(defaultValue="scores.csv")
	public String outputfile;
	
	
	@Element(defaultValue="INTERCEPT")
	public static String interceptname;
	
	/** The reference_date. 
	 * days are counted starting here
	 * */
	@Element(defaultValue="01JAN2006")
	public static String reference_date;
	
	/** The upcaser for all data fields. */
	@Element(defaultValue="true")
	public boolean upcase;
	
	@Element
	public String logfile;
	
	/** The upcaser for all data fields. */
	@Element(defaultValue="false")
	public boolean addPidToSvm;
	
	/**
	 * Gets the inputfiles.
	 *
	 * @return the inputfiles
	 */
	public List<Datafile> getInputfiles() {
		return inputfiles.datafile;
	}
	
	/**
	 * Gets the outputfile.
	 *
	 * @return the outputfile
	 */
	public String getOutputfile() {
		if (outputfile==null) outputfile = "scores.csv";
		return outputpath + "\\" + outputfile;
	}
	
	/**
	 * Gets the modelpath.
	 *
	 * @return the modelpath
	 */
	public String getModelpath() {
		return modelpath;
	}
	
	/**
	 * Gets the model coeff ext.
	 *
	 * @return the model coeff ext
	 */
	public String getModelCoeffExt() {
		return modelCoeffExt;
	}
	
	/**
	 * Gets the model config ext.
	 *
	 * @return the model config ext
	 */
	public String getModelConfigExt() {
		return modelConfigExt;
	}
	
	
	/**
	 * Gets the profilfile dense.
	 *
	 * @param model the model
	 * @return the profilfile dense
	 */
	public String getProfilfileDense(String model, boolean targets) {
		if (targets)
			return outputpath + "\\" + model + profilfileDense + "_targets.csv";
		else 
			return outputpath + "\\" + model + profilfileDense + ".csv";
	}
	
	/**
	 * Gets the profilfile dense tmp.
	 *
	 * @param model the model
	 * @return the profilfile dense tmp
	 */
	public String getProfilfileDenseTmp(String model, boolean targets) {
		if (targets)
			return outputpath + "\\" + model + profilfileDense + "_targets.tmp";
		else 
			return outputpath + "\\" + model + profilfileDense + ".tmp";
	}
	
	/**
	 * Gets the profilfile sparse.
	 *
	 * @param model the model
	 * @return the profilfile sparse
	 */
	public String getProfilfileSparse(String model, boolean targets) {
		if (targets)
			return outputpath + "\\" + model + profilfileSparse + "_targets.csv";
		else 
			return outputpath + "\\" + model + profilfileSparse + ".csv";
	}
	
	/**
	 * Gets the profilfile sparse co ls.
	 *
	 * @param model the model
	 * @return the profilfile sparse co ls
	 */
	public String getProfilfileSparseCOLs(String model, boolean targets) {
		if (targets)
			return outputpath + "\\" + model + profilfileSparseCol + "_targets.csv";
		else 
			return outputpath + "\\" + model + profilfileSparseCol + ".csv";
	}
	
	/**
	 * Gets the profilfile sparse ro ws.
	 *
	 * @param model the model
	 * @return the profilfile sparse ro ws
	 */
	public String getProfilfileSparseROWs(String model) {
		return outputpath + "\\" + model + profilfileSparseRow + ".csv";
	}
	
	
	public String getProfilfileSvmlight(String model, boolean targets) {
		if (targets)
			return outputpath + "\\" + model + profilfileSvmlight + "_targets.svm";
		else 
			return outputpath + "\\" + model + profilfileSvmlight + ".svm";
	}
	
	
	public String getProfilfileSvmlightHeader(String model, boolean targets) {
		if (targets)
			return outputpath + "\\" + model + profilfileSvmlight + "_targets"+profilfileSvmlightHeadExt+".svm";
		else 
			return outputpath + "\\" + model + profilfileSvmlight + profilfileSvmlightHeadExt+".svm";
	}
	
	
	public boolean createProfilDense() {
		return createProfilDense;
	}
	
	
	public boolean createProfilSparse() {
		return createProfilMatrixMarket;
	}
	
	public boolean createProfilSvmlight() {
		return createProfilSvmlight;
	}
	
	public boolean createScores() {
		return createScores;
	}
	
	//temp path for db if sorting
	/**
	 * Gets the tmp path.
	 *
	 * @return the tmp path
	 */
	public String getTmpPath() {
		return this.getOutputPath();
	}
	
	public String getOutputPath() {
		return outputpath;
	}
	
	public boolean upcase() {
		return upcase;
	}
	
	public boolean addPidToSvm() {
		return addPidToSvm;
	}
	
	public ArrayList<Filter> getFilters4File (String data_id) {
		if (inputfilter==null) return null;
		return inputfilter.getFilters4File(data_id);
	}
	
	public static LocalDate getReferenceDate() {
		if (Configuration.reference_date == null) 
			return Utils.parseDate(Consts.reference_date);
		else return Utils.parseDate(Configuration.reference_date);
	}
	
	
	public String getLogfile() {
		return logfile;
	}

}
