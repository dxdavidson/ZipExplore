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
	String OUTPUT_XML;
	String OUTPUT_CSV;
	String START_DIR;
	String SOURCE_FILELIST;
	String CATALOG_CSV;
	
	public static void main(String[] args) {

		InspectZip iz = new InspectZip();
		iz.loadprops();
		
		//iz.createPTCMediaCatalog();
		iz.compareSourceWithCatalog();
		
        System.out.println("Finished");			

	}

	private void loadCatalog() {
		try {
			RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
			CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder( new FileReader(CATALOG_CSV));
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
		System.out.println("Loaded " + catalogFiles.size() + " files from " + CATALOG_CSV);
	}
	
	private void compareSourceWithCatalog() {
		
		CustomerFile cf;
		
		//Loads the TreeMap catalogFiles
		this.loadCatalog();
		
		try {
			RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
			CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder( new FileReader(SOURCE_FILELIST));
			CSVReader reader = csvReaderBuilder.withCSVParser(rfc4180Parser).build();
			
			String [] nextLine;
			while ((nextLine = reader.readNext()) != null) {
			    // nextLine[] is an array of values from the line
			    //System.out.println(nextLine[0] + nextLine[1] + "etc...");
			    cf = new CustomerFile(nextLine);
			    
			    if (catalogFiles.get(cf.getFileName())==null ) {
			    	System.out.println(cf.getFileName() + " missing from Catalog");
			    } else {
			    	//System.out.println(cf.getFileName() + " found in Catalog");
			    }
			 
			}
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());			
        } catch (CsvValidationException csve) {
            System.out.println(csve.getMessage());
        }
	}
	
	private void createPTCMediaCatalog() {

		walkDirectory(START_DIR);
		
		//outputToXML();
		outputToCSV();

	}
	
	private void loadprops() {
		Properties prop = new Properties();
		InputStream input = null;
		try {
	        input = new FileInputStream("ZipExplore.properties");

	        prop.load(input);
	        OUTPUT_XML = prop.getProperty("output.xml");
	        OUTPUT_CSV = prop.getProperty("output.csv");
	        START_DIR = prop.getProperty("start.dir");
	        SOURCE_FILELIST=prop.getProperty("source.file.listing");
	        CATALOG_CSV=prop.getProperty("catalog.csv");
	        		
		} catch (IOException e) { 
	        e.printStackTrace(); 
	    } 
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
            			readZipFile(new FileInputStream(f),f.getPath(), 1);
            		} catch(IOException ioe) {
          	          System.out.println("IOException : " + ioe);			
            		}
            	}
                //System.out.println( "File:" + f.getAbsoluteFile() );
            }
        }
    }
	
	private void readZipFile(InputStream fileInputStream, String targetFile, int level) throws IOException{

		//If level is 1, then method has been called to process a file on the OS, rather than an internal archive (zip within a zip)
		if (level ==1) {
			currentOSArchive = targetFile;
			System.out.println("Inspecting archive: " + targetFile);
		}
		
		 ZipInputStream zin = new ZipInputStream(fileInputStream); 

	      ZipEntry entry;
	      PTCMediaFile mediafile;
	      while((entry = zin.getNextEntry())!=null){
	    	  if ( !entry.isDirectory() && (entry.getName().endsWith(".zip") || entry.getName().endsWith(".jar") ) ){
	    		  readZipFile(zin,entry.getName(), level+1);
	    	  }
  	
    	  
	    	  if (!entry.isDirectory()) {
	    		  mediafile = new PTCMediaFile(entry,currentOSArchive );
	    		  
	    		  catalogFiles.put(entry.getName(),mediafile);

	    	  }
	         //zin.closeEntry();
	      }
	      //zin.close();
	   }


	public void outputToCSV() 
	{ 
		System.out.println("Writing to CSV: " + OUTPUT_CSV);
		 
		//first create file object for file placed at location 
	    // specified by filepath 
	    File file = new File(OUTPUT_CSV); 
	    
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
		        		  ze.getName(), 
		        		  pmf.getSourceArchiveFile(),
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
		System.out.println("Writing to XML: " + OUTPUT_XML);
		
		File xmlFile = new File(OUTPUT_XML);
		
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
		            tr.transform(new DOMSource(dom), new StreamResult(new FileOutputStream(OUTPUT_XML)));

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
