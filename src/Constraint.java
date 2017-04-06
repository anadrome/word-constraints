/**
 * A generic constraint.
 *
 * @author Mark J. Nelson
 * @date   2007-2008
 */

import java.util.Map;
import java.util.Set;

public abstract class Constraint
{
   /**
    * A unique identifier.
    *
    * Used as a name of sorts for some saving/loading stuff.
    */
   private int id;

   /**
    * Set the unique constraint ID.
    */
   public void setID(int id)
   {
      this.id = id;
   }

   /**
    * Get the unique constraint ID.
    */
   public int getID()
   {
      return id;
   }

   /**
    * Given a (possibly partial) variable assignment, check whether it
    * might satisfy this constraint.
    *
    * Return false if the constraint is definitely not satisfied, given the
    * assignment so far. Return true otherwise, i.e. either when the constraint
    * is definitely satisfied, or when it might or might not be depending on
    * the values assigned to some of the variables not yet in the assignment.
    */
   public abstract boolean check(Map<Variable, String> assignment);

   /**
    * Return a string explaining (through a trace of relevant values) how a
    * particular variable's assignment satisfies this constraint, possibly 
    * also in light of the other assignments.
    */
   public abstract String howSatisfied(Variable var, Map<Variable, String> assignment);

   /**
    * Return the set of variables whose assignment might impact this
    * constraint's satisfaction.
    */
   public abstract Set<Variable> relevantVars();

   /**
    * Get a short description suitable for display.
    *
    * May omit some details about the constraint, and shouldn't include
    * information about the involved variables, since those will be represented
    * separately in a graph view. For a full output suitable for
    * reconstruction, use fullString().
    */
   public abstract String toString();

   /**
    * Get a full string representation suitable for reconstruction.
    *
    * Each implementation should be kept in sync with the corresponding section
    * of ConstraintFactory's parseConstraint() and newConstraint().  Yes,
    * that's poor encapsulation, but it seemed like the least horrible way to
    * do it.
    */
   public abstract String fullString();
}
