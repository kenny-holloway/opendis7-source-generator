package edu.nps.moves.dis7.source.generator.enumerations;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.Properties;
import java.util.Set;
import java.util.HashSet;

public class CSharpDictionaryWriter
{

    CSharpEnumGenerator parentClass = null;
    Properties aliases = null;
    Set<String> enumNames = new HashSet<>();

    private CSharpDictionaryWriter()
    {
    }

    public CSharpDictionaryWriter(CSharpEnumGenerator aParent)
    {
        parentClass = aParent;
    }

    public void writeOutDictionary(GeneratedEnumClasses.DictionaryElem dictionaryElement)
    {
        try {
            String clsName = parentClass.uidClassNames.get(dictionaryElement.uid);
            System.out.println(String.format("Generating dictionary class %s(%s)\n", clsName, dictionaryElement.uid));

            if (clsName == null)
            {
                System.out.flush();
                System.err.println("*** Didn't find a class name for uid = " + dictionaryElement.uid);
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

            if(dictionaryElement.uid.equals("4"))
              aliases = parentClass.uid4aliases;
            else
              aliases = null;

            StringBuilder sb = new StringBuilder();
            sb.append(parentClass.licenseTemplate);
            sb.append(String.format(parentClass.disenumfilestartTemplate));


            sb.append(StringUtils.formatNamespaceStatement(parentClass.globalNamespace + "::dictionary", parentClass.CSHARP ));
            sb.append("\n");

            String listName = StringUtils.firstCharLower(classNameCorrected) + "List";
            sb.append(String.format(parentClass.disdictenumpart1Template,
                                    parentClass.sisoSpecificationTitleDate, 
                                    parentClass.packageName, 
                                    "UID " + dictionaryElement.uid,   // UID
                                    listName  // listName
                                    ));

            writeDictionaryElements(sb, dictionaryElement, classNameCorrected);
            // end the Dictionary definition
            sb.append(StringUtils.tabs(2) + "};\n");

            sb.append("\n");
            sb.append(String.format(parentClass.disdictenumtostringTemplate, 
                                    listName,       // GetDescription
                                    listName        // ToString
                                    ));
            sb.append("\n");
            
            // end the accessor class
            sb.append(StringUtils.tabs(1) + "}\n");

            sb.append("\n");
            sb.append(StringUtils.formatNamespaceEndStatement(parentClass.globalNamespace + "::dictionary", parentClass.CSHARP));

            parentClass.writeOutFile(sb, classNameCorrected, parentClass.outputDirectory, classNameCorrected + ".cs");

        } catch (Exception e) {
            System.err.println("Failed to write dictionary data for element " + dictionaryElement.name);
            System.err.println(e);
            System.exit(-1);
        }
    }

    private void writeDictionaryElements(StringBuilder sb, GeneratedEnumClasses.DictionaryElem dictionaryElement, String className)
    {

        // enumNames.clear();

        for (GeneratedEnumClasses.DictionaryRowElem row : dictionaryElement.elems)
        {
            String name = row.value.replaceAll("[^a-zA-Z0-9]", ""); // only chars and numbers
            String fullName = StringUtils.normalizeDescription(row.description);

            sb.append(String.format(parentClass.disdictenumpart2Template, name, fullName));
        }
    }
}