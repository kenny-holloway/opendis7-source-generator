/**
 * Copyright (c) 2008-2025, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */
package edu.nps.moves.dis7.source.generator.entityTypes;

import edu.nps.moves.dis7.source.generator.enumerations.GenerateEnumerations;
import edu.nps.moves.dis7.source.generator.enumerations.StringUtils;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;

import java.util.regex.Pattern;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * GenerateEntityTypes creates source code from SISO enumeration definitions.
 * Created on Jul 22, 2019 by
 * MOVES Institute, Naval Postgraduate School (NPS), Monterey California USA https://www.nps.edu
 *
 * @author Don McGregor, Mike Bailey and Don Brutzman
 * @version $Id$
 */
public class GenerateEntityTypes
{

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
        String name;
        String footnote;
        String xrefclassuid;

        public String toString()
        {
          String stringValue = "";
          stringValue += "        Name : " + name + "\n";
          stringValue += "       Value : " + value + "\n";
          stringValue += " description : " + description + "\n";
          stringValue += "    footnote : " + footnote + "\n";
          stringValue += "xrefclassuid : " + xrefclassuid + "\n";
          return stringValue;
        }
    }

    static List<EnumRowElem> countryEnums = new ArrayList();
    static List<EnumRowElem> entityKindEnums = new ArrayList();

    static List<EnumRowElem> platformDomainEnums = new ArrayList();
    static List<EnumRowElem> munitionDomainEnums = new ArrayList();
    static List<EnumRowElem> supplyDomainEnums = new ArrayList();

    Map<String, List<EnumRowElem>> enumMap = new HashMap<String, List<EnumRowElem>>();
      // enumMap.put("Country", countryEnums);
      // enumMap.put("EntityKind", entityKindEnums);
      // enumMap.put("PlatformDomain", platformDomainEnums);
      // enumMap.put("MunitionDomain", munitionDomainEnums);
      // enumMap.put("SupplyDomain", supplyDomainEnums);


    // set defaults to allow direct run
    private        File   outputDirectory;
    private static String outputDirectoryPath = "src-generated/java/edu/nps/moves/dis7/entityTypes"; // default
    private static String         packageName =                    "edu.nps.moves.dis7.entityTypes"; // default
    private static String            language = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_PROGRAMMING_LANGUAGE;
    private static String         sisoXmlFile = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_SISO_XML_FILE;
    private static String       sisoSpecificationTitleDate = "";

    String globalNamespace = "";
    String enumNamespace = "";
    String currentNamespace = "";

    /** String constant */ public static final String JAVA = "java";
    /** String constant */ public static final String CPP = "cpp";
    /** String constant */ public static final String OBJC = "objc";
    /** String constant */ public static final String CSHARP = "csharp";
    /** String constant */ public static final String JAVASCRIPT = "javascript";
    /** String constant */ public static final String PYTHON = "python";

    private BufferedWriter uid2ClassWriter = null;

    String entitytypecommonTemplate;
    String       uidfactoryTemplate;
    String          licenseTemplate;

    class TypeClassData
    {
      String pkg;
      File directory;
      StringBuilder sb;

      private String fullName;
      private String className;
      private String countryName;
      private String countryNamePretty;
      private String countryValue;
      private String entityKindName;
      private String entityKindNameDescription;
      private String entityDomainDescription; // apparently superfluous
      private String entityKindValue;
      private String entityDomainName;
      private String entityDomainValue;
    //private String domainPrettyName;
      private String entityUid;

      public String toString()
      {
        String out = "\n";

        out += "         fullName : \n" + fullName + "\n";
        out += "        className : " + className + "\n";
        out += "      countryName : " + countryName + "\n";
        out += "countryNamePretty : " + countryNamePretty + "\n";
        out += "     countryValue : " + countryValue + "\n";
        out += "   entityKindName : " + entityKindName + "\n";
        out += "  entityKindValue : " + entityKindValue + "\n";
        out += " entityDomainName : " + entityDomainName + "\n";
        out += "entityDomainValue : " + entityDomainValue + "\n";
        out += "        entityUid : " + entityUid + "\n";

        return out;
      }
    }
    
    private String        packageInfoPath;
    private File          packageInfoFile;
    private StringBuilder packageInfoBuilder;

  /** Constructor for GenerateEntityTypes
     * @param xmlFile sisoXmlFile
     * @param outputDir outputDirectoryPath
     * @param packageName key to package name for entity types */
  public GenerateEntityTypes(String xmlFile, String outputDir, String packageName, String languageToGenerate)
  {
        if (!xmlFile.isEmpty())
             sisoXmlFile = xmlFile;
        if (!outputDir.isEmpty())
            outputDirectoryPath = outputDir;
        if (!packageName.isEmpty())
           GenerateEntityTypes.packageName = packageName;
        if (!languageToGenerate.isEmpty())
          language = languageToGenerate;

        Properties systemProperties = System.getProperties();
        globalNamespace = systemProperties.getProperty("xmlpg.namespace");
          if (globalNamespace == null)
              globalNamespace = "";

          // set global namespace for enums
          enumNamespace = systemProperties.getProperty("xmlpg.enumNamespace");
          if (enumNamespace == null)
              enumNamespace = "";

        System.out.println (GenerateEntityTypes.class.getName());
        System.out.println ("              xmlFile=" + sisoXmlFile);
        System.out.println ("          packageName=" + GenerateEntityTypes.packageName);
        System.out.println ("  outputDirectoryPath=" + outputDirectoryPath);
        System.out.println ("             language=" + language);
        System.out.println("             namespace= " + globalNamespace);
        System.out.println("        enum namespace= " + enumNamespace);
        
        outputDirectory  = new File(outputDirectoryPath);
        outputDirectory.mkdirs();
//      FileUtils.cleanDirectory(outputDirectory); // do NOT clean directory, results can co-exist with other classes
        System.out.println ("actual directory path=" + outputDirectory.getAbsolutePath());
        
        if (language.toLowerCase().equals(JAVA))
        {
          packageInfoPath = outputDirectoryPath + "/" + "package-info.java";
          packageInfoFile = new File(packageInfoPath);
          
          OutputStreamWriter packageInfoFileWriter;
          FileOutputStream fso;
          try {
              packageInfoFile.createNewFile();
              fso = new FileOutputStream(packageInfoFile);
              packageInfoFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
              packageInfoBuilder = new StringBuilder();
              packageInfoBuilder.append("/**\n");
              packageInfoBuilder.append(" * The <code>entities</code>  packages provide a large number of autogenerated utility classes for world entities of interest.");
              packageInfoBuilder.append("\n");
              packageInfoBuilder.append(" * <p> Online references: </p>\n");
              packageInfoBuilder.append(" * <ul>\n");
              packageInfoBuilder.append(" *      <li> GitHub <a href=\"https://github.com/open-dis/open-dis7-java\" target=\"_blank\">open-dis7-java library</a> </li> \n");
              packageInfoBuilder.append(" *      <li> NPS <a href=\"https://gitlab.nps.edu/Savage/NetworkedGraphicsMV3500/-/tree/master/examples/src/OpenDis7Examples\" target=\"MV3500\">MV3500 Distributed Simulation Fundamentals course examples</a> </li> \n");
              packageInfoBuilder.append(" *      <li> <a href=\"https://gitlab.nps.edu/Savage/NetworkedGraphicsMV3500/-/tree/master/specifications/README.md\" target=\"README.MV3500\">IEEE and SISO specification references</a> of interest</li> \n");
              packageInfoBuilder.append(" *      <li> <a href=\"https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&amp;EntryId=46172\" target=\"SISO-REF-010\" >SISO-REF-010-2022 Reference for Enumerations for Simulation Interoperability</a> </li> \n");
              packageInfoBuilder.append(" *      <li> <a href=\"https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&amp;EntryId=47284\" target=\"SISO-REF-10.1\">SISO-REF-10.1-2019 Reference for Enumerations for Simulation, Operations Manual</a></li>\n");
              packageInfoBuilder.append(" *      <li> <a href=\"https://savage.nps.edu/open-dis7-java/javadoc\" target=\"_blank\">open-dis7 Javadoc</a>, <a href=\"https://savage.nps.edu/open-dis7-java/xml/DIS_7_2012.autogenerated.xsd\" target=\"_blank\">open-dis7 XML Schema</a>and <a href=\"https://savage.nps.edu/open-dis7-java/xml/SchemaDocumentation\" target=\"_blank\">open-dis7 XML Schema documentation</a></li> </ul>\n");
              packageInfoBuilder.append("\n");
              packageInfoBuilder.append(" * @see java.lang.Package\n");
              packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/22095487/why-is-package-info-java-useful\">Stack Overflow: why-is-package-info-java-useful</a>\n");
              packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/624422/how-do-i-document-packages-in-java\">Stack Overflow: how-do-i-document-packages-in-java</a>\n");
              packageInfoBuilder.append(" */\n");
              packageInfoBuilder.append("\n");
              packageInfoBuilder.append("package edu.nps.moves.dis7.entities;\n");

              packageInfoFileWriter.write(packageInfoBuilder.toString());
              packageInfoFileWriter.flush();
              packageInfoFileWriter.close();
              System.out.println("Created " + packageInfoPath);
          }
          catch (IOException ex) {
              System.out.flush(); // avoid intermingled output
              System.err.println (ex.getMessage()
                + packageInfoFile.getAbsolutePath()
              );
              ex.printStackTrace(System.err);
          }
        }
  }

  Method methodPlatformDomainFromIntMethod;
  Method methodPlatformDomainNameMethod;
  Method methodPlatformDomainDescriptionMethod;
  
  Method countryFromIntMethod;
  Method countryNameMethod;
  Method countryDescriptionMethod;
  
  Method kindFromIntMethod;
  Method kindNameMethod;
  Method kindDescriptionMethod;

  Method munitionDomainFromIntMethod;
  Method munitionDomainNameMethod;
  Method munitionDomainDescriptionMethod;
  
  Method supplyDomainFromIntMethod;
  Method supplyDomainNameMethod;
  Method supplyDomainDescriptionMethod;

  // private String DescriptionMethod(String enumName, int enumValue)
  // {
  //   enumMap.get(enumName).get(enumValue).description;
  // }

  // PlatformDomain getter methods
    private static String PlatformDomainDescriptionMethod(int aValue)
    {
      return GetEnumDescription(platformDomainEnums, aValue);
    }
    private static String PlatformDomainFromIntMethod(int aValue)
    {
      return "PlatformDomain." + GetEnumName(platformDomainEnums, aValue);
    }
    private static String PlatformDomainNameMethod(int aValue)
    {
      return platformDomainEnums.get(aValue).name;
    }

    private static Optional<EnumRowElem> GetEnumByValue(List<EnumRowElem> enumList, String aValue)
    {
      return enumList.stream()
          .filter(p -> p.value.equals(aValue))
          .findFirst();
    }

    private static String GetEnumDescription(List<EnumRowElem> enumList, int aValue)
    {
        String description = "NotFound";

        Optional<EnumRowElem> enumElement = GetEnumByValue(enumList, Integer.toString(aValue));
        if (enumElement.isPresent())
        {
           description = enumElement.get().description;
        }

        return description;
    }

    private static String GetEnumName(List<EnumRowElem> enumList, int aValue)
    {
        String name = "NotFound";

        Optional<EnumRowElem> enumElement = GetEnumByValue(enumList, Integer.toString(aValue));
        if (enumElement.isPresent())
        {
           name = enumElement.get().name;
        }

        return name;
    }

  // Country getter methods
    private static String CountryDescriptionMethod(int aValue)
    {
      return GetEnumDescription(countryEnums, aValue);
    }

    private static String CountryFromIntMethod(int aValue)
    {
      return "countr." + GetEnumName(countryEnums, aValue);
    }

    private static String CountryNameMethod(int aValue)
    {
      return GetEnumName(countryEnums, aValue);
    }

  // Entity Kind getter methods
    private static String KindDescriptionMethod(int aValue)
    {
      return GetEnumDescription(entityKindEnums, aValue);
    }
    private static String KindFromIntMethod(int aValue)
    {
      return "EntityKind." + GetEnumName(entityKindEnums, aValue);
    }
    private static String KindNameMethod(int aValue)
    {
      return GetEnumName(entityKindEnums, aValue);
    }

  // Munition Domain getter methods
    private static String MunitionDomainDescriptionMethod(int munitionValue)
    {
      return GetEnumDescription(munitionDomainEnums, munitionValue);
    }
    private static String MunitionDomainFromIntMethod(int aValue)
    {
      return "MunitionDomain." + GetEnumName(munitionDomainEnums, aValue);

    }
    private static String MunitionDomainNameMethod(int aValue)
    {
      return GetEnumName(munitionDomainEnums, aValue);
    }

  // Supply Domain Getter Methods
    private static String SupplyDomainDescriptionMethod(int supplyValue)
    {
      return GetEnumDescription(supplyDomainEnums, supplyValue);
    }
    private static String SupplyFromIntMethod(int aValue)
    {
      return "SupplyDomain." + GetEnumName(supplyDomainEnums, aValue);
    }
    private static String SupplyDomainNameMethod(int aValue)
    {
      return GetEnumName(supplyDomainEnums, aValue);
    }

  // Don't put imports in code for this, needs to have enumerations built first; do it this way
  // Update, this might now be unnecessary with the re-structuring of the projects
  private void buildKindDomainCountryInstances()
  {
    Method[] ma;
    try {
      ma = getEnumMethods("edu.nps.moves.dis.enumerations.PlatformDomain");
      methodPlatformDomainFromIntMethod     = ma[FORVALUE];
      methodPlatformDomainNameMethod        = ma[NAME];
      methodPlatformDomainDescriptionMethod = ma[DESCRIPTION];

      ma = getEnumMethods("edu.nps.moves.dis7.enumerations.Country");
      countryFromIntMethod     = ma[FORVALUE];
      countryNameMethod        = ma[NAME];
      countryDescriptionMethod = ma[DESCRIPTION];

      ma = getEnumMethods("edu.nps.moves.dis7.enumerations.EntityKind");
      kindFromIntMethod     = ma[FORVALUE];
      kindNameMethod        = ma[NAME];
      kindDescriptionMethod = ma[DESCRIPTION];

      ma = getEnumMethods("edu.nps.moves.dis7.enumerations.MunitionDomain");
      munitionDomainFromIntMethod     = ma[FORVALUE];
      munitionDomainNameMethod        = ma[NAME];
      munitionDomainDescriptionMethod = ma[DESCRIPTION];

      ma = getEnumMethods("edu.nps.moves.dis7.enumerations.SupplyDomain");
      supplyDomainFromIntMethod     = ma[FORVALUE];
      supplyDomainNameMethod        = ma[NAME];
      supplyDomainDescriptionMethod = ma[DESCRIPTION];
    }
    catch (Exception ex) {
      throw new RuntimeException(ex.getClass().getName() + ": " + ex.getLocalizedMessage());
    }
  }
  private static final int FORVALUE = 0;
  private static final int NAME = 1;
  private static final int DESCRIPTION = 2;

  // Get the String enum rep for an enumerated value
  private Method getEnumForValueMethod(String className)
  {
    Method enumValueMethod = null;

    try
    {
      if (className.contains("PlatformDomain"))
      {
        enumValueMethod = this.getClass().getDeclaredMethod("PlatformDomainFromIntMethod", int.class);
      }
      else if (className.contains("Country"))
      {
        enumValueMethod = this.getClass().getDeclaredMethod("CountryFromIntMethod", int.class);
      }

      else if (className.contains("EntityKind"))
      {
        enumValueMethod = this.getClass().getDeclaredMethod("KindFromIntMethod", int.class);
      }

      else if (className.contains("MunitionDomain"))
      {
        enumValueMethod = this.getClass().getDeclaredMethod("MunitionDomainFromIntMethod", int.class);
      }

      else if (className.contains("SupplyDomain"))
      {
        enumValueMethod = this.getClass().getDeclaredMethod("SupplyFromIntMethod", int.class);
      }
    }
    catch (Exception e)
    {
      System.err.println ("ERROR : " + this.getClass().getName() + ".getEnumValueMethod() exception:" + e);
    }

    return enumValueMethod;
  }

  private Method getNameMethod(String className)
  {
    Method nameMethod = null;

      try
      {
        if (className.contains("PlatformDomain"))
        {
          nameMethod = this.getClass().getDeclaredMethod("PlatformDomainNameMethod", int.class);
        }
        else if (className.contains("Country"))
        {
          nameMethod = this.getClass().getDeclaredMethod("CountryNameMethod", int.class);
        }

        else if (className.contains("EntityKind"))
        {
          nameMethod = this.getClass().getDeclaredMethod("KindNameMethod", int.class);
        }

        else if (className.contains("MunitionDomain"))
        {
          nameMethod = this.getClass().getDeclaredMethod("MunitionDomainNameMethod", int.class);
        }

        else if (className.contains("SupplyDomain"))
        {
          nameMethod = this.getClass().getDeclaredMethod("SupplyDomainNameMethod", int.class);
        }
      }
      catch (Exception e)
      {
        System.err.println ("ERROR : " + this.getClass().getName() + ".getDescription() exception:" + e);
      }

      return nameMethod;

  }

  private Method getDescriptionMethod(String className)
  {
      // Class[] cArgs = new Class[1];
      // cArgs[0] = int.class;

      Method descriptionMethod = null;

      try
      {
        if (className.contains("PlatformDomain"))
        {
          // Class cls = this.getClass();
          descriptionMethod = this.getClass().getDeclaredMethod("PlatformDomainDescriptionMethod", int.class);
        }

        else if (className.contains("Country"))
        {
          descriptionMethod = this.getClass().getDeclaredMethod("CountryDescriptionMethod", int.class);
        }

        else if (className.contains("EntityKind"))
        {
          descriptionMethod = this.getClass().getDeclaredMethod("KindDescriptionMethod", int.class);
        }

        else if (className.contains("MunitionDomain"))
        {
          descriptionMethod = this.getClass().getDeclaredMethod("MunitionDomainDescriptionMethod", int.class);
        }

        else if (className.contains("SupplyDomain"))
        {
          descriptionMethod = this.getClass().getDeclaredMethod("SupplyDomainDescriptionMethod", int.class);
        }
      }
      catch (Exception e)
      {
        System.err.println ("ERROR : " + this.getClass().getName() + ".getDescription() exception:" + e);
      }

      return descriptionMethod;
  }

  private Method[] getEnumMethods(String className) throws Exception
  {
    Method[] ma = new Method[3];

    if (language.toLowerCase().equals(JAVA))
    {
      Class<?> cls = Class.forName(className);
      ma[FORVALUE] = cls.getDeclaredMethod("getEnumForValue", int.class);
      ma[NAME] = cls.getMethod("name", (Class[]) null);
      ma[DESCRIPTION] = cls.getDeclaredMethod("getDescription", (Class[]) null);
    }
    else
    {
      ma[FORVALUE]    = getEnumForValueMethod(className);
      ma[NAME]        = getNameMethod(className);
      ma[DESCRIPTION] = getDescriptionMethod(className);
    }

    return ma;
  }

  String getDescription(Method enumGetter, Method descriptionGetter, int i) throws Exception
  {
      String result = "";
      
      if (enumGetter == null) System.out.println(GenerateEntityTypes.class.getName() + ".getDescription : enumGetter is null");
      if (descriptionGetter == null) System.out.println(GenerateEntityTypes.class.getName() + ".getDescription : descriptionGetter is null");

      if ((enumGetter == null) || (descriptionGetter == null))
          return result;
      try 
      {
        Object enumObj = null;
        
        if (language.toLowerCase().equals(JAVA))
        {
          enumObj = getEnum(enumGetter, i);
          result = (String) descriptionGetter.invoke(enumObj, (Object[]) null);
        }
        else
        {
          result = (String) descriptionGetter.invoke(enumObj, i);
        }
        
      }
      catch (Exception e)
      {
          System.err.println (this.getClass().getName() + ".getDescription() exception:" + e);
      }

      return result;
  }

  String getName(Method enumGetter, Method nameGetter, int i) throws Exception
  {
      if (enumGetter == null)
          return ""; // TODO fix

    String result = "";
    Object enumObj = null;

    if (language.toLowerCase().equals(JAVA))
    {
      enumObj = getEnum(enumGetter, i);
      result = (String) nameGetter.invoke(enumObj, (Object[]) null);
    }
    else
    {
      // System.out.println("Name getter method : " + nameGetter.toString());
      if (nameGetter == null)
      {
        System.out.println("NameGetter is null");
        System.out.print(Thread.currentThread().getStackTrace());
      }
      result = (String) nameGetter.invoke(enumObj, i);
    }

    return result;
    
  }

  Object getEnum(Method enumGetter, int i) throws Exception
  {
    if (enumGetter == null)
    {
        System.err.println ("NPE: getEnum (enumGetter == null, i=" + i + ")");
        return null;
    }
    return enumGetter.invoke(null, i);
  }

  private void run() throws SAXException, IOException, ParserConfigurationException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(true);

    loadTemplates();
    buildKindDomainCountryInstances(); // TODO questionable invocation, now working...
    
    if (!language.toLowerCase().equals(JAVA))
    {
      // Parse the XML file to load the enumeration data
      System.out.println("Begin processsing required enumertion values");
      EnumCollector enumCollector = new EnumCollector();
      factory.newSAXParser().parse(new File(sisoXmlFile), enumCollector);

      System.out.println("Completed parsing enumerations");

      System.out.println("  countryEnums        : " + countryEnums.size());
      System.out.println("  entityKindEnums     : " + entityKindEnums.size());
      System.out.println("  platformDomainEnums : " + platformDomainEnums.size());
      System.out.println("  munitionDomainEnums : " + munitionDomainEnums.size());
      System.out.println("  supplyDomainEnums   : " + supplyDomainEnums.size());
      // for (int index = 0; index < 2; index++) 
      //   System.out.println(countryEnums.get(index).toString());

      // for (int index = 0; index < platformDomainEnums.size(); index++) 
      //   System.out.println(platformDomainEnums.get(index).toString());

      // for (int index = 0; index < supplyDomainEnums.size(); index++) 
      //   System.out.println(supplyDomainEnums.get(index).toString());


    }

    System.out.println("Generating entities:");
    MyHandler handler = new MyHandler();
    factory.newSAXParser().parse(new File(sisoXmlFile), handler);
    
    if(uid2ClassWriter != null) 
    {
       uid2ClassWriter.flush();
       uid2ClassWriter.close();
    }
    saveUidFactory();
    System.out.println (GenerateEntityTypes.class.getName() + " complete."); // TODO  + handler.enums.size() + " enums created.");
  }

  private void loadTemplates()
  {

    String languageFolder = this.language;
    if (language.equals(JAVA))
      languageFolder = "";

    try {
      if (language.equals(PYTHON))
        licenseTemplate          = loadOneTemplate("../pdus/dis7pythonlicense.txt");
      else
        licenseTemplate          = loadOneTemplate("../pdus/dis7javalicense.txt");
      entitytypecommonTemplate = loadOneTemplate("../entitytypes/" + languageFolder + "/entitytypecommon.txt");
      uidfactoryTemplate       = loadOneTemplate("../entitytypes/" + languageFolder + "/uidfactory.txt");
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private String loadOneTemplate(String s) throws Exception
  {

    // String templateContents = new String(Files.readAllBytes(Paths.get(getClass().getResource(s).toURI())));
    System.out.println("Loading template : " + s);
    // System.out.println("     path : " + Paths.get(getClass()).normalize().toString());
    System.out.println("    class : " + getClass());
    System.out.println(" resource : " + getClass().getResource(s));
    System.out.println("      uri : " + getClass().getResource(s).toURI());

    return new String(Files.readAllBytes(Paths.get(getClass().getResource(s).toURI())), StandardCharsets.UTF_8.name());
  }
  
  private void saveUidFactory()
  {
    saveFile(outputDirectory, "EntityTypeFactory.java",uidfactoryTemplate);
  }
  
  class DescriptionElem
  {
    String description            = new String();
    String packageFromDescription = new String();
    String enumFromDescription    = new String();
    ArrayList<DescriptionElem> children = new ArrayList<>();
    String value                  = new String();
    String uid                    = new String();

    public String toString()
    {
      String retVal = "";
      retVal += "  Kind : " + description + "\n";
      retVal += "   pkg : " + packageFromDescription + "\n";
      retVal += "  enum : " + enumFromDescription + "\n";
      retVal += " Value : " + value + "\n";
      retVal += "   UID : " + uid + "\n";

      return retVal;
    }
  }

  class EntityElem
  {
    String kind    = new String();
    String domain  = new String();
    String country = new String();
    String uid     = new String();
    List<DescriptionElem> categories = new ArrayList<>();

    public String toString()
    {
      String retVal = "   Kind : " + kind + "\n";
      retVal += " Domain : " + domain + "\n";
      retVal += "Country : " + country + "\n";
      retVal += "    UID : " + uid + "\n";

      return retVal;
    }

  }

  class CategoryElem extends DescriptionElem
  {
    EntityElem parent;
  }

  class SubCategoryElem extends DescriptionElem
  {
    CategoryElem parent;
  }

  class SpecificElem extends DescriptionElem
  {
    SubCategoryElem parent;
  }

  class ExtraElem extends DescriptionElem
  {
    SpecificElem parent;
  }


  public class EnumCollector extends DefaultHandler
  {
    EnumElem currentEnum;
    EnumRowElem currentEnumRow;

    public EnumCollector()
    {
      super();
    }

    public boolean IsEnumOfInterest(String enumString)
    {
      boolean isInteresting = false;

      int enumValue = Integer.parseInt(enumString);

      switch (enumValue)
      {
        case 7:     // EntityKind
        case 8:     // PlatformDomain
        case 14:    // MunitionDomain
        case 29:    // Country
        case 600:   // SupplyDomain
          isInteresting = true;
          break;
      }
      
      return isInteresting;
    }

    void HandleEnumValue()
    {
      if (currentEnumRow == null) return;

      switch (currentEnum.name)
      {
        case "Country":
          countryEnums.add(currentEnumRow);
          break;
        case "SupplyDomain":
          supplyDomainEnums.add(currentEnumRow);
          break;
        case "MunitionDomain":
          munitionDomainEnums.add(currentEnumRow);
          break;
        case "PlatformDomain":
          platformDomainEnums.add(currentEnumRow);
          break;
        case "EntityKind":
          entityKindEnums.add(currentEnumRow);
          break;
        default:
          System.out.println("Enum name : " + currentEnum.name);
          break;
      }

    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    {
      switch (qName)
      {
        case "enum":
          // See if we can process an enum
          String uidString = attributes.getValue("uid");
          if (!IsEnumOfInterest(uidString)) break;

          currentEnum = new EnumElem();
          currentEnum.name = StringUtils.fixName(attributes.getValue("name")); // name canonicalization C14N
          currentEnum.uid  = attributes.getValue("uid");
          if (currentEnum.footnote != null)
              currentEnum.footnote = normalizeDescription(currentEnum.footnote);
          break;

          case "enumrow":
            if (currentEnum == null) break;

            currentEnumRow = new EnumRowElem();
            currentEnumRow.description = attributes.getValue("description");
            if (currentEnumRow.description != null)
              currentEnumRow.description = normalizeDescription(currentEnumRow.description);
            currentEnumRow.value = attributes.getValue("value");
            currentEnumRow.name = StringUtils.createEnumName(StringUtils.normalizeDescription(currentEnumRow.description), false);
            HandleEnumValue();
            break;
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
      switch (qName)
      {
          case "enum":
              currentEnum = null;
              break;

          // case "enumrow":
          //     currentEnumRow = null;
          //     break;
      }
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
    ArrayList<EntityElem> entities = new ArrayList<>();
    EntityElem            currentEntity;
    CategoryElem          currentCategory;
    SubCategoryElem       currentSubCategory;
    SpecificElem          currentSpecific;
    ExtraElem             currentExtra;
    boolean               inCot  = false;   // we don't want categories, subcategories, etc. from this group
    boolean               inCetUid30 = false;
    int                   filesWrittenCount = 0;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    {

      if (inCot)   // don't want anything in this group
        return;

      if (attributes.getValue("deprecated") != null)
        return;
      
      if(qName.equalsIgnoreCase("revision")) {
        if(sisoSpecificationTitleDate.isEmpty()) // only want the first/latest
           sisoSpecificationTitleDate = legalJavaDoc(attributes.getValue("title") + " (" + attributes.getValue("date") + ")");
        return;
      }
      if (qName.equalsIgnoreCase("cet")) {
        if (attributes.getValue("uid").equalsIgnoreCase("30"))
          inCetUid30 = true;
        return;
      }
      // must follow setting of flag above
      if (!inCetUid30) // only want entities in this group, uid 30
        return;
      

      switch (qName) 
      {
        case "cot":
          inCot = true; // cot element, object types
          break;

        case "entity":
          currentEntity         = new EntityElem();
          currentEntity.kind    = attributes.getValue("kind");
          currentEntity.domain  = attributes.getValue("domain");
          currentEntity.country = attributes.getValue("country");
          currentEntity.uid     = attributes.getValue("uid");
          
          if (currentCategory != null) // attribute order independent
          {
                currentCategory.parent = currentEntity;
                setUniquePackageAndEmail(currentCategory, currentEntity.categories);
                currentEntity.categories.add(currentCategory);
          }
          entities.add(currentEntity);
          break;

        case "category":
          currentCategory                  = new CategoryElem();
          currentCategory.value            = attributes.getValue("value");
          currentCategory.description      = attributes.getValue("description");
          if (currentCategory.description != null)
              currentCategory.description  = normalizeDescription(currentCategory.description);
          currentCategory.uid              = attributes.getValue("uid");

          if (currentSubCategory != null) // attribute order independent
          {
                currentSubCategory.parent  = currentCategory;
                setUniquePackageAndEmail(currentSubCategory, currentCategory.children);
                currentCategory.children.add(currentSubCategory);
          }
          if (currentEntity != null) // attribute order independent
          {
                currentCategory.parent = currentEntity;
                setUniquePackageAndEmail(currentCategory, currentEntity.categories);
                currentEntity.categories.add(currentCategory);
          }
          break;

        case "subcategory_xref":
          printUnsupportedContainedELementMessage("subcategory_xref", attributes.getValue("description"), currentCategory);
          break;

        case "subcategory_range":
          printUnsupportedContainedELementMessage("subcategory_range", attributes.getValue("description"), currentCategory);
          break;
          
        case "subcategory":
          currentSubCategory                  = new SubCategoryElem();
          currentSubCategory.value            = attributes.getValue("value");
          currentSubCategory.description      = attributes.getValue("description");
          if (currentSubCategory.description != null)
              currentSubCategory.description  = normalizeDescription(currentSubCategory.description);
          currentSubCategory.uid              = attributes.getValue("uid");
          
          if (currentCategory != null) // attribute order independent
          {
                currentSubCategory.parent = currentCategory;
                setUniquePackageAndEmail(currentSubCategory, currentCategory.children);
                currentCategory.children.add(currentSubCategory);
          }
          break;

        case "specific_range":
          printUnsupportedContainedELementMessage("specific_range", attributes.getValue("description"), currentSubCategory);
          break;

        case "specific":
          currentSpecific                  = new SpecificElem();
          currentSpecific.value            = attributes.getValue("value");
          currentSpecific.description      = attributes.getValue("description");
          if (currentSpecific.description != null)
              currentSpecific.description  = normalizeDescription(currentSpecific.description);
          currentSpecific.uid              = attributes.getValue("uid");
          currentSpecific.parent           = currentSubCategory;
          
          if ((currentSubCategory != null) && (currentSubCategory.children != null))
          {
              currentSubCategory.children.add(currentSpecific);
              setUniquePackageAndEmail(currentSpecific, currentSubCategory.children);
          }
          else if (currentExtra != null) // attribute order independent
          {
                currentExtra.parent = currentSpecific;
                setUniquePackageAndEmail(currentExtra, currentSpecific.children);
                currentSpecific.children.add(currentExtra);
          }
          break;

        case "extra":
          currentExtra                  = new ExtraElem();
          currentExtra.value            = attributes.getValue("value");
          currentExtra.description      = attributes.getValue("description");
          if (currentExtra.description != null)
              currentExtra.description  = normalizeDescription(currentExtra.description);
          currentExtra.uid              = attributes.getValue("uid");
          
          if (currentSpecific != null) // attribute order independent
          {
                currentExtra.parent = currentSpecific;
                setUniquePackageAndEmail(currentExtra, currentSpecific.children);
                currentSpecific.children.add(currentExtra);
          }
          break;

        default:

      }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
      
      if (qName == null)
        return;

      if (!qName.equals("cot") && inCot)
        return;

      try {
        switch (qName) {
          case "cot":
            inCot = false;
            break;

          case "cet":
            if (inCetUid30)
              inCetUid30 = false;
            break;

          case "entity":
            currentEntity = null;
            break;

          case "category":
            if (currentCategory != null) //might have been deprecated
                writeCategoryFile(null);
            currentCategory = null; // reset for next time
            // System.exit(0);
            break;

          case "subcategory":
            if (currentSubCategory != null) // might have been deprecated
                writeSubCategoryFile(null); // Datapkt
            currentSubCategory = null; // reset for next time
            break;

          case "specific":
            if (currentSpecific != null) // might have been deprecated
              writeSpecificFile(null);
            currentSpecific = null; // reset for next time
            break;

          case "extra":
            if (currentExtra != null) // might have been deprecated
                writeExtraFile(null);
            currentExtra = null; // reset for next time
            break;

          case "subcategory_xref":
          case "subcategory_range":
          case "specific_range":
              // not supported
            break;

          default:
            break;
        }
      }
      catch (Exception ex) {
        System.err.println("endElement qName=" + qName + ", " + ex.getMessage());
        throw new RuntimeException(ex);
      }

    }

    @Override
    public void endDocument() throws SAXException
    {
    }
    
    private void addToPropertiesFile(String pkg, String className, String uid)
    {
      try {
        if(uid2ClassWriter == null)
          buildUid2ClassWriter();
        uid2ClassWriter.write(uid+"="+pkg+"."+className);
        uid2ClassWriter.newLine();
      }
      catch(IOException ex) {
        throw new RuntimeException (ex);
      }
    }
    
    private void buildUid2ClassWriter() throws IOException
    {
      File f = new File(outputDirectory,"uid2EntityClass.properties");
      f.createNewFile();
      uid2ClassWriter = new BufferedWriter(new FileWriter(f));
    }
    
  
    private void saveEntityFile(TypeClassData data, String uid)
    {
        String fileExtension = StringUtils.getSourceFileExtension(language);

        // end the ctor statement
        if (!language.toLowerCase().equals(PYTHON))
          data.sb.append("    }\n");

        if (!language.toLowerCase().equals(JAVA))
        {
          if (language.toLowerCase().equals(CPP))
            data.sb.append("};\n");
          else if (!language.toLowerCase().equals(PYTHON))
            data.sb.append("}\n");

          data.sb.append("\n");
          data.sb.append(StringUtils.formatNamespaceEndStatement(currentNamespace, language));
          saveFile(data.directory, data.className + fileExtension, data.sb.toString());
          return;
        }

        data.sb.append("    /** Create a new instance of this final (unmodifiable) class\n");
        data.sb.append("      * @return copy of class for use as data */\n");
        data.sb.append("    public static " + data.className + " createInstance()\n");
        data.sb.append("    {\n");
        data.sb.append("            return new " + data.className + "();\n");
        data.sb.append("    }\n");
        data.sb.append("}\n");

        saveFile(data.directory, data.className + fileExtension, data.sb.toString());
        
        addToPropertiesFile(data.pkg, data.className, uid);
        
        packageInfoPath = data.directory + "/" + "package-info.java";
        File   packageInfoFile = new File(packageInfoPath);
      
        if (!packageInfoFile.exists()) // write package-info.java during first time through
        {
          OutputStreamWriter packageInfoFileWriter;
            FileOutputStream fso;
            try {
                packageInfoFile.createNewFile();
                fso = new FileOutputStream(packageInfoFile);
                packageInfoFileWriter = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
                packageInfoBuilder = new StringBuilder();
                packageInfoBuilder.append("/**\n");
                if (!data.countryValue.isEmpty())
                    packageInfoBuilder.append(" ").append(data.countryNamePretty);
                if (!data.entityKindName.isEmpty())
                    packageInfoBuilder.append(" ").append(data.entityKindName);
                if (!data.entityDomainName.isEmpty())
                    packageInfoBuilder.append(" ").append(data.entityDomainValue);
                if (!data.countryValue.isEmpty() && !data.entityKindName.isEmpty() && !data.entityDomainValue.isEmpty())
                    packageInfoBuilder.append(" t");
                else
                    packageInfoBuilder.append(" T");
                packageInfoBuilder.append("yped classes for world entities defined by ").append(sisoSpecificationTitleDate).append(" enumerations.\n");
                packageInfoBuilder.append("\n");
                packageInfoBuilder.append(" * <p> Online references: </p>\n");
                packageInfoBuilder.append(" * <ul>\n");
                packageInfoBuilder.append(" *      <li> GitHub <a href=\"https://github.com/open-dis/open-dis7-java\" target=\"_blank\">open-dis7-java library</a> </li> \n");
                packageInfoBuilder.append(" *      <li> NPS <a href=\"https://gitlab.nps.edu/Savage/NetworkedGraphicsMV3500/-/tree/master/examples/src/OpenDis7Examples\" target=\"MV3500\">MV3500 Distributed Simulation Fundamentals course examples</a> </li> \n");
                packageInfoBuilder.append(" *      <li> <a href=\"https://gitlab.nps.edu/Savage/NetworkedGraphicsMV3500/-/tree/master/specifications/README.md\" target=\"README.MV3500\">IEEE and SISO specification references</a> of interest</li> \n");
                packageInfoBuilder.append(" *      <li> <a href=\"https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&amp;EntryId=46172\" target=\"SISO-REF-010\" >SISO-REF-010-2022 Reference for Enumerations for Simulation Interoperability</a> </li> \n");
                packageInfoBuilder.append(" *      <li> <a href=\"https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&amp;EntryId=47284\" target=\"SISO-REF-10.1\">SISO-REF-10.1-2019 Reference for Enumerations for Simulation, Operations Manual</a></li>\n");
                packageInfoBuilder.append(" *      <li> <a href=\"https://savage.nps.edu/open-dis7-java/javadoc\" target=\"_blank\">open-dis7 Javadoc</a>, <a href=\"https://savage.nps.edu/open-dis7-java/xml/DIS_7_2012.autogenerated.xsd\" target=\"_blank\">open-dis7 XML Schema</a>and <a href=\"https://savage.nps.edu/open-dis7-java/xml/SchemaDocumentation\" target=\"_blank\">open-dis7 XML Schema documentation</a></li> </ul>\n");
                packageInfoBuilder.append("\n");
                packageInfoBuilder.append(" * @see java.lang.Package\n");
                packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/22095487/why-is-package-info-java-useful\">Stack Overflow: why-is-package-info-java-useful</a>\n");
                packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/624422/how-do-i-document-packages-in-java\">Stack Overflow: how-do-i-document-packages-in-java</a>\n");
                packageInfoBuilder.append(" */\n");
                packageInfoBuilder.append("// created by edu/nps/moves/dis7/source/generator/entityTypes/GenerateEntityTypes.java\n");
                packageInfoBuilder.append("\n");
                packageInfoBuilder.append("package ").append(data.pkg).append(";\n");

                packageInfoFileWriter.write(packageInfoBuilder.toString());
                packageInfoFileWriter.flush();
                packageInfoFileWriter.close();
                // System.out.println("Created " + packageInfoPath);
            }
            catch (IOException ex) {
                System.out.flush(); // avoid intermingled output
                System.err.println (ex.getMessage()
                   + packageInfoFile.getAbsolutePath()
                );
                ex.printStackTrace(System.err);
            }
        }
    }
  
    private void appendCommonStatements(TypeClassData data)
    {
      String currentSpecificName  = new String(); // handle potential nulls
      String currentSpecificValue = new String();
      int currentSpecificNumericValue = 0;


      if (currentSpecific != null)
      {
          currentSpecificName  = fixName(currentSpecific.description);
          currentSpecificValue = " = " + "<code>" + currentSpecific.value + "</code>";
          currentSpecificNumericValue = Integer.parseInt(currentSpecific.value);
      }
      else
      {
          currentSpecificValue  = "<code>(none)</code>"; // second of two entries to avoid whitespace
      }

      String currentSubCategoryName  = new String(); // handle potential nulls
      String currentSubCategoryValue = new String();
      int currentSubCategoryNumericValue = 0;
      if (currentSubCategory != null)
      {
          currentSubCategoryName  = fixName(currentSubCategory.description);
          // TODO often subcategories are replicated across different packages
          currentSubCategoryName  = currentSubCategoryName.substring(0,1) + 
                                    currentSubCategoryName.substring(1).replace("_","");
          currentSubCategoryValue = " = " + "<code>" + currentSubCategory.value + "</code>";
          currentSubCategoryNumericValue = Integer.parseInt(currentSubCategory.value);
      }
      else
      {
          currentSubCategoryValue  = "<code>(none)</code>"; // second of two entries to avoid whitespace
      }
      String seeCurrentCategory = new String();
      if (!currentCategory.packageFromDescription.isEmpty())
          seeCurrentCategory = " * @see Category\n" + " * @see " + currentCategory.packageFromDescription;
      String seeCurrentSubcategory = new String();
      if (!currentSubCategoryName.isEmpty())
          seeCurrentSubcategory = " * @see SubCategory\n";
          // TODO often package-specific since repeated, sometimes numeric:  + " * @see " + currentSubCategoryName;

      String usingNamespaceStatement = "";
      if (language.toLowerCase().equals(JAVA))
      {
        currentNamespace = "";
      }
      else if (language.toLowerCase().equals(CPP))
      {
        usingNamespaceStatement += "#include <" + globalNamespace + "/EntityType.h>\n";
        usingNamespaceStatement += "#include <" + globalNamespace + "/enumerations/Country.h>\n";
        usingNamespaceStatement += "#include <" + globalNamespace + "/enumerations/EntityKind.h>\n";
        usingNamespaceStatement += "#include <" + globalNamespace + "/enumerations/PlatformDomain.h>\n";
        usingNamespaceStatement += "#include <" + globalNamespace + "/enumerations/MunitionDomain.h>\n";
        usingNamespaceStatement += "#include <" + globalNamespace + "/enumerations/SupplyDomain.h>\n";
        usingNamespaceStatement += "\n";
        usingNamespaceStatement += "using namespace " + enumNamespace  + ";";
      }
      else if (language.toLowerCase().equals(PYTHON))
      {
        usingNamespaceStatement += "from " + globalNamespace + ".entity_type import EntityType\n";
        usingNamespaceStatement += "from " + globalNamespace + ".domain import Domain\n";
        usingNamespaceStatement += "from " + enumNamespace + ".country import Country\n";
        usingNamespaceStatement += "from " + enumNamespace + ".entity_kind import EntityKind\n";
      }
      else
      {
        usingNamespaceStatement  = "using " + enumNamespace + ";";
      }

      String contents = "";

      if (language.toLowerCase().equals(PYTHON))
      {

        contents = String.format(entitytypecommonTemplate,
                                      data.pkg,     // package comment
                                      usingNamespaceStatement,
                                      data.className,                                     // Opening sentence
                                      data.className,                 data.className,     // Usage 

                                      data.countryNamePretty,         data.countryValue,
                                      data.entityDomainName,          data.entityDomainValue,
                                      data.entityKindNameDescription, data.entityKindValue,
                                      currentCategory.description,    currentCategory.value,
                                      currentSubCategoryName,         currentSubCategoryValue,
                                      currentSpecificName,            currentSpecificValue,
                                      data.entityUid,

                                      sisoSpecificationTitleDate,                         // URL

                                      data.fullName,                                      // Full name. this outputs it all
                                      data.countryName,                                   // @see Country#*
                                      data.entityKindName,                                // @see EntityKind#*
                                      data.entityDomainName,                              // @see %s (interface Domain)
                                      seeCurrentCategory,                                 // @see Category (if present)
                                      seeCurrentSubcategory,                              // @see SubCategory (if present)
                                      data.className,
                                      data.entityKindName,
                                      data.entityDomainName, data.entityDomainValue,
                                      data.countryName,
                                      currentCategory.value,
                                      currentSubCategoryNumericValue,
                                      currentSpecificNumericValue);
      }
      else
      {
        contents = String.format(entitytypecommonTemplate,       data.pkg,     // Class definition 
                                      usingNamespaceStatement,
                                      data.className,                                     // Opening sentence
                                      data.className,                 data.className, // Usage 
                                      data.countryNamePretty,         data.countryValue,
                                      data.entityDomainName,          data.entityDomainValue,
                                      data.entityKindNameDescription, data.entityKindValue,
                                      currentCategory.description,    currentCategory.value,
                                      currentSubCategoryName,         currentSubCategoryValue,
                                      currentSpecificName,            currentSpecificValue,
                                      data.entityUid,
                                      sisoSpecificationTitleDate,    
                                      data.fullName,                                      // Full name
                                      data.countryName,                                   // @see Country#*
                                      data.entityKindName,                                // @see EntityKind#*
                                      data.entityDomainName,                              // @see %s (interface Domain)
                                      seeCurrentCategory,                                 // @see Category (if present)
                                      seeCurrentSubcategory,                              // @see SubCategory (if present)
                                      StringUtils.formatNamespaceStatement(currentNamespace, language),                // global namespace build for type seperation
                                      data.className, globalNamespace, data.className,                 // class definition
                                      data.countryName, 
                                      data.entityKindName, 
                                      data.entityDomainName, 
                                      data.entityDomainValue);
      }
      
      data.sb.append(licenseTemplate);
      data.sb.append(contents);

      if (data.entityDomainValue.isEmpty())
      {
        data.sb.append("// Domain value is empty\n");
        data.sb.append(data.toString());
        data.sb.append(currentEntity.toString());
      }
    }

    private void appendStatement(DescriptionElem elem, String typ, StringBuilder sb)
    {
      String template = "";

      if (language.toLowerCase().equals(CSHARP))
        template = "        "+typ+" = (byte)%s; // uid %s, %s\n";
      else if (language.toLowerCase().equals(CPP))
        template = "        set"+typ+"((std::uint8_t)%s); // uid %s, %s\n";
      else if (language.toLowerCase().equals(JAVA))
        template = "        set"+typ+"((byte)%s); // uid %s, %s\n";

      if (elem             == null)
          return;
      if (elem.value       == null)
          elem.value        = "";
      if (elem.uid         == null)
          elem.uid          = "";
      if (elem.description == null)
          elem.description  = "";
      sb.append(String.format(template, elem.value, elem.uid, elem.description));
    }
    
    private void setNamespace()
    {
      if (currentEntity != null)
      {
        int countryInteger  = Integer.parseInt(currentEntity.country);
        int entityKindInteger   = Integer.parseInt(currentEntity.kind);
        int entityDomainInteger = Integer.parseInt(currentEntity.domain);

        String countryDescription = CountryDescriptionMethod(countryInteger);
        String countryCode = buildCountryPackagePart(countryDescription);
        String kindName    = KindNameMethod(entityKindInteger);

        currentNamespace = globalNamespace + "::entities" + "::" + countryCode + "::" + kindName;

        String domainName = "";
        switch (kindName.toLowerCase())
        {
          case "munition":
            // domainName = getName(munitionDomainFromIntMethod, munitionDomainNameMethod, entityDomainInteger);
            domainName = MunitionDomainNameMethod(entityDomainInteger);
            break;
          case "supply":
            // domainName = getName(supplyDomainFromIntMethod, supplyDomainNameMethod, entityDomainInteger);
            domainName = SupplyDomainNameMethod(entityDomainInteger);
            break;
          default:
            // domainName = getName(methodPlatformDomainFromIntMethod, methodPlatformDomainNameMethod, entityDomainInteger);
            domainName = PlatformDomainNameMethod(entityDomainInteger);
            break;
        }

        currentNamespace += "::" + domainName;


      }
    }
    private void writeCategoryFile(TypeClassData d)
    {
      TypeClassData data = d;

      setNamespace();

      if (data == null) {
        data = buildEntityCommon(currentCategory.toString(), fixName(currentCategory),currentCategory.uid);
      }
// data.sb.append("// This is a Category file\n");
      data.sb.append("\n");
      appendStatement(currentCategory, "Category", data.sb);

      if (d == null) {
        saveEntityFile(data,currentCategory.uid);
      }
    }

    private void writeSubCategoryFile(TypeClassData d)
    {

      setNamespace();

      if ((currentCategory == null) || (currentSubCategory == null))
      {
          System.err.println (this.getClass().getName() + " writeSubCategoryFile has problem with currentCategory or currentSubCategory null, no file written");
          if (d != null)
              System.err.println ("DataPacket d=" + d.sb.toString());
      }

      TypeClassData data = d;
      if (data == null) {
        data = buildEntityCommon(currentSubCategory.toString(), fixName(currentSubCategory), currentSubCategory.uid);
      }
// data.sb.append("// This is a SubCategory file\n");
      data.sb.append("\n");
      appendStatement(currentCategory,    "Category",    data.sb);
      appendStatement(currentSubCategory, "SubCategory", data.sb);

      if (d == null)
        saveEntityFile(data,currentSubCategory.uid);
    }

    private void writeSpecificFile(TypeClassData d)
    {
      setNamespace();

      TypeClassData data = d;
      if (data == null) {
        data = buildEntityCommon(currentSpecific.toString(), fixName(currentSpecific),currentSpecific.uid);
      }
// data.sb.append("// This is a specific file\n");
      data.sb.append("\n");
      appendStatement(currentCategory,    "Category",    data.sb);
      appendStatement(currentSubCategory, "SubCategory", data.sb);
      appendStatement(currentSpecific,    "Specific",    data.sb);

      if (d == null)
        saveEntityFile(data,currentSpecific.uid);
    }

    private void writeExtraFile(TypeClassData d)
    {
      setNamespace();
      
      TypeClassData data = d;
      if (data == null) {
        data = buildEntityCommon(currentExtra.toString(), fixName(currentExtra),currentExtra.uid);
      }
// data.sb.append("// This is a Extra file\n");
      data.sb.append("\n");
      appendStatement(currentCategory,    "Category",    data.sb);
      appendStatement(currentSubCategory, "SubCategory", data.sb);
      appendStatement(currentSpecific,    "Specific",    data.sb);
      appendStatement(currentExtra,       "Extra",       data.sb);

      if (d == null)
        saveEntityFile(data,currentExtra.uid);
    }

    private TypeClassData buildEntityCommon(String fullName, String fixedName, String uid)
    {
        try {
        TypeClassData dataPacket = new TypeClassData();

        if  (fullName == null)
             dataPacket.fullName = "";
        else dataPacket.fullName = fullName;
        dataPacket.sb = new StringBuilder();
//        System.err.println("buildEntityCommon fixedName=" + fixedName + ", uid=" + uid + ", outputDirectory=" + outputDirectory); // debug trace
        
        buildPackagePath(currentEntity, dataPacket);

        dataPacket.directory = new File(outputDirectory, dataPacket.sb.toString());
        dataPacket.directory.mkdirs(); // ensure that directory exists

        // Protect against duplicate class names
        int i=1;
        while(new File(dataPacket.directory,fixedName+".java").exists()){
          fixedName = fixedName+ i++;
        }
//        System.err.println("fixedName.java=" + fixedName + ".java"); // debug trace

        String packagePath = packageName + "." + pathToPackage(dataPacket.sb.toString());
        int    countryInteger  = Integer.parseInt(currentEntity.country);
        String countryName     = getName(countryFromIntMethod, countryNameMethod, countryInteger);

        int entityDomainInteger = Integer.parseInt(currentEntity.domain);
        int entityKindInteger   = Integer.parseInt(currentEntity.kind);

        String entityKindName        = getName(kindFromIntMethod, kindNameMethod, entityKindInteger);
        String entityKindDescription = legalJavaDoc(getDescription(kindFromIntMethod, kindDescriptionMethod, entityKindInteger));
        
        String entityDomainName;
        String entityDomainDescription;
        String entityDomainValue;
        switch (entityKindName.toUpperCase()) {
          case "MUNITION":
            entityDomainName = "MunitionDomain";
            // System.out.println("Finding munition domain name from  " + entityDomainInteger);
            entityDomainDescription = "Munition Domain";
            entityDomainValue = getName(munitionDomainFromIntMethod, munitionDomainNameMethod, entityDomainInteger);
            break;
          case "SUPPLY":
            entityDomainName = "SupplyDomain";
            entityDomainDescription = "Supply Domain";
            // System.out.println("Finding supply domain name from " + entityDomainInteger);
            entityDomainValue = getName(supplyDomainFromIntMethod, supplyDomainNameMethod, entityDomainInteger);

            if (entityDomainValue.isEmpty())
            {
              System.out.println("Failed to find suuply domain value from integer : " + entityDomainInteger);
              System.out.println(currentEntity.toString());
            }
            break;
          case "OTHER":
          case "PLATFORM":
          case "LIFE_FORM":
          case "ENVIRONMENTAL":
          case "CULTURAL_FEATURE":
          case "RADIO":
          case "EXPENDABLE":
          case "SENSOR_EMITTER":
          default:
            entityDomainName = "PlatformDomain";
            entityDomainDescription = "Platform Domain";
            // System.out.println("Finding platform domain name from  " + entityDomainInteger);
            entityDomainValue = getName(methodPlatformDomainFromIntMethod, methodPlatformDomainNameMethod, entityDomainInteger);
            // System.out.println("Found domain name : " + entityDomainValue);
            break;
        }

        dataPacket.pkg = packagePath;
        dataPacket.entityUid = uid; //currentEntity.uid;
        dataPacket.countryName       = countryName;
        dataPacket.countryValue      = String.valueOf(countryInteger);
        dataPacket.entityKindName    = entityKindName;
        dataPacket.entityKindValue   = String.valueOf(entityKindInteger);
        dataPacket.entityKindNameDescription = entityKindDescription;
        dataPacket.entityDomainDescription   = entityDomainDescription; // unused, apparently superfluoua
        dataPacket.entityDomainName  = entityDomainName;
        dataPacket.entityDomainValue = String.valueOf(entityDomainInteger);
        //data.domainPrettyName = domainDescription;
        dataPacket.entityDomainValue = entityDomainValue;
        dataPacket.className = fixedName;

        dataPacket.sb.setLength(0);

        appendCommonStatements(dataPacket);
        return dataPacket;
      }
      catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  private void saveFile(File parentDir, String name, String contents)
  {
    // save file
    File target = new File(parentDir, name);
    try {
      target.createNewFile();
      try (FileOutputStream fso = new FileOutputStream(target)) {
        OutputStreamWriter fw = new OutputStreamWriter(fso,StandardCharsets.UTF_8);
        fw.write(contents);
        fw.flush();
      }
    }
    catch (IOException ex) {
      throw new RuntimeException("Error saving " + name + ": " + ex.getLocalizedMessage(), ex);
    }
  }

  private void setUniquePackageAndEmail(DescriptionElem descriptionElement, List<DescriptionElem> descriptionElementList)
  {
      if ((descriptionElement == null) || (descriptionElementList == null))
      {
          if (descriptionElement == null)
              System.err.println(this.getClass().getName() + ".setUniquePackageAndEmail() error: descriptionElement=null");
          if (descriptionElementList == null)
              System.err.println(this.getClass().getName() + ".setUniquePackageAndEmail() error: descriptionElementList=null");
          return;
      }
    String mangledDescription = fixName(descriptionElement);
    mangledDescription = makeUnique(mangledDescription, descriptionElementList);
    descriptionElement.packageFromDescription  = mangledDescription;
    descriptionElement.enumFromDescription = mangledDescription.toUpperCase();
  }

  private String makeUnique(String s, List<DescriptionElem> lis)
  {
    String news = s;
    for (int i = 1; i < 1000; i++) {
      outer:
      {
        for (DescriptionElem hd : lis) {
          if ((hd.packageFromDescription != null) && hd.packageFromDescription.equalsIgnoreCase(news))
            break outer;
        }
        return news;
      }
      news = s + i;
    }
    throw new RuntimeException("Problem generating unique name for " + s);
  }

  Pattern regex = Pattern.compile("\\((.*?)\\)");

  private String buildCountryPackagePart(String s)
  {
    if ((s == null) || s.isEmpty())
        return "";
    Matcher m = regex.matcher(s);
    String fnd = null;
    while (m.find()) {
      fnd = m.group(1);
      if (fnd.length() == 3)
        break;
      fnd = null;
    }
    if (fnd != null)
      return fnd.toLowerCase();

    s = fixName(s);
    s = s.toLowerCase();
    if (s.length() > 3)
      return s.substring(0, 3);
    else
      return s;
  }

/**
 * Naming conventions for cleaning up provided names
 * @param s enumeration string from XML data file
 * @return normalized name
 */
  private String buidKindOrDomainPackagePart(String s)
  {
    s = fixName(s);
    s = s.replaceAll("_", "");
    s = s.toLowerCase();
    return s;
  }
  
  //Country, kind, domain 
  private void buildPackagePath(EntityElem ent, TypeClassData data) throws Exception
  {
    // System.out.println(ent.toString());
    // System.out.println(data.toString());
    if (data == null)
        System.err.println("buildPackagePath data.sb 0: data = null");
    if (data.sb == null)
        System.err.println("buildPackagePath data.sb 0.5: data.sb = null");
//    if  (data.sb.toString().isEmpty())
//         System.err.println("buildPackagePath data.sb 1: empty string");
//    else System.err.println("buildPackagePath data.sb 1: " + data.sb.toString());
    
    String countryDescription = GenerateEntityTypes.this.getDescription(countryFromIntMethod, countryDescriptionMethod, Integer.parseInt(ent.country));
    
    if (countryDescription.isEmpty())
    {
      System.exit(0);
        System.err.println(this.getClass().getName() + ".buildPackagePath() failure, no country description");
        return;
    }
//    System.err.println("countryDescription=" + countryDescription);
    data.countryNamePretty = countryDescription;
    data.sb.append(buildCountryPackagePart(countryDescription));
    data.sb.append("/");
//    System.err.println("buildPackagePathdata.sb 2: " + data.sb.toString());

    String kindname = getName(kindFromIntMethod, kindNameMethod, Integer.parseInt(ent.kind));
    kindname = buidKindOrDomainPackagePart(kindname);
    data.sb.append(buidKindOrDomainPackagePart(kindname));
    data.sb.append("/");
//    System.err.println("buildPackagePathdata.sb 3: " + data.sb.toString());

    String domainname;
    String kindnamelc = kindname.toLowerCase();

    switch (kindnamelc) {
      case "munition":
        domainname = getName(munitionDomainFromIntMethod, munitionDomainNameMethod, Integer.parseInt(ent.domain));
        break;
      case "supply":
        domainname = getName(supplyDomainFromIntMethod, supplyDomainNameMethod, Integer.parseInt(ent.domain));
        break;
      default:
        domainname = getName(methodPlatformDomainFromIntMethod, methodPlatformDomainNameMethod, Integer.parseInt(ent.domain));
        break;
    }

    domainname = buidKindOrDomainPackagePart(domainname);
    data.sb.append(buidKindOrDomainPackagePart(domainname));
    data.sb.append("/");
//    System.err.println("buildPackagePathdata.sb 4: " + data.sb.toString());
  }

  private String pathToPackage(String s)
  {
    s = s.replaceAll("_",""); // no underscore divider
    s = s.replace("/", ".");
    if (s.endsWith("."))
        s = s.substring(0, s.length() - 1);
    return s;
  }
/*
  private String parentPackage(String s)
  {
    return s.substring(0, s.lastIndexOf('.'));
  }
*/
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

  private void printUnsupportedContainedELementMessage(String elname, String eldesc, CategoryElem cat)
  {
    StringBuilder bldr = new StringBuilder();
    bldr.append(cat.description);
    bldr.append("/");
    bldr.append(cat.parent.kind);
    bldr.append("/");
    bldr.append(cat.parent.domain);
    bldr.append("/");
    bldr.append(cat.parent.country);

    System.err.println("contained XML element " + elname + " (" + eldesc + " in " + bldr.toString() + ") not supported");
  }

  private void printUnsupportedContainedELementMessage(String elname, String eldesc, SubCategoryElem sub)
  {
    StringBuilder bldr = new StringBuilder();
    bldr.append(sub.description);
    bldr.append("/");
    bldr.append(sub.parent.description);
    bldr.append("/");
    bldr.append(sub.parent.parent.kind);
    bldr.append("/");
    bldr.append(sub.parent.parent.domain);
    bldr.append("/");
    bldr.append(sub.parent.parent.country);

    System.err.println("contained XML element " + elname + " (" + eldesc + " in " + bldr.toString() + ") not supported");
  }

  private String legalJavaDoc(String s)
  {
    s = s.replace("<", "&lt;");
    s = s.replace(">", "&gt;");
    s = s.replace("&", "&amp;");
    return s;
  }
  
  private String tryParent(DescriptionElem elem)
  {
    if(elem instanceof ExtraElem )
      return fixName(((ExtraElem)elem).parent.description);

   if(elem instanceof SpecificElem )
      return fixName(((SpecificElem)elem).parent.description);

   if(elem instanceof SubCategoryElem )
      return fixName(((SubCategoryElem)elem).parent.description);

   if(elem instanceof CategoryElem )
      return "uid"+((CategoryElem)elem).parent.uid;

   return null;
  }
  
  private String makeNonNumeric(DescriptionElem elem, String s)
  {
    if(s.startsWith("_"))
      s = s.substring(1);
    
    while(isNumeric(s)) {
      String p = tryParent(elem);
      if(p == null)
        return "_"+s;
      s = p+"_"+s;
    }
    return s;
  }
  
  private boolean isNumeric(String s)
  {
    try {
      int i = Integer.parseInt(s);
      return true;
    }
    catch(NumberFormatException t) {
      return false;
    }
  }
  
  private String fixName(DescriptionElem elem)
  {
    String r = new String();
    if ((elem != null) && (elem.description != null))
    {
        r = fixName(elem.description);
        if (r.isEmpty())
            return r;
        if(!r.isEmpty() && (isNumeric(r) | isNumeric(r.substring(1))))
        {
          r = makeNonNumeric(elem,r);    
        }
        r = r.substring(0,1) + r.substring(1).replaceAll("_",""); // no underscore divider after first character
    }
    return r;
  }
  
  private String fixName(String s)
  {
    String r = s.trim();
    
    if (r.isEmpty())
        return r;
  
    // Convert any of these chars to underbar (u2013 is a hyphen observed in source XML):
    r = r.trim().replaceAll(",", " ").replaceAll("—"," ").replaceAll("-", " ").replaceAll("\\."," ").replaceAll("&"," ")
                                     .replaceAll("/"," ").replaceAll("\"", " ").replaceAll("\'", " ").replaceAll("( )+"," ").replaceAll(" ", "_");
    r = r.substring(0,1) + r.substring(1).replaceAll("_",""); // no underscore divider after first character

    r = r.replaceAll("[\\h-/,\";:\\u2013]", "_");

    // Remove any of these chars (u2019 is an apostrophe observed in source XML):
    r = r.replaceAll("[\\[\\]()}{}'.#&\\u2019]", "");

    // Special case the plus character:
    r = r.replace("+", "PLUS");

    // Collapse all contiguous underbars:
    r = r.replaceAll("_{2,}", "_");

    r = r.replaceAll("<=", "LTE");
    r = r.replaceAll("<",  "LT");
    r = r.replaceAll(">=", "GTE");
    r = r.replaceAll(">",  "GT");
    r = r.replaceAll("=",  "EQ");
    r = r.replaceAll("%",  "pct");
    
    if (r.contains("\\"))
    {
        // editing XML file and reporting disparity seems best way to fix
        System.out.println("*** [GenerateEntityTypes] warning, encountered backslash: " + s);
        r = r.replaceAll("\\\\",  "_"); // \\\\ is regex escape
    } 
    
    // Java identifier can't start with digit
    if (Character.isDigit(r.charAt(0)))
        r = "_" + r;
    
    if (r.contains("__"))
    {
        System.err.println("fixname contains multiple underscores: " + r);
        r = r.replaceAll("__", "_");
    }
    // If there's nothing there, put in something:
    if (r.trim().isEmpty() || r.equals("_"))
    {
      System.err.print("fixname: erroneous name \"" + s + "\"");
      r = "undefinedName";
      if (!s.equals(r))
           System.err.print( " converted to \"" + r + "\"");
      System.err.println();
    }
    //System.out.println("In: "+s+" out: "+r);
    return r;
}

    /**
     * Normalize string characters to create valid description
     * @param value of interest
     * @return normalized value
     */
    private String normalizeDescription(String value)
    {
        return GenerateEnumerations.normalizeDescription(value);
    }
    /**
     * Normalize string characters to create valid Java name.  Note that unmangled name typically remains available in the description
     * @param value of interest
     * @return normalized value
     */
    private String normalizeToken(String value)
    {
        return GenerateEnumerations.normalizeToken(value);
    }

  /** GenerateEntityTypes invocation, passing run-time arguments (if any)
     * @param args three configuration arguments, if defaults not used
     */
  public static void main(String[] args)
  {

    sisoXmlFile         = args[0];
    outputDirectoryPath = args[1];
    packageName         = args[2];
    language            = args[3];

    System.out.println("GenerateJammers");
    System.out.println("   sisoXmlFile : "         + sisoXmlFile);
    System.out.println("   outputDirectoryPath : " + outputDirectoryPath);
    System.out.println("   packageName : "         + packageName);
    System.out.println("   language : "            + language);

    try 
    {
        System.out.println ("GenerateEntityTypes(" + sisoXmlFile + outputDirectoryPath + packageName + language  +").run()");
        new GenerateEntityTypes(sisoXmlFile, outputDirectoryPath, packageName, language).run();
    }
    catch (SAXException | IOException | ParserConfigurationException ex)
    {
        System.err.println(ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage());
    }
  }
}
