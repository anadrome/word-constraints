/**
 * A variable.
 *
 * @author Mark J. Nelson
 * @date   2007-2008
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public class Variable
{
   public String name;
   public String type;

   /** Possible values this variable can take on.
    *
    * Can be null, in which case the solver should use a default set of
    * possible values for this type.
    */
   public List<String> values;
   
   /**
    * Instantiate the variable, with its possible values defaulting to the
    * possible values for its type, as managed by ConstraintSolver.
    */
   public Variable(String name_, String type_)
   {
      name = name_;
      type = type_;
      values = null;
   }

   /**
    * Instantiate the variable with a list of the possible values it can take on.
    */
   public Variable(String name_, String type_, Collection<String> values_)
   {
      name = name_;
      type = type_;
      values = new ArrayList(values_);
   }

   public String toString()
   {
      return name;
   }
}
