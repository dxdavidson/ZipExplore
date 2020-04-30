package com.percallgroup;

import java.util.zip.ZipEntry;

public class PTCMediaFile {
	private ZipEntry archiveEntry;
	private String fileName;
	private String sourceArchiveFile;
	private String fileSize;
	private String CRC;
	private String lastModified;
    private String filePath;

	private String archivePath;

	public PTCMediaFile(String [] csvline) {
		super();

		this.fileName = csvline[0];
		this.sourceArchiveFile = csvline[1];
		this.fileSize = csvline[2];
		this.CRC = csvline[3];
		this.lastModified = csvline[4];
		
		this.setfilePath();

	}

	public PTCMediaFile(ZipEntry entry, String OSArchive, String localArchivePath) {
		super();

		this.fileName = entry.getName();
		this.archiveEntry = entry;
		this.sourceArchiveFile = OSArchive;
		this.archivePath = localArchivePath;
		
		this.setfilePath();
	}

	public String getfilePath() {
		return this.filePath;
	}
	public void setfilePath() {
		//ZIP files from PTC Media are not consistent in whether "codebase" is included at front of path
		//To make this consistent, remove the codebase/ prefix if it exists
		
		
		if (this.fileName.toLowerCase().startsWith("codebase/")) {
			// "codebase/" is 9 chars so truncate starting at char 9
			this.filePath = this.fileName.substring(9);
		} else {
			this.filePath = this.fileName;
		}
		
	}
	
	public String getArchivePath() {
		return archivePath;
	}

	public void setArchivePath(String archivePath) {
		this.archivePath = archivePath;
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
	public String getCRC() {
		return CRC;
	}
	public void setCRC(String cRC) {
		CRC = cRC;
	}
	public String getLastModified() {
		return lastModified;
	}
	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	public String getSourceArchiveFile() {
		return sourceArchiveFile;
	}

	public void setSourceArchiveFile(String sourceArchiveFile) {
		this.sourceArchiveFile = sourceArchiveFile;
	}

	public void setZipEntry(ZipEntry thisEntry) {
		archiveEntry = thisEntry;
	}

	public ZipEntry getZipEntry()
	{
		return this.archiveEntry;
	}
}
