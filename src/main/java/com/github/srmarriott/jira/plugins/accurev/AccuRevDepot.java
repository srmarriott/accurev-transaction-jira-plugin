package com.github.srmarriott.jira.plugins.accurev;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import com.github.srmarriott.jira.plugins.accurev.AccuRevCli;
import com.github.srmarriott.jira.plugins.accurev.AccuRevVersion.elementType;

public class AccuRevDepot {
	private String server;
	private long port;
	private String depotName; // TODO: sort out unused variables
	private int depotId;
	private String username;
    private String password;
    private static final Logger log = LoggerFactory.getLogger(AccuRevDepot.class);
    private final static boolean filterWorkspaceTrans = true;
	private final static long TRANSBUFFER = 1500; // How many transactions to store in an object at a time
    
    public AccuRevDepot(
    		String server,
    		long port,
    		String depotName,
    		String username,
    		String password) {
    	this.server = server;
    	this.port = port;
    	this.depotName = depotName;
    	this.username = username;
    	this.password = password;
    	this.depotId = -1;
    }
    
    public boolean setDepotId() throws AccuRevException, IOException, InterruptedException {
    	String depots = AccuRevCli.runAccuRevCommand("show depots", this.server, this.port, this.username, this.password);
    	BufferedReader bufReader = new BufferedReader(new StringReader(depots));
    	String line = null;
    	while((line = bufReader.readLine()) != null) {
    		String[] depotArray = line.split("\\s+");
    		if (depotArray.length == 3) {
				if (depotArray[0].equals(depotName)) {
					this.depotId = Integer.parseInt(depotArray[1]);
					return true;
				}
    		}
    	}
    	return false;
    }
    
    private AccuRevVersion getAccuRevVersionFromNode(Node node) {
    	String path = "";
		String eid = "";
		String virtualVer = "";
		String realVer = "";
		String virtualNamedVer = "";
		String realNamedVer = "";
		elementType elemType = null;
		String dir = "";
		if (node.getAttributes().getNamedItem("path") != null) {
			path = node.getAttributes().getNamedItem("path").getNodeValue();	
		}
		if (node.getAttributes().getNamedItem("eid") != null) {
			eid = node.getAttributes().getNamedItem("eid").getNodeValue();
		}
		if (node.getAttributes().getNamedItem("virtual") != null) {
			virtualVer = node.getAttributes().getNamedItem("virtual").getNodeValue();
		}
		if (node.getAttributes().getNamedItem("real") != null) {
			realVer = node.getAttributes().getNamedItem("real").getNodeValue();
		}
		if (node.getAttributes().getNamedItem("virtualNamedVersion") != null) {
			virtualNamedVer = node.getAttributes().getNamedItem("virtualNamedVersion").getNodeValue();
		}
		if (node.getAttributes().getNamedItem("realNamedVersion") != null) {
			realNamedVer = node.getAttributes().getNamedItem("realNamedVersion").getNodeValue();
		}
		if (node.getAttributes().getNamedItem("elem_type") != null) {
			String strElemType = node.getAttributes().getNamedItem("elem_type").getNodeValue();
			if (strElemType.equals("binary"))
				elemType = elementType.BINARY;
			else if (strElemType.equals("text"))
				elemType = elementType.TEXT;
			else if (strElemType.equals("dir"))
				elemType = elementType.DIR;
			else if (strElemType.equals("slink"))
				elemType = elementType.SLINK;
			else if (strElemType.equals("elink"))
				elemType = elementType.ELINK;
			else if (strElemType.equals("ptext"))
				elemType = elementType.PTEXT;
		}
		if (node.getAttributes().getNamedItem("dir") != null) {
			dir = node.getAttributes().getNamedItem("dir").getNodeValue();
		}
		return new AccuRevVersion(
				path,
				eid,
				virtualVer,
				realVer,
				virtualNamedVer,
				realNamedVer,
				elemType,
				dir);
    }
    
    private AccuRevTrans getAccuRevTransFromNode(Node node) {
    	long id = 0;
		String type = "";
		Date time = null;
		String user = "";
		String comment = "";
		String stream = ""; // TODO
		final List<AccuRevVersion> versions = new ArrayList<AccuRevVersion>();
		
		if (node.getNodeName() == "transaction") {
		    id = Long.valueOf(node.getAttributes().getNamedItem("id").getNodeValue());
		    type = node.getAttributes().getNamedItem("type").getNodeValue();
		    String strTime = node.getAttributes().getNamedItem("time").getNodeValue();
		    time = new Date(Long.parseLong(strTime) * 1000);
		    user = node.getAttributes().getNamedItem("user").getNodeValue();
		    // Find comment and versions
		    if (node.hasChildNodes()) {
		    	NodeList nodeList2 = node.getChildNodes();
		    	for (int j = 0; j < nodeList2.getLength(); ++j) {
		    		Node value = nodeList2.item(j);
		    		if (value.getNodeName() == "comment") {
		    			if (value.getLastChild() != null) {
		    				if (value.getLastChild().getTextContent() != null) {
		    					comment = value.getLastChild().getTextContent().trim();
		    				}
		    			}
		    		}
		    		if (value.getNodeName() == "version") {
		        		versions.add(getAccuRevVersionFromNode(value));
		    		}
		    	}
		    }
		}
    	AccuRevTrans accuRevTrans = new AccuRevTrans(id, type, time, user, comment, versions);
    	return accuRevTrans;
    }
    
    private AccuRevTrans getAccuRevTransFromXmlString(String input) throws ParserConfigurationException, SAXException, IOException {
    	AccuRevTrans trans = null;
    	log.debug("getAccuRevTransFromXmlString: " + input);
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document document = builder.parse(new InputSource(new StringReader(input)));
    	NodeList nodeList = document.getDocumentElement().getChildNodes();
    	for (int i = 0; i < nodeList.getLength(); ++i) {
    		Node node = nodeList.item(i);
    		if (node.getNodeName() == "transaction") {
    			log.debug("Found a trans!");
    			trans = getAccuRevTransFromNode(node);
    			break;
    		}
    	}
    	return trans;
    }
    
    private boolean isTransFromWorkspace(Map<String, AccuRevVersion.streamType> streams, AccuRevTrans trans) {
    	if (trans.getVersions().isEmpty()) {
    		return true;
    	}
    	for (AccuRevVersion version : trans.getVersions()) {
    		String virtualNamedStream = version.getVirtualNamedVersion().split("/")[0];
    		if (streams.containsKey(virtualNamedStream)) {
    			if (streams.get(virtualNamedStream) == AccuRevVersion.streamType.WORKSPACE) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    private Map<String, AccuRevVersion.streamType> getStreamMapFromNode(Node node) {
    	final Map<String, AccuRevVersion.streamType> streams = new HashMap<String, AccuRevVersion.streamType>();
    	if (node.getNodeName() == "streams") {
    		if (node.hasChildNodes()) {
    			NodeList nodeList2 = node.getChildNodes();
    			for (int i = 0; i < nodeList2.getLength(); ++i) {
    				Node stream = nodeList2.item(i);
    				if (stream.getNodeName() == "stream") {
    					String name = stream.getAttributes().getNamedItem("name").getNodeValue();
    					String type = stream.getAttributes().getNamedItem("type").getNodeValue();
    					AccuRevVersion.streamType typeEnum = AccuRevVersion.streamType.NORMAL;
    					if (type.equals("workspace")) {
    						typeEnum = AccuRevVersion.streamType.WORKSPACE;
    					}
    					streams.put(name, typeEnum);
    				}
    			}
    		}
    	}
    	return streams;
    }
    
    private synchronized Collection<AccuRevTrans> getMultipleAccuRevTransFromXmlString(String input) throws ParserConfigurationException, SAXException, IOException {
    	final Collection<AccuRevTrans> transactions = new ArrayList<AccuRevTrans>();
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document document = builder.parse(new InputSource(new StringReader(input)));
    	NodeList nodeList = document.getDocumentElement().getChildNodes();
    	final Map<String, AccuRevVersion.streamType> streams = new HashMap<String, AccuRevVersion.streamType>();
    	if (filterWorkspaceTrans) {
    		NodeList stream = document.getElementsByTagName("streams");
    		if (stream != null) {
    			if (stream.getLength() != 0) {
    				streams.putAll(getStreamMapFromNode(stream.item(0)));
    			}
    		}
    	}
    	for (int i = 0; i < nodeList.getLength(); ++i) {
    		Node node = nodeList.item(i);
    		if (node.getNodeName() == "transaction") {
    			AccuRevTrans tmpTrans =  getAccuRevTransFromNode(nodeList.item(i));
    			
    			if (tmpTrans.getId() != 0) {
    				if (filterWorkspaceTrans) {
    					if (isTransFromWorkspace(streams, tmpTrans) == false) {
    						transactions.add(tmpTrans);
    					}
    				} else {
    					transactions.add(tmpTrans);
    				}
    			}
    		}
    	}
    	return transactions;
    }
    
    public synchronized long getLatestTransactionId() throws InterruptedException, IOException, AccuRevException, ParserConfigurationException, SAXException {
    	String cmd = "hist -fx -p " + this.depotName;
    	cmd += " -t highest";
    	String output = AccuRevCli.runAccuRevCommand(cmd, this.server, this.port, this.username, this.password);
    	log.debug("getLatestTransactionId: " + cmd);
    	AccuRevTrans trans = getAccuRevTransFromXmlString(output);
    	return trans.getId();
    }
    
    public synchronized AccuRevTrans getTransaction(long transaction) throws InterruptedException, IOException, AccuRevException, ParserConfigurationException, SAXException {
    	String cmd = "hist -fx -p " + this.depotName;
    	cmd += " -t " + transaction;
    	String output = AccuRevCli.runAccuRevCommand(cmd, this.server, this.port, this.username, this.password);
    	AccuRevTrans trans = getAccuRevTransFromXmlString(output);
    	if (trans == null) {
    		log.debug("getTransaction(long transaction): trans is null!");
    	}
    	return trans;
    }
    
    public synchronized Collection<AccuRevTrans> hist(long retrieveStart, long retrieveEnd) throws InterruptedException, IOException, AccuRevException, ParserConfigurationException, SAXException {
    	String cmd = "hist -fx -p " + this.depotName;
    	cmd += " -t " + retrieveStart + "-" + retrieveEnd;
    	String output = AccuRevCli.runAccuRevCommand(cmd, this.server, this.port, this.username, this.password);
    	log.debug(cmd);
    	Collection<AccuRevTrans> transactions = getMultipleAccuRevTransFromXmlString(output);
    	return transactions;
    }
    
    // To reduce memory footprint for large transaction ranges
    public synchronized Collection<AccuRevTrans> histBuffered(long retrieveStart, long retrieveEnd) throws InterruptedException, IOException, AccuRevException, ParserConfigurationException, SAXException {
    	long start = retrieveStart;
    	long transRange = retrieveEnd - start;
    	Collection<AccuRevTrans> transactions = new ArrayList<AccuRevTrans>();
    	while (transRange > TRANSBUFFER) {
    		log.debug("hist: " + start + ", " + (start + TRANSBUFFER));
    		transactions.addAll(hist(start, start + TRANSBUFFER));
    		start += TRANSBUFFER + 1;
    		transRange = retrieveEnd - start;
    	}
    	log.debug("hist: " + start + ", " + (start + TRANSBUFFER));
    	transactions.addAll(hist(start,retrieveEnd));
    	return transactions;
    }
    
    public String getDepotName() {
    	return this.depotName;
    }
    
    public int getDepotId() {
    	return this.depotId;
    }
}