/**
 * A constraint requiring a ConceptNet relation between two terms, either or
 * both of which may be a variable.
 *
 * Can also do "inheritance" via WordNet to expand the coverage.
 *
 * @author Mark J. Nelson
 * @date   2007-2008
 */

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class ConceptNetConstraint
   extends Constraint
{
   private String type;
   private Variable source, target;
   private String sourceLiteral, targetLiteral;
   private boolean[] inheritance = new boolean[4];

   private ConceptNet conceptNet = GlobalData.getInstance().conceptNet;

   private Set<Variable> relVars = new HashSet<>();

   /**
    * A constraint between two variables.
    *
    * The inheritance parameter specifies whether WordNet "inheritance" will be
    * done on, respectively: hypernyms of source, hyponyms of source, hypernyms
    * of target, and hyponyms of target. (See ConceptNet.java for details.)
    */
   public ConceptNetConstraint(String type_, Variable source_,
         Variable target_, boolean[] inheritance_)
   {
      type = type_;
      source = source_;
      sourceLiteral = null;
      target = target_;
      targetLiteral = null;
      for (int i = 0; i < 4; ++i)
         inheritance[i] = inheritance_[i];
      relVars.add(source);
      relVars.add(target);
   }

   /**
    * A constraint between a source variable and a target literal string.
    */
   public ConceptNetConstraint(String type_, Variable source_,
         String target_, boolean[] inheritance_)
   {
      type = type_;
      source = source_;
      sourceLiteral = null;
      target = null;
      targetLiteral = target_;
      for (int i = 0; i < 4; ++i)
         inheritance[i] = inheritance_[i];
      relVars.add(source);
   }

   /**
    * A constraint between a source literal string and a target variable.
    */
   public ConceptNetConstraint(String type_, String source_,
         Variable target_, boolean[] inheritance_)
   {
      type = type_;
      source = null;
      sourceLiteral = source_;
      target = target_;
      targetLiteral = null;
      inheritance = inheritance_;
      for (int i = 0; i < 4; ++i)
         inheritance[i] = inheritance_[i];
      relVars.add(target);
   }

   public boolean check(Map<Variable, String> assignment)
   {
      final String sourceString = source == null ? sourceLiteral : assignment.get(source);
      final String targetString = target == null ? targetLiteral : assignment.get(target);
      // if we're checking a constraint where one of the variables hasn't been
      // assigned yet, it passes for now
      if (sourceString == null || targetString == null)
         return true;
      return conceptNet.linkExists(type, sourceString, targetString, inheritance);
   }

   public String howSatisfied(Variable var, Map<Variable, String> assignment)
   {
      final String sourceString = source == null ? sourceLiteral : assignment.get(source);
      final String targetString = target == null ? targetLiteral : assignment.get(target);
      // we should only get complete assignments
      assert sourceString != null && targetString != null;

      String[] trace = new String[1];
      if (conceptNet.linkExists(type, sourceString, targetString, inheritance, trace))
         return trace[0];
      return "Not satisfied";
   }

   public Set<Variable> relevantVars()
   {
      return relVars;
   }

   public String toString()
   {
      return "ConceptNet: " + type;
   }

   public String fullString()
   {
      // 7 params: type, source, target, and 4 inheritance booleans
      return "(ConceptNet " +
         type + " " +
         (source == null ? "\"" + sourceLiteral + "\"" : "?" + source.name) + " " +
         (target == null ? "\"" + targetLiteral + "\"" : "?" + target.name) + " " +
         inheritance[0] + " " +
         inheritance[1] + " " +
         inheritance[2] + " " +
         inheritance[3] +
         ")";
   }
}
