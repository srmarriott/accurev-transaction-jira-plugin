package com.github.srmarriott.jira.plugins.accurev.transactions.scheduling;

import com.atlassian.core.exception.InfrastructureException;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;

import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.Date;
import java.util.HashMap;

public class UpdateIndexMonitorImpl implements UpdateIndexMonitor, LifecycleAware, DisposableBean {
	
	private static final String JOB_NAME = "AccuRev Transaction Indexing Service";
    private final static Logger logger = LoggerFactory.getLogger(UpdateIndexMonitorImpl.class);
	private final PluginScheduler pluginScheduler;
    private final MultipleAccuRevDepotManager multipleAccuRevDepotManager;
    private static final long DEFAULT_INDEX_INTERVAL = DateTimeConstants.MILLIS_PER_MINUTE;

	public UpdateIndexMonitorImpl(PluginScheduler pluginScheduler, MultipleAccuRevDepotManager multipleAccuRevDepotManager) {
		this.pluginScheduler = pluginScheduler;
        this.multipleAccuRevDepotManager =  multipleAccuRevDepotManager;
	}
	
	public void onStart() {
        schedule();
	}
	
	public void schedule() {
		pluginScheduler.scheduleJob(
                JOB_NAME,
                UpdateIndexTask.class,
                new HashMap<String, Object>() {{
                    put("UpdateIndexMonitorImpl:instance", UpdateIndexMonitorImpl.this);
                    put("MultipleAccuRevDepotManager", multipleAccuRevDepotManager);
                }},
                new Date(),
                DEFAULT_INDEX_INTERVAL);
        logger.info(String.format("UpdateIndexMonitorImpl scheduled to run every %dms", DEFAULT_INDEX_INTERVAL));
	}

        public void onStop() {
        }

    @Override
    public void destroy() {
//    	// unscheduleJob stalls JIRA shutdown sometimes. No idea so far.
//    	logger.info("UpdateIndexMonitorImpl DESTROY +");
//        try
//        {
//            pluginScheduler.unscheduleJob(JOB_NAME);
//        }
//        catch (IllegalArgumentException e)
//        {
//        	//logger.info("Error unschedule update index job " + e);
//            //throw new InfrastructureException("Error unschedule update index job " + e);
//        }
//        logger.info("UpdateIndexMonitorImpl DESTROY -");
    }
}
