/**
 * Copyright (c) 2008-2022, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */

package edu.nps.moves.dis7.source.generator.entityTypes;

// import edu.nps.moves.dis7.source.generator.enumerations.GenerateEnumerations;
import java.io.*;
// import java.io.FileWriter;
// import java.io.FileOutputStream;
// import java.io.OutputStreamWriter;
// import java.io.IOException;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.Files;
// import java.nio.file.Paths;
// import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
// import javax.xml.parsers.SAXParserFactory;
// import org.xml.sax.Attributes;
 import org.xml.sax.SAXException;
// import org.xml.sax.helpers.DefaultHandler;

import java.io.File;

import java.util.List;

/**
 * GenerateOpenDis7JavaPackages creates source code from SISO enumeration definitions.
 * Created on Aug 6, 2019 by MOVES Institute, Naval Postgraduate School (NPS), Monterey California USA https://www.nps.edu
 *
 * @author Don McGregor, Mike Bailey and Don Brutzman
 * @version $Id$by 
 */
public class GenerateLanguageObjectTypes
{
    // set defaults to allow direct run
    private        File   outputDirectory;
    private static String outputDirectoryPath = "src-generated/java/edu/nps/moves/dis7/objectTypes"; // default
    private static String         packageName =                    "edu.nps.moves.dis7.objectTypes"; // default
    private static String            language = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_PROGRAMMING_LANGUAGE;
    private static String         sisoXmlFile = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_SISO_XML_FILE;
    private String sisoSpecificationTitleDate = "";

    /** String constant */ public static final String JAVA = "java";
    /** String constant */ public static final String CPP = "cpp";
    /** String constant */ public static final String OBJC = "objc";
    /** String constant */ public static final String CSHARP = "csharp";
    /** String constant */ public static final String JAVASCRIPT = "javascript";
    /** String constant */ public static final String PYTHON = "python";

    private static String[] languageChoices = {JAVA, CPP, CSHARP, PYTHON};

    String objecttypeTemplate;
    String    licenseTemplate;

    private String        packageInfoPath;
    private File          packageInfoFile;
    private StringBuilder packageInfoBuilder;

  /** Constructor for GenerateEntityTypes
     * @param xmlFile sisoXmlFile
     * @param outputDir outputDirectoryPath
     * @param packageName key to package name for object types */
  public GenerateLanguageObjectTypes(String xmlFile, String outputDir, String packageName, String languageToGenerate)
  {
    switch (languageToGenerate.toLowerCase())
    {
      case CPP:
          CppObjectTypesGenerator cppObjectTypesGenerator = new CppObjectTypesGenerator(xmlFile, outputDir, packageName);
          cppObjectTypesGenerator.writeObjectTypes();
          // cppEnumGenerator.writeEnums();
          break;

      case CSHARP:
          CSharpObjectTypesGenerator csharpObjectTypesGenerator = new CSharpObjectTypesGenerator(xmlFile, outputDir, packageName);
          csharpObjectTypesGenerator.writeObjectTypes();
          // cppEnumGenerator.writeEnums();
          break;

      case PYTHON:
          PythonObjectTypesGenerator pythonObjectTypesGenerator = new PythonObjectTypesGenerator(xmlFile, outputDir, packageName);
          pythonObjectTypesGenerator.writeObjectTypes();
          // cppEnumGenerator.writeEnums();
          break;

      default:
          System.out.println("GenerateLanguageEnumerations : ERROR Langauage " + 
                              languageToGenerate + " Is not currently supported");
          break;
    }
  }

  private void run() throws SAXException, IOException, ParserConfigurationException
  {
    // Run just allows the constructor to call the right generator type
    System.out.println (GenerateLanguageObjectTypes.class.getName() + " complete");
    // SAXParserFactory factory = SAXParserFactory.newInstance();
    // factory.setValidating(false);
    // factory.setNamespaceAware(true);
    // factory.setXIncludeAware(true);

    // loadTemplates();

    // System.out.println("Generating object types: ");
    // MyHandler handler = new MyHandler();
    // factory.newSAXParser().parse(new File(sisoXmlFile), handler);
    // System.out.println (GenerateObjectTypes.class.getName() + " complete."); // TODO  + handler.enums.size() + " enums created.");
  }

  
    public static void Usage(String... notice)
    {
        System.out.print("GenerateLanguageObjectTypes");
        if (notice.length > 0)
            System.out.println(": " + notice);
        else
            System.out.println("");
        System.out.println("Usage: GenerateLanguageObjectTypes");
        System.out.println("    xmlFile, outputDir, packagename, language"); 
        System.out.println("    Language types are java, cpp, csharp, python");

        //new Throwable().printStackTrace();
        Thread.currentThread().dumpStack();
    }
    
  /** GenerateObjectTypes invocation, passing run-time arguments (if any)
     * @param args three configuration arguments, if defaults not used
     */
  public static void main(String[] args)
  {
    if(args.length != 4)
    {
        Usage("Wrong number of arguments");
        System.exit(0);
    }

    sisoXmlFile         = args[0];
    outputDirectoryPath = args[1];
    packageName         = args[2];
    language            = args[3];

    System.out.println (GenerateLanguageObjectTypes.class.getName());
    System.out.println("   sisoXmlFile : "         + sisoXmlFile);
    System.out.println("   outputDirectoryPath : " + outputDirectoryPath);
    System.out.println("   packageName : "         + packageName);
    System.out.println("   language : "            + language);

    CheckArguments(sisoXmlFile, language);

    try {
        new GenerateLanguageObjectTypes(args[0], args[1], args[2], args[3]).run();
    }
    catch (SAXException | IOException | ParserConfigurationException ex) {
      System.err.println(ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage());
      ex.printStackTrace(System.err);
    }
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

  public static void CheckArguments(String xmlFile, String language)
  {
      if (!CheckLanguage(language))
      {
          System.out.println(language + " Is Not a valid language to generate");
          Usage();
          System.exit(-1);
      }

      try
      {
          FileInputStream ifs = new FileInputStream(xmlFile);
          ifs.close();
      }
      catch (FileNotFoundException fex)
      {
          Usage("Xml file : " + xmlFile + " not found");
          fex.printStackTrace();
          System.exit(-1);
      }
      catch(IOException e)
      {
          Usage("Problem with file arguments, dunno");
          e.printStackTrace();
          System.exit(-1);
      }
  }

}
