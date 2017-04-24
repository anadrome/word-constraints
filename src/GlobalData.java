/**
  * Singleton class to hold all our global data (eg ConceptNet instance).
  *
  * @author Mark J. Nelson
  * @date   2007-2008
  */

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

public class GlobalData
{
   /* instantiate at class-loading time */
   private static GlobalData instance = new GlobalData();
   
   public ConceptNet conceptNet;
   public WordNet wordNet;

   private GlobalData()
   {
      System.err.println("Loading WordNet...");
      System.err.flush();
      try
      {
         wordNet = new WordNet();
      }
      catch (IOException e)
      {
         throw new RuntimeException("Loading WordNet failed");
      }
      System.err.println("...done.\n");

      System.err.println("Loading ConceptNet...");
      System.err.flush();
      try
      {
         conceptNet = new ConceptNet();
      }
      catch (IOException e)
      {
         throw new RuntimeException("Loading ConceptNet failed");
      }
      System.err.println("...done.\n");
   }

   public static GlobalData getInstance()
   {
      return instance;
   }
}
