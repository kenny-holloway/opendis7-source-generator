/**
 * Copyright (c) 2008-2022, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */
package edu.nps.moves.dis7.source.generator.enumerations;

import java.io.*;

import java.io.File;
// import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
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

/**
 * GenerateEnumerationsForGivenLanguage creates source code from SISO enumeration definitions.
 * Created on Apr 16, 2019 by MOVES Institute, Naval Postgraduate School (NPS), Monterey California USA https://www.nps.edu
 *
 */

public class GenerateLanguageEnumerations
{
    // set defaults to allow direct run
    private File   outputDirectory;
    private static String outputDirectoryPath = "src-generated/java/edu/nps/moves/dis7/enumerations"; // default
    private static String packageName =                    "edu.nps.moves.dis7.enumerations"; // default
    private static String language = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_PROGRAMMING_LANGUAGE;
    private static String sisoXmlFile = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_SISO_XML_FILE;
    
    private Properties uid2ClassName;
    private Properties uid4aliases;
    private Properties interfaceInjection;
    private Map<String,String> uidClassNames;
    private Set<String> uidDoNotGenerate;
    private Map<String,String> uid2ExtraInterface;

    private static String       sisoSpecificationTitleDate = "";

    /** String constant */ public static final String JAVA = "java";
    /** String constant */ public static final String CPP = "cpp";
    /** String constant */ public static final String OBJC = "objc";
    /** String constant */ public static final String CSHARP = "csharp";
    /** String constant */ public static final String JAVASCRIPT = "javascript";
    /** String constant */ public static final String PYTHON = "python";

    private static String[] languageChoices = {JAVA, CPP, CSHARP, PYTHON};

    // https://stackoverflow.com/questions/11883043/does-an-enum-class-containing-20001-enum-constants-hit-any-limit
    final int MAX_ENUMERATIONS = 2000;

    private int additionalEnumClassesCreated = 0;
    
    private String        packageInfoPath;
    private File          packageInfoFile;
    private StringBuilder packageInfoBuilder;

    public GenerateLanguageEnumerations(String xmlFile, String outputDir, 
                                        String packageName, String languageToGenerate)
    {
        switch (languageToGenerate.toLowerCase())
        {
            case JAVA:
                // Can we make this call the other generator?  Skip it.
                break;

            case CPP:
                CppEnumGenerator cppEnumGenerator = new CppEnumGenerator(xmlFile, outputDir, packageName);
                cppEnumGenerator.writeEnums();
                break;

            case CSHARP:
                CSharpEnumGenerator csharpEnumGenerator = new CSharpEnumGenerator(xmlFile, outputDir, packageName);
                csharpEnumGenerator.writeEnums();
                break;

            case PYTHON:
                PythonEnumGenerator pythonEnumGenerator = new PythonEnumGenerator(xmlFile, outputDir, packageName);
                pythonEnumGenerator.writeEnums();
                break;

            default:
                System.out.println("GenerateLanguageEnumerations : ERROR Langauage " + 
                                   languageToGenerate + " Is not currently supported");
                break;
        }
    }

    public static void main(String[] args)
    {
        String indent="    ";
        if(args.length != 4)
        {
            Usage("Wrong number of arguments");
            System.exit(0);
        }

        sisoXmlFile         = args[0];
        outputDirectoryPath = args[1];
        packageName         = args[2];
        language            = args[3];

        System.out.println("GenerateLanguageEnumerations");
        System.out.println("   sisoXmlFile : "         + sisoXmlFile);
        System.out.println("   outputDirectoryPath : " + outputDirectoryPath);
        System.out.println("   packageName : "         + packageName);
        System.out.println("   language : "            + language);

        CheckArguments(sisoXmlFile, language);

        try {
            new GenerateLanguageEnumerations(args[0], args[1], args[2], args[3]).run();
        }
        catch (SAXException | IOException | ParserConfigurationException ex) {
            ex.printStackTrace(System.err);
        }
    }

    public void run() throws SAXException, IOException, ParserConfigurationException
    {
        // This does all the work
        //  System.out.println (GenerateLanguageEnumerations.class.getName() + " complete, " + (handler.enums.size() + additionalEnumClassesCreated) + " enum classes created.");
        System.out.println (GenerateLanguageEnumerations.class.getName() + " complete");
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

    public static void Usage(String... notice)
    {
        System.out.print("GenerateLanguageEnumerations");
        if (notice.length > 0)
            System.out.println(": " + notice);
        else
            System.out.println("");
        System.out.println("Usage: GenerateLanguageEnumerations");
        System.out.println("    xmlFile, outputDir, packagename, language"); 
        System.out.println("    Language types are java, cpp, csharp, python");

        //new Throwable().printStackTrace();
        Thread.currentThread().dumpStack();
    }

    //public GenerateLanguageEnumerations(String xmlFile, String outputDir, String packageName, String languageToGenerate)
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
