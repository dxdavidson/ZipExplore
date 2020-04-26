package com.percallgroup;




//https://stackoverflow.com/questions/11287486/read-a-zip-file-inside-zip-file
//https://stackoverflow.com/questions/2056221/recursively-list-files-in-java
	
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipFile;


public class InspectZip {
   
	public static void main(String[] args) {
		//String startFile="D:\\temp\\buildlogs.zip";
		/*
		String startFile="D:\\temp\\wmd_WC_MODULES-rt_zg_ia_sf.zip";
		File zipfile = new File(startFile);
		
		try {
	         readZipFile(new FileInputStream(zipfile), startFile);
	      } catch(IOException ioe) {
	          System.out.println("IOException : " + ioe);			
		}
		*/
		
		InspectZip iz = new InspectZip();
		String startDir="D:\\temp\\DD\\";
		iz.walk(startDir);
		
	}

	
	private void walk( String path ) {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk( f.getAbsolutePath() );
                System.out.println( "Dir:" + f.getAbsoluteFile() );
            }
            else {
            	if (f.getName().endsWith(".zip")) {
            		try {
            			readZipFile(new FileInputStream(f),f.getPath());
            		} catch(IOException ioe) {
          	          System.out.println("IOException : " + ioe);			
            		}
            	}
                //System.out.println( "File:" + f.getAbsoluteFile() );
            }
        }
    }
	
	private static void readZipFile(InputStream fileInputStream, String targetFile) throws IOException{

		 ZipInputStream zin = new ZipInputStream(fileInputStream); 

	      ZipEntry entry;
	      while((entry = zin.getNextEntry())!=null){
	    	  if ( !entry.isDirectory() && (entry.getName().endsWith(".zip") || entry.getName().endsWith(".jar") ) ){
	    		  readZipFile(zin,entry.getName());
	    	  }
  	
    	  
	    	  if (!entry.isDirectory()) {
		    	  System.out.println(
		    			 targetFile +  '\t' +
		    			 entry.getName() + '\t' +
		    			 entry.getTime() + '\t' +
		    			 entry.getCrc() + '\t' +
		    			 entry.getSize() 
		    	  );
	    	  }
	         //zin.closeEntry();
	      }
	      //zin.close();
	   }
	
}
