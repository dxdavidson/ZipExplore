package com.percallgroup;

//https://stackoverflow.com/questions/3934470/how-to-iterate-through-google-multimap
//https://www.programcreek.com/java-api-examples/?api=java.util.zip.ZipEntry
//https://stackoverflow.com/questions/12310978/check-if-string-ends-with-certain-pattern


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
//import java.util.TreeMap;
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

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.ArrayListMultimap;



public class InspectZip {


	private String currentOSArchive;
	private Multimap<String, PTCMediaFile> catalogFilesMulti = MultimapBuilder.treeKeys().arrayListValues().build();
	ArrayList<String> filesToExtract = new ArrayList<String>(); 
    private boolean extractionMode = false;    
	
	public static void main(String[] args) {

		InspectZip iz = new InspectZip();
		AppProperties.load();
		//System.out.println("Time adjustment: "+AppProperties.TIMEADJUSTMENT);

		if (args.length == 0) {
			System.err.println("Usage: InspectZip CreateCatalog|CompareSource|ExtractFromMedia");
			System.exit(1);
		}
		switch (args[0].toUpperCase()) {
		case "CREATECATALOG":
			iz.createPTCMediaCatalog();	
			break;
		case "COMPARESOURCE":
			iz.compareSourceWithCatalog();
			break;
		case "EXTRACTFROMMEDIA":
			iz.extractFromCatalog();	
			break;			
		default:
			System.err.println("Usage: InspectZip CreateCatalog|CompareSource|ExtractFromMedia");
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
				catalogFilesMulti.put(mediafile.getFileName(),mediafile);
			}
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());			
		} catch (CsvValidationException csve) {
			System.out.println(csve.getMessage());
		}
		System.out.println("Loaded " + catalogFilesMulti.size() + " files from " + AppProperties.CATALOG_CSV);
	}

	
	private void compareSourceWithCatalog() {

		DeployedFile df;
		//PTCMediaFile pmf;
		
		//There is an issue where many files in the PTC Media ZIP are 0 bytes
		//Yet the deployed version is not 0 bytes
		//Sometimes this is because a localized version is copied over when the install takes place
		//Other times, I cannot explain! e.g. there are GIF files of 0 bytes in the catalog, but once deployed they are 846 bytes, they must be copied from somewhere
		//To avoid these 0byte files skewing the report, I will identify them with a warning, but not count them as having been modified
		Long sumOfFileSizes;
		String issue="";
		
		Collection<PTCMediaFile> pmfiles;
		
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
			boolean foundMatchingCatalogEntry; 
			String catalogFileDetails;
			
			String [] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				// nextLine[] is an array of values from the line
				//System.out.println(nextLine[0] + nextLine[1] + "etc...");
				df = new DeployedFile(nextLine);
				foundMatchingCatalogEntry = false;
				sumOfFileSizes = (long) 0;
				
				if (df.isValidForReport() ) {
	
					pmfiles = catalogFilesMulti.get(df.getFileName());
					
					if (pmfiles.size() == 0 ) {
						//System.out.println(df.getFileName() + " missing from Catalog");
						String[] line = {
								df.getFileName(),
								df.getFileExtension(),
								"Additional",
								df.getFileName() + " not in Media Catalog, may be custom added"
						};
						writer.writeNext(line);
						
					} else {
						for (PTCMediaFile pmf : pmfiles) {
							sumOfFileSizes = sumOfFileSizes + Long.parseLong(pmf.getFileSize());
							
							if (df.equalsfileSize(pmf)) {
								foundMatchingCatalogEntry = true;
								break;
							}
						}
						if (!foundMatchingCatalogEntry) {

							if (sumOfFileSizes ==0 ) {
								catalogFileDetails = pmfiles.size()+" files exist in catalog, all 0 bytes. Probably these files have not been modified. ";
								issue = "Warning";
							} else {
								if (pmfiles.size() > 1 ) {
									catalogFileDetails = pmfiles.size()+" files existing in catalog, none of matching size";
									issue = "Modified";
								} else {
									catalogFileDetails = " Catalog version: " +  pmfiles.iterator().next().getFileSize() + " bytes. ";
									issue = "Modified";
									}
							}
							
							String[] line = {
									df.getFileName(),
									df.getFileExtension(),
									issue,
									df.getFileName() + " deployed version: " + df.getFileSize() + " bytes. " + catalogFileDetails
							};
							
							writer.writeNext(line);						

						}
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

	private void extractFromCatalog() {

		extractionMode = true;
		buildExtractList();
		
		walkDirectory(AppProperties.START_DIR);

		//extractFiles();
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
		PTCMediaFile existingMediaFile;
		PTCMediaFile mediafile;
		while((entry = zin.getNextEntry())!=null){
			if ( !entry.isDirectory() && (entry.getName().endsWith(".zip") || entry.getName().endsWith(".jar") ) ){
			//if ( !entry.isDirectory() && (entry.getName().endsWith(".gz") ) ){			
				readZipFile(zin,entry.getName(), level+1, archivePath+"|"+entry.getName());
			}


			if (!entry.isDirectory()) {
				mediafile = new PTCMediaFile(zin, entry,currentOSArchive, archivePath );

				//If file is in list of those to be extracted, then extract it.
				if (extractionMode && filesToExtract.contains(entry.getName())) {
					this.extractSingleFile(zin, entry);
				}
				//if (entry.getName().endsWith("ConstraintDisplayNamesRB.java")) {this.extractSingleFile(zin, entry);};

				catalogFilesMulti.put(entry.getName(),mediafile);
			}
			//zin.closeEntry();
		}
		//zin.close();
	}


	public void buildExtractList() {
		
		try {
			RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
			CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder( new FileReader(AppProperties.FILESTOEXTRACT));
			CSVReader reader = csvReaderBuilder.withCSVParser(rfc4180Parser).build();
			
			String [] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				filesToExtract.add(nextLine[0]);
			}
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());	
		} catch (CsvValidationException csve) {
			System.out.println(csve.getMessage());
		}

		
	}
	
	public void extractFiles() {

		Collection<PTCMediaFile> pmfiles;
		PTCMediaFile tmp;
		
		try {
			RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
			CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder( new FileReader(AppProperties.FILESTOEXTRACT));
			CSVReader reader = csvReaderBuilder.withCSVParser(rfc4180Parser).build();

			String [] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				// nextLine[] is an array of values from the line
				//System.out.println(nextLine[0] + nextLine[1] + "etc...");
				pmfiles = catalogFilesMulti.get(nextLine[0]);
				
				//File has not been found in Catalog
				if (pmfiles.size() == 0 ) {
					System.out.println(nextLine[0] + " not found");
				} else {
					System.out.println(nextLine[0] + " " + pmfiles.size() + " files FOUND");
					tmp = pmfiles.iterator().next();
					for (PTCMediaFile pmf : pmfiles) {
						System.out.println(pmf.getfilePath() + " " + new Date(pmf.getZipEntry().getTime()));
						//Check if this element has a later modifiedtimestamp than the tmp element
						if (pmf.getZipEntry().getTime() > tmp.getZipEntry().getTime()) {
							tmp = pmf;
						}
					}
					System.out.println("Extract "+ tmp.getFileName() + " dated " + new Date(tmp.getZipEntry().getTime()));
					this.extractSingleFile(tmp.getZipInputStream(), tmp.getZipEntry());
					
				}
			}			
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());	
		} catch (CsvValidationException csve) {
			System.out.println(csve.getMessage());
		}

	}

	
	private void extractSingleFile(ZipInputStream thisZin, ZipEntry thisZipEntry) {
		System.out.println("Extracting " + thisZipEntry.getName());
		boolean extractFile = false;
		
		try {
			File destFile = newDir(new File(AppProperties.EXTRACT_DIR), thisZipEntry);
			//If the file either doesn't already exist
			//Or the file that exists is older than the one in the zip
			//Then extract it
			if (!destFile.exists() ) {
				extractFile=true;
			} else if (thisZipEntry.getTime() > destFile.lastModified()) {
				System.out.println(thisZipEntry.getName() + " " + new Date(destFile.lastModified()) + " ZIP entry more recent than exising " +  new Date(thisZipEntry.getTime()) + " extracting from zip");
				extractFile=true;
			} else if (thisZipEntry.getTime() == destFile.lastModified()) {
				System.out.println(thisZipEntry.getName() + " " + new Date(destFile.lastModified()) + " ZIP entry same datstamp as existing  " +  new Date(thisZipEntry.getTime()) + " do not extract");
				extractFile=true;
			} else if (thisZipEntry.getTime() < destFile.lastModified()) {
				System.out.println(thisZipEntry.getName() + " " + new Date(destFile.lastModified()) + " ZIP entry older than existing " +  new Date(thisZipEntry.getTime()) + " do not extract");
				extractFile=false;
			} 
				
			
			if (extractFile) {
				FileOutputStream fos = new FileOutputStream(destFile);
				byte[] buffer = new byte[1024];
			
				int len;
		        while ((len = thisZin.read(buffer)) > 0) {
		            fos.write(buffer, 0, len);
		        }
		        fos.close();
		        destFile.setLastModified(thisZipEntry.getTime());
			}
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());	
		} 
		

	}
	
	public File newDir(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());
         
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        
        destFile.getParentFile().mkdirs();
        
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
         
        return destFile;
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

			for (PTCMediaFile pmf : catalogFilesMulti.values()) {
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

	/*
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
*/
}
