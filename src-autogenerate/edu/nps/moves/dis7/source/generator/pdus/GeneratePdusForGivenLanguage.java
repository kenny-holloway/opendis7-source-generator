/**
 * Copyright (c) 2008-2025, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */
package edu.nps.moves.dis7.source.generator.pdus;

import edu.nps.moves.dis7.source.generator.pdus.GeneratedClassAttribute.ClassAttributeType;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * <p>
 * A class that reads an XML file in a specific format, and spits out Java, Python, C#,
 * C++, or Objective-C classes that do <i>most</i> of the work of the generating a 
 * DIS protocol API codebase.</p>
 *
 * <p>In effect, this program is reading an XML file and creating an abstract description 
 * for each of the protocol data units (PDUs). That abstract description is written out as 
 * source code in various languages, such as Java, Python, C++, etc.</p>
 *
 * <p>This program can rely on properties set in the XML file for the language. For example,
 * the Java element in the XML file can specify whether JAXB or Hibernate support
 * is included in the generated code.</p>
 * 
 * <p>There is a <i>huge</i> risk of using variable names that have ambiguous meaning
 * here, since many of the terms such as "class" are also used
 * by java or c++. Be careful and scrupulous out there!</p>
 *
 * @author Don McGregor, Mike Bailey and Don Brutzman
 */
public class GeneratePdusForGivenLanguage  // TODO rename? perhaps GeneratePdusByProgrammingLanguage
{
    // set defaults to allow direct run
    private static String programmingLanguage = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_PROGRAMMING_LANGUAGE;
    private static String         sisoXmlFile = "xml/dis_7_2012/DIS_7_2012.xml";
        
    // Elements and attributes we look for in our XML pdu description files:

    /** String constant */ public static final String INHERITSFROM = "inheritsFrom";
    /** String constant */ public static final String ALIASFOR = "aliasFor";
    /** String constant */ public static final String IMPLEMENTS = "implements";
    /** String constant */ public static final String XMLROOTELEMENT = "xmlRootElement";
    /** String constant */ public static final String SISOENUM = "sisoenum";
    /** String constant */ public static final String SISOBITFIELD = "sisobitfield";
    /** String constant */ public static final String CLASS = "class";
    /** String constant */ public static final String ATTRIBUTE = "attribute";
    /** String constant */ public static final String COMMENT = "comment";
    /** String constant */ public static final String INITIALVALUE = "initialvalue";
    /** String constant */ public static final String NAME = "name";
    /** String constant */ public static final String CLASSREF = "classref";
    /** String constant */ public static final String COUNTFIELDNAME = "countfieldname";
    /** String constant */ public static final String TYPE = "type";
    /** String constant */ public static final String DEFAULTVALUE = "defaultvalue";
    /** String constant */ public static final String PRIMITIVE = "primitive";
    /** String constant */ public static final String PRIMITIVELIST = "primitivelist";
    /** String constant */ public static final String OBJECTLIST = "objectlist";
    /** String constant */ public static final String LENGTH = "length";
    /** String constant */ public static final String FIXEDLENGTH = "fixedlength";
    /** String constant */ public static final String COULDBESTRING = "couldbestring";
    /** String constant */ public static final String TRUE = "true";
    /** String constant */ public static final String FALSE = "false";
    /** String constant */ public static final String VALUE = "value";
    /** String constant */ public static final String SERIALIZE = "serialize";
    /** String constant */ public static final String HIDDEN = "hidden";
    /** String constant */ public static final String SPECIALCASE = "specialCase";
    /** String constant */ //public static final String DOMAINHOLDER = "domainHolder";
    /** String constant */ public static final String PADTOBOUNDARY = "padtoboundary";
    /** String constant */ public static final String ABSTRACT = "abstract";
    /** String constant */ public static final String JAVA = "java";
    /** String constant */ public static final String CPP = "cpp";
    /** String constant */ public static final String OBJC = "objc";
    /** String constant */ public static final String CSHARP = "csharp";
    /** String constant */ public static final String JAVASCRIPT = "javascript";
    /** String constant */ public static final String PYTHON = "python";
    // Pending to investigate:
    /** String constant */ public static final String FLAG = "flag";
    /** String constant */ public static final String MASK = "mask";
    /** String constant */ public static final String STATICIVAR = "staticivar";
    
    /** Contains the database of all the classes described by the XML document */
    protected Map<String, GeneratedClass> generatedClassNames = new HashMap<>();
    
    /** The language types we might generate, currently only supporting Java */
    public enum LanguageType {
        /** C++ */
        CPP,
        /** Java */
        JAVA,
        /** C# */
        CSHARP,
        /** ObjectiveC */
        OBJECTIVEC,
        /** JavaScript (ECMAScript) */
        JAVASCRIPT,
        /** Python */
        PYTHON }
    
    /** As we parse the XML document, this is the class we are currently working on */
    private GeneratedClass currentGeneratedClass = null;
    
    /** As we parse the XML document, this is the current attribute */
    private GeneratedClassAttribute currentClassAttribute = null;

    /** As we parse the XML, lets hold the classAttribute from the object list so we can set the underlying type */
    private GeneratedClassAttribute currentListAttribute =  new GeneratedClassAttribute();
    
    // The languages may have language-specific properties, such as libraries that they
    // depend on. Each language has its own set of properties.
    
    /** Java properties--imports, packages, etc. */
    Properties javaProperties = new Properties();
    
    /** C++ properties--includes, etc. */
    Properties cppProperties = new Properties();
    
    /** C# properties--using, namespace, etc. */
    Properties csharpProperties = new Properties();

    /** Objective-C properties */
    Properties objcProperties = new Properties();
    
    /** JavaScript properties */
    Properties javascriptProperties = new Properties();

    /** source code generation options */
    Properties sourceGenerationOptions;
    
    /** source code generation for python */
    Properties pythonProperties = new Properties();
    
    /** Hash table of all the primitive types we can use (short, long, byte, etc.)*/
    private Set<String> primitiveTypes = new HashSet<>();
    
    /** Directory in which the java class package is created */
    private String javaDirectory = null;
    
    /** Directory in which the C++ classes are created */
    private String cppDirectory = null;
    
    //PES
    /** Directory in which the C# classes are created */
    private String csharpDirectory = null;

    /** Director in which the objc classes are created */
    private String objcDirectory = null;
    
    private int classCount = 0;   

    private boolean processingListObject = false;
   
    /**
     * Create a new collection of Java objects by reading an XML file; these
     * java objects can be used to generate code templates of any language,
     * once you write the translator for that language.
     * @param xmlDescriptionFileName file name
     * @param languageToGenerate programming language (e.g. Java, Python)
     */
    public GeneratePdusForGivenLanguage(String xmlDescriptionFileName, String languageToGenerate)
    {       
        try {
            DefaultHandler handler = new MyHandler();

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(true);
            factory.newSAXParser().parse(new File(xmlDescriptionFileName), handler);
        }
        catch (IOException | ParserConfigurationException | SAXException e) {
            System.out.println(e);
        }

        // System.out.println("GeneratePdusForGivenLanguage class list:\n");
        // for (GeneratedClass aClass : generatedClassNames.values())
        // {
        //     System.out.println("Listing class " + aClass.getName());
        // }

        // This does at least a cursory santity check on the data that has been read in from XML
        // It is far from complete.
        // if (!this.abstractSyntaxTreeIsPlausible()) {
        //     System.out.println("The generated XML file is not internally consistent according to astIsPlausible()");
        //     System.out.println("There are one or more errors in the XML file. See output for details.");
        //     System.exit(1);
        // }

        // generatedClassNames.put("PduStatus", new GeneratedClass());

        switch (languageToGenerate.toLowerCase()) {
            case JAVA:
                // System.out.println("putting java files in " + javaDirectory);
                JavaGenerator javaGenerator = new JavaGenerator(generatedClassNames, javaProperties);
                javaGenerator.writeClasses();
                break;

            // Use the same information to generate classes in C++
            case CPP:
                CppGenerator cppGenerator = new CppGenerator(generatedClassNames, cppProperties);
                cppGenerator.writeClasses();
                break;

            case CSHARP:
                // Create a new generator object to write out the source code for all the classes in csharp
                CsharpGenerator csharpGenerator = new CsharpGenerator(generatedClassNames, csharpProperties);
                csharpGenerator.writeClasses();
                break;

            case OBJC:
                // create a new generator object for objc
                ObjcGenerator objcGenerator = new ObjcGenerator(generatedClassNames, objcProperties);
                objcGenerator.writeClasses();
                break;

            case JAVASCRIPT:
                // create a new generator object for javascript
                JavascriptGenerator javascriptGenerator = new JavascriptGenerator(generatedClassNames, javascriptProperties);
                javascriptGenerator.writeClasses();
                break;

            case PYTHON:
                // create a new generator object for Python
                PythonGenerator pythonGenerator = new PythonGenerator(generatedClassNames, pythonProperties);
                pythonGenerator.writeClasses();
                break;
        }
    }

    /**
     * Entry point: pass in two arguments,
     * the XML file that describes the classes and the programming language API that you want to generate.
     * @param args arguments for xmlfile and programming language
     */
    public static void main(String args[])
    {
        programmingLanguage = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_PROGRAMMING_LANGUAGE; // JAVA_LANGUAGE PYTHON_LANGUAGE
        
        System.out.println (GeneratePdusForGivenLanguage.class.getName());
        if(args.length < 2 || args.length > 2)
        {
            System.out.print("Arguments: "); 
            if (args.length == 0)
                System.out.print ("none provided");
            for(String s : args)
                System.out.print (s);
            System.out.println();
            System.out.println("Usage: xmlFile language"); 
            System.out.println("Allowable language values are java, python, cpp, csharp, and objc");
            System.out.println("Continuing with GeneratePdus default values..."); 
//            System.exit(0);
        }
        else
        {
            if (!args[0].isEmpty())
                sisoXmlFile = args[0];
            if (!args[1].isEmpty())
                programmingLanguage = args[1];
        }
        programmingLanguage = programmingLanguage.toLowerCase();
        System.out.println (" sisoXmlFile=" + sisoXmlFile);
        System.out.println ("    language=" + programmingLanguage);
        
        checkArguments(sisoXmlFile, programmingLanguage);
        
        GeneratePdusForGivenLanguage generatePdusResult = new GeneratePdusForGivenLanguage(sisoXmlFile, programmingLanguage);  // includes simple list of PDUs
        System.out.println (GeneratePdusForGivenLanguage.class.getName() + " complete.");
    }
    
    /**
     * Does a sanity check on the args passed in: does the XML file exist, and is
     * the language valid.
     * @param xmlFile output file name
     * @param language programming language
     */
    public static void checkArguments(String xmlFile, String language)
    {
        try 
        {
            FileInputStream fis = new FileInputStream(xmlFile);
            fis.close();
            
            if(!(language.equalsIgnoreCase(JAVA)       || language.equalsIgnoreCase(CPP) ||
                 language.equalsIgnoreCase(OBJC)       || language.equalsIgnoreCase(CSHARP) ||
                 language.equalsIgnoreCase(JAVASCRIPT) || language.equalsIgnoreCase(PYTHON) ))
            {
                System.out.println("Not a valid language to generate. The options are java (supported), python (testing), and cpp, csharp, javascript and objc");
                System.out.println("Usage: GeneratePdus xmlFile language"); // formerly xmlpg
                System.exit(-1);
            }
        }
        catch (FileNotFoundException fnfe) 
        {
            System.err.println("XML file " + xmlFile + " not found. Please check the path and try again");
            System.err.println("Usage: GeneratePdus xmlFile language"); // formerly xmlpg
            fnfe.printStackTrace(System.err);
            System.exit(-1);
        }
        catch(IOException e)
        {
            System.err.println("Problem with arguments to GeneratePdus. Please check them."); // formerly xmlpg
            System.err.println("Usage: GeneratePdus xmlFile language"); 
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
    
    /*
     * Returns true if the information parsed from the protocol description XML file
     * is "plausible" in addition to being syntactically correct. This means that:
     * <ul>
     * <li>references to other classes in the file are correct; if a class attribute
     * refers to a "EntityID", there's a class by that name elsewhere in the document;
     * <li> The primitive types belong to a list of known correct primitive types,
     * eg short, unsigned short, etc
     *
     * AST is a reference to "abstract syntax tree", which this really isn't, but
     * sort of is.
     * 10 Sep 2019, I think this method can be removed.  If a class is missing, it will
     * show up soon enough when the products are compiled.  It's had to be hacked to
     * get around some of the special cases.
     */
    private boolean abstractSyntaxTreeIsPlausible()
    { 
        // Create a list of primitive types we can use to check against

        primitiveTypes.add("uint8");
        primitiveTypes.add("uint16");
        primitiveTypes.add("uint32");
        primitiveTypes.add("uint64");
        primitiveTypes.add("int8");
        primitiveTypes.add("int16");
        primitiveTypes.add("int32");
        primitiveTypes.add("int64");
        primitiveTypes.add("float32");
        primitiveTypes.add("float64");
        
        // A temporary hack to get past the tests below
        generatedClassNames.put("JammerKind",new GeneratedClass());
        generatedClassNames.put("JammerCategory",new GeneratedClass());
        generatedClassNames.put("JammerSubCategory",new GeneratedClass());
        generatedClassNames.put("JammerSpecific",new GeneratedClass());
        generatedClassNames.put("EntityCapabilities", new GeneratedClass());
        generatedClassNames.put("PduStatus", new GeneratedClass());
        generatedClassNames.put("Domain", new GeneratedClass());
        
        // trip through every class specified
        for (GeneratedClass aClass : generatedClassNames.values())
        {
            // Trip through every class attribute in this class and confirm that the type is either a primitive or references
            // another class defined in the document.
            for(GeneratedClassAttribute anAttribute : aClass.getClassAttributes())
            {
                GeneratedClassAttribute.ClassAttributeType kindOfNode = anAttribute.getAttributeKind();
                
                // The primitive type is on the known list of primitives.
                if(kindOfNode == GeneratedClassAttribute.ClassAttributeType.PRIMITIVE)
                {
                    if(primitiveTypes.contains(anAttribute.getType()) == false)
                    {
                        System.out.println("Cannot find a primitive type of " + anAttribute.getType() + " in class " + aClass.getName());
                        return false;
                    }
                }
                // The class referenced is available elsewehere in the document
                if(kindOfNode == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
                {
                    if(generatedClassNames.get(anAttribute.getType()) == null)
                    {
                        if(!anAttribute.getType().equals("Object")) {
                            System.out.println("Makes reference to a class of name " + anAttribute.getType() + " in class " + aClass.getName() + " but no user-defined class of that type can be found in the document");
                            return false;
                        }
                    }
                }
            } // end of trip through one class' attributes
            
            // Run through the list of initial values, ensuring that the initial values mentioned actually exist as attributes
            // somewhere up the inheritance chain.
            
            for(GeneratedInitialValue anInitialValue : aClass.getInitialValues()) {          
                GeneratedClass currentClass = aClass;
                boolean found = false;
                
                //System.out.println("----Looking for matches of inital value " + anInitialValue.getVariable());
                while(currentClass != null)
                {
                    List attributesForCurruentClass = currentClass.getClassAttributes();
                    for(GeneratedClassAttribute anAttribute : currentClass.getClassAttributes()) {
                        //System.out.println("--checking " + anAttribute.getName() + " against inital value " + anInitialValue.getVariable());
                        if(anInitialValue.getVariable().equals(anAttribute.getName()))
                        {
                            found = true;
                            break;
                        }
                    }
                    currentClass = generatedClassNames.get(currentClass.getParentClass());
                }
                if(!found)
                {
                    System.out.println("Could not find initial value matching attribute name for " + anInitialValue.getVariable() + " in class " + aClass.getName());
                }
            } // end of for loop through initial values

        } // End of trip through classes
        generatedClassNames.remove("JammerKind");
        generatedClassNames.remove("JammerCategory");
        generatedClassNames.remove("JammerSubCategory");
        generatedClassNames.remove("JammerSpecific");
        generatedClassNames.remove("EntityCapabilities");
        generatedClassNames.remove("PduStatus");
        generatedClassNames.remove("Domain");

        return true;
    } // end of abstractSyntaxTreeIsPlausible

  /** XML handler for recursively reading information and autogenerating code, namely an
     * inner class that handles the SAX parsing of the XML file. This is relatively simple, if
     * a little verbose. Basically we just create the appropriate objects as we come across the
     * XML elements in the file.
     */
    public class MyHandler extends DefaultHandler
    {
        /** default constructor */
        public MyHandler()
        {
            super();
        }
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
        {
            // Lanaguage-specific elements. All the properties needed to generate code specific
            // to a language should be included in the properties list for that language.
            Properties props=null;
            switch(qName) {
                case JAVA:
                    props = javaProperties;
                    break;
                case CPP:
                    props = cppProperties;
                    break;
                case CSHARP:
                    props = csharpProperties;
                    break;
                case JAVASCRIPT:
                    props = javascriptProperties;
                    break;
                case OBJC:
                    props = objcProperties;
                    break;
                case PYTHON:
                    props = pythonProperties;
                    break;
            }
            if(props != null)
              for(int idx = 0; idx < attributes.getLength(); idx++) {
                  props.setProperty(attributes.getQName(idx), attributes.getValue(idx));
              }
//  System.out.println("    ++ startElement");
//  System.out.println("          Qname : " + qName.toLowerCase());
//  System.out.println("           name : " + attributes.getValue("name"));
//  System.out.println("        comment : " + attributes.getValue("comment"));
// System.out.println("           name : " + attributes.getValue("name"));

            switch (qName.toLowerCase()) {

                case CLASS:                  
                    handleClass(attributes);
                    break;
                    
                case INITIALVALUE:
                    handleInitialValue(attributes);
                    break;
                    
                case ATTRIBUTE:
                    handleAttribute(attributes);
                    break;

                case FLAG:
                    handleFlag(attributes);
                    break;

                case PRIMITIVE:
                    handlePrimitive(attributes);
                    break;
                    
                case CLASSREF:
                    handleClassRef(attributes);
                    break;
                    
                case SISOENUM:
                    handleSisoEnum(attributes);
                    break;
 
                case SISOBITFIELD:
                    handleSisoBitfield(attributes);
                    break;
                    
                case PADTOBOUNDARY:
                    handlePadToBoundary(attributes);
                    break;
                    
                case STATICIVAR:
                    handleStaticIvar(attributes);
                    break;
                    
                case OBJECTLIST:
                    processingListObject = true;
                    handleObjectList(attributes);
                    break;

                case PRIMITIVELIST:
                    processingListObject = true;
                    handlePrimitiveList(attributes);
                    break;
            }
            // System.out.println("    -- startElement");
        } // end of startElement
        
        @Override
        public void endElement(String uri, String localName, String qName)
        {
            // We've reached the end of a class element. The class should be complete; add it to the hash table.
            switch (qName.toLowerCase()) {
                case CLASS:
                    classCount--;
                    //System.out.println("classCount is " + classCount);
                    //System.out.println("---#End of class" + currentGeneratedClass.getName());
                    generatedClassNames.put(currentGeneratedClass.getName(), currentGeneratedClass);
                    break;
                case ATTRIBUTE:
                    //System.out.println("     end attribute");
                    currentGeneratedClass.addClassAttribute(currentClassAttribute);
                    break;
            }

        }

        private void handleAttribute(Attributes attributes)
        {
            currentClassAttribute = new GeneratedClassAttribute();

            // Attributes on the attribute tag.
            for (int idx = 0; idx < attributes.getLength(); idx++) {
                switch (attributes.getQName(idx).toLowerCase()) {
                    case NAME:
                        currentClassAttribute.setName(attributes.getValue(idx));
                        break;
                    case COMMENT:
                        currentClassAttribute.setComment(attributes.getValue(idx));
                        break;
                    case SERIALIZE:
                        if (attributes.getValue(idx).toLowerCase().equals(FALSE))
                            currentClassAttribute.shouldSerialize = false;
                        break;
                    case HIDDEN:
                        String tf = attributes.getValue((idx));
                        currentClassAttribute.setHidden(Boolean.parseBoolean(tf));
                        break;
                }

            }
        }
       
        private void handleInitialValue(Attributes attributes)
        {
            String anAttributeName = null;
            String anInitialValue = null;

            // Attributes on the initial value tag
            for (int idx = 0; idx < attributes.getLength(); idx++) {
                switch (attributes.getQName(idx).toLowerCase()) {
                    case NAME:
                        anAttributeName = attributes.getValue(idx);
                        break;
                    case VALUE:
                        anInitialValue = attributes.getValue(idx);
                        break;
                }
            }
            if ((anAttributeName != null) && (anInitialValue != null)) {
                GeneratedInitialValue aValue = new GeneratedInitialValue(anAttributeName, anInitialValue);
                currentGeneratedClass.addInitialValue(aValue);
            }
        }
        
        private void handleClass(Attributes attributes)
        {
            classCount++;
            //System.out.println("classCount is" + classCount);

            currentGeneratedClass = new GeneratedClass();

            // The default is that this inherits from Object
            currentGeneratedClass.setParentClass("root");

            // Trip through all the attributes of the class tag
            for (int idx = 0; idx < attributes.getLength(); idx++) {
                switch (attributes.getQName(idx)) {

                    case NAME:// class name
                        //System.out.println("--->Processing class named " + attributes.getValue(idx));
                        currentGeneratedClass.setName(attributes.getValue(idx));
                        break;

                    case COMMENT:// Class comment

                        //System.out.println("comment is " + attributes.getValue(idx));
                        currentGeneratedClass.setComment(attributes.getValue(idx));
                        break;

                    case INHERITSFROM:// Inherits from
                        //System.out.println("inherits from " + attributes.getValue(idx));
                        currentGeneratedClass.setParentClass(attributes.getValue(idx));
                        break;
                        
                    case ALIASFOR: // write empty subclass
                        currentGeneratedClass.setAliasFor(attributes.getValue(idx));
                        break;
                        
                    case IMPLEMENTS:
                        currentGeneratedClass.setInterfaces(attributes.getValue(idx));
                        break;
                        
                    case XMLROOTELEMENT:// XML root element--used for marshalling to XML with JAXB

                        //System.out.println("is root element " + attributes.getValue(idx));
                        if (attributes.getValue(idx).equalsIgnoreCase(TRUE)) {
                            currentGeneratedClass.setXmlRootElement(true);
                        }
                    // by default it is false unless specified otherwise
                        break;
                        
                    case SPECIALCASE:
                        currentGeneratedClass.setSpecialCase(attributes.getValue(idx));
                        break;
                        
                    case ABSTRACT:
                        currentGeneratedClass.setAbstract(attributes.getValue(idx));
                        break;
                }
            }
        }
        
        private void handleStaticIvar(Attributes attributes)
        {
            currentClassAttribute.setAttributeKind(GeneratedClassAttribute.ClassAttributeType.STATIC_IVAR);
            for (int idx = 0; idx < attributes.getLength(); idx++) {
                switch (attributes.getQName(idx).toLowerCase()) {
                    case TYPE:
                        currentClassAttribute.setType(attributes.getValue(idx));
                        break;
                    case VALUE:
                        currentClassAttribute.setDefaultValue(attributes.getValue(idx));
                        break;
                }
            }
        }
        
        private void handleFlag(Attributes attributes)
        {
            String flagName = null;
            String flagComment = null;
            String flagMask = "0";

            for (int idx = 0; idx < attributes.getLength(); idx++) {
                // Name of class attribute
                switch (attributes.getQName(idx).toLowerCase()) {
                    case NAME:
                        flagName = attributes.getValue(idx);
                        break;
                    case COMMENT:
                        flagComment = attributes.getValue(idx);
                        break;
                    case MASK:
                        // Should parse "0x80" or "31" equally well.
                        String text = attributes.getValue(idx);
                        flagMask = text;
                        break;
                }
            }
            GeneratedBitField bitField = new GeneratedBitField(flagName, flagMask, flagComment, currentClassAttribute);
            currentClassAttribute.bitFieldList.add(bitField);
        }
        
        private void handlePrimitive(Attributes attributes)
        {
            if (currentClassAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.UNSET) 
                currentClassAttribute.setAttributeKind(GeneratedClassAttribute.ClassAttributeType.PRIMITIVE);
         
            currentClassAttribute.setUnderlyingTypeIsPrimitive(true);

            if (processingListObject == true)
            {
                //Add the kind to the list type
                currentListAttribute.underlyingKind = GeneratedClassAttribute.ClassAttributeType.PRIMITIVE;
                processingListObject = false;
            }

            for (int idx = 0; idx < attributes.getLength(); idx++) {
                switch (attributes.getQName(idx).toLowerCase()) {
                    case TYPE:
                        currentClassAttribute.setType(attributes.getValue(idx));
                        break;
                    case DEFAULTVALUE:
                        currentClassAttribute.setDefaultValue(attributes.getValue(idx));
                        break;
                }
            }
        }
        
        private void handleClassRef(Attributes attributes)
        {
            if (processingListObject == true)
            {
                //Add the kind to the list type
                currentListAttribute.underlyingKind = GeneratedClassAttribute.ClassAttributeType.CLASSREF;
                processingListObject = false;
            }

            // The classref may occur inside a List element; if that's the case, we want to 
            // respect the existing list type.
            if (currentClassAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.UNSET) {
                currentClassAttribute.setAttributeKind(GeneratedClassAttribute.ClassAttributeType.CLASSREF);
                currentClassAttribute.setUnderlyingTypeIsPrimitive(false);
            }
            else if (currentClassAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.SISO_ENUM)
                currentClassAttribute.setUnderlyingTypeIsEnum(true);

            for (int idx = 0; idx < attributes.getLength(); idx++) {
                switch (attributes.getQName(idx).toLowerCase()) {
                    case NAME:
                        currentClassAttribute.setType(attributes.getValue(idx));
                        break;
                    case "initialclass":
                        currentClassAttribute.setInitialClass(attributes.getValue(idx));
                        break;
                    case DEFAULTVALUE:
                        currentClassAttribute.setDefaultValue(attributes.getValue(idx));
                        break;                        
                }
            }
        }
        
        private void handleSisoEnum(Attributes attributes)
        {
            currentClassAttribute.setUnderlyingTypeIsEnum(true);

            if (currentClassAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.UNSET) {
                currentClassAttribute.setAttributeKind(GeneratedClassAttribute.ClassAttributeType.SISO_ENUM);
                currentClassAttribute.setUnderlyingTypeIsPrimitive(false);
            }

            if (processingListObject == true)
            {
                //Add the kind to the list type
                currentListAttribute.underlyingKind = GeneratedClassAttribute.ClassAttributeType.SISO_ENUM;
                processingListObject = false;
            }

            for (int idx = 0; idx < attributes.getLength(); idx++) {
                String attributeName = attributes.getQName(idx);

                String flagName = null;

                switch (attributes.getQName(idx).toLowerCase()) {
                    case NAME:
                        flagName = attributes.getValue(idx);
                        break;
                    case TYPE:
                        currentClassAttribute.setType(attributes.getValue(idx));
                        break;
                    case COMMENT:
                        String s = currentClassAttribute.getComment();

                        // if comment starts with uid x, lets pull the uid value for later alias lookups
                        String attributeComment = attributes.getValue("comment").toLowerCase();
                        if (attributeComment.indexOf("uid") != -1)
                        {
                            attributeComment = attributeComment.replaceAll("uid", "").replaceAll("\\s", "");
                            currentClassAttribute.setUnderlyingUid(attributeComment);
                        }
                        currentClassAttribute.setComment((s==null?"":s)+" "+attributes.getValue(idx));
                        break;
                    case DEFAULTVALUE:
                        currentClassAttribute.setDefaultValue(attributes.getValue(idx));
                        break;
                }
            }
        }

       private void handleSisoBitfield(Attributes attributes)
        {
            if (currentClassAttribute.getAttributeKind() == GeneratedClassAttribute.ClassAttributeType.UNSET) {
                currentClassAttribute.setAttributeKind(GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD);
                currentClassAttribute.setUnderlyingTypeIsPrimitive(false);
            }

            if (processingListObject == true)
            {
                //Add the kind to the list type
                currentListAttribute.underlyingKind = GeneratedClassAttribute.ClassAttributeType.SISO_BITFIELD;
                processingListObject = false;
            }

            currentClassAttribute.setIsBitField(true);

            for (int idx = 0; idx < attributes.getLength(); idx++) {
                String nm = attributes.getQName(idx);
                switch (nm.toLowerCase()) {
                    case TYPE:
                        currentClassAttribute.setType(attributes.getValue(idx));
                        break;
                    case COMMENT:
                        String s = currentClassAttribute.getComment();
                        // if comment starts with uid x, lets pull the uid value for later alias lookups
                        String attributeComment = attributes.getValue("comment").toLowerCase();
                        if (attributeComment.indexOf("uid") != -1)
                        {
                            attributeComment = attributeComment.replaceAll("uid", "").replaceAll("\\s", "");
                            currentClassAttribute.setUnderlyingUid(attributeComment);
                        }
                        currentClassAttribute.setComment((s==null?"":s)+" "+attributes.getValue(idx));
                        break;
                    case DEFAULTVALUE:
                        currentClassAttribute.setDefaultValue(attributes.getValue(idx));
                        break;
                }
            }
        }
        private void handlePadToBoundary(Attributes attributes)
        {
            for (int idx = 0; idx < attributes.getLength(); idx++) {
                String nm = attributes.getQName(idx);
                switch (nm.toLowerCase()) {
                    case LENGTH:
                        switch (attributes.getValue(idx)) {
                            case "16":
                                currentClassAttribute.setAttributeKind(GeneratedClassAttribute.ClassAttributeType.PADTO16);
                                break;
                            case "32":
                                currentClassAttribute.setAttributeKind(GeneratedClassAttribute.ClassAttributeType.PADTO32);
                                break;
                            case "64":
                                currentClassAttribute.setAttributeKind(GeneratedClassAttribute.ClassAttributeType.PADTO64);
                                break;
                            default:
                                System.err.println("Unrecognized value for padtoboundary length attribute: "+attributes.getValue(idx));
                                break;
                            }
                        break;
                    default:
                        System.err.println("Unrecognized attribute to padtoboundary element: "+nm);
                }
            }
        }
/*       
    <attribute name="mineLocation" comment="Mine locations">
        <objectlist countFieldName="numberOfMinesInThisPdu">
            <classRef name="Vector3Float"/>
        </objectlist>
    </attribute>
*/
        private void handleObjectList(Attributes attributes)
        {
            // processingListObject = true;
            currentClassAttribute.setAttributeKind(GeneratedClassAttribute.ClassAttributeType.OBJECT_LIST);

            currentListAttribute = currentClassAttribute;

            for (int idx = 0; idx < attributes.getLength(); idx++) {
                // Variable list length fields require a name of another field that contains how many
                // list items there are. This is used in unmarshalling.
                if (attributes.getQName(idx).equalsIgnoreCase(TYPE))
                {
                    // Could this be the type of the data in the object list?
                    System.out.println("QNAME TYPE : " + attributes.getValue(idx));
                }

                // if(kindOfNode == GeneratedClassAttribute.ClassAttributeType.CLASSREF)
                if (attributes.getQName(idx).equalsIgnoreCase(COUNTFIELDNAME)) {
                    currentClassAttribute.setCountFieldName(attributes.getValue(idx));

                    // We also want to inform the attribute associated with countFieldName that
                    // it is keeping track of a list--this modifies the getter method and
                    // eliminates the setter method. This code assumes that the count field
                    // attribute has already been processed.
                    List ats = currentGeneratedClass.getClassAttributes();
                    backReferenceCountField(attributes, ats, idx, currentClassAttribute.getAttributeKind());/*
                    boolean atFound = false;

                    for (int jdx = 0; jdx < ats.size(); jdx++) {
                        GeneratedClassAttribute at = (GeneratedClassAttribute) ats.get(jdx);
                        if (at.getName().equals(attributes.getValue(idx))) {
                            at.setIsDynamicListLengthField(true);
                            at.setDynamicListClassAttribute(currentClassAttribute);
                            atFound = true;
                            break;
                        }
                    }
                    if (atFound == false) {
                        System.out.println("Could not find a matching attribute for the length field for " + attributes.getValue(idx));
                    } */
                }
            }
        }
      private void backReferenceCountField(Attributes attributes, List ats, int idx, ClassAttributeType lstType)
      {
        boolean attributeFound = false;

        for (int jdx = 0; jdx < ats.size(); jdx++)
        {
            GeneratedClassAttribute at = (GeneratedClassAttribute) ats.get(jdx);
            if (at.getName().equals(attributes.getValue(idx))) 
            {
                at.setIsDynamicListLengthField(lstType == ClassAttributeType.OBJECT_LIST);
                at.setIsPrimitiveListLengthField(lstType == ClassAttributeType.PRIMITIVE_LIST);
                at.setDynamicListClassAttribute(currentClassAttribute);
                attributeFound = true;

                break;
            }
        }
        if (attributeFound == false)
        {
          System.out.println("Could not find a matching attribute for the length field for " + attributes.getValue(idx));
        }
      }
      
    private void handlePrimitiveList(Attributes attributes)
    {
        currentClassAttribute.setAttributeKind(GeneratedClassAttribute.ClassAttributeType.PRIMITIVE_LIST);
        currentListAttribute = currentClassAttribute;

        for (int idx = 0; idx < attributes.getLength(); idx++) 
        {
            String attributeName = attributes.getQName(idx).toLowerCase();
            switch (attributes.getQName(idx).toLowerCase())
            {
                case COULDBESTRING:
                    if (attributes.getValue(idx).equalsIgnoreCase(TRUE))
                      currentClassAttribute.setCouldBeString(true);
                    break;

                case LENGTH:
                    String length = attributes.getValue(idx);
                    try {
                      int listLen = Integer.parseInt(length);
                      currentClassAttribute.setListLength(listLen);
                    }
                    catch (NumberFormatException e) {
                      System.out.println("Invalid list length found. Bad format for integer " + length);
                      currentClassAttribute.setListLength(0);
                    }
                    break;

                case FIXEDLENGTH:
                    currentClassAttribute.setFixedLength(Boolean.parseBoolean(attributes.getValue(idx)));
                    break;

                case COUNTFIELDNAME:
                    currentClassAttribute.setCountFieldName(attributes.getValue(idx));
                    backReferenceCountField(attributes, currentGeneratedClass.getClassAttributes(), idx, currentClassAttribute.getAttributeKind());
                    break;

                default:
                    currentClassAttribute.setListLength(0); //Apr8
                    break;
              }
            }
        }
    }
}
