package edu.nps.moves.dis7.source.generator.enumerations;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.Properties;
import java.util.Set;
import java.util.HashSet;

public class CSharpBitfieldWriter
{

    CSharpEnumGenerator parentClass = null;
    Properties aliases = null;
    Set<String> enumNames = new HashSet<>();

    protected boolean useDotNet = true;

    private CSharpBitfieldWriter()
    {
    }

    public CSharpBitfieldWriter(CSharpEnumGenerator aParent)
    {
        parentClass = aParent;

        writeCpabilityInterface();
    }

    public void writeCpabilityInterface()
    {
        // Write out the interface for the Capability bitfields
        String className = "ICapabilities";
        String indent = StringUtils.tabs(1);

        StringBuilder sb = new StringBuilder();
        sb.append(parentClass.licenseTemplate);
        sb.append("\n");
        sb.append("using System;\n");

        sb.append(StringUtils.formatNamespaceStatement(parentClass.enumNamespace, parentClass.CSHARP ) + "\n");

        sb.append(indent + "public interface " + className +"\n");
        sb.append(indent + "{\n");
        sb.append(indent + StringUtils.tabs(1) + "string ToString();\n");
        sb.append(indent + StringUtils.tabs(1) + "Int32 GetMarshaledSize();\n");
        sb.append(indent + StringUtils.tabs(1) + "void Marshal(dis.DataOutputStream dataOutputStream);\n");
        sb.append(indent + StringUtils.tabs(1) + "void Unmarshal(dis.DataInputStream dataInputStream);\n");
        sb.append(indent + "}\n");


        sb.append("\n");
        sb.append(StringUtils.formatNamespaceEndStatement(parentClass.enumNamespace, parentClass.CSHARP));
        sb.append("\n");

        parentClass.writeOutFile(sb, className, parentClass.outputDirectory, className + ".cs");
    }

    public void writeOutBitfield(GeneratedEnumClasses.BitfieldElem bitfieldElement)
    {
        try {
            String clsName = parentClass.uidClassNames.get(bitfieldElement.uid);
            // System.out.println(String.format("Generating bitfield class %s(%s)\n", clsName, bitfieldElement.uid));

            if (clsName == null)
            {
                System.out.flush();
                System.err.println("*** Didn't find a class name for uid = " + bitfieldElement.uid);
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

            if(bitfieldElement.uid.equals("4"))
              aliases = parentClass.uid4aliases;
            else
              aliases = null;


            StringBuilder sb = new StringBuilder();
            sb.append(parentClass.licenseTemplate);
            // writeUsingStatements(sb, bitfieldElement, classNameCorrected);

            String otherInf = parentClass.uid2ExtraInterface.get(bitfieldElement.uid);

            if (otherInf == null)
                writeBitfieldStart(sb, bitfieldElement, classNameCorrected);
            else
                writeBitfieldStart(sb, bitfieldElement, classNameCorrected, otherInf);
            sb.append("\n");
            writeBitfieldMasks(sb, bitfieldElement, classNameCorrected);
            writeBitfieldToString(sb, bitfieldElement, classNameCorrected);

            if(useDotNet)
            {
                writeReflectionMethod(sb, bitfieldElement, classNameCorrected);
            }

            writeBitfieldOperators(sb, bitfieldElement, classNameCorrected);
            writeBitfieldMarshalers(sb, bitfieldElement, classNameCorrected);

            // end the bitfield class
            sb.append(StringUtils.tabs(1) + "}\n");

            sb.append("\n");
            sb.append(StringUtils.formatNamespaceEndStatement(parentClass.enumNamespace, parentClass.CSHARP));
            sb.append("\n");

            parentClass.writeOutFile(sb, classNameCorrected, parentClass.outputDirectory, classNameCorrected + ".cs");

        } catch (Exception e) {
            System.err.println("Failed to write bitfield data for element " + bitfieldElement.name);
            System.err.println(e);
            System.exit(-1);
        }
    }

    private void writeBitfieldToString(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {
        // public override string ToString()
        // {
        //     string report = appearance.ToString() + "\n";
        //     report += "\tPaint Scheme : " + PaintScheme.ToString() + "\n";
        //     report += "\t  Propulsion : " + Propulsion.ToString() + "\n";
        //     report += "\t      Damage : " + Damage.ToString() + "\n";
        //     report += "\t       Smoke : " + Smoke.ToString() + "\n";
        //     report += "\t         Fun : " + Fun.ToString() + "\n";
        //     return report;
        // }

        String indent = StringUtils.tabs(2);
        String mapName  = StringUtils.firstCharLower(className);

        enumNames.clear();

        sb.append(indent + "public string BitfieldToString()\n");
        sb.append(indent + "{\n");
        sb.append(String.format("%sstring outputStream = %s.ToString();\n", StringUtils.tabs(3), mapName));
        sb.append(StringUtils.tabs(3) + "return outputStream;\n");
        sb.append(indent + "}\n");

        sb.append("\n");

        sb.append(indent + "public override string ToString()\n");
        sb.append(indent + "{\n");

        indent += StringUtils.tabs(1);

        sb.append(String.format("%sstring outputStream = %s.ToString() + System.Environment.NewLine;\n", indent, mapName));

        String enumName = "";
        for (GeneratedEnumClasses.BitfieldRowElem row : bitfieldElement.elems)
        {
            String xrefName = null;
            if (row.xrefclassuid != null)
            {
                xrefName = parentClass.uidClassNames.get(row.xrefclassuid);
                // sb.append("XREF : " + xrefName + "\n");
            }

            enumName = parentClass.cleanupEnumName(row.name, false);
            // if (xrefName != null) enumName = xrefName;

            sb.append(String.format("%soutputStream += \"%s : \" +  %s.ToString() + System.Environment.NewLine;\n", indent, enumName, enumName));

        }
        sb.append("\n");
        sb.append(StringUtils.tabs(3) + "return outputStream;\n");
        sb.append(StringUtils.tabs(2) + "}\n");
    }

    private void writeReflectionMethod(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {
        String indent = StringUtils.tabs(2);
        String mapName  = StringUtils.firstCharLower(className);
        String enumName = "";

        sb.append(indent + "public void Reflection(StringBuilder sb)\n");
        sb.append(indent + "{\n");

        indent += StringUtils.tabs(1);

        // Write out the bitfield representation
        // <Bitfield type=\"BitVector32>\">0101010101</BitField>
        enumName = "BitField";
        sb.append(String.format("%ssb.AppendLine(\"<%s type=\\\"BitVector32\\\">\" + %s.ToString() + \"</%s>\");\n", 
                                                    indent, enumName, mapName, enumName));

        
        for (GeneratedEnumClasses.BitfieldRowElem row : bitfieldElement.elems)
        {
            String xrefName = null;
            if (row.xrefclassuid != null)
            {
                xrefName = parentClass.uidClassNames.get(row.xrefclassuid);
            }
            

            enumName = parentClass.cleanupEnumName(row.name, false);

            // String toStringMethod = String.format("%s.%s_Accessors.ToString(%s)", enumNamespace, anAttribute.getType(), IVAR_PREFIX + anAttribute.getName());
            if (xrefName != null)
                sb.append(String.format("%ssb.AppendLine(\"<%s type=\\\"%s\\\">\" + %s_Accessors.ToString(%s) + \"</%s>\");\n", 
                                        indent, enumName, xrefName, xrefName, enumName, enumName
                                        ));
            else
            {
                xrefName = "Uint32";
                sb.append(String.format("%ssb.AppendLine(\"<%s type=\\\"%s\\\">\" + %s.ToString() + \"</%s>\");\n", 
                            indent, enumName, xrefName, enumName, enumName
                            ));
            }
        }
        sb.append(StringUtils.tabs(2) + "}\n");
        sb.append("\n");
    }

    private void writeBitfieldMasks(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {
        enumNames.clear();

        String lastMask = "";
        for (GeneratedEnumClasses.BitfieldRowElem row : bitfieldElement.elems)
        {
            String xrefName = null;
            if (row.xrefclassuid != null)
            {
                xrefName = parentClass.uidClassNames.get(row.xrefclassuid);
            }
            else
            {
                xrefName = "UInt8";
            }

            String enumName = parentClass.cleanupEnumName(row.name, false);
            String mapName  = StringUtils.firstCharLower(className);
            String maskName = StringUtils.firstCharLower(enumName) + "Mask";                

            String bitsType = new String();
            if  (Integer.valueOf(row.length) == 1)
                bitsType = "boolean";
            else bitsType = "length=" + row.length;

            // sb.append(String.format(parentClass.disbitsetcommentxrefTemplate, 
            //                         "bit position " + row.bitposition + ", " + bitsType,
            //                         StringUtils.htmlize((row.description==null?"":StringUtils.normalizeDescription(row.description)+", ")),xrefName));

            // output the BitVector32 decl
            if (lastMask.equals(""))
                sb.append(String.format("%sprivate static BitVector32.Section %s = BitVector32.CreateSection(%s);\n", StringUtils.tabs(2), maskName, row.length));
            else
                sb.append(String.format("%sprivate static BitVector32.Section %s = BitVector32.CreateSection(%s, %s);\n", StringUtils.tabs(2), maskName, row.length, lastMask));

            sb.append(String.format(parentClass.disbitsetgetsetTemplate,
                                    xrefName, enumName,                 // class statement
                                    xrefName, mapName, maskName,        // getter
                                    mapName, maskName                   // settter
                                    ));
            sb.append("\n");
            lastMask = maskName;
                
        }
    }

    private void writeBitfieldStart(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className, String...  interfaceName)
    {
        String interfaceStatement = "";

        if (interfaceName.length > 0)
        {
            interfaceStatement = ": " + interfaceName[0];
        }
        
        sb.append(String.format(parentClass.disbitset1Template, 
                    parentClass.packageName, 
                    parentClass.sisoSpecificationTitleDate,
                    "UID " + bitfieldElement.uid, bitfieldElement.size,
                    bitfieldElement.name,
                    StringUtils.formatNamespaceStatement(parentClass.enumNamespace, parentClass.CSHARP ),
                    className, interfaceStatement,
                    StringUtils.firstCharLower(className)
                    ));
    }

    private void writeUsingStatements(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {
        bitfieldElement.elems.forEach((row) -> {
            // this one has a class type
            String xrefName = null;
            if (row.xrefclassuid != null)
            {
                xrefName = parentClass.uidClassNames.get(row.xrefclassuid); //Main.this.uid2ClassName.getProperty(row.xrefclassuid);
                // sb.append(String.format("#include <dis/enumerations/%s.h>\n", xrefName));
                sb.append(String.format("using %s.%s;\n", parentClass.enumNamespace, xrefName));
            }
        });
    }

    private void writeBitfieldOperators(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {
        String mapName  = StringUtils.firstCharLower(className);

        sb.append(String.format(parentClass.disbitsetopersTemplate,
                                mapName,                        // GetHashCode
                                className,                      // Override Equals
                                className, mapName, mapName,    // Equals
                                className, className,           // operator ==
                                className, className            // operator !=
                                ));

    }
    private void writeBitfieldMarshalers(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {
        String mapName  = StringUtils.firstCharLower(className);

        sb.append(String.format(parentClass.disbitsetmarshalersTemplate,
                                mapName,        // Marshal
                                mapName         // Unmarshal
                                ));

    }

}