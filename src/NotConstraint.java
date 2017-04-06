/**
 * A negation of another constraint.
 *
 * @author Mark J. Nelson
 * @date 2008
 */

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;

public class NotConstraint
   extends BooleanConstraint
{
   private Constraint constraint;

   public NotConstraint(Constraint constraint)
   {
      this.constraint = constraint;
   }

   public boolean check(Map<Variable, String> assignment)
   {
      return !constraint.check(assignment);
   }

   public String howSatisfied(Variable var, Map<Variable, String> assignment)
   {
      return "NOT: FIXME with something useful here";
   }

   public Set<Variable> relevantVars()
   {
      return constraint.relevantVars();
   }

   public String toString()
   {
      return "NOT";
   }

   public String fullString()
   {
      return "(NOT " + constraint.getID() + ")";
   }

   public List<Constraint> getConstraints()
   {
      return Arrays.asList(constraint);
   }
}
