/**
 * Copyright (c) 2008-2025, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */
package edu.nps.moves.dis7.source.generator.pdus;

/**
 * Represents an initial value for a primitive type. This can be set in a subclass
 * of a given class, for example to initialize a header, like version number of
 * packet type.
 *
 * @author DMcG
 */
public class GeneratedInitialValue // TODO refactor rename as GeneratedInitialValue for consistency
{
    /** The field name that will be  initialized in this class's constructor */
    private String variable;
    
    /** the value that it will be set to. */
    private String variableValue;
    
    /**
     * Set initial value of variable
     * @param pVariable name of variable
     * @param pValue initial value
     */
    public GeneratedInitialValue(String pVariable, String pValue)
    {
        variable = pVariable;
        variableValue = pValue;
    }
    
    /**
     * get variable name
     * @return variable name
     */
    public String getVariable()
    {
        return variable;
    }
    
    /**
     * set variable name
     * @param pVariable new variable name
     */
    public void setVariable(String pVariable)
    {
        variable = pVariable;
    }
    
    /**
     * get variable value
     * @return variable value
     */
    public String getVariableValue()
    {
        return variableValue;
    }
    
    /**
     * set variable value
     * @param pValue new variable value
     */
    public void setVariableValue(String pValue)
    {
        variableValue = pValue;
    }
    
    /** 
     * Returns the "standard" method name for a setter, given the variable name.
     * @return method name corresponding to variable setter
     */
    public String getSetterMethodName()
    {
        String methodName = variable;
        methodName = "set" + this.initialCapital(methodName);
        
        return methodName;
    }
    
    /** 
     * Returns the "standard" method name for a setter, given the variable name.
     * @return  method name corresponding to variable setter
     */
	public String getSetterMethodNameCSharp() //PES added for CSharp Support
	{
		String methodName = variable;
		//My original intent was to just remove the "set" prefix, however other problems were encountered such as
		//the DIS1998.XML file had one type with a capital letter in the beginning (IntercomSignalPDU --> TdlType)
		//this caused problems as it was already capitialized and the get/set method would have had the same name. Therefore
		//I decided to append the underscore "_" to the protected variable names, this worked except for the following problem.
		//In two instances the Class name was the same name as one of its fields except that the first letter was cap which
		//caused the accessor methods to return an error stating member names can not be the same as
		//there enclosing type.  Anthore problem was a key word was used  "System".  Therefore my solution was to check for the class name and the field names being the same and changing
		//the field name by adding an underscore at the end which is being done in the CSharpGenerator.java file, this is not an ideal solution.
		methodName = this.initialCapital(methodName) ; 
        
		return methodName;
	}
    
    /** 
     * returns a string with the first letter capitalized. 
     * @param aString of interest
     * @return same string with first letter capitalized
     */
    public String initialCapital(String aString)
    {
        StringBuffer stb = new StringBuffer(aString);
        stb.setCharAt(0, Character.toUpperCase(aString.charAt(0)));
        
        return new String(stb);
    }

    public String ToString(String... indent)
    {
        String tabs = "";
        if (indent.length > 0)
            tabs += indent[0];

        String stringValue = "";
        stringValue += tabs + "///         Value : " + getVariableValue() + "\n";
        stringValue += tabs + "/// Setter Method : " + getSetterMethodName() + "\n";
        return stringValue;
    }
    
}
