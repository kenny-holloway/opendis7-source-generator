package edu.nps.moves.dis7.source.generator.entityTypes;

// import edu.nps.moves.dis7.source.generator.enumerations.GenerateEnumerations;
import edu.nps.moves.dis7.source.generator.enumerations.StringUtils;

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

public class CppObjectTypesGenerator extends AbstractObjectTypesGenerator
{

    class TypeClassData
    {
        String pkg;
        File directory;
        StringBuilder sb;
        String className;
    }

    String objecttypeTemplate;
    String objecttypestatementTemplate;
    String    licenseTemplate;
    private String sisoSpecificationTitleDate = "";
    private String        packageInfoPath;
    private File          packageInfoFile;
    private StringBuilder packageInfoBuilder;
    String globalNamespace = "";
    String currentNamespace = "";


    public CppObjectTypesGenerator(String xmlFile, String outputDir, String packagename)
    {
        // Thread.currentThread().dumpStack();

        System.out.println (CppObjectTypesGenerator.class.getName());
        sisoXmlFile = xmlFile;
        outputDirectoryPath = outputDir;
        CppObjectTypesGenerator.packageName = packageName;

         outputDirectory  = new File(outputDirectoryPath);
         outputDirectory.mkdirs();
    }

    public void writeObjectTypes()
    {
        System.out.println (CppObjectTypesGenerator.class.getName());
        System.out.println("Creating C++ Object Types");

        Properties systemProperties = System.getProperties();
        globalNamespace = systemProperties.getProperty("xmlpg.namespace");
        if (globalNamespace == null)
            globalNamespace = "";

        currentNamespace = globalNamespace;

        System.out.println ("              xmlFile = " + sisoXmlFile);
        System.out.println ("          packageName = " + CppObjectTypesGenerator.packageName);
        System.out.println ("  outputDirectoryPath = " + outputDirectoryPath);
        System.out.println ("actual directory path = " + outputDirectory.getAbsolutePath());
        System.out.println ("            namespace = " + globalNamespace);

        try{
            writeLanguageObjectTypes();
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            ex.printStackTrace(System.err);
        }

        System.out.println("Finished creating C++ Object Types");
    }

    public void writeLanguageObjectTypes() throws SAXException, IOException, ParserConfigurationException
    {
        // This is all the work
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(true);

        loadTemplates();

        System.out.println("Generating C++ object types: ");
        MyHandler handler = new MyHandler();
        factory.newSAXParser().parse(new File(sisoXmlFile), handler);
        System.out.println (CppObjectTypesGenerator.class.getName() + " complete."); // TODO  + handler.objects.size() + " enums created.");
    }
    private void loadTemplates()
    {
        try {
        licenseTemplate          = loadOneTemplate("../pdus/dis7javalicense.txt");
        objecttypeTemplate       = loadOneTemplate("../entitytypes/cpp/objecttype.txt");
        objecttypestatementTemplate = loadOneTemplate("../entitytypes/cpp/objecttypestatement.txt");
        }
        catch (Exception ex) {
        throw new RuntimeException(ex);
        }
    }

  private String loadOneTemplate(String s) throws Exception
  {
    return new String(Files.readAllBytes(Paths.get(getClass().getResource(s).toURI())), StandardCharsets.UTF_8.name());
  }

  class DescriptionElem
  {
    String description;

    String packageFromDescription;
    String enumFromDescription;
    ArrayList<DescriptionElem> children = new ArrayList<>();
  }

  class CotElem extends DescriptionElem
  {
    List<DescriptionElem> objects = new ArrayList<>();
    String uid;
    String domain;
  }

  class ObjectElem extends DescriptionElem
  {
    CotElem parent;
    String kind;
    String domain;
  }

  class CategoryElem extends DescriptionElem
  {
    ObjectElem parent;
    String value;
  }

  class SubCategoryElem extends DescriptionElem
  {
    CategoryElem parent;
    String value;
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
    CotElem currentCot;
    ObjectElem currentObject;
    CategoryElem currentCategory;
    SubCategoryElem currentSubCategory;
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

        case "cot":
          String uid = attributes.getValue("uid");
          if(uid.equals("226") || uid.equals("227") || uid.equals("228")) {
            currentCot = new CotElem();
            currentCot.uid = uid;
            currentCot.description = fixName(specialCaseObjectTypeName(attributes.getValue("name")));  // not an error
          }
          else
            currentCot = null;
          break;

        case "object":
          if (currentCot == null)
            break;

          currentObject = new ObjectElem();
          currentObject.kind = attributes.getValue("kind");
          currentObject.domain = attributes.getValue("domain");
          currentObject.description = attributes.getValue("description");
          if (currentObject.description != null)
              currentObject.description = currentObject.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          currentObject.parent = currentCot;
          currentCot.domain = currentObject.domain;
          setUniquePkgAndEmail(currentObject, currentCot.objects);
          currentCot.objects.add(currentObject);
          break;

        case "category":
          if (currentObject == null)
            break;
          currentCategory = new CategoryElem();
          currentCategory.value = attributes.getValue("value");
          currentCategory.description = attributes.getValue("description");
          if (currentCategory.description != null)
              currentCategory.description = currentCategory.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          currentCategory.parent = currentObject;
          setUniquePkgAndEmail(currentCategory, currentObject.children);
          currentObject.children.add(currentCategory);
          break;

        case "subcategory":
          if (currentCategory == null)
            break;
          currentSubCategory = new SubCategoryElem();
          currentSubCategory.value = attributes.getValue("value");
          currentSubCategory.description = attributes.getValue("description");
          if (currentSubCategory.description != null)
              currentSubCategory.description = currentSubCategory.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          currentSubCategory.parent = currentCategory;
          setUniquePkgAndEmail(currentSubCategory, currentCategory.children);
          currentCategory.children.add(currentSubCategory);
          break;

        default:
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
      try {
        switch (qName) {

          case "cot":
            if (currentCot != null)
            {
              currentNamespace = globalNamespace;
              writeCotFile(null);
            }
            currentCot = null;
            break;

          case "object":
            if (currentObject != null) //might have been deprecated
            {
              currentNamespace = globalNamespace + "::" + StringUtils.firstCharLower(fixName(currentCot));
              writeObjectFile(null);
            }
            currentObject = null;
            break;

          case "category":
            if (currentCategory != null) // might have been deprecated
            {
              currentNamespace = globalNamespace + "::" + StringUtils.firstCharLower(fixName(currentCot)) + "::" + StringUtils.firstCharLower(fixName(currentObject));
              writeCategoryFile(null);
            }
            currentCategory = null;
            break;

          case "subcategory":
            if (currentSubCategory != null) // might have been deprecated)
            {
              currentNamespace = globalNamespace + "::" + 
                                 StringUtils.firstCharLower(fixName(currentCot)) + "::" + 
                                 StringUtils.firstCharLower(fixName(currentObject)) + "::" +
                                 StringUtils.firstCharLower(fixName(currentCategory));
              writeSubCategoryFile(null);
            }
            currentSubCategory = null;
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
    
    private String specialCaseObjectTypeName(String s)
    {
        switch(s.toLowerCase()) {
            case "object types-areal object":
                return "ArealObject";
            case "object types-linear object":
                return "LinearObject";
            case "object types-point object":
                return "PointObject";
            default:
                return s;
        }
    }
    
    private void saveFile(TypeClassData data)
    {
        data.sb.append("}\n\n");
        data.sb.append(StringUtils.formatNamespaceEndStatement(currentNamespace, "cpp"));
        CppObjectTypesGenerator.this.saveFile(data.directory, data.className + ".h", data.sb.toString());
    }

    private void appendCommonStatements(TypeClassData data)
    {
      data.sb.append(licenseTemplate);

      String contents = String.format(objecttypeTemplate, data.pkg,
                                      sisoSpecificationTitleDate, currentCot.uid,
                                      StringUtils.formatNamespaceStatement(currentNamespace, "cpp"),
                                      data.className,data.className,                // class decl
                                      data.className, data.className                // inline ctor
                                      );
        
      data.sb.append(contents);
    }

    private void appendCategoryValueStatement(CategoryElem elem, String typ, StringBuilder sb)
    {
      sb.append(String.format(objecttypestatementTemplate,
                StringUtils.firstCharUpper(typ),
                "std::uint8_t",
                elem.value, elem.description));

    }
    private void appendSubCategoryValueStatement(SubCategoryElem elem, String typ, StringBuilder sb)
    {
      sb.append(String.format(objecttypestatementTemplate,
                      StringUtils.firstCharUpper(typ),
                      "std::uint8_t",
                      elem.value, elem.description));
    }
   
    private void appendKindStatement(ObjectElem elem, String typ, StringBuilder sb)
    {
      sb.append(String.format(objecttypestatementTemplate,
                      StringUtils.firstCharUpper(typ),
                      "ObjectKind",
                      elem.kind, elem.description));
    }
    
     private void appendDomainStatement(CotElem cot, String typ, StringBuilder sb)
    {
       sb.append(String.format(objecttypestatementTemplate,
                              StringUtils.firstCharUpper(typ),
                              "PlatformDomain",
                              cot.domain, cot.description));
    }
  
    private void writeCotFile(TypeClassData d)
    {
      TypeClassData data = d;
      if (data == null) {

        data = buildObjectTypeCommon(fixName(currentCot), currentCot);
        appendDomainStatement(currentCot, "Domain", data.sb); // not an error        

        saveFile(data);
        
      }
    }

    private void writeObjectFile(TypeClassData d)
    {
      

      TypeClassData data = d;
      if (data == null) {
        data = buildObjectTypeCommon(fixName(currentObject), currentObject);
      }
      appendDomainStatement(currentCot, "Domain", data.sb);  // not an error
      appendKindStatement(currentObject, "ObjectKind", data.sb);

      if (d == null) {
        saveFile(data);
      }
    }

    private void writeCategoryFile(TypeClassData d)
    {
      TypeClassData data = d;
      if (data == null) {
        data = buildObjectTypeCommon(fixName(currentCategory), currentCategory);
      }
      appendDomainStatement(currentCot, "Domain", data.sb); // not an error
      appendKindStatement(currentObject, "ObjectKind", data.sb);
      appendCategoryValueStatement(currentCategory, "Category", data.sb);

      if (d == null)
        saveFile(data);
    }

    private void writeSubCategoryFile(TypeClassData d) throws Exception
    {
      TypeClassData data = d;
      if (data == null) {
        data = buildObjectTypeCommon(fixName(currentSubCategory), currentSubCategory);
      }
      appendDomainStatement(currentCot, "Domain", data.sb); // not an error
      appendKindStatement(currentObject, "ObjectKind", data.sb);
      appendCategoryValueStatement(currentCategory, "Category", data.sb);
      appendSubCategoryValueStatement(currentSubCategory, "SubCategory", data.sb);

      if (d == null)
        saveFile(data);
    }

    private TypeClassData buildObjectTypeCommon(String fixedName, DescriptionElem elem)
    {
      try {
        TypeClassData data = new TypeClassData();
        data.sb = new StringBuilder();
        buildPackagePathAbstract(elem, data.sb);

        data.directory = new File(outputDirectory, data.sb.toString());
        data.directory.mkdirs();

        // Protect against duplicate class names
        int i = 1;
        while (new File(data.directory, fixedName + ".java").exists()) {
          fixedName = fixedName + i++;
        }

        String pkg = packageName + "." + pathToPackage(data.sb.toString());
        data.pkg = pkg;
        data.className = fixedName;
        data.sb.setLength(0);

        appendCommonStatements(data);
        return data;
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
      try (FileOutputStream  fso = new FileOutputStream(target);) {
        OutputStreamWriter fw = new OutputStreamWriter(fso, StandardCharsets.UTF_8);
        fw.write(contents);
        fw.flush();
      }
    }
    catch (IOException ex) {
      throw new RuntimeException("Error saving " + name + ": " + ex.getLocalizedMessage(), ex);
    }
  }

  private void setUniquePkgAndEmail(DescriptionElem elem, List<DescriptionElem> lis)
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
    if (elem instanceof CotElem)
      buildPackagePath((CotElem) elem, sb);
    else if (elem instanceof ObjectElem)
      buildPackagePath((ObjectElem) elem, sb);
    else if (elem instanceof CategoryElem)
      buildPackagePath((CategoryElem) elem, sb);
    else if (elem instanceof SubCategoryElem)
      buildPackagePath((SubCategoryElem) elem, sb);
  }

  private void buildPackagePath(CotElem cot, StringBuilder sb) throws Exception
  {
    sb.append(fixName(cot.description));
    sb.append("/");
  }

  private void buildPackagePath(ObjectElem obj, StringBuilder sb) throws Exception
  {
    buildPackagePath(obj.parent, sb);
    sb.append(fixName(obj.description));
    sb.append("/");
  }

  private void buildPackagePath(CategoryElem cat, StringBuilder sb) throws Exception
  {
    buildPackagePath(cat.parent, sb);
    sb.append(fixName(cat.description));
    sb.append("/");
  }

  private void buildPackagePath(SubCategoryElem sub, StringBuilder sb) throws Exception
  {
    buildPackagePath(sub.parent, sb);
//    sb.append(fixName(sub.description)); // TODO isn't this the name of the enumeration itself
//    sb.append("/");
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
/*
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
*/
  private String legalJavaDoc(String s)
  {
    s = s.replace("<", "&lt;");
    s = s.replace(">", "&gt;");
    s = s.replace("&", "&amp;");
    return s;
  }

  private String tryParent(DescriptionElem elem)
  {
    if (elem instanceof SubCategoryElem)
      return fixName(((SubCategoryElem) elem).parent.description);

    if (elem instanceof CategoryElem)
      return fixName(((CategoryElem) elem).parent.description);

    if (elem instanceof ObjectElem)
      return fixName(((ObjectElem) elem).parent.description);

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
  /**
     * Normalize string characters to create valid description
     * @param value of interest
     * @return normalized value
     */
    private String normalizeDescription(String value)
    {
        return StringUtils.normalizeToken(value);
    }
    /**
     * Normalize string characters to create valid Java name.  Note that unmangled name typically remains available in the description
     * @param value of interest
     * @return normalized value
     */
    private String normalizeToken(String value)
    {
        return StringUtils.normalizeToken(value);
    }

}