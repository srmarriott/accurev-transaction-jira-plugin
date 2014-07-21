package ut.com.github.srmarriott.jira.plugins.accurev;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;
import org.mockito.Mock;

import com.github.srmarriott.jira.plugins.accurev.AccuRevDepotManager;
import com.github.srmarriott.jira.plugins.accurev.AccuRevDepotManagerImpl;
import com.github.srmarriott.jira.plugins.accurev.AccuRevProperties;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.map.MapPropertySet;

public class AccuRevDepotManagerImplUnitTest{
    private AccuRevDepotManager accuRevDepotManager;

    private PropertySet propertySet;

    private AccuRevProperties accurevProperties;

    private Mock mockAccuRevDepot;
	
	@Test
	public void testEncryptPassword() {
	}
}