package edu.nps.moves.dis7.source.generator.enumerations;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

  /** XML handler for recursively reading information and autogenerating code, namely an
     * inner class that handles the SAX parsing of the XML file. This is relatively simple, if
     * a little verbose. Basically we just create the appropriate objects as we come across the
     * XML elements in the file.
     */
public class CSharpEnumHandler extends DefaultHandler
{
    private static String       sisoSpecificationTitleDate = "";
    CSharpEnumWriter enumWriter = null;
    CSharpBitfieldWriter bitfieldWriter = null;
    CSharpDictionaryWriter dictionaryWriter = null;

    private CSharpEnumGenerator parentClass = null;

    /** default constructor */
    private CSharpEnumHandler()
    {
        super();
    }

    public CSharpEnumHandler(CSharpEnumGenerator aParent)
    {
        this();
        parentClass = aParent;
        enumWriter = new CSharpEnumWriter(aParent);
        bitfieldWriter = new CSharpBitfieldWriter(aParent);
        dictionaryWriter = new CSharpDictionaryWriter(aParent);
    }

    List<GeneratedEnumClasses.EnumElem> enums = new ArrayList<>();
    GeneratedEnumClasses.EnumElem currentEnum;
    GeneratedEnumClasses.EnumRowElem currentEnumRow;

    List<GeneratedEnumClasses.DictionaryElem> dictionaries = new ArrayList<>();
    GeneratedEnumClasses.DictionaryElem currentDict;
    GeneratedEnumClasses.DictionaryRowElem currentDictRow;

    List<GeneratedEnumClasses.BitfieldElem> bitfields = new ArrayList<>();
    GeneratedEnumClasses.BitfieldElem currentBitfield;
    GeneratedEnumClasses.BitfieldRowElem currentBitfieldRow;

    Set<String> testElems = new HashSet<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    {
        if (qName.equals("category") && "true".equals(attributes.getValue("deprecated")))
            return;

        switch (qName) {
            case "enum":
                currentEnum = new GeneratedEnumClasses.EnumElem();
                currentEnum.name = StringUtils.fixName(attributes.getValue("name")); // name canonicalization C14N
                currentEnum.uid  = attributes.getValue("uid");
                currentEnum.size = attributes.getValue("size");
                currentEnum.footnote = attributes.getValue("footnote");
                if (currentEnum.footnote != null)
                    currentEnum.footnote = StringUtils.normalizeDescription(currentEnum.footnote);
                enums.add(currentEnum);
                //maybeSysOut(attributes.getValue("xref"), "enum uid " + currentEnum.uid + " " + currentEnum.name);
                break;

            case "meta":
                if (currentEnum == null)
                    break;
                if ((currentEnumRow.description == null) || currentEnumRow.description.isEmpty())
                {
                    String   key = attributes.getValue("key");
                    String value = attributes.getValue("value");
                    if (key != null)
                        currentEnumRow.description = key.toUpperCase() + "_";
                    if (value != null)
                        currentEnumRow.description += value;
                    if (currentEnumRow.description != null)
                        currentEnumRow.description = StringUtils.normalizeDescription(currentEnumRow.description);
                }
                break;

            case "enumrow":
                if (currentEnum == null)
                    break;
                currentEnumRow = new GeneratedEnumClasses.EnumRowElem();
                // TODO if description is empty, get meta key-value
                currentEnumRow.description = attributes.getValue("description");
                if (currentEnumRow.description != null)
                    currentEnumRow.description = StringUtils.normalizeDescription(currentEnumRow.description);
                currentEnumRow.value = attributes.getValue("value");

/* special case, 2147483648 is one greater than max Java integer, reported 30 JAN 2022
<enumrow value="2147483648" description="Rectangular Volume Record 4" group="1" status="hold" uuid="fdccf8e0-e73c-4137-b140-f7d0882b0778">
    <cr value="1913" />
</enumrow>
*/            
        if (currentEnumRow.value.equals("2147483648"))
        {
            System.out.println ("*** Special case 'Rectangular Volume Record 4' value 2147483648 reset to 2147483647" +
                                " in order to avoid exceeding max integer value");
            currentEnumRow.value = "2147483647";
        }
                currentEnumRow.footnote = attributes.getValue("footnote");
                if (currentEnum.footnote != null)
                    currentEnum.footnote = StringUtils.normalizeDescription(currentEnum.footnote);
                currentEnumRow.xrefclassuid = attributes.getValue("xref");
                currentEnum.elems.add(currentEnumRow);
                //maybeSysOut(attributes.getValue("xref"), "enumrow uid " + currentEnum.uid + " " + currentEnum.name + " " + currentEnumRow.value + " " + currentEnumRow.description);
                break;

            case "bitfield":
                currentBitfield = new GeneratedEnumClasses.BitfieldElem();
                currentBitfield.name = StringUtils.fixName(attributes.getValue("name")); // name canonicalization C14N
                currentBitfield.size = attributes.getValue("size");
                currentBitfield.uid = attributes.getValue("uid");
                bitfields.add(currentBitfield);
                //maybeSysOut(attributes.getValue("xref"), "bitfieldrow uid " + currentBitfield.uid + " " + currentBitfield.name);
                break;

            case "bitfieldrow":
                if (currentBitfield == null)
                    break;
                currentBitfieldRow = new GeneratedEnumClasses.BitfieldRowElem();
                currentBitfieldRow.name = StringUtils.fixName(attributes.getValue("name")); // name canonicalization C14N
                currentBitfieldRow.description = attributes.getValue("description");
                if (currentBitfieldRow.description != null)
                    currentBitfieldRow.description = StringUtils.normalizeDescription(currentBitfieldRow.description);
                currentBitfieldRow.bitposition = attributes.getValue("bit_position");
                String len = attributes.getValue("length");
                if (len != null)
                    currentBitfieldRow.length = len;
                currentBitfieldRow.xrefclassuid = attributes.getValue("xref");
                currentBitfield.elems.add(currentBitfieldRow);
                //maybeSysOut(attributes.getValue("xref"), "bitfieldrow uid " + currentBitfield.uid + " " + currentBitfield.name + " " + currentBitfieldRow.name + " " + currentBitfieldRow.description);
                break;

            case "dict":
                currentDict = new GeneratedEnumClasses.DictionaryElem();
                currentDict.name = StringUtils.fixName(attributes.getValue("name")); // name canonicalization C14N
                currentDict.uid = attributes.getValue("uid");
                dictionaries.add(currentDict);
                //maybeSysOut(attributes.getValue("xref"), "dict uid " + currentDict.uid + " " + currentDict.name);
                break;

            case "dictrow":
                if (currentDict == null)
                    break;
                currentDictRow = new GeneratedEnumClasses.DictionaryRowElem();
                currentDictRow.value = attributes.getValue("value");
                currentDictRow.description = attributes.getValue("description");
                if (currentDictRow.description != null)
                    currentDictRow.description = StringUtils.normalizeDescription(currentDictRow.description);
                currentDict.elems.add(currentDictRow);
                //maybeSysOut(attributes.getValue("xref"), "dictrow uid" + currentDict.uid + " " + currentDict.name + " " + currentDictRow.value + " " + currentDictRow.description);
                break;

            case "revision":
                if (sisoSpecificationTitleDate == null) // assume first encountered is latest
                    sisoSpecificationTitleDate = attributes.getValue("title") + ", " + attributes.getValue("date");
                break;

            // fall throughs
            case "cot":
            case "cet":
            case "copyright":
            case "revisions":
            case "ebv":
            case "jammer_technique":
            case "jammer_kind":
            case "jammer_category":
            case "jammer_subcategory":
            case "jammer_specific":
            default:
                testElems.add(qName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
        switch (qName) {
            case "enum":
                if (currentEnum != null) {
                    enumWriter.writeOutEnum(currentEnum);
                }
                currentEnum = null;
                break;

            case "enumrow":
                currentEnumRow = null;
                break;

            case "bitfield":
                if (currentBitfield != null) {
                    bitfieldWriter.writeOutBitfield(currentBitfield);
                }
                currentBitfield = null;
                break;

            case "bitfieldrow":
                currentBitfieldRow = null;
                break;

            case "dict":
                if (currentDict != null)
                    dictionaryWriter.writeOutDictionary(currentDict);
                currentDict = null;
                break;

            case "dictrow":
                currentDictRow = null;
                break;
        }
    }
}