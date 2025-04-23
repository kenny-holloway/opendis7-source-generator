package edu.nps.moves.dis7.source.generator.enumerations;

import java.util.*;
// import java.util.Set;
import java.nio.CharBuffer;
import java.util.HashMap;

public final class StringUtils
{


    /** String constant */ public static final String JAVA = "java";
    /** String constant */ public static final String CPP = "cpp";
    /** String constant */ public static final String OBJC = "objc";
    /** String constant */ public static final String CSHARP = "csharp";
    /** String constant */ public static final String JAVASCRIPT = "javascript";
    /** String constant */ public static final String PYTHON = "python";

    private static Map<String, String> fileExtensions;
    static {
        fileExtensions = new HashMap<>();
        fileExtensions.put(JAVA, ".java");
        fileExtensions.put(CPP, ".cpp");
        fileExtensions.put(CSHARP, ".cs");
        fileExtensions.put(JAVASCRIPT, ".js");
        fileExtensions.put(PYTHON, ".py");
    }

    /**
    * Normalize string characters to create valid description
    * @param value of interest
    * @return normalized value
    */
    public static String normalizeDescription(String value)
    {
        String normalizedEntry = value.trim()
                                        .replaceAll("&", "&amp;").replaceAll("&amp;amp;", "&amp;")
                                        .replaceAll("\"", "'");
        return normalizedEntry;
    }

    /**
    * Replace special characters in name with underscore _ character
    * @param name name value (typically from XML)
    * @return normalized name
    */
    public final static String fixName(String name)
    {
        if ((name==null) || name.trim().isEmpty())
        {
            System.out.flush();
            System.err.println("fixName() found empty name... replaced with \"undefinedName\"");
            return "undefinedName";
        }
        // https://stackoverflow.com/questions/14080164/how-to-replace-a-string-in-java-which-contains-dot/14080194
        // https://stackoverflow.com/questions/2932392/java-how-to-replace-2-or-more-spaces-with-single-space-in-string-and-delete-lead
        String newName = name.trim().replaceAll(",", " ").replaceAll("—"," ").replaceAll("-", " ").replaceAll("\\."," ").replaceAll("&"," ")
                                    .replaceAll("/"," ").replaceAll("\"", " ").replaceAll("\'", " ").replaceAll("( )+"," ").replaceAll(" ", "_");
        newName = newName.replaceAll("_",""); // no underscore divider
        if (newName.contains("__"))
        {
            System.out.flush();
            System.err.println("fixname: " + newName);
            newName = newName.replaceAll("__", "_");
        }
        return newName;
    }

    public static String formatNamespaceStatement(String namespaceDeclaration, String languageToGenerate)
    {
        String namespaceStatement = "";

        String namespaceTokens[] = namespaceDeclaration.split("::");

        switch (languageToGenerate.toLowerCase())
        {
            case CPP:
                for (String s : namespaceTokens) {
                    // namespaceToken = String.format("namespace %s { ", s);
                    namespaceStatement = namespaceStatement + "namespace " + s + " {\n";
                }
                break;

            case CSHARP:
                namespaceStatement = "namespace " + namespaceDeclaration.replaceAll("::", ".") + "\n{";
                break;
        }

        return namespaceStatement;
    }

    public static String formatNamespaceEndStatement(String namespaceDeclaration, String languageToGenerate)
    {
        String namespaceStatement = "";
        String namespaceTokens[] = namespaceDeclaration.split("::");

        Collections.reverse(Arrays.asList(namespaceTokens));

        switch (languageToGenerate.toLowerCase())
        {
            case CPP:
                for (String s : namespaceTokens) {
                    // namespaceToken = String.format("namespace %s { ", s);
                    namespaceStatement = namespaceStatement + "} // end namespace " + s + "\n";
                }
                break;

            case CSHARP:
                namespaceStatement = "} // end namespace " +  namespaceDeclaration.replaceAll("::", ".") + "\n";
                break;
        }

        return namespaceStatement;
    }

    /**
    * Naming conventions for enumeration names
    * @param s enumeration string from XML data file
    * @return normalized name
    */
    public static String createEnumName(Set<String> enumNames, String s, Boolean... setUpperCase)
    {
        // For the cases where we don't set to all upper case
        String r = s.toLowerCase();

        if (setUpperCase.length == 0)
        {
            r = s.toUpperCase();
        }
        else
        {
            if (setUpperCase[0] == true) {
                r = s.toUpperCase();
            }
        }

        // Convert any of these chars to underbar (u2013 is a hyphen observed in source XML):
        r = r.replaceAll("[\\h-/,\";:\\u2013]", "_");

        // Remove any of these chars (u2019 is an apostrophe observed in source XML):
        r = r.replaceAll("[()}{}'.#&\\u2019]", "");

        // Special case the plus character:
        r = r.replace("+", "PLUS");

        // Collapse all contiguous underbars:
        r = r.replaceAll("_{2,}", "_");

        // If there's nothing there, put in something:
        if (r.isEmpty() || r.equals("_"))
            r = "undef";

        // Java identifier can't start with digit
        if (Character.isDigit(r.charAt(0)))
            r = "_" + r; // originally "$"

        // Handle multiply defined entries in the XML by appending a digit:
        String origR = r;
        int count = 2;
        while (enumNames.contains(r)) {
            r = origR + "_" + Integer.toString(count++);
        }
        enumNames.add(r);

        return r;
    }

    /**
         * Naming conventions for enumeration names
         * @param s enumeration string from XML data file
         * @return normalized name
         */
        public static String createEnumName(String s, Boolean... setUpperCase)
        {
            // For the cases where we don't set to all upper case
            String r = s.toLowerCase();

            if (setUpperCase.length == 0)
            {
                r = s.toUpperCase();
            }
            else
            {
                if (setUpperCase[0] == true) {
                    r = s.toUpperCase();
                }
            }

            // Convert any of these chars to underbar (u2013 is a hyphen observed in source XML):
            r = r.replaceAll("[\\h-/,\";:\\u2013]", "_");

            // Remove any of these chars (u2019 is an apostrophe observed in source XML):
            r = r.replaceAll("[()}{}'.#&\\u2019]", "");

            // Special case the plus character:
            r = r.replace("+", "PLUS");

            // Collapse all contiguous underbars:
            r = r.replaceAll("_{2,}", "_");

            // If there's nothing there, put in something:
            if (r.isEmpty() || r.equals("_"))
                r = "undef";

            // Java identifier can't start with digit
            if (Character.isDigit(r.charAt(0)))
                r = "_" + r; // originally "$"

            return r;
        }

    /**
    * Normalize string characters to create valid Java name.  Note that unmangled name typically remains available in the description
    * @param value of interest
    * @return normalized value
    */
    public static String normalizeToken(String value)
    {
        String normalizedEntry = value.trim()
                                        .replaceAll("\"", "").replaceAll("\'", "")
                                        .replaceAll("—","-").replaceAll("–","-") // mdash
                // 
                                        .replaceAll("\\*","x").replaceAll("/","") // escaped regex for multiply, divide
                                        .replaceAll("&", "&amp;").replaceAll("&amp;amp;", "&amp;");
        if (!normalizedEntry.isEmpty() && Character.isDigit(normalizedEntry.toCharArray()[0]))
                normalizedEntry = '_' + normalizedEntry;
        if (!value.equals(normalizedEntry) && !normalizedEntry.equals(value.trim()))
            System.out.println ("*** normalize " + "\n" + 
                                "'" + value + "' to\n" + 
                                "'" + normalizedEntry + "'");
        return normalizedEntry;
    }

    /** cleanup special characters in string
     *  @param s input string
     *  @return output string
     */
    public final static String htmlize(String s)
    {
        return s.replace("&","and").replace("&","and");
    }

    // String valueIndent = new String(new char[16]).replace('\0', ' ');
    public static String indent(int numSpaces)
    {
        return CharBuffer.allocate(numSpaces).toString().replace('\0', ' ');
    }

    public static String tabs(int numTabs)
    {
        int numSpaces = 4 * numTabs;
        return indent(numSpaces);
    }

    public static String firstCharLower(String s)
    {
        char c[] = s.toCharArray();
        // c[0] +=32;
        c[0] = Character.toLowerCase(c[0]);
        String ret = new String(c);
        return ret;
    }

    public static String firstCharUpper(String s)
    {
        char c[] = s.toCharArray();
        c[0] = Character.toUpperCase(c[0]);
        String ret = new String(c);
        return ret;
    }

    public static void stringBuilderRemoveLastChar(StringBuilder sb)
    {
        // If the string is larger than one remove last character
        sb.setLength(Math.max(sb.length() - 1, 0));
        // if (sb.length() > 0) {
        //     sb.setLength(sb.length() - 1);
        // }
    }

    /** For an enumeration statement "DisPduType.ACKNOWLEDGE", return the enum value lowercase
     *  DisPduType.acknowledge
     *  @param s input string
     *  @return output string
     */
    public static String setEnumValueToLowerCase(String enumStatement)
    {
        String enumOutput = enumStatement;

        String[] enumTokens = enumStatement.split("\\.");
        if (enumTokens.length == 2)
        {
            enumOutput = enumTokens[0] + "." + enumTokens[1].toLowerCase();
        }

        return enumOutput;
    }

    /** For an enumerated variable, get the type.  DisPduType.action_request returns DisPduType
     *  @param variableValue input string
     *  @return output string or null
     */
    public static String getEnumType(String enumVariable)
    {
        String enumType = null;

        if (enumVariable.contains("."))
        {
            String[] valueTokens = enumVariable.split("\\.");
            enumType = valueTokens[0];
        }

        return enumType;
    }

    /** For an enumerated variable, get the value.  DisPduType.action_request returns action_request
     *  @param variableValue input string
     *  @return output string or null
     */
    public static String getEnumValue(String enumVariable)
    {
        String enumValue = null;

        if (enumVariable.contains("."))
        {
            String[] valueTokens = enumVariable.split("\\.");
            enumValue = valueTokens[1];
        }

        return enumValue;
    }

    public static boolean isEnumType(String variable)
    {
        return variable.contains(".") ? true : false;
    }

    public static String getSourceFileExtension(String language)
    {
        return fileExtensions.get(language);
    }

    // remove the first word and whitespace at front of string
    // example : "new LandPlatformCapability" becomes "LandPlatformCapability"
    // Optional delimiter use:
    // removeFirstWord(enumNamespace, ".");
    // dis.siso_ref_010.enums becomse siso_ref_010.enums
    public static String removeFirstWord(String aString, String... delimiter)
    {
        if (aString == null || aString.length() == 0) return null;

        String [] nameArray;
        if (delimiter.length ==0)
            nameArray = aString.split(" ", 2);
        else
            nameArray = aString.split(delimiter[0], 2);

        return nameArray[1];
    }

    /** Convert a given CamelCase string to lower case underscore camel_case
     *  @param value input string
     *  @return output string or null
     */
    public static String camelCasetoLowerUnderscore(String value)
    {
        if(value == null) return value;
        String regex = "([a-z])([A-Z])";
        String replacement = "$1_$2";
        return value.replaceAll(regex, replacement).toLowerCase();
    }

}
