package com.percallgroup;




//https://stackoverflow.com/questions/11287486/read-a-zip-file-inside-zip-file
//https://stackoverflow.com/questions/2056221/recursively-list-files-in-java

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.zip.ZipFile;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException; 


public class InspectZip {


	private String currentOSArchive;
	private TreeMap<String, PTCMediaFile> catalogFiles = new TreeMap<String, PTCMediaFile>();

	public static void main(String[] args) {

		InspectZip iz = new InspectZip();
		AppProperties.load();
		//System.out.println("Time adjustment: "+AppProperties.TIMEADJUSTMENT);

		if (args.length == 0) {
			System.err.println("Usage: InspectZip CreateCatalog|CompareSource");
			System.exit(1);
		}
		switch (args[0].toUpperCase()) {
		case "CREATECATALOG":
			iz.createPTCMediaCatalog();	
			break;
		case "COMPARESOURCE":
			iz.compareSourceWithCatalog();
			break;
		default:
			System.err.println("Usage: InspectZip CreateCatalog|CompareSource");
			System.exit(1);

		}


		System.out.println("Finished");			

	}

	private void loadCatalog() {
		try {
			RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
			CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder( new FileReader(AppProperties.CATALOG_CSV));
			CSVReader reader = csvReaderBuilder.withCSVParser(rfc4180Parser).withSkipLines(1).build();

			String [] nextLine;
			PTCMediaFile mediafile;

			while ((nextLine = reader.readNext()) != null) {
				// nextLine[] is an array of values from the line
				//System.out.println(nextLine[0] + nextLine[1] + "etc...");
				mediafile = new PTCMediaFile(nextLine);
				catalogFiles.put(mediafile.getFileName(),mediafile);
			}
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());			
		} catch (CsvValidationException csve) {
			System.out.println(csve.getMessage());
		}
		System.out.println("Loaded " + catalogFiles.size() + " files from " + AppProperties.CATALOG_CSV);
	}

	private void compareSourceWithCatalog() {

		DeployedFile df;
		PTCMediaFile pmf;

		//Loads the TreeMap catalogFiles
		this.loadCatalog();

		File file = new File(AppProperties.OUTPUT_REPORT); 

		try {
			RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
			CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder( new FileReader(AppProperties.SOURCE_FILELIST));
			CSVReader reader = csvReaderBuilder.withCSVParser(rfc4180Parser).withSkipLines(1).build();
			
			// create FileWriter object with file as parameter 
			boolean result = Files.deleteIfExists(file.toPath());
			FileWriter outputfile = new FileWriter(file); 

			// create CSVWriter object filewriter object as parameter 
			CSVWriter writer = new CSVWriter(outputfile); 

			// adding header to csv 
			String[] header = { "FileName", "Extension", "Issue", "Comment" }; 
			writer.writeNext(header); 
			
			String [] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				// nextLine[] is an array of values from the line
				//System.out.println(nextLine[0] + nextLine[1] + "etc...");
				df = new DeployedFile(nextLine);
				//System.out.println(df.getFileName() + " lastModified: " + df.printLastModified());

				pmf = catalogFiles.get(df.getFileName());
				if (pmf==null ) {
					String[] line = {
							df.getFileName(),
							df.getFileExtension(),
							"Additional",
							df.getFileName() + " not in Media Catalog, may be custom added"
					};
					writer.writeNext(line); 
					//System.out.println(df.getFileName() + " missing from Catalog");
				} else {
					//System.out.println(cf.getFileName() + " found in Catalog");
					if (!df.equalsfileSize(pmf)) {
						String[] line = {
								df.getFileName(),
								df.getFileExtension(),
								"Modified",
								df.getFileName() + " deployed version: " + df.getFileSize() + "bytes" + " catalog version: " +  pmf.getFileSize() + "bytes"
						};
						writer.writeNext(line);						
						//System.out.println(df.getFileName() + " deployed version has " + df.getFileSize() + "bytes" + " catalog version has " +  pmf.getFileSize() + "bytes");
					}
				}

			}
			outputfile.close();
			writer.close();
			reader.close();
			
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());			
		} catch (CsvValidationException csve) {
			System.out.println(csve.getMessage());
		}
		

	}

	private void createPTCMediaCatalog() {

		walkDirectory(AppProperties.START_DIR);

		//outputToXML();
		outputToCSV();

	}


	private void walkDirectory( String path ) {

		File root = new File( path );
		File[] list = root.listFiles();

		if (list == null) return;

		for ( File f : list ) {
			if ( f.isDirectory() ) {
				walkDirectory( f.getAbsolutePath() );
				//System.out.println( "Dir:" + f.getAbsoluteFile() );
			}
			else {
				if (f.getName().endsWith(".zip")) {
					try {
						readZipFile(new FileInputStream(f),f.getPath(), 1, "");
					} catch(IOException ioe) {
						System.out.println("IOException : " + ioe);			
					}
				}
				//System.out.println( "File:" + f.getAbsoluteFile() );
			}
		}
	}

	private void readZipFile(InputStream fileInputStream, String targetFile, int level, String archivePath) throws IOException{

		//If level is 1, then method has been called to process a file on the OS, rather than an internal archive (zip within a zip)
		if (level ==1) {
			currentOSArchive = targetFile;
			archivePath = targetFile;
			System.out.println("Inspecting archive: " + targetFile);
		}

		ZipInputStream zin = new ZipInputStream(fileInputStream); 

		ZipEntry entry;
		PTCMediaFile mediafile;
		while((entry = zin.getNextEntry())!=null){
			if ( !entry.isDirectory() && (entry.getName().endsWith(".zip") || entry.getName().endsWith(".jar") ) ){
				readZipFile(zin,entry.getName(), level+1, archivePath+"|"+entry.getName());
			}


			if (!entry.isDirectory()) {
				mediafile = new PTCMediaFile(entry,currentOSArchive, archivePath );

				catalogFiles.put(entry.getName(),mediafile);

			}
			//zin.closeEntry();
		}
		//zin.close();
	}


	public void outputToCSV() 
	{ 
		System.out.println("Writing to CSV: " + AppProperties.OUTPUT_CSV);

		//first create file object for file placed at location 
		// specified by filepath 
		File file = new File(AppProperties.OUTPUT_CSV); 

		try { 
			// create FileWriter object with file as parameter 
			boolean result = Files.deleteIfExists(file.toPath());
			FileWriter outputfile = new FileWriter(file); 

			// create CSVWriter object filewriter object as parameter 
			CSVWriter writer = new CSVWriter(outputfile); 

			// adding header to csv 
			String[] header = { "FileName", "OSArchive", "Bytes", "CRC", "LastModified" }; 
			writer.writeNext(header); 

			Set filesSet = catalogFiles.entrySet();
			Iterator iterator = filesSet.iterator();
			while(iterator.hasNext()) {
				Map.Entry mentry = (Map.Entry)iterator.next();

				PTCMediaFile pmf = (PTCMediaFile) mentry.getValue();
				ZipEntry ze = pmf.getZipEntry();

				String[] line = {
						//ze.getName(),
						pmf.getfilePath(),
						pmf.getArchivePath(),
						//pmf.getSourceArchiveFile(),
						String.valueOf(ze.getSize()),
						String.valueOf(ze.getCrc()), 
						String.valueOf(ze.getLastModifiedTime())
				};
				writer.writeNext(line); 

			}

			// closing writer connection 
			writer.close(); 
		} 
		catch (IOException e) { 
			// TODO Auto-generated catch block 
			e.printStackTrace(); 
		} 
	} 

	private void outputToXML() {
		System.out.println("Writing to XML: " + AppProperties.OUTPUT_XML);

		File xmlFile = new File(AppProperties.OUTPUT_XML);

		try {
			boolean result = Files.deleteIfExists(xmlFile.toPath()); //surround it in try catch block
		} catch(IOException ioe) {
			System.out.println("IOException : " + ioe);			
		}


		Document dom;
		Element e = null;

		// instance of a DocumentBuilderFactory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.newDocument();

			// create the root element
			Element rootEle = dom.createElement("PTCMediaFiles");

			Set filesSet = catalogFiles.entrySet();
			Iterator iterator = filesSet.iterator();
			while(iterator.hasNext()) {
				Map.Entry mentry = (Map.Entry)iterator.next();

				PTCMediaFile pmf = (PTCMediaFile) mentry.getValue();
				ZipEntry ze = pmf.getZipEntry();

				// create data elements and place them under root
				e = dom.createElement("File");

				e.setAttribute("sourcearchive", pmf.getSourceArchiveFile());
				e.setAttribute("bytes", String.valueOf(ze.getSize()));
				e.setAttribute("CRC", String.valueOf(ze.getCrc()));

				e.appendChild(dom.createTextNode(ze.getName()));
				rootEle.appendChild(e);

				//System.out.println(mentry.getKey() + " size:=" + ze.getSize());

			}
			dom.appendChild(rootEle);

			try {
				Transformer tr = TransformerFactory.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty(OutputKeys.METHOD, "xml");
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd");
				tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

				// send DOM to file
				tr.transform(new DOMSource(dom), new StreamResult(new FileOutputStream(AppProperties.OUTPUT_XML)));

			} catch (TransformerException te) {
				System.out.println(te.getMessage());
			} catch (IOException ioe) {
				System.out.println(ioe.getMessage());
			}

		} catch (ParserConfigurationException pce) {
			System.out.println("UsersXML: Error trying to instantiate DocumentBuilder " + pce);
		}
	}

}
