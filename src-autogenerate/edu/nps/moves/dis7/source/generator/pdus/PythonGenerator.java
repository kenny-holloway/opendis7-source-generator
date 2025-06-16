/**
 * Copyright (c) 2008-2025, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */
package edu.nps.moves.dis7.source.generator.pdus;

import edu.nps.moves.dis7.source.generator.enumerations.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/* not thouroughly examined, global change: VARIABLE_LIST to OBJECT_LIST and FIXED_LIST to PRIMITIVE_LIST */
/**
 * This class autogenerates Python source code from XML PDU definitions, specifically 
 * producing most source code needed for the open-dis7-python distribution.
 * TODO see JavaGenerator for related functionality that will be needed in Python.
 * Constructor, not fully implemented.
 * @see AbstractGenerator
 * @see JavaGenerator
 * @author DMcG
 */


public class PythonGenerator extends AbstractGenerator
{
    public static final String IVAR_PREFIX ="";

    /** Standard python indent is four spaces */
    public String INDENT="    ";
    /** Properties of interest */
    public Properties   marshalTypes = new Properties();
    /** Properties of interest */
    public Properties unmarshalTypes = new Properties();

    String globalNamespace = "";
    String enumNamespace = "";
    String enumNamespacePath = "";
    String debugAttributes = "";

/**
 * Given the input object, something of an abstract syntax tree, this generates a source code file in the Python language.It has ivars, getters, setters, and serialization/deserialization methods.Warning: only partially implemented.
 * @author DMcG
     * @param pClassDescriptions String Map of class descriptions
     * @param languagePropertiesPython special language properties
 */
    public PythonGenerator(Map<String, GeneratedClass> pClassDescriptions, Properties languagePropertiesPython)
    {
        super(pClassDescriptions, languagePropertiesPython);
        
        Properties systemProperties = System.getProperties();
        String clDirectory = systemProperties.getProperty("xmlpg.generatedSourceDir");

        // Directory to place generated source code
        if(clDirectory != null)
            languagePropertiesPython.setProperty("directory", clDirectory);

        globalNamespace = systemProperties.getProperty("xmlpg.namespace");
        if (globalNamespace != null)
            languagePropertiesPython.setProperty("namespace", globalNamespace);
        else
            globalNamespace = "";

        // set global namespace for enums
        enumNamespace = systemProperties.getProperty("xmlpg.enumNamespace");
        if (enumNamespace != null)
        {
            languagePropertiesPython.setProperty("enumNamespace", enumNamespace);
            System.out.println("Setting the enum path");
            System.out.println(enumNamespacePath);
            enumNamespacePath = enumNamespace.replaceAll("\\.", "/");
            System.out.println(enumNamespacePath);
        }
        else
        {
            enumNamespace = "";
        }

        debugAttributes = systemProperties.getProperty("xmlpg.debugatrributes");
        if (debugAttributes == null)
            debugAttributes = "";

        super.setGeneratedSourceDirectoryName(languagePropertiesPython.getProperty("directory"));

        /** Maps the primitive types listed in the XML file to the python types */

        // Set up the mapping between Open-DIS primitive types (key) and marshal types in Python (value).
        
        // Set up the mapping between Open-DIS primitive types and marshal types.       
        marshalTypes.setProperty("uint8",   "byte");
        marshalTypes.setProperty("uint16",  "short");
        marshalTypes.setProperty("uint32",  "int");
        marshalTypes.setProperty("uint64",  "long");
        marshalTypes.setProperty("int8",    "byte");
        marshalTypes.setProperty("int16",   "short");
        marshalTypes.setProperty("int32",   "int");
        marshalTypes.setProperty("int64",   "long");
        marshalTypes.setProperty("float32", "float");
        marshalTypes.setProperty("float64", "double");
        //marshalTypes.setProperty("utf","EntityID");

        // Unmarshalling types
        //unmarshalTypes.setProperty("EntityID","utf");
        unmarshalTypes.setProperty("uint8",   "unsigned_byte");
        unmarshalTypes.setProperty("uint16",  "unsigned_short");
        unmarshalTypes.setProperty("uint32",  "int");
        unmarshalTypes.setProperty("uint64",  "long");
        unmarshalTypes.setProperty("int8",    "byte");
        unmarshalTypes.setProperty("int16",   "short");
        unmarshalTypes.setProperty("int32",   "int");
        unmarshalTypes.setProperty("int64",   "long");
        unmarshalTypes.setProperty("float32", "float");
        unmarshalTypes.setProperty("float64", "double");

        GenerateTypesFile();
        
    }

    private void GenerateTypesFile()
    {
        String templateFileName = "../pdus/dis7types.txt";
        String typesFileName = getGeneratedSourceDirectoryName() + "dis7types.py";
        String typesData = "";

        try {
            typesData = new String(Files.readAllBytes(Paths.get(getClass().getResource(templateFileName).toURI())), StandardCharsets.UTF_8.name());
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        try
        {            
            File outputFile = new File(typesFileName);
            outputFile.createNewFile();

            PrintWriter pw = new PrintWriter(outputFile);
            pw.print(typesData);
            pw.flush();
            pw.close();
        }
        catch(IOException e)
        {
            System.err.println("problem creating Dis Types file " + e);
        }

    }

    public Boolean stringIsBlank(String inputString)
    {
        Boolean result = false;
        if (inputString == null || inputString.isEmpty() || inputString.trim().isEmpty())
        {
            result = true;
        }
        return result;
    }

    private void writePackageFile()
    {
        String namespace = languageProperties.getProperty("namespace");
        String packageFilename = getGeneratedSourceDirectoryName() + "/__init__.py";

        PrintStringBuffer  printBuffer = new PrintStringBuffer();

        printBuffer.println("import sys");
        printBuffer.println();
        printBuffer.println("sys.path.append('" + namespace + "')");
        printBuffer.println("sys.path.append('" + namespace + "/utils')");
        printBuffer.println("sys.path.append('" + namespace + "/siso_ref_010/enums')");
        printBuffer.println();

        List sortedClasses =  this.sortClasses();
        Collections.sort(sortedClasses);

        Iterator it = sortedClasses.iterator();

        while(it.hasNext())
        {
            GeneratedClass aClass = (GeneratedClass)it.next();
            String name = aClass.getName();
            String pythonNaming = StringUtils.camelCasetoLowerUnderscore(name);

            printBuffer.println("from " + namespace + "." + pythonNaming + " import " + name);
            
        }

        System.out.println("Generating package file : " + packageFilename);

        try
        {
            File outputFile = new File(packageFilename);
            outputFile.createNewFile();

            PrintWriter pw = new PrintWriter(outputFile);
            pw.print(printBuffer.toString());
            pw.flush();
            pw.close();
        }
        catch(IOException e)
        {
            System.err.println("problem creating package file " + e);
        }

    }

    @Override
    public void writeClasses()
    {
        List sortedClasses =  this.sortClasses(); // TODO empty
        // generatedSourceDirectoryName = "./src/python";
       
        // somewhat duplicative of code that follows, TODO refactor each
        createGeneratedSourceDirectory(false); // boolean: whether to clean out prior files, if any exist in that directory
        
        // PrintWriter pw;

        writePackageFile();
       
        try
        {
/*
            // *** TODO missing languageProperties! ***
            // Create the new, empty file, and create printwriter object for output to it
            String outputFileName = "opendis7.py"; // default filename from prior open-dis-python implementation
//            if (!languageProperties.getProperty("filename").isBlank())
//                 outputFileName = languageProperties.getProperty("filename");
            String directoryName = this.generatedSourceDirectoryName; // default
//            if (!languageProperties.getProperty("directory").isBlank())
//                 directoryName = languageProperties.getProperty("directory");
            if (!stringIsBlank(directoryName) && !stringIsBlank(outputFileName))
                System.out.println("putting network code in " + directoryName + "/" + outputFileName);
            else
                System.out.println("problem with output file directory/name ...");
            
            File outputFile = new File(directoryName + "/" + outputFileName); // just creates object...
            if (!outputFile.getParentFile().exists()) // watch out, don't wipe out other contents in this directory
                 outputFile.getParentFile().mkdirs(); // superfluous, already handled by createGeneratedSourceDirectory() above
            outputFile.createNewFile(); // now creates file
            pw = new PrintWriter(outputFile);
            this.writeLicense(pw);
            pw.println();
            
            pw.println("import sys, os");
            pw.println("sys.path.append(os.path.join(os.path.dirname(sys.path[0]), 'utils'))");
            
            pw.println("sys.path.append(os.path.join(os.path.dirname(sys.path[0]), '" + enumNamespacePath +"'))");

            pw.println("");
            pw.println("import DataInputStream");
            pw.println("import DataOutputStream");
            pw.println();
*/            
            
            System.out.println("number of classes: " + sortedClasses.size());
            Iterator it = sortedClasses.iterator();
            
            // // for now all classes in a single file, all imports must be pre-processed
            // // I don't like it any more than you do
            // PrintStringBuffer  printBuffer = new PrintStringBuffer();
            // while(it.hasNext())
            // {                    
                    
            //     GeneratedClass aClass = (GeneratedClass)it.next();
            //     String name = aClass.getName();

            //     writeUsingStatements(printBuffer, aClass);
            // }
            // pw.print(printBuffer.toString());

            String namespace = languageProperties.getProperty("namespace");
            String fullPath;

            // reset the iteratror, lets do it all again
            it = sortedClasses.iterator();
            while(it.hasNext())
            {                        

                GeneratedClass aClass = (GeneratedClass)it.next();
                String name = aClass.getName();

                String pythonNaming = StringUtils.camelCasetoLowerUnderscore(name);

                fullPath = getGeneratedSourceDirectoryName() + "/" + pythonNaming + ".py";
                File outputFile = new File(fullPath);
                outputFile.createNewFile();

                try (PrintWriter pw = new PrintWriter(outputFile))
                {
                    System.out.println("creating python class " + fullPath);
                    // print the source code of the class to the file

                    PrintStringBuffer  printBuffer = new PrintStringBuffer();
                    writeUsingStatements(printBuffer, aClass);
                    pw.print(printBuffer.toString());

                    this.writeClass(pw, aClass);

                    pw.flush();
                    pw.close();
                }
                
            }

        }
        catch(IOException e)
        {
            System.err.println("problem creating class " + e);
        }
 
   } // end of writeClasses
    
    private void writePaddingAttribute(PrintWriter printWriter, GeneratedClassAttribute anAttribute, String tabIndent)
    {
        String attributeName = anAttribute.getName();
        String bitCount = String.format("%s", anAttribute.getAttributeKind());
        GeneratedClassAttribute.ClassAttributeType attrType = anAttribute.getAttributeKind();

        bitCount = bitCount.substring(bitCount.length() - 2);

        String outputString = "";
        outputString = String.format(tabIndent + "%s: uint8 = []", attributeName);
        printWriter.print(outputString);

    }

    private void writePrimitiveAttribute(PrintWriter printWriter, GeneratedClassAttribute anAttribute, String tabIndent)
    {
        String outputString = "";
        String defaultValue = anAttribute.getDefaultValue();

        if(defaultValue == null)
        {
            if (anAttribute.getType().toLowerCase().contains("float"))
                defaultValue = "0.0";
            else
                defaultValue = "0";
        }

        // If we're a normal primitivetype, initialize directly; Otherwise, we need a get for this
        // and a property type, where the get returns the list size
        // if(anAttribute.getIsDynamicListLengthField() == true)
        // {
        //     GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();
        //     printWriter.println(INDENT + INDENT  + "self._" + anAttribute.getName() + " = " + defaultValue);

        //     String attrName = anAttribute.getName();

        //     String getOper = String.format("        def get_%s(self):\n", attrName)
        //              .concat(String.format("            return len(self._%s)\n", listAttribute.getName()))
        //              .concat(String.format("        def set_%s(self, value):\n", attrName))
        //              .concat(String.format("            self._%s = value\n", attrName))
        //              .concat(String.format("        %s = property(get_%s, set_%s)\n", attrName, attrName, attrName))
        //              .concat("\n");
        //     printWriter.println(getOper);
        // }
        // else
        // {

        
        // We do not output these, they will have a property defined later
        if(anAttribute.getIsDynamicListLengthField() == true)
            outputString = "#";
        
        outputString += String.format("%s: %s = %s", anAttribute.getName(), anAttribute.getType(), defaultValue);
        // printWriter.println(INDENT + INDENT  + "self." + anAttribute.getName() + " = " + defaultValue);
        // }

        printWriter.print(tabIndent + outputString);
    }

    private void writeEnumAttribute(PrintWriter printWriter, GeneratedClassAttribute anAttribute, String tabIndent)
    {
        String uidValue = anAttribute.getUnderlyingUid();
        String defaultValue = anAttribute.getDefaultValue();
        String typeName = anAttribute.getType();

        // normalize the default value
        if (defaultValue != null)
        {
            String defaultValueTokens[] = defaultValue.split("\\.");
            if (defaultValueTokens[1] != null)
            {
                defaultValue = defaultValueTokens[0] + "." + defaultValueTokens[1].toLowerCase();
            }
        }

        String outputStatement = "";
        if (anAttribute.getDefaultValue() == null)
        {
            outputStatement = String.format("%s%s: \"%s | None\" = %s.default", tabIndent,  IVAR_PREFIX + anAttribute.getName(), typeName, typeName);
           // printWriter.println(String.format("%sself.%s = %s.default\n", tabIndent, IVAR_PREFIX + anAttribute.getName(), typeName));
        }
        else
        {
             // printWriter.println(tabIndent + "self." + anAttribute.getName() + " = " + defaultValue + "\n");
             outputStatement = String.format("%s%s: \"%s | None\" = %s", tabIndent,  IVAR_PREFIX + anAttribute.getName(), typeName, defaultValue);
        }

        // String attrComment = "";
        // if ((anAttribute.getComment() != null) && !anAttribute.getComment().trim().isEmpty())
        // {
        //     attrComment = " # " + anAttribute.getComment();
        // }
        // else 
        // {
        //     attrComment = "# " + IVAR_PREFIX + anAttribute.getName() + " is an undescribed parameter... ";
        //     // printWriter.println(tabIndent + "# " + IVAR_PREFIX + anAttribute.getName() + " is an undescribed parameter... ");
        // }

        // outputStatement += attrComment;

        printWriter.print(outputStatement);

    }

    private void writeSisoBitfieldAttribute(PrintWriter printWriter, GeneratedClassAttribute anAttribute, String tabIndent)
    {
        String uidValue = anAttribute.getUnderlyingUid();
        String defaultValue = anAttribute.getDefaultValue();
        String typeName = anAttribute.getType();
        String aliasName = uid2ClassName.getProperty(uidValue);

        if (aliasName != null && !aliasName.isEmpty() && !typeName.equals(aliasName)) typeName = aliasName;        

        // need to change the initializer for the Capability Type

        // TODO : Not a fan of this.
        if (uidValue.equals("55"))
        {
            if (defaultValue != null)
            {
                defaultValue = removeFirstWord(defaultValue).replace("()", "");
                // defaultValue += " {}";
            }
        }

        // printWriter.println("# Default : " + defaultValue);
        // if (defaultValue != null)
        // {
        //     String defaultValueTokens[] = defaultValue.split("\\.");
        //     if (defaultValueTokens[1] != null)
        //     {
        //         defaultValue = defaultValueTokens[0] + "." + defaultValueTokens[1].toLowerCase();
        //     }
        // }
        // printWriter.println("# Default : " + defaultValue);
        // if (aliasName != null && !aliasName.isEmpty() && !typeName.equals(aliasName)) typeName = aliasName;

        String outputStatement = "";
        if (anAttribute.getDefaultValue() == null)
        {
            outputStatement =  String.format("%s = None", anAttribute.getName());
            // outputStatement =  String.format("self.%s = %s()\n",  anAttribute.getName(), typeName);
        }                
        else
        {
            outputStatement = String.format("%s: \"%s | None\" = %s()", IVAR_PREFIX + anAttribute.getName(),typeName, defaultValue);
            // if (aliasName.equals(typeName))
            //     printWriter.println(" # " + tabIndent + IVAR_PREFIX + anAttribute.getName() + " = " + defaultValue + ";\n");
            // else
            //     printWriter.println(" # " + tabIndent + IVAR_PREFIX + anAttribute.getName() + " = (" + aliasName + ")" + defaultValue + ";\n");
        }

        printWriter.print(tabIndent + outputStatement);
    }

    private void writeClassRefAttribute(PrintWriter printWriter, GeneratedClassAttribute anAttribute, String tabIndent)
    {
        String attributeType = anAttribute.getType();

        if (anAttribute.getDefaultValue() != null)
        {
            printWriter.println(tabIndent + "# TODO - Process the default value for a CLASSREF type");
        }
        
        String outputStatement = String.format("%s%s: \"%s | None\" = None",
        tabIndent, anAttribute.getName(), attributeType, attributeType);
        printWriter.print(outputStatement);

        // printWriter.println(String.format("%s%s:", tabIndent,  IVAR_PREFIX + anAttribute.getName());
        // printWriter.println(INDENT + INDENT  + anAttribute.getName() + ":")
        // printWriter.println(INDENT + INDENT  + "self." + anAttribute.getName() + " = " + attributeType + "()");
    }

    private void writePrimitiveListAttribute(PrintWriter printWriter, GeneratedClassAttribute anAttribute, String tabIndent)
    {
        String attributeType = anAttribute.getType();
        int listLength = anAttribute.getListLength();
        String listLengthString = "" + listLength;

        String uidValue = anAttribute.getUnderlyingUid();
        String typeName = anAttribute.getType();
        String aliasName = uid2ClassName.getProperty(uidValue);

        if (aliasName != null && !aliasName.isEmpty() && !typeName.equals(aliasName)) typeName = aliasName;

        String outputStatement = "";
        
        if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
        {
            outputStatement += String.format("%s = None", anAttribute.getName());
        }
        else
        {
            printWriter.println("TODO");
            printWriter.print(INDENT + INDENT + "self." + anAttribute.getName() + " =  " +
                            "[");
            for(int arrayLength = 0; arrayLength < anAttribute.getListLength(); arrayLength++)
            {
                printWriter.print(" " + attributeType + "()");
                if(arrayLength < anAttribute.getListLength() - 1)
                    printWriter.print(",");
            }
            printWriter.println("]");
        }

        printWriter.print(tabIndent + outputStatement);
    }

    private void writeObjectListAttribute(PrintWriter printWriter, GeneratedClassAttribute anAttribute, String tabIndent)
    {
        String attributeType = anAttribute.getType();
        int listLength = anAttribute.getListLength();
        String listLengthString = "" + listLength;

        String outputStatement = "";
        // outputStatement = String.format("%s: Optional[List[%s]] = None", anAttribute.getName(), anAttribute.getType());
        outputStatement = String.format("%s: List[%s] = None", anAttribute.getName(), anAttribute.getType());

        // printWriter.print(INDENT + INDENT + "self._" + anAttribute.getName() + " = []");

        printWriter.print(tabIndent + outputStatement);
    }

    public void writeClassParameters(PrintWriter printWriter, GeneratedClass aClass, String tabIndent)
    {
        List ivars = aClass.getClassAttributes();

        // For classes without attributes (abstract) close the init
        // if (ivars.size() == 0) printWriter.println("):");
        // else printWriter.println(",");

        for(int idx = 0; idx < ivars.size(); idx++)
        {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute)ivars.get(idx);
            String typeName = anAttribute.getType();
            String attributeName = anAttribute.getName();
            String uidValue = anAttribute.getUnderlyingUid();
            String aliasName = uid2ClassName.getProperty(uidValue);
            String defaultValue = anAttribute.getDefaultValue();

            if (aliasName != null && !aliasName.isEmpty() && !typeName.equals(aliasName)) typeName = aliasName;

            GeneratedClassAttribute.ClassAttributeType attrType = anAttribute.getAttributeKind();

            boolean isLastOne = idx == ivars.size() - 1;

            switch (attrType)
            {
                case PADTO16:
                case PADTO32:
                case PADTO64:
                    writePaddingAttribute(printWriter, anAttribute, tabIndent);
                    break;

                case PRIMITIVE:
                case STATIC_IVAR:
                    writePrimitiveAttribute(printWriter, anAttribute, tabIndent);
                    break;

                case SISO_ENUM:
                    writeEnumAttribute(printWriter, anAttribute, tabIndent);
                    break;

                case SISO_BITFIELD:
                    writeSisoBitfieldAttribute(printWriter, anAttribute, tabIndent);
                    break;

                case CLASSREF:
                    writeClassRefAttribute(printWriter, anAttribute, tabIndent);
                    break;

                case PRIMITIVE_LIST:
                     writePrimitiveListAttribute(printWriter, anAttribute, tabIndent);
                    break;

                case OBJECT_LIST:
                    writeObjectListAttribute(printWriter, anAttribute, tabIndent);
                    break;

            }

            if (isLastOne)
            {
                printWriter.println("):");
            }
            else
            {
                printWriter.println(",");
            }


        }


    }

    private void DebugPrintClassAttributes(PrintWriter printWriter, GeneratedClass aClass, String tabIndent)
    {

        // String commentString = String.format("%20s %-20s : %s", anAttribute.getName(), "(" + anAttribute.getType() + ")", attrComment);

        if (debugAttributes.equals("true"))
        {
            List ivars = aClass.getClassAttributes();

            printWriter.println("");
            printWriter.println(tabIndent + "\"\"\"");
            printWriter.println(tabIndent + "Attribute Types");
            printWriter.println(tabIndent + "Class " + aClass.getName() + " has " + ivars.size() + " Attributes");
            for (int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
            {
                GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
                printWriter.println(tabIndent + tabIndent + String.format("%-20s : %s", anAttribute.getName(), anAttribute.getAttributeKind()));
                // printWriter.println(tabIndent + "    ///    " +  anAttribute.getName() + "\t : " + anAttribute.getAttributeKind());;
            }
             printWriter.println(tabIndent + "\"\"\"");
             printWriter.println("");
        } 
    }

    public void writeClassParameterComments(PrintWriter printWriter, GeneratedClass aClass, String tabIndent)
    {
        printWriter.println();
        printWriter.println(tabIndent  + "\"\"\"");
        printWriter.println(tabIndent + aClass.getClassComments());
        printWriter.println();
        printWriter.println(tabIndent + "Args:");

        List ivars = aClass.getClassAttributes();
        for(int idx = 0; idx < ivars.size(); idx++)
        {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute)ivars.get(idx);

            String attrComment = "";
            if ((anAttribute.getComment() != null) && !anAttribute.getComment().trim().isEmpty())
            {
                attrComment = " # " + anAttribute.getComment();
            }
            else 
            {
                attrComment = "# " + IVAR_PREFIX + anAttribute.getName() + " is an undescribed parameter... ";
                // printWriter.println(tabIndent + "# " + IVAR_PREFIX + anAttribute.getName() + " is an undescribed parameter... ");
            }

            String commentString = String.format("%25s %-25s : %s", anAttribute.getName(), "(" + anAttribute.getType() + ")", attrComment);

            printWriter.println(tabIndent + tabIndent + commentString);

        }

        printWriter.println(tabIndent + "\"\"\"");

        DebugPrintClassAttributes(printWriter, aClass, tabIndent);

    }

    public void writeClassParameterAssignments(PrintWriter printWriter, GeneratedClass aClass, String tabIndent)
    {
        List ivars = aClass.getClassAttributes();
        
        for(int idx = 0; idx < ivars.size(); idx++)
        {

            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute)ivars.get(idx);
            GeneratedClassAttribute.ClassAttributeType attrType = anAttribute.getAttributeKind();

            String typeName = anAttribute.getType();
            String attributeName = anAttribute.getName();
            String uidValue = anAttribute.getUnderlyingUid();
            String aliasName = uid2ClassName.getProperty(uidValue);
            String defaultValue = anAttribute.getDefaultValue();
            
            if (aliasName != null && !aliasName.isEmpty() && !typeName.equals(aliasName)) typeName = aliasName;

            // DebugPrintAttribute(printWriter, anAttribute, tabIndent);

        //             if (defaultValue != null)
        // {
        //     String defaultValueTokens[] = defaultValue.split("\\.");
        //     if (defaultValueTokens[1] != null)
        //     {
        //         defaultValue = defaultValueTokens[0] + "." + defaultValueTokens[1].toLowerCase();
        //     }
        // }

            String outputStatement = "";
            
            switch (attrType)
            {
                case PRIMITIVE:
                    if(anAttribute.getIsDynamicListLengthField() != true)
                    {
                        outputStatement += String.format("self.%s = %s\n", attributeName, attributeName);
                    }
                    else outputStatement += "\n";
                    break;

                case SISO_BITFIELD:
                    {
                        StringBuilder commandSet = new StringBuilder();
                        commandSet.append(tabIndent + "\n# Put out the allocation of a bitfield type\n");
                        // commandSet.append("   name : " +attributeName + "\n");
                        // commandSet.append("  alias : " + aliasName + "\n");
                        // commandSet.append("default : " +defaultValue + "\n");
                        
                        commandSet.append(tabIndent + String.format("if %s is None:\n", attributeName));
                        commandSet.append(tabIndent + String.format("    self.%s = %s()\n", attributeName, typeName));
                        commandSet.append(tabIndent + String.format("else:\n", attributeName, attributeName));
                        commandSet.append(tabIndent + String.format("    self.%s = %s\n", attributeName, attributeName));
                        outputStatement += commandSet.toString();
                    }
                    break;

                case CLASSREF:
                    printWriter.println(tabIndent + "# TODO : process any default for CLASSREF");
                    outputStatement += String.format("self.%s = %s or %s()\n", attributeName, attributeName, typeName);
                    break;

                case PADTO16:
                case PADTO32:
                case PADTO64:
                    outputStatement += String.format("self.%s = %s\n", attributeName, attributeName);
                    outputStatement += tabIndent+ String.format("self.padToSize = %s\n", attrType);
                    break;

                case PRIMITIVE_LIST:
                    int listLength = anAttribute.getListLength();
                    outputStatement += String.format("self.%sListSize = %s\n",attributeName, listLength);

                    StringBuilder commandSet = new StringBuilder();
                    commandSet.append(tabIndent + String.format("if %s is None:\n", attributeName));
                    commandSet.append(tabIndent + String.format("    self.%s = [0 for _ in range(self.%sListSize)]\n", attributeName, attributeName));
                    commandSet.append(tabIndent + String.format("else:\n"));
                    commandSet.append(tabIndent + String.format("    self.%s = %s\n", attributeName, attributeName));
                    outputStatement += commandSet.toString();

                    // if characters is None:
                    //     self.characters = [0 for _ in range(self.characterSetSize)]
                    // else:
                    //     self.characters = characters 

                    break;

                case OBJECT_LIST:
                    StringBuilder objBuilder = new StringBuilder();
                    outputStatement += "\n";
                    outputStatement += tabIndent + String.format("if %s is None:\n", attributeName);
                    outputStatement += tabIndent + String.format("    self.%s = []\n", attributeName);
                    outputStatement += tabIndent + String.format("else:\n");
                    outputStatement += tabIndent + String.format("    self.%s = %s\n", attributeName, attributeName);

                    // objBuilder.append("\n");
                    // objBuilder.append(tabIndent + String.format("if %s is None:\n", attributeName));
                    // objBuilder.append(tabIndent + String.format("    self.%s = []\n", attributeName));
                    // objBuilder.append(tabIndent + String.format("else:\n"));
                    // objBuilder.append(tabIndent + String.format("    self.%s = %s\n", attributeName, attributeName));
                    // outputStatement += objBuilder.toString();
                    break;

                case STATIC_IVAR:
                    outputStatement += String.format("self._%s = %s\n", attributeName, attributeName);
                    // Want to skip this, spec has it as a class attribute like for the ctor, but it may not be
                    break;

                default:
                    outputStatement += String.format("self.%s = %s\n", attributeName, attributeName);
                    break;
            }
            
            printWriter.print(tabIndent + outputStatement);

            // en enum with default:
            // self.country = country or Country.default

            // a classRef without default:
            // self.domain = domain or Domain()


            // If it has a default, can just use the value
            // self.entityKind = entityKind

            // Samples
            // self.entityKind = entityKind or EntityKind.default
            // self.domain = domain or Domain()
            // self.country = country or Country.default
            // self.category = category
            // self.subCategory = subCategory
            // self.specific = specific
            // self.extra = extra

        //             String uidValue = anAttribute.getUnderlyingUid();
        // String defaultValue = anAttribute.getDefaultValue();
        // String typeName = anAttribute.getType();

        // // normalize the default value
        // if (defaultValue != null)
        // {
        //     String defaultValueTokens[] = defaultValue.split("\\.");
        //     if (defaultValueTokens[1] != null)
        //     {
        //         defaultValue = defaultValueTokens[0] + "." + defaultValueTokens[1].toLowerCase();
        //     }
        // }

        // String outputStatement = "";
        // if (anAttribute.getDefaultValue() == null)
        // {
        //     outputStatement = String.format("%s%s: \"%s | None\" = %s.default", tabIndent,  IVAR_PREFIX + anAttribute.getName(), typeName, typeName);
        //    // printWriter.println(String.format("%sself.%s = %s.default\n", tabIndent, IVAR_PREFIX + anAttribute.getName(), typeName));
        // }
        // else
        // {
        //      // printWriter.println(tabIndent + "self." + anAttribute.getName() + " = " + defaultValue + "\n");
        //      outputStatement = String.format("%s%s: \"%s | None\" = %s", tabIndent,  IVAR_PREFIX + anAttribute.getName(), typeName, defaultValue);
        // }
        }
    }

    private void writeClassProperties(PrintWriter printWriter, GeneratedClass aClass, String tabIndent)
    {
        // Write out the "hidden" static ivar settings
        StringBuilder commandSet = new StringBuilder();
        List ivars = aClass.getClassAttributes();

        for(int idx = 0; idx < ivars.size(); idx++)
        {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute)ivars.get(idx);

            String attributeName = anAttribute.getName();
            GeneratedClassAttribute.ClassAttributeType attrType = anAttribute.getAttributeKind();

            if (attrType == GeneratedClassAttribute.ClassAttributeType.STATIC_IVAR)
            {
                commandSet.append(tabIndent + String.format("def %s(self):\n", attributeName));
                commandSet.append(tabIndent + String.format("    return self._%s\n\n", attributeName));
            }

            printWriter.write(commandSet.toString());
        }
    }
    

    private void DebugPrintAttribute(PrintWriter printWriter, GeneratedClassAttribute anAttribute, String tabIndent)
    {
        if (debugAttributes.equals("true"))
        {
            String uidValue = anAttribute.getUnderlyingUid();
            String aliasName = uid2ClassName.getProperty(uidValue);

            printWriter.println(tabIndent + "\"\"\"");
            printWriter.println(tabIndent + "/// --------------------------------------------------------------------------");
            printWriter.println(tabIndent + "/// ATTRIBUTE");
            printWriter.println(anAttribute.ToString(tabIndent));
            printWriter.println(tabIndent + "///           Alias : " + aliasName);
            printWriter.println(tabIndent + "\"\"\"");
        }
    }

    private void holdOldCode(PrintWriter printWriter, GeneratedClass aClass, String tabIndent)
    {
// Comment this code out for now
    if (false)
    {
        // Write class attributes
        List ivars = aClass.getClassAttributes();
if (debugAttributes.equals("true"))
{
    printWriter.println("");
    // printWriter.print(ch , ch , ch);
    printWriter.println(StringUtils.tabs(2) + "\"\"\"");
    printWriter.println(StringUtils.tabs(2) + "/// ATTRIBUTES");
    printWriter.println(StringUtils.tabs(2) + "/// Class " + aClass.getName() + " has " + ivars.size() + " Attributes");
    for (int idx = 0; idx < aClass.getClassAttributes().size(); idx++) {
        GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(idx);
        printWriter.println(StringUtils.tabs(2) + "    ///    " +  anAttribute.getName() + "\t : " + anAttribute.getAttributeKind());;
    }
    printWriter.println(StringUtils.tabs(2) + "\"\"\"");
    printWriter.println("");
}


        for(int idx = 0; idx < ivars.size(); idx++)
        {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute)ivars.get(idx);
            String typeName = anAttribute.getType();
            String attributeName = anAttribute.getName();
            String uidValue = anAttribute.getUnderlyingUid();
            String aliasName = uid2ClassName.getProperty(uidValue);
            String defaultValue = anAttribute.getDefaultValue();

if (debugAttributes.equals("true"))
{
    printWriter.println(StringUtils.tabs(2) + "\"\"\"");
    printWriter.println(StringUtils.tabs(2) + "/// --------------------------------------------------------------------------");
    printWriter.println(StringUtils.tabs(2) + "/// ATTRIBUTE");
    printWriter.println(anAttribute.ToString(StringUtils.tabs(2)));
    printWriter.println(StringUtils.tabs(2) + "///           Alias : " + aliasName);
    printWriter.println(StringUtils.tabs(2) + "\"\"\"");
}

            if (aliasName != null && !aliasName.isEmpty() && !typeName.equals(aliasName)) typeName = aliasName;

            if((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO16) ||
               (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO32) ||
               (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PADTO64))
            {
                String bitCount = String.format("%s", anAttribute.getAttributeKind());
                bitCount = bitCount.substring(bitCount.length() - 2);

                printWriter.println(tabIndent + "self." + IVAR_PREFIX + attributeName + " = [0] * " + bitCount); //Create standard type using underscore
            }


            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
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
                    printWriter.println(tabIndent + "# /** " + anAttribute.getComment() + " */");
                }
                else 
                {
                    printWriter.println(tabIndent + "# " + IVAR_PREFIX + anAttribute.getName() + " is an undescribed parameter... ");
                }

                if (anAttribute.getDefaultValue() == null)
                {
                    printWriter.println(String.format("%sself.%s = %s.default\n", tabIndent, IVAR_PREFIX + anAttribute.getName(), typeName));
                }
                else
                {
                    printWriter.println(tabIndent + "self." + anAttribute.getName() + " = " + defaultValue + "\n");
                }
            }

            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD)
            {
                // need to change the initializer for the Capability Type
                // TODO : Not a fan of this.
                if (uidValue.equals("55"))
                {
                    if (defaultValue != null)
                    {
                        defaultValue = removeFirstWord(defaultValue).replace("()", "");
                        // defaultValue += " {}";
                    }
                }

                if ((anAttribute.getComment() != null) && !anAttribute.getComment().trim().isEmpty())
                {
                    printWriter.println(tabIndent + "# " + anAttribute.getComment());
                }
                else 
                {
                    printWriter.println(tabIndent + "# " + IVAR_PREFIX + anAttribute.getName() + " is an undescribed parameter... ");
                }

                // printWriter.println("# Default : " + defaultValue);
                // if (defaultValue != null)
                // {
                //     String defaultValueTokens[] = defaultValue.split("\\.");
                //     if (defaultValueTokens[1] != null)
                //     {
                //         defaultValue = defaultValueTokens[0] + "." + defaultValueTokens[1].toLowerCase();
                //     }
                // }
                // printWriter.println("# Default : " + defaultValue);
                // if (aliasName != null && !aliasName.isEmpty() && !typeName.equals(aliasName)) typeName = aliasName;
                if (anAttribute.getDefaultValue() == null)
                {
                    printWriter.println(String.format("%sself.%s = %s()\n", tabIndent, IVAR_PREFIX + anAttribute.getName(), typeName));
                }                
                else
                {
                    printWriter.println(String.format("%sself.%s = %s()\n", tabIndent, IVAR_PREFIX + anAttribute.getName(), defaultValue));
                    // if (aliasName.equals(typeName))                    
                    //     printWriter.println(" # " + tabIndent + IVAR_PREFIX + anAttribute.getName() + " = " + defaultValue + ";\n");
                    // else
                    //     printWriter.println(" # " + tabIndent + IVAR_PREFIX + anAttribute.getName() + " = (" + aliasName + ")" + defaultValue + ";\n");
                }

            }

            // This attribute is a primitive. 
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
            {
                if(defaultValue == null)
                {
                    if (anAttribute.getType().toLowerCase().contains("float"))
                        defaultValue = "0.0";
                    else
                        defaultValue = "0";
                }
                
                boolean hasComment = anAttribute.getComment() != null;
                
                if(hasComment)
                {
                    printWriter.println(INDENT + INDENT  + "\"\"\" " + anAttribute.getComment() + "\"\"\"");
                }

                // If we're a normal primitivetype, initialize directly; Otherwise, we need a get for this
                // and a property type, where the get returns the list size
                // if(anAttribute.getIsDynamicListLengthField() == true)
                // {
                //     GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();
                //     printWriter.println(INDENT + INDENT  + "self._" + anAttribute.getName() + " = " + defaultValue);

                //     String attrName = anAttribute.getName();

                //     String getOper = String.format("        def get_%s(self):\n", attrName)
                //              .concat(String.format("            return len(self._%s)\n", listAttribute.getName()))
                //              .concat(String.format("        def set_%s(self, value):\n", attrName))
                //              .concat(String.format("            self._%s = value\n", attrName))
                //              .concat(String.format("        %s = property(get_%s, set_%s)\n", attrName, attrName, attrName))
                //              .concat("\n");
                //     printWriter.println(getOper);
                // }
                // else
                // {
                    printWriter.println(INDENT + INDENT  + "self." + anAttribute.getName() + " = " + defaultValue);
                // }
            } // end of primitive attribute type
            
            // This is a class
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
            {
                String attributeType = anAttribute.getType();

                
                if (anAttribute.getDefaultValue() != null)
                {
                    printWriter.println(tabIndent + "# TODO - Process the default value for a CLASSREF type");
                }

                if(anAttribute.getComment() != null)
                {
                    printWriter.println(INDENT + INDENT  + "\"\"\" " + anAttribute.getComment() + "\"\"\"");
                }
                else 
                {
                    printWriter.println(tabIndent + "# " + IVAR_PREFIX + anAttribute.getName() + " is an undescribed parameter... ");
                }
                printWriter.println(INDENT + INDENT  + "self." + anAttribute.getName() + " = " + attributeType + "()");
            }
            
            // The attribute is a fixed list, ie an array of some type--maybe primitve, maybe a class.
            
            if( (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST) )
            {
                String attributeType = anAttribute.getType();
                int listLength = anAttribute.getListLength();
                String listLengthString = "" + listLength;
                
                if(anAttribute.getComment() != null)
                {
                    printWriter.println(INDENT + INDENT + "\"\"\" " + anAttribute.getComment() + "\"\"\"");
                }

                if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
                {
                    printWriter.print(INDENT + INDENT + "self." + anAttribute.getName() + " =  " +
                                 "[");
                    for(int arrayLength = 0; arrayLength < anAttribute.getListLength(); arrayLength++)
                    {
                        printWriter.print(" 0");
                        if(arrayLength < anAttribute.getListLength() - 1)
                            printWriter.print(",");
                    }
                    printWriter.println("]");
                }
                else
                {                    
                    printWriter.print(INDENT + INDENT + "self." + anAttribute.getName() + " =  " +
                                 "[");
                    for(int arrayLength = 0; arrayLength < anAttribute.getListLength(); arrayLength++)
                    {
                        printWriter.print(" " + attributeType + "()");
                        if(arrayLength < anAttribute.getListLength() - 1)
                            printWriter.print(",");
                    }
                    printWriter.println("]");
                }
            }
            
            // The attribute is a variable list of some kind. 
            if( (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST) )
            {
                String attributeType = anAttribute.getType();
                int listLength = anAttribute.getListLength();
                String listLengthString = "" + listLength;
                
                if(anAttribute.getComment() != null)
                {
                    printWriter.println(INDENT + INDENT +"\"\"\" " + anAttribute.getComment() + "\"\"\"");
                }
                printWriter.println(INDENT + INDENT + "self._" + anAttribute.getName() + " = []");
            }
             
        } // end of loop through attributes
    }
    }

    private void writeSuperInit(PrintWriter printWriter, GeneratedClass aClass, String tabIndent)
    {
        int numClassAttributes = aClass.getClassAttributes().size();

        // If this is a subclass, call the superclass intializer
        if(!aClass.getParentClass().equalsIgnoreCase("root"))
        {
            // printWriter.println(INDENT + INDENT  + "super(" + aClass.getParentClass() + ", self).__init__()");
            printWriter.println(tabIndent  + "super().__init__()\n");
        }
        else if (numClassAttributes == 0)
        {
            printWriter.println(tabIndent + "pass");
        }
    }

    /**
     * Create custom output
     * @param printWriter output
     * @param aClass class of interest
     */
    public void writeClass(PrintWriter printWriter, GeneratedClass aClass)
    {

        int numClassAttributes = aClass.getClassAttributes().size();

        String tabIndent = StringUtils.tabs(2);

        printWriter.println();
        
        String parentClassName = aClass.getParentClass();
        if(parentClassName.equalsIgnoreCase("root"))
            parentClassName = "object";
        
        printWriter.println("class " + aClass.getName() + "( " + parentClassName + " ):");
        this.writeClassComments(printWriter, aClass);

        printWriter.println(INDENT + "\"\"\" Initializer for " + aClass.getName() + "\"\"\"");
        
        // Output constructor paramaters
        if (numClassAttributes > 0)
        {
            printWriter.println(INDENT + "def __init__(self,");
            writeClassParameters(printWriter, aClass, tabIndent);
            writeClassParameterComments(printWriter, aClass, tabIndent);
            writeSuperInit(printWriter, aClass, tabIndent);
            writeClassParameterAssignments(printWriter, aClass, tabIndent);
            writeClassProperties(printWriter, aClass, tabIndent);
        }
        else 
        {
            printWriter.println(INDENT + "def __init__(self):");
            writeSuperInit(printWriter, aClass, tabIndent);
        }

        // Write out the static IVARS here.

        // Some variables may be set to an inital value.
        List inits = aClass.getInitialValues();
        for(int idx = 0; idx < inits.size(); idx++)
        {
            GeneratedInitialValue anInit = (GeneratedInitialValue)inits.get(idx);
            GeneratedClass currentClass = aClass;
            boolean found = false;
        
            while(currentClass != null)
                {
                    List thisClassesAttributes = currentClass.getClassAttributes();
                    for(int jdx = 0; jdx < thisClassesAttributes.size(); jdx++)
                    {
                        GeneratedClassAttribute anAttribute = (GeneratedClassAttribute)thisClassesAttributes.get(jdx);
                        //System.out.println("--checking " + anAttribute.getName() + " against inital value " + anInitialValue.getVariable());
                        if(anInit.getVariable().equals(anAttribute.getName()))
                        {
                            found = true;
                            break;
                        }
                    }
                    currentClass = classDescriptions.get(currentClass.getParentClass());
                }
                if(!found)
                {
                    System.out.println("Could not find initial value matching attribute name for " + anInit.getVariable() + " in class " + aClass.getName());
                }
                else
                {
                    if (debugAttributes.equals("true"))
                    {
                        printWriter.println("");
                        printWriter.println(tabIndent + "\"\"\"");
                        printWriter.println(anInit.ToString(tabIndent));
                        printWriter.println(tabIndent + "\"\"\"");
                        printWriter.println("");
                    }

                    String variableValue = anInit.getVariableValue();
                    if (variableValue != null) variableValue = StringUtils.setEnumValueToLowerCase(variableValue);

                    // printWriter.println(tabIndent  +"self." + anInit.getVariable() + " = " + anInit.getVariableValue() );
                    // printWriter.println(tabIndent + "\"\"\" initialize value \"\"\"");

                    if (StringUtils.isEnumType(variableValue))
                    {
                        String enumType = StringUtils.getEnumType(variableValue);
                        String enumValue = StringUtils.getEnumValue(variableValue);
                        printWriter.println(tabIndent + "self." + anInit.getVariable() + " = " + enumType + "." + enumValue);
                    }
                    else
                    {
                        // Use the setter and value ?  no.
                        printWriter.println(tabIndent + "self." + anInit.getVariable() + " = " + variableValue);
                        // pw.println(indent +1, StringUtils.firstCharUpper(anInit.getVariable()) + " = " 
                        //                       + anInit.getSetterMethodName()
                        //                       + "("
                        //                       + variableValue
                        //                       + ")"
                        //                       + ";");
                    }

                }
        } // End initialize initial values
    
        this.writeGettersAndSetters(printWriter, aClass);
        this.writeToString(printWriter, aClass);
        this.writeEnumMarshallers(printWriter, aClass);
        this.writeMarshal(printWriter, aClass);
        this.writeUnmarshal(printWriter, aClass);
        this.writeAttrCounters(printWriter, aClass);
        this.writeEqualityOperators(printWriter, aClass);
        // this.writeFlagMethods(printWriter, aClass);
        printWriter.println();
        printWriter.println();
        
        printWriter.flush();
    }

    public void writeGettersAndSetters(PrintWriter pw, GeneratedClass aClass)
    {

        List<GeneratedClassAttribute> attributes = aClass.getClassAttributes();
        for(int idx = 0; idx < attributes.size(); idx++)
        {
            GeneratedClassAttribute anAttribute = /*(GeneratedClassAttribute)*/attributes.get(idx);

            String tabIndent = StringUtils.tabs(1);
            String attrName  = anAttribute.getName();
            
            switch(anAttribute.getAttributeKind())
            {
                case PRIMITIVE:
                    // pw.println(tabIndent + "# PRIMITIVE");
                     String marshalType = marshalTypes.getProperty(anAttribute.getType());
                
                     // If we're a normal primitivetype, marshal out directly; otherwise, marshall out
                     // the list length.
                     
                     if(anAttribute.getIsDynamicListLengthField() == true)
                     {
                        GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();

                        pw.println('\n');
                        pw.println(tabIndent + "@property");
                        pw.println(String.format("%sdef %s(self):", tabIndent, anAttribute.getName()));
                        pw.println(String.format("%s%sreturn len(self.%s)", tabIndent, tabIndent, listAttribute.getName()));
                        pw.println();
                        pw.println(String.format("%s@%s.setter", tabIndent, anAttribute.getName()));
                        pw.println(String.format("%sdef %s(self, new_value: %s):", tabIndent, anAttribute.getName(), anAttribute.getType()));
                        pw.println(String.format("%s%sself._%s = new_value", tabIndent, tabIndent, anAttribute.getName()));

                        // pw.println(tabIndent + "@property");

                        // pw.println(String.format("%sdef get_%s(self):", tabIndent, anAttribute.getName()));
                        // pw.println(String.format("%s%sreturn len(self._%s)", tabIndent, tabIndent, listAttribute.getName()));
                        // pw.println(String.format("%sdef set_%s(self, value):", tabIndent, anAttribute.getName()));
                        // pw.println(String.format("%s%s%s = value", tabIndent, tabIndent, anAttribute.getName()));
                        
                     }
                     break;

                case OBJECT_LIST:
                    String objListText = String.format("\n%sdef get_%s(self):\n", tabIndent, attrName)
                                 .concat(String.format("%s%sreturn self.%s\n\n", tabIndent, tabIndent, attrName))
                                 .concat(String.format("%sdef set_%s(self, value):\n", tabIndent, attrName))
                                 .concat(String.format("%s%sself.%s = value\n", tabIndent, tabIndent, attrName));
                    pw.println(objListText);

                    String addObjText = String.format("%sdef add_%s(self, value : %s):\n", tabIndent, attrName, anAttribute.getType())
                                .concat(String.format("%s%sself.%s.append(value)\n", tabIndent, tabIndent, attrName));

                    pw.println(addObjText);
                    // pw.println(String.format("%sdef get_%s(self):", tabIndent, attrName));
                    // pw.println(String.format("%s%sreturn self.%s", tabIndent, tabIndent, attrName));
                    // pw.println(String.format("%sdef set_%s(self, value):", tabIndent, attrName));
                    // pw.println(String.format("%s%sself.%s = value", tabIndent, tabIndent, attrName));
                    // pw.println(String.format("%s%s = property(get_%s, set_%s)", tabIndent, attrName, attrName, attrName));

                    // pw.println('\n');
                    // pw.println(String.format("%sdef add_%s(self, value : %s):", tabIndent, attrName, anAttribute.getType()));
                    // pw.println(String.format("%s%sself.%s.append(value)", tabIndent, tabIndent, attrName));
                    // pw.println('\n');

                    pw.println(tabIndent + "\"\"\"");
                    pw.println(anAttribute.ToString(tabIndent));
                    pw.println(tabIndent + "\"\"\"");
                    pw.println('\n');
                    break;
            }
        }
    }
    public void writeToStringMembers(PrintWriter pw, GeneratedClass aClass)
    {
        String tabIndent = StringUtils.tabs(2);

        List<GeneratedClassAttribute> attributes = aClass.getClassAttributes();
        for(int idx = 0; idx < attributes.size(); idx++)
        {
            GeneratedClassAttribute anAttribute = /*(GeneratedClassAttribute)*/attributes.get(idx);
            String attrType = anAttribute.getType();
            
            // Some fields may be declared but shouldn't be serialized
            if(anAttribute.shouldSerialize == false)
            {
                pw.println(tabIndent + "# attribute " + anAttribute.getName() + " marked as do not serialize");
                continue;
            }
            
            switch(anAttribute.getAttributeKind())
            {
                case SISO_ENUM:
                    // pw.println(tabIndent + "# SISO_ENUM");
                    String enumFormat = "%soutputString += \"%s : \" + self.%s.description + \"(\" + (str(int(self.%s))) + \")\" + \"\\n\"";
                    pw.println(String.format(enumFormat, tabIndent, anAttribute.getType(), anAttribute.getName(), anAttribute.getName()));
                    break;

                    

                case SISO_BITFIELD:
                    String bitfieldFormat = "%soutputString += \"%s : \" + str(self.%s) + \"\\n\"";
                    pw.println(String.format(bitfieldFormat, tabIndent, anAttribute.getType(), anAttribute.getName()));
                    break;

                case PRIMITIVE:
                    // pw.println(tabIndent + "# PRIMITIVE");
                     String marshalType = marshalTypes.getProperty(anAttribute.getType());
                
                     // If we're a normal primitivetype, marshal out directly; otherwise, marshall out
                     // the list length.
                     
                     if(anAttribute.getIsDynamicListLengthField() == true)
                     {
                          GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();
                          String primitiveFormat = "%soutputString += \"%s : \" + str(len(self.%s)) + \"\\n\"";
                          pw.println(String.format(primitiveFormat, tabIndent, StringUtils.firstCharUpper(anAttribute.getName()), listAttribute.getName()));
                     }
                     else
                     {
                        String primitiveFormat = "%soutputString += \"%s : \" + str(self.%s) + \"\\n\"";
                        pw.println(String.format(primitiveFormat, tabIndent, StringUtils.firstCharUpper(anAttribute.getName()), anAttribute.getName()));
                     }
                    // pw.flush();
                    break;
                    
                case CLASSREF:
                    // pw.println(tabIndent + "# CLASSREF - " + anAttribute.getType());
                    String classRefFormat = "%soutputString += \"%s :\" + \"\\n\" + self.%s.to_string() + \"\\n\"";
                    pw.println(String.format(classRefFormat, tabIndent, StringUtils.firstCharUpper(anAttribute.getName()), anAttribute.getName()));
                    break;

                case PRIMITIVE_LIST:
                case OBJECT_LIST:
                    // pw.println(tabIndent + "# " + anAttribute.getAttributeKind() + " - " + anAttribute.getName());
                    if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST)
                        attrType = anAttribute.getName();

                    // String attrType = anAttribute.getType();
                    pw.println(String.format("%soutputString += \"%s : \" + \"\\n\"", tabIndent, StringUtils.firstCharUpper(anAttribute.getName())));

                    String listLength = (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST) ? "Count" : "Length";
                    
                    if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
                    {
                        pw.println(tabIndent + "for idx in range(0, len(self." + anAttribute.getName() + ")):");
                        pw.println(tabIndent + StringUtils.tabs(1) + "outputString += str(self." + anAttribute.getName() + "[idx])");
                    }
                    else
                    {
                        pw.println(tabIndent + "for idx in range(0, len(self." + anAttribute.getName() + ")):");
                        pw.println(tabIndent + "    if self." + anAttribute.getName() + "[idx] is not None:");
                        pw.println(tabIndent + StringUtils.tabs(1) + "    outputString += self." +  anAttribute.getName() + "[idx].to_string()");
                    }
                    pw.println();
                    break;


            }
        }
    }
    /** The method that writes out the python to_string and __str__ methods
     * @param pw PrintWriter
     * @param aClass of interest */
    public void writeToString(PrintWriter pw, GeneratedClass aClass)
    {
        String tabIndent = StringUtils.tabs(1);

        pw.println();
        pw.println(tabIndent + "def to_string(self) ->str:");
        pw.println(tabIndent + StringUtils.tabs(1) + "outputString = \"\"");

        
        // pw.println(tabIndent + StringUtils.tabs(1) + "outputString += \"" + aClass.getName() + " :\\n\"");
        String parentClassName = aClass.getParentClass();
        if(!parentClassName.equalsIgnoreCase("root"))
        {
            // pw.println(tabIndent + StringUtils.tabs(1) + "outputString += super(" + aClass.getParentClass() + ", self).to_string()");
            pw.println(tabIndent + StringUtils.tabs(1) + "outputString += super().to_string()");
        }
        tabIndent = StringUtils.tabs(2);
        writeToStringMembers(pw, aClass);
        pw.println(tabIndent + "return outputString");

        tabIndent = StringUtils.tabs(1);
        pw.println();
        pw.println(tabIndent + "def __str__(self):");
        pw.println(tabIndent + StringUtils.tabs(1) + "return self.to_string()");
        // pw.println();
    }
    
    
    public void writeEnumMarshallers(PrintWriter pw, GeneratedClass aClass)
    {
        String enumMarshallers = "    def serialize_enum(self, enumValue, enumSize, outputStream):\n"
                        //  .concat("        enumSize = enumValue.get_marshaled_size()\n")
                         .concat("        marshallers = {8 : 'byte', 16 : 'short', 32 : 'int'}\n")
                         .concat("        slot = (enumSize +7) & ~7\n")
                         .concat("        marshalFunction = getattr(outputStream, 'write_unsigned_' + marshallers[slot])\n")
                         .concat("        result = marshalFunction(int(enumValue))\n\n")
                         .concat("    def parse_enum(self, enumValue, enumSize, intputStream) -> int:\n")
                        //  .concat("        enumSize = enumValue.get_marshaled_size()\n")
                         .concat("        marshallers = {8 : 'byte', 16 : 'short', 32 : 'int'}\n")
                         .concat("        slot = (enumSize +7) & ~7\n")
                         .concat("        marshalFunction = getattr(intputStream, 'read_unsigned_' + marshallers[slot])\n")
                         .concat("        return marshalFunction()\n");

            pw.println();
            pw.print(enumMarshallers);
    }

    /** The method that writes out the python marshalling code
     * @param pw PrintWriter
     * @param aClass of interest */
    public void writeMarshal(PrintWriter pw, GeneratedClass aClass)
    {
        int numClassAttributes = aClass.getClassAttributes().size();

        pw.println();
        pw.println(INDENT + "def serialize(self, outputStream):" );
        pw.println(INDENT + INDENT + "\"\"\"serialize the class \"\"\"");
        
        // If this is not a top-level class, call the superclass
        String parentClassName = aClass.getParentClass();
        if(!parentClassName.equalsIgnoreCase("root"))
        {
            pw.println(INDENT + INDENT + "super( " + aClass.getName() + ", self ).serialize(outputStream)");
        }

        if (numClassAttributes == 0)
        {
            pw.println(INDENT + INDENT + "pass");
        }
        
        List<GeneratedClassAttribute> attributes = aClass.getClassAttributes();
        for(int idx = 0; idx < attributes.size(); idx++)
        {
            GeneratedClassAttribute anAttribute = /*(GeneratedClassAttribute)*/attributes.get(idx);
            
            // Some fields may be declared but shouldn't be serialized
            if(anAttribute.shouldSerialize == false)
            {
                pw.println(INDENT + INDENT + "# attribute " + anAttribute.getName() + " marked as do not serialize");
                continue;
            }
            
            String marshalType = "";
            switch(anAttribute.getAttributeKind())
            {
                case SISO_ENUM:
                    // marshalType = "unsigned_int";
                    // pw.println(INDENT + INDENT + "outputStream.write_" + marshalType + "(int(self." + anAttribute.getName() + "))");

                    // self.serialize_enum(self.forceId,outputStream)
                    pw.println(INDENT + INDENT + "enumSize = " + anAttribute.getType() + "().get_marshaled_size()");
                    pw.println(INDENT + INDENT + "self.serialize_enum(self." + anAttribute.getName() + ",enumSize, outputStream)");
                    
                    // pw.println(INDENT + INDENT + "self." + anAttribute.getName() + ".serialize(outputStream)");
                    break;

                case SISO_BITFIELD:
                    marshalType = "unsigned_int";
                    // pw.println(INDENT + INDENT + "self.serialize_enum(self." + anAttribute.getName() + ".asbyte" + ",outputStream)");
                    pw.println(INDENT + INDENT + "outputStream.write_" + marshalType + "(int(self." + anAttribute.getName() + ".asbyte))");
                    break;

                case PRIMITIVE:
                     marshalType = marshalTypes.getProperty(anAttribute.getType());
                
                     // If we're a normal primitivetype, marshal out directly; otherwise, marshall out
                     // the list length.
                     
                     if(anAttribute.getIsDynamicListLengthField() == true)
                     {
                          GeneratedClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();
                          pw.println(INDENT + INDENT + "outputStream.write_" + marshalType + "( len(self." + listAttribute.getName() + "))");
                     }
                     else
                     {
                        pw.println(INDENT + INDENT + "outputStream.write_" + marshalType + "(int(self."+ anAttribute.getName() + "))");
                     }
                    pw.flush();
                    break;
                    
                case CLASSREF:
                    pw.println(INDENT + INDENT + "self." + anAttribute.getName() + ".serialize(outputStream)");
                    break;
                    
                case PRIMITIVE_LIST:
                    // Write out the method call to encode a fixed length list, aka an array.
                
                    pw.println(INDENT + INDENT + "for idx in range(0, " + anAttribute.getListLength() + "):");

                    if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
                    {
                         marshalType = unmarshalTypes.getProperty(anAttribute.getType());

                        pw.println(INDENT + INDENT + INDENT +"outputStream.write_" + marshalType + "( self." + anAttribute.getName() + "[ idx ] );");
                    }
                    else if(anAttribute.listIsClass() == true) 
                    {
                        pw.println(INDENT + INDENT + INDENT+ "self." + anAttribute.getName() + "[ idx ].serialize(outputStream);");
                    }

                    pw.println();

                    break;
                    
                case OBJECT_LIST:
                    //pw.println(INDENT + INDENT + "while idx < len(" + anAttribute.getName() + "):");
                    if(anAttribute.getIsDynamicListLengthField() == true)
                    {
                        pw.println(INDENT + INDENT + "for anObj in self." + anAttribute.getName() + ":");
                    }
                    else
                    {
                        pw.println(INDENT + INDENT + "for anObj in self." + anAttribute.getName() + ":");
                    }
                    // This is some sleaze. We're an array, but an array of what? We could be either a
                    // primitive or a class. We need to figure out which. This is done via the expedient
                    // but not very reliable way of trying to do a lookup on the type. If we don't find
                    // it in our map of primitives to marshal types, we assume it is a class.

                    marshalType = marshalTypes.getProperty(anAttribute.getType());

                    if(marshalType == null) // It's a class
                    {
                        pw.println(INDENT + INDENT + INDENT+ "anObj.serialize(outputStream)");
                    }
                    else // It's a primitive
                    {
                        pw.println(INDENT + INDENT + INDENT + "outputStream.write_" + marshalType + "( anObj )");
                    }
                    pw.println();
                    break;
            }
        }
        
    }
    

    /**
     * Create custom method
     * @param printWriter output
     * @param aClass class of interest
     */
    public void writeUnmarshal(PrintWriter printWriter, GeneratedClass aClass)
    {
        int numClassAttributes = aClass.getClassAttributes().size();

        printWriter.println();
        printWriter.println(INDENT + "def parse(self, inputStream):");
        printWriter.println(INDENT + INDENT + "\"\"\"\"Parse a message. This may recursively call embedded objects.\"\"\"");
        
        // If this is not a top-level class, call the superclass
        String parentClassName = aClass.getParentClass();
        if(!parentClassName.equalsIgnoreCase("root"))
        {
            printWriter.println(INDENT + INDENT + "super( " + aClass.getName() + ", self).parse(inputStream)");
        }

        if (numClassAttributes == 0)
        {
            printWriter.println(INDENT + INDENT + "pass");
        }
        
        List attributes = aClass.getClassAttributes();
        for(int idx = 0; idx < attributes.size(); idx++)
        {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute)attributes.get(idx);
            
            // Some fields may be declared but should not be serialized or
            // unserialized
            if(anAttribute.shouldSerialize == false)
            {
                printWriter.println(INDENT + INDENT + "# attribute " + anAttribute.getName() + " marked as do not serialize");
                continue;
            }
            
            String marshalType = "";
            switch(anAttribute.getAttributeKind())
            {
                case SISO_ENUM:
                    // self.forceId = ForceID.get_enum(self.parse_enum(self.forceId, inputStream))
                    // marshalType = "unsigned_int";
                    String enumName = anAttribute.getType();
                    // self.pad = inputStream.read_byte();
                    // printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + " = " + enumName + ".get_enum(inputStream.read_" + marshalType + "())");
                    printWriter.println(INDENT + INDENT + "enumSize = " + anAttribute.getType() + "().get_marshaled_size()");
                    printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + " = " 
                             + enumName + ".get_enum(self.parse_enum(self." + anAttribute.getName() + ",enumSize, inputStream))");

                    // printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + ".parse(inputStream)");
                    break;

                case SISO_BITFIELD:
                    // printWriter.println(INDENT + INDENT + "# TODO - Need to unmarshal the BITFIELD "  + anAttribute.getType());
                    marshalType = "unsigned_int";
                    String bitfieldName = anAttribute.getType();
                    // printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + ".asbyte = " +
                    //          "self.parse_enum(self." + anAttribute.getName() + ",inputStream)");
                    printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + ".asbyte = " +
                             "inputStream.read_unsigned_int()");                             
                    // printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + ".parse(inputStream)");
                    break;

                case PRIMITIVE:
                    marshalType = marshalTypes.getProperty(anAttribute.getType());
                    String localized = "";
                    if(anAttribute.getIsDynamicListLengthField() == true)
                        printWriter.println(INDENT + INDENT + "_" + anAttribute.getName() + " = inputStream.read_" + marshalType + "()");
                    else
                        printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + " = inputStream.read_" + marshalType + "()");
                    break;
                    
                case CLASSREF:
                    printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + ".parse(inputStream)");
                    break;
                    
                case PRIMITIVE_LIST:
                    // Write out the method call to parse a fixed length list, aka an array.
                
                    printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + " = [0]*" + anAttribute.getListLength());
                    
                    printWriter.println(INDENT + INDENT + "for idx in range(0, " + anAttribute.getListLength() + "):");

                    if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
                    {
                         marshalType = unmarshalTypes.getProperty(anAttribute.getType());
                        printWriter.println(INDENT + INDENT + INDENT + "val = inputStream.read_" + marshalType + "()");
                        printWriter.println(INDENT + INDENT + INDENT + "self." + anAttribute.getName() + "[  idx  ] = val");
                        //pw.println(INDENT + INDENT + INDENT +"inputStream.read_" + marshalType + "( self." + anAttribute.getName() + "[ idx ] );");
                    }
                    //else if(anAttribute.listIsClass() == true) 
                    ///{
                    //    pw.println(INDENT + INDENT + INDENT+ "self." + anAttribute.getName() + "[ idx ].serialize(outputStream);");
                    //}

                    printWriter.println();
                    break;
                    
                case OBJECT_LIST:
                    printWriter.println(INDENT + INDENT + "for idx in range(0, _" + anAttribute.getCountFieldName() + "):");

                    // This is some sleaze. We're an array, but an array of what? We could be either a
                    // primitive or a class. We need to figure out which. This is done via the expedient
                    // but not very reliable way of trying to do a lookup on the type. If we don't find
                    // it in our map of primitives to marshal types, we assume it is a class.

                    marshalType = marshalTypes.getProperty(anAttribute.getType());

                    if(marshalType == null) // It's a class
                    {
                        printWriter.println(INDENT + INDENT + INDENT + "element = " + anAttribute.getType() + "()");
                        printWriter.println(INDENT + INDENT + INDENT + "element.parse(inputStream)");
                        // printWriter.println(anAttribute.ToString(StringUtils.tabs(3)));
                        printWriter.println(INDENT + INDENT + INDENT+ "self." + anAttribute.getName() + ".append(element)");
                    }
                    else // It's a primitive
                    {
                        printWriter.println(INDENT + INDENT + INDENT + "self." + anAttribute.getName() + ".add( inputStream.read_" + marshalType + "(  )");
                    }
                    printWriter.println();
                    
                    break;
            } // end switch  
            
        } // End loop through attributes
    }
    
    /**
     * Write out methods to compare two objects
     * Expected to be used for debug and generator development only
     * @param printWriter output
     * @param aClass class of interest
     */
    public void writeEqualityOperators(PrintWriter printWriter, GeneratedClass aClass)
    {
        String comparisonOperators = "    def __eq__(self, other):\n"
                            .concat("        if isinstance(other, self.__class__):\n")
                            .concat("            return self.__dict__ == other.__dict__\n")
                            .concat("        else:\n")
                            .concat("            return False\n")
                            .concat("\n")
                            .concat("    def __ne__(self, other):\n")
                            .concat("        return not self.__eq__(other)\n")
                            .concat("\n")
                            .concat("    def diff(self,other) -> set | None:\n")
                            .concat("        diffs = set()\n")
                            .concat("        for key, value in self.__dict__.items():\n")
                            .concat("            value2 = other.__dict__[key]\n")
                            .concat("            if (value != value2):\n")
                            .concat("                if type(value) is list:\n")
                            .concat("                    diffs.add((key, str(value)))\n")
                            .concat("                    diffs.add((key, str(value2)))\n")
                            .concat("                elif (type(value).__module__ == \"builtins\"):\n")
                            .concat("                    diffs.add((key, value))\n")
                            .concat("                    diffs.add((key, value2))\n")
                            .concat("                elif (isinstance(value, Enum)):\n")
                            .concat("                    diffs.add((key, value))\n")
                            .concat("                    diffs.add((key, value2))\n")
                            .concat("                elif (isinstance(value, object)):\n")
                            .concat("                    diffs.update(value.diff(value2))\n")
                            .concat("                else:\n")
                            .concat("                    diffs.add((key, value))\n")
                            .concat("                    diffs.add((key, value2))\n")
                            .concat("        return diffs if diffs else None\n");

                            //  .concat("    def __hash__(self):\n")
                            //  .concat("        return hash(tuple(sorted(self.__dict__.items())))\n")
                            //  .concat("\n")
                            // .concat("    def diff(self, other) -> str:\n")
                            // .concat("        return str(self.__dict__.items() ^ other.__dict__.items())\n");

                            // .concat("        return str(tuple(self.__dict__.items()) == tuple(other.__dict__.items()))\n");
        printWriter.println();
        printWriter.print(comparisonOperators);
        printWriter.println();
/*
    def __eq__(self, other):
        if isinstance(other, self.__class__):
            return self.__dict__ == other.__dict__
        else:
            return False

    def __hash__(self):
        """Overrides the default implementation"""
        return hash(tuple(sorted(self.__dict__.items())))

    def __ne__(self, other):
        return not self.__eq__(other)
*/

    }

    /**
     * Write out methods to get design atribute count and actual attribute count
     * Expected to be used for debug and generator development only
     * @param printWriter output
     * @param aClass class of interest
     */
    public void writeAttrCounters(PrintWriter printWriter, GeneratedClass aClass)
    {
        String indent = StringUtils.tabs(1);

        printWriter.println();
        printWriter.println(indent + "# Get the number of attributes defined by SISO");
        printWriter.println(indent + "def get_design_attribute_count(self) -> int:");
        printWriter.println(indent + indent + "return " + aClass.getClassAttributes().size());

        // This is terrible.
        String getCountStatement = "    def get_attribute_count(self) -> int:\n"
                           .concat("        attrList = list()\n")
                           .concat("        for attr in dir(self):\n")
                           .concat("            if not callable(getattr(self, attr)):\n")
                           .concat("                if not attr.startswith(\"__\"):\n")
                           .concat("                    if not hasattr(self.__class__.__base__(), attr):\n")
                           .concat("                        attrList.append(attr)\n")
                           .concat("        return len(attrList)\n");

        printWriter.println("");
        printWriter.print(getCountStatement);
    }

    /**
     * Create custom output
     * @param printWriter output
     * @param aClass class of interest
     */
    public void writeClassComments(PrintWriter printWriter, GeneratedClass aClass)
    {
        printWriter.println(INDENT + "\"\"\"" + aClass.getClassComments() + "\"\"\"");
        printWriter.println();
    }
    
    /**
     * Some fields have integers with bit fields defined, eg an integer where 
     * bits 0-2 represent some value, while bits 3-4 represent another value, 
     * and so on. This writes accessor and mutator methods for those fields.
     * 
     * @param pw PrintWriter
     * @param aClass of interest 
     */
    public void writeFlagMethods(PrintWriter pw, GeneratedClass aClass)
    {
        List attributes = aClass.getClassAttributes();
        
        for(int idx = 0; idx < attributes.size(); idx++)
        {
            GeneratedClassAttribute anAttribute = (GeneratedClassAttribute)attributes.get(idx);
           
            
            switch(anAttribute.getAttributeKind())
            {
                
                // Anything with bitfields must be a primitive type
                case PRIMITIVE:
                    
                    List bitfields = anAttribute.bitFieldList;
   
                    for(int jdx = 0; jdx < bitfields.size(); jdx++)
                    {
                        GeneratedBitField bitfield = (GeneratedBitField)bitfields.get(jdx);
                        String capped = this.initialCapital(bitfield.name);
                        int shiftBits = this.getBitsToShift(anAttribute, bitfield.mask);
                        
                        // write getter
                        pw.println();
                        pw.println(INDENT + "def get" + capped + "(self):");
                        if(bitfield.description != null)
                        {
                            pw.println(INDENT + INDENT + "\"\"\"" + bitfield.description + " \"\"\"");
                        }
                        
                        pw.println(INDENT + INDENT + "val = self." + bitfield.parentAttribute.getName() + " & " + bitfield.mask);
                        pw.println(INDENT + INDENT + "return val >> " + shiftBits);
                        pw.println();
                        
                        // Write the setter/mutator
                        
                        pw.println();
                        pw.println(INDENT + "def set" + capped + "(self, val):");
                        if(bitfield.description != null)
                        {
                            pw.println(INDENT + INDENT + "\"\"\"" + bitfield.description + " \"\"\"");
                        }
                        pw.println(INDENT + INDENT + "aVal = 0");
                        pw.println(INDENT + INDENT + "self." + bitfield.parentAttribute.getName() + " &= ~" + bitfield.mask);
                        pw.println(INDENT + INDENT + "val = val << " + shiftBits);
                        pw.println(INDENT + INDENT + "self." + bitfield.parentAttribute.getName() + " = self." + bitfield.parentAttribute.getName() + " | val" );
                        //pw.println(INDENT + INDENT + bitfield.parentAttribute.getName() + " = val & ~" + mask);
                        pw.println();
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
    
    /**
     * Create output
     * @param printWriter output
     */
    public void writeLicense(PrintWriter printWriter)
    {
        printWriter.println("#");
        printWriter.println("#This code is licensed under the BSD software license");
        printWriter.println("# Copyright 2009-2025, MOVES Institute");
        printWriter.println("# Author: KenH");
        printWriter.println("#");
    }
    
    
    /**
     * Python doesn't like forward-declaring classes, so a subclass must be
     * declared after its superclass.This reorders the list of classes so that
     * this is the case. This re-creates the semantic class inheritance tree
     * structure, then traverses the tree in preorder fashion to ensure that a
     * base class is written before a subclass. The implementation is a little
     * wonky in places.
     * // TODO alternative is to provide __inii.py list of classes
     * @return sorted List of classes
     */
    public List sortClasses()
    {
        List<GeneratedClass> allClasses = new ArrayList<>(classDescriptions.values());
        List<GeneratedClass> sortedClasses = new ArrayList<>();
        
        TreeNode root = new TreeNode(null);
        
        while(allClasses.size() > 0)
        {
            Iterator<GeneratedClass> li = allClasses.listIterator();
            while(li.hasNext())
            {
                GeneratedClass aClass = li.next();
                if(aClass.getParentClass().equalsIgnoreCase("root"))
                {
                    root.addClass(aClass);
                    li.remove();
                }
            }
            
           li = allClasses.listIterator();
           while(li.hasNext())
            {
                GeneratedClass aClass = li.next();
                TreeNode aNode = root.findClass(aClass.getParentClass());
                if(aNode != null)
                {
                    aNode.addClass(aClass);
                    li.remove();
                }
            }
           

        } // while all classes still has content

        // Get a sorted list
        List<TreeNode> blah = new ArrayList<>();
        root.getList(blah);
        
        Iterator<TreeNode> it = blah.iterator();
        while(it.hasNext())
        {
            TreeNode node = it.next();
            if(node.aClass != null)
                sortedClasses.add(node.aClass);
        }
                
        return sortedClasses;
    }

    private void writeUsingStatements(PrintStringBuffer pw, GeneratedClass aClass)
    {
        Map<String, String> usingStatements = new HashMap<>();

        pw.println("from enum import Enum");
        pw.println("from typing import Optional, List");
        pw.println("from .dis7types import *");
        pw.println();

        // Add using statements for any types used to initialize attributes
        List inits = aClass.getInitialValues();
        for (int idx = 0; idx < inits.size(); idx++) {
            GeneratedInitialValue anInit = (GeneratedInitialValue) inits.get(idx);

            String variableValue = anInit.getVariableValue();
            String enumType = StringUtils.getEnumType(variableValue);
            if (enumType != null)
            {
                String pythonPath = StringUtils.removeFirstWord(enumNamespace, "\\.") + "." + StringUtils.camelCasetoLowerUnderscore(enumType);
                usingStatements.put(pythonPath, enumType);
            }
        }

        for (int index = 0; index < aClass.getClassAttributes().size(); index++)
        {
            // need to find out which ones are classes and not put a using

            GeneratedClassAttribute anAttribute = aClass.getClassAttributes().get(index);
            String typeName = anAttribute.getType();
            String uidValue = anAttribute.getUnderlyingUid();
            String defaultValue = anAttribute.getDefaultValue();
            String pythonImportName = StringUtils.camelCasetoLowerUnderscore(typeName);

            // We probably already put this one out above
            // pw.println();
            // pw.println(anAttribute.ToString());
            // pw.println("/// Type : " + anAttribute.getType());
            if (anAttribute.getType() == null) continue;
        
            String aliasName = uid2ClassName.getProperty(uidValue);

            if (aliasName != null && !aliasName.isEmpty() && !typeName.equals(aliasName)) typeName = aliasName;
            
            if (debugAttributes.equals("true"))
            {
                pw.println("# Kind is " + anAttribute.getAttributeKind());
                pw.println("\"\"\"");
                pw.println(anAttribute.ToString());
                pw.println("///           Alias : " + aliasName);
                pw.println("\"\"\"");
            }

            if ((anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
                (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD))
            {
                // pw.println("# Need to process the alias here");
                // This is the capability types UID
                if (uidValue.equals("55"))
                {
                    if (defaultValue != null)
                    {
                        defaultValue = removeFirstWord(defaultValue).replace("()", "");                        
                        // defaultValue += " {}";
                    }

                    String enumPath = StringUtils.removeFirstWord(enumNamespace, "\\.");
                    String pythonPath =  enumPath + "." + StringUtils.camelCasetoLowerUnderscore(defaultValue);
                    usingStatements.put(pythonPath, defaultValue);
                }

                if (aliasName != null)
                {
                    pythonImportName = StringUtils.camelCasetoLowerUnderscore(aliasName);
                }
                String pythonPath = StringUtils.removeFirstWord(enumNamespace, "\\.") + "." + pythonImportName;
                usingStatements.put(pythonPath, typeName);
            }

            // The object list may actually be a list of dis classes, dis enumerations, no great way of knowing
            // Could also be a list of system built in types
            // <objectlist countFieldName="numberOfRecords">
            //      <classRef name="RecordSpecificationElement"/>
            //      <sisoenum type="VariableRecordType" comment="uid = 66"/>

            if (anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST)
            {
                if ((anAttribute.underlyingKind == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM) ||
                    (anAttribute.underlyingKind == GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD))
                {
                    String pythonPath = StringUtils.removeFirstWord(enumNamespace, "\\.") + "." + pythonImportName;
                    usingStatements.put(pythonPath, typeName);   
                }
                else if (anAttribute.underlyingKind == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
                {
                    usingStatements.put(pythonImportName, typeName);
                }
                else
                {
                    pw.println("\"\"\"");
                    pw.println("# TODO : This is an OBJECT LIST, need to process it");
                    if (anAttribute.underlyingTypeIsClass)
                        pw.println("/// Class : \n" + anAttribute.ToString());
                    else if (anAttribute.underlyingTypeIsEnum)
                        pw.println("/// Enum : \n" + anAttribute.ToString());
                    else
                        pw.println("/// Unknown Type : \n" + anAttribute.ToString());
                    pw.println("\"\"\"");
                    pw.println();
                }
                
            }

            // If this attribute is a class, we need to do an import on that class
            if(anAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
            {                
                // pw.println(String.format("from %s import %s", anAttribute.getType(), anAttribute.getType()));
                usingStatements.put(pythonImportName, typeName);
            }

        }

        // if we inherit from another class we need to do an include on it
        if(!(aClass.getParentClass().equalsIgnoreCase("root")))
        {
            // pw.println("#include <" + namespace + aClass.getParentClass() + ".h>");
            // pw.println(String.format("from %s import %s", aClass.getParentClass(), aClass.getParentClass()));
            usingStatements.put(StringUtils.camelCasetoLowerUnderscore(aClass.getParentClass()), aClass.getParentClass());
        }

        for (Map.Entry<String,String> entry : usingStatements.entrySet())
        {

            pw.println(String.format("from %s import %s", "." + entry.getKey(), entry.getValue()));
            // pw.println("import " + entry.getKey() + " = " + entry.getValue() + ";");
            // pw.println("using " + entry.getKey + " = " entry.getValue() + ";");
        }

        // if (usingStatements.size() > 0)
        //     pw.println("");

    }
   
}
