/**
 * A conjunction between other constraints.
 *
 * @author Mark J. Nelson
 * @date 2008
 */

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class AndConstraint
   extends BooleanConstraint
{
   private List<Constraint> constraints;
   private Set<Variable> relVars = new HashSet<>();

   public AndConstraint(Collection<Constraint> constraints)
   {
      this.constraints = new ArrayList(constraints);

      for (Constraint c : constraints)
         relVars.addAll(c.relevantVars());
   }

   public boolean check(Map<Variable, String> assignment)
   {
      for (Constraint c : constraints)
         if (!c.check(assignment))
            return false;
      return true;
   }

   public String howSatisfied(Variable var, Map<Variable, String> assignment)
   {
      String result = "AND: ";
      for (Constraint c : constraints)
         result += "(" + c.howSatisfied(var, assignment) + ") ";
      return result;
   }

   public Set<Variable> relevantVars()
   {
      return relVars;
   }

   public String toString()
   {
      return "AND";
   }

   public String fullString()
   {
      String ret = "(AND";
      for (Constraint c : constraints)
         ret += " " + c.getID();
      ret += ")";
      return ret;
   }

   public List<Constraint> getConstraints()
   {
      return Collections.unmodifiableList(constraints);
   }
}
