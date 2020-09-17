package es.bsc.inb.debbie.export.json.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.shared.utils.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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

	    Set<String> processedFiles = null;
	    try {
	    	processedFiles = getFiles(outputFilePath);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    
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
			process(inputFilePath, outputFilePath, workdirPath, processedFiles);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Process directory and convert XML GATE format to JSON 
	 * @param properties_parameters_path
     * @throws IOException 
	 */
	public static void process(String inputDirectoryPath, String outputDirectoryPath, String workdir, Set<String> processedFiles) throws IOException {
    	System.out.println("App::processTagger :: INIT ");
    	long begTest = new java.util.Date().getTime();
		if (java.nio.file.Files.isDirectory(Paths.get(inputDirectoryPath))) {
			File inputDirectory = new File(inputDirectoryPath);
			File[] files =  inputDirectory.listFiles();
			System.out.println("Total files : " + files.length);
			System.out.println("Files already processed : " + processedFiles.size());
			for (File file : files) {
				if(file.getName().endsWith(".xml") && !processedFiles.contains(FileUtils.removeExtension(file.getName()))){
					try {
						System.out.println("App::process :: processing file : " + file.getAbsolutePath());
						String fileOutPutName = file.getName();
						File outputAbstractFile = new File (outputDirectoryPath +  File.separator + fileOutPutName.replace(".xml", "_abstract.json"));
						File outputAnnotationsFile = new File (outputDirectoryPath +  File.separator + fileOutPutName.replace(".xml", "_annotations.json"));
						processDocument(file, outputAbstractFile, outputAnnotationsFile);
						fileOutPutName=null;
						outputAbstractFile=null;
						outputAnnotationsFile=null;
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
		Double secs = new Double((new java.util.Date().getTime() - begTest)*0.001);
		System.out.println("Execution time:  " + secs + " seconds");
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
	private static void processDocument(File inputFile, File outputAbstractFile, File outputAnnotationsFile) throws ResourceInstantiationException, JsonGenerationException, IOException{
		try {
			gate.Document doc = Factory.newDocument(inputFile.toURI().toURL(), "UTF-8");
			AnnotationSet as = doc.getAnnotations("BSC");
		    Map<String, Collection<Annotation>> anns = new HashMap<String, Collection<Annotation>>();
		    anns.put("Biomaterial", as.get("Biomaterial"));
		    anns.put("BiomaterialTypes", as.get("BiomaterialTypes"));
		    anns.put("Chemical", as.get("Chemical"));
		    anns.put("BiologicallyActiveSubstance", as.get("BiologicallyActiveSubstance"));
		    anns.put("ManufacturedObject", as.get("ManufacturedObject"));
		    anns.put("ManufacturedObjectComponent", as.get("ManufacturedObjectComponent"));
		    anns.put("MedicalApplication", as.get("MedicalApplication"));
		    anns.put("ManufacturedObjectFeatures", as.get("ManufacturedObjectFeatures"));
		    anns.put("Shape", as.get("Shape"));
		    anns.put("Structure", as.get("Structure"));
		    anns.put("ArchitecturalOrganization", as.get("ArchitecturalOrganization"));
		    anns.put("DegradationFeatures", as.get("DegradationFeatures"));
		    anns.put("AssociatedBiologicalProcess", as.get("AssociatedBiologicalProcess"));
		    anns.put("MaterialProcessing", as.get("MaterialProcessing"));
		    anns.put("Cell", as.get("Cell"));
		    anns.put("Species", as.get("Species"));
		    anns.put("Tissue", as.get("Tissue"));
		    anns.put("AdverseEffects", as.get("AdverseEffects"));
		    anns.put("ResearchTechnique", as.get("ResearchTechnique"));
		    anns.put("EffectOnBiologicalSystem", as.get("EffectOnBiologicalSystem"));
		    anns.put("StudyType", as.get("StudyType"));
		    String plainText = doc.getContent().getContent(0l, gate.Utils.lengthLong(doc)).toString();
			String[] splitText = plainText.split("\n");
			String pubDate = splitText[0];
			String title = splitText[1];
			String pmid = inputFile.getName().replace(".xml", "");
 	        //write the gate annotations into a string, because we need to agregate more relevant attributes later
	        StringWriter sw = new StringWriter();
	        java.io.Writer out = new java.io.BufferedWriter(sw);
	        gate.corpora.DocumentJsonUtils.writeDocument(doc, anns, out);
	    	
	        //now add other relevant attributes, first parse the string to json, then add attributes.
	    	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	    	Date date = new Date();
	    	JSONParser parser = new JSONParser();
	    	JSONObject json = (JSONObject) parser.parse(sw.toString());
			json.put("_id", pmid);
			json.put("pmid", pmid);
			json.put("date", dateFormat.format(date));
			json.put("pubdate", pubDate);
			json.put("title", title);
			String text_document = json.get("text").toString();
			json.remove("text");
			//write to file metadata
	    	java.io.Writer writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new FileOutputStream(outputAnnotationsFile, false)));
	    	writer.write(json.toJSONString());
	    	writer.flush();
	    	writer.close();
	    	
	    	//document text
	    	JSONObject textJsonObject = new JSONObject();
	    	textJsonObject.put("_id", pmid);
	    	textJsonObject.put("pmid", pmid);
	    	textJsonObject.put("text", text_document);
			//write to file document text
	    	java.io.Writer writer2 = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new FileOutputStream(outputAbstractFile, false)));
	    	writer2.write(textJsonObject.toJSONString());
	    	writer2.flush();
	    	writer2.close();
	    	out.close();
			doc.cleanup();
			anns.clear();
			as.clear();
			sw.close();
			json.clear();
			textJsonObject.clear();
			parser=null;
			json=null;
			textJsonObject=null;
			writer=null;
			writer2=null;
			sw=null;
			out=null;
			doc=null;
			as=null;
			anns=null;
			text_document=null;
			pmid=null;
			title=null;
			pubDate=null;
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidOffsetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}catch(Exception e) {
			System.out.println("processDocument :: ERROR with file " + inputFile.getAbsolutePath());
			e.printStackTrace();
		}
	}
	
	/**
     * Return a set of files 
     * @param dir
     * @return
     * @throws IOException
     */
	public static Set<String> getFiles(String dir) throws IOException {
	    Set<String> fileList = new HashSet<>();
	    try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
	        for (Path path : stream) {
	            if (!Files.isDirectory(path)) {
	                fileList.add(FileUtils.removeExtension(path.getFileName().toString()));
	            }
	        }
	    }
	    return fileList;
	}
}
