/**
 * Copyright (c) 2008-2025, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */
package edu.nps.moves.dis7.source.generator.pdus;

import edu.nps.moves.dis7.source.generator.enumerations.StringUtils;

// import edu.nps.moves.dis7.source.generator.*;
import edu.nps.moves.dis7.source.generator.pdus.GeneratedClassAttribute.ClassAttributeType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Arrays;

/* not thoroughly examined, global change: OBJECT_LIST to OBJECT_LIST and FIXED_LIST to PRIMITIVE_LIST */
/**
 * Given the input object, something of an abstract syntax tree, this generates
 * a source code file in the C# language. It has ivars, getters,  setters,
 * and serialization/deserialization methods.
 * Warning: only partially implemented.
 * @author DMcG
 * modified by Peter Smith (Naval Air Warfare Center - Training Systems Division
 * modified by Zvonko Bostjancic (Blubit d.o.o.)
 */
public class CsharpGenerator extends AbstractGenerator {

    /**
     * ivars are often preceded by a special character. This sets what that character is, 
     * so that instance variable names will be preceded by a "_".
     */
    public static final String IVAR_PREFIX ="_";

    /** whether using dot net */
    protected boolean useDotNet = true;
    
    /** Maps the primitive types listed in the XML file to the C# types */
    Properties types = new Properties();

    /**
     * Provides the default values for primitive types.
     */
    Properties typeDefaultValue = new Properties();

    /** What primitive types should be marshalled as. This may be different from
     * the C# get/set methods, ie an unsigned short might have ints as the getter/setter,
     * but is marshalled as a short.
     */
    Properties marshalTypes = new Properties();
    Properties marshalMethods = new Properties();

    /** Similar to above, but used on unmarshalling. There are some special cases (unsigned
     * types) to be handled here.
     */
    Properties unmarshalTypes = new Properties();

    /** sizes of various primitive types */
    Properties primitiveSizes = new Properties();

    /** A property list that contains c#-specific code generation information, such
     * as namespace which correlates to package names, using which correlates to imports, etc.
     */
    Properties csharpProperties;

    String disVersion;

    /** PES 02/10/2009 Added to save all classes linked to Upper Class (PDU)
     * Will be used to allow automatic setting of Length when Marshall method called
     */
    Map<String, String> classesInstantiated = new HashMap<>();

    String globalNamespace = "";
    String enumNamespace = "";
    String debugAttributes = "";

    /**
     * Create a generator
     * @param pClassDescriptions String map of GeneratedClass
     * @param pCsharpProperties C# properties
     */
    public CsharpGenerator(Map<String, GeneratedClass> pClassDescriptions, Properties pCsharpProperties) {
        super(pClassDescriptions, pCsharpProperties);

        Properties systemProperties = System.getProperties();
        String clDirectory = systemProperties.getProperty("xmlpg.generatedSourceDir");
        String clNamespace = systemProperties.getProperty("xmlpg.namespace");
        String clUsing = systemProperties.getProperty("xmlpg.using");

        // Directory to place generated source code
        if(clDirectory != null)
            pCsharpProperties.setProperty("directory", clDirectory);
        
        // Namespace for generated code
        if(clNamespace != null)
            pCsharpProperties.setProperty("namespace", clNamespace);
        
        // the using (imports) for the generated code
        if(clUsing != null)
            pCsharpProperties.setProperty("using", clUsing);
        

        super.setGeneratedSourceDirectoryName(pCsharpProperties.getProperty("directory"));
        
        String dotNet = pCsharpProperties.getProperty("useDotNet");
        if(dotNet != null && dotNet.equalsIgnoreCase("false"))
            useDotNet = false;
            
        globalNamespace = systemProperties.getProperty("xmlpg.namespace");
        if (globalNamespace != null)
            pCsharpProperties.setProperty("namespace", globalNamespace);
        else
            globalNamespace = "";

        // set global namespace for enums
        enumNamespace = systemProperties.getProperty("xmlpg.enumNamespace");
        if (enumNamespace != null)
            pCsharpProperties.setProperty("enumNamespace", enumNamespace);
        else
            enumNamespace = "";

        debugAttributes = systemProperties.getProperty("xmlpg.debugatrributes");
        if (debugAttributes != null)
            pCsharpProperties.setProperty("debugAttributes", debugAttributes);
        else
            debugAttributes = "";

        System.out.println("C# PDU Output :");
        System.out.println("  directory set to : " + clDirectory);
        System.out.println("         namespace : " + globalNamespace);
        System.out.println("    enum namespace : " + enumNamespace);
            
        // Set up a mapping between the strings used in the XML file and the strings used
        // in the C# file, specifically the data types. This could be externalized to
        // a properties file, but there's only a dozen or so and an external props file
        // would just add some complexity.
types.setProperty("uint8",   "byte");
types.setProperty("uint16",  "ushort");
types.setProperty("uint32",  "UInt32");
types.setProperty("uint64",  "ulong");
types.setProperty("int8",    "sbyte");
types.setProperty("int16",   "short");
types.setProperty("int32",   "int");
types.setProperty("int64",   "long");
types.setProperty("float32", "float");
types.setProperty("float64", "double");

typeDefaultValue.setProperty("byte",   "0");
typeDefaultValue.setProperty("short",  "0");
typeDefaultValue.setProperty("int",    "0");
typeDefaultValue.setProperty("long",   "0");
typeDefaultValue.setProperty("byte",   "0");
typeDefaultValue.setProperty("ushort",  "0");
typeDefaultValue.setProperty("uint",    "0");
typeDefaultValue.setProperty("ulong",   "0");
typeDefaultValue.setProperty("float",  "0.0F");
typeDefaultValue.setProperty("double", "0");
typeDefaultValue.setProperty("UInt32", "0");


        // Set up the mapping between primitive types and marshal types.
        marshalTypes.setProperty("uint8",   "byte");
        marshalTypes.setProperty("uint16",  "ushort");
        marshalTypes.setProperty("uint32",  "UInt32");
        marshalTypes.setProperty("uint64",  "ulong");
        marshalTypes.setProperty("int8",    "byte");
        marshalTypes.setProperty("int16",   "short");
        marshalTypes.setProperty("int32",   "int");
        marshalTypes.setProperty("int64",   "long");
        marshalTypes.setProperty("float32", "float");
        marshalTypes.setProperty("float64", "double");

        // For the datastream these are the Write methods
        marshalMethods.setProperty("uint8",   "UnsignedByte");
        marshalMethods.setProperty("uint16",  "UnsignedShort");
        marshalMethods.setProperty("uint32",  "UnsignedInt");
        marshalMethods.setProperty("uint64",  "UnsignedLong");
        marshalMethods.setProperty("int8",    "Byte");
        marshalMethods.setProperty("int16",   "Short");
        marshalMethods.setProperty("int32",   "Int");
        marshalMethods.setProperty("int64",   "Long");
        marshalMethods.setProperty("float32", "Float");
        marshalMethods.setProperty("float64", "Double");


        // Unmarshalling types
        unmarshalTypes.setProperty("uint8",   "UInt8");
        unmarshalTypes.setProperty("uint16",  "UInt16");
        unmarshalTypes.setProperty("uint32",  "UInt32");
        unmarshalTypes.setProperty("uint64",  "long");
        unmarshalTypes.setProperty("int8",    "byte");
        unmarshalTypes.setProperty("int16",   "short");
        unmarshalTypes.setProperty("int32",   "int");
        unmarshalTypes.setProperty("int64",   "long");
        unmarshalTypes.setProperty("float32", "float");
        unmarshalTypes.setProperty("float64", "double");

        // How big various primitive types are
        primitiveSizes.setProperty("uint8",   "1");
        primitiveSizes.setProperty("uint16",  "2");
        primitiveSizes.setProperty("uint32",  "4");
        primitiveSizes.setProperty("uint64",  "8");
        primitiveSizes.setProperty("int8",    "1");
        primitiveSizes.setProperty("int16",   "2");
        primitiveSizes.setProperty("int32",   "4");
        primitiveSizes.setProperty("int64",   "8");
        primitiveSizes.setProperty("float32", "4");
        primitiveSizes.setProperty("float64", "8");
        primitiveSizes.setProperty("unsigned short", "2");
        primitiveSizes.setProperty("unsigned byte", "1");
        primitiveSizes.setProperty("unsigned int", "4");
        primitiveSizes.setProperty("unsigned long", "8");

        primitiveSizes.setProperty("byte", "1");
        primitiveSizes.setProperty("short", "2");
        primitiveSizes.setProperty("int", "4");
        primitiveSizes.setProperty("long", "8");

        primitiveSizes.setProperty("double", "8");
        primitiveSizes.setProperty("float", "4");
    }

    /**
     * Generate the classes and write them to a directory
     */
    @Override
    public void writeClasses() {
        createGeneratedSourceDirectory(false); // boolean: whether to clean out prior files, if any exist in that directory

        Iterator it = classDescriptions.values().iterator();

        System.out.println("Creating C# source code.");

        // PES 02/10/2009 used to store all classes
        Iterator it2 = classDescriptions.values().iterator();

        while (it2.hasNext()) {
            GeneratedClass aClass = (GeneratedClass) it2.next();
            String parentClass = aClass.getParentClass();

            if (parentClass.equalsIgnoreCase("root")) {
                parentClass = "Object";
            }

            classesInstantiated.put(aClass.getName(), parentClass);
        }

        //END storing all Classes

        while (it.hasNext()) {
            try {
                GeneratedClass aClass = (GeneratedClass) it.next();
                String name = aClass.getName();

                // Create namespace structure, if any
                String namespace = languageProperties.getProperty("namespace");
                String fullPath;

                // If we have a namespace specified, replace the dots in the namespace name
                // with slashes and create that directory
                if (namespace != null)
                {
                    namespace = namespace.replace(".", "/");
                    fullPath = getGeneratedSourceDirectoryName() + "/" + name + ".cs";
                    //System.out.println("full path is " + fullPath);
                } 
                else
                {
                    fullPath = getGeneratedSourceDirectoryName() + "/" + name + ".cs";
                }
                //System.out.println("Creating Csharp source code file for " + fullPath);

                // Create the new, empty file, and create printwriter object for output to it
                File outputFile = new File(fullPath);
                outputFile.createNewFile();
                //System.out.println("created output file");
                try (PrintWriter pw = new PrintWriter(outputFile)) {
                    PrintStringBuffer psw = new PrintStringBuffer(); //PES 05/01/2009
                    
                    //System.out.println("psw is " + PrintStringBuffer.class.getName());
                    //System.out.println("created pw, psw " + pw + ", " + psw.toString());
                    
                    //PES 05/01/2009 modified to print data to a stringbuilder prior to output to a file
                    //will use this to post process any changes
                    this.writeClass(psw, aClass);
                    //System.out.println("wrote class");
                    
                    //See if any post processing is needed
                    this.postProcessData(psw, aClass);
                    //System.out.println("post processed");
                    
                    // print the source code of the class to the file
                    pw.print(psw.toString());
                    pw.flush();
                } //PES 05/01/2009

            }
            catch (IOException e)
            {
                e.printStackTrace(System.err);
                System.err.println("error creating source code " + e);
            }

        } // End while

    } // End write classes

    /**
     * Generate a source code file with getters, setters, ivars, and marshal/unmarshal
     * methods for one class.
     * @param pw PrintWriter
     * @param aClass of interest
     */
    public void writeClass(PrintStringBuffer pw, GeneratedClass aClass) {
        // Note inside of the DIS XML1998 or XML1995 file the following needs to be inserted
        // <csharp namespace="DIS1998net" />  DIS1998net can be renamed to whatever the namespace is needed.

        this.writeLicenseNotice(pw);
        this.writeCopyrightNotice(pw);
        this.writeImports(pw, aClass);
        pw.println("using " + enumNamespace + ";");
        pw.println();
        // this.writeUsingStatements(pw, aClass);
        pw.println("using UInt8 = System.Byte;");
        pw.println();
        this.writeNamespace(pw);
        this.writeClassComments(pw, aClass, 1);
        this.writeClassDeclaration(pw, aClass, 1);
        this.writeIvars(pw, aClass, 2);
        this.writeConstructor(pw, aClass, 2);
        pw.println("#nullable enable");
            this.writeOperators(pw, aClass, 2);
            this.writeEqualityMethod(pw, aClass, 2);
        pw.println("#nullable disable");
        this.writeGetMarshalledSizeMethod(pw, aClass, 2);
        this.writeGettersAndSetters(pw, aClass, 2);
        this.writeExceptionHandler(pw, aClass, 2);
        this.writeToStringMethod(pw, aClass, 2);
        this.writeMarshalMethod(pw, aClass, 2);
        this.writeUnmarshallMethod(pw, aClass, 2);
        if(useDotNet)
        {
            this.writeReflectionMethod(pw, aClass, 2);
        }
        this.writeBitflagMethods(pw, aClass, 2);

        pw.println(1, "}");
        pw.println("}");
    }

    private void writeExceptionHandler(PrintStringBuffer pw, GeneratedClass aClass, int indent)
    {
        if (aClass.getParentClass().equalsIgnoreCase("root"))
        {
            pw.println(indent, "/// <summary>");
            pw.println(indent, "/// Occurs when exception when processing PDU is caught.");
            pw.println(indent, "/// </summary>");
            pw.println(indent, "public event Action<Exception> Exception;");
            pw.println();
            pw.println(indent, "/// <summary>");
            pw.println(indent, "/// Called when exception occurs (raises the <see cref=\"Exception\"/> event).");
            pw.println(indent, "/// </summary>");
            pw.println(indent, "/// <param name=\"e\">The exception.</param>");
            pw.println(indent, "protected void OnException(Exception e)");
            pw.println(indent, "{");
            pw.println(indent + 1, "if (this.Exception != null)");
            pw.println(indent + 1, "{");
            pw.println(indent + 2, "Exception(e);");
            pw.println(indent + 1, "}");
            pw.println(indent, "}");
            pw.println();
        }
    }

    /**
     * Writes the namespace and namespace using code at the top of the C# source file
     *
     * @param pw PrintWriter
     * @param aClass of interest
     */
    private void writeImports(PrintStringBuffer pw, GeneratedClass aClass) {

        // Write the various import statements
        String using = languageProperties.getProperty("using");
        StringTokenizer tokenizer = new StringTokenizer(using, ", ");
        while (tokenizer.hasMoreTokens()) {
            String aPackage = tokenizer.nextToken();
            pw.println("using " + aPackage + ";");
        }

        pw.println();
    }

    private void writeUsingStatements(PrintStringBuffer pw, GeneratedClass aClass)
    {
        Map<String, String> usingStatements = new HashMap<>();

        pw.println("using " + enumNamespace + ";");


        // Add using statements for any types used to initialize attributes
        List inits = aClass.getInitialValues();
        for (int idx = 0; idx < inits.size(); idx++) {
            GeneratedInitialValue anInit = (GeneratedInitialValue) inits.get(idx);

            String variableValue = anInit.getVariableValue();
            String enumType = StringUtils.getEnumType(variableValue);
            if (enumType != null)
            {
                usingStatements.put(enumType, enumNamespace + "." + enumType);
            }
        }

        
        for (int index = 0; index < aClass.getClassAttributes().size(); index++)
        {
            GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(index);
            // We probably already put this one out above
            // pw.println();
            // pw.println(anAttribute.ToString());
            // pw.println("/// Type : " + anAttribute.getType());
            if (anAttribute.getType() == null) continue;

            String useNamespace = globalNamespace;
            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
                useNamespace = enumNamespace;

            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
                (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF))
            {
                usingStatements.put(anAttribute.getType(), useNamespace + "." + anAttribute.getType());
            }

            // The object list may actually be a list of dis classes, dis enumerations, no great way of knowing
            // Could also be a list of system built in types
            // <objectlist countFieldName="numberOfRecords">
            //      <classRef name="RecordSpecificationElement"/>
            //      <sisoenum type="VariableRecordType" comment="uid = 66"/>

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
            {
                pw.println(anAttribute.ToString());
                if (anAttribute.underlyingTypeIsClass)
                    pw.println("/// Class : " + anAttribute.ToString());
                else if (anAttribute.underlyingTypeIsEnum)
                pw.println("/// Enum : " + anAttribute.ToString());
            }

        }

        for (Map.Entry<String,String> entry : usingStatements.entrySet())
        {
            pw.println("using " + entry.getKey() + " = " + entry.getValue() + ";");
            // pw.println("using " + entry.getKey + " = " entry.getValue() + ";");
        }

        if (usingStatements.size() > 0)
            pw.println("");

    }

    private void writeNamespace(PrintStringBuffer pw)
    {
        String namespace = languageProperties.getProperty("namespace");

        //if missing create default name
        if (namespace == null) {
            namespace = "DISnet";
        }

        pw.println("namespace " + namespace);
        pw.println("{");
    }

    /**
     * Write the class comments block
     * @param pw PrintWriter
     * @param aClass of interest
     */
    private void writeClassComments(PrintStringBuffer pw, GeneratedClass aClass, int indent) {
        if (aClass.getClassComments() != null) {
            pw.println(indent, "/// <summary>");
            pw.println(indent, "/// " + aClass.getClassComments());
            pw.println(indent, "/// </summary>");
        }
    }

    /**
     * Writes the class declaration, including any inheritence and interfaces
     *
     * @param pw PrintWriter
     * @param aClass of interest
     */
    private void writeClassDeclaration(PrintStringBuffer pw, GeneratedClass aClass, int indent)
    {
        // Class declaration
        String parentClass = aClass.getParentClass();
        pw.println("///    Parent Class : " + aClass.getParentClass());

        if(useDotNet)
        {
            // Added serializable attribute, additional tags will be needed for non-serializable and
            // if XML serialization will be used
            pw.println(indent, "[Serializable]");
            pw.println(indent, "[XmlRoot]");	// PES added for XML compatiblity
        }

        //Following will find the classes that are referenced within the current class being processed
        //These will then be added to the Xmlinclude attribute to allow the reflection of those classes
        List ivars = aClass.getClassAttributes();
        List<String> referencedClasses = new ArrayList<>();

        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute) ivars.get(idx);

            //String attributeType = types.getProperty(anAttribute.getType());

            //if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
            //{

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF && useDotNet) {
                if (!referencedClasses.contains(anAttribute.getType())) {
                    referencedClasses.add(anAttribute.getType());
                    pw.println(indent, "[XmlInclude(typeof(" + anAttribute.getType() + "))]");
                }

                //pw.println("   protected " + attributeType + "  " + anAttribute.getName() + " = new " + attributeType + "(); \n");
            }
            //}

            if (anAttribute.listIsClass() == true && useDotNet) {
                pw.println(indent, "[XmlInclude(typeof(" + anAttribute.getType() + "))]");
            }
        }

        // PES 12-02-2009 added based upon user "Rogier" request
        // ZB modified
        if (parentClass.equalsIgnoreCase("root"))
        {
            if (aClass.getName().equals("Pdu"))
            {
                pw.println(indent, "public partial class " + aClass.getName() + ": IEquatable<" + aClass.getName() + ">");
            }
            else
            {
                pw.println(indent, "public class " + aClass.getName());
            }
        }
        else
        {
            pw.println(indent, "public class " + aClass.getName() + " : " + parentClass + ", IEquatable<" + aClass.getName() + ">");
        } 

        pw.println(indent, "{");
    }

    private void writeIvars(PrintStringBuffer pw, GeneratedClass aClass, int indent) {
        List ivars = aClass.getClassAttributes();

        String tabIndent = StringUtils.tabs(indent);

if (debugAttributes.equals("true"))
{
    pw.println("");
    pw.println("/// ATTRIBUTES");
    pw.println("/// Class " + aClass.getName() + " has " + ivars.size() + " Attributes");
    for (int idx = 0; idx < aClass.getClassAttributes().size(); idx++) {
            GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
            pw.println("    ///    " + IVAR_PREFIX + anAttribute.getName() + "\t : " + anAttribute.getAttributeKind());;
    }
    pw.println("");
}    

        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute) ivars.get(idx);

            String typeName = anAttribute.getType();
            String attributeName = anAttribute.getName();
            String uidValue = anAttribute.getUnderlyingUid();
            String aliasName = uid2ClassName.getProperty(uidValue);
            String defaultValue = anAttribute.getDefaultValue();

if (debugAttributes.equals("true"))
{                
    pw.println("/// --------------------------------------------------------------------------");
    pw.println("/// ATTRIBUTE");
    pw.println(anAttribute.ToString());
}    
            // Need to do some magic to output a capability type
            if (uidValue.equals("55"))
            {
                aliasName = "ICapabilities";
            }
if (debugAttributes.equals("true"))
{                
    pw.println("///           Alias : " + aliasName);
    pw.println("/// --------------------------------------------------------------------------");
    pw.println("");
}

            if (anAttribute.getComment() != null)
            {
                pw.println(indent, "/// <summary>");
                pw.println(indent, "/// " + anAttribute.getComment());
                pw.println(indent, "/// </summary>");
            }

            if((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO16) ||
               (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO32) ||
               (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO64))
            {
                String bitCount = String.format("%s", anAttribute.getAttributeKind());
                bitCount = bitCount.substring(bitCount.length() - 2);

                pw.print(indent, "private " + "UInt8[] " + IVAR_PREFIX + attributeName + " = new UInt8[" + bitCount + "]" + ";"); //Create standard type using underscore
            }

            // This attribute is a primitive.
            else if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE) ||
                     (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.STATIC_IVAR))
            {
                // The primitive type--we need to do a lookup from the abstract type in the
                // xml to the C#-specific type. The output should look something like
                //
                // /** This is a description */
                // protected int foo;
                //
                String attributeType = types.getProperty(anAttribute.getType());
                
                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.STATIC_IVAR)
                    pw.print(indent, "public static " + attributeType + " " + attributeName); //Create standard type using underscore
                else
                    pw.print(indent, "private " + attributeType + " " + IVAR_PREFIX + attributeName); //Create standard type using underscore
                
                //if (defaultValue != null && !typeDefaultValue.getProperty(attributeType).equals(defaultValue))
                if (defaultValue == null)
                {
                    // get the attribute type for the marshalled value
                    defaultValue = typeDefaultValue.getProperty(attributeType);
                }

                pw.print(" = (" + attributeType + ")" + defaultValue);

                pw.println(";");
            } // end of primitive attribute type


            // this attribute is a reference to another class defined in the XML document, The output should look like
            //
            // /** This is a description */
            // protected AClass foo = new AClass();
            //
            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
            {
                String attributeType = anAttribute.getType();
                pw.println(indent, "/// <summary>");
                if (anAttribute.getComment() != null)
                {
                    pw.println(indent, "/// " + anAttribute.getComment());
                }
                else 
                {
                    pw.println(indent, "/// " + IVAR_PREFIX + anAttribute.getName() + " is an undescribed parameter... ");
                }
                pw.println(indent, "/// </summary>");

                pw.println(indent, "/// TODO - Process the default value for a CLASSREF type");

                pw.println(indent, "private " + attributeType + " " + IVAR_PREFIX + attributeName + " = new " + attributeType + "();");
            }

            // The attribute is a fixed list, ie an array of some type--maybe primitve, maybe a class.

            else if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST))
            {
                String attributeType = anAttribute.getType();
                int listLength = anAttribute.getListLength();
                String listLengthString = "" + listLength;

                if (anAttribute.getComment() != null)
                {
                    pw.println(indent, "/// <summary>");
                    pw.println(indent, "/// " + anAttribute.getComment());
                    pw.println(indent, "/// </summary>");
                }

                if (anAttribute.getUnderlyingTypeIsPrimitive() == true)
                {
                    pw.println(indent, "private " + types.getProperty(attributeType) + "[] " + IVAR_PREFIX + anAttribute.getName() + " = new "
                            + types.getProperty(attributeType) + "[" + listLengthString + "]" + ";");
                } 
                else if (anAttribute.listIsClass() == true)
                {
                    pw.println(indent, "private " + attributeType + "[] " + IVAR_PREFIX + anAttribute.getName() + " = new "
                            + attributeType + "[" + listLengthString + "]" + ";");
                }
            }

            // The attribute is a variable list of some kind.
            else if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)) {
                if (anAttribute.getComment() != null) {
                    pw.println(indent, "/// <summary>");
                    pw.println(indent, "/// " + anAttribute.getComment());
                    pw.println(indent, "/// </summary>");
                }

                //PES 04/29/2009  Added to speed up unboxing of data
                if (anAttribute.getType().equalsIgnoreCase("OneByteChunk")) {
                    pw.println(indent, "private byte[] " + IVAR_PREFIX + anAttribute.getName() + "; ");
                } 
                else
                {
                    //Make the list referenced to the type that will be stored within 01/21/2009 PES
                    pw.println(indent, "private List<" + anAttribute.getType() + "> " + IVAR_PREFIX + anAttribute.getName() + " = new List<" + anAttribute.getType() + ">();");
                }
            }

            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
            {

                uidValue = anAttribute.getUnderlyingUid();

                if (defaultValue != null)
                {
                    String defaultValueTokens[] = defaultValue.split("\\.");
                    if (defaultValueTokens[1] != null)
                    {
                        defaultValue = defaultValueTokens[0] + "." + defaultValueTokens[1].toLowerCase();
                    }
                }

                if ((anAttribute.getComment() != null) && !anAttribute.getComment().trim().isEmpty())
                {
                    pw.println(tabIndent + "/** " + anAttribute.getComment() + " */");
                }
                else 
                {
                    pw.println(tabIndent + "/** " + IVAR_PREFIX + anAttribute.getName() + " is an undescribed parameter... */");
                }

                if (anAttribute.getDefaultValue() == null)
                {
                    pw.println(String.format("%sprivate %s %s = default;\n", tabIndent, typeName, IVAR_PREFIX + anAttribute.getName()));
                }
                else
                {                       
                    pw.println(tabIndent + " private " + typeName + " " + IVAR_PREFIX + anAttribute.getName() + " = " + defaultValue + ";\n");
                }
            }
            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD)
            {
                if ((anAttribute.getComment() != null) && !anAttribute.getComment().trim().isEmpty())
                {
                    pw.println(tabIndent + "/** " + anAttribute.getComment() + " */");
                }
                else 
                {
                    pw.println(tabIndent + "/** " + IVAR_PREFIX + anAttribute.getName() + " is an undescribed parameter... */");
                }

                // if (aliasName != null && !aliasName.isEmpty() && !typeName.equals(aliasName)) typeName = aliasName;
                if (anAttribute.getDefaultValue() == null)
                {
                    pw.println(String.format("%sprivate %s %s = default;\n", tabIndent, typeName, IVAR_PREFIX + anAttribute.getName()));
                }
                else
                {
                    if (aliasName.equals(typeName))                    
                        pw.println(tabIndent + " private " + typeName + " " + IVAR_PREFIX + anAttribute.getName() + " = " + defaultValue + ";\n");
                    else
                        pw.println(tabIndent + " private " + aliasName + " " + IVAR_PREFIX + anAttribute.getName() + " = (" + aliasName + ")" + defaultValue + ";\n");
                }

            }
            else
            {
                pw.println(indent + 1, "writeIvars : " + anAttribute.getAttributeKind()  + " - Kind not handled.  That's  why you got the compile error!");
            }

            pw.println();
        } // End of loop through ivars
    }

    private void writeConstructor(PrintStringBuffer pw, GeneratedClass aClass, int indent) {
        List ivars = aClass.getClassAttributes();

         // PES 01/22/2009  Added for intellisense support
        if (aClass.getClassComments() != null)
        {
            pw.println(indent, "/// <summary>");
            pw.println(indent, "/// Initializes a new instance of the <see cref=\"" + aClass.getName() + "\"/> class.");
            pw.println(indent, "/// </summary>");
        }
        
        pw.println(indent, "public " + aClass.getName() + "()");
        pw.println(indent, "{");

        // Set primitive types with initial values
        List inits = aClass.getInitialValues();
        for (int idx = 0; idx < inits.size(); idx++) {
            GeneratedInitialValue anInit = (GeneratedInitialValue) inits.get(idx);

            // This is irritating. we have to match up the attribute name with the type,
            // so we can do a cast. Otherwise java pukes because it wants to interpret all
            // numeric strings as ints or doubles, and the attribute may be a short.

            boolean found = false;
            GeneratedClass currentClass = aClass;
            String aType = null;

            while (currentClass != null) {
                List thisClassesAttributes = currentClass.getClassAttributes();
                
                for (int jdx = 0; jdx < thisClassesAttributes.size(); jdx++) {
                    GeneratedClassAttribute anAttribute = (GeneratedClassAttribute) thisClassesAttributes.get(jdx);

                    if (anInit.getVariable().equals(anAttribute.getName())) {
                        found = true;
                        aType = anAttribute.getType();
                        break;
                    }
                }
                currentClass = classDescriptions.get(currentClass.getParentClass());
            }

            if (!found) {
                System.out.println("Could not find initial value matching attribute name for " + anInit.getVariable() + " in class " + aClass.getName());
            } else {
                pw.println("// HERE?");
                    String variableValue = anInit.getVariableValue();
                    pw.println(anInit.ToString());

                    if (variableValue != null)
                    {
                        variableValue = StringUtils.setEnumValueToLowerCase(variableValue);
                    }

                    //PES modified the InitalValue.java class to provide a method name that would work with the changes made in this file
                    // ZB: only initialize if the value is not the same as the default type value

                    // if the variable value is not already the default value
                    if (!anInit.getVariableValue().equals(typeDefaultValue.getProperty(aType)))
                    {
                        if (StringUtils.isEnumType(variableValue))
                        {
                            String enumType = StringUtils.getEnumType(variableValue);
                            String enumValue = StringUtils.getEnumValue(variableValue);
                            pw.println(indent +1, StringUtils.firstCharUpper(anInit.getVariable()) + " = " + enumType + "." + enumValue + ";");
                        }
                        else
                        {
                            // Use the setter and value ?  no.
                            pw.println(indent +1, StringUtils.firstCharUpper(anInit.getVariable()) + " = " + variableValue + ";");
                            // pw.println(indent +1, StringUtils.firstCharUpper(anInit.getVariable()) + " = " 
                            //                       + anInit.getSetterMethodName()
                            //                       + "("
                            //                       + variableValue
                            //                       + ")"
                            //                       + ";");
                        }
                    }
            }
        } // End initialize initial values

        // If we have fixed lists with object instances in them, initialize thos

        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute) ivars.get(idx);

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST) {
                //System.out.println("Generating constructor fixed list for " + anAttribute.getName() + " listIsClass:" + anAttribute.listIsClass());
                if (anAttribute.listIsClass() == true) {
                    pw.println(indent + 1, "");
                    pw.println(indent + 1, "for (int idx = 0; idx < " + anAttribute.getName() + ".Length; idx++)");
                    pw.println(indent + 1, "{");
                    pw.println(indent + 2, anAttribute.getName() + "[idx] = new " + anAttribute.getType() + "();");
                    pw.println(indent + 1, "}");
                }
            }
        }
        pw.println(indent, "}");
    }

    /**
     * Write out method to get marshalled size
     * @param pw output
     * @param aClass input
     * @param indent indentation
     */
    public void writeGetMarshalledSizeMethod(PrintStringBuffer pw, GeneratedClass aClass, int indent) {
        List ivars = aClass.getClassAttributes();

        String newKeyword = ""; //PES 032209 added to remove warning from C# compiler
        //PES 032209 added to remove warning from C# compiler
        if (!aClass.getParentClass().equalsIgnoreCase("root")) {
            newKeyword = "new ";
        } else {
            newKeyword = "";
        }
        // Create a getMarshalledSize() method
        pw.println();
        pw.println(indent, "public " + newKeyword + "Int32 GetMarshaledSize()");
        pw.println(indent, "{");

        // Size of superclass is the starting point
        if (!aClass.getParentClass().equalsIgnoreCase("root")) {
            pw.println(indent + 1, "int marshalSize = base.GetMarshaledSize();");
        }
        else
        {
            pw.println(indent + 1, "int marshalSize = 0; ");
        }

        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute) ivars.get(idx);
// pw.println();
// pw.println(anAttribute.ToString());

            if(anAttribute.getComment() != null) {
                pw.println(indent +1, "/** " + anAttribute.getComment() + " */");
            }

            if((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO16) ||
               (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO32) ||
               (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO64))
            {
                String bitCount = String.format("%s", anAttribute.getAttributeKind());
                bitCount = bitCount.substring(bitCount.length() - 2);
                String padType = "UInt8";

                pw.print(indent +1, "marshalSize += " + "sizeof(" + padType + ") * " + bitCount + ";\n");
            }

            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) {
                pw.print(indent +1, "marshalSize += ");
                pw.println(String.format("%s_Accessors.GetMarshaledSize(%s);", 
                                         anAttribute.getType(),
                                         IVAR_PREFIX + anAttribute.getName()));
            }
            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD) {
                     pw.print(indent +1, "marshalSize += ");
                     pw.println(String.format("%s.GetMarshaledSize();", 
                                         IVAR_PREFIX + anAttribute.getName()));
            }

            else if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE) ||
                     (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.STATIC_IVAR))
            {
                     pw.print(indent + 1, "marshalSize += ");
                     pw.println(primitiveSizes.get(anAttribute.getType()) + ";  // this." + IVAR_PREFIX + anAttribute.getName());
            }

            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF) {
                pw.print(indent + 1, "marshalSize += ");
                pw.println("this." + IVAR_PREFIX + anAttribute.getName() + ".GetMarshaledSize();  // this." + IVAR_PREFIX + anAttribute.getName());
            }

            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST) {
                //System.out.println("Generating fixed list for " + anAttribute.getName() + " listIsClass:" + anAttribute.listIsClass());
                // If this is a fixed list of primitives, it's the list size times the size of the primitive.
                if (anAttribute.getUnderlyingTypeIsPrimitive() == true) {
                    pw.println(indent + 1, "marshalSize += " + IVAR_PREFIX + anAttribute.getName() + ".Length" + " * " + primitiveSizes.get(anAttribute.getType()) + ";  // _" + anAttribute.getName());
                } else if (anAttribute.listIsClass() == true) {
                    
                    pw.println("");
                    pw.println(indent + 1, "for (int idx = 0; idx < " + IVAR_PREFIX + anAttribute.getName() + ".Length; idx++)");
                    pw.println(indent + 1, "{");
                    pw.println(indent + 2, "marshalSize += this." + IVAR_PREFIX + anAttribute.getName() + "[idx].GetMarshaledSize();");
                    pw.println(indent + 1, "}");
                    pw.println();
                } else {
                    //pw.println( anAttribute.getListLength() + " * " +  " new " + anAttribute.getType() + "().getMarshaledSize()"  + ";  // _" + anAttribute.getName());
                    pw.println(indent + 1, "THIS IS A CONDITION NOT HANDLED BY XMLPG: a fixed list array of lists. That's  why you got the compile error.");
                }
            }

            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST) {
                // If this is a dynamic list of primitives, it's the list size times the size of the primitive.

                ClassAttributeType underlyingKind = anAttribute.getUnderlyingKind();

                if (anAttribute.getUnderlyingTypeIsPrimitive() == true) {
                    pw.println(indent + 1, "this._" + anAttribute.getName() + ".Count " + " * " + primitiveSizes.get(anAttribute.getType()) + ";  // " + anAttribute.getName());
                } else {
                    //PES 04/29/2009  Added to speed up unboxing of data, using byte[] vice unboxing of a Class ie. OneByteChunk
                    if (anAttribute.getType().equalsIgnoreCase("OneByteChunk")) {
                        pw.println(indent + 1, "marshalSize += this._" + anAttribute.getName() + ".Length;");
                    } else {
                        pw.println(indent + 1, "for (int idx = 0; idx < this." + IVAR_PREFIX + anAttribute.getName() + ".Count; idx++)");
                        pw.println(indent + 1, "{");
                        pw.println(indent + 2, "var listElement = " + IVAR_PREFIX + anAttribute.getName() + "[idx];");

                        if (underlyingKind == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
                            pw.println(indent + 2, "marshalSize += " + anAttribute.getType() + "_Accessors.GetMarshaledSize(listElement);");
                        else
                            pw.println(indent + 2, "marshalSize += listElement.GetMarshaledSize();");
                        pw.println(indent + 1, "}");
                        pw.println();
                    }
                }
            }
            else
            {
                pw.println("TODO : This kind " + anAttribute.getAttributeKind() + " is not included in the size calculation");
                
            }
        }
        pw.println();
        pw.println(indent + 1, "return marshalSize;");
        pw.println(indent, "}");
        pw.println();
    }

    private void writeOperators(PrintStringBuffer pw, GeneratedClass aClass, int indent) {

        pw.println();
        pw.println(indent, "/// <summary>");
        pw.println(indent, "/// Implements the operator !=.");
        pw.println(indent, "/// </summary>");
        pw.println(indent, "/// <param name=\"left\">The left operand.</param>");
        pw.println(indent, "/// <param name=\"right\">The right operand.</param>");
        pw.println(indent, "/// <returns>");
        pw.println(indent, "/// 	<c>true</c> if operands are not equal; otherwise, <c>false</c>.");
        pw.println(indent, "/// </returns>");
        pw.println(indent, "public static bool operator !=(" + aClass.getName() + "? left, " + aClass.getName() + "? right) => !(left == right);");
        pw.println();
        pw.println(indent, "/// <summary>");
        pw.println(indent, "/// Implements the operator ==.");
        pw.println(indent, "/// </summary>");
        pw.println(indent, "/// <param name=\"left\">The left operand.</param>");
        pw.println(indent, "/// <param name=\"right\">The right operand.</param>");
        pw.println(indent, "/// <returns>");
        pw.println(indent, "/// 	<c>true</c> if both operands are equal; otherwise, <c>false</c>.");
        pw.println(indent, "/// </returns>");
        pw.println(indent, "public static bool operator ==(" + aClass.getName() + "? left, " + aClass.getName() + "? right)");
        pw.println(indent, "{");
        pw.println(indent + 1, "if (object.ReferenceEquals(left, right))");
        pw.println(indent + 1, "{");
        pw.println(indent + 2, "return true;");
        pw.println(indent + 1, "}");
        pw.println();
        pw.println(indent + 1, "if ((left is null) || (right is null))");
        pw.println(indent + 1, "{");
        pw.println(indent + 2, "return false;");
        pw.println(indent + 1, "}");
        pw.println();
        pw.println(indent + 1, "return left.Equals(right);");
        pw.println(indent, "}");
    }

    private void writePropertySummary(PrintStringBuffer pw, GeneratedClassAttribute anAttribute, int indent) {
        if (anAttribute.getComment() != null) { //PES 01/22/2009  Added for intellisense support
            pw.println(indent, "/// <summary>");
            pw.println(indent, "/// Gets or sets the " + anAttribute.getComment());
            pw.println(indent, "/// </summary>");
        }
    }
    
    /**
     * Some fields have integers with bit fields defined, eg an integer where 
     * bits 0-2 represent some value, while bits 3-4 represent another value, 
     * and so on.This writes accessor and mutator methods for those fields.
     * 
     * @param pw PrintWriter
     * @param aClass of interest 
     * @param indent number of 4-character whitespace indents
     */
    public void writeBitflagMethods(PrintStringBuffer pw, GeneratedClass aClass, int indent)
    {
        List<GeneratedClassAttribute> attributes = aClass.getClassAttributes();
        
        for(int idx = 0; idx < attributes.size(); idx++)
        {
            GeneratedClassAttribute anAttribute = attributes.get(idx);
           
            
            switch(anAttribute.getAttributeKind())
            {
                
                // Anything with bitfields must be a primitive type
                case PRIMITIVE:
                    
                    List bitfields = anAttribute.bitFieldList;
   
                    for(int jdx = 0; jdx < bitfields.size(); jdx++)
                    {
                        GeneratedBitField bitfield = (GeneratedBitField)bitfields.get(jdx);
                        String capped = this.initialCapital(bitfield.name);
                        int shiftBits = super.getBitsToShift(anAttribute, bitfield.mask);
                        String attributeType = types.getProperty(anAttribute.getType());
                        
                        // write getter
                        pw.println();
                        if(bitfield.description != null)
                        {
                            pw.println( "// " + bitfield.description );
                        }
                        
                        pw.println("public int get" + capped + "()");
                        pw.println("{");
                        
                        
                        pw.println("    int val = this._" + bitfield.parentAttribute.getName() + " & " + bitfield.mask + ";");
                        pw.println("    val = val >> " + shiftBits + ";");
                        pw.println("    return val;");
                        pw.println("}\n");
                        
                        // Write the setter/mutator
                        
                        pw.println();
                        if(bitfield.description != null)
                        {
                            pw.println( "// " + bitfield.description);
                        }
                        pw.println("public void set" + capped + "(int val)");
                        pw.println("{");
                        pw.println("    " + attributeType + " aVal = (" + attributeType + ")val;");
                        pw.println("    " + attributeType + " mask = (" + attributeType + ")("+ bitfield.mask + ");   // type dance");
                        pw.println("    aVal = (" + attributeType + ")(aVal << " + shiftBits + ");");
                        pw.println("    _" + anAttribute.getName() + " = (" + attributeType + ")(_" + anAttribute.getName() + " & ~mask); // clear" );
                        pw.println("    _" + anAttribute.getName() + " = (" + attributeType + ")(_" + anAttribute.getName() + " | aVal);  // set");
                        pw.println("}\n");
                    }
                    
                    break;
                    
                default:
                    bitfields = anAttribute.bitFieldList;
                    if(!bitfields.isEmpty())
                    {
                        System.out.println("Attempted to use bit flags on a non-primitive field");
                        System.out.println( "Field: " + anAttribute.getName() );
                    }
            }
        
        }
    }
    
    
    private void writeGettersAndSetters(PrintStringBuffer pw, GeneratedClass aClass, int indent) {
        List ivars = aClass.getClassAttributes();

        String classNameConflictModifier;

        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute) ivars.get(idx);

            pw.println();
            pw.println(anAttribute.ToString());

            classNameConflictModifier = ""; //Used to modify the get/set public accessor if class name is the same

            //Check to see if conflict with Class name or C# key words.  Appended underscore as a temporary workaround.  Also note that
            //the key words and class names should be put into a collection to make future testing easier.
            if (aClass.getName().equals(this.initialCapital(anAttribute.getName())) || anAttribute.getName().equalsIgnoreCase("system")) {
                classNameConflictModifier = "_";
            }

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
            {
                String beanType = types.getProperty(anAttribute.getType());
                    writePropertySummary(pw, anAttribute, indent);
                    // if(useDotNet)
                    // {
                    //     pw.println(indent, "[XmlElement(Type = typeof(" + beanType + "), ElementName = \"" + anAttribute.getName() + "\")]");
                    // }
                    
                    pw.println(indent, "public " + anAttribute.getType() + " " + this.initialCapital(anAttribute.getName()) + classNameConflictModifier);
                    pw.println(indent, "{");
                    pw.println(indent + 1, "get => " + IVAR_PREFIX + anAttribute.getName() + ";");
                    pw.println(indent + 1, "set => " + IVAR_PREFIX + anAttribute.getName() + " = value;");
                    pw.println(indent, "}");
                    
                    pw.println();
            }

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE) {
                if (anAttribute.getIsDynamicListLengthField() == false) {
                    String beanType = types.getProperty(anAttribute.getType());

//                    writePropertySummary(pw, anAttribute, indent);
//                    pw.println(indent, "public void set" + this.initialCapital(anAttribute.getName()) + "(" + beanType + " p" + this.initialCapital(anAttribute.getName()) + ")");
//                    pw.println(indent, "{ ");
//                    pw.println(indent + 1, "_" + anAttribute.getName() + " = p" + this.initialCapital(anAttribute.getName()) + ";");
//                    pw.println(indent, "}");
//
//                    pw.println();

                    writePropertySummary(pw, anAttribute, indent);
                    if(useDotNet)
                    {
                        pw.println(indent, "[XmlElement(Type = typeof(" + beanType + "), ElementName = \"" + anAttribute.getName() + "\")]");
                    }
                    
                    pw.println(indent, "public " + beanType + " " + this.initialCapital(anAttribute.getName()) + classNameConflictModifier);
                    pw.println(indent, "{");
                    pw.println(indent + 1, "get => " + IVAR_PREFIX + anAttribute.getName() + ";");
                    pw.println(indent + 1, "set => " + IVAR_PREFIX + anAttribute.getName() + " = value;");
                    pw.println(indent, "}");
                    
                    pw.println();
                } else // This is the count field for a dynamic list
                {//PES 01/21/2009 added back in to account for getting length on dynamic lists
                    String beanType = types.getProperty(anAttribute.getType());
                    GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();

//                    pw.println(indent, "/// <summary>");
//                    pw.println(indent, "/// Note that setting this value will not change the marshalled value. The list whose length this describes is used for that purpose.");
//                    pw.println(indent, "/// The get" + anAttribute.getName() + " method will also be based on the actual list length rather than this value. ");
//                    pw.println(indent, "/// The method is simply here for completeness and should not be used for any computations.");
//                    pw.println(indent, "/// </summary>");
//                    pw.println(indent, "public void set" + this.initialCapital(anAttribute.getName()) + "(" + beanType + " p" + this.initialCapital(anAttribute.getName()) + ")");
//                    pw.println(indent, "{");
//                    pw.println(indent + 1, "_" + anAttribute.getName() + " = p" + this.initialCapital(anAttribute.getName()) + ";");
//                    pw.println(indent, "}");
//
//                    pw.println();

                    pw.println(indent, "/// <summary>");
                    pw.println(indent, "/// Note that setting this value will not change the marshalled value. The list whose length this describes is used for that purpose.");
                    pw.println(indent, "/// The get" + anAttribute.getName() + " method will also be based on the actual list length rather than this value. ");
                    pw.println(indent, "/// The method is simply here for completeness and should not be used for any computations.");
                    pw.println(indent, "/// </summary>");
                    if(useDotNet)
                    {
                        pw.println(indent, "[XmlElement(Type = typeof(" + beanType + "), ElementName = \"" + anAttribute.getName() + "\")]");
                    }
                    
                    pw.println(indent, "public " + beanType + " " + this.initialCapital(anAttribute.getName()) + classNameConflictModifier);
                    pw.println(indent, "{");
                    pw.println(indent + 1, "get => " + IVAR_PREFIX + anAttribute.getName() + ";");
                    pw.println(indent + 1, "set => " + IVAR_PREFIX + anAttribute.getName() + " = value;");
                    pw.println(indent, "}");
                    pw.println();
                }

            } // End is primitive

            // The attribute is a class of some sort. Generate getters and setters.

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF) {
//                writePropertySummary(pw, anAttribute, indent);
//                pw.println(indent, "public void set" + this.initialCapital(anAttribute.getName()) + "(" + anAttribute.getType() + " p" + this.initialCapital(anAttribute.getName()) + ")");
//                pw.println(indent, "{ ");
//                pw.println(indent + 1, "_" + anAttribute.getName() + " = p" + this.initialCapital(anAttribute.getName()) + ";");
//                pw.println(indent, "}");
//                pw.println();
//
//                writePropertySummary(pw, anAttribute, indent);
//                pw.println(indent, "public " + anAttribute.getType() + " get" + this.initialCapital(anAttribute.getName()) + "()");
//                pw.println(indent, "{");
//                pw.println(indent + 1, "return _" + anAttribute.getName() + ";");
//                pw.println(indent, "}");
//                pw.println();

                writePropertySummary(pw, anAttribute, indent);
                if(useDotNet)
                {
                    pw.println(indent, "[XmlElement(Type = typeof(" + anAttribute.getType() + "), ElementName = \"" + anAttribute.getName() + "\")]");
                }
                pw.println(indent, "public " + anAttribute.getType() + " " + this.initialCapital(anAttribute.getName()) + classNameConflictModifier);
                pw.println(indent, "{");
                pw.println(indent + 1, "get => " + IVAR_PREFIX + anAttribute.getName() + ";");
                pw.println(indent + 1, "set => " + IVAR_PREFIX + anAttribute.getName() + " = value;");
                pw.println(indent, "}");
                pw.println();
            }

            // The attribute is an array of some sort. Generate getters and setters.
            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)) {
                if (anAttribute.getUnderlyingTypeIsPrimitive()) {
//                    writePropertySummary(pw, anAttribute, indent);
//                    pw.println(indent, "public void set" + this.initialCapital(anAttribute.getName()) + "(" + types.getProperty(anAttribute.getType()) + "[] p" + this.initialCapital(anAttribute.getName()) + ")");
//                    pw.println(indent, "{");
//                    pw.println(indent + 1, "_" + anAttribute.getName() + " = p" + this.initialCapital(anAttribute.getName()) + ";");
//                    pw.println(indent, "}");
//                    pw.println();
//
//                    writePropertySummary(pw, anAttribute, indent);
//                    //pw.println("@XmlElement(name=\"" + anAttribute.getName() + "\" )");
//                    pw.println(indent, "public " + types.getProperty(anAttribute.getType()) + "[] get" + this.initialCapital(anAttribute.getName()) + "()");
//                    pw.println(indent, "{");
//                    pw.println(indent + 1, "return _" + anAttribute.getName() + ";");
//                    pw.println(indent, "}");
//                    pw.println();

                    writePropertySummary(pw, anAttribute, indent);
                    if(useDotNet)
                    {
                         pw.println(indent, "[XmlArray(ElementName = \"" + anAttribute.getName() + "\")]");
                    }
                    pw.println(indent, "public " + types.getProperty(anAttribute.getType()) + "[] " + this.initialCapital(anAttribute.getName()) + classNameConflictModifier);
                    pw.println(indent, "{");
                    pw.println(indent + 1, "get => " + IVAR_PREFIX + anAttribute.getName() + ";");
                    pw.println(indent + 1, "set => " + IVAR_PREFIX + anAttribute.getName() + " = value;");
                    pw.println(indent, "}");
                    pw.println();

                } else if (anAttribute.listIsClass() == true) {
//                    writePropertySummary(pw, anAttribute, indent);
//                    pw.println(indent, "public void set" + this.initialCapital(anAttribute.getName()) + "(" + anAttribute.getType() + "[] p" + this.initialCapital(anAttribute.getName()) + ")");
//                    pw.println(indent, "{");
//                    pw.println(indent + 1, "_" + anAttribute.getName() + " = p" + this.initialCapital(anAttribute.getName()) + ";");
//                    pw.println(indent, "}");
//                    pw.println();
//
//                    writePropertySummary(pw, anAttribute, indent);
//                    //pw.println("@XmlElementWrapper(name=\"" + anAttribute.getName() + "Array\" )");
//                    pw.println(indent, "public " + anAttribute.getType() + "[] get" + this.initialCapital(anAttribute.getName()) + "()");
//                    pw.println(indent, "{");
//                    pw.println(indent + 1, "return _" + anAttribute.getName() + ";");
//                    pw.println(indent, "}");
//                    pw.println();

                    writePropertySummary(pw, anAttribute, indent);
                    if(useDotNet)
                    {
                        pw.println(indent, "[XmlArrayItem(ElementName = \"" + anAttribute.getName() + "Array\", DataType = \"" + anAttribute.getType() + "\"))]");
                    }
                    pw.println(indent, "public " + anAttribute.getType() + "[] " + this.initialCapital(anAttribute.getName()));
                    pw.println(indent, "{");
                    pw.println(indent + 1, "get => " + IVAR_PREFIX + anAttribute.getName() + ";");
                    pw.println(indent + 1, "set => " + IVAR_PREFIX + anAttribute.getName() + " = value;");
                    pw.println(indent, "}");
                    pw.println();
                }
            }

            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)) {	//Set List to the actual type 01/21/2009 PES

                //PES 04/29/2009  Added to speed up unboxing of data, using byte[] vice unboxing of a Class ie. OneByteChunk
                if (anAttribute.getType().equalsIgnoreCase("OneByteChunk")) {
//                    pw.println(indent, "public void set" + this.initialCapital(anAttribute.getName()) + "(byte[] p" + this.initialCapital(anAttribute.getName()) + ")");
//                    pw.println(indent, "{");
//                    pw.println(indent + 1, "_" + anAttribute.getName() + " = p" + this.initialCapital(anAttribute.getName()) + ";");
//                    pw.println(indent, "}");
//                    pw.println();
//
//                    //Set List to actual type 01/21/2009 PES
//                    //pw.println("@XmlElementWrapper(name=\"" + anAttribute.getName() + "List\" )");
//                    writeClassAttributeSummary(pw, anAttribute, indent);
//                    pw.println(indent, "public byte[] get" + this.initialCapital(anAttribute.getName()) + "()");
//                    pw.println(indent, "{");
//                    pw.println(indent + 1, "return _" + anAttribute.getName() + ";");
//                    pw.println(indent, "}");
//                    pw.println();

                    writePropertySummary(pw, anAttribute, indent);
                    if(useDotNet)
                    {
                        pw.println(indent, "[XmlElement(ElementName = \"" + anAttribute.getName() + "List\", DataType = \"hexBinary\")]");
                    }
                    
                    pw.println(indent, "public byte[] " + this.initialCapital(anAttribute.getName()));
                    pw.println(indent, "{");
                    pw.println(indent + 1, "get => " + IVAR_PREFIX + anAttribute.getName() + ";");
                    pw.println(indent + 1, "set => " + IVAR_PREFIX + anAttribute.getName() + " = value;");
                    pw.println(indent, "}");
                    pw.println();

                } else {
//                    pw.println(indent, "public void set" + this.initialCapital(anAttribute.getName()) + "(List<" + anAttribute.getType() + ">" + " p" + this.initialCapital(anAttribute.getName()) + ")");
//                    pw.println(indent, "{");
//                    pw.println(indent + 1, "_" + anAttribute.getName() + " = p" + this.initialCapital(anAttribute.getName()) + ";");
//                    pw.println(indent, "}");
//                    pw.println();
//
//                    //Set List to actual type 01/21/2009 PES
//                    //pw.println("@XmlElementWrapper(name=\"" + anAttribute.getName() + "List\" )");
//                    writeClassAttributeSummary(pw, anAttribute, indent);
//                    pw.println(indent, "public List<" + anAttribute.getType() + ">" + " get" + this.initialCapital(anAttribute.getName()) + "()");
//                    pw.println(indent, "{");
//                    pw.println(indent + 1, "return _" + anAttribute.getName() + ";");
//                    pw.println(indent, "}");
//                    pw.println();

                    writePropertySummary(pw, anAttribute, indent);
                    if(useDotNet)
                    {
                        pw.println(indent, "[XmlElement(ElementName = \"" + anAttribute.getName() + "List\", Type = typeof(List<" + anAttribute.getType() + ">))]");
                    }
                    pw.println(indent, "public List<" + anAttribute.getType() + "> " + this.initialCapital(anAttribute.getName()));
                    pw.println(indent, "{");
                    pw.println(indent + 1, "get => " + IVAR_PREFIX + anAttribute.getName() + ";");
//                    pw.println(indent + 1, "set");
//                    pw.println(indent + 1, "{");
//                    pw.println(indent + 2, "this._" + anAttribute.getName() + " = value;");
//                    pw.println(indent + 1, "}");
                    pw.println(indent, "}");
                    pw.println();
                }
            }
        } // End of loop trough writing getter/setter methods

    }

    private void writeToStringMethod(PrintStringBuffer pw, GeneratedClass aClass, int indent)
    {

        List<GeneratedClassAttribute> ivars;

        String newKeyword = "";
        if (aClass.getParentClass().equalsIgnoreCase("root")) {
            newKeyword = "override";
        } else {
            newKeyword = "new";
        }

        pw.println("///    Parent Class : " + aClass.getParentClass());

        pw.println();
        pw.println(indent, "/// <summary>");
        pw.println(indent, "/// This allows for a quick display of PDU data.");
        pw.println(indent, "/// </summary>");
        if(useDotNet)
        {
            pw.println(indent, "[SuppressMessage(\"Microsoft.Design\", \"CA1031:DoNotCatchGeneralExceptionTypes\", Justification = \"Due to ignoring errors.\")]");
        }

        pw.println(indent, "public " + newKeyword + " string ToString()");
        pw.println(indent, "{");

        pw.println(indent + 1, "string outputString = \"\";");
        pw.println();

        // If we're a base class of another class, we should first call base
        // to make sure the base's ivars are reflected out.
        String baseclassName = aClass.getParentClass();
        if (!baseclassName.equalsIgnoreCase("root")) {
            pw.println(indent + 1, "outputString += base.ToString();");
        }

        ivars = aClass.getClassAttributes();
        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = ivars.get(idx);
            ClassAttributeType underlyingKind = anAttribute.getUnderlyingKind();

            pw.println();
            pw.println(anAttribute.ToString());

            String attrType = anAttribute.getType();

            if((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO16) ||
               (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO32) ||
               (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO64))
            {
                continue;
            }

            pw.print(indent + 1, "outputString += ");

            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST) ||
                (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST))
            {
                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
                    attrType = anAttribute.getName();

                pw.println(String.format("\"%s : \";", attrType));

                String listLength = (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST) ? "Count" : "Length";

                pw.println(indent + 1, "for (int idx = 0; idx < " + IVAR_PREFIX + anAttribute.getName() + "." + listLength + "; idx++)");
                pw.println(indent + 1, "{");
                pw.println(indent + 2, "outputString += " + IVAR_PREFIX + anAttribute.getName() + "[idx].ToString();");
                // pw.println(indent + 4, "this." + IVAR_PREFIX + anAttribute.getName() + "[idx].Marshal(dos);");
                pw.println(indent + 1, "}");
            }

            // this might work for all other types?
            else
            {
                String attrName = IVAR_PREFIX + anAttribute.getName();

                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
                    attrType = anAttribute.getName();

                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.STATIC_IVAR)
                {
                    attrName = anAttribute.getName();
                    attrType = attrName;
                }

                pw.println(String.format(
                        "\"%s : \" + %s.ToString() + System.Environment.NewLine;",
                        attrType,
                        attrName
                        ));
            }


        }

        pw.println(indent, "return outputString;");
        pw.println(indent, "}"); // end of ToString method
    }

    private void writeMarshalMethod(PrintStringBuffer pw, GeneratedClass aClass, int indent) {
        List<GeneratedClassAttribute> ivars;
        String baseclassName = aClass.getParentClass();
        String newKeyword = ""; //PES 032209 added to remove warning from C# compiler

        //PES 02/10/2009 Added to support auto setting of length field
        if (!baseclassName.equalsIgnoreCase("root")) {
            boolean exitLoop = false;
            boolean foundMatch = true;
            String matchValue = baseclassName;
            String key = "";

            if (!matchValue.equalsIgnoreCase("pdu")) {
                do {
                    key = "";
                    foundMatch = false;

                    if (classesInstantiated.containsKey(matchValue)) {
                        key = classesInstantiated.get(matchValue);
                    } else {
                        //No match to key, get out
                        break;
                    }

                    //There was a key test if the upper class is PDU.
                    //If so then can add new method to retrieve pdu length
                    if (key != null) {
                        matchValue = key;
                        foundMatch = true;

                        if (key.equalsIgnoreCase("pdu")) {
                            exitLoop = true;
                        }
                    }

                    //If match not found at this point then get out
                    if (foundMatch == false) {
                        exitLoop = true;
                    }

                } while (exitLoop == false);

            }

            if (foundMatch == true) {
                //System.out.println("Found PDU writing data");

                //PES 032209 added to remove warning from C# compiler
                if (!baseclassName.equalsIgnoreCase("pdu")) {
                    newKeyword = "new ";
                } else {
                    newKeyword = "";
                }

                pw.println(indent, "/// <summary>");
                pw.println(indent, "/// Automatically sets the length of the marshalled data, then calls the marshal method.");
                pw.println(indent, "/// </summary>");
                pw.println(indent, "/// <param name=\"dos\">The DataOutputStream instance to which the PDU is marshaled.</param>");
                pw.println(indent, "public " + newKeyword + "void MarshalAutoLengthSet(DataOutputStream dos)");
                pw.println(indent, "{");
                pw.println(indent + 1, "// Set the length prior to marshalling data");
                pw.println(indent + 1, "this.Length = (ushort)this.GetMarshaledSize();");
                pw.println(indent + 1, "this.Marshal(dos);");
                pw.println(indent, "}");
                pw.println();
            }

        }

        if (!baseclassName.equalsIgnoreCase("root")) {
            newKeyword = "new";
        } else {
            newKeyword = "";
        }

        pw.println(indent, "/// <summary>");
        pw.println(indent, "/// Marshal the data to the DataOutputStream.  Note: Length needs to be set before calling this method");
        pw.println(indent, "/// </summary>");
        pw.println(indent, "/// <param name=\"dos\">The DataOutputStream instance to which the PDU is marshaled.</param>");
        if(useDotNet)
        {
            pw.println(indent, "[SuppressMessage(\"Microsoft.Design\", \"CA1031:DoNotCatchGeneralExceptionTypes\", Justification = \"Due to ignoring errors.\")]");
        }
        pw.println(indent, "public " + newKeyword + " void Marshal(DataOutputStream dos)");
        pw.println(indent, "{");

        // If we're a base class of another class, we should first call base
        // to make sure the base's ivars are marshaled out.
        if (!baseclassName.equalsIgnoreCase("root")) {
            pw.println(indent + 1, "base.Marshal(dos);");
        }

        pw.println(indent + 1, "if (dos != null)");
        pw.println(indent + 1, "{");

        pw.println(indent + 2, "try");
        pw.println(indent + 2, "{");

        // Loop through the class attributes, generating the output for each.
        ivars = aClass.getClassAttributes();

        //This is a way to make sure that the variable used to store the count uses the .Length nomenclature.  There was no way
        //for me to determine if the OneByteChunk was used as it defaulted to a short data type.
        List<String> variableListfix = new ArrayList<>();
        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = ivars.get(idx);        
try
{
            if (anAttribute.getType().equalsIgnoreCase("OneByteChunk")) {
                variableListfix.add(anAttribute.getName());
            }
}
catch (Exception e)
{
    pw.println(indent, "");
    pw.println(indent, "/// FAILED to find type of attribute");
    pw.println(indent, "/// Attribute name : " + anAttribute.getName());
    pw.println(indent, "/// Attribute Type : " + anAttribute.getType());
}
        }


        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = ivars.get(idx);
            pw.println();
            pw.println("/// ATTRIBUTE :");
            pw.print(anAttribute.ToString());
            pw.println();

            // Some attributes can be marked as do-not-marshal
            if(anAttribute.shouldSerialize == false)
            {
                 pw.println("    // attribute " + anAttribute.getName() + " marked as not serialized");
                 continue;
            }

            // Write out a method call to serialize a primitive type
            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE) {
                String marshalType = marshalTypes.getProperty(anAttribute.getType());
                String marshalMethod = marshalMethods.getProperty(anAttribute.getType());
                String capped = this.camelCaseCapIgnoreSpaces(anAttribute.getType());
                pw.println("/// MarshalMethod : " + marshalMethod);
                pw.println("///     MarshalAs : " + marshalType);
                pw.println("///        Capped : " + capped);

                // If we're a normal primitivetype, marshal out directly; otherwise, marshall out
                // the list length.
                if (anAttribute.getIsDynamicListLengthField() == false) {                    
                    pw.println(indent + 3, "dos.Write" + marshalMethod + "((" + marshalType + ")this." + IVAR_PREFIX + anAttribute.getName() + ");");
                } else {
                    GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();

// pw.println();
// pw.println("    // LIST ATTRIBUTE :");
// pw.print("    " + listAttribute.ToString());
// pw.println();

                    //This was determined not to be working due to the fact that the OneByteChunk class is never referenced for the
                    //data length field.  See above for work around
                    //PES 04/29/2009  Added to speed up unboxing of data, using byte[] vice unboxing of a Class ie. OneByteChunk
                    //if (anAttribute.getType().equalsIgnoreCase("OneByteChunk"))
                    //{
                    //	pw.println("       dos.write" + capped + "((" + marshalType + ")_" + listAttribute.getName() + ".Length);");
                    //}
                    //else
                    //{
                    if (variableListfix.contains(listAttribute.getName()) == true) {
                        // pw.println("HERE");
                        pw.println(indent + 3, "dos.Write" + marshalMethod + "((" + marshalType + ")this." + IVAR_PREFIX + listAttribute.getName() + ".Length);");
                    } else {
                        // pw.println("THERE");
                        pw.println(indent + 3, "dos.Write" + marshalMethod + "((" + marshalType + ")this." + IVAR_PREFIX + listAttribute.getName() + ".Count);");
                    }
                    //}
                }

            }

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) {

                pw.println(indent + 3, String.format("%s_Accessors.Marshal(dos.DS, %s);",
                                         anAttribute.getType(),
                                         IVAR_PREFIX + anAttribute.getName()));
            }

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD) {
                pw.println(indent + 3, String.format("%s.Marshal(dos);",
                                         IVAR_PREFIX + anAttribute.getName()));
            }

            // Write out a method call to serialize a class.
            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF) {
                String marshalType = anAttribute.getType();

                pw.println(indent + 3, "this." + IVAR_PREFIX + anAttribute.getName() + ".Marshal(dos);");
            }

            // Write out the method call to marshal a fixed length list, aka an array.
            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)) {
                pw.println();
                pw.println(indent + 3, "for (int idx = 0; idx < this." + IVAR_PREFIX + anAttribute.getName() + ".Length; idx++)");
                pw.println(indent + 3, "{");

                ClassAttributeType underlyingKind = anAttribute.getUnderlyingKind();

                if (underlyingKind == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
                {
                    pw.println("Don't compile : We have a dis class reference in this list");
                }

                // This is some sleaze. We're an array, but an array of what? We could be either a
                // primitive or a class. We need to figure out which. This is done via the expedient
                // but not very reliable way of trying to do a lookup on the type. If we don't find
                // it in our map of primitives to marshal types, we assume it is a class.
                String marshalMethod = marshalMethods.getProperty(anAttribute.getType());

                //String attributeArrayModifier = "";
                //if (anAttribute.getUnderlyingTypeIsPrimitive() == true)
                //{
                //    attributeArrayModifier = "[]";
                //}

                if (anAttribute.getUnderlyingTypeIsPrimitive()) {
                    String capped = this.camelCaseCapIgnoreSpaces(anAttribute.getType());
                    pw.println(indent + 4, "dos.Write" + marshalMethod + "(this." + IVAR_PREFIX + anAttribute.getName() + "[idx]);");
                } else if (anAttribute.getUnderlyingTypeIsEnum()) {
                    pw.println(enumNamespace + "." + anAttribute.getType() + "_Accessors");
                } else {
                    pw.println(indent + 4, "this." + IVAR_PREFIX + anAttribute.getName() + "[idx].Marshal(dos);");
                }

                pw.println(indent + 3, "}"); // end of array marshaling
            }

            // Write out a section of code to marshal a variable length list. The code should look like
            //
            // for(int idx = 0; idx < attrName.size(); idx++)
            // { anAttribute.marshal(dos);
            // }
            //

            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)) {
                //PES 04/29/2009  Added to speed up unboxing of data, using byte[] vice unboxing of a Class ie. OneByteChunk
            
                pw.println();
                pw.println(anAttribute.ToString());
                if (anAttribute.getType().equalsIgnoreCase("OneByteChunk")) {
                    pw.println(indent + 3, "dos.WriteByte (this." + IVAR_PREFIX + anAttribute.getName() + ");");

                } else {
                    pw.println();
                    pw.println(indent + 3, "for (int idx = 0; idx < this." + IVAR_PREFIX + anAttribute.getName() + ".Count; idx++)");
                    pw.println(indent + 3, "{");

                    // This is some sleaze. We're an array, but an array of what? We could be either a
                    // primitive or a class. We need to figure out which. This is done via the expedient
                    // but not very reliable way of trying to do a lookup on the type. If we don't find
                    // it in our map of primitives to marshal types, we assume it is a class.

                    String marshalType = marshalTypes.getProperty(anAttribute.getType());
                    ClassAttributeType underlyingKind = anAttribute.getUnderlyingKind();

                    if (underlyingKind == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
                    {
                        pw.println(indent + 4, String.format("%s_Accessors.Marshal(dos, %s[idx]);",
                            anAttribute.getType(),
                            IVAR_PREFIX + anAttribute.getName()));
                    }
                    // else if (underlyingKind == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD)
                    // {
                    //     pw.println(indent + 4, "a" + initialCapital(anAttribute.getType()) + ".Marshal(dos.DS);");
                    // }
                    else if (anAttribute.getUnderlyingTypeIsPrimitive())
                    {
                        String capped = this.camelCaseCapIgnoreSpaces(anAttribute.getType());
                        pw.println(indent + 4, "dos.Write" + capped + "(this." + IVAR_PREFIX + anAttribute.getName() + ");");
                    }
                    else
                    {
                        pw.println(indent + 4, anAttribute.getType() + " a" + initialCapital(anAttribute.getType() + " = (" + anAttribute.getType() + ")this." + IVAR_PREFIX
                                + anAttribute.getName() + "[idx];"));
                        if (underlyingKind == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD)
                            pw.println(indent + 4, "a" + initialCapital(anAttribute.getType()) + ".Marshal(dos.DS);");
                        else
                            pw.println(indent + 4, "a" + initialCapital(anAttribute.getType()) + ".Marshal(dos);");
                    }

                    pw.println(indent + 3, "}");  // end of list marshalling
                }
            }
        } // End of loop through the ivars for a marshal method

        pw.println(indent + 2, "}"); // end try
        pw.println(indent + 2, "catch (Exception e)");
        pw.println(indent + 2, "{");
        pw.println(0, "#if DEBUG");
        pw.println(indent + 3, "Trace.WriteLine(e);");
        pw.println(indent + 3, "Trace.Flush();");
        pw.println(0, "#endif");
        pw.println(indent + 3, "this.OnException(e);");
        pw.println(indent + 2, "}");
        pw.println(indent + 1, "}");
        pw.println(indent, "}"); // end of marshal method
    }

    private void writeUnmarshallMethod(PrintStringBuffer pw, GeneratedClass aClass, int indent) {
        List<GeneratedClassAttribute> ivars;
        String baseclassName;

        String newKeyword = ""; //PES 032209 added to remove warning from C# compiler

        String marshalType = null;
        String marshalMethod = null;
        ClassAttributeType underlyingKind = null;

        //PES 032209 added to remove warning from C# compiler
        if (!aClass.getParentClass().equalsIgnoreCase("root")) {
            newKeyword = "new ";
        } else {
            newKeyword = " ";
        }

        pw.println();
        if(useDotNet)
        {
            pw.println(indent, "[SuppressMessage(\"Microsoft.Design\", \"CA1031:DoNotCatchGeneralExceptionTypes\", Justification = \"Due to ignoring errors.\")]");
        }
        pw.println(indent, "public " + newKeyword + "void Unmarshal(DataInputStream dataInputStream)");
        pw.println(indent, "{");

        baseclassName = aClass.getParentClass();
        if (!baseclassName.equalsIgnoreCase("root")) {
            pw.println(indent + 1, "base.Unmarshal(dataInputStream);\n");
        }

        pw.println(indent + 1, "if (dataInputStream != null)");
        pw.println(indent + 1, "{");

        pw.println(indent + 2, "try");
        pw.println(indent + 2, "{");

        // Loop through the class attributes, generating the output for each.

        ivars = aClass.getClassAttributes();
        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = ivars.get(idx);

            pw.println();
            pw.println(anAttribute.ToString());

            // Some attributes can be marked as do-not-marshal
            if(anAttribute.shouldSerialize == false)
            {
                 pw.println("    // attribute " + anAttribute.getName() + " marked as not serialized");
                 continue;
            }

            underlyingKind = anAttribute.getUnderlyingKind();

            if (anAttribute.getType() != null)
            {
                marshalType = marshalTypes.getProperty(anAttribute.getType());
                marshalMethod = marshalMethods.getProperty(anAttribute.getType());
            }

            // Write out a method call to deserialize a primitive type
            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE) {
                String capped = this.camelCaseCapIgnoreSpaces(marshalType);

                // pw.println("/// MarshalMethod : " + marshalMethod);
                // pw.println("///     MarshalAs : " + marshalType);
                // pw.println("///        Capped : " + capped);

                if (marshalType == null)
                {
                   pw.println(indent + 3, "// FAiled to find unmarshaltype type : " + anAttribute.getType());
                }
                else
                {
                    pw.println(indent + 3, "this." + IVAR_PREFIX + anAttribute.getName() + " = (" + marshalType + ")dataInputStream.Read" + marshalMethod + "();");
                }
            }

            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) {
                pw.println(indent + 3, String.format("%s_Accessors.Unmarshal(dataInputStream.DS, ref %s);",
                                         anAttribute.getType(),
                                         IVAR_PREFIX + anAttribute.getName()));
            }

            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD) {
                pw.println(indent + 3, String.format("%s.Unmarshal(dataInputStream);",
                                         IVAR_PREFIX + anAttribute.getName()));
            }

            // Write out a method call to deserialize a class.
            else if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF) {
                pw.println(indent + 3, "this." + IVAR_PREFIX + anAttribute.getName() + ".Unmarshal(dataInputStream);");
            }

            // Write out the method call to unmarshal a fixed length list, aka an array.
            else if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)) {
                pw.println(indent + 3, "for (int idx = 0; idx < this." +IVAR_PREFIX + anAttribute.getName() + ".Length; idx++)");
                pw.println(indent + 3, "{");

                // This is some sleaze. We're an array, but an array of what? We could be either a
                // primitive or a class. We need to figure out which. This is done via the expedient
                // but not very reliable way of trying to do a lookup on the type. If we don't find
                // it in our map of primitives to marshal types, we assume it is a class.

                if (marshalType == null) // It's a class
                {
                    pw.println(indent + 4, "this." + IVAR_PREFIX + anAttribute.getName() + "[idx].Unmarshal(dataInputStream);");
                } 
                else // It's a primitive
                {
                    String capped = this.camelCaseCapIgnoreSpaces(anAttribute.getType());
                    pw.println(indent + 4, "this." + IVAR_PREFIX + anAttribute.getName() + "[idx] = dataInputStream.Read" + marshalMethod + "();");
                }

                pw.println(indent + 3, "}"); // end of array unmarshaling
            } // end of array unmarshalling

            // Unmarshall a variable length array.

            else if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)) {

                //PES 04/29/2009  Added to speed up unboxing of data, using byte[] vice unboxing of a Class ie. OneByteChunk
                if (anAttribute.getType().equalsIgnoreCase("OneByteChunk")) {
                    pw.println(indent + 3, "this._" + anAttribute.getName() + " = dataInputStream.ReadByteArray" + "(this." + IVAR_PREFIX + anAttribute.getCountFieldName() + ");");
                } else {
                    pw.println(indent + 3, "for (int idx = 0; idx < this." + this.initialCapital(anAttribute.getCountFieldName()) + "; idx++)");
                    pw.println(indent + 3, "{");

                    if (marshalType == null) // It's a class
                    {
                        pw.println(indent + 4, anAttribute.getType() + " anX = new " + anAttribute.getType() + "();");
                        
                        // if (underlyingKind == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD)
                        //     pw.println(indent + 4, "anX.Unmarshal(dataInputStream.DS);");
                        // else
                        if (underlyingKind == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
                            pw.println(indent + 4, anAttribute.getType() + "_Accessors.Unmarshal(dataInputStream, ref anX);");
                        else
                            pw.println(indent + 4, "anX.Unmarshal(dataInputStream);");
                        pw.println(indent + 4, "this." + IVAR_PREFIX + anAttribute.getName() + ".Add(anX);");
                    } else // It's a primitive
                    {
                        String capped = this.camelCaseCapIgnoreSpaces(anAttribute.getType());
                        pw.println(indent + 4, "dataInputStream.Read" + marshalMethod + "(this." + IVAR_PREFIX + anAttribute.getName() + ");");
                    }
                    pw.println(indent + 3, "};");
                    pw.println();
                }
            } // end of unmarshalling a variable list

        } // End of loop through ivars for writing the unmarshal method

        pw.println(indent + 2, "}"); // end try
        pw.println(indent + 2, "catch (Exception e)");
        pw.println(indent + 2, "{");
        pw.println(0, "#if DEBUG");
        pw.println(indent + 3, "Trace.WriteLine(e);");
        pw.println(indent + 3, "Trace.Flush();");
        pw.println(0, "#endif");
        pw.println(indent + 3, "this.OnException(e);");
        pw.println(indent + 2, "}");
        pw.println(indent + 1, "}");
        pw.println(indent, "}"); // end of unmarshal method

    }

    //Generate listing of all parameters using psuedo reflection.  This method needs to be further refined as it is only useful for
    //printing out all the data, the format used is not nice.  This method however will display faster than using the XML reflection method provided.
    //Only used for debugging purposes until a better method could be developed.
    private void writeReflectionMethod(PrintStringBuffer pw, GeneratedClass aClass, int indent) {
        List<GeneratedClassAttribute> ivars;
        String tab = "\\t ";

        String newKeyword = ""; //PES 032209 added to remove warning from C# compiler

        //PES 032209 added to remove warning from C# compiler
        if (!aClass.getParentClass().equalsIgnoreCase("root")) {
            newKeyword = "new ";
        } else {
            newKeyword = "";
        }


        pw.println();
        pw.println(indent, "/// <summary>");
        pw.println(indent, "/// This allows for a quick display of PDU data.  The current format is unacceptable and only used for debugging.");
        pw.println(indent, "/// This will be modified in the future to provide a better display.  Usage: ");
        pw.println(indent, "/// pdu.GetType().InvokeMember(\"Reflection\", System.Reflection.BindingFlags.InvokeMethod, null, pdu, new object[] { sb });");
        pw.println(indent, "/// where pdu is an object representing a single pdu and sb is a StringBuilder.");
        pw.println(indent, "/// Note: The supplied Utilities folder contains a method called 'DecodePDU' in the PDUProcessor Class that provides this functionality");
        pw.println(indent, "/// </summary>");
        pw.println(indent, "/// <param name=\"sb\">The StringBuilder instance to which the PDU is written to.</param>");
        if(useDotNet)
        {
            pw.println(indent, "[SuppressMessage(\"Microsoft.Design\", \"CA1031:DoNotCatchGeneralExceptionTypes\", Justification = \"Due to ignoring errors.\")]");
        }
        pw.println(indent, "public " + newKeyword + "void Reflection(StringBuilder sb)");
        pw.println(indent, "{");
        pw.println(indent + 1, "sb.AppendLine(\"<" + aClass.getName() + ">\");");

        // If we're a base class of another class, we should first call base
        // to make sure the base's ivars are reflected out.
        String baseclassName = aClass.getParentClass();
        if (!baseclassName.equalsIgnoreCase("root")) {
            pw.println(indent + 1, "base.Reflection(sb);");
        }

        pw.println(indent + 1, "try");
        pw.println(indent + 1, "{");
        // Loop through the class attributes, generating the output for each.

        ivars = aClass.getClassAttributes();

        //This is a way to make sure that the variable used to store the count uses the .Length nomenclature.  There was no way
        //for me to determine if the OneByteChunk was used as it defaulted to a short data type.
        List<String> variableListfix = new ArrayList<>();
        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = ivars.get(idx);

try {
            if (anAttribute.getType().equalsIgnoreCase("OneByteChunk")) {
                variableListfix.add(anAttribute.getName());
            }
}
catch (Exception e) {
int old_indent=indent;
indent=0;
    pw.println("");
    pw.println("/// DEBUG");
    pw.println("/// Attribute name         : " + anAttribute.getName());
    pw.println("/// Attribute Type         : " + anAttribute.getType());
indent = old_indent;
}
        }

        for (int idx = 0; idx < ivars.size(); idx++) {
            GeneratedClassAttribute anAttribute = ivars.get(idx);
            ClassAttributeType underlyingKind = anAttribute.getUnderlyingKind();

            // pw.println();
            // pw.println(anAttribute.ToString());
            // Write out a method call to reflect a primitive type
            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE) {
                String marshalType = marshalTypes.getProperty(anAttribute.getType());
                //String capped = this.initialCapital(marshalType);

                // If we're a normal primitivetype, marshal out directly; otherwise, marshall out
                // the list length.
                if (anAttribute.getIsDynamicListLengthField() == false) {
                    //pw.println("           sb.Append(\"" + marshalType + tab + "_" + anAttribute.getName() + tab + "\" + _" + anAttribute.getName() + ".ToString() + System.Environment.NewLine);");
                    pw.println(indent + 2, "sb.AppendLine(\"<" + anAttribute.getName() + " type=\\\"" + marshalType + "\\\">\" + this." + IVAR_PREFIX + anAttribute.getName() + ".ToString(CultureInfo.InvariantCulture) + \"</" + anAttribute.getName() + ">\");");

                } else {
                    GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();

                    //PES 04/29/2009  Added to speed up unboxing of data, using byte[] vice unboxing of a Class ie. OneByteChunk
                    if (variableListfix.contains(listAttribute.getName()) == true) {
                        pw.println(indent + 2, "sb.AppendLine(\"<" + listAttribute.getName() + " type=\\\"" + marshalType + "\\\">\" + this." + IVAR_PREFIX + listAttribute.getName() + ".Length.ToString(CultureInfo.InvariantCulture) + \"</" + listAttribute.getName() + ">\");");
                    } else {
                        pw.println(indent + 2, "sb.AppendLine(\"<" + listAttribute.getName() + " type=\\\"" + marshalType + "\\\">\" + this." + IVAR_PREFIX + listAttribute.getName() + ".Count.ToString(CultureInfo.InvariantCulture) + \"</" + listAttribute.getName() + ">\");");
                    }
                }
            }

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
            {
                // String marshalType = marshalTypes.getProperty(anAttribute.getType());
                String marshalType = "UInt" + anAttribute.getEnumMarshalSize();
                // pw.println("///     MarshalAs : " + marshalType);

                String toStringMethod = String.format("%s.%s_Accessors.ToString(%s)", enumNamespace, anAttribute.getType(), IVAR_PREFIX + anAttribute.getName());
                pw.println(indent + 2, "sb.AppendLine(\"<" + anAttribute.getName() + " type=\\\"" + marshalType + "\\\">\" + " + toStringMethod + " " + " + \"</" + IVAR_PREFIX + anAttribute.getName() + ">\");");
            }

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD)
            {

            }

            // Write out a method call to reflect another class.
            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF) {
                //String marshalType = anAttribute.getType();
                pw.println(indent + 2, "sb.AppendLine(\"<" + anAttribute.getName() + ">\");");
                pw.println(indent + 2, "this." + IVAR_PREFIX + anAttribute.getName() + ".Reflection(sb);");
                pw.println(indent + 2, "sb.AppendLine(\"</" + anAttribute.getName() + ">\");");
            }

            // Write out the method call to marshal a fixed length list, aka an array.
            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)) {
                //pw.println("    sb.Append(\"</" + anAttribute.getName() + ">\"  + System.Environment.NewLine);");

                pw.println(indent + 2, "for (int idx = 0; idx < this." + IVAR_PREFIX + anAttribute.getName() + ".Length; idx++)");
                pw.println(indent + 2, "{");

                // This is some sleaze. We're an array, but an array of what? We could be either a
                // primitive or a class. We need to figure out which. This is done via the expedient
                // but not very reliable way of trying to do a lookup on the type. If we don't find
                // it in our map of primitives to marshal types, we assume it is a class.

                String marshalType = marshalTypes.getProperty(anAttribute.getType());

                if (anAttribute.getUnderlyingTypeIsPrimitive()) {
                    //String capped = this.initialCapital(marshalType);
                    pw.println(indent + 3, "sb.AppendLine(\"<" + anAttribute.getName() + "\" + idx.ToString(CultureInfo.InvariantCulture) + \" type=\\\"" + marshalType + "\\\">\" + this." + IVAR_PREFIX + anAttribute.getName() + "[idx] + \"</" + anAttribute.getName() + "\" + idx.ToString(CultureInfo.InvariantCulture) + \">\");");
                } else {
                    pw.println(indent + 3, "sb.AppendLine(\"<" + anAttribute.getName() + "\" + idx.ToString(CultureInfo.InvariantCulture) + \" type=\\\"" + anAttribute.getType() + "\\\">\" + this." + IVAR_PREFIX + anAttribute.getName() + "[ \" + idx.ToString(CultureInfo.InvariantCulture) + \"]);");
                    pw.println(indent + 3, "this." + IVAR_PREFIX + anAttribute.getName() + "[idx].Reflection(sb);");
                    pw.println(indent + 3, "sb.AppendLine(\"</" + anAttribute.getName() + "\" + idx.ToString(CultureInfo.InvariantCulture) + \">\");");
                }

                pw.println(indent + 2, "}"); // end of array reflection
                pw.println();
            }

            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)) {
                //This will fix the OneByteChunk problem where the arrays length is correctly set to Length vice Count.  This is needed as
                //there was no way to determine if the underlining data referenced the OneByteChunk class
                //PES 04/29/2009  Added to speed up unboxing of data, using byte[] vice unboxing of a Class ie. OneByteChunk
                if (variableListfix.contains(anAttribute.getName()) == true) {
                    pw.println(indent + 2, "sb.AppendLine(\"<" + anAttribute.getName() + " type=\\\"byte[]\\\">\");");
                    pw.println(indent + 2, "foreach (byte b in this." + IVAR_PREFIX + anAttribute.getName() + ")");
                    pw.println(indent + 2, "{");
                    pw.println(indent + 3, "sb.Append(b.ToString(\"X2\", CultureInfo.InvariantCulture));");
                    pw.println(indent + 2, "}");
                    pw.println();
                    pw.println(indent + 2, "sb.AppendLine(\"</" + anAttribute.getName() + ">\");");
                    pw.println();

                } else {

                    pw.println(indent + 2, "for (int idx = 0; idx < this." + IVAR_PREFIX + anAttribute.getName() + ".Count; idx++)");
                    pw.println(indent + 2, "{");

                    // This is some sleaze. We're an array, but an array of what? We could be either a
                    // primitive or a class. We need to figure out which. This is done via the expedient
                    // but not very reliable way of trying to do a lookup on the type. If we don't find
                    // it in our map of primitives to marshal types, we assume it is a class.

                    String marshalType = marshalTypes.getProperty(anAttribute.getType());

                    if (anAttribute.getUnderlyingTypeIsPrimitive()) {
                        //String capped = this.initialCapital(marshalType);
                        pw.println(indent + 3, "sb.AppendLine(\"<" + anAttribute.getName() + "\" + idx.ToString(CultureInfo.InvariantCulture) + \" type=\\\"" + anAttribute.getType() + "\\\">\" + this." + IVAR_PREFIX + anAttribute.getName() + "[idx].ToString(CultureInfo.InvariantCulture));");
                        pw.println(indent + 3, "sb.AppendLine(\"</" + anAttribute.getName() + "\" + idx.ToString(CultureInfo.InvariantCulture) + \">\");");

                        //pw.println("           sb.Append(\"" + marshalType + tab + "\" + _" + anAttribute.getName() + "  + System.Environment.NewLine);");
                    } 
                    else
                    {
                        pw.println(indent + 3, "sb.AppendLine(\"<" + anAttribute.getName() + "\" + idx.ToString(CultureInfo.InvariantCulture) + \" type=\\\"" + anAttribute.getType() + "\\\">\");");
                        pw.println(indent + 3, anAttribute.getType() + " a" + initialCapital(anAttribute.getType() + " = (" + anAttribute.getType() + ")this." + IVAR_PREFIX + anAttribute.getName() + "[idx];"));
                        if (underlyingKind == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
                        {
                            String xrefName = "Uint32";
                            String enumName = anAttribute.getType();
                            pw.print(String.format("%ssb.AppendLine(\"<%s type=\\\"%s\\\">\" + %s.ToString() + \"</%s>\");\n", 
                                        StringUtils.tabs(5), enumName, xrefName, "a" + enumName, anAttribute.getName()
                                        ));
                        }
                        else
                            pw.println(indent + 3, "a" + initialCapital(anAttribute.getType()) + ".Reflection(sb);");

                        pw.println(indent + 3, "sb.AppendLine(\"</" + anAttribute.getName() + "\" + idx.ToString(CultureInfo.InvariantCulture) + \">\");");
                    }
                    pw.println(indent + 2, "}"); // end of list marshalling
                    pw.println();
                }
            }
        } // End of loop through the ivars for a marshal method

        pw.println(indent + 2, "sb.AppendLine(\"</" + aClass.getName() + ">\");");

        pw.println(indent + 1, "}"); // end try
        pw.println(indent + 1, "catch (Exception e)");
        pw.println(indent + 1, "{");
        pw.println(0, "#if DEBUG");
        pw.println(indent + 3, "Trace.WriteLine(e);");
        pw.println(indent + 3, "Trace.Flush();");
        pw.println(0, "#endif");
        pw.println(indent + 3, "this.OnException(e);");
        pw.println(indent + 1, "}");
        pw.println(indent, "}"); // end of reflection method
    }

    private void writeEqualityMethod(PrintStringBuffer pw, GeneratedClass aClass, int indent)
    {

        String newKeyword = "";
        pw.println("///    Parent Class : " + aClass.getParentClass());
        if (aClass.getParentClass().equalsIgnoreCase("root")) {
            newKeyword = "override";
        } else {
            newKeyword = "new";
        }

        try
        {
            pw.println();
            pw.println(indent, "/// <summary>");
            pw.println(indent, "/// Determines whether the specified <see cref=\"System.Object\"/> is equal to this instance.");
            pw.println(indent, "/// </summary>");
            pw.println(indent, "/// <param name=\"obj\">The <see cref=\"System.Object\"/> to compare with this instance.</param>");
            pw.println(indent, "/// <returns>");
            pw.println(indent, "/// 	<c>true</c> if the specified <see cref=\"System.Object\"/> is equal to this instance; otherwise, <c>false</c>.");
            pw.println(indent, "/// </returns>");

            pw.println(indent, "public override bool Equals(object? obj) => Equals(obj as " + aClass.getName() + ");");

            pw.println();
            pw.println(indent, "/// <summary>");
            pw.println(indent, "/// Compares for reference AND value equality.");
            pw.println(indent, "/// </summary>");
            pw.println(indent, "/// <param name=\"obj\">The object to compare with this instance.</param>");
            pw.println(indent, "/// <returns>");
            pw.println(indent, "/// 	<c>true</c> if both operands are equal; otherwise, <c>false</c>.");
            pw.println(indent, "/// </returns>");
            pw.println(indent, "public bool Equals(" + aClass.getName() + "? obj)");
            pw.println(indent, "{");
            
            pw.println();
            pw.println(indent +1, "if (obj == null) return false;");
            pw.println();
            pw.println(indent + 1, "if (obj.GetType() != this.GetType())");
            pw.println(indent + 1, "{");
            pw.println(indent + 2, "return false;");
            pw.println(indent + 1, "}");
                     
            pw.println();

            //If the class is PDU then do not use the base.Equals as it defaults to the base API version which will return a false
            String parentClass = aClass.getParentClass();

            if (!parentClass.equalsIgnoreCase("root"))
            {
                pw.println(indent + 1, "bool ivarsEqual = base.Equals(obj);");
                pw.println();
            }
            else
            {
                pw.println(indent + 1, "bool ivarsEqual = true;");
            }

            for (int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
            {
                GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);

                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
                {
                    pw.println(indent + 1, "if (this." + IVAR_PREFIX + anAttribute.getName() + " != obj." + IVAR_PREFIX + anAttribute.getName() + ")");
                    pw.println(indent + 1, "{");
                    pw.println(indent + 2, "ivarsEqual = false;");
                    pw.println(indent + 1, "}");
                    pw.println();
                }

                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
                {
                    pw.println(indent + 1, "if (!this." + IVAR_PREFIX + anAttribute.getName() + ".Equals(obj." + IVAR_PREFIX + anAttribute.getName() + "))");
                    pw.println(indent + 1, "{");
                    pw.println(indent + 2, "ivarsEqual = false;");
                    pw.println(indent + 1, "}");
                    pw.println();
                }

                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
                {
                    //PES 12082009 Added to account for issue with comparison of fields that are not marshalled.  Such as when creating two identical PDUs then
                    //comparing them.  The _numberofxxx variables will contain 0 as they only get filled when marshalling.

                    //pw.println(indent + 1, "if( ! (_" + anAttribute.getListLength() + " == rhs._" + anAttribute.getName() + ")) { ivarsEqual = false; }");
                    pw.println(indent + 1, "if (obj." + IVAR_PREFIX + anAttribute.getName() + ".Length != " + anAttribute.getListLength() + ") ");
                    pw.println(indent + 1, "{");
                    pw.println(indent + 2, "ivarsEqual = false;");
                    pw.println(indent + 1, "}");
                    pw.println();

                    //If ivars is false then do not iterate through loop
                    pw.println(indent + 1, "if (ivarsEqual)");
                    pw.println(indent + 1, "{");
                    pw.println(indent + 2, "for (int idx = 0; idx < " + anAttribute.getListLength() + "; idx++)");
                    pw.println(indent + 2, "{");
                    pw.println(indent + 3, "if (this." + IVAR_PREFIX + anAttribute.getName() + "[idx] != obj." + IVAR_PREFIX + anAttribute.getName() + "[idx])");
                    pw.println(indent + 3, "{");
                    pw.println(indent + 4, "ivarsEqual = false;");
                    pw.println(indent + 3, "}");
                    pw.println(indent + 2, "}");
                    pw.println(indent + 1, "}");
                    pw.println();
                }

                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
                {
                    //PES 04/29/2009  Added to speed up unboxing of data, using byte[] vice unboxing of a Class ie. OneByteChunk
                    if (anAttribute.getType().equalsIgnoreCase("OneByteChunk"))
                    {
                        pw.println(indent + 1, "if (!this." + IVAR_PREFIX + anAttribute.getName() + ".Equals(obj." + IVAR_PREFIX + anAttribute.getName() + "))");
                        pw.println(indent + 1, "{");
                        pw.println(indent + 2, "ivarsEqual = false;");
                        pw.println(indent + 1, "}");
                        pw.println();
                    }
                    else
                    {
                        pw.println(indent + 1, "if (this." + IVAR_PREFIX + anAttribute.getName() + ".Count != obj." + IVAR_PREFIX + anAttribute.getName() + ".Count)");
                        pw.println(indent + 1, "{");
                        pw.println(indent + 2, "ivarsEqual = false;");
                        pw.println(indent + 1, "}");
                        pw.println();

                        //If ivars is false then do not iterate through loop
                        pw.println(indent + 1, "if (ivarsEqual)");
                        pw.println(indent + 1, "{");
                        pw.println(indent + 2, "for (int idx = 0; idx < this." + IVAR_PREFIX + anAttribute.getName() + ".Count; idx++)");
                        pw.println(indent + 2, "{");
                        //PES 12102009 Do not believe this line is needed so commented out
                        //pw.println(indent + 3, anAttribute.getType() + " x = (" + anAttribute.getType() + ")_" + anAttribute.getName() + "[idx];");
                        pw.println(indent + 3, "if (!this." + IVAR_PREFIX + anAttribute.getName() + "[idx].Equals(obj." + IVAR_PREFIX + anAttribute.getName() + "[idx]))");
                        pw.println(indent + 3, "{");
                        pw.println(indent + 4, "ivarsEqual = false;");
                        pw.println(indent + 3, "}");
                        pw.println(indent + 2, "}");
                        pw.println(indent + 1, "}");
                        pw.println();
                    }
                }
            }

            pw.println(indent + 1, "return ivarsEqual;");
            pw.println(indent, "}");
            pw.println();

            pw.println(indent, "/// <summary>");
            pw.println(indent, "/// HashCode Helper");
            pw.println(indent, "/// </summary>");
            pw.println(indent, "/// <param name=\"hash\">The hash value.</param>");
            pw.println(indent, "/// <returns>The new hash value.</returns>");
            pw.println(indent, "private static int GenerateHash(int hash)");
            pw.println(indent, "{");
            pw.println(indent + 1, "hash <<= (5 + hash);");
            pw.println(indent + 1, "return hash;");
            pw.println(indent, "}");
            pw.println();

            pw.println(indent, "/// <summary>");
            pw.println(indent, "/// Gets the hash code.");
            pw.println(indent, "/// </summary>");
            pw.println(indent, "/// <returns>The hash code.</returns>");
            pw.println(indent, "public override int GetHashCode()");
            pw.println(indent, "{");
            pw.println(indent + 1, "int result = 0;");
            pw.println();

            //PES 12102009 needed to ensure that the base GetHashCode was not executed on a root class otherwise it returns random results
            if (!parentClass.equalsIgnoreCase("root")) {
                pw.println(indent + 1, "result = GenerateHash(result) ^ base.GetHashCode();");
            }

            for (int idx = 0; idx < aClass.getClassAttributes().size(); idx++) {
                GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
                
                // pw.print(anAttribute.ToString());
                // pw.println();

                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST ||
                    anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST ||
                    (!parentClass.equalsIgnoreCase("root") && idx == 0))
                {
                    pw.println();
                }

                if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE) ||
                    (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM))
                {
                    pw.println(indent + 1, "result = GenerateHash(result) ^ this." + IVAR_PREFIX + anAttribute.getName() + ".GetHashCode();");
                }

                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
                {
                    pw.println(indent + 1, "result = GenerateHash(result) ^ this." + IVAR_PREFIX + anAttribute.getName() + ".GetHashCode();");
                }

                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
                {
                    //pw.println(indent + 1, "if (" + anAttribute.getListLength() + " > 0)");
                    //pw.println(indent + 1, "{");
                    pw.println(indent + 1, "for (int idx = 0; idx < " + anAttribute.getListLength() + "; idx++)");
                    pw.println(indent + 1, "{");
                    pw.println(indent + 2, "result = GenerateHash(result) ^ this." + IVAR_PREFIX + anAttribute.getName() + "[idx].GetHashCode();");
                    pw.println(indent + 1, "}");
                    //pw.println(indent + 1, "}");
                }

                if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
                {
                    if (anAttribute.getType().equalsIgnoreCase("OneByteChunk"))
                    {
                        //PES need to modify as onebytechunks are represented as byte[] therefore need to change code slightly
                        pw.println(indent + 1, "if (this." + IVAR_PREFIX + anAttribute.getName() + ".Length > 0)");
                        pw.println(indent + 1, "{");
                        pw.println(indent + 2, "for (int idx = 0; idx < this." + IVAR_PREFIX + anAttribute.getName() + ".Length; idx++)");
                        pw.println(indent + 2, "{");
                        pw.println(indent + 3, "result = GenerateHash(result) ^ this." + IVAR_PREFIX + anAttribute.getName() + "[idx].GetHashCode();");
                        pw.println(indent + 2, "}");
                        pw.println(indent + 1, "}");
                    } 
                    else
                    {
                        pw.println(indent + 1, "if (this." + IVAR_PREFIX + anAttribute.getName() + ".Count > 0)");
                        pw.println(indent + 1, "{");
                        pw.println(indent + 2, "for (int idx = 0; idx < this." + IVAR_PREFIX + anAttribute.getName() + ".Count; idx++)");
                        pw.println(indent + 2, "{");
                        //PES 12102009 Do not believe this line is needed so commented out
                        //pw.println(indent + 3, anAttribute.getType() + " x = (" + anAttribute.getType() + ")_" + anAttribute.getName() + "[idx];");
                        pw.println(indent + 3, "result = GenerateHash(result) ^ this." + IVAR_PREFIX + anAttribute.getName() + "[idx].GetHashCode();");
                        pw.println(indent + 2, "}");
                        pw.println(indent + 1, "}");
                    }
                }
            }

            pw.println();
            pw.println(indent + 1, "return result;");
            pw.println(indent, "}");
        } 
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    /**
     * returns a string with the first letter capitalized.
     */
    @Override
    public String initialCapital(String aString) {
        StringBuffer stb = new StringBuffer(aString);
        stb.setCharAt(0, Character.toUpperCase(aString.charAt(0)));

        return new String(stb);
    }

    /** String utility
     * @param aString input
     * @return modified string */
    public String camelCaseCapIgnoreSpaces(String aString) {
        StringBuffer stb = new StringBuffer();

        if (aString.length() > 0){
            stb.append(Character.toUpperCase(aString.charAt(0)));

            boolean previousIsSpace = false;
            for (int i = 1; i < aString.length(); i++)
            {
                boolean currentIsSpace = aString.charAt(i) == ' ';

                if (previousIsSpace)
                {
                    stb.append(Character.toUpperCase(aString.charAt(i)));
                }
                else if (!currentIsSpace)
                {
                    stb.append(aString.charAt(i));
                }

                previousIsSpace = currentIsSpace;
            }
        }

        String newString = new String(stb);
        //System.out.println(newString);

        return newString;
    }

    /**
     * postprocess data as needed
     * @param printStringBuffer output
     * @param aClass GeneratedClass of interest
     */
    public void postProcessData(PrintStringBuffer printStringBuffer, GeneratedClass aClass) {
        //aClass.getName()

        if (aClass.getName().equalsIgnoreCase("VariableDatum")) {

            postProcessVariableDatum(printStringBuffer);
        }

        if (aClass.getName().equalsIgnoreCase("SignalPdu")) {
            postProcessSignalPdu(printStringBuffer);
        }
    }

    /**
     * postprocess SignalPdu as needed
     * @param printStringBuffer output
     */
    public void postProcessSignalPdu(PrintStringBuffer printStringBuffer) {
        int startfind, endfind;
        String findString;
        String newString;
        if (disVersion != null && this.disVersion.equals("1998"))
        {
            findString = "this._data = dis.ReadByteArray(this._dataLength);";
            newString = "this._data = dis.ReadByteArray((this._dataLength / 8) + (this._dataLength % 8 > 0 ? 1 : 0));  //09062009 Post processed. Needed to convert from bits to bytes";  //PES changed to reflex that the datalength should hold bits

            startfind = printStringBuffer.sb.indexOf(findString);
            printStringBuffer.sb.replace(startfind, startfind + findString.length(), newString);

            findString = "dos.WriteShort((short)this._data.Length);";
            newString = "dos.WriteShort((short)((this._dataLength == 0 && this._data.Length > 0) ? this._data.Length * 8 : this._dataLength)); //09062009 Post processed.  If value is zero then default to every byte will use all 8 bits";  //09062009 PES changed to reflex that the datalength should be set by user and not automatically as this value is the number of bits in the data field that should be used

            startfind = printStringBuffer.sb.indexOf(findString);
            printStringBuffer.sb.replace(startfind, startfind + findString.length(), newString);

            findString = "/// Note that setting this value will not change the marshalled value. The list whose length this describes is used for that purpose.";
            newString  = "/// This value must be set to the number of bits that will be used from the Data field.  Normally this value would be in increments of 8.  If this is the case then multiply the number of bytes used in the Data field by 8 and store that number here.";

            startfind = printStringBuffer.sb.indexOf(findString);
            printStringBuffer.sb.replace(startfind, startfind + 326, newString);

//          ///Do this twice as there are two occurences
//          startfind = pw.sb.indexOf(findString);
//          pw.sb.replace(startfind, startfind + 326, newString);
        }
    }

    /**
     * postprocess SignalPdu as needed
     * @param printStringBuffer output
     */
    public void postProcessVariableDatum(PrintStringBuffer printStringBuffer) {
        ///String findString1 = "/// Note that setting this value will not change the marshalled value. The list whose length this describes is used for that purpose.
/// The getvariableDatumLength method will also be based on the actual list length rather than this value.
/// The method is simply here for completeness and should not be used for any computations.

        int startfind, endfind;
        String findString;
        String newString;

        if ("1998".equals(this.disVersion))
        {
            //for (int i = 0; i < 2; i++) {
                startfind = printStringBuffer.sb.indexOf("Note that");
                endfind   = printStringBuffer.sb.indexOf("for any computations.");
                printStringBuffer.sb.replace(startfind, endfind + 21, "This value must be set for any PDU using it to work!" + PrintStringBuffer.newline + "/// This value should be the number of bits used.");
            //}

            startfind = printStringBuffer.sb.indexOf("dos.WriteUnsignedInt((uint)this._variableDatums.Count);");
            printStringBuffer.sb.replace(startfind, startfind + 43, "dos.WriteUnsignedInt((uint)this._variableDatumLength); //Post processed");

            findString = "_variableDatumLength = dis.ReadUnsignedInt();";
            newString = PrintStringBuffer.newline + "        int variableCount = (int)(this._variableDatumLength / 64) + (this._variableDatumLength % 64 > 0 ? 1 : 0);  //Post processed";
            startfind = printStringBuffer.sb.indexOf(findString);
            printStringBuffer.sb.insert(startfind + findString.length() + 1, newString);

            findString = "for (int idx = 0; idx < this.VariableDatumLength; idx++)";
            newString = "for (int idx = 0; idx < variableCount; idx++)";
            startfind = printStringBuffer.sb.indexOf(findString);
            printStringBuffer.sb.replace(startfind, startfind + findString.length(), newString);
        }
    }

    private void writeCopyrightNotice(PrintStringBuffer printStringBuffer) {
        printStringBuffer.println("//");
        printStringBuffer.println("// Copyright (c) 2008, MOVES Institute, Naval Postgraduate School. All ");
        printStringBuffer.println("// rights reserved. This work is licensed under the BSD open source license,");
        printStringBuffer.println("// available at https://www.movesinstitute.org/licenses/bsd.html");
        printStringBuffer.println("//");
        printStringBuffer.println("// Author: DMcG");
        printStringBuffer.println("// Modified for use with C#:");
        printStringBuffer.println("//  - Peter Smith (Naval Air Warfare Center - Training Systems Division)");
        printStringBuffer.println("//  - Zvonko Bostjancic (Blubit d.o.o. - zvonko.bostjancic@blubit.si)");
        printStringBuffer.println();
    }

    private void writeLicenseNotice(PrintStringBuffer printStringBuffer) {
        printStringBuffer.println("// Copyright (c) 1995-2022 held by the author(s).  All rights reserved.");
        printStringBuffer.println("// Redistribution and use in source and binary forms, with or without");
        printStringBuffer.println("// modification, are permitted provided that the following conditions");
        printStringBuffer.println("// are met:");
        printStringBuffer.println("// * Redistributions of source code must retain the above copyright");
        printStringBuffer.println("//    notice, this list of conditions and the following disclaimer.");
        printStringBuffer.println("// * Redistributions in binary form must reproduce the above copyright");
        printStringBuffer.println("//   notice, this list of conditions and the following disclaimer");
        printStringBuffer.println("//   in the documentation and/or other materials provided with the");
        printStringBuffer.println("//   distribution.");
        printStringBuffer.println("// * Neither the names of the Naval Postgraduate School (NPS)");
        printStringBuffer.println("//   Modeling Virtual Environments and Simulation (MOVES) Institute");
        printStringBuffer.println("//   (http://www.nps.edu and http://www.MovesInstitute.org)");
        printStringBuffer.println("//   nor the names of its contributors may be used to endorse or");
        printStringBuffer.println("//   promote products derived from this software without specific");
        printStringBuffer.println("//   prior written permission.");
        printStringBuffer.println("// ");
        printStringBuffer.println("// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS");
        printStringBuffer.println("// AS IS AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT");
        printStringBuffer.println("// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS");
        printStringBuffer.println("// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE");
        printStringBuffer.println("// COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,");
        printStringBuffer.println("// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,");
        printStringBuffer.println("// BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;");
        printStringBuffer.println("// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER");
        printStringBuffer.println("// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT");
        printStringBuffer.println("// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN");
        printStringBuffer.println("// ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE");
        printStringBuffer.println("// POSSIBILITY OF SUCH DAMAGE.");
    }
}

/**
 * Created to do post processing of any changes
 */
class PrintStringBuffer {

    public StringBuilder sb = new StringBuilder();
    public static String newline = System.getProperty("line.separator");

    public PrintStringBuffer() {
    }

    public void print(int nrOfIndents, String s) {
        sb.append(getPaddingOfLength(nrOfIndents)).append(s);
    }

    public void print(String s) {
        sb.append(s);
    }

    public void println() {
        sb.append(newline);
    }

    private StringBuffer getPaddingOfLength(int pIndent) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 4 * pIndent; i++) {
            buf.append(' ');
        }
        return buf;
    }

    public void println(String s) {
        sb.append(s).append(newline);
    }

    /**
     * Indents and prints a line including a newline
     * @param numberOfIndents Number of 4-space indents to use to indent the code
     * @param s line to print
     */
    public void println(int numberOfIndents, String s) {
        sb.append(getPaddingOfLength(numberOfIndents)).append(s).append(newline);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
