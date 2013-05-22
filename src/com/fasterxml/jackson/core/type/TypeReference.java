package com.fasterxml.jackson.core.type;

/*
 * (C) Copyright IBM Corp, 2012
 * 
 * Clean room implementation of Jackson code 
 */
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TypeReference<T> {
	
	private final Type genericType;
	
	/*
	 * Needs to have zero-arg constructor
	 */
	public TypeReference() { 
		// TODO

		Type superClassType = getClass().getGenericSuperclass();
		this.genericType = ((ParameterizedType)superClassType).getActualTypeArguments()[0];
	}
	
	/*
	 * This method needs to return a value such that:
	 * 
	 * new TypeReference<T>(){}.getType() == T.class
	 * 
	 * See JUnit
	 */
	public Type getType() {
		
		// TODO
		return genericType;
	}
}