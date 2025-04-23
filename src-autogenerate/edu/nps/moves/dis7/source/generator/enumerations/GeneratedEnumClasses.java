/**
 * Copyright (c) 2008-2022, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */
package edu.nps.moves.dis7.source.generator.enumerations;

import java.util.*;

/**
 * Used as part of the code generator to represenent one generated enum
 */

public class GeneratedEnumClasses
{
    public static class EnumRowElem
    {
        String value;
        String description;
        String footnote;
        String xrefclassuid;
    }

    public static class EnumElem
    {
        String uid;
        String name;
        String size;
        String footnote;
        List<EnumRowElem> elems = new ArrayList<>();
    }

    public static class BitfieldRowElem
    {
        String name;
        String bitposition;
        String length = "1"; // default
        String description;
        String xrefclassuid;
    }

    public static class BitfieldElem
    {
        String name;
        String size;
        String uid;
        List<BitfieldRowElem> elems = new ArrayList<>();
    }

    public static class DictionaryRowElem
    {
        String value;
        String description;
    }

    public static class DictionaryElem
    {
        String name;
        String uid;
        List<DictionaryRowElem> elems = new ArrayList<>();
    }
}

