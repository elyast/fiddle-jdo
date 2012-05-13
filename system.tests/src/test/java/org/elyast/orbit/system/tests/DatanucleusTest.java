package org.elyast.orbit.system.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.eclipse.jetty.server.Server;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DatanucleusTest {
	static final Logger LOGGER = LoggerFactory
			.getLogger(DatanucleusTest.class);

	@Test
	public void should_allow_to_store_in_rdbms_with_jmx_enabled()
			throws Exception {
		LOGGER.info("should_allow_to_store_in_rdbms_with_jmx_enabled start...");
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionDriverName", "org.h2.Driver");
		properties.put("datanucleus.ConnectionURL", "jdbc:h2:mem:test");
		properties.put("datanucleus.ConnectionUserName", "sa");
		properties.put("datanucleus.ConnectionPassword", "");
		properties.put("datanucleus.autoCreateSchema", "true");
		properties.put("datanucleus.storeManagerType", "rdbms");
		properties.put("datanucleus.connectionPoolingType", "dbcp-builtin");
		properties.put("datanucleus.cache.level2.mode", "ENABLE_SELECTIVE");
		properties.put("datanucleus.cache.level2.type", "ehcache");
		properties.put("datanucleus.cache.level2.cacheName", "test2");
		properties.put("datanucleus.managedRuntime", "true");
		makePersistentTest(properties, Bunny.class);
		LOGGER.info("should_allow_to_store_in_rdbms_with_jmx_enabled stop...");
	}


	@Test
	public void should_store_in_excel() throws Exception {
		LOGGER.info("should_store_in_excel start...");
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL", "excel:file:target/myfile"
				+ System.currentTimeMillis() + ".xls");
		makePersistentTest(properties, Bunny.class);
		LOGGER.info("should_store_in_excel stop...");
	}
	
	@Test
	public void should_store_in_xml() throws Exception {
		LOGGER.info("should_store_in_xml start...");
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL", "xml:file:target/myfile"
				+ System.currentTimeMillis() + ".xml");
		makePersistentTest(properties, Bunny.class);
		LOGGER.info("should_store_in_xml stop...");
	}
	
	@Test
	public void should_store_in_excel_ooxml() throws Exception {
		LOGGER.info("should_store_in_excel ooxml start...");
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL", "ooxml:file:target/myfile"
				+ System.currentTimeMillis() + ".xlsx");
		makePersistentTest(properties, Bunny.class);
		LOGGER.info("should_store_in_excel ooxml stop...");
	}
	
	@Test
	public void should_store_in_odf() throws Exception {
		LOGGER.info("should_store_in_odf start...");
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL", "odf:file:target/myfile"
				+ System.currentTimeMillis() + ".ods");
		makePersistentTest(properties, Bunny.class);
		LOGGER.info("should_store_in_odf stop...");
	}	

	@Test
	//assumption is localhost ldap running
	public void should_store_in_ldap() throws Exception {
		LOGGER.info("should_store_in_ldap start...");
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionDriverName",
				"com.sun.jndi.ldap.LdapCtxFactory");
		properties.put("datanucleus.ConnectionURL", "ldap://localhost:10389");
		properties.put("datanucleus.ConnectionUserName", "uid=admin,ou=system");
		properties.put("datanucleus.ConnectionPassword", "secret");
		properties.put("datanucleus.connectionPoolingType", "JNDI");
		makePersistentTest(properties, BunnyLdap.class);
		LOGGER.info("should_store_in_ldap stop...");
	}

	@Test// assumption is local mongo
	public void should_store_in_mongo() throws Exception {
		LOGGER.info("should_store_in_mongo start...");
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL", "mongodb:127.0.0.1/test");
		makePersistentTest(properties, Bunny.class);
		LOGGER.info("should_store_in_mongo stop...");
	}

	@Test
	// assumption is local hbase with schema initiated
	public void should_store_in_hbase() throws Exception {
		LOGGER.info("should_store_in_hbase start...");
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL", "hbase:127.0.0.1");
		properties.put("datanucleus.autoCreateSchema", "true");
		properties.put("datanucleus.PersistenceUnitName", "CoolBunny");
		makePersistentTest(properties, Bunny.class);
		LOGGER.info("should_store_in_hbase stop...");
	}

	@Test
	public void should_store_in_json() throws Exception {
		LOGGER.info("should_store_in_json start...");
		Server jetty = new Server(56784);
		jetty.setHandler(new JsonStorageHandler());
		jetty.start();
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL",
				"json:http://localhost:56784/api");
		makePersistentTest(properties, Bunny.class);
		jetty.stop();
		LOGGER.info("should_store_in_json stop...");
	}

	private <T> void makePersistentTest(Map<String, Object> properties, Class<T> entity) throws Exception {
		ClassLoader cl = getClass().getClassLoader();
		properties.put("javax.jdo.PersistenceManagerFactoryClass",
				"org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
		properties.put("datanucleus.primaryClassLoader", cl);
		Thread.currentThread().setContextClassLoader(cl);
		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(
				properties, cl);
		makeBunnyTest(pmf, entity);
	}
	private <T> void makeBunnyTest(PersistenceManagerFactory pmf, Class<T> entity) throws Exception {
		PersistenceManager pm = pmf.getPersistenceManager();
		Transaction tx = pm.currentTransaction();
		tx.begin();
		pm.makePersistent(entity.getConstructor(String.class).newInstance("Name" + UUID.randomUUID().toString()));
		tx.commit();
		fetchBunnies(pm, entity);
		pm.close();
		pmf.close();
	}

	@SuppressWarnings("unchecked")
	private <T> void fetchBunnies(PersistenceManager pm, Class<T> entity) {
		Transaction tx1 = pm.currentTransaction();
		tx1.begin();
		for (int i = 0; i < 3; i++) {
			Query newQuery = pm.newQuery(entity);
			List<T> execute = (List<T>) newQuery.execute();
			LOGGER.info("Bunny list: {}", execute.iterator().next());
		}
		tx1.commit();
	}
	
}
