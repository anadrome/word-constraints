/**
 * A disjunction between other constraints.
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

public class OrConstraint
   extends BooleanConstraint
{
   private List<Constraint> constraints;
   private Set<Variable> relVars = new HashSet<Variable>();

   public OrConstraint(Collection<Constraint> constraints)
   {
      this.constraints = new ArrayList(constraints);

      for (Constraint c : constraints)
         relVars.addAll(c.relevantVars());
   }

   public boolean check(Map<Variable, String> assignment)
   {
      for (Constraint c : constraints)
         if (c.check(assignment))
            return true;
      return false;
   }

   public String howSatisfied(Variable var, Map<Variable, String> assignment)
   {
      return "OR: FIXME with something useful here";
   }

   public Set<Variable> relevantVars()
   {
      return relVars;
   }

   public String toString()
   {
      return "OR";
   }

   public String fullString()
   {
      String ret = "(OR";
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
