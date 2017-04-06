/**
  * Generates and represents a high-level type of game (Dodger, Shooter, etc.).
  *
  * @author Mark J. Nelson
  * @date   January 2007
  */
public abstract class GameType
{
   /** The maximum hops we care to search. */
   protected static final int MAX_HOPS = 5;

   /** Some game info (ConceptNet reference, etc.). */
   protected GlobalData data = GlobalData.getInstance();
   
   /**
     * Generate up to num games from the given verb and noun.
     */
   public abstract void generate(String verb, String noun, int num);
}
