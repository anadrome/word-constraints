/**
 * Command-line constraint-solver interface.
 *
 * Specify a constraint file and maximum number of solutions, and it outputs
 * them to the standard output.
 *
 * @author Mark J. Nelson
 * @date   2008
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class solver
{
   public static void main(String args[])
   {
      try
      {
         if (args.length != 2)
         {
            System.err.println("Usage: java -Xmx300m -jar solver.jar constraintFilename maxSolutions");
            return;
         }

         ConstraintSolver solver = new ConstraintSolver(args[0]);

         List<Map<Variable, String>> assignments = solver.generate(Integer.valueOf(args[1]));

         for (Map<Variable, String> assignment : assignments)
         {
            for (Map.Entry<Variable, String> e : assignment.entrySet())
               System.out.println(e.getKey().name + ": " + e.getValue());
            System.out.println("---");
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}
