package edu.nps.moves.dis7.source.generator.entityTypes;

import java.io.*;
import java.util.*;

public abstract class AbstractObjectTypesGenerator
{
   protected File outputDirectory;

   protected  static String outputDirectoryPath = "src-generated/java/edu/nps/moves/dis7/enumerations"; // default
   protected  static String packageName =         "edu.nps.moves.dis7.enumerations"; // default
   protected  static String language    = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_PROGRAMMING_LANGUAGE;
   protected  static String sisoXmlFile = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_SISO_XML_FILE;

   public AbstractObjectTypesGenerator()
   {
   }
}
