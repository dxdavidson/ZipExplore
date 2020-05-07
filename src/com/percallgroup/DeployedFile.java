package com.percallgroup;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DeployedFile {
	private String fileName;
	private String fileSize;
	private Date lastModified;
	private String fileExtension;

	public DeployedFile(String [] csvline) {
		super();

		this.fileName = csvline[0];
		this.fileSize = csvline[1];

		this.setLastModified(csvline[2]);
		this.setFileExtension();

	}

	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension() {
		int i = this.fileName.lastIndexOf('.');
		if (i > 0) {
		    this.fileExtension= this.fileName.substring(i+1);
		}
		
	}


	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileSize() {
		return fileSize;
	}
	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}
	public Date getLastModified() {
		return lastModified;
	}

	public String printLastModified() {

		DateFormat dateFormat = new SimpleDateFormat(AppProperties.DATE_FORMAT);
		return dateFormat.format(this.lastModified);

	}
	/**
	 * @param csvDate
	 */
	public void setLastModified(String csvDate) {

		Date rawDate = null;

		try {
			rawDate=new SimpleDateFormat(AppProperties.DATE_FORMAT).parse(csvDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  

		//Subtract the TIMEADJUSTMENT property to adjust the local time recorded to the UTC time in the Media Catalog
		Calendar cal = Calendar.getInstance();
		cal.setTime(rawDate);
		cal.add(Calendar.HOUR, -AppProperties.TIMEADJUSTMENT);
		this.lastModified = cal.getTime();

	}

	public boolean equalsLastModified(PTCMediaFile thisPMF) {
		return true;
	}

	public boolean equalsfileSize(PTCMediaFile thisPMF) {

		return thisPMF.getFileSize().equals(this.getFileSize());
	}


	/**
	 * Check if this file should be included in the report
	 * Some reasons why not to include
	 * 	- class is under the tasks/com/infoengine/compiledTasks folder (list of folders defined in .properties)
	 *  - log files, or other file extensions defined in .properties
	 */
	public boolean isValidForReport () {
		boolean isValid = true;
		
		List<String> excludeStartsWith = Arrays.asList(AppProperties.EXCLUDE_STARTSWITH.split(","));
		for (String str : excludeStartsWith) {
			if (this.fileName.toUpperCase().startsWith(str.toUpperCase())) {
				isValid = false;
				break;
			}
		}
		
		if (isValid) {
			List<String> excludeExtension = Arrays.asList(AppProperties.EXCLUDE_EXTENSION.split(","));
			for (String str : excludeExtension) {
				if (this.fileName.toUpperCase().endsWith(str.toUpperCase())) {
					isValid = false;
					break;
				}
			}			
		}

		return isValid;
	}
	

}
