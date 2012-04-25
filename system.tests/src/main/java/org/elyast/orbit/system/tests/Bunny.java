package org.elyast.orbit.system.tests;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.validation.constraints.NotNull;

@PersistenceCapable(cacheable = "true")
public class Bunny {

	public Bunny(String string) {
		this.surname = string;
	}

	@PrimaryKey
	@NotNull
    private String surname;
	
	public final String getSurname() {
		return surname;
	}
}
