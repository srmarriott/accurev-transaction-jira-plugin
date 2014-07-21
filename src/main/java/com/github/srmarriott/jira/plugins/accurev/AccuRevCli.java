package com.github.srmarriott.jira.plugins.accurev;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.srmarriott.jira.plugins.accurev.AccuRevException;

// TODO: setup accurev binary path correctly

public class AccuRevCli {
	private static final Logger log = LoggerFactory.getLogger(AccuRevCli.class);
	private static final boolean useNonexpiringLogin = true;
	private static final String accurevBinary = "/usr/bin/accurev";
	transient static final Lock ACCUREV_LOCK = new ReentrantLock();
	
    public static String runAccuRevCommand(String query, String server, long port, String username, String password) throws AccuRevException, IOException, InterruptedException {
        final StringBuilder sb = new StringBuilder();
    	try {
	        ACCUREV_LOCK.lock();
	    	if (loggedIntoAccuRev(server ,port, username, password) == false) {
	    		throw new AccuRevException("Could not authenticate login to accurev server " + server + ":" + port);
	    	}
	    	String cmd = accurevBinary + " " + query;
	        final Process p = Runtime.getRuntime().exec(cmd);
	        final BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String line = null;
	        int exitValue = 1;
	        
			while((line = input.readLine()) != null) {
				sb.append(line).append("\n");
	        }
	        
	        exitValue = p.waitFor();
	        
	        if (exitValue != 0) {
	        	log.debug("Exited with error code " + exitValue);
	        	throw new AccuRevException("Non-zero return from accurev");
	        }
    	} finally {
    		ACCUREV_LOCK.unlock();    		
    	}
        return sb.toString();
    }
    
    // Don't check for logins here
    private static String runAccuRevCommand(String query) throws AccuRevException, IOException, InterruptedException {
    	String cmd = accurevBinary + " " + query;
        final Process p = Runtime.getRuntime().exec(cmd);
        final BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        final StringBuilder sb = new StringBuilder();
        String line = null;
        int exitValue = 1;
        
		while((line = input.readLine()) != null) {
			sb.append(line).append("\n");
        }
        
        exitValue = p.waitFor();
        
        if (exitValue != 0) {
        	log.debug("Exited with error code " + exitValue);
        	throw new AccuRevException("Non-zero return from accurev");
        }
        
        return sb.toString();
    }
    
    // Generic CLI command which just returns true or false
    private static boolean runCommand(String cmd) throws AccuRevException, IOException, InterruptedException {
        final Process p = Runtime.getRuntime().exec(cmd);
        final BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        final StringBuilder sb = new StringBuilder();
        String line = null;
        int exitValue = 1;
        
		while((line = input.readLine()) != null) {
			sb.append(line).append("\n");
        }
        
        exitValue = p.waitFor();
        
        if (exitValue != 0) {
        	return false;
        }
        return true;
    }
    
    private static String getLoggedInUsername(String server, long port) throws IOException, AccuRevException, InterruptedException {
    	String output = runAccuRevCommand("info");
        final String usernameHeading = "Principal:";
        final String controlCharsOrSpaceRegex = "[ \\x00-\\x1F\\x7F]+";
        final Reader stringReader = new StringReader(output);
        final BufferedReader lineReader = new BufferedReader(stringReader);
        String line;
        
        try {
            line = lineReader.readLine();
            while (line != null) {
                final String[] parts = line.split(controlCharsOrSpaceRegex);
                for (int i = 0; i < parts.length; i++) {
                    final String part = parts[i];
                    if (usernameHeading.equals(part)) {
                        if ((i + 1) < parts.length) {
                            final String username = parts[i + 1];
                            return username;
                        }
                    }
                }
                line = lineReader.readLine();
            }
        } finally {
            lineReader.close();
        }
        throw new AccuRevException("Output from info cmd did not contain " + usernameHeading + " "
                + controlCharsOrSpaceRegex + " <username>");
    }
    
    private static boolean loggedIntoAccuRev(String server, long port, String requiredUsername, String password) throws IOException, AccuRevException, InterruptedException {
    	if (server == null) {
            return false;
        }
        if( requiredUsername==null || requiredUsername.trim().length()==0 ) {
            return false;
        }
        boolean loggedIn = false;

        String loggedInName = getLoggedInUsername(server, port);
        if (loggedInName.equals("(not") == true) {
        	// Try logging in again
        	String cmd = accurevBinary + " login -H " + server + ":" + port;
        	if (useNonexpiringLogin) {
        		cmd += " -n";
        	}
        	cmd += " " + requiredUsername + " " + password;
        	loggedIn = runCommand(cmd);
        	final Process p = Runtime.getRuntime().exec(cmd);
        	int exitValue = p.waitFor();
        	if (exitValue == 0) {
        		loggedIn = true;
        	} else {
        		loggedIn = false;
        	}
        } else {
        	loggedIn = true;
        }
        return loggedIn;
    }
}