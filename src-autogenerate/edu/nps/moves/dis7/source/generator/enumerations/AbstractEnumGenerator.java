package edu.nps.moves.dis7.source.generator.enumerations;

import java.io.*;
import java.util.*;

import java.io.File;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.nio.charset.StandardCharsets;

public abstract class AbstractEnumGenerator 
{

   class DictionaryRowElem
   {
      String value;
      String description;
   }

   class DictionaryElem
   {
      String name;
      String uid;
      List<DictionaryRowElem> elems = new ArrayList<>();
   }

   protected String globalNamespace = "";
   protected String enumNamespace = "";

   protected File outputDirectory;
   protected Properties uid2ClassName;
   protected Properties uid4aliases;
   protected Map<String,String> uidClassNames;
   protected Properties interfaceInjection;

   protected String disdictenumpart1Template;
   protected String disdictenumpart2Template;
   protected String disdictenumpart3Template;
   protected String disdictenumtostringTemplate;

   protected  static String outputDirectoryPath = "src-generated/java/edu/nps/moves/dis7/enumerations"; // default
   protected  static String packageName =         "edu.nps.moves.dis7.enumerations"; // default
   protected  String language    = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_PROGRAMMING_LANGUAGE;
   protected  static String sisoXmlFile = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_SISO_XML_FILE;
   
    /** String constant */ public static final String JAVA = "java";
    /** String constant */ public static final String CPP = "cpp";
    /** String constant */ public static final String OBJC = "objc";
    /** String constant */ public static final String CSHARP = "csharp";
    /** String constant */ public static final String JAVASCRIPT = "javascript";
    /** String constant */ public static final String PYTHON = "python";

    private static String[] languageChoices = {JAVA, CPP, CSHARP, PYTHON};

   protected static String sisoSpecificationTitleDate = "";

    protected String disenumpart1Template;
    protected String disenumpart1withfootnoteTemplate;
    protected String disenumcommentTemplate;
    protected String disenumfootnotecommentTemplate;
    protected String disenumpart2Template;
    protected String disenumpart21Template;
    protected String disenumpart25Template;
    protected String disenumpart3_8Template;
    protected String disenumpart3_16Template;
    protected String disenumpart3_32Template;
    protected String disenummarshalersTemplate;
    protected String disenum8bitmarshalerTemplate;
    
    protected String disbitset1Template;
    protected String disbitset15Template;
    protected String disbitset16Template;
    protected String disbitset2Template;
    protected String disbitsetcommentxrefTemplate;
    protected String disbitsetcommentTemplate;
    protected String disbitsetgetsetTemplate;
    protected String disbitsetcppstartTemplate;
    protected String disbitsetmarshalersTemplate;
    protected String disbitsetopersTemplate;
    protected String disbitsetbitcastTemplate;
    protected String disbitsetunionTemplate;

    protected String licenseTemplate;

    protected String disenumfilestartTemplate;
    protected String disenumfileendTemplate;
    protected String disenumstringsTemplate;
    protected String disenumvaluesTemplate;
    protected String disenumCtorTemplate;
    protected String disenumOperTemplate;
    protected String disenumValidTemplate;
    protected String disenumOperDefTemplate;
    protected String disenumtostringTemplate;
    protected String disenumdescriptionsTemplate;

    protected String disenumheadertemplate;
    protected String disenumcppstarttemplate;
    protected String disenumcppmutexTemplate;

    protected String disDictEnumDescriptionStart;
    protected String disDictEnumDescriptionValue;
    protected String disDictEnumDescriptionEnd;

    protected String disDictEnumStringsStart;
    protected String disDictEnumStringsValue;
    protected String disDictEnumStringsEnd;
    protected String disDictEnumOperators;
    protected String disDictCppStartTemplate;
    protected String disDictHeaderOperDefTemplate;

    protected Map<String,String> uid2ExtraInterface;
    protected Map<String, String> capabilityNames = new HashMap<>();
    

   private AbstractEnumGenerator()
   {
      uidClassNames = new HashMap<>();
      uid2ExtraInterface = new HashMap<>();
        uid2ExtraInterface.put("450", "ICapabilities"); //Land Platform Entity Capabilities
        uid2ExtraInterface.put("451", "ICapabilities");
        uid2ExtraInterface.put("452", "ICapabilities");
        uid2ExtraInterface.put("453", "ICapabilities");
        uid2ExtraInterface.put("454", "ICapabilities");
        uid2ExtraInterface.put("455", "ICapabilities");
        uid2ExtraInterface.put("456", "ICapabilities");
        uid2ExtraInterface.put("457", "ICapabilities");
        uid2ExtraInterface.put("458", "ICapabilities");
        uid2ExtraInterface.put("459", "ICapabilities");
        uid2ExtraInterface.put("460", "ICapabilities");
        uid2ExtraInterface.put("461", "ICapabilities");
        uid2ExtraInterface.put("462", "ICapabilities"); //Sensor/Emitter Entity Capabilities

      loadAliasDefinitions();
      populateCapabilityNames();
   }

   public AbstractEnumGenerator(String aLanguage)
   {
        this();
        language = aLanguage;
   }

   public void SetLanguage(String aLanguage)
   {
        language = aLanguage;
   }

    public static Boolean CheckLanguage(String language)
    {
        Boolean languageFound = false;
        String checkLanguage = language.toLowerCase();
        for (int languageIndex = 0; languageIndex < languageChoices.length; languageIndex++)
        {
            //if (checkLanguage == languageChoices[languageIndex])
            if (checkLanguage.equals(languageChoices[languageIndex]))
            {
                languageFound = true;
            }
        }

        return languageFound;
    }

    private void populateCapabilityNames()
    {
        for (Map.Entry<String, String> entry : uid2ExtraInterface.entrySet())
        {
            String uidValue = entry.getKey();
            String typeName = uid2ClassName.getProperty(uidValue);
            if (typeName != null)
            {
                capabilityNames.put(uidValue, typeName);
            }
        }
    }

//    protected void loadAliasDefinitions() throws IOException
   protected void loadAliasDefinitions() 
   {
        try {
            uid2ClassName = new Properties();
            uid2ClassName.load(getClass().getResourceAsStream("Uid2ClassName.properties"));
            uid4aliases = new Properties();
            uid4aliases.load(getClass().getResourceAsStream("uid4aliases.properties")); 
        } catch (IOException e) {
            System.err.println("Failed to define aliases. Modify the XML file to include the missing property");
            System.err.println(e);
            System.exit(-1);
        }
   }
   protected void loadNamespaceDefinitions()
   {
        try {
            Properties systemProperties = System.getProperties();

            globalNamespace = systemProperties.getProperty("xmlpg.namespace");
            if (globalNamespace == null) {
                globalNamespace = "";
            }

            // set global namespace for enums
            enumNamespace = systemProperties.getProperty("xmlpg.enumNamespace");
            if (enumNamespace == null) {
                enumNamespace = "";
            }
        } catch (Exception e) {
            System.err.println("Required property not set. Modify the XML file to include the missing property");
            System.err.println(e);
            System.exit(-1);
        }
   }

   /**
   * Naming conventions for enumeration names
   * @param s enumeration string from XML data file
   * @return normalized name
   */
   protected String cleanupEnumName(String s, Boolean... setUpperCase)
   {

      String r = s;

      // default is to set enum name to upper case
      if (setUpperCase.length == 0)
      {
        r = s.toUpperCase();
      }
      else
      {
        if (setUpperCase[0] == true) r = s.toUpperCase();
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

      return r;
   }

    protected String formatNamespaceStatement(String namespaceDeclaration)
    {
        String namespaceStatement = "";

        String namespaceTokens[] = namespaceDeclaration.split("::");
        for (String s : namespaceTokens) {
            // namespaceToken = String.format("namespace %s { ", s);
            namespaceStatement = namespaceStatement + "namespace " + s + " {\n";
        }

        return namespaceStatement;
    }

    protected String formatNamespaceEndStatement(String namespaceDeclaration)
    {
        String namespaceStatement = "";

        String namespaceTokens[] = namespaceDeclaration.split("::");
        for (String s : namespaceTokens) {
            // namespaceToken = String.format("namespace %s { ", s);
            namespaceStatement = namespaceStatement + "} // end namespace " + s + "\n";
        }

        return namespaceStatement;
    }

    public String removeLastCharacter(String aString)
    {
        return (aString == null || aString.length() == 0)
        ? null : (aString.substring(0, aString.length() - 1));
    }

    /**
     * returns a string with the first letter lower case.
     * @param aString of interest
     * @return same string with first letter lower case
     */
    public String initialLower(String aString)
    {
        StringBuffer stb = new StringBuffer(aString);
        stb.setCharAt(0, Character.toLowerCase(aString.charAt(0)));

        return new String(stb);
    }

    public String parseToFirstCap(String aString)
    {
        String lString = initialLower(aString);

        for (int index = 0; index < aString.length(); index++)
        {
            if (Character.isUpperCase(lString.charAt(index)))
            {
                lString = lString.substring(0,index);
                break;
            }
        }
        return lString;
    }

    public static int getEnumSize(GeneratedEnumClasses.EnumElem el)
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

   private String loadOneTemplate(String s) throws Exception
    {
        
        // This reads the entire template in
        // String templateContents = new String(Files.readAllBytes(Paths.get(getClass().getResource(s).toURI())));
        // System.out.println("Loading template : " + s);
        // System.out.println("    class : " + getClass());
        // System.out.println(" resource : " + getClass().getResource(s));
        // System.out.println("      uri : " + getClass().getResource(s).toURI());

        try {
            return new String(Files.readAllBytes(Paths.get(getClass().getResource(s).toURI())));
        } catch (Exception ex) {
            System.out.println("FAILED TO FIND template : " + s);
            throw new RuntimeException(ex);
        }
        
    }

/*
     * https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#format-java.lang.String-java.lang.Object...-
     * https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#syntax
     * @see java.util.Formatter
     * @see java.util.logging.Formatter
     */
    protected void loadEnumTemplates()
    {

        String languageFolder = this.language;

        if (this.language == null) {
            System.out.println("NULL LANGUAGE +++++++++++++++++");
            throw new RuntimeException("NO Language Set");
        }

        try {
            disenumpart1Template               = loadOneTemplate(languageFolder + "/disenumpart1.txt");

            // Ok, not the best approach
            if (this.language == CSHARP)
            {
                disenummarshalersTemplate          = loadOneTemplate(languageFolder + "/disenummarshalers.txt");
                disenum8bitmarshalerTemplate       = loadOneTemplate(languageFolder + "/disenum8bitmarshaler.txt");
                disdictenumtostringTemplate        = loadOneTemplate(languageFolder + "/disdictenumtostring.txt");
                disbitsetopersTemplate             = loadOneTemplate(languageFolder + "/disbitsetopers.txt");
            }
            if (this.language == PYTHON)
            {
                disdictenumtostringTemplate        = loadOneTemplate(languageFolder + "/disdictenumtostring.txt");
                
                disenummarshalersTemplate          = loadOneTemplate(languageFolder + "/disenummarshalers.txt");
                disenum8bitmarshalerTemplate       = loadOneTemplate(languageFolder + "/disenum8bitmarshaler.txt");
                disbitsetopersTemplate             = loadOneTemplate(languageFolder + "/disbitsetopers.txt");
                disbitsetunionTemplate             = loadOneTemplate(languageFolder + "/disbitsetunion.txt");
            }
            
            disenumcommentTemplate             = loadOneTemplate(languageFolder + "/disenumcomment.txt");
            disenumfootnotecommentTemplate     = loadOneTemplate(languageFolder + "/disenumfootnotecomment.txt");
            disenumpart2Template               = loadOneTemplate(languageFolder + "/disenumpart2.txt");
            disenumpart21Template              = loadOneTemplate(languageFolder + "/disenumpart21.txt");
            disenumpart25Template              = loadOneTemplate(languageFolder + "/disenumpart25.txt");
            disenumpart3_32Template            = loadOneTemplate(languageFolder + "/disenumpart3_32.txt");
            disenumpart3_16Template            = loadOneTemplate(languageFolder + "/disenumpart3_16.txt");
            disenumpart3_8Template             = loadOneTemplate(languageFolder + "/disenumpart3_8.txt");
            disdictenumpart1Template           = loadOneTemplate(languageFolder + "/disdictenumpart1.txt");
            disdictenumpart2Template           = loadOneTemplate(languageFolder + "/disdictenumpart2.txt");
            disdictenumpart3Template           = loadOneTemplate(languageFolder + "/disdictenumpart3.txt");
            disenumpart1withfootnoteTemplate   = loadOneTemplate(languageFolder + "/disenumpart1withfootnote.txt");
            disbitset1Template                 = loadOneTemplate(languageFolder + "/disbitset1.txt");
            disbitset15Template                = loadOneTemplate(languageFolder + "/disbitset15.txt");
            disbitset16Template                = loadOneTemplate(languageFolder + "/disbitset16.txt");
            disbitset2Template                 = loadOneTemplate(languageFolder + "/disbitset2.txt");
            disbitsetcommentxrefTemplate       = loadOneTemplate(languageFolder + "/disbitsetcommentxref.txt");
            disbitsetcommentTemplate           = loadOneTemplate(languageFolder + "/disbitsetcomment.txt");
            disbitsetgetsetTemplate            = loadOneTemplate(languageFolder + "/disbitsetgetset.txt");
            disbitsetcppstartTemplate          = loadOneTemplate(languageFolder + "/disbitsetcppstart.txt");
            disbitsetmarshalersTemplate        = loadOneTemplate(languageFolder + "/disbitsetmarshalers.txt");
            disbitsetbitcastTemplate           = loadOneTemplate(languageFolder + "/disbitsetbitcast.txt");
            if (this.language == PYTHON)
                licenseTemplate                    = loadOneTemplate("../pdus/dis7pythonlicense.txt");
            else
                licenseTemplate                    = loadOneTemplate("../pdus/dis7javalicense.txt");
            disenumfilestartTemplate           = loadOneTemplate(languageFolder + "/disenumfilestart.txt");
            disenumfileendTemplate             = loadOneTemplate(languageFolder + "/disenumfileend.txt");
            disenumstringsTemplate             = loadOneTemplate(languageFolder + "/disenumstrings.txt");
            disenumvaluesTemplate              = loadOneTemplate(languageFolder + "/disenumvalues.txt");
            disenumCtorTemplate                = loadOneTemplate(languageFolder + "/disenumctor.txt");
            disenumOperTemplate                = loadOneTemplate(languageFolder + "/disenumoper.txt");
            disenumValidTemplate               = loadOneTemplate(languageFolder + "/disenumvalid.txt");
            disenumOperDefTemplate             = loadOneTemplate(languageFolder + "/disEnumOperDef.txt");
            disenumtostringTemplate            = loadOneTemplate(languageFolder + "/disenumtostring.txt");
            disenumdescriptionsTemplate        = loadOneTemplate(languageFolder + "/disenumdescriptions.txt");
            disenumheadertemplate              = loadOneTemplate(languageFolder + "/disenumheader.txt");
            disenumcppstarttemplate            = loadOneTemplate(languageFolder + "/disenumcppstart.txt");
            disenumcppmutexTemplate            = loadOneTemplate(languageFolder + "/disenumcppmutex.txt");


            disDictEnumDescriptionStart        = loadOneTemplate(languageFolder + "/disDictEnumDescriptionStart.txt");
            disDictEnumDescriptionValue        = loadOneTemplate(languageFolder + "/disDictEnumDescriptionValue.txt");
            disDictEnumDescriptionEnd          = loadOneTemplate(languageFolder + "/disDictEnumDescriptionEnd.txt");

            disDictEnumStringsStart            = loadOneTemplate(languageFolder + "/disDictEnumStringsStart.txt");
            disDictEnumStringsValue            = loadOneTemplate(languageFolder + "/disDictEnumStringsValue.txt");
            disDictEnumStringsEnd              = loadOneTemplate(languageFolder + "/disDictEnumStringsEnd.txt");
            disDictEnumOperators               = loadOneTemplate(languageFolder + "/disDictEnumOperators.txt");
            disDictCppStartTemplate            = loadOneTemplate(languageFolder + "/disDictCppStart.txt");
            disDictHeaderOperDefTemplate       = loadOneTemplate(languageFolder + "/disDictHeaderOperDef.txt");

        }
        catch (Exception ex) {
            System.out.flush();
            System.err.println (ex.getMessage() + " Failed to load one or more templates");
            ex.printStackTrace(System.err);
        }
        // System.exit(-1);
    }

    public void writeOutFile(StringBuilder sb, String className, File outputDirectory, String fileName)
    {
        File targetFile = new File(outputDirectory, fileName);

        System.out.println("Creating file " + targetFile.getAbsolutePath());

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
   

        
}

