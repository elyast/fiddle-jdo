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
		properties.put("datanucleus.connectionPoolingType", "DBCP");
		properties.put("datanucleus.autoCreateSchema", "true");
		properties.put("datanucleus.cache.level2.mode", "ENABLE_SELECTIVE");
		properties.put("datanucleus.cache.level2.type", "ehcache");
		properties.put("datanucleus.cache.level2.cacheName", "test2");
		properties.put("datanucleus.primaryClassLoader", cl);
		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(
				properties, cl);
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
}
