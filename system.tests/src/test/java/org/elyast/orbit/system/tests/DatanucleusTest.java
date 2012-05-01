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
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DatanucleusTest {
	static final Logger LOGGER = LoggerFactory
			.getLogger(DatanucleusTest.class);

	@Test
	public void should_allow_to_store_in_rdbms_with_jmx_enabled()
			throws Exception {
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
		makePersistentTest(properties);
	}


	@Test
	public void should_store_in_excel() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL", "excel:file:target/myfile"
				+ System.currentTimeMillis() + ".xls");
		makePersistentTest(properties);
	}

	@Test
	public void should_store_in_ldap() throws Exception {
		// need to store small ldap
		// LDAPServer.main(new String[]{});
		new TinyLdapServer();
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionDriverName",
				"com.sun.jndi.ldap.LdapCtxFactory");
		properties.put("datanucleus.ConnectionURL", "ldap://localhost:55665");
		properties.put("datanucleus.ConnectionUserName", "uid=admin,ou=system");
		properties.put("datanucleus.ConnectionPassword", "secret");
		properties.put("datanucleus.connectionPoolingType", "JNDI");
		makePersistentTest(properties);
	}

	@Test
	public void should_store_in_mongo() throws Exception {
		// assumption is local mongo
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL", "mongodb:127.0.0.1/test");
		makePersistentTest(properties);
	}

	@Test
	public void should_store_in_hbase() throws Exception {
		// assumption is local hbase
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL", "hbase:127.0.0.1");
		properties.put("datanucleus.autoCreateSchema", "true");
		makePersistentTest(properties);
	}

	@Test
	public void should_store_in_json() throws Exception {
		// need to have small http server
		Server jetty = new Server(56784);
		jetty.setHandler(new JsonStorageHandler());
		jetty.start();
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("datanucleus.ConnectionURL",
				"json:http://localhost:56784/api");
		makePersistentTest(properties);
		jetty.stop();
	}

	private void makePersistentTest(Map<String, Object> properties) {
		ClassLoader cl = getClass().getClassLoader();
		properties.put("javax.jdo.PersistenceManagerFactoryClass",
				"org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
		properties.put("datanucleus.primaryClassLoader", cl);
		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(
				properties, cl);
		makeBunnyTest(pmf);
	}
	private void makeBunnyTest(PersistenceManagerFactory pmf) {
		PersistenceManager pm = pmf.getPersistenceManager();
		Transaction tx = pm.currentTransaction();
		tx.begin();
		pm.makePersistent(new Bunny("Name" + UUID.randomUUID().toString()));
		tx.commit();
		fetchBunnies(pm);
		fetchBunnies(pm);
		fetchBunnies(pm);
		pm.close();
		pmf.close();
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
					classloader = bundles[x].loadClass(classtoLoad)
							.getClassLoader();
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
