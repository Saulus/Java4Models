package configuration;

import java.util.List;

import org.xmappr.Element;
import org.xmappr.RootElement;


/**
 * The Class Konfiguration.
 * Elements from konfiguration.xml are loaded into here, into variables defined acc. to xml tag.
 * Class for Tags konfiguration
 */
@RootElement
public class Konfiguration {
	
	/** The model coeff ext. */
	private String modelCoeffExt = ".coeff";
	
	/** The model config ext. */
	private String modelConfigExt = ".config";
	
	/** The profilfile dense. */
	private String profilfileDense = "profil_dense";
	
	/** The profilfile sparse. */
	private String profilfileSparse = "profil_matlab";
	
	/** The profilfile sparse col. */
	private String profilfileSparseCol = "profil_matlab_cols";
	
	/** The profilfile sparse row. */
	private String profilfileSparseRow = "profil_matlab_rows";
	
	private String profilfileSvmlight = "profil_svmlight";
	private String profilfileSvmlightHeadExt = "_head";
	
	/** The input. */
	@Element
	public Input input;
	
	/** The outputpfad. */
	@Element(defaultValue="C:\\")
	public String outputpfad;
	
	/** The modellpfad. */
	@Element(defaultValue="C:\\")
	public String modellpfad;
	
	/** The create profil dense. */
	@Element(defaultValue="false")
	public boolean createProfilDense;
	
	/** The create profil sparse. */
	@Element(defaultValue="false")
	public boolean createProfilMatlab;
	
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
	
	/**
	 * Gets the inputfiles.
	 *
	 * @return the inputfiles
	 */
	public List<Datei> getInputfiles() {
		return input.datei;
	}
	
	/**
	 * Gets the outputfile.
	 *
	 * @return the outputfile
	 */
	public String getOutputfile() {
		if (outputfile==null) outputfile = "scores.csv";
		return outputpfad + "\\" + outputfile;
	}
	
	/**
	 * Gets the modelpath.
	 *
	 * @return the modelpath
	 */
	public String getModelpath() {
		return modellpfad;
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
			return outputpfad + "\\" + model + profilfileDense + "_targets.csv";
		else 
			return outputpfad + "\\" + model + profilfileDense + ".csv";
	}
	
	/**
	 * Gets the profilfile dense tmp.
	 *
	 * @param model the model
	 * @return the profilfile dense tmp
	 */
	public String getProfilfileDenseTmp(String model, boolean targets) {
		if (targets)
			return outputpfad + "\\" + model + profilfileDense + "_targets.tmp";
		else 
			return outputpfad + "\\" + model + profilfileDense + ".tmp";
	}
	
	/**
	 * Gets the profilfile sparse.
	 *
	 * @param model the model
	 * @return the profilfile sparse
	 */
	public String getProfilfileSparse(String model, boolean targets) {
		if (targets)
			return outputpfad + "\\" + model + profilfileSparse + "_targets.csv";
		else 
			return outputpfad + "\\" + model + profilfileSparse + ".csv";
	}
	
	/**
	 * Gets the profilfile sparse co ls.
	 *
	 * @param model the model
	 * @return the profilfile sparse co ls
	 */
	public String getProfilfileSparseCOLs(String model, boolean targets) {
		if (targets)
			return outputpfad + "\\" + model + profilfileSparseCol + "_targets.csv";
		else 
			return outputpfad + "\\" + model + profilfileSparseCol + ".csv";
	}
	
	/**
	 * Gets the profilfile sparse ro ws.
	 *
	 * @param model the model
	 * @return the profilfile sparse ro ws
	 */
	public String getProfilfileSparseROWs(String model) {
		return outputpfad + "\\" + model + profilfileSparseRow + ".csv";
	}
	
	
	public String getProfilfileSvmlight(String model, boolean targets) {
		if (targets)
			return outputpfad + "\\" + model + profilfileSvmlight + "_targets.csv";
		else 
			return outputpfad + "\\" + model + profilfileSvmlight + ".csv";
	}
	
	
	public String getProfilfileSvmlightHeader(String model, boolean targets) {
		if (targets)
			return outputpfad + "\\" + model + profilfileSvmlight + "_targets"+profilfileSvmlightHeadExt+".csv";
		else 
			return outputpfad + "\\" + model + profilfileSvmlight + profilfileSvmlightHeadExt+".csv";
	}
	
	
	public boolean createProfilDense() {
		return createProfilDense;
	}
	
	
	public boolean createProfilSparse() {
		return createProfilMatlab;
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
		return outputpfad;
	}

}
