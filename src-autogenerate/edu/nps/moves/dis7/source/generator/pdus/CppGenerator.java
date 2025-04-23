/**
 * Copyright (c) 2008-2025, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */
package edu.nps.moves.dis7.source.generator.pdus;

import java.io.*;
import java.util.*;

/* not thouroughly examined, global change: VARIABLE_LIST to OBJECT_LIST and FIXED_LIST to PRIMITIVE_LIST */
/*
 * Generates the c++ language source code files needed to read and write a protocol described
 * by an XML file. This is a counterpart to the JavaGenerator. This should generate .h and
 * .cpp files with ivars, getters, setters, marshaller, unmarshaler, constructors, and
 * destructors.
 * Warning: only partially implemented.
 * John Grant specified the desired features of the C++ files.
 *
 * @author DMcG
 */

/** PDU autogeneration supporting class. */
public class CppGenerator extends AbstractGenerator
{
    /**
     * ivars are often preceded by a special character. This sets what that character is, 
     * so that instance variable names will be preceded by a "_".
     */
    public static final String IVAR_PREFIX ="";
    
    /** Maps the primitive types listed in the XML file to the cpp types */
    Properties types = new Properties();

    // class aliases, class name for specific UID
    // protected Properties uid2ClassName;
    
    /** What primitive types should be marshalled as. This may be different from
    * the cpp get/set methods, ie an unsigned short might have ints as the getter/setter,
    * but is marshalled as a short.
    */
    Properties marshalTypes = new Properties();
    
    /** sizes of various primitive types */
    Properties primitiveSizes = new Properties();
    
    /** A property list that contains cpp-specific code generation information, such
        * as package names, includes, etc.
        */
     Properties cppProperties;

     String globalNamespace = "";
     String enumNamespace = "";
     String debugAttributes = "";
     String outputCastEnums = "";

    // Kind of a strange way to keep track of the Capability types
    // private Map<String, String> capabilityNames = new HashMap<>();
    // private static Map<String,String> uid2ExtraInterface;
    // static {
    //     uid2ExtraInterface = new HashMap<>();
    //     uid2ExtraInterface.put("450", "EntityCapabilities"); //Land Platform Entity Capabilities
    //     uid2ExtraInterface.put("451", "EntityCapabilities");
    //     uid2ExtraInterface.put("452", "EntityCapabilities");
    //     uid2ExtraInterface.put("453", "EntityCapabilities");
    //     uid2ExtraInterface.put("454", "EntityCapabilities");
    //     uid2ExtraInterface.put("455", "EntityCapabilities");
    //     uid2ExtraInterface.put("456", "EntityCapabilities");
    //     uid2ExtraInterface.put("457", "EntityCapabilities");
    //     uid2ExtraInterface.put("458", "EntityCapabilities");
    //     uid2ExtraInterface.put("459", "EntityCapabilities");
    //     uid2ExtraInterface.put("460", "EntityCapabilities");
    //     uid2ExtraInterface.put("461", "EntityCapabilities");
    //     uid2ExtraInterface.put("462", "EntityCapabilities"); //Sensor/Emitter Entity Capabilities
    // }
    
    /**
     * Constructor
     * @param pClassDescriptions String Map of GeneratedClass
     * @param pCppProperties C++ properties
     */
    public CppGenerator(Map<String, GeneratedClass> pClassDescriptions, Properties pCppProperties)
    {

        super(pClassDescriptions, pCppProperties);

        // String uid2ResourceName = "Uid2ClassName.properties";
        // try {
        //     uid2ClassName = new Properties();
        //     uid2ClassName.load(this.getClass().getResourceAsStream(uid2ResourceName));
        // } catch (Exception ex) {
        //      System.out.println("Failed to read resource " + uid2ResourceName);
        //      System.out.println(ex);
        //      throw new RuntimeException(ex);
        // }

        // populateCapabilityNames();

// System.out.print(pClassDescriptions);
        // For C++ let the tool generate this PDU
        // pClassDescriptions.put("PduStatus", new GeneratedClass());

        // final Properties props = System.getProperties();
        // props.list(System.out);

        // System.out.println("CppGenerator class list:\n");
        // for (GeneratedClass aClass : pClassDescriptions.values())
        // {
        //     System.out.println("Generating class " + aClass.getName());
        // }

        try {
            Properties systemProperties = System.getProperties();
            String clDirectory = systemProperties.getProperty("xmlpg.generatedSourceDir");

            if(clDirectory != null)
                pCppProperties.setProperty("directory", clDirectory);

            super.setGeneratedSourceDirectoryName(pCppProperties.getProperty("directory"));
            // super.setGeneratedSourceDirectoryName(clDirectory);

            globalNamespace = systemProperties.getProperty("xmlpg.namespace");
            if (globalNamespace != null)
                pCppProperties.setProperty("namespace", globalNamespace);
            else
                globalNamespace = "";

            // set global namespace for enums
            enumNamespace = systemProperties.getProperty("xmlpg.enumNamespace");
            if (enumNamespace != null)
                pCppProperties.setProperty("enumNamespace", enumNamespace);
            else
                enumNamespace = "";

            debugAttributes = systemProperties.getProperty("xmlpg.debugatrributes");
            if (debugAttributes != null)
                pCppProperties.setProperty("debugAttributes", debugAttributes);
            else
                debugAttributes = "";

            outputCastEnums = systemProperties.getProperty("xmlpg.outputCastEnums");
            if (outputCastEnums != null)
                pCppProperties.setProperty("outputCastEnums", outputCastEnums);
            else
                outputCastEnums = "GOMEr";

            System.out.println("C++ PDU Output :");
            System.out.println("  directory set to : " + clDirectory);
            System.out.println("         namespace : " + globalNamespace);
            System.out.println("    enum namespace : " + enumNamespace);
        }
        catch (Exception e) {
            System.err.println("Required property not set. Modify the XML file to include the missing property");
            System.err.println(e);
            System.exit(-1);
        }

        
        // Set up a mapping between the strings used in the XML file and the strings used
        // in the java file, specifically the data types. This could be externalized to
        // a properties file, but there's only a dozen or so and an external props file
        // would just add some complexity.
        types.setProperty("unsigned short", "unsigned short");
        types.setProperty("unsigned byte", "unsigned char");
        types.setProperty("unsigned int", "unsigned int");
	types.setProperty("unsigned long", "long");
        
        types.setProperty("byte", "char");
        types.setProperty("short", "short");
        types.setProperty("int", "int");
        types.setProperty("long", "long");
        
        types.setProperty("double", "double");
        types.setProperty("float", "float");

types.setProperty("uint8",   "std::uint8_t");
types.setProperty("uint16",  "std::uint16_t");
types.setProperty("uint32",  "std::uint32_t");
types.setProperty("uint64",  "std::uint64_t");

types.setProperty("int8",    "std::int8_t");
types.setProperty("int16",   "std::int16_t");
types.setProperty("int32",   "std::int32_t");
types.setProperty("int64",   "std::int64_t");

types.setProperty("float32", "float");
types.setProperty("float64", "double");
        
        // Set up the mapping between primitive types and marshal types.
        
        marshalTypes.setProperty("unsigned short", "unsigned short");
        marshalTypes.setProperty("unsigned byte", "unsigned char");
        marshalTypes.setProperty("unsigned int", "unsigned int");
	marshalTypes.setProperty("unsigned long", "long");
        
        marshalTypes.setProperty("byte", "char");
        marshalTypes.setProperty("short", "short");
        marshalTypes.setProperty("int", "int");
        marshalTypes.setProperty("long", "long");
        
        marshalTypes.setProperty("double", "double");
        marshalTypes.setProperty("float", "float");
        
marshalTypes.setProperty("uint8",   "std::uint8_t");
marshalTypes.setProperty("uint16",  "std::uint16_t");
marshalTypes.setProperty("uint32",  "std::uint32_t");
marshalTypes.setProperty("uint64",  "std::uint64_t");

marshalTypes.setProperty("int8",    "std::int8_t");
marshalTypes.setProperty("int16",   "std::int16_t");
marshalTypes.setProperty("int32",   "std::int32_t");
marshalTypes.setProperty("int64",   "std::int64_t");

marshalTypes.setProperty("float32", "float");
marshalTypes.setProperty("float64", "double");

        // How big various primitive types are
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
        
    }
    
    /**
     * Generates the cpp source code classes
     */
    @Override
    public void writeClasses()
    {
        createGeneratedSourceDirectory(false); // boolean: whether to clean out prior files, if any exist in that directory
        
        this.writeMacroFile();
        
        Iterator it = classDescriptions.values().iterator();

        String namespace = languageProperties.getProperty("namespace");
            if(namespace == null)
                namespace = "";
            else
                namespace = namespace + "/";
            
            String enumNamespace = languageProperties.getProperty("enumNamespace");
            if(enumNamespace == null)
                enumNamespace = "";
            else
                enumNamespace = enumNamespace + "::";
        
        // Loop through all the class descriptions, generating a header file and cpp file for each.
        while(it.hasNext())
        {
            try
           {
              GeneratedClass aClass = (GeneratedClass)it.next();
			//   System.out.println("Generating class " + aClass.getName());
              this.writeHeaderFile(aClass);
              this.writeCppFile(aClass);
           }
           catch(Exception e)
           {
                System.out.println("error creating source code " + e);
           }
            
        } // End while
        
    }
   
    /**
     * Microsoft C++ requires a macro file to generate dlls. The preprocessor will import this and
     * resolve it to an empty string in the gcc/unix world. In the Microsoft C++ world, the macro
     * will resolve and do something useful about creating libraries.
     */
    public void writeMacroFile()
    {
        System.out.println("Creating microsoft library macro file");
        
        /*
        String headerFile = languageProperties.getProperty("microsoftLibHeaderMacro");
        
        if(headerFile == null)
            return;
         */
        
        String headerFile = "msLibMacro";
        
        try
        {
            String headerFullPath = getGeneratedSourceDirectoryName() + "/" + headerFile + ".h";
            File outputFile = new File(headerFullPath);
            outputFile.createNewFile();
            try (PrintWriter pw = new PrintWriter(outputFile)) {
                String libMacro = languageProperties.getProperty("microsoftLibMacro");
                String library = languageProperties.getProperty("microsoftLibDef");
                
                pw.println("#ifndef " + headerFile.toUpperCase() + "_H");
                pw.println("#define " + headerFile.toUpperCase() + "_H");
                
                pw.println("#if defined(_MSC_VER) || defined(__CYGWIN__) || defined(__MINGW32__) || defined( __BCPLUSPLUS__)  || defined( __MWERKS__)");
                pw.println("#  ifdef EXPORT_LIBRARY");
                pw.println("#    define " + "EXPORT_MACRO"  + " __declspec(dllexport)");
                pw.println("#  else");
                pw.println("#    define EXPORT_MACRO  __declspec(dllimport)");
                pw.println("#  endif");
                pw.println("#else");
                pw.println("#  define " + "EXPORT_MACRO");
                pw.println("#endif");
                
                pw.println("#endif");
                
                pw.flush();
            }
        }
        catch(IOException e)
        {
            System.err.println(e);
        }
}

// Generate a list of UID to name for each capability type
// private void populateCapabilityNames()
// {
//     for (Map.Entry<String, String> entry : uid2ExtraInterface.entrySet())
//     {
//         String uidValue = entry.getKey();
//         String typeName = uid2ClassName.getProperty(uidValue);
//         if (typeName != null)
//         {
//             capabilityNames.put(uidValue, typeName);
//         }
//     }
// }


private void outputCapabilityToString(PrintWriter pw)
{
    pw.println();
    pw.println("    struct Capabilities");
    pw.println("    {");
}

private void outputCapabilityUnion(PrintWriter pw)
{
    pw.println();
    pw.println("    struct Capabilities");
    pw.println("    {");
    pw.print("        enum class Type {");
    String unionStatement = "";
    for (Map.Entry<String, String> entry : capabilityNames.entrySet())
    {
        String enumName = parseToFirstCap(entry.getValue()).toUpperCase();
        unionStatement += String.format("%s,", enumName);
    }
    unionStatement = removeLastCharacter(unionStatement);
    pw.print(unionStatement);
    pw.println("};");

    pw.println("        Type type;");

    pw.println("        union");
    pw.println("        {");
    for (Map.Entry<String, String> entry : capabilityNames.entrySet())
    {
        String capName  = entry.getValue();
        pw.println(String.format("            %s %s;", capName, initialLower(capName)));
    }
    pw.println("        };");    // end the union
    pw.println();

    // Generate the constructors
    for (Map.Entry<String, String> entry : capabilityNames.entrySet())
    {
        String uidValue = entry.getKey();
        String capName  = entry.getValue();
        String enumName = parseToFirstCap(entry.getValue()).toUpperCase();
        pw.print(String.format("        Capabilities(const %s& capValue) : type{Type::%s} { %s = capValue; }\n",
            capName, enumName, initialLower(capName)));
    }

    pw.println("    };");   // end the struct

//     String variantStatement = "    struct Capabilities \n    { \n";
//     variantStatement += "        enum class Type {";
//             variantStatement += "};\n"
// private static Map<String,String> uid2ExtraInterface;
//             // Add variant and using statements for the capability types
//             // TODO : is this the best approach??
//             String variantStatement = "    struct Capabilities \n    { \n";
//             variantStatement += "        enum class Type {";
//             variantStatement += "};\n"

//             for (Map.Entry<String, String> entry : uid2ExtraInterface.entrySet())
//             {
//                 String uidValue = entry.getKey();
//                 String typeName = uid2ClassName.getProperty(uidValue);
//                 pw.println(String.format("    using %s = %s%s;",
//                             typeName,
//                             enumNamespace,
//                             typeName));
//                 variantStatement += String.format("        %s %s;\n", typeName, initialLower(typeName));
//             }
//             variantStatement += "    };\n";
//     return variantStatement;
}

public void addCapabilityIncludeStatements(PrintWriter pw)
{
    // Add capabiity include files
    for (Map.Entry<String, String> entry : uid2ExtraInterface.entrySet())
    {
        String uidValue = entry.getKey();
        String aliasName = uid2ClassName.getProperty(uidValue);
        if (aliasName != null)
            pw.println("#include <" + globalNamespace + "enumerations/" + aliasName + ".h>");
    }
}


/**
 * Add include statements to generated class
 * @param aClass class of interest
 * @param namespace global namespace for all output
 * @param PrintWriter print writer for output
 * @param hasVariableLengthList set if an attribute of a variable list is found
 */

public void writeIncludeStatements(GeneratedClass aClass, String namespace, PrintWriter pw, boolean hasVariableLengthList)
{
// Add the include statements
        for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
        {
            GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
            if( (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
                (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD) )
            {
                String typeName = anAttribute.getType();
                String uidValue = anAttribute.getUnderlyingUid();

                if (!uidValue.isEmpty())
                {
                    String aliasName = uid2ClassName.getProperty(uidValue);
                    if (aliasName != null && !aliasName.isEmpty())
                    {
                        // if (!typeName.equals(aliasName))
                        // {
                        //     pw.println("// Using an alias here");
                        //     pw.println("//     UID : " +uidValue);
                        //     pw.println("//    From : " + typeName);
                        //     pw.println("//    To   : " + aliasName);
                        // }
                        typeName = aliasName;
                    }

                }

                pw.println("#include <" + namespace + "enumerations/" + typeName + ".h>");
            }
            // If this attribute is a class, we need to do an import on that class
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
            {
                pw.println("#include <" + namespace + anAttribute.getType() + ".h>");
            }
            
            // if this attribute is a variable-length list that holds a class, we need to
            // do an import on the class that is in the list.
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
            {
                // The object list may actually be a list of enumerations, no great way of knowing
                // <objectlist countFieldName="numberOfRecords">
                //      <classRef name="RecordSpecificationElement"/>
                //      <sisoenum type="VariableRecordType" comment="uid = 66"/>

                if ( (anAttribute.underlyingTypeIsEnum) || (anAttribute.isBitField))
                {
                    pw.println("#include <" + namespace + "enumerations/" + anAttribute.getType() + ".h>");
                }
                else {
                    pw.println("#include <" + namespace + anAttribute.getType() + ".h>");
                }
                hasVariableLengthList = true;
            }
        }

        pw.println();
        // Add the include statements for all the capability types
        if (classUsesCapabilityTypes(aClass))
        {
            pw.println("#include <" + namespace + "enumerations/Capabilities.h>");
            for (Map.Entry<String, String> entry : uid2ExtraInterface.entrySet())
            {
                String uidValue = entry.getKey();
                String aliasName = uid2ClassName.getProperty(uidValue);
                if (aliasName != null)
                    pw.println("#include <" + namespace + "enumerations/" + aliasName + ".h>");
            }
            pw.println();
        }
        
        if(hasVariableLengthList == true)
        {
            pw.println("#include <vector>");
        }
        
        // if we inherit from another class we need to do an include on it
        if(!(aClass.getParentClass().equalsIgnoreCase("root")))
        {
            pw.println("#include <" + namespace + aClass.getParentClass() + ".h>");
        }
        
        // "the usual" includes.
        // pw.println("#include <vector>");
        pw.println("#include <iostream>");
        pw.println("#include <cstdint>");
        //pw.println("#include <" + namespace + "DataStream.h>");
        pw.println("#include <" + namespace + "utils/DataStream.h>");
        // pw.println("#include <dis/utils/DataStream.h>");
        
        // This is a macro file included only for microsoft compilers. set in the cpp properties tag.
        String msMacroFile = "msLibMacro";
        
        if(msMacroFile != null)
        {
            pw.println("#include <" + namespace + msMacroFile + ".h>");
        }
}

/**
 * Add include statements to generated class
 * @param aClass class of interest
 * @param namespace global namespace for all output
 * @param enumNamespace namespace for enumerated types
 * @param PrintWriter print writer for output
 */
public void writeUsingStatements(GeneratedClass aClass, String namespace, String enumNamespace, PrintWriter pw)
{
    pw.println("public:");
    for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
    {
        GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);

        String typeName = anAttribute.getType();
        String uidValue = anAttribute.getUnderlyingUid();

        if (!uidValue.isEmpty())
        {
            String aliasName = uid2ClassName.getProperty(uidValue);
            if (aliasName != null && !aliasName.isEmpty()) typeName = aliasName;
        }

        if ( (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
                (anAttribute.getIsBitField() == true))
        {
            pw.println(String.format("    using %s = %s%s;",
                                        typeName,
                                        enumNamespace,
                                        typeName));
        }
    }

    // Generate the using statements
    if (classUsesCapabilityTypes(aClass))
    {
        pw.println(String.format("    using Capabilities = %sCapabilities;", enumNamespace));
        for (Map.Entry<String, String> entry : capabilityNames.entrySet())
        {
            String uidValue = entry.getKey();
            String capName  = entry.getValue();

            pw.println(String.format("    using %s = %s%s;", capName, enumNamespace, capName));
        }
    }
}

/**
 * Add include statements to generated class
 * @param aClass class of interest
 * @param enumNamespace namespace for enumerated types
 * @param PrintWriter print writer for output
 */
public void writeIvars(GeneratedClass aClass, String enumNamespace, PrintWriter pw)
{

    // Print out ivars. These are made protected for now.
    pw.println("\nprotected:");
    if (debugAttributes.equals("true"))
    {
        pw.println("");
        pw.println("/// ATTRIBUTES");
        pw.println("/// Class " + aClass.getName() + " has " + aClass.getClassAttributes().size() + " Attributes");
        for (int idx = 0; idx < aClass.getClassAttributes().size(); idx++) {
                GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
                pw.println("    ///    " + anAttribute.getName() + "\t : " + anAttribute.getAttributeKind());
        }
        pw.println("");
    }

    
    for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
    {
        GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
        if (debugAttributes.equals("true"))
        {                
            pw.println("/// --------------------------------------------------------------------------");
            pw.println("/// ATTRIBUTE");
            pw.println(anAttribute.ToString());
            pw.println("");
        }

        if((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO16) ||
            (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO32) ||
            (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO64))
        {
            if(anAttribute.getComment() != null)
                pw.println("  " + "/** " + anAttribute.getComment() + " */");

            String bitCount = String.format("%s", anAttribute.getAttributeKind());
            bitCount = bitCount.substring(bitCount.length() - 2);
            pw.println(String.format("    std::uint8_t %s%s[%s];", IVAR_PREFIX, anAttribute.getName(), bitCount));
        }

        if( (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
            (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD))
        {
            String typeName = anAttribute.getType();
            String uidValue = anAttribute.getUnderlyingUid();

            if (!uidValue.isEmpty())
            {
                String aliasName = uid2ClassName.getProperty(uidValue);
                if (aliasName != null && !aliasName.isEmpty()) typeName = aliasName;
                // Need to do some magic to output a capability type
                if (uidValue.equals("55"))
                {
                    typeName = "Capabilities";
                }
            }

            if(anAttribute.getComment() != null) {
                pw.println("  " + "/** " + anAttribute.getComment() + " */");
            }
            pw.println("  " + typeName + " " + IVAR_PREFIX + anAttribute.getName() + "; ");
            pw.println();
        }

        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
        {
            if(anAttribute.getComment() != null)
                pw.println("  " + "/** " + anAttribute.getComment() + " */");
            pw.println("  " + types.get(anAttribute.getType()) + " " + IVAR_PREFIX + anAttribute.getName() + "; ");
            pw.println();
            
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
        {
            if(anAttribute.getComment() != null)
                pw.println("  " + "/** " + anAttribute.getComment() + " */");
            
            pw.println("  " + anAttribute.getType() + " " + IVAR_PREFIX + anAttribute.getName() + "; ");
            pw.println();
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
        {
            if(anAttribute.getComment() != null)
                pw.println("  " + "/** " + anAttribute.getComment() + " */");
            
            pw.println("  " + types.get(anAttribute.getType()) + " " + IVAR_PREFIX + anAttribute.getName() + "[" + anAttribute.getListLength() + "]; ");
            pw.println();
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
        {
            if(anAttribute.getComment() != null)
                pw.println("  " + "/** " + anAttribute.getComment() + " */");

            if (anAttribute.underlyingTypeIsEnum) {
                pw.println("  std::vector<" + enumNamespace + anAttribute.getType() + "> " + IVAR_PREFIX + anAttribute.getName() + "; ");
            }
            else {
                pw.println("  std::vector<" + anAttribute.getType() + "> " + IVAR_PREFIX + anAttribute.getName() + "; ");
            }
            pw.println();
        }

    }
}

/**
 * Add include statements to generated class
 * @param aClass class of interest
 * @param enumNamespace namespace for enumerated types
 * @param PrintWriter print writer for output
 */
public void writeGetterAndSetterDecl(GeneratedClass aClass, String enumNamespace, PrintWriter pw)
{
    // Getter and setter methods for each ivar
    for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
    {
        GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
        
        if((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
            (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD))
        {
            String uidValue = anAttribute.getUnderlyingUid();
            String typeName = anAttribute.getType();

            if (uidValue != null && !uidValue.isEmpty())
            {
                String aliasName = uid2ClassName.getProperty(uidValue);
                if (aliasName != null && !aliasName.isEmpty()) typeName = aliasName;
            }

            // write methods to case the enums to unsigned shorts for backwards compatibility
            if (outputCastEnums.equals("true"))
                pw.println("    uint16_t get" + this.initialCapital(anAttribute.getName()) + "() const; ");
            else
                pw.println("    " + typeName + " " + "get" + this.initialCapital(anAttribute.getName()) + "() const; ");
            if(anAttribute.getIsDynamicListLengthField() == false)
            {
                pw.println("    void " + "set" + this.initialCapital(anAttribute.getName()) + "(" + typeName + " pX); ");
                if (outputCastEnums.equals("true"))
                {
                    pw.println("    void " + "set" + this.initialCapital(anAttribute.getName()) + "(uint16_t pX); ");
                }
            }
        }

        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
        {
            pw.println("    " + types.get(anAttribute.getType()) + " " + "get" + this.initialCapital(anAttribute.getName()) + "() const; ");
            if(anAttribute.getIsDynamicListLengthField() == false)
            {
                pw.println("    void " + "set" + this.initialCapital(anAttribute.getName()) + "(" + types.get(anAttribute.getType()) + " pX); ");
            }
        }

        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
        {
            pw.println("    " + anAttribute.getType() + "& " + "get" + this.initialCapital(anAttribute.getName()) + "(); ");
            pw.println("    const " + anAttribute.getType() + "&  get" + this.initialCapital(anAttribute.getName()) + "() const; ");
            pw.println("    void set" + this.initialCapital(anAttribute.getName()) + "(const " + anAttribute.getType() + "    &pX);");
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
        {
            // Sleaze. We need to figure out what type of array we are, and this is slightly complex.
            String arrayType = this.getArrayType(anAttribute.getType());
            pw.println("    " + arrayType + "*  get" + this.initialCapital(anAttribute.getName()) + "(); ");
            pw.println("    const " + arrayType + "*  get" + this.initialCapital(anAttribute.getName()) + "() const; ");
            pw.println("    void set" + this.initialCapital(anAttribute.getName()) + "( const " + arrayType + "*    pX);");
            if(anAttribute.getCouldBeString() == true)
            {
                pw.println("    void " + "setByString" + this.initialCapital(anAttribute.getName()) + "(const " + arrayType + "* pX);");
            }
            
        }
        
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
        {
            String nameSpace = "";
            if (anAttribute.underlyingTypeIsEnum) nameSpace = enumNamespace;

            pw.println("    std::vector<" + nameSpace + anAttribute.getType() + ">& " + "get" + this.initialCapital(anAttribute.getName()) + "(); ");
            pw.println("    const std::vector<" + nameSpace + anAttribute.getType() + ">& " + "get" + this.initialCapital(anAttribute.getName()) + "() const; ");
            pw.println("    void set" + this.initialCapital(anAttribute.getName()) + "(const std::vector<" + nameSpace + anAttribute.getType() + ">&    pX);");
        }
        
        pw.println();
    }
}

/**
 * Generate a c++ header file for the classes
 * @param aClass class of interest
 */
public void writeHeaderFile(GeneratedClass aClass)
{
    try
    {
        String name = aClass.getName();
        // System.out.println(String.format("Generating header file for class %s\n", name));
        //System.out.println("Creating cpp and .h source code files for " + name);
        String headerFullPath = getGeneratedSourceDirectoryName() + "/" + name + ".h";
        File outputFile = new File(headerFullPath);
        outputFile.createNewFile();

        // Write the usual #ifdef to prevent multiple inclusions by the preprocessor
        try (PrintWriter pw = new PrintWriter(outputFile)) {
            // Write the usual #ifdef to prevent multiple inclusions by the preprocessor
            pw.println("#ifndef " + aClass.getName().toUpperCase() + "_H");
            pw.println("#define " + aClass.getName().toUpperCase() + "_H");
            pw.println();
            
            // Write includes for any classes we may reference. this generates multiple #includes if we
            // use a class multiple times, but that's innocuous. We could sort and do a unqiue to prevent
            // this if so inclined.

            String namespace = languageProperties.getProperty("namespace");
            if(namespace == null)
                namespace = "";
            else
                namespace = namespace + "/";
            
            String enumNamespace = languageProperties.getProperty("enumNamespace");
            if(enumNamespace == null)
                enumNamespace = "";
            else
                enumNamespace = enumNamespace + "::";

            boolean hasVariableLengthList = false;

            writeIncludeStatements(aClass, namespace, pw, hasVariableLengthList);

            // pw.println();
            
            // shameful hardcoded value for now
            // Declare the namespace in case no header was used, this is terrible
            // pw.println("namespace DIS { namespace siso_ref_010 { namespace enums{ }}}");
            // pw.println("using namespace DIS::siso_ref_010::enums;");
            pw.println();

            // Print out namespace, if any
            namespace = languageProperties.getProperty("namespace");
            if(namespace != null)
            {
                pw.println("namespace " + namespace);
                pw.println("{");
            }
            
            // Print out the class comments, if any
            if(aClass.getClassComments() != null)
            {
                pw.println("// " + aClass.getClassComments() );
            }
            
            pw.println();
            pw.println("// Copyright (c) 2007-2012, MOVES Institute, Naval Postgraduate School. All rights reserved. ");
            pw.println("// Licensed under the BSD open source license. See http://www.movesinstitute.org/licenses/bsd.html");
            pw.println("//");
            pw.println("// @author DMcG, jkg");
            pw.println();
            
            if(hasVariableLengthList == true)
            {
                pw.println("#pragma warning(disable: 4251 ) // Disables warning for stl vector template DLL export in msvc");
                pw.println();
            }
            
            // Print out class header and ivars
            
            String macroName = languageProperties.getProperty("microsoftLibMacro");
            
            if(aClass.getParentClass().equalsIgnoreCase("root"))
                pw.println("class EXPORT_MACRO " + aClass.getName());
            else
                pw.println("class EXPORT_MACRO " + aClass.getName() + " : public " + aClass.getParentClass());
            
            pw.println("{");

            writeUsingStatements(aClass, namespace, enumNamespace, pw);

            // only need this if we have capability type
            // if (classUsesCapabilityTypes(aClass))
            // {
            //     outputCapabilityUnion(pw);
            //     // outputCapabilityToString(pw);
            // }

            writeIvars(aClass, enumNamespace, pw);
            
            // Delcare ctor and dtor in the public area
            pw.println("\n public:");
            // Constructor
            pw.println("    " + aClass.getName() + "();");
            
            
            // Destructor
            pw.println("    virtual ~" + aClass.getName() + "();");
            pw.println();
            
            
            // Marshal and unmarshal methods
            pw.println("    virtual void marshal(DataStream& dataStream) const;");
            pw.println("    virtual void unmarshal(DataStream& dataStream);");
            pw.println("    virtual int get_marshaled_size() const;");

            pw.println();
            pw.print(String.format("    std::string to_string() const;\n"));
            // pw.print(String.format("    std::ostream& operator<<(std::ostream& os, const %s& value);\n", aClass.getName()));

            pw.println();
            writeGetterAndSetterDecl(aClass, enumNamespace, pw);
            
            // Generate an equality operator
            pw.println("    bool operator  ==(const " + aClass.getName() + "& rhs) const;");
            
            pw.println("};");
            
            // Write out a Dis6 equavalent for this one
            if (aClass.getName().equals("TransferOwnershipPdu"))
            {
                pw.println("using TransferControlRequestPdu = TransferOwnershipPdu;");
                pw.println();
            }

            // Close out namespace brace, if any
            if(namespace != null)
            {
                pw.println("}");
            }
            
            // Close if #ifndef statement that prevents multiple #includes
            pw.println("\n#endif");



            this.writeLicenseNotice(pw);
            
            pw.flush();
        }
    } // End of try // End of try
    catch(IOException e)
    {
        System.err.println(e);
    }
                
} // End write header file

/** Check if the class uses an enum, used to write out enum namespace
  * @param aClass GeneratedClass to look for enum
  */
public Boolean classHasEnum(GeneratedClass aClass)
{
    Boolean hasEnum = false;
    for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
    {
        GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
        if((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
           (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD))
        {
            hasEnum = true;
            break;
        }

        if((anAttribute.underlyingTypeIsEnum) ||
           (anAttribute.getIsBitField()))
        {
            hasEnum = true;
            break;
        }
    }

    return hasEnum;
}

   /** output file for this class
     * @param aClass GeneratedClass to write file for */
public void writeCppFile(GeneratedClass aClass)
{
    try
   {
        String name = aClass.getName();
        //System.out.println("Creating cpp and .h source code files for " + name);
        String headerFullPath = getGeneratedSourceDirectoryName() + "/" + name + ".cpp";
        File outputFile = new File(headerFullPath);
        outputFile.createNewFile();
        try (PrintWriter pw = new PrintWriter(outputFile)) {

            // resolve namespace names
            // String namespace = languageProperties.getProperty("namespace");
            // String enumNamespace = languageProperties.getProperty("namespace");

            String globalNamespaceFolder = "";
            if(globalNamespace == "")
                globalNamespaceFolder ="";
            else
                globalNamespaceFolder = globalNamespace +"/";

            pw.println("#include <" + globalNamespaceFolder + aClass.getName() + ".h> ");
            pw.println();

            if (globalNamespace != "") {
                pw.println("using namespace " + globalNamespace + ";");
            }

            if (classHasEnum(aClass) == true && enumNamespace != "") {
                pw.println("using namespace " + enumNamespace + ";");
            }

            pw.println();
            
            // Write ctor
            this.writeCtor(pw, aClass);
            this.writeDtor(pw, aClass);
            
            // Write the getter and setter methods for each of the attributes
            for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
            {
                GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
                this.writeGetterMethod(pw, aClass, anAttribute);
                this.writeSetterMethod(pw, aClass, anAttribute);
            }
            
            // Write marshal and unmarshal methods
            this.writeMarshalMethod(pw, aClass);
            this.writeUnmarshalMethod(pw, aClass);
            
            // Write a comparision operator
            this.writeEqualityOperator(pw, aClass);
            
            // Method to determine the marshalled length of the PDU
            this.writeGetMarshalledSizeMethod(pw, aClass);

            this.writeToSringMethod(pw, aClass);
            
            // License notice
            this.writeLicenseNotice(pw);
            
            pw.flush();
        }
    }
    catch(IOException e)
    {
        System.err.println(e);
    }
}

/**
 * Write the code for an equality operator.This allows you to compare
 two objects for equality.The code should look like
 
 bool operator ==(const ClassName&amp; rhs)
 return (_ivar1==rhs._ivar1 &amp;&amp; _var2 == rhs._ivar2 ...)
 *
     * @param pw output
     * @param aClass class of interest
 */
public void writeEqualityOperator(PrintWriter pw, GeneratedClass aClass)
{
    try
    {
        pw.println();
        pw.println("bool " + aClass.getName() + "::operator ==(const " + aClass.getName() + "& rhs) const");
        pw.println(" {");
        pw.println("     bool ivarsEqual = true;");
        pw.println();
        
        // Handle the superclass, if any
        String parentClass = aClass.getParentClass();
        if(!(parentClass.equalsIgnoreCase("root")) )
        {
            pw.println("     ivarsEqual = " + parentClass + "::operator==(rhs);");
            pw.println();
        }
        
        for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
        {
            GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
            
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE || anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
            {
                if(anAttribute.getIsDynamicListLengthField() == false)
                {
                    pw.println("     if( ! (" + IVAR_PREFIX + anAttribute.getName() + " == rhs." + IVAR_PREFIX + anAttribute.getName() + ") ) ivarsEqual = false;");
                }
                /*
                else
                {
                    pw.println("     if( ! (  this.get" + this.initialCapital(anAttribute.getName()) + "() == rhs.get" + this.initialCapital(anAttribute.getName()) + "()) ) ivarsEqual = false;");
                }
                 */
                
            }
            
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
            {
                String indexType = (String)types.get(anAttribute.getType());
                indexType = "int";
                
                pw.println();
                if (anAttribute.getCountFieldName() != null)
                    pw.println("     for(size_t idx = 0; idx < " + anAttribute.getCountFieldName() + "; idx++)");
                else
                    pw.println("     for(" + indexType + " idx = 0; idx < " + anAttribute.getListLength() + "; idx++)");
                pw.println("     {");
                pw.println("          if(!(" + IVAR_PREFIX + anAttribute.getName() + "[idx] == rhs." + IVAR_PREFIX + anAttribute.getName() + "[idx]) ) ivarsEqual = false;");
                pw.println("     }");
                pw.println();
            }
            
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
            {
                pw.println();
                pw.println("     for(size_t idx = 0; idx < " + IVAR_PREFIX + anAttribute.getName() + ".size(); idx++)");
                pw.println("     {");
               // pw.println("        " + aClass.getName() + " x = " + IVAR_PREFIX + anAttribute.getName() + "[idx];");
                pw.println("        if( ! ( " + IVAR_PREFIX + anAttribute.getName() + "[idx] == rhs." + IVAR_PREFIX + anAttribute.getName() + "[idx]) ) ivarsEqual = false;");
                pw.println("     }");
                pw.println();
            }
            
        }
        
        
        pw.println();
        pw.println("    return ivarsEqual;");
        pw.println(" }");
    }
    catch(Exception e)
    {
        System.out.println(e);
    }
    
}

/**
 * Write the code for a method that marshals out the object into a DIS format
 * byte array.
 * @param pw PrintWriter
 * @param aClass class of interest
 */
public void writeMarshalMethod(PrintWriter pw, GeneratedClass aClass)
{
    try
    {
        pw.println("void " + aClass.getName() + "::" + "marshal(DataStream& dataStream) const");
        pw.println("{");
        
        // If this inherits from one of our classes, we should call the superclasses' 
        // marshal method first. The syntax for this is SuperclassName::marshal(dataStream).
        
        // If it's not already a root class
        if(!(aClass.getParentClass().equalsIgnoreCase("root")))
        {
            String superclassName = aClass.getParentClass();
            pw.println("    " + superclassName + "::marshal(dataStream); // Marshal information in superclass first");
        }
            
        
        for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
        {

            GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
            
            if(anAttribute.shouldSerialize == false)
            {
                pw.println("     // attribute " + anAttribute.getName() + " marked as do not serialize");
                continue;
            }

            // Write out the code to marshal this, depending on the type of attribute
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
            { 
                if(anAttribute.getIsDynamicListLengthField() == false)
                {
                     pw.println("    dataStream << " +  IVAR_PREFIX + anAttribute.getName() + ";");
                }
                else
                {
                     GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();
                     pw.println("    dataStream << ( " + types.get(anAttribute.getType()) + " )" +  IVAR_PREFIX + listAttribute.getName() + ".size();");
                }

            }
            
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
            { 
                pw.println("    " +  IVAR_PREFIX + anAttribute.getName() + ".marshal(dataStream);");
            }
            
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
            { 
                pw.println();
                
                if (anAttribute.getCountFieldName() != null)
                    pw.println("     for(size_t idx = 0; idx < " + anAttribute.getCountFieldName() + "; idx++)");
                else
                    pw.println("     for(size_t idx = 0; idx < " + anAttribute.getListLength() + "; idx++)");
                pw.println("     {");
                
                // This is some sleaze. We're an array, but an array of what? We could be either a
                // primitive or a class. We need to figure out which. This is done via the expedient
                // but not very reliable way of trying to do a lookup on the type. If we don't find
                // it in our map of primitives to marshal types, we assume it is a class.
                
                String marshalType = marshalTypes.getProperty(anAttribute.getType());
                
                if(marshalType == null) // It's a class
                {
                    pw.println("     " +  IVAR_PREFIX + anAttribute.getName() + "[idx].marshal(dataStream);");
                }
                else
                {
                    pw.println("        dataStream << " +  IVAR_PREFIX + anAttribute.getName() + "[idx];");
                }
                
                pw.println("     }");
                pw.println();
            }
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
            {
                pw.println("    " + enumNamespace + "::marshal(dataStream," + IVAR_PREFIX + anAttribute.getName() + ");");
                // pw.println("    DIS::siso_ref_010::enums::marshal(dataStream," + IVAR_PREFIX + anAttribute.getName() + ");");
            }

            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
            { 
                pw.println();
                pw.println("     for(size_t idx = 0; idx < " +  IVAR_PREFIX + anAttribute.getName() + ".size(); idx++)");
                pw.println("     {");

                String marshalType = marshalTypes.getProperty(anAttribute.getType());

                // cast an enum to a uint8.  Some enums can be uint16 or uinty32....
                // if (anAttribute.underlyingTypeIsEnum)
                if ( (anAttribute.underlyingTypeIsEnum) || (anAttribute.isBitField))
                {
                    pw.println("        " + anAttribute.getType() + " x = " +  IVAR_PREFIX + anAttribute.getName() + "[idx];");
                    pw.println("        " + enumNamespace + "::marshal(dataStream,x);");
                }
                else if(marshalType == null) // It's a class
                {
                    pw.println("        " + anAttribute.getType() + " x = " +  IVAR_PREFIX + anAttribute.getName() + "[idx];");
                    pw.println("        x.marshal(dataStream);");
                }
                else // it's a primitive
                {
                    pw.println("        " + anAttribute.getType() + " x = " +  IVAR_PREFIX + anAttribute.getName() + "[idx];");
                    pw.println("    dataStream <<  x;"); 
                }
               
                    pw.println("     }");
                    pw.println();
            }
        }
        pw.println("}");
        pw.println();
    
    }
  catch(Exception e)
  {
      System.err.println(e);
  }
}

    /**
     * write out unmarshal method
     * @param pw PrintWriter
     * @param aClass a GeneratedClass
     */
public void writeUnmarshalMethod(PrintWriter pw, GeneratedClass aClass)
{
  try
  {
    pw.println("void " + aClass.getName() + "::" + "unmarshal(DataStream& dataStream)");
    pw.println("{");
    
    // If it's not already a root class
    if(!(aClass.getParentClass().equalsIgnoreCase("root")))
    {
        String superclassName = aClass.getParentClass();
        pw.println("    " + superclassName + "::unmarshal(dataStream); // unmarshal information in superclass first");
    }
    
    for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
    {
        GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
        
        if(anAttribute.shouldSerialize == false)
        {
            pw.println("     // attribute " + anAttribute.getName() + " marked as do not serialize");
            continue;
        }
        
        // Write out the code to marshal this, depending on the type of attribute
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
        { 
            pw.println("    dataStream >> " +  IVAR_PREFIX + anAttribute.getName() + ";");
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
        {
            pw.println("    " + enumNamespace + "::unmarshal(dataStream," + IVAR_PREFIX + anAttribute.getName() + ");");
        }
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
        { 
            pw.println("    " +  IVAR_PREFIX + anAttribute.getName() + ".unmarshal(dataStream);");
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
        { 
            pw.println();
            if (anAttribute.getCountFieldName() != null)
                pw.println("     for(size_t idx = 0; idx < " + anAttribute.getCountFieldName() + "; idx++)");
            else
                pw.println("     for(size_t idx = 0; idx < " + anAttribute.getListLength() + "; idx++)");
            pw.println("     {");
            pw.println("        dataStream >> " +  IVAR_PREFIX + anAttribute.getName() + "[idx];");
            pw.println("     }");
            pw.println();
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
        { 
            pw.println();
            pw.println("     " + IVAR_PREFIX + anAttribute.getName() + ".clear();"); // Clear out any existing objects in the list
            pw.println("     for(size_t idx = 0; idx < " + IVAR_PREFIX + anAttribute.getCountFieldName() + "; idx++)");
            pw.println("     {");
            
            // This is some sleaze. We're an list, but an list of what? We could be either a
            // primitive or a class. We need to figure out which. This is done via the expedient
            // but not very reliable way of trying to do a lookup on the type. If we don't find
            // it in our map of primitives to marshal types, we assume it is a class.
            
            String marshalType = marshalTypes.getProperty(anAttribute.getType());
            
            if ( (anAttribute.underlyingTypeIsEnum) || (anAttribute.isBitField))
            {
                pw.println("        " + anAttribute.getType() + " x = " +  IVAR_PREFIX + anAttribute.getName() + "[idx];");
                // pw.println("        DIS::siso_ref_010::enums::unmarshal(dataStream, x);" );
                pw.println("       " + enumNamespace + "::unmarshal(dataStream,x);");
                // pw.println("        dataStream >> (uint8_t)x;"); 
            }
            else if(marshalType == null) // It's a class
            {
                pw.println("        " + anAttribute.getType() + " x;");
                pw.println("        x.unmarshal(dataStream);" );
                pw.println("        " +  IVAR_PREFIX + anAttribute.getName() + ".push_back(x);");
            }
            else // It's a primitive
            {
                pw.println("       " +  IVAR_PREFIX + anAttribute.getName() + "[idx] << dataStream");
            }

            pw.println("     }");
        }
    }
    
    pw.println("}");
    pw.println();
    
}
catch(Exception e)
{
    System.out.println(e);
}
}

/**
    * returns a string with the enumerated value portion in lower case, replacting . with ::
    * @param anEnum enumeration in form EnumeratedType::ENUM_VALUE
    * @return same string with enum value lower
    */
public String reformatEnumValue(String anEnum)
{
    String returnValue = anEnum;

    if (anEnum.contains("."))
    {
        String values[] = anEnum.split("\\.");
        returnValue = values[0] + "::" + values[1].toLowerCase();
    }

    return returnValue;
}

/** 
 * Write a constructor. This uses an initialization list to initialize the various object
* ivars in the class. God, C++ is a PITA. The result should be something like
* Foo::Foo() : bar(Bar(), baz(Baz()
*/
private void writeCtor(PrintWriter pw, GeneratedClass aClass)
{
    boolean colonForInitializerListUsed = false;
    
    pw.print(aClass.getName() + "::" + aClass.getName() + "()");
    
    // Need to do a pre-flight here; cycle throguh the attributes and get a count
    // of the attribtes that are either primitives or objects. The 
    // fixed length lists are not initialized in the initializer list.
    // Initialize enums as well.
    int attributeCount = 0;
    
    for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
    {
        GeneratedClassAttribute attribute = aClass.getClassAttributes().get(idx);
        if((attribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE) ||
           (attribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
           (attribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD) ||
           (attribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF) ||
           (attribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST) ||
           (attribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO16) ||
           (attribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO32) ||
           (attribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO64))
        {
            attributeCount++;
        }
    }
        
        // If this has a superclass, class the constructor for that (via the initializer list)
        if(!(aClass.getParentClass().equalsIgnoreCase("root")))
        {
             // Do an initailizer list for the ctor    
            pw.print("\n   : "); // Start initializer list
            colonForInitializerListUsed = true;
            pw.print(aClass.getParentClass() + "()");
            if(attributeCount > 0)
                pw.print(",");
            pw.println();
        }

       // If we have one or more things in the initializer list, and if we haven't already started an
       // initializer list with the superclass, print the colon that starts the initializer list
       if((attributeCount > 0) && (colonForInitializerListUsed == false))
           pw.print("\n   :");

        String indent = " ";
        for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
        {
            if ((idx > 0) || (colonForInitializerListUsed == true)) indent = "     ";

            GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
            String defaultValue = anAttribute.getDefaultValue();

            // This is a primitive type; initialize it to either the default value specified in 
            // the XML file or to zero. Tends to minimize the possiblity
            // of bad stack-allocated values hosing the system.
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
            { 
                String initialValue = "0";
                String ivarType = anAttribute.getType();
                if( (ivarType.equalsIgnoreCase("float")) || (ivarType.equalsIgnoreCase("double")))
                    initialValue = "0.0";
                
                if(defaultValue != null)
                    initialValue = defaultValue;
                
                pw.print(indent + IVAR_PREFIX + anAttribute.getName() + "(" + initialValue + ")");
                
                attributeCount--;
                if(attributeCount != 0)
                {
                    pw.println(", "); // Every initiailizer list element should have a following comma except the last
                }
                         
            }
                       
            // We need to allcoate ivars that are objects....
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
            { 
               // pw.print(" " + anAttribute.getName() + "( " + anAttribute.getType() + "())" );
                pw.println(indent + "// TODO - Process the default value for a CLASSREF type");
                pw.print(indent +  IVAR_PREFIX + anAttribute.getName() + "()" );
                attributeCount--;
                if(attributeCount != 0)
                {
                    pw.println(", "); // Every initiailizer list element should have a following comma except the last
                }
            }
            
            // We need to allcoate ivars that are lists/vectors....
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
            { 
               // pw.print(" " + anAttribute.getName() + "( " + anAttribute.getType() + "())" );
                pw.print(indent +  IVAR_PREFIX + anAttribute.getName() + "(0)" );
                attributeCount--;
                if(attributeCount != 0)
                {
                    pw.println(", "); // Every initiailizer list element should have a following comma except the last
                }
            }

            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO16) ||
                (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO32) ||
                (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO64))
            {
                pw.print(indent + IVAR_PREFIX + anAttribute.getName() + "{0}");
                attributeCount--;
                if(attributeCount != 0)
                {
                    pw.println(", "); // Every initiailizer list element should have a following comma except the last
                }
            }

            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
                (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD))
            {

                String typeName = anAttribute.getType();
                String uidValue = anAttribute.getUnderlyingUid();

                if (!uidValue.isEmpty())
                {
                    String aliasName = uid2ClassName.getProperty(uidValue);
                    if (aliasName != null && !aliasName.isEmpty()) typeName = aliasName;
                }

                // need to change the ctor for the Capability Type
                // TODO : Not a fan of this.
                if (uidValue.equals("55"))
                {
                    if (defaultValue != null)
                    {
                        defaultValue = removeFirstWord(defaultValue).replace("()", "");
                        defaultValue += " {}";
                    }
                }

                // If no default value provided use a default constructor
                if (defaultValue == null)
                {
                    pw.print(String.format("%s%s%s(%s())", indent, IVAR_PREFIX, anAttribute.getName(), typeName));
                }
                else
                {
                    // Convert EnumType::ENUM_VALUE to EnumType::enum_value
                    defaultValue = reformatEnumValue(defaultValue);
                    
                    pw.print(String.format("%s%s%s(%s)", indent, IVAR_PREFIX, anAttribute.getName(), defaultValue));
                }
                attributeCount--;
                if(attributeCount != 0)
                {
                    pw.println(", "); // Every initiailizer list element should have a following comma except the last
                }
            }
            // if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD)
            // {
            //     pw.println("/// Need to initialize this bitfield");
            //     pw.print(String.format("%s%s%s(%s())", indent, IVAR_PREFIX, anAttribute.getName(), anAttribute.getType()));
            //     attributeCount--;
            //     if(attributeCount != 0)
            //     {
            //         pw.println(", "); // Every initiailizer list element should have a following comma except the last
            //     }
            // }
            
        } // end of loop through attributes
    
    pw.println("\n{");

        // Set initial values
        List inits = aClass.getInitialValues();
        for(int idx = 0; idx < inits.size(); idx++)
        {
            GeneratedInitialValue anInitialValue = (GeneratedInitialValue)inits.get(idx);
            String setterName = anInitialValue.getSetterMethodName();

            // For simplicity if this is the setPduType, lets modify the format, not sure the else - KH
            if (setterName.equals("setPduType")) {
                String setterValue = anInitialValue.getVariableValue();
                String enumName = setterValue.split("\\.")[1];
                enumName = enumName.toLowerCase();
                pw.println(String.format("    %s(%s::DisPduType::%s);\n", setterName, enumNamespace, enumName));
            }
            else if (setterName.equals("setProtocolFamily")) {
                String setterValue = anInitialValue.getVariableValue();
                String enumName = setterValue.split("\\.")[1];
                enumName = enumName.toLowerCase();
                pw.println(String.format("    %s(%s::DISProtocolFamily::%s);\n", setterName, enumNamespace, enumName));
            }
            else {
                pw.println("/// KH - THIS MIGHT BE ANOTHER SMELLY TYPE, like protocol family\n");
                pw.println("    " + setterName + "( " + anInitialValue.getVariableValue() + " );");
            }
            
        }
    
       for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
       {
          GeneratedClassAttribute attribute = aClass.getClassAttributes().get(idx);

          // We need to initialize primitive array types
          if(attribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
          {
            

              pw.println("     // Initialize fixed length array");
              int arrayLength = attribute.getListLength();
              String indexName = "length" + attribute.getName();
              pw.println("     for(int " + indexName + "= 0; " + indexName + " < " + arrayLength + "; " + indexName + "++)");
              pw.println("     {");
              pw.println("         " + IVAR_PREFIX + attribute.getName() + "[" + indexName + "] = 0;");
              pw.println("     }");
              pw.println();
          }
       }
    
    pw.println("}\n");
}

/**
 * Generate a destructor method, which deallocates objects
 */
private void writeDtor(PrintWriter pw, GeneratedClass aClass)
{
    pw.println(aClass.getName() + "::~" + aClass.getName() + "()");
    pw.println("{");
    
    for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
    {
        GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
        
        // We need to deallocate ivars that are objects....
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
        { 
           pw.println("    " +  IVAR_PREFIX + anAttribute.getName() + ".clear();");
        } // end of if object
    } // end of loop through attributes
    
    pw.println("}\n");
}

private void writeGetterMethod(PrintWriter pw, GeneratedClass aClass, GeneratedClassAttribute anAttribute)
{
    if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
    { 
        pw.println(types.get(anAttribute.getType()) + " " + aClass.getName()  +"::" + "get" + this.initialCapital(anAttribute.getName()) + "() const");
        pw.println("{");
        if(anAttribute.getIsDynamicListLengthField() == false)
        {
            pw.println("    return " +  IVAR_PREFIX + anAttribute.getName() + ";");
        }
        else
        {
            GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();
            pw.println( "   return " +  IVAR_PREFIX + listAttribute.getName() + ".size();");
        }
        
        pw.println("}\n");
    }
    
    if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
    { 
        pw.println(anAttribute.getType() + "& " + aClass.getName()  +"::" + "get" + this.initialCapital(anAttribute.getName()) + "() ");
        pw.println("{");
        pw.println("    return " +  IVAR_PREFIX + anAttribute.getName() + ";");
        pw.println("}\n");
        
        pw.println("const " + anAttribute.getType() + "& " + aClass.getName()  +"::" + "get" + this.initialCapital(anAttribute.getName()) + "() const");
        pw.println("{");
        pw.println("    return " +  IVAR_PREFIX + anAttribute.getName() + ";");
        pw.println("}\n");
    }
    
    if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
    { 
        pw.println(this.getArrayType(anAttribute.getType()) + "* " + aClass.getName()  +"::" + "get" + this.initialCapital(anAttribute.getName()) + "() ");
        pw.println("{");
        pw.println("    return " +  IVAR_PREFIX + anAttribute.getName() + ";");
        pw.println("}\n");
        
        pw.println("const " + this.getArrayType(anAttribute.getType()) + "* " + aClass.getName()  +"::" + "get" + this.initialCapital(anAttribute.getName()) + "() const");
        pw.println("{");
        pw.println("    return " +  IVAR_PREFIX + anAttribute.getName() + ";");
        pw.println("}\n");
        
    }
    
    
    if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
    { 
        pw.println("std::vector<" + anAttribute.getType() + ">& " + aClass.getName()  +"::" + "get" + this.initialCapital(anAttribute.getName()) + "() ");
        pw.println("{");
        pw.println("    return " + IVAR_PREFIX +  anAttribute.getName() + ";");
        pw.println("}\n");
        
        pw.println("const std::vector<" + anAttribute.getType() + ">& " + aClass.getName()  +"::" + "get" + this.initialCapital(anAttribute.getName()) + "() const");
        pw.println("{");
        pw.println("    return " +  IVAR_PREFIX + anAttribute.getName() + ";");
        pw.println("}\n");
    }
    
    if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
    {
        String typeName = anAttribute.getType();
        String uidValue = anAttribute.getUnderlyingUid();

        if (!uidValue.isEmpty())
        {
            String aliasName = uid2ClassName.getProperty(uidValue);
            if (aliasName != null && !aliasName.isEmpty()) typeName = aliasName;
        }

        String attributeCast = anAttribute.getName();
        if (outputCastEnums.equals("true"))
        {
            typeName = "uint16_t";
            attributeCast = "static_cast<uint16_t>(" + anAttribute.getName() + ")";
        }

        pw.println(typeName + " " + aClass.getName()  +"::" + "get" + this.initialCapital(anAttribute.getName()) + "() const");
        pw.println("{");
        if(anAttribute.getIsDynamicListLengthField() == false)
        {
            pw.println("    return " +  IVAR_PREFIX + attributeCast + ";");
        }
        else
        {
            GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();
            String listAttributeCast = listAttribute.getName();
            if (outputCastEnums.equals("true"))
            {
                 listAttributeCast = "static_cast<uint16_t>(" + anAttribute.getName();
            }
            pw.println( "   return " +  IVAR_PREFIX + listAttributeCast + ".size();");
        }

        pw.println("}\n");
    }
    //pw.println(aClass.getName() + "::get" + aClass.getName() + "()");
   //pw.println("{");
    
}

    /**
     * write out setter method
     * @param pw PrintWriter
     * @param aClass GeneratedClass
     * @param anAttribute a GeneratedClassAttribute
     */
    public void writeSetterMethod(PrintWriter pw, GeneratedClass aClass, GeneratedClassAttribute anAttribute)
{
    if((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE) && (anAttribute.getIsDynamicListLengthField() == false))
    { 
        pw.println("void " + aClass.getName()  + "::" + "set" + this.initialCapital(anAttribute.getName()) + "(" + types.get(anAttribute.getType()) + " pX)");
        pw.println("{");
        if(!anAttribute.getIsDynamicListLengthField())
            pw.println( "    " +  IVAR_PREFIX + anAttribute.getName() + " = pX;");
        
        pw.println("}\n");
    }
    
    if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
    { 
        pw.println("void " + aClass.getName()  + "::" + "set" + this.initialCapital(anAttribute.getName()) + "(const " + anAttribute.getType() + " &pX)");
        pw.println("{");
        pw.println( "    " +  IVAR_PREFIX + anAttribute.getName() + " = pX;");
        pw.println("}\n");
    }
    
    if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
    {
        pw.println("void " + aClass.getName()  + "::" + "set" + this.initialCapital(anAttribute.getName()) + "(const " + this.getArrayType(anAttribute.getType()) + "* x)");
        pw.println("{");

        pw.println(String.format("   std::uint8_t listLength = sizeof(%s)/sizeof(%s);", anAttribute.getName(), this.getArrayType(anAttribute.getType())));
        
        // The safest way to handle this is to set up a loop and individually copy over the array in a for loop. This makes
        // primitives and objects handling orthogonal, vs. doing a memcpy, which is faster but may or may not work.

        if (anAttribute.getCountFieldName() != null)
        {
            pw.println("   for(int i = 0; i < " + anAttribute.getCountFieldName() + "; i++)");
        }
        else
        {
            pw.println("   for(int i = 0; i < " + anAttribute.getListLength() + "; i++)");
        }

        pw.println("   {");
        pw.println("        " +  IVAR_PREFIX + anAttribute.getName() + "[i] = x[i];");
        pw.println("   }");
        pw.println("}\n");
        
        // An alternative that is c-string friendly
        
        if(anAttribute.getCouldBeString() == true)
        {
            pw.println("// An alternate method to set the value if this could be a string. This is not strictly comnpliant with the DIS standard.");
            pw.println("void " + aClass.getName()  + "::" + "setByString" + this.initialCapital(anAttribute.getName()) + "(const " + this.getArrayType(anAttribute.getType()) + "* x)");
            pw.println("{");
            pw.println("   memcpy(" + IVAR_PREFIX + anAttribute.getName() + ", x, " + anAttribute.getListLength() + "-1);");
            pw.println("   " + IVAR_PREFIX + anAttribute.getName() + "[" + anAttribute.getListLength() + " -1] = '\\0';");
            pw.println("}");
            pw.println();
        }
    }
    
    
    if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
    { 
        pw.println("void " + aClass.getName()  + "::" + "set" + this.initialCapital(anAttribute.getName()) + "(const std::vector<" + anAttribute.getType() + ">& pX)");
        pw.println("{");
        pw.println( "     " +  IVAR_PREFIX + anAttribute.getName() + " = pX;");
        pw.println("}\n");
    }

    if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
    {
        String typeName = anAttribute.getType();
        String uidValue = anAttribute.getUnderlyingUid();

        if (!uidValue.isEmpty())
        {
            String aliasName = uid2ClassName.getProperty(uidValue);
            if (aliasName != null && !aliasName.isEmpty()) typeName = aliasName;
        }

        // write both setters, using an enum and a uint16_t
        pw.println("void " + aClass.getName()  + "::" + "set" + this.initialCapital(anAttribute.getName()) + "(const " + typeName + " pX)");
        
        pw.println("{");
        if(!anAttribute.getIsDynamicListLengthField())
            pw.println( "    " +  IVAR_PREFIX + anAttribute.getName() + " = pX;");
        pw.println("}\n");

        if (outputCastEnums.equals("true"))
        {
            pw.println("void " + aClass.getName()  + "::" + "set" + this.initialCapital(anAttribute.getName()) + "(const uint16_t pX)");
            pw.println("{");
            if(!anAttribute.getIsDynamicListLengthField())
                pw.println( "    " +  IVAR_PREFIX + anAttribute.getName() + " = static_cast<" + typeName + ">(pX);");
            pw.println("}\n");
        }

    }
}



/** output PDU into a string
 * @param pw PrintWriter to use
 * @param aClass a GeneratedClass */
public void writeToSringMethod(PrintWriter pw, GeneratedClass aClass)
{
    List ivars = aClass.getClassAttributes();
    
    // Generate a getMarshalledLength() method header
    // pw.println();
    pw.println("std::string " + aClass.getName()  + "::" + "to_string() const");
    pw.println("{");
    pw.println();

    pw.println("    std::string outputString;");
    // pw.println("    outputString += \"\\n\";\n");

    // Size of superclass is the starting point
    if(!aClass.getParentClass().equalsIgnoreCase("root"))
    {
        pw.println(String.format("    outputString += \"%s : \" + %s::to_string();",
                aClass.getParentClass(), aClass.getParentClass()));
    }
    
    for(int idx = 0; idx < ivars.size(); idx++)
    {
        GeneratedClassAttribute anAttribute = (GeneratedClassAttribute)ivars.get(idx);
 pw.println("// " + anAttribute.getType() + " : " + anAttribute.getAttributeKind());

        if((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
           (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD))
        {

            pw.print("    outputString += ");
            pw.println(String.format("\"%s : \"  + %s::to_string(%s) + \"\\n\";", 
                anAttribute.getType(), enumNamespace, anAttribute.getName()));
        }

        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
        {
            pw.print("    outputString += ");
            pw.println(String.format("\"%s : \"  + std::to_string(%s) + \"\\n\";", 
                anAttribute.getName(), anAttribute.getName()));
            // pw.println(primitiveSizes.get(anAttribute.getType()) + ";  // " + IVAR_PREFIX + anAttribute.getName());
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
        {
            pw.print("    outputString += ");
            
            pw.println(String.format(
                "\"%s : \" + %s%s.to_string() + \"\\n\";",
                anAttribute.getType(),
                IVAR_PREFIX, anAttribute.getName()
            ));
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
        {               
            if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
            {
                pw.println(String.format("    outputString += std::string(\"%s : \") + \"\\n\";", anAttribute.getName()));

                if(anAttribute.getCountFieldName() != null)
                    pw.println(String.format("    for(int idx=0; idx < %s; idx++)", anAttribute.getCountFieldName()));
                else
                    pw.println(String.format("    for(int idx=0; idx < %s; idx++)", anAttribute.getListLength()));

                pw.println("    {");
                pw.println(String.format("        outputString += std::to_string(idx) + \" : \" + std::to_string(%s%s[idx]) + \"\\n\";", IVAR_PREFIX, anAttribute.getName()));
                pw.println("    }");
            }
            else
            {                
                pw.println(" THIS IS A CONDITION NOT HANDLED BY XMLPG: a fixed list array of objects. That's  why you got the compile error.");
            }
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
        {            
            // // If this is a dynamic list of primitives, it's the list size times the size of the primitive.
            if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
            {
                pw.println("// TODO : Primitive type in OBJECT LIST");
                pw.println("// " + anAttribute.getType() + " : " + anAttribute.getAttributeKind());

            //     pw.println( anAttribute.getName() + ".size() " + " * " + primitiveSizes.get(anAttribute.getType()) + ";  // " + IVAR_PREFIX + anAttribute.getName());
            }
            else
            {
                pw.println(String.format("    for(int idx=0; idx < %s; idx++)", anAttribute.getListLength()));
                pw.println("    {");
                if ( (anAttribute.underlyingTypeIsEnum) || (anAttribute.isBitField))
                {
                    pw.println("// " + anAttribute.getType() + " : " + anAttribute.getAttributeKind());
                    pw.println(String.format("        outputString += %s::to_string(%s%s[idx]);", enumNamespace, IVAR_PREFIX, anAttribute.getName()));
                }
                else
                {
                    pw.println(String.format("        outputString += %s%s[idx].to_string();", IVAR_PREFIX, anAttribute.getName()));
                }
                pw.println("    }");
            }
        }
        
    }
    pw.println("    return outputString;");
    pw.println("}");
    pw.println();

}

/** output marshalled size
 * @param pw PrintWriter to use
 * @param aClass a GeneratedClass */
public void writeGetMarshalledSizeMethod(PrintWriter pw, GeneratedClass aClass)
{
    List ivars = aClass.getClassAttributes();
    
    // Generate a getMarshalledLength() method header
    // pw.println();
    pw.println("int " + aClass.getName()  + "::" + "get_marshaled_size() const");
    pw.println("{");
    pw.println("   int marshalSize = 0;");
    pw.println();

    // Size of superclass is the starting point
    if(!aClass.getParentClass().equalsIgnoreCase("root"))
    {
        pw.println("   marshalSize = " + aClass.getParentClass() + "::get_marshaled_size();");
    }
    
    for(int idx = 0; idx < ivars.size(); idx++)
    {
        GeneratedClassAttribute anAttribute = (GeneratedClassAttribute)ivars.get(idx);
    
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
        {
            pw.print("   marshalSize = marshalSize + ");
            pw.println(enumNamespace + "::get_marshaled_size(" + anAttribute.getName() + ");");
        }

        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
        {
            pw.print("   marshalSize = marshalSize + ");
            if (primitiveSizes.get(anAttribute.getType()) == null)
            {
pw.println("");
pw.println("/// FAIL");
pw.println("/// Attribute name         : " + anAttribute.getName());
pw.println("/// Attribute Type         : " + anAttribute.getType());
            }
            pw.println(primitiveSizes.get(anAttribute.getType()) + ";  // " + IVAR_PREFIX + anAttribute.getName());
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
        {
            pw.print("   marshalSize = marshalSize + ");
            pw.println(IVAR_PREFIX + anAttribute.getName() + ".get_marshaled_size();");
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
        {
            pw.print("   marshalSize = marshalSize + ");
            // If this is a fixed list of primitives, it's the list size times the size of the primitive.
            if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
            {
                pw.println( anAttribute.getListLength() + " * " + primitiveSizes.get(anAttribute.getType()) + ";  // " + IVAR_PREFIX + anAttribute.getName());
            }
            else
            {
                //pw.println( anAttribute.getListLength() + " * " +  " new " + anAttribute.getType() + "().getMarshalledSize()"  + ";  // " + anAttribute.getName());
                pw.println(" THIS IS A CONDITION NOT HANDLED BY XMLPG: a fixed list array of objects. That's  why you got the compile error.");
            }
        }
        
        if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
        {
            // If this is a dynamic list of primitives, it's the list size times the size of the primitive.
            if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
            {
                pw.println( anAttribute.getName() + ".size() " + " * " + primitiveSizes.get(anAttribute.getType()) + ";  // " + IVAR_PREFIX + anAttribute.getName());
            }
            else
            {
                pw.println();
                pw.println("   for(int idx=0; idx < " + IVAR_PREFIX + anAttribute.getName() + ".size(); idx++)");
                pw.println("   {");
                //pw.println( anAttribute.getName() + ".size() " + " * " +  " new " + anAttribute.getType() + "().getMarshalledSize()"  + ";  // " + anAttribute.getName());
                pw.println("        " + anAttribute.getType() + " listElement = " + IVAR_PREFIX + anAttribute.getName() + "[idx];");
                if ( (anAttribute.underlyingTypeIsEnum) || (anAttribute.isBitField))
                    pw.println("        marshalSize = marshalSize + " + enumNamespace + "::get_marshaled_size(listElement);");
                else
                pw.println("        marshalSize = marshalSize + listElement.get_marshaled_size();");
                pw.println("    }");
                pw.println();
            }
        }
        
    }
    pw.println("    return marshalSize;");
    pw.println("}");
    pw.println();
}
    
/** 
* returns a string with the first letter capitalized. 
* @param aString of interest
* @return same string with first letter capitalized
*/
    @Override
    public String initialCapital(String aString)
{
    StringBuffer stb = new StringBuffer(aString);
    stb.setCharAt(0, Character.toUpperCase(aString.charAt(0)));
    
    return new String(stb);
}

/**
 * Returns true if this class consists only of instance variables that are
 * primitives, such as short, int, etc. Things that are not allowed include
 * ivars that are classes, arrays, or variable length lists. If a class
 * contains any of these, false is returned.
 */
private boolean classHasOnlyPrimitives(GeneratedClass aClass)
{
    boolean isAllPrimitive = true;
    
    // Flip flag to false if anything is not a primitive.
    for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
    {
        GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
        if(anAttribute.getAttributeKind() != GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
        {
            isAllPrimitive = false;
            System.out.println("Not primitive for class " + aClass.getName() + " and attribute " + anAttribute.getName() + " " + anAttribute.getAttributeKind());
        }
    }
    
    return isAllPrimitive;
}

/**
 * Some code to figure out the characters to use for array types. We may have arrays of either primitives
 * or classes; this figures out which it is and returns the right string.
 */
private String getArrayType(String xmlType)
{
    String marshalType = marshalTypes.getProperty(xmlType);
    
    if(marshalType == null) // It's a class
    {
        return xmlType;
    }
    else // It's a primitive
    {
        return marshalType;
    }
    
}
                       
private void writeLicenseNotice(PrintWriter pw)
{
        pw.println("// Copyright (c) 1995-2022 held by the author(s).  All rights reserved.");
       
        pw.println("// Redistribution and use in source and binary forms, with or without");
        pw.println("// modification, are permitted provided that the following conditions");
        pw.println("//  are met:");
        pw.println("// ");
        pw.println("//  * Redistributions of source code must retain the above copyright");
        pw.println("// notice, this list of conditions and the following disclaimer.");
        pw.println("// * Redistributions in binary form must reproduce the above copyright");
        pw.println("// notice, this list of conditions and the following disclaimer");
        pw.println("// in the documentation and/or other materials provided with the");
        pw.println("// distribution.");
        pw.println("// * Neither the names of the Naval Postgraduate School (NPS)");
        pw.println("//  Modeling Virtual Environments and Simulation (MOVES) Institute");
        pw.println("// (http://www.nps.edu and http://www.MovesInstitute.org)");
        pw.println("// nor the names of its contributors may be used to endorse or");
        pw.println("//  promote products derived from this software without specific");
        pw.println("// prior written permission.");
        pw.println("// ");
        pw.println("// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS");
        pw.println("// AS IS AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT");
        pw.println("// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS");
        pw.println("// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE");
        pw.println("// COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,");
        pw.println("// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,");
        pw.println("// BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;");
        pw.println("// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER");
        pw.println("// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT");
        pw.println("// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN");
        pw.println("// ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE");
        pw.println("// POSSIBILITY OF SUCH DAMAGE.");
                       
}


}
