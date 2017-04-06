/**
 * Basically a static method to parse the textual constraint specifications in
 * a datafile and create a corresponding Constraint object.
 *
 * @author Mark J. Nelson
 * @date   2007-2008
 */

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ConstraintFactory
{
   /**
    * Create a constraint from a string.
    *
    * This function resolves ?X notation to Variable instances, and also
    * (recursively) resolves nested constraints (anything starting with a
    * parenthesis) to the new nested Constraint instances, and then calls
    * the appropriate constructor via the newConstraint() dispatcher.
    */
   public static Constraint parseConstraint(String s, ConstraintSolver solver)
   {
      // kind of a mess of recursive-descent parsing and interleaved lexing

      // opening '('
      int pos = 0;
      if (s.charAt(pos++) != '(')
         throw new RuntimeException("Failed to find opening '(' in constraint: " + s);

      // constraint name
      int nextPos = s.indexOf(' ', pos);
      if (nextPos == -1)
         nextPos = s.indexOf(')', pos);
      final String constraintName = s.substring(pos, nextPos);
      List<Object> args = new ArrayList<Object>();
      
      pos = nextPos;
      // either a space if (more) args come next, or a ')' if no (more) args
      while (s.charAt(pos) == ' ')
      {
         ++pos;
         if (s.charAt(pos) == '(') // nested constraint
         {
            // find the end of the constraint, which is the next
            // non-double-quote-protected ')'
            int constraintEnd = pos;
            boolean inQuotes = false;
            for (int i = pos+1; i < s.length(); ++i)
            {
               final char c = s.charAt(i);
               if (c == '"')
               {
                  inQuotes = !inQuotes;
               }
               else if (!inQuotes && c == ')')
               {
                  constraintEnd = i;
                  break;
               }
            }
            args.add(parseConstraint(s.substring(pos, constraintEnd+1), solver));
            pos = constraintEnd+1;
         }
         else if (s.charAt(pos) == '?') // variable
         {
            nextPos = s.indexOf(' ', pos);
            if (nextPos == -1)
               nextPos = s.indexOf(')', pos);
            final String varName = s.substring(pos+1, nextPos);
            final Variable var = solver.getVariable(varName);
            if (var == null)
               throw new RuntimeException("Constraint referenced unknown variable '?" + varName + "': " + s);
            args.add(var);
            pos = nextPos;
         }
         else if (s.charAt(pos) == '"') // double-quoted string
         {
            nextPos = s.indexOf('"', pos+1);
            args.add(s.substring(pos+1, nextPos));
            pos = nextPos+1;
         }
         else // bare string
         {
            nextPos = s.indexOf(' ', pos);
            if (nextPos == -1)
               nextPos = s.indexOf(')', pos);
            args.add(s.substring(pos, nextPos));
            pos = nextPos;
         }
      }

      return newConstraint(constraintName, args, solver);
   }

   /**
    * Constraint-creation dispatcher.
    *
    * Map a name and params to the right constructor calls.
    */
   private static Constraint newConstraint(String name, List<Object> args, ConstraintSolver solver)
   {
      if (name.equals("ConceptNet"))
      {
         // 7 params: String, String|Variable, String|Variable, String, String, String, String
         // last 4 strings are really booleans
         final String type = (String) args.get(0);
         final boolean[] inheritance = new boolean[] {
            Boolean.valueOf((String)args.get(3)).booleanValue(),
            Boolean.valueOf((String)args.get(4)).booleanValue(),
            Boolean.valueOf((String)args.get(5)).booleanValue(),
            Boolean.valueOf((String)args.get(6)).booleanValue() };
         final boolean set1 = args.get(1) instanceof Variable;
         final boolean set2 = args.get(2) instanceof Variable;
         if (set1 && set2)
            return new ConceptNetConstraint(type,
                  (Variable) args.get(1), (Variable) args.get(2), inheritance);
         else if (set1 && !set2)
            return new ConceptNetConstraint(type,
                  (Variable) args.get(1), (String) args.get(2), inheritance);
         else if (!set1 && set2)
            return new ConceptNetConstraint(type,
                  (String) args.get(1), (Variable) args.get(2), inheritance);
         else
            throw new RuntimeException("A ConceptNet constraint must involve at least one variable");
      }
      else if (name.equals("WordNet"))
      {
         // 3 params: type, source, and target
         // tpyes: String String|Variable String|Variable
         final boolean hypernym = ((String) args.get(0)).equals("hypernym");
         final boolean set1 = args.get(1) instanceof Variable;
         final boolean set2 = args.get(2) instanceof Variable;
         if (set1 && set2)
            return new WordNetConstraint((Variable) args.get(1), (Variable) args.get(2), hypernym);
         else if (set1 && !set2)
            return new WordNetConstraint((Variable) args.get(1), (String) args.get(2), hypernym);
         else if (!set1 && set2)
            return new WordNetConstraint((String) args.get(1), (Variable) args.get(2), hypernym);
         else
            throw new RuntimeException("A WordNet constraint must involve at least one variable");
      }
      else if (name.equals("AND") || name.equals("OR") || name.equals("NOT"))
      {
         // params are all subsumed constraint IDs, so substitute references
         List<Constraint> constraints = new ArrayList<Constraint>();
         for (Object arg : args)
         {
            final int id = Integer.valueOf((String)arg);
            final Constraint constraint = solver.getConstraint(id);
            if (constraint == null)
               throw new RuntimeException("Invalid constraint ID: " + id);
            constraints.add(constraint);
         }
         
         if (name.equals("AND"))
            return new AndConstraint(constraints);
         else if (name.equals("OR"))
            return new OrConstraint(constraints);
         // else, name.equals("NOT")
         return new NotConstraint(constraints.get(0));
      }
      else
      {
         throw new RuntimeException("Unknown constraint type: " + name);
      }
   }
}
