/**
 * Copyright (c) 2008-2025, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */

package edu.nps.moves.dis7.source.generator.entityTypes;

import edu.nps.moves.dis7.source.generator.enumerations.StringUtils;

import java.io.*;
import java.util.*;

import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.List;

/**
 * GenerateOpenDis7JavaPackages.java creates source code from SISO enumeration definitions.
 * Created on Aug 6, 2019 by
 * MOVES Institute, Naval Postgraduate School (NPS), Monterey California USA https://www.nps.edu
 *
 * @author Don McGregor, Mike Bailey and Don Brutzman
 * @version $Id$
 */
public class GenerateJammers
{
    // set defaults to allow direct run
    private        File   outputDirectory;
    private static String outputDirectoryPath = "src-generated/java/edu/nps/moves/dis7/jammers"; // default
    private static String         packageName =                    "edu.nps.moves.dis7.jammers";
    private static String            language = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_PROGRAMMING_LANGUAGE;
    private static String         sisoXmlFile = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_SISO_XML_FILE;
    private static String       sisoSpecificationTitleDate = "";

    // Prefix used for c++
    public static final String IVAR_PREFIX ="_";

    String globalNamespace = "";
    String enumNamespace = "";
    String currentNamespace = "";

    /** String constant */ public static final String JAVA = "java";
    /** String constant */ public static final String CPP = "cpp";
    /** String constant */ public static final String OBJC = "objc";
    /** String constant */ public static final String CSHARP = "csharp";
    /** String constant */ public static final String JAVASCRIPT = "javascript";
    /** String constant */ public static final String PYTHON = "python";

    String jammertechniqueTemplate;
    String         licenseTemplate;

    class TypeClassData
    {
      String pkg;
      File directory;
      // TODO kind, category
      StringBuilder sb;
      String className                         = new String();
      private String countryName               = new String();
      private String countryNamePretty         = new String();
      private String countryValue              = new String();
      private String entityKindName            = new String();
      private String entityKindNameDescription = new String();
    }
    
    private String        packageInfoPath;
    private File          packageInfoFile;
    private StringBuilder packageInfoBuilder;

  /** Constructor for GenerateJammers
     * @param xmlFile sisoXmlFile
     * @param outputDir outputDirectoryPath
     * @param packageName key to package name for jammer */
  public GenerateJammers(String xmlFile, String outputDir, String packageName, String languageToGenerate)
  {
        if (!xmlFile.isEmpty())
             sisoXmlFile = xmlFile;
        if (!outputDir.isEmpty())
            outputDirectoryPath = outputDir;
        if (!packageName.isEmpty())
           GenerateJammers.packageName = packageName;
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

        System.out.println (GenerateJammers.class.getName());
        System.out.println ("              xmlFile=" + sisoXmlFile);
        System.out.println ("          packageName=" + GenerateJammers.packageName);
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
              packageInfoBuilder.append(" * Jammers type infrastructure classes for ").append(sisoSpecificationTitleDate).append(" enumerations.\n");
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
              packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/22095487/why-is-package-info-java-useful\">https://stackoverflow.com/questions/22095487/why-is-package-info-java-useful</a>\n");
              packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/624422/how-do-i-document-packages-in-java\">https://stackoverflow.com/questions/624422/how-do-i-document-packages-in-java</a>\n");
              packageInfoBuilder.append(" *").append("/\n");
              packageInfoBuilder.append("\n");
              packageInfoBuilder.append("package edu.nps.moves.dis7.jammers;\n");

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

  private void run() throws SAXException, IOException, ParserConfigurationException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(true);

    loadTemplates();

    //System.out.println("Generating jammers: ");
    MyHandler handler = new MyHandler();
    factory.newSAXParser().parse(new File(sisoXmlFile), handler);
    System.out.println (GenerateJammers.class.getName() + " complete."); // TODO  + handler.enums.size() + " enums created.");
  }

  private void loadTemplates()
  {
    String languageFolder = this.language;

    try {

      licenseTemplate          = loadOneTemplate("../pdus/dis7javalicense.txt");
      if (language.toLowerCase().equals(JAVA))
      {  
        jammertechniqueTemplate  = loadOneTemplate("../entitytypes/jammertechnique.txt");
      }
      else
      {
        jammertechniqueTemplate  = loadOneTemplate("../entitytypes/" + languageFolder + "/jammertechnique.txt");
      }
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private String loadOneTemplate(String s) throws Exception
  {
    // String templateContents = new String(Files.readAllBytes(Paths.get(getClass().getResource(s).toURI())));
    // System.out.println("Loading template : " + s);
    // System.out.println("    class : " + getClass());
    // System.out.println(" resource : " + getClass().getResource(s));
    // System.out.println("      uri : " + getClass().getResource(s).toURI());

    return new String(Files.readAllBytes(Paths.get(getClass().getResource(s).toURI())));
  }

  class DescriptionElem
  {
    String description;
    String value;

    String packageFromDescription;
    String enumFromDescription;
    List<DescriptionElem> children = new ArrayList<>();
  }

  class JammerKindElem extends DescriptionElem
  {
    List<DescriptionElem> categories = new ArrayList<>();
  }

  class JammerCategoryElem extends DescriptionElem
  {
    JammerKindElem parent;
  }

  class JammerSubCategoryElem extends DescriptionElem
  {
    JammerCategoryElem parent;
  }

  class JammerSpecificElem extends DescriptionElem
  {
    JammerSubCategoryElem parent;
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
    JammerKindElem        currentKind;
    JammerCategoryElem    currentCategory;
    JammerSubCategoryElem currentSubCategory;
    JammerSpecificElem    currentSpecific;
    int filesWrittenCount = 0;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    {
      if (qName.equalsIgnoreCase("revision")) {
        if (sisoSpecificationTitleDate.length() <= 0) // only want the first/latest
            sisoSpecificationTitleDate = legalJavaDoc(attributes.getValue("title") + " (" + attributes.getValue("date") + ")");
        return;
      }

      if (attributes.getValue("deprecated") != null)
        return;

      switch (qName) {

        case "jammer_kind":
          currentKind = new JammerKindElem();
          currentKind.value = attributes.getValue("value");
          currentKind.description = attributes.getValue("description");
          if (currentKind.description != null)
              currentKind.description = currentKind.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          break;

        case "jammer_category":
          if (currentKind == null)
            break;

          currentCategory = new JammerCategoryElem();
          currentCategory.value = attributes.getValue("value");
          currentCategory.description = attributes.getValue("description");
          if (currentCategory.description != null)
              currentCategory.description = currentCategory.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          currentCategory.parent = currentKind;
          setUniquePkgAndEmnum(currentCategory, currentKind.categories);
          currentKind.categories.add(currentCategory);
          break;

        case "jammer_subcategory":
          if (currentCategory == null)
            break;
          currentSubCategory = new JammerSubCategoryElem();
          currentSubCategory.value = attributes.getValue("value");
          currentSubCategory.description = attributes.getValue("description");
          if (currentSubCategory.description != null)
              currentSubCategory.description = currentSubCategory.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          currentSubCategory.parent = currentCategory;
          setUniquePkgAndEmnum(currentSubCategory, currentCategory.children);
          currentCategory.children.add(currentSubCategory);
          break;

        case "jammer_specific":
          if (currentSubCategory == null)
            break;
          currentSpecific = new JammerSpecificElem();
          currentSpecific.value = attributes.getValue("value");
          currentSpecific.description = attributes.getValue("description");
          if (currentSpecific.description != null)
              currentSpecific.description = currentSpecific.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          currentSpecific.parent = currentSubCategory;
          setUniquePkgAndEmnum(currentSpecific, currentSubCategory.children);
          currentSubCategory.children.add(currentSpecific);
          break;

        default:
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
      try {
        switch (qName) {

          case "jammer_kind":
            if (currentKind != null)
              writeKindFile(null);
            currentKind = null;
            break;

          case "jammer_category":
            if (currentCategory != null) //might have been deprecated
              writeCategoryFile(null);
            currentCategory = null;
            break;

          case "jammer_subcategory":
            if (currentSubCategory != null) // might have been deprecated
              writeSubCategoryFile(null);
            currentSubCategory = null;
            break;

          case "jammer_specific":
            if (currentSpecific != null) // might have been deprecated)
              writeSpecificFile(null);
            currentSpecific = null;
            break;

          default:
        }
      }
      catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public void endDocument() throws SAXException
    {
    }

    private void saveJammerFile(TypeClassData data)
    {

        if (language.toLowerCase().equals(CSHARP))
        {
          
          saveFile(data.directory, data.className + ".cs", data.sb.toString());
        }
        else if (language.toLowerCase().equals(CPP))
        {
          saveFile(data.directory, data.className + ".h", data.sb.toString());
        }
        else if (language.toLowerCase().equals(PYTHON))
        {
          saveFile(data.directory, data.className + ".py", data.sb.toString());
        }
        else if (language.toLowerCase().equals(JAVA))
        {
          // data.sb.append("    }\n}\n");
          saveFile(data.directory, data.className + ".java", data.sb.toString());  
      
          packageInfoPath = data.directory + "/" + "package-info.java";
          packageInfoFile = new File(packageInfoPath);

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
                  if (!data.countryNamePretty.isEmpty())
                      packageInfoBuilder.append(" ").append(data.countryNamePretty);
                  if (!data.entityKindName.isEmpty())
                      packageInfoBuilder.append(" ").append(data.entityKindName);
                  if (!data.countryValue.isEmpty() && !data.entityKindName.isEmpty())
                      packageInfoBuilder.append(" j");
                  else
                      packageInfoBuilder.append(" J");
                  packageInfoBuilder.append("ammers type infrastructure classes for ").append(sisoSpecificationTitleDate).append(" enumerations.\n");
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
                  packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/22095487/why-is-package-info-java-useful\">https://stackoverflow.com/questions/22095487/why-is-package-info-java-useful</a>\n");
                  packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/624422/how-do-i-document-packages-in-java\">https://stackoverflow.com/questions/624422/how-do-i-document-packages-in-java</a>\n");
                  packageInfoBuilder.append(" */\n");
                  packageInfoBuilder.append("// created by edu/nps/moves/dis7/source/generator/entityTypes/GenerateJammers.java\n");
                  packageInfoBuilder.append("\n");
                  packageInfoBuilder.append("package ").append(data.pkg).append(";\n");

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
    }

    private void appendCommonStatements(TypeClassData data)
    {
      String contents = "";

      if (language.toLowerCase().equals(JAVA))
      {
         contents = String.format(jammertechniqueTemplate, data.pkg,
            sisoSpecificationTitleDate, 
            "284", // TODO huh?
            // TODO kind, category
            data.className,data.className);
      }
      else if (language.toLowerCase().equals(CSHARP) ||
               language.toLowerCase().equals(CPP))
      {
        contents = String.format(jammertechniqueTemplate, data.pkg,
            sisoSpecificationTitleDate, 
            "284", // TODO huh?
            // TODO kind, category
            StringUtils.formatNamespaceStatement(currentNamespace, language),
            data.className,data.className);
      }

      else if (language.toLowerCase().equals(PYTHON))
      {
        contents = String.format(jammertechniqueTemplate, data.pkg,
            sisoSpecificationTitleDate, 
            "284", // TODO huh?
            // TODO kind, category
            data.className);

      }

      if (language.toLowerCase().equals(PYTHON)) data.sb.append("\"\"\"\n");
      data.sb.append(licenseTemplate);
      if (language.toLowerCase().equals(PYTHON)) data.sb.append("\"\"\"\n");

      data.sb.append(contents);
    }

    private void appendStatement(DescriptionElem elem, String typ, StringBuilder sb)
    {
      String template = "";

      switch (language.toLowerCase())
      {
        case JAVA:
          template = "        set" + typ + "((byte)%s); // %s\n";        
          break;

        case CSHARP:
          template = "            " + StringUtils.firstCharUpper(typ) + " = %s; // %s\n";
          break;

        case CPP:
          template = "            " + IVAR_PREFIX + typ + " = %s; // %s\n";

        case PYTHON:
          template = StringUtils.tabs(2) + "self." + StringUtils.firstCharLower(typ) + " = %s // %s\n";
          break;
      }

      if (!template.isEmpty())
      {
        sb.append(String.format(template, elem.value, elem.description));
      }
    }

    private void writeKindFile(TypeClassData d)
    {

      int indentLevel = 2;
      if (language.toLowerCase().equals(JAVA)) indentLevel = 1;

      TypeClassData data = d;
      if (data == null) {
        data = buildJammerCommon(fixName(currentKind), currentKind);
      }

      currentNamespace = globalNamespace + "::jammers";
      
      appendCommonStatements(data);

      appendStatement(currentKind, "Kind", data.sb);

      if (!language.toLowerCase().equals(PYTHON))
      {
        data.sb.append(StringUtils.tabs(indentLevel) + "}\n");  // end the ctor decl
        data.sb.append(StringUtils.tabs(indentLevel -1) + "}\n\n");    // end the class decl
      }

      if (!language.toLowerCase().equals(JAVA))
      {
        data.sb.append(StringUtils.formatNamespaceEndStatement(currentNamespace, language));
      }

      // data.sb.append(StringUtils.formatNamespaceEndStatement(globalNamespace, language));
      saveJammerFile(data);

    }

    private void writeCategoryFile(TypeClassData d)
    {
      int indentLevel = 2;
      if (language.toLowerCase().equals(JAVA)) indentLevel = 1;

      TypeClassData data = d;
      if (data == null) {
        data = buildJammerCommon(fixName(currentCategory), currentCategory);
      }

      currentNamespace = globalNamespace + "::jammers::" +
                         StringUtils.firstCharLower(fixName(currentKind));


      appendCommonStatements(data);
      appendStatement(currentKind, "Kind", data.sb);
      appendStatement(currentCategory, "Category", data.sb);

      if(!language.toLowerCase().equals(PYTHON))
      {
        data.sb.append(StringUtils.tabs(indentLevel) + "}\n");
        data.sb.append(StringUtils.tabs(indentLevel -1) + "}\n\n");
      }
      if (!language.toLowerCase().equals(JAVA))
      {
        data.sb.append(StringUtils.formatNamespaceEndStatement(currentNamespace, language));
      }

      if (d == null) {
        saveJammerFile(data);
      }
    }

    private void writeSubCategoryFile(TypeClassData d)
    {
      int indentLevel = 2;
      if (language.toLowerCase().equals(JAVA)) indentLevel = 1;

      TypeClassData data = d;
      if (data == null) {
        data = buildJammerCommon(fixName(currentSubCategory), currentSubCategory);
      }
      
      appendCommonStatements(data);
      appendStatement(currentKind, "Kind", data.sb);
      appendStatement(currentCategory, "Category", data.sb);
      appendStatement(currentSubCategory, "SubCategory", data.sb);

      if (!language.toLowerCase().equals(PYTHON))
      {
        data.sb.append(StringUtils.tabs(indentLevel) + "}\n");
        data.sb.append(StringUtils.tabs(indentLevel -1) + "}\n\n");
      }

      if (!language.toLowerCase().equals(JAVA))
      {
        data.sb.append(StringUtils.formatNamespaceEndStatement(currentNamespace, language));
      }

      if (d == null)
        saveJammerFile(data);
    }

    private void writeSpecificFile(TypeClassData d) throws Exception
    {
      int indentLevel = 2;
      if (language.toLowerCase().equals(JAVA)) indentLevel = 1;

      TypeClassData data = d;
      if (data == null) {
        data = buildJammerCommon(fixName(currentSpecific), currentSpecific);
      }

      appendCommonStatements(data);
      appendStatement(currentKind, "Kind", data.sb);
      appendStatement(currentCategory, "Category", data.sb);
      appendStatement(currentSubCategory, "SubCategory", data.sb);
      appendStatement(currentSpecific, "Specific", data.sb);

      if (!language.toLowerCase().equals(PYTHON))
      {
        data.sb.append(StringUtils.tabs(indentLevel) + "}\n");
        data.sb.append(StringUtils.tabs(indentLevel -1) + "}\n\n");
      }
      
      if (!language.toLowerCase().equals(JAVA))
      {
        data.sb.append(StringUtils.formatNamespaceEndStatement(currentNamespace, language));
      }

      if (d == null)
        saveJammerFile(data);
    }

    private TypeClassData buildJammerCommon(String fixedName, DescriptionElem elem)
    {
      try {
        TypeClassData data = new TypeClassData();
        data.sb = new StringBuilder();
        buildPackagePathAbstract(elem, data.sb);

        data.directory = new File(outputDirectory, data.sb.toString());
        data.directory.mkdirs();

        // Protect against duplicate class names
        // only spans current kind, not other folders
        if (language.toLowerCase().equals(JAVA))
        {
          int i = 1;
          while (new File(data.directory, fixedName + ".java").exists()) {
            fixedName = fixedName + i++;
          }
        }
        else if (language.toLowerCase().equals(CSHARP))
        {
          int i = 1;
          while (new File(data.directory, fixedName + ".cs").exists()) {
            fixedName = fixedName + i++;
          }
        }
        else if (language.toLowerCase().equals(CPP))
        {
          int i = 1;
          while (new File(data.directory, fixedName + ".h").exists()) {
            fixedName = fixedName + i++;
          }
        }
        else if (language.toLowerCase().equals(PYTHON))
        {
          int i = 1;
          while (new File(data.directory, fixedName + ".py").exists()) {
            fixedName = fixedName + i++;
          }
        }

        String pkg = packageName + "." + pathToPackage(data.sb.toString());
        data.pkg = pkg;
        data.className = fixedName;
        data.sb.setLength(0);
        
        // appendCommonStatements(data);
        return data;
      }
      catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  private void saveFile(File parentDir, String name, String contents)
  {
    // System.out.println("Generating file " + name);
    // save file
    File target = new File(parentDir, name);
    try {
      target.createNewFile();
      try (FileOutputStream fso = new FileOutputStream(target)) {
        OutputStreamWriter fw = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
        fw.write(contents);
        fw.flush();
      }
    }
    catch (IOException ex) {
      throw new RuntimeException("Error saving " + name + ": " + ex.getLocalizedMessage(), ex);
    }
  }

  private void setUniquePkgAndEmnum(DescriptionElem elem, List<DescriptionElem> lis)
  {
    String mangledDescription = fixName(elem);
    mangledDescription = makeUnique(mangledDescription, lis);
    elem.packageFromDescription = mangledDescription;
    elem.enumFromDescription = mangledDescription.toUpperCase();
  }

  private String makeUnique(String s, List<DescriptionElem> lis)
  {
    String news = s;
    for (int i = 1; i < 1000; i++) {
      outer:
      {
        for (DescriptionElem hd : lis) {
          if (hd.packageFromDescription.equalsIgnoreCase(news))
            break outer;
        }
        return news;
      }
      news = s + i;
    }
    throw new RuntimeException("Problem generating unique name for " + s);
  }

  private void buildPackagePathAbstract(DescriptionElem elem, StringBuilder sb) throws Exception
  {
    if (elem instanceof JammerKindElem)
      buildPackagePath((JammerKindElem) elem, sb);
    else if (elem instanceof JammerCategoryElem)
      buildPackagePath((JammerCategoryElem) elem, sb);
    else if (elem instanceof JammerSubCategoryElem)
      buildPackagePath((JammerSubCategoryElem) elem, sb);
    else if (elem instanceof JammerSpecificElem)
      buildPackagePath((JammerSpecificElem) elem, sb);
  }

  private void buildPackagePath(JammerKindElem kind, StringBuilder sb) throws Exception
  {
    sb.append(fixName(kind.description));
    sb.append("/");
  }

  private void buildPackagePath(JammerCategoryElem cat, StringBuilder sb) throws Exception
  {
    buildPackagePath(cat.parent, sb);
    sb.append(fixName(cat.description));
    sb.append("/");
  }

  private void buildPackagePath(JammerSubCategoryElem sub, StringBuilder sb) throws Exception
  {
    buildPackagePath(sub.parent, sb);
    sb.append(fixName(sub.description));
    sb.append("/");
  }

  private void buildPackagePath(JammerSpecificElem spec, StringBuilder sb) throws Exception
  {
    buildPackagePath(spec.parent, sb);
    sb.append(fixName(spec.description));
    sb.append("/");
    //return sb.toString();
  }

  private String pathToPackage(String s)
  {
    s = s.replace("_", "");
    s = s.replace("/", ".");
    if (s.endsWith("."))
      s = s.substring(0, s.length() - 1);
    return s;
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

  private void printUnsupportedMessage(String elname, String eldesc, JammerCategoryElem cat)
  {
    StringBuilder bldr = new StringBuilder();
    bldr.append(cat.description);

    System.out.println("XML element " + elname + " {" + eldesc + "in " + bldr.toString() + " not supported");
  }

  private void printUnsupportedMessage(String elname, String eldesc, JammerSubCategoryElem sub)
  {
    StringBuilder bldr = new StringBuilder();
    bldr.append(sub.description);
    bldr.append("/");
    bldr.append(sub.parent.description);

    System.out.println("XML element " + elname + " {" + eldesc + "in " + bldr.toString() + " not supported");
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
    if (elem instanceof JammerSpecificElem)
      return fixName(((JammerSpecificElem) elem).parent.description);

    if (elem instanceof JammerSubCategoryElem)
      return fixName(((JammerSubCategoryElem) elem).parent.description);

    if (elem instanceof JammerCategoryElem)
      return fixName(((JammerCategoryElem) elem).parent.description);

    return null;
  }

  private String makeNonNumeric(DescriptionElem elem, String s)
  {
    if (s.startsWith("_"))
      s = s.substring(1);

    while (isNumeric(s)) {
      String p = tryParent(elem);
      if (p == null)
        return "_" + s;
      s = p + "_" + s;
    }
    return s;
  }

  private boolean isNumeric(String s)
  {
    try {
      int i = Integer.parseInt(s);
      return true;
    }
    catch (NumberFormatException t) {
      return false;
    }
  }

  private String fixName(DescriptionElem elem)
  {
    String r = new String();
    if ((elem != null) && (elem.description != null))
    {
        r = fixName(elem.description);
        if(!r.isEmpty() && (isNumeric(r) | isNumeric(r.substring(1))))
        {
          r = makeNonNumeric(elem,r);    
        }
        r = r.substring(0,1) + r.substring(1).replaceAll("_",""); // no underscore divider after first character
    }
    return r;
  }

/**
 * Naming conventions for cleaning up provided names
 * @param s enumeration string from XML data file
 * @return normalized name
 */
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

    r = r.replace("<=", "LTE");
    r = r.replace("<", "LT");
    r = r.replace(">=", "GTE");
    r = r.replace(">", "GT");
    r = r.replace("=", "EQ");
    r = r.replace("%", "pct");

    r = r.replaceAll("—","_").replaceAll("–","_").replaceAll("\"", "").replaceAll("\'", "");

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

  /** Command-line invocation (CLI)
         <arg value="xml/SISO/${SISO-REF-010.xml}"/>
         <arg value="src-generated/java/edu/nps/moves/dis7/jammers"/>
         <arg value="edu.nps.moves.dis7.jammers"/>
         <arg value="csharp"/>
    * @param args command-line arguments */

  public static void main(String[] args)
  {
    sisoXmlFile         = args[0];
    outputDirectoryPath = args[1];
    packageName         = args[2];
    if (args.length > 3)
      language          = args[3];

    System.out.println("GenerateJammers");
    System.out.println("   sisoXmlFile : "         + sisoXmlFile);
    System.out.println("   outputDirectoryPath : " + outputDirectoryPath);
    System.out.println("   packageName : "         + packageName);
    System.out.println("   language : "            + language);

    try {
        new GenerateJammers(sisoXmlFile, outputDirectoryPath, packageName, language).run();
    }
    catch (SAXException | IOException | ParserConfigurationException ex) {
      System.err.println(ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage());
      ex.printStackTrace(System.err);
    }
  }
}
