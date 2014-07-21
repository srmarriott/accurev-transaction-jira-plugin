package com.github.srmarriott.jira.plugins.accurev;

public class AccuRevVersion {
    private String path;
    private String eid;
    private String virtualVer;
    private String realVer;
    private String virtualNamedVersion;
    private String realNamedVersion;
    private elementType elemType;
    private String dir;
    
    public enum streamType {
        NORMAL, WORKSPACE
    }
    
    public enum elementType {
    	TEXT, PTEXT, BINARY, DIR, SLINK, ELINK
    }
    
    public AccuRevVersion(
    		String path,
    		String eid,
    		String virtualVer,
    		String realVer,
    		String virtualNamedVersion,
    		String realNamedVersion,
    		elementType elemType,
    		String dir) {
    	this.path = path;
    	this.eid = eid;
    	this.virtualVer = virtualVer;
    	this.realVer = realVer;
    	this.virtualNamedVersion = virtualNamedVersion;
    	this.realNamedVersion = realNamedVersion;
    	this.elemType = elemType;
    	this.dir = dir;
    }
    
    public String getPath() {
    	return this.path;
    }
    
    public String getEid() {
    	return this.eid;
    }
    
    public String getVirtualVer() {
    	return this.virtualVer;
    }
    
    public String getRealVer() {
    	return this.realVer;
    }
    
    public String getVirtualNamedVersion() {
    	return this.virtualNamedVersion;
    }
    
    public String getRealNamedVersion() {
    	return this.realNamedVersion;
    }
    
    public elementType getElemType() {
    	return this.elemType;
    }
    
    public String getDir() {
    	return this.dir;
    }
}