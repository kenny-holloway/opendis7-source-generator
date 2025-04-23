package edu.nps.moves.dis7.source.generator.enumerations;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.Properties;
import java.util.Set;
import java.util.HashSet;

public class PythonBitfieldWriter
{

    PythonEnumGenerator parentClass = null;
    Properties aliases = null;
    Set<String> enumNames = new HashSet<>();

    protected boolean useDotNet = true;

    private PythonBitfieldWriter()
    {
    }

    public PythonBitfieldWriter(PythonEnumGenerator aParent)
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

        sb.append(StringUtils.formatNamespaceStatement(parentClass.enumNamespace, parentClass.PYTHON ) + "\n");

        sb.append(indent + "public interface " + className +"\n");
        sb.append(indent + "{\n");
        sb.append(indent + StringUtils.tabs(1) + "string ToString();\n");
        sb.append(indent + StringUtils.tabs(1) + "Int32 GetMarshaledSize();\n");
        sb.append(indent + StringUtils.tabs(1) + "void Marshal(dis.DataOutputStream dataOutputStream);\n");
        sb.append(indent + StringUtils.tabs(1) + "void Unmarshal(dis.DataInputStream dataInputStream);\n");
        sb.append(indent + "}\n");


        sb.append("\n");
        sb.append(StringUtils.formatNamespaceEndStatement(parentClass.enumNamespace, parentClass.PYTHON));
        sb.append("\n");

        String pythonFileName = StringUtils.camelCasetoLowerUnderscore(className);
        parentClass.writeOutFile(sb, className, parentClass.outputDirectory, pythonFileName + ".cs");
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
            writeUsingStatements(sb, bitfieldElement, classNameCorrected);
            sb.append("\n");

            String otherInf = parentClass.uid2ExtraInterface.get(bitfieldElement.uid);


            if (otherInf == null)
                writeBitfieldStart(sb, bitfieldElement, classNameCorrected);
            else
                writeBitfieldStart(sb, bitfieldElement, classNameCorrected, otherInf);

            writeBitfieldDecl(sb, bitfieldElement, classNameCorrected);
            sb.append(StringUtils.tabs(1) + "]\n\n");

            writeBitFieldUnion(sb,bitfieldElement, classNameCorrected);
            sb.append("\n");

            writeBitFieldGettersSetters(sb, bitfieldElement, classNameCorrected);

            writeBitfieldToString(sb, bitfieldElement, classNameCorrected);
            writeBitfieldOperators(sb, bitfieldElement, classNameCorrected);
            writeBitfieldMarshalers(sb, bitfieldElement, classNameCorrected);

            String pythonFileName = StringUtils.camelCasetoLowerUnderscore(classNameCorrected);
            parentClass.writeOutFile(sb, classNameCorrected, parentClass.outputDirectory, pythonFileName + ".py");

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

        String indent = StringUtils.tabs(1);
        String mapName  = StringUtils.firstCharLower(className);

        enumNames.clear();

        sb.append("\n");

        sb.append(indent + "def to_string(self):\n");

        indent += StringUtils.tabs(1);
        sb.append(indent + "outputStream = \"\"\n");
        sb.append(indent + "outputStream += format(self.asbyte, '#032b') + \"\\n\"\n");

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

            // if not set, we assume this as a UInt8
            if (row.xrefclassuid != null)
                sb.append(String.format("%soutputStream += \"%s : \" +  self.%s.get_description + \"\\n\"\n", indent, enumName, enumName));
            else
                sb.append(String.format("%soutputStream += \"%s : \" +  str(self.%s) + \"\\n\"\n", indent, enumName, enumName));

            // if (xrefName != null) enumName = xrefName;
// outputStream += "PaintScheme : " +  self.PaintScheme.get_description + "\n"
            

        }
        sb.append(indent + "return outputStream\n");
        sb.append("\n");

        sb.append(StringUtils.tabs(1) + "def __str__(self):\n");
        sb.append(StringUtils.tabs(2) + "return self.to_string()\n");
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

    private void writeBitFieldGettersSetters(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {
        enumNames.clear();

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

            String enumName = StringUtils.firstCharLower(parentClass.cleanupEnumName(row.name, false));
            String mapName  = StringUtils.firstCharLower(className);

            String bitsType = new String();
            if  (Integer.valueOf(row.length) == 1)
                bitsType = "boolean";
            else bitsType = "length=" + row.length;    

            String getStatement = "";
            if (row.xrefclassuid == null)
                getStatement = "self.capabilities._" + enumName;
            else
                getStatement = String.format("%s.get_enum(self._%s)", xrefName, enumName);

            sb.append(String.format(parentClass.disbitsetgetsetTemplate,
                                    enumName, xrefName, enumName,           // setter
                                    enumName, xrefName, getStatement,       // getter
                                    StringUtils.firstCharUpper(enumName), enumName, enumName            // property
            ));
            // sb.append(String.format(parentClass.disbitsetgetsetTemplate,
            //                         xrefName, enumName,                 // class statement
            //                         xrefName, mapName, maskName,        // getter
            //                         mapName, maskName                   // settter
            //                         ));
            sb.append("\n");
                
        }
    }

    private void writeBitfieldDecl(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {
        enumNames.clear();

        int numElems = bitfieldElement.elems.size();
        int elemNum = 0;
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

            String bitsType = new String();
            if  (Integer.valueOf(row.length) == 1)
                bitsType = "boolean";
            else bitsType = "length=" + row.length;

            sb.append(String.format(parentClass.disbitsetcommentxrefTemplate, 
                                    "bit position " + row.bitposition + ", " + bitsType,
                                    StringUtils.htmlize((row.description==null?"":StringUtils.normalizeDescription(row.description)+", ")),xrefName));
            // ("_paintScheme", ctypes.c_ubyte, 1),

            String ctype = "UByte";
            if (Integer.parseInt(row.length) >= 16)
            {
                ctype = "UInt";
            }
            sb.append(String.format("%s(\"_%s\", %s, %s)", 
                StringUtils.tabs(2),
                StringUtils.firstCharLower(enumName),
                ctype,
                row.length
                ));

            elemNum += 1;
            if (elemNum < numElems) sb.append(",\n");

            sb.append("\n");
                
        }
    }

    private void writeBitFieldUnion(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {
        sb.append(String.format(parentClass.disbitsetunionTemplate, className, className));
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
                    className                                  // bitfield class decl
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
                String pythonFileName = StringUtils.camelCasetoLowerUnderscore(xrefName);
                sb.append(String.format("from .%s import %s\n", pythonFileName, xrefName));
            }
        });
    }

    private void writeBitfieldOperators(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {

        String comparisonOperators = "    def __eq__(self, other):\n"
                             .concat("        if isinstance(other, self.__class__):\n")
                             .concat("            return self.__dict__ == other.__dict__\n")
                             .concat("        else:\n")
                             .concat("            return False\n")
                             .concat("\n")
                             .concat("    def __ne__(self, other):\n")
                             .concat("        return not self.__eq__(other)\n")
                             .concat("\n")
                            .concat("    def diff(self,other) -> set:\n")
                            .concat("        diffs = set()\n")
                            .concat("        for key, value in self.__dict__.items():\n")
                            .concat("            value2 = other.__dict__[key]\n")
                            .concat("            if (value != value2):\n")
                            .concat("                if type(value) is list:\n")
                            .concat("                    diffs.add((key, str(value)))\n")
                            .concat("                    diffs.add((key, str(value2)))\n")
                            .concat("                elif (type(value).__module__ == \"builtins\"):\n")
                            .concat("                    diffs.add((key, value))\n")
                            .concat("                    diffs.add((key, value2))\n")
                            .concat("                elif (isinstance(value, object)):\n")
                            .concat("                    diffs.update(value.diff(value2))\n")
                            .concat("                else:\n")
                            .concat("                    diffs.add((key, value))\n")
                            .concat("                    diffs.add((key, value2))\n")
                            .concat("        return(diffs)\n");
         sb.append("\n");
         sb.append(comparisonOperators);
         sb.append("\n");

    }
    private void writeBitfieldMarshalers(StringBuilder sb, GeneratedEnumClasses.BitfieldElem bitfieldElement, String className)
    {
        String mapName  = StringUtils.firstCharLower(className);

        sb.append(String.format(parentClass.disbitsetmarshalersTemplate,
                                bitfieldElement.size));    // get_marshaled_size

    }

}