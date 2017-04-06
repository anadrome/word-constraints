/**
 * A type of Constraint that is a boolean constraint on top of other
 * constraints.
 *
 * @author Mark J. Nelson
 * @date 2008
 */

import java.util.List;

public abstract class BooleanConstraint
   extends Constraint
{
   /**
    * Get a list of the subsidiary constraints that this boolean constraint is
    * defined over.
    *
    * For example, the constraint (a OR b) would return (a, b).
    */
   public abstract List<Constraint> getConstraints();
}
