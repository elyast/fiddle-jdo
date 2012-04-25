package org.elyast.orbit.system.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatanucleusTest {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(DatanucleusTest.class);

	@Test
	public void should_allow_to_store_in_rdbms() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		ClassLoader cl = getClass().getClassLoader();
		properties.put("javax.jdo.PersistenceManagerFactoryClass",
				"org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
		properties.put("datanucleus.ConnectionDriverName","org.h2.Driver");
		properties.put("datanucleus.ConnectionURL","jdbc:h2:mem:test");
		properties.put("datanucleus.ConnectionUserName","sa");
		properties.put("datanucleus.ConnectionPassword","");
		properties.put("datanucleus.autoCreateSchema", "true");
		properties.put("datanucleus.storeManagerType", "rdbms");
		properties.put("datanucleus.connectionPoolingType", "DBCP");
		properties.put("datanucleus.cache.level2.mode", "ENABLE_SELECTIVE");
		properties.put("datanucleus.cache.level2.type", "ehcache");
		properties.put("datanucleus.cache.level2.cacheName", "test2");
		properties.put("datanucleus.primaryClassLoader", cl);
		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(
				properties, cl); 
//				getClassLoader("org.datanucleus.api.jdo", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory"));
		PersistenceManager pm = pmf.getPersistenceManager();
		Transaction tx = pm.currentTransaction();
		tx.begin();
		pm.makePersistent(new Bunny("Name"));
		tx.commit();
		fetchBunnies(pm);
		fetchBunnies(pm);
		fetchBunnies(pm);
	}

	@SuppressWarnings("unchecked")
	private void fetchBunnies(PersistenceManager pm) {
		Transaction tx1 = pm.currentTransaction();
		tx1.begin();
		for (int i = 0; i < 3; i++) {
			Query newQuery = pm.newQuery(Bunny.class);
			List<Bunny> execute = (List<Bunny>) newQuery.execute();
			LOGGER.info("Bunny list: {}", execute);
		}
		tx1.commit();
	}
	
	private ClassLoader getClassLoader(String bundle, String classtoLoad) {
		ClassLoader classloader = null;
        Bundle[] bundles = Activator.context.getBundles();

        for (int x = 0; x < bundles.length; x++) {

            if (bundle.equals(bundles[x].getSymbolicName())) {
                try {
                    classloader = bundles[x].loadClass(classtoLoad).getClassLoader();
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
            }
        }

        return classloader;
	}
}
