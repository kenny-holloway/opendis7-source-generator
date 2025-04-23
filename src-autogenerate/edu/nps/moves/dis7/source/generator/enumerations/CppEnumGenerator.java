package edu.nps.moves.dis7.source.generator.enumerations;

import java.io.*;
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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class CppEnumGenerator extends AbstractEnumGenerator
{
    private Properties uid4aliases;
    private Properties cppEnumAliases;
    
    private Set<String> uidDoNotGenerate;
    // private Map<String,String> uid2ExtraInterface;
    // private Map<String, String> capabilityNames = new HashMap<>();

    private static String       sisoSpecificationTitleDate = "";

    // https://stackoverflow.com/questions/11883043/does-an-enum-class-containing-20001-enum-constants-hit-any-limit
    final int MAX_ENUMERATIONS = 4096;

    private int additionalEnumClassesCreated = 0;
    
    private String        packageInfoPath;
    private File          packageInfoFile;
    private StringBuilder packageInfoBuilder;

    String globalNamespace = "";
    String enumNamespace = "";

    // Not using defaults, parameters are checked on the way in
    public CppEnumGenerator(String xmlFile, String outputDir, String packagename)
    {
        // Thread.currentThread().dumpStack();

        this();

        System.out.println (CppEnumGenerator.class.getName());
        sisoXmlFile = xmlFile;
        outputDirectoryPath = outputDir;
        CppEnumGenerator.packageName = packageName;

        try {
            Properties systemProperties = System.getProperties();

            globalNamespace = systemProperties.getProperty("xmlpg.namespace");
            if (globalNamespace == null)
                globalNamespace = "";

            // set global namespace for enums
            enumNamespace = systemProperties.getProperty("xmlpg.enumNamespace");
            if (enumNamespace == null)
                enumNamespace = "";
        }
        catch (Exception e) {
            System.err.println("Required property not set. Modify the XML file to include the missing property");
            System.err.println(e);
            System.exit(-1);
        }

         outputDirectory  = new File(outputDirectoryPath);
         outputDirectory.mkdirs();
    }

   public CppEnumGenerator()
   {
      super(CPP);
   }

   public void writeEnums()
   {

        System.out.println (CppEnumGenerator.class.getName());
        System.out.println("Creating C++ Enumerations");

        System.out.println ("              xmlFile = " + sisoXmlFile);
        System.out.println ("          packageName = " + CppEnumGenerator.packageName);
        System.out.println ("  outputDirectoryPath = " + outputDirectoryPath);
        System.out.println ("actual directory path = " + outputDirectory.getAbsolutePath());
        
        try{
            writeLanguageEnums();
            writeCpabilityFiles();
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            ex.printStackTrace(System.err);
        }

        System.out.println("Finished creating C++ Enumerations");
        
   }

    public void addCapabilityIncludeStatements(StringBuilder sb)
    {
        // Add capabiity include files
        for (Map.Entry<String, String> entry : uid2ExtraInterface.entrySet())
        {
            String uidValue = entry.getKey();
            String aliasName = uid2ClassName.getProperty(uidValue);
            if (aliasName != null)
                sb.append("#include <" + globalNamespace + "/enumerations/" + aliasName + ".h>\n");
        }
    }

    private void outputCapabilityUnion(StringBuilder sb)
    {
        sb.append("struct Capabilities\n");
        sb.append("{\n");
        sb.append("    enum class Type {");

        String unionStatement = "";
        // Add types
        for (Map.Entry<String, String> entry : capabilityNames.entrySet())
        {
            String enumName = parseToFirstCap(entry.getValue()).toUpperCase();
            unionStatement += String.format("%s,", enumName);
        }
        unionStatement = removeLastCharacter(unionStatement);
        sb.append(unionStatement);
        sb.append("};\n");

        sb.append("    Type type;\n");

        sb.append("    union\n");
        sb.append("    {\n");

        for (Map.Entry<String, String> entry : capabilityNames.entrySet())
        {
            String capName  = entry.getValue();
            sb.append(String.format("        %s %s;\n", capName, initialLower(capName)));
        }
        sb.append("    };\n");    // end the union
        sb.append("\n");

        // Generate the constructors
        for (Map.Entry<String, String> entry : capabilityNames.entrySet())
        {
            String uidValue = entry.getKey();
            String capName  = entry.getValue();
            String enumName = parseToFirstCap(entry.getValue()).toUpperCase();
            sb.append(String.format("    Capabilities(const %s& capValue) : type{Type::%s} { %s = capValue; }\n",
                capName, enumName, initialLower(capName)));
        }

        sb.append("};\n");   // end the struct

    }

    public void writeCapabilityHeaderFile()
    {
        String name = "Capabilities";
        StringBuilder sb = new StringBuilder();

        // Write the usual #ifdef to prevent multiple inclusions by the preprocessor
        sb.append("#ifndef " + name.toUpperCase() + "_H\n");
        sb.append("#define " + name.toUpperCase() + "_H\n");
        sb.append("\n");

        addCapabilityIncludeStatements(sb);

        sb.append("\n");
        sb.append(formatNamespaceStatement(enumNamespace));
        sb.append("\n");

        outputCapabilityUnion(sb);
        sb.append("\n");

        sb.append("std::string to_string(const Capabilities& cp);\n");
        sb.append("\n");

        sb.append(formatNamespaceEndStatement(enumNamespace));
        sb.append("\n");

        sb.append("#endif // " + name.toUpperCase() + "_H\n");

        File targetFile = new File(outputDirectory, name + ".h");
        OutputStreamWriter targetFileWriter;
        try {
            targetFile.createNewFile();
            FileOutputStream fso = new FileOutputStream(targetFile);
            targetFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
            targetFileWriter.write(sb.toString());
            targetFileWriter.flush();
            targetFileWriter.close();
        }
        catch (IOException ex) {
            System.out.flush();
            System.err.println (ex.getMessage() + " targetFile.getAbsolutePath()=" + targetFile.getAbsolutePath());
            ex.printStackTrace(System.err);
        }

    }

    public void writeCapabilityToString(StringBuilder sb)
    {
        String unionStatement = "";

        sb.append("std::string to_string(const Capabilities& cp)\n");
        sb.append("{\n");

        sb.append("    std::string outputString = \"\";\n");
        sb.append("\n");
        sb.append("    switch(cp.type)\n");
        sb.append("    {\n");

        // Add case for each type
        for (Map.Entry<String, String> entry : capabilityNames.entrySet())
        {
            String enumName = parseToFirstCap(entry.getValue()).toUpperCase();
            sb.append(String.format("        case Capabilities::Type::%s:\n", enumName));
            sb.append(String.format("            outputString = to_string(cp.%s);\n", initialLower(entry.getValue())));
            sb.append("            break;\n");
            // unionStatement += String.format("%s,", enumName);
        }


        sb.append("    }\n");
        sb.append("\n");

        sb.append("    return outputString;\n");
        sb.append("}\n");
    }

    public void writeCapabilityCppFile()
    {

        String name = "Capabilities";
        StringBuilder sb = new StringBuilder();

        sb.append("#include <" + globalNamespace + "/enumerations/" + name + ".h>\n");

        sb.append("\n");
        sb.append(formatNamespaceStatement(enumNamespace));
        sb.append("\n");

        writeCapabilityToString(sb);
        sb.append("\n");

        sb.append(formatNamespaceEndStatement(enumNamespace));

        File targetFile = new File(outputDirectory, name + ".cpp");
        OutputStreamWriter targetFileWriter;
        try {
            targetFile.createNewFile();
            FileOutputStream fso = new FileOutputStream(targetFile);
            targetFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
            targetFileWriter.write(sb.toString());
            targetFileWriter.flush();
            targetFileWriter.close();
        }
        catch (IOException ex) {
            System.out.flush();
            System.err.println (ex.getMessage() + " targetFile.getAbsolutePath()=" + targetFile.getAbsolutePath());
            ex.printStackTrace(System.err);
        }

    }

   public void writeCpabilityFiles()
   {
        System.out.println("+++ GENERATE THE CAPABILITIES[.h.cpp] file sets");

        try
        {
            writeCapabilityHeaderFile();
            writeCapabilityCppFile();
        }
        catch(Exception e)
        {
            System.out.println("error creating source code " + e);
        }
   }



   private void writeLanguageEnums() throws SAXException, IOException, ParserConfigurationException
   {
     
        // Manual:
        uid2ClassName = new Properties();
        uid2ClassName.load(getClass().getResourceAsStream("Uid2ClassName.properties"));
        uid4aliases = new Properties();
        uid4aliases.load(getClass().getResourceAsStream("uid4aliases.properties"));

        cppEnumAliases = new Properties();
        cppEnumAliases.load(getClass().getResourceAsStream("cppEnumAlias.properties"));

        // Final:
        uidClassNames = new HashMap<>();

        interfaceInjection = new Properties();
        interfaceInjection.load(getClass().getResourceAsStream("interfaceInjection.properties"));
        loadEnumTemplates();

        // These 2 are to support the special case of uid 55, where each enum row should be a BitField
        // TBD: figure out how to do this through the normal methods
       // uidDoNotGenerate = new HashSet<>();
       // uidDoNotGenerate.add("55");  // Entity Capabilities
        
        // uid2ExtraInterface = new HashMap<>();
        // uid2ExtraInterface.put("450", "EntityCapabilities"); //Land Platform Entity Capabilities
        // uid2ExtraInterface.put("451", "EntityCapabilities");
        // uid2ExtraInterface.put("452", "EntityCapabilities");
        // uid2ExtraInterface.put("453", "EntityCapabilities");
        // uid2ExtraInterface.put("454", "EntityCapabilities");
        // uid2ExtraInterface.put("455", "EntityCapabilities");
        // uid2ExtraInterface.put("456", "EntityCapabilities");
        // uid2ExtraInterface.put("457", "EntityCapabilities");
        // uid2ExtraInterface.put("458", "EntityCapabilities");
        // uid2ExtraInterface.put("459", "EntityCapabilities");
        // uid2ExtraInterface.put("460", "EntityCapabilities");
        // uid2ExtraInterface.put("461", "EntityCapabilities");
        // uid2ExtraInterface.put("462", "EntityCapabilities"); //Sensor/Emitter Entity Capabilities
    
        /*
        for (Entry<Object, Object> ent : uid2ClassName.entrySet()) {
            System.out.println(ent.getKey() + " " + ent.getValue());
        }
         */
        File xmlFile = new File(sisoXmlFile);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(true);
        System.out.println("Begin uid preprocess...");
        factory.newSAXParser().parse(xmlFile,new UidCollector());

        System.out.println("Begin enumeration generation...");
        MyHandler handler = new MyHandler();
        factory.newSAXParser().parse(xmlFile, handler); // apparently can't reuse xmlFile

        System.out.println (CppEnumGenerator.class.getName() + " complete, " + (handler.enums.size() + additionalEnumClassesCreated) + " enum classes created.");
   }

    /**
    * Replace special characters in name with underscore _ character
    * @param name name value (typically from XML)
    * @return normalized name
    */
    public final static String fixName(String name)
    {
        if ((name==null) || name.trim().isEmpty())
        {
            System.out.flush();
            System.err.println("fixName() found empty name... replaced with \"undefinedName\"");
            return "undefinedName";
        }
        // https://stackoverflow.com/questions/14080164/how-to-replace-a-string-in-java-which-contains-dot/14080194
        // https://stackoverflow.com/questions/2932392/java-how-to-replace-2-or-more-spaces-with-single-space-in-string-and-delete-lead
        String newName = name.trim().replaceAll(",", " ").replaceAll("—"," ").replaceAll("-", " ").replaceAll("\\."," ").replaceAll("&"," ")
                                    .replaceAll("/"," ").replaceAll("\"", " ").replaceAll("\'", " ").replaceAll("( )+"," ").replaceAll(" ", "_");
        newName = newName.replaceAll("_",""); // no underscore divider
        if (newName.contains("__"))
        {
            System.out.flush();
            System.err.println("fixname: " + newName);
            newName = newName.replaceAll("__", "_");
        }
        return newName;
    }

    /** cleanup special characters in string
    *  @param s input string
    *  @return output string */
    public final static String htmlize(String s)
    {
        return s.replace("&","and").replace("&","and");
    }

   

    

    class EnumElem
    {
        String uid;
        String name;
        String size;
        String footnote;
        List<EnumRowElem> elems                 = new ArrayList<>();
    } 

    class EnumRowElem
    {
        String value;
        String description;
        String footnote;
        String xrefclassuid;
    }

    

    class BitfieldElem
    {
        String name;
        String size;
        String uid;
        List<BitfieldRowElem> elems = new ArrayList<>();
    }

    class BitfieldRowElem
    {
        String name;
        String bitposition;
        String length = "1"; // default
        String description;
        String xrefclassuid;
    }

    /** Utility class */
    public class UidCollector extends DefaultHandler
    {
        /** default constructor */
        public UidCollector()
        {
            super();
        }
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
        {
            switch (qName) {
                case "enum":
                case "bitfield":
                case "dict":
                    String uid = attributes.getValue("uid");
                    if (uid != null) {
                        String name = fixName(attributes.getValue("name")); // name canonicalization C14N
                        String name2 = CppEnumGenerator.this.uid2ClassName.getProperty(uid);
                        if(name2 != null)
                          uidClassNames.put(uid, name2);
                        else
                          uidClassNames.put(uid,name);
                    }
                default:
            }
        }
        private String makeLegalClassName(String s)
        {
            s = s.replace(" ","");
            s = s.replace(".", "");  // Check for other routines
            return s;
        }
    }

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
        List<EnumElem> enums = new ArrayList<>();
        EnumElem currentEnum;
        EnumRowElem currentEnumRow;

        List<DictionaryElem> dictionaries = new ArrayList<>();
        DictionaryElem currentDict;
        DictionaryRowElem currentDictRow;

        List<BitfieldElem> bitfields = new ArrayList<>();
        BitfieldElem currentBitfield;
        BitfieldRowElem currentBitfieldRow;

        Set<String> testElems = new HashSet<>();
/*
        private void maybeSysOut(String xref, String msg)
        {
            if (xref != null)
                System.out.println(msg + " xref=" + xref);
        }
*/
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
        {
            if (qName.equals("category") && "true".equals(attributes.getValue("deprecated")))
                return;

            switch (qName) {
                case "enum":
                    currentEnum = new EnumElem();
                    currentEnum.name = fixName(attributes.getValue("name")); // name canonicalization C14N
                    currentEnum.uid  = attributes.getValue("uid");
                    currentEnum.size = attributes.getValue("size");
                    currentEnum.footnote = attributes.getValue("footnote");
                    if (currentEnum.footnote != null)
                        currentEnum.footnote = normalizeDescription(currentEnum.footnote);
                    enums.add(currentEnum);
                    //maybeSysOut(attributes.getValue("xref"), "enum uid " + currentEnum.uid + " " + currentEnum.name);
                    break;

                case "meta":
                    if (currentEnum == null)
                        break;
                    if ((currentEnumRow.description == null) || currentEnumRow.description.isEmpty())
                    {
                        String   key = attributes.getValue("key");
                        String value = attributes.getValue("value");
                        if (key != null)
                            currentEnumRow.description = key.toUpperCase() + "_";
                        if (value != null)
                            currentEnumRow.description += value;
                        if (currentEnumRow.description != null)
                            currentEnumRow.description = normalizeDescription(currentEnumRow.description);
                    }
                    break;

                case "enumrow":
                    if (currentEnum == null)
                        break;
                    currentEnumRow = new EnumRowElem();
                    // TODO if description is empty, get meta key-value
                    currentEnumRow.description = attributes.getValue("description");
                    if (currentEnumRow.description != null)
                        currentEnumRow.description = normalizeDescription(currentEnumRow.description);
                    currentEnumRow.value = attributes.getValue("value");

/* special case, 2147483648 is one greater than max Java integer, reported 30 JAN 2022
    <enumrow value="2147483648" description="Rectangular Volume Record 4" group="1" status="hold" uuid="fdccf8e0-e73c-4137-b140-f7d0882b0778">
      <cr value="1913" />
    </enumrow>
*/            
            if (currentEnumRow.value.equals("2147483648"))
            {
                System.out.println ("*** Special case 'Rectangular Volume Record 4' value 2147483648 reset to 2147483647" +
                                    " in order to avoid exceeding max integer value");
                currentEnumRow.value = "2147483647";
            }
                    currentEnumRow.footnote = attributes.getValue("footnote");
                    if (currentEnum.footnote != null)
                        currentEnum.footnote = normalizeDescription(currentEnum.footnote);
                    currentEnumRow.xrefclassuid = attributes.getValue("xref");
                    currentEnum.elems.add(currentEnumRow);
                    //maybeSysOut(attributes.getValue("xref"), "enumrow uid " + currentEnum.uid + " " + currentEnum.name + " " + currentEnumRow.value + " " + currentEnumRow.description);
                    break;

                case "bitfield":
                    currentBitfield = new BitfieldElem();
                    currentBitfield.name = fixName(attributes.getValue("name")); // name canonicalization C14N
                    currentBitfield.size = attributes.getValue("size");
                    currentBitfield.uid = attributes.getValue("uid");
                    bitfields.add(currentBitfield);
                    //maybeSysOut(attributes.getValue("xref"), "bitfieldrow uid " + currentBitfield.uid + " " + currentBitfield.name);
                    break;

                case "bitfieldrow":
                    if (currentBitfield == null)
                        break;
                    currentBitfieldRow = new BitfieldRowElem();
                    currentBitfieldRow.name = fixName(attributes.getValue("name")); // name canonicalization C14N
                    currentBitfieldRow.description = attributes.getValue("description");
                    if (currentBitfieldRow.description != null)
                        currentBitfieldRow.description = normalizeDescription(currentBitfieldRow.description);
                    currentBitfieldRow.bitposition = attributes.getValue("bit_position");
                    String len = attributes.getValue("length");
                    if (len != null)
                        currentBitfieldRow.length = len;
                    currentBitfieldRow.xrefclassuid = attributes.getValue("xref");
                    currentBitfield.elems.add(currentBitfieldRow);
                    //maybeSysOut(attributes.getValue("xref"), "bitfieldrow uid " + currentBitfield.uid + " " + currentBitfield.name + " " + currentBitfieldRow.name + " " + currentBitfieldRow.description);
                    break;

                case "dict":
                    currentDict = new DictionaryElem();
                    currentDict.name = fixName(attributes.getValue("name")); // name canonicalization C14N
                    currentDict.uid = attributes.getValue("uid");
                    dictionaries.add(currentDict);
                    //maybeSysOut(attributes.getValue("xref"), "dict uid " + currentDict.uid + " " + currentDict.name);
                    break;

                case "dictrow":
                    if (currentDict == null)
                        break;
                    currentDictRow = new DictionaryRowElem();
                    currentDictRow.value = attributes.getValue("value");
                    currentDictRow.description = attributes.getValue("description");
                    if (currentDictRow.description != null)
                        currentDictRow.description = normalizeDescription(currentDictRow.description);
                    currentDict.elems.add(currentDictRow);
                    //maybeSysOut(attributes.getValue("xref"), "dictrow uid" + currentDict.uid + " " + currentDict.name + " " + currentDictRow.value + " " + currentDictRow.description);
                    break;

               case "revision":
                    if (sisoSpecificationTitleDate == null) // assume first encountered is latest
                        sisoSpecificationTitleDate = attributes.getValue("title") + ", " + attributes.getValue("date");
                    break;

                // fall throughs
                case "cot":
                case "cet":
                case "copyright":
                case "revisions":
                case "ebv":
                case "jammer_technique":
                case "jammer_kind":
                case "jammer_category":
                case "jammer_subcategory":
                case "jammer_specific":
                default:
                    testElems.add(qName);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
        {
            switch (qName) {
                case "enum":
                    if (currentEnum != null)
                       writeOutEnum(currentEnum);
                    currentEnum = null;
                    break;

                case "enumrow":
                    currentEnumRow = null;
                    break;

                case "bitfield":
                    if (currentBitfield != null)
                        writeOutBitfield(currentBitfield);
                    currentBitfield = null;
                    break;

                case "bitfieldrow":
                    currentBitfieldRow = null;
                    break;

                case "dict":
                    if (currentDict != null)
                        writeOutDict(currentDict);
                    currentDict = null;
                    break;

                case "dictrow":
                    currentDictRow = null;
                    break;
            }
        }

        Set<String> dictNames = new HashSet<>();
        
        private void writeOutDictOpers(DictionaryElem el, StringBuilder sb, String className)
        {
            sb.append(String.format(disDictEnumOperators,
                                    className, className                      // dictionary map name
                                    ));
        }
        private void writeOutDictStrings(DictionaryElem el, StringBuilder sb, String classNameCorrected)
        {
            // write out the start
            sb.append(String.format(disDictEnumStringsStart, 
                                    classNameCorrected,                 //Dictionary Type
                                    firstCharLower(classNameCorrected)  //to_string param
                                    ));

            // Output an enum string value for each
            for (DictionaryRowElem row : el.elems) {
                String name = row.value.replaceAll("[^a-zA-Z0-9]", ""); // only chars and numbers
                sb.append(String.format(disDictEnumStringsValue, name));
            }
            // Output an INVALID text without a comma
            String enumStringValue = String.format(disDictEnumDescriptionValue, "Invalid").replace(",","");
            sb.append(enumStringValue);

            // finish the const char
            sb.append("    };\n");

            // write out the methods
            sb.append(String.format(disDictEnumStringsEnd, 
                                    firstCharLower(classNameCorrected), classNameCorrected, "INVALID",     // to_string lookup
                                    classNameCorrected, classNameCorrected, classNameCorrected, "INVALID",  // GetEnumByIndex
                                    classNameCorrected, firstCharLower(classNameCorrected), firstCharLower(classNameCorrected) // ostream <<
                                    ));

        }

        private void writeOutDictHeader(DictionaryElem el)
        {
            String clsName = uidClassNames.get(el.uid); //Main.this.uid2ClassName.getProperty(el.uid);
            if (clsName == null)
            {
                System.out.flush();
                System.err.println("*** Didn't find a class name for uid = " + el.uid + ", ignored");
                return;
            }
            String classNameCorrected = clsName;
            if (classNameCorrected.contains("Link11/11"))
            {
                System.out.flush();
                System.err.print  ( "original classNameCorrected=" + classNameCorrected);
                classNameCorrected = classNameCorrected.replace("Link11/11B", "Link11_11B"); // Fix slash in entry
                System.err.println(", revised classNameCorrected=" + classNameCorrected);
            }
            StringBuilder sb = new StringBuilder();

            // Header section
            String additionalInterface = "";
            String otherIf = interfaceInjection.getProperty(clsName);
            if (otherIf != null)
                additionalInterface = ", " + otherIf;

            sb.append(licenseTemplate);
            sb.append(String.format(disenumfilestartTemplate,clsName.toUpperCase(), clsName.toUpperCase()));

            sb.append("\n");
            sb.append(String.format("namespace %s {\nnamespace dictionary {\n", globalNamespace));
            // sb.append(formatNamespaceStatement(enumNamespace));
            sb.append("\n");

            sb.append(String.format(disdictenumpart1Template,
                                    sisoSpecificationTitleDate, packageName, "UID " + el.uid,   // UID
                                    classNameCorrected.toLowerCase(), classNameCorrected
                                    ));

            // enumerations section
            dictNames.clear();

            int numberOfEnumerations = el.elems.size();
            int enumValue = 0;
            for (DictionaryRowElem row : el.elems) {
                String name = row.value.replaceAll("[^a-zA-Z0-9]", ""); // only chars and numbers

                // Force them to lower case
                // name = name.toLowerCase();

                // check for reserved keywords
                if (cppEnumAliases !=null && cppEnumAliases.getProperty(name)!= null) {
                    name = cppEnumAliases.getProperty(name);
                }

                if (!dictNames.contains(name))
                {
                     String fullName = normalizeDescription(row.description);
                     sb.append(String.format(disdictenumpart2Template, name, fullName));
                     dictNames.add(name);
                }
                else System.out.println("   Duplicate dictionary entry for " + name + " in " + clsName);
                ++enumValue;
            }
            // Add an INVALID enum for the end
            String enumStatement = String.format(disdictenumpart2Template, "INVALID", "Invalid");
            // enumStatement = enumStatement.replace(",","");
            sb.append(enumStatement);
            
            // End the enum class
            sb.append("    };\n");

            writeOutDictOpers(el, sb, classNameCorrected);
            
            // end the get_description method
            sb.append("}\n");

            sb.append(String.format("} } // end namespace %s::dictionary\n", globalNamespace));
            // sb.append(String.format("\n\n} } } // end namespace %s\n", enumNamespace));
            sb.append(String.format(disenumfileendTemplate,classNameCorrected.toUpperCase()));

            // save file
            File targetFile = new File(outputDirectory, classNameCorrected + ".h");
            targetFile.getParentFile().mkdirs();
            OutputStreamWriter targetFileWriter;
            try {
                targetFile.createNewFile();
                FileOutputStream fso = new FileOutputStream(targetFile);
                targetFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
                targetFileWriter.write(sb.toString());
                targetFileWriter.flush();
                targetFileWriter.close();
            }
            catch (IOException ex) {
                System.out.flush();
                System.err.println (ex.getMessage()
                // + " targetFile.getAbsolutePath()=" 
                   + targetFile.getAbsolutePath()
                // + ", classNameCorrected=" + classNameCorrected
                );
                ex.printStackTrace(System.err);
            }
        
        }

        private void writeOutDictCppFile(DictionaryElem el)
        {
            String clsName = uidClassNames.get(el.uid); //Main.this.uid2ClassName.getProperty(el.uid);
            if (clsName == null)
            {
                System.out.flush();
                System.err.println("*** Didn't find a class name for uid = " + el.uid + ", ignored");
                return;
            }
            String classNameCorrected = clsName;
            if (classNameCorrected.contains("Link11/11"))
            {
                System.out.flush();
                System.err.print  ( "original classNameCorrected=" + classNameCorrected);
                classNameCorrected = classNameCorrected.replace("Link11/11B", "Link11_11B"); // Fix slash in entry
                System.err.println(", revised classNameCorrected=" + classNameCorrected);
            }
            StringBuilder sb = new StringBuilder();

            sb.append(licenseTemplate);
            // int enumSize = getEnumSize(el);
            sb.append(String.format(disDictCppStartTemplate,
                                    classNameCorrected,                                     // include statement
                                    enumNamespace,
                                    el.uid, classNameCorrected                             // UID statement
                                    ));;


            // Namespace from build parameters can have multiple layers
            sb.append(formatNamespaceStatement(enumNamespace));
            sb.append("\n");
            sb.append("\n");

            writeOutDictOpers(el, sb,  classNameCorrected);

            sb.append(String.format("\n} } } // end namespace %s\n\n", enumNamespace));

            // save file
            File targetFile = new File(outputDirectory, classNameCorrected + ".cpp");
            targetFile.getParentFile().mkdirs();
            OutputStreamWriter targetFileWriter;
            try {
                targetFile.createNewFile();
                FileOutputStream fso = new FileOutputStream(targetFile);
                targetFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
                targetFileWriter.write(sb.toString());
                targetFileWriter.flush();
                targetFileWriter.close();
            }
            catch (IOException ex) {
                System.out.flush();
                System.err.println (ex.getMessage()
                // + " targetFile.getAbsolutePath()=" 
                   + targetFile.getAbsolutePath()
                // + ", classNameCorrected=" + classNameCorrected
                );
                ex.printStackTrace(System.err);
            }
        }

        private void writeOutDict(DictionaryElem el)
        {
            writeOutDictHeader(el);
            // writeOutDictCppFile(el);
        }

        private void writeOutBitfieldHeaderFile(BitfieldElem el)
        {
            String clsName = uidClassNames.get(el.uid); //Main.this.uid2ClassName.getProperty(el.uid);
            if (clsName == null)
            {
                System.out.flush();
                System.err.println("Didn't find a class name for uid = " + el.uid);
                return;
            }

            String classNameCorrected = clsName;
            if (classNameCorrected.contains("Link11/11"))
            {
                System.out.flush();
                System.err.print  ( "original classNameCorrected=" + classNameCorrected);
                classNameCorrected = classNameCorrected.replace("Link11/11B", "Link11_11B"); // Fix slash in entry
                System.err.println(", revised classNameCorrected=" + classNameCorrected);
            }
            StringBuilder sb = new StringBuilder();
      
            String otherInf = uid2ExtraInterface.get(el.uid);
            sb.append(licenseTemplate);

            sb.append(String.format("#ifndef %s_H\n",classNameCorrected));
            sb.append(String.format("#define %s_H\n",classNameCorrected));

            // Add all the include statements
            sb.append("\n");
            el.elems.forEach((row) -> {
                // this one has a class type
                String xrefName = null;
                if (row.xrefclassuid != null)
                {
                    xrefName = uidClassNames.get(row.xrefclassuid); //Main.this.uid2ClassName.getProperty(row.xrefclassuid);
                    sb.append(String.format("#include <dis/enumerations/%s.h>\n", xrefName));
                }
            });

            // sb.append("\nusing namespace dis::siso_ref_010::enums;\n");


            sb.append(String.format(disbitset1Template, 
                        packageName, sisoSpecificationTitleDate,
                        "UID " + el.uid, el.size,
                        el.name,
                        formatNamespaceStatement(enumNamespace),
                        el.size, classNameCorrected
                        ));

            enumNames.clear();

            // We need to keep track of the bits
            int bitsUsed = 0;
            for (BitfieldRowElem row : el.elems)
            {
                String xrefName = null;
                if (row.xrefclassuid != null)
                {
                    xrefName = uidClassNames.get(row.xrefclassuid);
                }

                String bitsType = new String();
                if  (Integer.valueOf(row.length) == 1)
                     bitsType = "boolean";
                else bitsType = "length=" + row.length;

                bitsUsed += Integer.parseInt(row.length);

                sb.append(String.format(disbitsetcommentxrefTemplate, 
                        "bit position " + row.bitposition + ", " + bitsType,
                        htmlize((row.description==null?"":normalizeDescription(row.description)+", ")),xrefName));

                String dataType = "unsigned";
                // if (xrefName != null) dataType = xrefName;
                sb.append(String.format("    %s %s : %s;\n", dataType, cleanupEnumName(row.name, false), row.length));
            }
            int bitsLeft = Integer.parseInt(el.size) - bitsUsed;
            if (bitsLeft > 0)
            {
                sb.append("\n    // Add padding for bit alignment\n");
                sb.append(String.format("    unsigned padding : %s;\n", bitsLeft));
            }

            sb.append("};\n");  // end class definition

            sb.append("\n");
            sb.append(String.format("std::string to_string(const %s &value);\n", classNameCorrected));
            sb.append(String.format("std::ostream& operator<<(std::ostream& os, const %s& value);\n", classNameCorrected));
            sb.append("\n");
            sb.append(String.format("bool operator==(const %s& lhs, const %s& rhs);\n", classNameCorrected, classNameCorrected));
            sb.append(String.format("bool operator!=(const %s& lhs, const %s& rhs);\n", classNameCorrected, classNameCorrected));
            sb.append("\n");
            sb.append(String.format("dis::DataStream& operator <<(dis::DataStream &ds, const %s& value);\n", classNameCorrected));
            sb.append(String.format("%s& operator>>(dis::DataStream& ds, %s& value);\n", classNameCorrected, classNameCorrected));
            sb.append("\n");
            sb.append(String.format("std::uint%s_t get_marshaled_size(const %s& value);\n", el.size, classNameCorrected));
            sb.append(String.format("void marshal(dis::DataStream& ds, const %s& value);\n", classNameCorrected));
            sb.append(String.format("void unmarshal(dis::DataStream& ds, %s& value);\n", classNameCorrected));
            sb.append(String.format("\n"));
            sb.append("\n");

            sb.append(String.format("} // end namespace %s\n", globalNamespace));
            sb.append("} // end namespace siso_ref_010\n");
            sb.append("} // end namespace enums\n\n");

            sb.append(String.format("#endif // %s_H",classNameCorrected));

            // sb.append(String.format(disbitset2Template, classNameCorrected, el.size, classNameCorrected, classNameCorrected, classNameCorrected, classNameCorrected, classNameCorrected));

            // save file
            File targetFile = new File(outputDirectory, classNameCorrected + ".h");
            OutputStreamWriter targetFileWriter;
            try {
                targetFile.createNewFile();
                FileOutputStream fso = new FileOutputStream(targetFile);
                targetFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
                targetFileWriter.write(sb.toString());
                targetFileWriter.flush();
                targetFileWriter.close();
            }
            catch (IOException ex) {
                System.out.flush();
                System.err.println (ex.getMessage() + " targetFile.getAbsolutePath()=" + targetFile.getAbsolutePath()
                      + ", classNameCorrected=" + classNameCorrected);
                ex.printStackTrace(System.err);
            }
        }

        private void writeOutBitfieldToString(String className, BitfieldElem el, StringBuilder sb)
        {
            sb.append(String.format("std::string to_string(const %s& value)\n", className));
            sb.append("{\n");
            sb.append("    std::string outputStream;\n");

            sb.append(String.format("    outputStream += std::bitset<%s>(bit_cast < std::uint%s_t>(value)).to_string() + \"\\n\";\n",
                      el.size, el.size));

            String enumName = "";
            for (BitfieldRowElem row : el.elems)
            {
                String xrefName = null;
                if (row.xrefclassuid != null)
                {
                    xrefName = uidClassNames.get(row.xrefclassuid);
                }

                enumName = cleanupEnumName(row.name, false);
                if (xrefName != null)
                {
                    sb.append(String.format("    outputStream += \"%s : \" +  get_description(static_cast<%s>(value.%s)) + \"\\n\";\n", enumName, xrefName, enumName));
                }
                else
                {
                    sb.append(String.format("    outputStream += \"%s : \" +  std::to_string(value.%s) + \"\\n\";\n", enumName, enumName));
                }
            }
            sb.append("    return outputStream;\n");
            sb.append("}\n");
        }

        private void writeOutBitfieldOstream(String className, BitfieldElem el, StringBuilder sb)
        {
            sb.append(String.format("std::ostream& operator<<(std::ostream& os, const %s& value)\n", className));
            sb.append("{\n");
            sb.append("    return os << to_string(value);\n");
            sb.append("}\n");
        }

        private void writeOutBitfieldOperators(String className, BitfieldElem el, StringBuilder sb)
        {
            for (char c : "=!".toCharArray())
            {
                sb.append(String.format("bool operator%c=(const %s& lhs, const %s& rhs)\n", c, className, className));
                sb.append("{\n");
                sb.append(String.format("    return bit_cast<uint%s_t>(lhs) %s= bit_cast<uint%s_t>(rhs);\n", el.size, c, el.size));
                sb.append("}\n");
            }
        }

        private void writeOutbitfieldMarshals(String className, BitfieldElem el, StringBuilder sb)
        {

            sb.append(String.format("dis::DataStream& operator<<(dis::DataStream& ds, const %s& value)\n", className));
            sb.append("{\n");
            sb.append(String.format("    return ds << bit_cast<uint%s_t>(value);\n", el.size));
            sb.append("}\n");

            sb.append(String.format("%s& operator>>(dis::DataStream& ds, %s& value)\n", className, className));
            sb.append("{\n");
            sb.append(String.format("    uint%s_t asInt;\n", el.size));
            sb.append("    ds >> asInt;\n");
            sb.append(String.format("    value = bit_cast<%s>(asInt);\n", className));
            sb.append("}\n");

            sb.append(String.format("std::uint%s_t get_marshaled_size(const %s& value)\n", el.size, className));
            sb.append("{\n");
            sb.append(String.format("    return %s;\n", el.size));
            sb.append("}\n");

            sb.append(String.format("void marshal(dis::DataStream& ds, const %s& value)\n", className));
            sb.append("{\n");
            sb.append(String.format("    ds << bit_cast<uint%s_t>(value);\n", el.size));
            sb.append("}\n");

            sb.append(String.format("void unmarshal(dis::DataStream& ds, %s& value)\n", className));
            sb.append("{\n");
            sb.append(String.format("    uint%s_t asInt;\n", el.size));
            sb.append("    ds >> asInt;\n");
            sb.append(String.format("    value = bit_cast<%s>(asInt);\n", className));
            sb.append("}\n");
        }

        private void writeOutBitfieldCppFile(BitfieldElem el)
        {
            String clsName = uidClassNames.get(el.uid); //Main.this.uid2ClassName.getProperty(el.uid);
            if (clsName == null)
            {
                System.out.flush();
                System.err.println("Didn't find a class name for uid = " + el.uid);
                return;
            }

            String classNameCorrected = clsName;
            if (classNameCorrected.contains("Link11/11"))
            {
                System.out.flush();
                System.err.print  ( "original classNameCorrected=" + classNameCorrected);
                classNameCorrected = classNameCorrected.replace("Link11/11B", "Link11_11B"); // Fix slash in entry
                System.err.println(", revised classNameCorrected=" + classNameCorrected);
            }
            StringBuilder sb = new StringBuilder();

            sb.append(String.format(disbitsetcppstartTemplate,
                                    "dis/enumerations/", classNameCorrected,      // include the class file
                                    formatNamespaceStatement(enumNamespace)       // using namespace statement
                                    ));;
            sb.append(String.format(disbitsetbitcastTemplate));
            writeOutBitfieldToString(classNameCorrected, el, sb);
            writeOutBitfieldOstream(classNameCorrected, el, sb);
            writeOutBitfieldOperators(classNameCorrected, el, sb);
            writeOutbitfieldMarshals(classNameCorrected, el, sb);
            sb.append("\n");

            sb.append(formatNamespaceEndStatement(enumNamespace));

            // save file
            File targetFile = new File(outputDirectory, classNameCorrected + ".cpp");
            OutputStreamWriter targetFileWriter;
            try {
                targetFile.createNewFile();
                FileOutputStream fso = new FileOutputStream(targetFile);
                targetFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
                targetFileWriter.write(sb.toString());
                targetFileWriter.flush();
                targetFileWriter.close();
            }
            catch (IOException ex) {
                System.out.flush();
                System.err.println (ex.getMessage() + " targetFile.getAbsolutePath()=" + targetFile.getAbsolutePath()
                      + ", classNameCorrected=" + classNameCorrected);
                ex.printStackTrace(System.err);
            }
        }

        private void writeOutBitfield(BitfieldElem el)
        {
            writeOutBitfieldHeaderFile(el);
            writeOutBitfieldCppFile(el);
        }

        Set<String> enumNames = new HashSet<>();
        Properties aliases = null; // previously aliasNames

        /** 
          * write out to_string operator for all enum values
          * @param sb StringBuilder to write to
          * @param el enumeration element
          * @param className parent class name
          * @param aliases if set, use to replace enumerated values
          */
        private void writeToStringOperator(StringBuilder sb, EnumElem el, String className, String enumVariableName, Properties aliases)
        {
            enumNames.clear();

            String keyName = "enumValue";

            sb.append(String.format(disenumtostringTemplate,
                                    className, keyName, // parameter for to_string
                                    className           // type for map
                                    ));

            int rowCount = 0;
            String enumName;
            for (EnumRowElem row : el.elems)
            {
                if(aliases != null && aliases.getProperty(row.value)!=null) {
                    enumName = createEnumName(aliases.getProperty(row.value), false);
                }
                else {
                    enumName = createEnumName(normalizeDescription(row.description),false);
                }

                // sb.append(String.format("        enumToString(%s::%s)", className, enumName));
                sb.append(String.format("        {%s::%s, enumToString(%s::%s)}", className, enumName, className, enumName));
                if (rowCount < el.elems.size() -1)
                {
                    sb.append(",");
                }
                sb.append("\n");
                ++rowCount;
            };

            // End the enumToStrings definition
            sb.append("    };\n\n");
            
            sb.append(String.format("    if (is_valid(%s))\n", keyName));
            sb.append(String.format("    {\n"));
            sb.append(String.format("        const auto iter = enumNames.find(%s);\n", keyName));
            sb.append(String.format("        return iter->second;\n"));
            // sb.append(String.format("        return std::string(enumNames[static_cast<int>(%s)]);\n", keyName));
            sb.append(String.format("    }\n"));
            sb.append("    else\n");
            sb.append(String.format("    {\n"));
            sb.append("        return \"Invalid enumerated value\";\n");
            sb.append(String.format("    }\n"));
            
            sb.append("}\n\n");

        }

        /** 
          * write out is valid operator for all enum values
          * @param sb StringBuilder to write to
          * @param el enumeration element
          * @param className parent class name
          * @param aliases if set, use to replace enumerated values
          */
        private void writeIsValidOperator(StringBuilder sb, EnumElem el, String className, String enumVariableName, Properties aliases)
        {
            enumNames.clear();
            String keyName = "enumValue";
            // return std::binary_search(EntityKindEnumValues.begin(), EntityKindEnumValues.end(), entityKind) != EntityKindEnumValues.end();
            sb.append(String.format(disenumValidTemplate, 
                                    className, keyName, // isValidEnum dec
                                    className, el.elems.size()
                                    ));
            int rowCount = 0;
            for (EnumRowElem row : el.elems)
            {
                String enumName;
                if(aliases != null && aliases.getProperty(row.value)!=null) {
                    enumName = createEnumName(aliases.getProperty(row.value), false);
                }
                else {
                    enumName = createEnumName(normalizeDescription(row.description),false);
                }

                sb.append(String.format("       %s::%s", className, enumName));
                if (rowCount < el.elems.size() -1)
                {
                    sb.append(",");
                }
                sb.append("\n");
                ++rowCount;
            };

            sb.append("    }};\n");
            sb.append(String.format("    return std::binary_search(enumValues.begin(), enumValues.end(), %s);\n", keyName));
            sb.append("}\n");

        }
        
        private void writeMarshalOperators(StringBuilder sb, EnumElem el, String className, String enumVariableName)
        {

        }



        

        private void writeEnumValues(StringBuilder sb, EnumElem el, String className)
        {
            sb.append(String.format(disenumvaluesTemplate, 
                                    className, // array type
                                    el.elems.size(),    // array size
                                    className  // array name
                                    ));

            int rowCount = 0;
            for (EnumRowElem row : el.elems)
            {
                String enumName;
                if(aliases != null && aliases.getProperty(row.value)!=null) {
                    enumName = createEnumName(aliases.getProperty(row.value), false);
                    sb.append("WRITING AN ALIAS");
                }
                else {
                    enumName = cleanupEnumName(normalizeDescription(row.description));
                }
                sb.append(String.format("        %s::%s", className, enumName));
                if (rowCount < el.elems.size() -1)
                {
                    sb.append(",");
                }
                sb.append("\n");
                ++rowCount;
            }

            // End the enumValues definition
            sb.append("    }};\n");
        }

        private int getEnumSize(EnumElem el)
        {
            int sizeValue = Integer.parseInt(el.size);
            int returnSize;

            if (sizeValue <= 8)
                returnSize = 8;
            else if (sizeValue <= 16)
                returnSize = 16;
            else
                returnSize = 32;

            return returnSize;
        }



        private void writeEnumHeader(EnumElem el)
        {
            // be careful here, concurrency scoping
            // overflow buffer matching el.elems to later avoid dreaded "code too large" error
            List<EnumRowElem> additionalRowElements = new ArrayList<>();

            String clsName = uidClassNames.get(el.uid); //Main.this.uid2ClassName.getProperty(el.uid);
            if (clsName == null)
            {
                System.out.flush();
                System.err.println("*** Didn't find a class name for uid = " + el.uid);
                return;
            }
            String classNameCorrected = clsName;
            if (!classNameCorrected.isEmpty() && classNameCorrected.contains("Link 11/11")) // special case
            {
                System.out.flush();
                System.err.print  ( "original classNameCorrected=" + classNameCorrected);
                classNameCorrected = classNameCorrected.replace("Link 11/11B", "Link11_11B"); // Fix slash in entry
                System.err.println(", revised classNameCorrected=" + classNameCorrected);
            }
            
            if(el.uid.equals("4"))
              aliases = uid4aliases;
            else
              aliases = null;
            
            StringBuilder sb = new StringBuilder();

            sb.append(licenseTemplate);
            sb.append(String.format(disenumfilestartTemplate,clsName.toUpperCase(), clsName.toUpperCase()));

            // Namespace from build parameters can have multiple layers
            sb.append("\n");
            sb.append(formatNamespaceStatement(enumNamespace));
            sb.append("\n");
            
            StringBuilder additionalRowStringBuilder = new StringBuilder();
            // change additional class name to match similarly
            final String ADDITIONAL_ENUMERATION_FILE_SUFFIX = "Additional";

            // Header section
            String additionalInterface = "";
            String otherIf = interfaceInjection.getProperty(clsName);
            String otherIf2 = uid2ExtraInterface.get(el.uid);

            // This is a java thing for enum AggregateSubcategory implements SubCategory
            // Set them to null to skip
            otherIf = null; otherIf2 = null;            
            if(otherIf != null | otherIf2 != null) {
                StringBuilder ifsb = new StringBuilder("implements ");
                if(otherIf != null)
                    ifsb.append(otherIf);
                if(otherIf2 != null){
                    ifsb.append(",");
                    ifsb.append(otherIf2);
                }
                additionalInterface = ifsb.toString();
            }

            /* enumeration initial template, de-spacify name */
            int numberOfEnumerations = el.elems.size();
            
            int enumSize = getEnumSize(el);
            if(el.footnote == null)
              sb.append(String.format(disenumpart1Template,
                                      packageName,
                                      sisoSpecificationTitleDate,
                                      "UID " + el.uid, 
                                      el.size,  // marshal size
                                      el.name, numberOfEnumerations,  // class has x enumerations total
                                      classNameCorrected,
                                      Integer.toString(enumSize)
                                    //   el.size.toString()
                                      ));
            else
              sb.append(String.format(disenumpart1withfootnoteTemplate, packageName, sisoSpecificationTitleDate,  
                                      "UID " + el.uid, el.size, el.name,
                                      numberOfEnumerations, 
                                      el.footnote, classNameCorrected,
                                      Integer.toString(enumSize)
                                      ));

            enumNames.clear();
            // enum section
            if (el.elems.isEmpty())
            {
                String elementName = "(undefined element)";
                if (el.name != null)
                       elementName = el.name;
                sb.append("   /** Constructor initialization */");
                sb.append(String.format(disenumpart2Template, "SELF", "0", elementName + " details not found in SISO spec"));
                // TODO resolve
                System.err.println("*** " + elementName + " uid='" + el.uid + "' has no child element (further SELF details not found in SISO reference)");
            }
            else // here we go
            {
                if (el.elems.size() > MAX_ENUMERATIONS)
                {
                    
                    System.out.flush();
                    System.err.println ("=================================");
                    System.err.println ("*** Enumerations class " + packageName + "." + classNameCorrected + " <enum name=\"" + el.name + "\" uid=\"" + el.uid + "\" etc.>" +
                                        " has " + el.elems.size() + " enumerations, possibly too large to compile.");
                    System.err.println ("*** Dropping enumerations above MAX_ENUMERATIONS=" + MAX_ENUMERATIONS + " for this class..." +
                                        " next, create auxiliary class with remainder.");
                    // https://stackoverflow.com/questions/1184636/shrinking-an-arraylist-to-a-new-size
                    // save the rest
                    System.err.println ("    last element=" + el.elems.get(MAX_ENUMERATIONS - 1).value + ", " + el.elems.get(MAX_ENUMERATIONS - 1).description);
                    // make copy (not reference) available  for further processing
                    additionalRowElements = new ArrayList<>(el.elems.subList(MAX_ENUMERATIONS, el.elems.size()));
                    // save what was created so far for later reuse
                    additionalRowStringBuilder.append(sb.toString().replace("enum " + classNameCorrected,
                                                                            "enum " + classNameCorrected + ADDITIONAL_ENUMERATION_FILE_SUFFIX));
                    el.elems.subList(MAX_ENUMERATIONS, el.elems.size()).clear(); // clear elements after this
                }
                // continue with original or reduced list
                boolean lastOne = false;
                int rowCount = 0;

                for (EnumRowElem row : el.elems) {
                    if (rowCount == el.elems.size() -1)  lastOne = true;

                    // aliases are set for UID 4 (DisPduType)
                    if(aliases != null && aliases.getProperty(row.value)!=null) {
                        writeOneEnum(sb,row,aliases.getProperty(row.value).toLowerCase());
                    }
                    else {
                        String enumName = createEnumName(normalizeDescription(row.description), false);
                        writeOneEnum(sb, row, enumName, lastOne);
                    }

                    ++rowCount;
                }

            }
            // End the enumeration definition
            sb.append("};\n\n");

            String enumVariableName = firstCharLower(classNameCorrected);
            sb.append(String.format(disenumheadertemplate,
                                    classNameCorrected, enumVariableName,       // add_custom_value
                                    classNameCorrected, enumVariableName,       // is_valid
                                    classNameCorrected, enumVariableName,       // get_description
                                    classNameCorrected,                         //to_string
                                    classNameCorrected,                         // concurrent_add_custom_value
                                    classNameCorrected,                         // concurrent_is_valid
                                    classNameCorrected,                         // concurrent_get_description
                                    classNameCorrected,                         // concurrent_to_string
                                    classNameCorrected,                         // ostream operator <<
                                    classNameCorrected,                         // DataStream operator <<
                                    classNameCorrected, classNameCorrected,     // DataSream operator >>
                                    classNameCorrected,                         // get_marshaled_size
                                    classNameCorrected,                         // marshal
                                    classNameCorrected                          // unmarshal
                                    ));

            sb.append(String.format("\n} } } // end namespace %s\n\n", enumNamespace));
            sb.append(String.format("#endif // %s_H",classNameCorrected));
                                    
            // sb.append(String.format("\nusing UID_%s = %s;\n",el.uid, classNameCorrected));
            
            // save file
            File targetFile = new File(outputDirectory, classNameCorrected + ".h");
            OutputStreamWriter targetFileWriter;
            try {
                targetFile.createNewFile();
                FileOutputStream fso = new FileOutputStream(targetFile);
                targetFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
                targetFileWriter.write(sb.toString());
                targetFileWriter.flush();
                targetFileWriter.close();
            }
            catch (IOException ex) {
                System.out.flush();
                System.err.println (ex.getMessage() + " targetFile.getAbsolutePath()=" + targetFile.getAbsolutePath()
                      + ", classNameCorrected=" + classNameCorrected);
                ex.printStackTrace(System.err);
            }
        //  now handle additionalRowElements similarly, if any, creating another file...
        if ((additionalRowElements.size() > 0) && !additionalRowStringBuilder.toString().isEmpty())
        {
            classNameCorrected = classNameCorrected + ADDITIONAL_ENUMERATION_FILE_SUFFIX;
            for (EnumRowElem row : additionalRowElements)
            {
//            additionalRowElements.elems.forEach((row) -> {

                // Check for aliases
                if(aliases != null && aliases.getProperty(row.value)!=null) {
                  writeOneEnum(additionalRowStringBuilder,row,aliases.getProperty(row.value));
                  sb.append("WRITING AN ALIAS");
                }
                else {
                  String enumName = createEnumName(normalizeDescription(row.description), false);
                  writeOneEnum(additionalRowStringBuilder, row, enumName);
                }
            } /* ); */
            additionalRowStringBuilder.setLength(additionalRowStringBuilder.length() - 2);
            additionalRowStringBuilder.append("; /*here*/\n");

            // save file
            targetFile = new File(outputDirectory, classNameCorrected + ".h"); // already appended ADDITIONAL_ENUMERATION_FILE_SUFFIX
            try {
                targetFile.createNewFile();
                FileOutputStream fso = new FileOutputStream(targetFile);
                targetFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
                targetFileWriter.write(additionalRowStringBuilder.toString());
                targetFileWriter.flush();
                targetFileWriter.close();
                System.out.flush();
                System.err.println ("*** Created additional-enumerations file, "
                                    + "classNameCorrected=" + classNameCorrected
                                    + ",\n    "
                            //      + "targetFile.getAbsolutePath()="
                                    + targetFile.getAbsolutePath()
                );
                System.err.println ("    first element=" + additionalRowElements.get(0).value + ", " + additionalRowElements.get(0).description);
                System.err.println ("=================================");
                // reset this special case
                additionalRowElements.clear();
                additionalRowStringBuilder.setLength(0);
                additionalEnumClassesCreated++;
            }
            catch (IOException ex) {
                System.out.flush();
                System.err.println (ex.getMessage() + " targetFile.getAbsolutePath()=" + targetFile.getAbsolutePath()
                      + ", classNameCorrected=" + classNameCorrected);
                ex.printStackTrace(System.err);
            }
        }
        }

        /** 
          * write out descriptions for the enums.
          * @param sb StringBuilder to write to
          * @param el enumeration element
          * @param className parent class name
          * @param aliases if set, use to replace enumerated values
          */
        private void writeEnumDescriptions(StringBuilder sb, EnumElem el, String className, Properties aliases)
        {
            enumNames.clear();
            String mapName = String.format("%s_descriptions", className);
            String keyName = "enumValue";

            // Output make key name, and the map name
            sb.append(String.format(disenumdescriptionsTemplate, className, mapName));

            int rowCount = 0;
            for (EnumRowElem row : el.elems)
            {
                String enumName;
                if(aliases != null && aliases.getProperty(row.value)!=null) {
                    enumName = createEnumName(aliases.getProperty(row.value), false);
                }
                else {
                    enumName = createEnumName(normalizeDescription(row.description),false);
                }

                // sb.append(String.format("        %s::%s", className, enumName));
                sb.append(String.format("        { %s::%s, \"%s\" }", className, enumName, row.description));
                if (rowCount < el.elems.size() -1)
                {
                    sb.append(",");
                }
                sb.append("\n");
                ++rowCount;
            }
            sb.append("    };\n");

            // sb.append(String.format("    int keyToFind = static_cast<int>(%s);\n", keyName));
            // sb.append(String.format("    auto it = std::find_if(%s.begin(), %s.end(),\n", mapName, mapName));
            // sb.append(String.format("    [%s](const std::pair<%s, std::string>& pair) {\n", keyName, className));
            // sb.append(String.format("       return pair.first == %s;\n", keyName));
            // sb.append("    });\n");

            // sb.append(String.format("    if (it != %s.end())\n", mapName));
            // sb.append(String.format("        return it->second;\n"));
            // sb.append(String.format("    else\n"));
            // sb.append(String.format("        return \"Enum value not found\";\n"));

                        //    return pair.first == keyToFind;
                        // });",
                        // mapName, mapName

            // sb.append("}\n");
        }

        /** 
          * write out names for the enums.
          * @param sb StringBuilder to write to
          * @param el enumeration element
          * @param className parent class name
          * @param aliases if set, use to replace enumerated values
          */
        private void writeEnumNames(StringBuilder sb, EnumElem el, String className, Properties aliases)
        {
            enumNames.clear();
            String mapName = String.format("%s_names", className);
            String keyName = "enumValue";

            // Output make key name, and the map name
            sb.append(String.format(disenumdescriptionsTemplate, className, mapName));

            int rowCount = 0;
            for (EnumRowElem row : el.elems)
            {
                String enumName;
                if(aliases != null && aliases.getProperty(row.value)!=null) {
                    enumName = createEnumName(aliases.getProperty(row.value), false);
                }
                else {
                    enumName = createEnumName(normalizeDescription(row.description),false);
                }

                // sb.append(String.format("        %s::%s", className, enumName));
                sb.append(String.format("        { %s::%s, enumToString(%s:%s) }", className, enumName, className, enumName));
                if (rowCount < el.elems.size() -1)
                {
                    sb.append(",");
                }
                sb.append("\n");
                ++rowCount;
            }
            sb.append("    };\n");

            // sb.append(String.format("    int keyToFind = static_cast<int>(%s);\n", keyName));
            // sb.append(String.format("    auto it = std::find_if(%s.begin(), %s.end(),\n", mapName, mapName));
            // sb.append(String.format("    [%s](const std::pair<%s, std::string>& pair) {\n", keyName, className));
            // sb.append(String.format("       return pair.first == %s;\n", keyName));
            // sb.append("    });\n");

            // sb.append(String.format("    if (it != %s.end())\n", mapName));
            // sb.append(String.format("        return it->second;\n"));
            // sb.append(String.format("    else\n"));
            // sb.append(String.format("        return \"Enum value not found\";\n"));

                        //    return pair.first == keyToFind;
                        // });",
                        // mapName, mapName

            // sb.append("}\n");
        }

        private void writeEnumOperators(StringBuilder sb, EnumElem el, String className)
        {
            int marshalSize = Integer.parseInt(el.size);
            String enumVaribleName = firstCharLower(className);
            String descMap = String.format("%s_descriptions", className);
            int enumSize = getEnumSize(el);

            // sb.append("\n/// TODO : output marshal methods\n");
            sb.append(String.format(disenumOperDefTemplate,
                                    className, descMap,                         // add_custom_value
                                    className, descMap,                         // is_valid
                                    className, descMap, descMap,                // get_descripton
                                    className,                                  // to_string
                                    className, className,                       // concurrent_add_custom_value
                                    className, className,                       // concurrent_is_valid
                                    className, className,                       // concurrent_get_descripton
                                    className, className,                       // concurrent_to_string
                                    className,                                  // ostream operator <<
                                    className, Integer.toString(enumSize),      // Datastream operator <<
                                    className, className, Integer.toString(enumSize), className, //DataStream operator >>
                                    className, enumSize, enumSize,              // get_marshaled_size
                                    className, enumSize,                        // marshal
                                    className, enumSize, className              // unmarshal
                                    ));
        }

        private void writeEnumCppFile(EnumElem el)
        {
            String clsName = uidClassNames.get(el.uid); //Main.this.uid2ClassName.getProperty(el.uid);
            if (clsName == null)
            {
                System.out.flush();
                System.err.println("*** Didn't find a class name for uid = " + el.uid);
                return;
            }
            String classNameCorrected = clsName;
            if (!classNameCorrected.isEmpty() && classNameCorrected.contains("Link 11/11")) // special case
            {
                System.out.flush();
                System.err.print  ( "original classNameCorrected=" + classNameCorrected);
                classNameCorrected = classNameCorrected.replace("Link 11/11B", "Link11_11B"); // Fix slash in entry
                System.err.println(", revised classNameCorrected=" + classNameCorrected);
            }
            
            if(el.uid.equals("4"))
              aliases = uid4aliases;
            else
              aliases = null;
            
            StringBuilder sb = new StringBuilder();

            sb.append(licenseTemplate);

            int enumSize = getEnumSize(el);
            sb.append(String.format(disenumcppstarttemplate,
                                    classNameCorrected,                                     // include statement
                                    enumNamespace,
                                    el.uid, classNameCorrected,                             // UID statement
                                    enumNamespace, classNameCorrected,                      // hash structure definition
                                    enumNamespace, classNameCorrected,                      // hash operator()
                                    Integer.toString(enumSize), Integer.toString(enumSize)  // enum size cast
                                    ));;

            sb.append("namespace { // anonymous namespace\n");

            writeEnumDescriptions(sb, el, classNameCorrected, aliases);

            // sb.append("    #define enumToString( name ) #name\n");
            // writeEnumNames(sb, el, classNameCorrected, aliases);
            
            sb.append("\n");
            sb.append(String.format(disenumcppmutexTemplate, classNameCorrected));
            sb.append("\n} // end anonymous namespace\n\n");

            // Namespace from build parameters can have multiple layers
            sb.append(formatNamespaceStatement(enumNamespace));

            writeEnumOperators(sb, el, classNameCorrected);

            sb.append(String.format("\n} } } // end namespace %s\n\n", enumNamespace));

            // save file
            File targetFile = new File(outputDirectory, classNameCorrected + ".cpp");
            OutputStreamWriter targetFileWriter;
            try {
                targetFile.createNewFile();
                FileOutputStream fso = new FileOutputStream(targetFile);
                targetFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
                targetFileWriter.write(sb.toString());
                targetFileWriter.flush();
                targetFileWriter.close();
            }
            catch (IOException ex) {
                System.out.flush();
                System.err.println (ex.getMessage() + " targetFile.getAbsolutePath()=" + targetFile.getAbsolutePath()
                      + ", classNameCorrected=" + classNameCorrected);
                ex.printStackTrace(System.err);
            }      
        }

        private void writeOutEnum(EnumElem el)
        {
            writeEnumHeader(el);
            writeEnumCppFile(el);
        }
    
    private void stringBuilderRemoveLastChar(StringBuilder sb) {
        // If the string is larger than one remove last character
        sb.setLength(Math.max(sb.length() - 1, 0));
        // if (sb.length() > 0) {
        //     sb.setLength(sb.length() - 1);
        // }
    }

      private void writeOneEnum(StringBuilder sb, EnumRowElem row, String enumName, boolean... isLastOne)
      {
        String xrefName = null;
        if (row.xrefclassuid != null) {
          xrefName = uidClassNames.get(row.xrefclassuid);
        }

        if (xrefName == null) {
        //    sb.append(String.format(disenumfootnotecommentTemplate, htmlize(normalizeDescription(row.description)) + (row.footnote == null ? "" : ", " + htmlize(normalizeDescription(row.footnote)))));
            String enumStatement = String.format(disenumpart2Template, normalizeToken(enumName), row.value, normalizeDescription(row.description));
            if ((isLastOne.length > 0) && (isLastOne[0] == true))
            {
                enumStatement = enumStatement.replace(",","");
            }
            sb.append(enumStatement);
          
        //    sb.append(String.format("    %s = %s, /*%s*/\n", normalizeToken(enumName), row.value, normalizeDescription(row.description)));
        }
        else {
            sb.append(String.format(disenumcommentTemplate, row.xrefclassuid, xrefName));
            String enumStatement = String.format(disenumpart21Template, normalizeToken(enumName), row.value, normalizeDescription(row.description), xrefName);
        //    String enumStatement = String.format(disenumpart21Template, createEnumName(normalizeDescription(row.description)), row.value, normalizeDescription(row.description), xrefName);
           if ((isLastOne.length > 0) && (isLastOne[0] == true))
           {
            enumStatement = enumStatement.replace(",","");
           }
           sb.append(enumStatement);
        //   sb.append("\n    C++ Enum Declaration - has xrefName\n");
            // sb.append(String.format("xrefName = %s\n", xrefName));
        //    sb.append("\n");
            // sb.append(String.format("    %s = %s, /* %s */\n", createEnumName(normalizeDescription(row.description)), row.value, normalizeDescription(row.description), xrefName));
        }

      }
        /**
         * Naming conventions for enumeration names
         * @param s enumeration string from XML data file
         * @return normalized name
         */
        private String createEnumName(String s, Boolean... setUpperCase)
        {
            // For the cases where we don't set to all upper case
            String r = s.toLowerCase();

            if (setUpperCase.length == 0)
            {
                r = s.toUpperCase();
            }
            else
            {
                if (setUpperCase[0] == true) {
                    r = s.toUpperCase();
                }
            }

            // Convert any of these chars to underbar (u2013 is a hyphen observed in source XML):
            r = r.replaceAll("[\\h-/,\";:\\u2013]", "_");

            // Remove any of these chars (u2019 is an apostrophe observed in source XML):
            r = r.replaceAll("[()}{}'.#&\\u2019]", "");

            // Special case the plus character:
            r = r.replace("+", "PLUS");

            // Collapse all contiguous underbars:
            r = r.replaceAll("_{2,}", "_");

            // If there's nothing there, put in something:
            if (r.isEmpty() || r.equals("_"))
                r = "undef";

            // Java identifier can't start with digit
            if (Character.isDigit(r.charAt(0)))
                r = "_" + r; // originally "$"

            // This takes care of any reserved keywords etc
            if (cppEnumAliases !=null && cppEnumAliases.getProperty(r)!= null) {
                r = cppEnumAliases.getProperty(r);
            }

            // Handle multiply defined entries in the XML by appending a digit:
            String origR = r;
            int count = 2;
            while (enumNames.contains(r)) {
                r = origR + "_" + Integer.toString(count++);
            }
            enumNames.add(r);

            return r;
        }

        private String firstCharUpper(String s)
        {
            String ret = s.toLowerCase();
            char[] ca = ret.toCharArray();
            ca[0] = Character.toUpperCase(ca[0]);
            return new String(ca);
        }

        private String firstCharLower(String s)
        {
            char c[] = s.toCharArray();
            c[0] +=32;
            String ret = new String(c);
            return ret;
        }

        String maybeSpecialCase(String s, String dflt)
        {
            String lc = s.toLowerCase();
            if (lc.equals("united states"))
                return "USA";
            if (lc.equals("not_used"))
                return "";
            return dflt;
        }

        String smallCountryName(String s, String integ)
        {
            if (integ.equals("0"))
                return "";  // "other

            if (s.length() <= 3)
                return s;
            try {
                s = s.substring(s.indexOf("(") + 1, s.indexOf(")"));
                if (s.length() > 3) {
                    return maybeSpecialCase(s, integ);
                }
                return s;
            }
            catch (Exception ex) {
                return integ;
            }
        }
    }

    /**
    * Normalize string characters to create valid description
    * @param value of interest
    * @return normalized value
    */
    public static String normalizeDescription(String value)
    {
        String normalizedEntry = value.trim()
                                        .replaceAll("&", "&amp;").replaceAll("&amp;amp;", "&amp;")
                                        .replaceAll("\"", "'");
        return normalizedEntry;
    }
    /**
    * Normalize string characters to create valid Java name.  Note that unmangled name typically remains available in the description
    * @param value of interest
    * @return normalized value
    */
    public static String normalizeToken(String value)
    {
        String normalizedEntry = value.trim()
                                        .replaceAll("\"", "").replaceAll("\'", "")
                                        .replaceAll("—","-").replaceAll("–","-") // mdash
                // 
                                        .replaceAll("\\*","x").replaceAll("/","") // escaped regex for multiply, divide
                                        .replaceAll("&", "&amp;").replaceAll("&amp;amp;", "&amp;");
        if (!normalizedEntry.isEmpty() && Character.isDigit(normalizedEntry.toCharArray()[0]))
                normalizedEntry = '_' + normalizedEntry;
        if (!value.equals(normalizedEntry) && !normalizedEntry.equals(value.trim()))
            System.out.println ("*** normalize " + "\n" + 
                                "'" + value + "' to\n" + 
                                "'" + normalizedEntry + "'");
        return normalizedEntry;
    }
}
