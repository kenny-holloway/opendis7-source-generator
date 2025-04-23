package edu.nps.moves.dis7.source.generator.enumerations;

import java.io.*;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class PythonEnumGenerator extends AbstractEnumGenerator
{

    protected Properties pythonEnumAliases;
    // Not using defaults, parameters are checked on the way in
    public PythonEnumGenerator(String xmlFile, String outputDir, String packagename)
    {
        this();
        
        System.out.println (PythonEnumGenerator.class.getName());
        sisoXmlFile = xmlFile;
        outputDirectoryPath = outputDir;
        PythonEnumGenerator.packageName = packageName;

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

   public PythonEnumGenerator() {
      super(PYTHON);
   }

   public void writeEnums() {
      System.out.println("Creating Python Enumerations");

      System.out.println ("              xmlFile = " + sisoXmlFile);
      System.out.println ("          packageName = " + PythonEnumGenerator.packageName);
      System.out.println ("  outputDirectoryPath = " + outputDirectoryPath);
      System.out.println ("actual directory path = " + outputDirectory.getAbsolutePath());
      System.out.println ("            Namespace = " +  globalNamespace);
      System.out.println ("       Enum Namespace = " + enumNamespace);

      try{
         writeLanguageEnums();
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            ex.printStackTrace(System.err);
        }

      System.out.println("Finished creating Python Enumerations");
   }

   private void writeLanguageEnums() throws SAXException, IOException, ParserConfigurationException
   {
      loadEnumTemplates();
      
      loadNamespaceDefinitions();

      pythonEnumAliases = new Properties();
      pythonEnumAliases.load(getClass().getResourceAsStream("pythonEnumAlias.properties"));

      File xmlFile = new File(sisoXmlFile);
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      factory.setXIncludeAware(true);
      System.out.println("\nBegin uid preprocess...");
      factory.newSAXParser().parse(xmlFile,new UidCollector());

      System.out.println("Found enumeration types " + uidClassNames.size());

      System.out.println("Begin enumeration generation...");
      PythonEnumHandler handler = new PythonEnumHandler(this);
      factory.newSAXParser().parse(xmlFile, handler); // apparently can't reuse xmlFile

      System.out.println (PythonEnumGenerator.class.getName() + " complete, " + (handler.enums.size()) + " enum classes created.");
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
                        String name = StringUtils.fixName(attributes.getValue("name")); // name canonicalization C14N
                        String name2 = PythonEnumGenerator.this.uid2ClassName.getProperty(uid);
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

} // end class PythonEnumGenerator