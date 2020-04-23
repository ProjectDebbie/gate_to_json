package es.bsc.inb.debbie.export.json.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonGenerationException;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Factory;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import gate.util.InvalidOffsetException;

/**
 * Generic Export from GATE to JSON.
 * 
 * For Batch processing
 * 
 * 
 * @author jcorvi
 * This version has been modified for the DEBBIE pipeline
 * The corresponding annotation categories have been added at the end of the file.  
 */
public class App {

	public static void main(String[] args ){

    	Options options = new Options();

        Option input = new Option("i", "input", true, "input directory path");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "output directory path");
        output.setRequired(true);
        options.addOption(output);

        Option workdir = new Option("workdir", "workdir", true, "workDir directory path");
        workdir.setRequired(false);
        options.addOption(workdir);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
    	try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        String inputFilePath = cmd.getOptionValue("input");
        String outputFilePath = cmd.getOptionValue("output");
        String workdirPath = cmd.getOptionValue("workdir");
        if (!java.nio.file.Files.isDirectory(Paths.get(inputFilePath))) {
    		System.out.println("Please set the inputDirectoryPath ");
			System.exit(1);
    	}


    	File outputDirectory = new File(outputFilePath);
	    if(!outputDirectory.exists())
	    	outputDirectory.mkdirs();

	    try {
			Gate.init();
		} catch (GateException e) {
			System.out.println("App::main :: Gate Exception  ");
			e.printStackTrace();
			System.exit(1);
		}

	    if(workdirPath==null) {
	    	workdirPath="";
	    }

		try {
			process(inputFilePath, outputFilePath,workdirPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Process directory and convert XML GATE format to JSON 
	 * @param properties_parameters_path
     * @throws IOException 
	 */
	public static void process(String inputDirectoryPath, String outputDirectoryPath, String workdir) throws IOException {
    	System.out.println("App::processTagger :: INIT ");
		if (java.nio.file.Files.isDirectory(Paths.get(inputDirectoryPath))) {
			File inputDirectory = new File(inputDirectoryPath);
			File[] files =  inputDirectory.listFiles();
			for (File file : files) {
				if(file.getName().endsWith(".xml")){
					try {
						System.out.println("App::process :: processing file : " + file.getAbsolutePath());
						String fileOutPutName = file.getName().replace(".xml", ".json");
						File outputGATEFile = new File (outputDirectoryPath +  File.separator + fileOutPutName);
						processDocument(file, outputGATEFile);
					} catch (ResourceInstantiationException e) {
						System.out.println("App::process :: error with document " + file.getAbsolutePath());
						e.printStackTrace();
					} catch (MalformedURLException e) {
						System.out.println("App::process :: error with document " + file.getAbsolutePath());
						e.printStackTrace();
					} catch (IOException e) {
						System.out.println("App::process :: error with document " + file.getAbsolutePath());
						e.printStackTrace();
					} catch (Exception e) {
						System.out.println("App::process :: error with document " + file.getAbsolutePath());
						e.printStackTrace();
					} 
				}
			}
		}else {
			System.out.println("No directory :  " + inputDirectoryPath);
		}
		System.out.println("App::process :: END ");
	}

	/**
	 * Execute process in a document
	 * @param inputFile
	 * @param outputGATEFileResearchTechnique
	 * @throws ResourceInstantiationException
	 * @throws IOException 
	 * @throws JsonGenerationException 
	 * @throws InvalidOffsetException
	 */
	private static void processDocument(File inputFile, File outputGATEFile) throws ResourceInstantiationException, JsonGenerationException, IOException{
		gate.Document doc = Factory.newDocument(inputFile.toURI().toURL(), "UTF-8");
		AnnotationSet as = doc.getAnnotations("BSC");
	    Map<String, Collection<Annotation>> anns = new HashMap<String, Collection<Annotation>>();
	    anns.put("MedicalApplication", as.get("MedicalApplication"));
	    anns.put("Structure", as.get("Structure"));
	    anns.put("AssociatedBiologicalProcess", as.get("AssociatedBiologicalProcess"));
	    anns.put("ResearchTechnique", as.get("ResearchTechnique"));
	    anns.put("Biomaterial", as.get("Biomaterial"));
	    anns.put("ManufacturedObject", as.get("ManufacturedObject"));
	    anns.put("BiologicallyActiveSubstance", as.get("BiologicallyActiveSubstance"));
	    anns.put("Cell", as.get("Cell"));
	    anns.put("Tissue", as.get("Tissue"));
	    anns.put("ManufacturedObjectFeatures", as.get("ManufacturedObjectFeatures"));
	    anns.put("EffectOnBiologicalSystem", as.get("EffectOnBiologicalSystem"));
	    anns.put("AdverseEffects", as.get("AdverseEffects"));
	    anns.put("StudyType", as.get("StudyType"));
	    anns.put("AnimalModel", as.get("AnimalModel"));
	    anns.put("MaterialProcessing", as.get("MaterialProcessing"));
            anns.put("ArchitecturalOrganization", as.get("ArchitecturalOrganization"));
            
	    java.io.Writer out = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new FileOutputStream(outputGATEFile, false)));
    	gate.corpora.DocumentJsonUtils.writeDocument(doc, anns, out);
		out.close();
    }
}
