package org.elyast.orbit.system.tests;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.configuration.MutableServerStartupConfiguration;
import org.apache.directory.server.jndi.ServerContextFactory;

public class TinyLdapServer {

	protected LdapContext sysRoot;

	protected MutableServerStartupConfiguration configuration = new MutableServerStartupConfiguration();

	public TinyLdapServer() throws Exception {
		configuration.setLdapPort(55665);
		configuration.setShutdownHookEnabled(false);
		setSysRoot("uid=admin,ou=system", "secret");
	}

	private LdapContext setSysRoot(String user, String passwd)
			throws NamingException {
		Hashtable env = new Hashtable(configuration.toJndiEnvironment());
		env.put(Context.SECURITY_PRINCIPAL, user);
		env.put(Context.SECURITY_CREDENTIALS, passwd);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		return setSysRoot(env);
	}

	protected LdapContext setSysRoot(Hashtable env) throws NamingException {
		Hashtable envFinal = new Hashtable(env);
		envFinal.put(Context.PROVIDER_URL, "ou=system");
		envFinal.put(Context.INITIAL_CONTEXT_FACTORY,
				ServerContextFactory.class.getName());
		return sysRoot = new InitialLdapContext(envFinal, null);
	}
}
