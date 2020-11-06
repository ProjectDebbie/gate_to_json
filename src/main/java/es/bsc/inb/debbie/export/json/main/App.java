package es.bsc.inb.debbie.export.json.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.shared.utils.io.FileUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

        Option set = new Option("a", "annotation_set", true, "Annotation set where the annotation will be included");
        set.setRequired(true);
        options.addOption(set);
        
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
        String annotationSet = cmd.getOptionValue("annotation_set");
        String workdirPath = cmd.getOptionValue("workdir");
        if (!java.nio.file.Files.isDirectory(Paths.get(inputFilePath))) {
    		System.out.println("Please set the inputDirectoryPath ");
			System.exit(1);
    	}

        if (annotationSet==null) {
        	System.out.println("Please set the annotation set where the annotation will be included");
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
			process(inputFilePath, outputFilePath, workdirPath, processedFiles, annotationSet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Process directory and convert XML GATE format to JSON 
	 * @param properties_parameters_path
     * @throws IOException 
	 */
	public static void process(String inputDirectoryPath, String outputDirectoryPath, String workdir, Set<String> processedFiles, String annotationSet) throws IOException {
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
						processDocument(file, outputAbstractFile, outputAnnotationsFile, annotationSet);
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
	 * @param outputGATEFile
	 * @throws ResourceInstantiationException
	 * @throws IOException 
	 * @throws JsonGenerationException 
	 * @throws InvalidOffsetException
	 */
	private static void processDocument(File inputFile, File outputTextFile, File outputAnnotationsFile, String annotationSet) throws ResourceInstantiationException, JsonGenerationException, IOException, InvalidOffsetException{
		gate.Document doc = Factory.newDocument(inputFile.toURI().toURL(), "UTF-8");
		Gson gsonBuilder = new GsonBuilder().create();
		JsonObject annotated_document = new JsonObject();
		long id_document = System.currentTimeMillis();
		String name = doc.getName().substring(0, doc.getName().indexOf(".xml")+4);
		String plainText = doc.getContent().getContent(0l, gate.Utils.lengthLong(doc)).toString();
		String[] splitText = plainText.split("\n");
		String pubDate = splitText[0];
		String pmid = splitText[1];
		String title = splitText[2];
		
		
		//now add other relevant attributes, first parse the string to json, then add attributes.
    	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    	Date date = new Date();
    	annotated_document.addProperty("_id", pmid);
    	annotated_document.addProperty("pmid", pmid);
    	annotated_document.addProperty("date", dateFormat.format(date));
    	annotated_document.addProperty("pubdate", pubDate);
    	annotated_document.addProperty("title", title);

    	Set<String> types = Stream.of("Biomaterial","BiomaterialTypes","Chemical","BiologicallyActiveSubstance","ManufacturedObject","ManufacturedObjectComponent",
									  "MedicalApplication","ManufacturedObjectFeatures","Shape","Structure","ArchitecturalOrganization","DegradationFeatures",
									  "AssociatedBiologicalProcess","MaterialProcessing","Cell","Species","Tissue","AdverseEffects",
									  "ResearchTechnique","EffectOnBiologicalSystem","StudyType").collect(Collectors.toCollection(HashSet::new));
		//JsonObject entities = new JsonObject();
		AnnotationSet as = doc.getAnnotations(annotationSet).get(types);
		JsonArray type_array = new JsonArray();
		for (String type : as.getAllTypes()) {
	    	for (Annotation annotation : as.get(type).inDocumentOrder()) {
		    	JsonObject annotationObject = new JsonObject();
		    	annotationObject.addProperty("type", annotation.getType());
		    	//annotationObject.addProperty("text", gate.Utils.stringFor(doc, annotation));
		    	annotationObject.addProperty("startOffset", annotation.getStartNode().getOffset());
		    	annotationObject.addProperty("endOffset", annotation.getEndNode().getOffset());
		    	for (Object key : annotation.getFeatures().keySet()) {
		    		annotationObject.addProperty(key.toString(), annotation.getFeatures().get(key).toString());
		    	}
		    	type_array.add(annotationObject);
		    }
	    	
		}
		annotated_document.add("annotations", type_array);
	    //write the annotations to file annotations
	    java.io.Writer writer1 = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new FileOutputStream(outputAnnotationsFile, false)));
	    writer1.write(gsonBuilder.toJson(annotated_document));
	    writer1.flush();
	    writer1.close();
	    writer1 = null;
	    annotated_document=null;
	    splitText=null;
	    as=null;
	    types=null;
	    //document text
    	JsonObject text_document = new JsonObject();
    	text_document.addProperty("id", id_document);
    	text_document.addProperty("name", name);
    	text_document.addProperty("text", plainText);
		//write to file document text
    	java.io.Writer writer2 = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new FileOutputStream(outputTextFile, false)));
    	writer2.write(gsonBuilder.toJson(text_document));
    	writer2.flush();
    	writer2.close();
    	writer2=null;
    	text_document = null;
    	plainText = null;
    	text_document=null;
		pmid=null;
		title=null;
		pubDate=null;
		gsonBuilder=null;
		doc=null;
		dateFormat=null;
		date=null;
		type_array=null;
		name=null;
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
