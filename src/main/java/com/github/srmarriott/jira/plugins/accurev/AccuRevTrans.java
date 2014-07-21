package com.github.srmarriott.jira.plugins.accurev;

import java.util.Date;
import java.util.List;

public class AccuRevTrans {
    private long id;
    private String type;
    private Date time;
    private String user;
    private String comment;
    private String stream;
    private List<AccuRevVersion> versions;
    
    public AccuRevTrans(
    		long id,
    		String type,
    		Date time,
    		String user,
    		String comment,
    		List<AccuRevVersion> versions) {
    	this.id = id;
    	this.type = type;
    	this.user = user;
    	this.time = time;
    	this.comment = comment;
    	this.versions = versions;
    	this.stream = parseStreamFromVersions(versions);
    }
    
    private String parseStreamFromVersions(List<AccuRevVersion> versions) {
    	String stream = "";
    	if (versions.isEmpty() == false) {
    		for (AccuRevVersion version : versions) {
    			if (version.getVirtualNamedVersion() != "") {
    				stream = version.getVirtualNamedVersion().split("/")[0];
    				break;
    			}
    		}
    	}
    	return stream;
    }
    
    public long getId() {
    	return this.id;
    }
    
    public String getType() {
    	return this.type;
    }
    
    public Date getTime() {
    	return this.time;
    }
    
    public String getUser() {
    	return this.user;
    }
    
    public String getComment() {
    	return this.comment;
    }
    
    public List<AccuRevVersion> getVersions() {
    	return this.versions;
    }
    
    public String getStream() {
    	return this.stream;
    }
}
