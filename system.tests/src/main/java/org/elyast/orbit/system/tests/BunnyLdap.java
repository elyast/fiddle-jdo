package org.elyast.orbit.system.tests;

import java.util.UUID;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;
import javax.validation.constraints.NotNull;

@PersistenceCapable(table="ou=users,ou=system", schema="person", detachable="true")
public class BunnyLdap {

	public BunnyLdap(String string) {
		this.surname = string;
	}

	@PrimaryKey
	@NotNull
	@Column(name="cn")
    private String surname;
	
	@Column(name="sn")
    private String lastname = "Random" + UUID.randomUUID().toString();
	
	public final String getSurname() {
		return surname;
	}
	
	public String getLastname() {
		return lastname;
	}
}
