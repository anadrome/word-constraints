/**
 * A constraint requiring a WordNet relation between two terms, either or both
 * of which may be a variable.
 *
 * TODO: we currently assume the two are the same POS, which keeps us from
 * having to explicitly POS-tag literals. Either remove this restriction if
 * we want more types than hypernym/hyponym, or enforce it.
 *
 * TODO: currently assume everything is either a noun or verb
 *
 * @author Mark J. Nelson
 * @date 2008
 */

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import edu.mit.jwi.item.POS;

public class WordNetConstraint
   extends Constraint
{
   private Variable source, target;
   private String sourceLiteral, targetLiteral;
   private boolean hypernym; // hypernym if true; hyponym otherwise

   private WordNet wordNet = GlobalData.getInstance().wordNet;

   private Set<Variable> relVars = new HashSet<Variable>();

   /**
    * A constraint between two variables.
    *
    * @param hypernym True if this is a hypernym constraint; false if hyponym.
    */
   public WordNetConstraint(Variable source, Variable target,
         boolean hypernym)
   {
      this.source = source;
      sourceLiteral = null;
      this.target = target;
      targetLiteral = null;
      this.hypernym = hypernym;

      relVars.add(source);
      relVars.add(target);
   }

   /**
    * A constraint between a source variable and a target literal string.
    */
   public WordNetConstraint(Variable source, String target, boolean hypernym)
   {
      this.source = source;
      sourceLiteral = null;
      this.target = null;
      targetLiteral = target;
      this.hypernym = hypernym;

      relVars.add(source);
   }

   /**
    * A constraint between a source literal string and a target variable.
    */
   public WordNetConstraint(String source, Variable target, boolean hypernym)
   {
      this.source = null;
      sourceLiteral = source;
      this.target = target;
      targetLiteral = null;
      this.hypernym = hypernym;

      relVars.add(target);
   }

   public boolean check(Map<Variable, String> assignment)
   {
      final String sourceString = source == null ? sourceLiteral : assignment.get(source);
      final String targetString = target == null ? targetLiteral : assignment.get(target);

      String posString = source != null ? source.type : target.type;
      POS pos = posString.equals("noun") ? POS.NOUN : POS.VERB;
      // if we're checking a constraint where one of the variables hasn't been
      // assigned yet, it passes for now
      if (sourceString == null || targetString == null)
         return true;
      if (hypernym)
         return wordNet.isHypernym(pos, sourceString, pos, targetString);
      // hyponym otherwise, which is just the reverse
      return wordNet.isHypernym(pos, targetString, pos, sourceString);
   }

   public String howSatisfied(Variable var, Map<Variable, String> assignment)
   {
      final String sourceString = source == null ? sourceLiteral : assignment.get(source);
      final String targetString = target == null ? targetLiteral : assignment.get(target);
      // we should only get complete assignments
      assert sourceString != null && targetString != null;

      return "FIXME: something here";
   }

   public Set<Variable> relevantVars()
   {
      return relVars;
   }

   public String toString()
   {
      return "WordNet: " + (hypernym ? "generalizationOf" : "specializationOf");
   }

   public String fullString()
   {
      // 3 params: type, source, and target
      return "(WordNet " +
         (hypernym ? "hypernym" : "hyponym") + " " +
         (source == null ? "\"" + sourceLiteral + "\"" : "?" + source.name) + " " +
         (target == null ? "\"" + targetLiteral + "\"" : "?" + target.name) + 
         ")";
   }
}

