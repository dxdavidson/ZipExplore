package com.percallgroup;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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



}
