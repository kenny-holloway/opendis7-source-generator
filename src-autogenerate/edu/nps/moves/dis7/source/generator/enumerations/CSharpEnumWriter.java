package edu.nps.moves.dis7.source.generator.enumerations;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.Properties;
import java.util.Set;
import java.util.HashSet;

// public class CSharpEnumWriter implements CSharpEnumGenerator::EnumWriter
public class CSharpEnumWriter
{
    CSharpEnumGenerator parentClass = null;
    Properties aliases = null;
    Set<String> enumNames = new HashSet<>();

    private CSharpEnumWriter()
    {
    } 

    public CSharpEnumWriter(CSharpEnumGenerator aParent)
    {
        parentClass = aParent;
    }

    public void writeOutEnum(GeneratedEnumClasses.EnumElem enumElement)
    {
        try {
            String clsName = parentClass.uidClassNames.get(enumElement.uid); //Main.this.uid2ClassName.getProperty(el.uid);
            if (clsName == null)
            {
                System.out.flush();
                System.err.println("*** Didn't find a class name for uid = " + enumElement.uid);
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

            if(enumElement.uid.equals("4"))
              aliases = parentClass.uid4aliases;
            else
              aliases = null;

            // System.out.println(String.format("Generating enumeration class %s(%s) ", clsName, enumElement.uid));

            StringBuilder sb = new StringBuilder();

            sb.append(parentClass.licenseTemplate);

            sb.append(String.format(parentClass.disenumfilestartTemplate));

            // // Create a handy namespace shortcut
            // String namespaceToken = parentClass.enumNamespace.replaceAll("::", ".");
            // sb.append("using DisEnums = " + namespaceToken + ";\n");

            sb.append("\n");
            sb.append(StringUtils.formatNamespaceStatement(parentClass.enumNamespace, parentClass.CSHARP ));
            sb.append("\n");

            writeEnumStart(sb, enumElement, classNameCorrected);
            writeEnumDefinitions(sb, enumElement, classNameCorrected);
            writeEnumEnd(sb, enumElement, classNameCorrected);
            sb.append("\n");

            // write out descriptions and get_description method
            writeEnumDescriptions(sb, enumElement, classNameCorrected);
            sb.append("\n");

            // all other operators  is_valid, ==, != etc
            writeEnumOperators(sb, enumElement, classNameCorrected);
            sb.append("\n");

            writeMarshalOperators(sb, enumElement, classNameCorrected);

            // end the accessor class
            sb.append(StringUtils.tabs(1) + "}\n");

            sb.append("\n");
            sb.append(StringUtils.formatNamespaceEndStatement(parentClass.enumNamespace, parentClass.CSHARP));
            sb.append("\n");

            writeOutFile(sb, classNameCorrected);

        } catch (Exception e) {
            System.err.println("Failed to write enum data for element " + enumElement.name);
            System.err.println(e);
            System.exit(-1);
        }
    }

    private void writeEnumDefinitions(StringBuilder sb, GeneratedEnumClasses.EnumElem enumElement, String className)
    {
        boolean lastOne = false;
        int rowCount = 0;

        enumNames.clear();

        for (GeneratedEnumClasses.EnumRowElem row : enumElement.elems) {
            if (rowCount == enumElement.elems.size() -1)  lastOne = true;

            // aliases are set for UID 4 (DisPduType)
            if(aliases != null && aliases.getProperty(row.value)!=null) {
                writeOneEnum(sb,row,aliases.getProperty(row.value).toLowerCase(), lastOne);
            }
            else {
                String enumName = StringUtils.createEnumName(enumNames, StringUtils.normalizeDescription(row.description), false);
                writeOneEnum(sb, row, enumName, lastOne);
            }
            if (lastOne == false) sb.append("\n");
            ++rowCount;
        }

    }

    private void writeOneEnum(StringBuilder sb, GeneratedEnumClasses.EnumRowElem row, String enumName, boolean... isLastOne)
    {
        String xrefName = null;
        String enumStatement = "";

        if (row.xrefclassuid != null) {
          xrefName = parentClass.uidClassNames.get(row.xrefclassuid);
        }

        enumName = StringUtils.normalizeToken(enumName);
        String enumDescription = StringUtils.normalizeDescription(row.description);

        enumName = "@" + enumName;
        if (xrefName == null)
        {
            enumStatement = String.format(parentClass.disenumpart2Template, enumDescription, enumName, row.value);
        }
        else
        {
            sb.append(String.format(parentClass.disenumcommentTemplate, row.xrefclassuid, xrefName));
            enumStatement = String.format(parentClass.disenumpart21Template, enumDescription, enumName, row.value);            
        }
        
        if ((isLastOne.length > 0) && (isLastOne[0] == true))
        {
            enumStatement = enumStatement.replace(",","");
        }

        sb.append(enumStatement);

    }


    private void writeEnumEnd(StringBuilder sb, GeneratedEnumClasses.EnumElem enumElement, String className)
    {
        // What else?
        // End the enumeration definition
        sb.append(StringUtils.tabs(1) + "}\n");
    }

    private void writeEnumStart(StringBuilder sb, GeneratedEnumClasses.EnumElem enumElement, String className)
    {
        int enumSize = parentClass.getEnumSize(enumElement);
        int numberOfEnumerations = enumElement.elems.size();

        if(enumElement.footnote == null)
        {
            sb.append(String.format(parentClass.disenumpart1Template,
                                    parentClass.packageName,
                                    parentClass.sisoSpecificationTitleDate,
                                    "UID " + enumElement.uid, 
                                    enumElement.size,  // marshal size
                                    enumElement.name, numberOfEnumerations,  // class has x enumerations total
                                    className,
                                    Integer.toString(enumSize)
                                    ));
        }
        else
        {
            sb.append(String.format(parentClass.disenumpart1withfootnoteTemplate, 
                                    parentClass.packageName, 
                                    parentClass.sisoSpecificationTitleDate,  
                                    "UID " + enumElement.uid,
                                    enumElement.size,
                                    enumElement.name,
                                    numberOfEnumerations, 
                                    enumElement.footnote,
                                    className,
                                    Integer.toString(enumSize)
                                    ));
        }
    }

    private void writeEnumDescriptions(StringBuilder sb, GeneratedEnumClasses.EnumElem enumElement, String className)
    {
        // TODO make a template
        String indent = "    ";
    
        String mapName = String.format("%s_descriptions", className);

        enumNames.clear();

        // Output make key name, and the map name
        sb.append(String.format(parentClass.disenumdescriptionsTemplate, 
                                className,              // Accessor class name
                                className, mapName,     // ConcurrentDictionary decl
                                className,              // new ConcurrentDictionary
                                className               // new Dictionary
                                ));

        int rowCount = 0;
        for (GeneratedEnumClasses.EnumRowElem row : enumElement.elems) {
            String enumName = StringUtils.createEnumName(enumNames, StringUtils.normalizeDescription(row.description), false);
            String enumDescription = StringUtils.normalizeDescription(row.description);
            String enumStatement = "";

            if(aliases != null && aliases.getProperty(row.value)!=null) {
                enumName = aliases.getProperty(row.value).toLowerCase();
            }

            enumStatement = String.format("%s{%s.@%s, \"%s\"},", StringUtils.tabs(5), className, enumName, enumDescription);  

            if (rowCount == enumElement.elems.size() -1) {
                enumStatement = enumStatement.replace(",","");
            }
            else {
                sb.append(enumStatement + "\n");
            }
        }

        // Close the Concurrent Dictionary declraration
        sb.append(String.format("%s}\n",  StringUtils.tabs(4)));
        sb.append(String.format("%s);\n", StringUtils.tabs(3)));

        sb.append("\n");
        sb.append(String.format(parentClass.disenumtostringTemplate, className, mapName));

    }
    
    private void writeEnumOperators(StringBuilder sb, GeneratedEnumClasses.EnumElem enumElement, String className)
    {
    
        String mapName = String.format("%s_descriptions", className);

        sb.append(String.format(parentClass.disenumOperDefTemplate,
                                className,                  // ToString
                                className, className,       // IsValid
                                className, mapName         // AddCustomValue
                                ));
    }

    private void writeMarshalOperators(StringBuilder sb, GeneratedEnumClasses.EnumElem enumElement, String className)
    {
        String mapName = String.format("%s_descriptions", className);

        int enumSize = parentClass.getEnumSize(enumElement);

        String castValue = "UInt"  + String.valueOf(enumSize);
        String converter = "ToInt" + String.valueOf(enumSize);

        if (enumSize == 8)
        {
            sb.append(String.format(parentClass.disenum8bitmarshalerTemplate,
                                    className, castValue,                       // GetMarshaledSize
                                    parentClass.globalNamespace, className,     // Marshal decl
                                    castValue,                                  // marshal cast
                                    parentClass.globalNamespace, className,     // Unmarshal decl
                                    className                                   // Unmarshal cast
                                    ));
        }

        else if (enumSize == 16 || enumSize == 32)
        {
            sb.append(String.format(parentClass.disenummarshalersTemplate,
                                    className, castValue,                       // GetMarshaledSize
                                    parentClass.globalNamespace, className,     // marshal with dataOutputStream
                                    parentClass.globalNamespace, className,     // Marshal decl
                                    castValue,                                  // Marshal cast
                                    parentClass.globalNamespace, className,     // Unmarshal with DataOutputStream
                                    parentClass.globalNamespace, className,     // Unmarshal decl
                                    className,                                  // byte array alloc
                                    className, converter                        // unmarshal cast and read
                                    ));

        }

    }

    private void writeOutFile(StringBuilder sb, String className)
    {
        File targetFile = new File(parentClass.outputDirectory, className + ".cs");

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