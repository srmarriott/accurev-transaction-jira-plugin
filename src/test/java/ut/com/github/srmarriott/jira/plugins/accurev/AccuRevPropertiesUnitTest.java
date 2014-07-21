package ut.com.github.srmarriott.jira.plugins.accurev;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import com.github.srmarriott.jira.plugins.accurev.AccuRevDepotManagerImpl;
import com.github.srmarriott.jira.plugins.accurev.AccuRevProperties;
import com.github.srmarriott.jira.plugins.accurev.AccuRevPropertiesImpl;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;
import com.opensymphony.module.propertyset.map.MapPropertySet;

public class AccuRevPropertiesUnitTest {
	
	
	
    @Test
    public void testExpectedInputs() throws IOException
    {        
    	MapPropertySet propertySet = new MapPropertySet();
    	propertySet.setMap(new HashMap());
        AccuRevProperties props = new AccuRevPropertiesImpl(
        	"Test",
        	"server",
        	5050L,
        	"http://localhost:5050",
        	"user",
        	"pwd",
        	true,
        	100
        );
        
        AccuRevProperties.Util.fillPropertySet(props, propertySet);
        
        assertEquals("Test", propertySet.getString(MultipleAccuRevDepotManager.ACCUREV_DEPOT_NAME));
        assertEquals("server", propertySet.getString(MultipleAccuRevDepotManager.ACCUREV_SERVER));
        assertEquals("user", propertySet.getString(MultipleAccuRevDepotManager.ACCUREV_USERNAME));
        assertEquals("http://localhost:5050", propertySet.getString(MultipleAccuRevDepotManager.ACCUREV_WEBLINK));
        assertEquals("pwd", AccuRevDepotManagerImpl.decryptPassword(propertySet.getString(MultipleAccuRevDepotManager.ACCUREV_PASSWORD)));
    }
}
