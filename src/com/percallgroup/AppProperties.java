package com.percallgroup;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppProperties {
	public static int TIMEADJUSTMENT;
	public static String CATALOG_CSV = "";
	public static String OUTPUT_XML;
	public static String OUTPUT_CSV;
	public static String START_DIR;
	public static String SOURCE_FILELIST;
	public static String DATE_FORMAT;
	public static String OUTPUT_REPORT;
	public static String EXCLUDE_STARTSWITH;	
	public static String EXCLUDE_EXTENSION;
	public static String FILESTOEXTRACT;
	public static String EXTRACT_DIR;
	
	
	public static void load() {

		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("ZipExplore.properties");

			prop.load(input);
			CATALOG_CSV=prop.getProperty("catalog.csv");
			OUTPUT_REPORT=prop.getProperty("output.report");			
			OUTPUT_XML = prop.getProperty("output.xml");
			OUTPUT_CSV = prop.getProperty("output.csv");
			START_DIR = prop.getProperty("start.dir");
			SOURCE_FILELIST=prop.getProperty("source.file.listing");
			TIMEADJUSTMENT = Integer.parseInt(prop.getProperty("deployedfile.timezone.adjustment"));
			DATE_FORMAT=prop.getProperty("deployedfile.dateformat");
			EXCLUDE_STARTSWITH=prop.getProperty("exclude.startswith");
			EXCLUDE_EXTENSION=prop.getProperty("exclude.extension");
			FILESTOEXTRACT=prop.getProperty("filestoextract");
			EXTRACT_DIR=prop.getProperty("extract.to.directory");
			
			input.close();
		} catch (IOException e) { 
			e.printStackTrace(); 
		}
	}


}
