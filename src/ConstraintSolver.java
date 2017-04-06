/**
 * Solve a set of constraints, using a fairly naive brute-force method.
 *
 * @author Mark J. Nelson
 * @date   2007-2008
 */

// implementation note: the original Variable and Constraint objects are
// kept by reference, not copied. this both allows add/remove to refer
// to them by reference, and allows the possible values of a Variable
// to be changed elsewhere without removing it and re-adding it

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class ConstraintSolver
{
   // the variables
   private List<Variable> vars;
   // the constraints
   private List<Constraint> constraints;

   // default possible values per type
   private Map<String, List<String>> defaultValues = new HashMap<String, List<String>>();

   // mapping of constraints that are subsumed by other (boolean) constraints
   // to a set of the constraints they're currently subsumed by
   private Map<Constraint, Set<BooleanConstraint>> subsumedConstraints
      = new HashMap<Constraint, Set<BooleanConstraint>>();

   // a mapping of variables to the constraints that they affect, so we just
   // check relevant constraints after assignment
   private Map<Variable, List<Constraint>> varsToConstraints = new HashMap<Variable, List<Constraint>>();

   // mapping of names to variables
   private Map<String, Variable> namesToVars = new HashMap<String, Variable>();
   // mapping of constraint IDs to constraints
   private Map<Integer, Constraint> idsToConstraints = new HashMap<Integer, Constraint>();

   // constraint ID counter-- higher than any existing constraint, so next one
   // gets assigned this ID
   private int constraintIdCounter = 0;

   // internal state during a generation run:
   
   // the variable assignment
   private Map<Variable, String> assignment = new HashMap<Variable, String>();
   // the set of assignments
   private List<Map<Variable, String>> assignments = new ArrayList<Map<Variable, String>>();
   // the number of assignments
   private int assignmentCounter = 0;

   public ConstraintSolver()
   {
      vars = new ArrayList<Variable>();
      constraints = new ArrayList<Constraint>();
   }

   /**
    * Instantiate a constraint solver from lists of variables and constraints.
    *
    * @param vars_        A list of the variables to assign.
    * @param constraints_ A list of constraints that an assignment must respect.
    */
   public ConstraintSolver(List<Variable> vars_, List<Constraint> constraints_)
   {
      vars = new ArrayList<Variable>(vars_);
      constraints = new ArrayList<Constraint>(constraints_);

      for (Variable var : vars)
         varsToConstraints.put(var, new ArrayList<Constraint>());

      for (Constraint constraint : constraints)
      {
         final Set<Variable> relevantVars = constraint.relevantVars();
         for (Variable var : relevantVars)
            varsToConstraints.get(var).add(constraint);
         constraint.setID(constraintIdCounter);
         idsToConstraints.put(constraintIdCounter, constraint);
         ++constraintIdCounter;
      }
   }

   /**
    * Instantiate a constraint solver from a specification file.
    *
    * Lines are of one of these forms:
    *    defaultVals type: val1, val2, val3
    *    type varWithDefaultPossibleValues
    *    type varWithExplicitList: val1, val2, val3
    *    constraint id: (ConstraintType arg1 arg2 "quoted arg3" ...)
    * Variables must be declared before any constraints that reference them.
    * Anything starting a line other than the words 'defaultVals' or
    * 'constraint' is a type for a variable declaration.
    */
   public ConstraintSolver(String filename)
      throws IOException
   {
      vars = new ArrayList<Variable>();
      constraints = new ArrayList<Constraint>();
      final BufferedReader file = new BufferedReader(new FileReader(filename));
      for (String s = file.readLine(); s != null; s = file.readLine())
      {
         if (s.startsWith("defaultVals "))
         {
            int pos = 12;
            int nextPos = s.indexOf(':', pos);
            final String type = s.substring(pos, nextPos);
            pos = nextPos+2;
            final String[] values = s.substring(pos).split(", ?", 0);
            setDefaultValues(type, Arrays.asList(values));
         }
         else if (s.startsWith("constraint "))
         {
            int pos = 11;
            int nextPos = s.indexOf(':', pos);
            final int constraintId = Integer.valueOf(s.substring(pos, nextPos));
            pos = nextPos + 2;
            final Constraint constraint = ConstraintFactory.parseConstraint(s.substring(pos), this);
            addConstraint(constraint, constraintId);
         }
         else if (s.indexOf(':') == -1) // variable with default values
         {
            int spacePos = s.indexOf(' ');
            final String type = s.substring(0, spacePos);
            final String name = s.substring(spacePos+1);
            final Variable var = new Variable(name, type);
            addVariable(var);
         }
         else // variable with explicit value list
         {
            int spacePos = s.indexOf(' ');
            int colonPos = s.indexOf(':');
            final String type = s.substring(0, spacePos);
            final String name = s.substring(spacePos+1, colonPos);
            final String[] values = s.substring(colonPos+2).split(", ?", 0);
            final Variable var = new Variable(name, type, Arrays.asList(values));
            addVariable(var);
         }
      }
   }

   /**
    * Save the constraint-space definition to a file.
    *
    * Save all the variables and constraints to a file. The file format is such
    * that it can be reloaded by the ConstraintSolver(filename) constructor
    * (see comments there).
    */
   public void save(String filename)
      throws IOException
   {
      final BufferedWriter file = new BufferedWriter(new FileWriter(filename));

      // output the default values per type
      for (Map.Entry<String, List<String>> defaultVals : defaultValues.entrySet())
      {
         file.write("defaultVals " + defaultVals.getKey() + ": ");
         outputStringList(file, defaultVals.getValue());
         file.newLine();
      }

      // output the variables
      for (Variable var : vars)
      {
         if (var.values != null)
         {
            file.write(var.type + " " + var.name + ": ");
            outputStringList(file, var.values);
         }
         else
         {
            file.write(var.type + " " + var.name);
         }
         file.newLine();
      }

      // output the constraints
      for (Constraint c : constraints)
      {
         // recursively output any subsumed constraints this constraint depends
         // on first, since the save format refers to them by ID (so they need
         // to be loaded first)
         //
         // TODO: this might output the same subsumed constraint multiple
         // times, which is ok since they have unique IDs and the loader will
         // skip them, but eventually should probably be cleaned up.
         //
         // Also TODO: if we want the save format to be easily human
         // interpretable, we probably want to handle this more intelligently.
         // If a subsumed constraint is only referenced once, it should be
         // embedded in its parent constraint, e.g. (AND (ConceptNet ...)
         // (ConceptNet ...)) instead of defining the two constraint separately
         // then (AND id1 id2). If it's referenced multiple times we do need to
         // define it separately, but should still mark it as a subsumed
         // constraint to make clear that it's just being defined for reference
         // by other constraints, but is not in itself an active top-level
         // constraint. As it is now reading the save file is confusing if you
         // don't know how the loading works.
         if (c instanceof BooleanConstraint)
            saveSubsumed((BooleanConstraint)c, file);

         file.write("constraint ");
         file.write(Integer.toString(c.getID()));
         file.write(": ");
         file.write(c.fullString());
         file.newLine();
      }

      file.close();
   }
   // helper function to recursively output constraints subsumed by the specified constraint
   private void saveSubsumed(BooleanConstraint constraint, BufferedWriter writer)
      throws IOException
   {
      final List<Constraint> subsumed = constraint.getConstraints();
      for (Constraint s : subsumed)
      {
         if (s instanceof BooleanConstraint)
         {
            saveSubsumed((BooleanConstraint)s, writer);
         }

         writer.write("constraint ");
         writer.write(Integer.toString(s.getID()));
         writer.write(": ");
         writer.write(s.fullString());
         writer.newLine();
      }
   }

   /**
    * Get a list of the variables.
    */
   public List<Variable> getVariables()
   {
      return Collections.unmodifiableList(vars);
   }

   /**
    * Get a list of the constraints.
    */
   public List<Constraint> getConstraints()
   {
      return Collections.unmodifiableList(constraints);
   }

   /**
    * Get a variable by name.
    *
    * Returns null if no such variable.
    */
   public Variable getVariable(String name)
   {
      return namesToVars.get(name);
   }

   /**
    * Get a constraint by ID.
    *
    * Returns null if no such constraint.
    */
   public Constraint getConstraint(int id)
   {
      return idsToConstraints.get(id);
   }

   /**
    * Add a variable.
    */
   public void addVariable(Variable var)
   {
      vars.add(var);
      varsToConstraints.put(var, new ArrayList<Constraint>());
      namesToVars.put(var.name, var);
   }

   /**
    * Remove a variable, along with all constraints to which it's relevant.
    *
    * @return A list of the constraints that were also removed.
    */
   public List<Constraint> removeVariable(Variable var)
   {
      vars.remove(var);
      final List<Constraint> removedConstraints = varsToConstraints.get(var);
      varsToConstraints.remove(var);
      for (Constraint removedConstraint : removedConstraints)
         constraints.remove(removedConstraint);
      namesToVars.remove(var.name);
      return removedConstraints;
   }

   /**
    * Add a constraint.
    *
    * Also gives it an ID (this class is responsible for all constraint-ID
    * management).
    */
   public void addConstraint(Constraint constraint)
   {
      System.err.println("Adding constraint with id: " + constraintIdCounter);
      constraints.add(constraint);
      final Set<Variable> relevantVars = constraint.relevantVars();
      for (Variable var : relevantVars)
         varsToConstraints.get(var).add(constraint);
      constraint.setID(constraintIdCounter);
      idsToConstraints.put(constraintIdCounter, constraint);
      ++constraintIdCounter;
      subsumedConstraints.put(constraint, new HashSet<BooleanConstraint>());
      
      // remove constraints that have been subsumed by being part of a new
      // boolean constraint 
      if (constraint instanceof BooleanConstraint)
      {
         final BooleanConstraint c = (BooleanConstraint) constraint;
         final List<Constraint> subConstraints = c.getConstraints();
         for (Constraint sc : subConstraints)
         {
            subsumedConstraints.get(sc).add(c);
            final Set<Variable> relevantScVars = sc.relevantVars();
            for (Variable v : relevantScVars)
               varsToConstraints.get(v).remove(sc);
            constraints.remove(sc);
         }
      }
   }
   /**
    * Add a constraint with a specific ID.
    *
    * Used when loading in save files.
    */
   public void addConstraint(Constraint constraint, int id)
   {
      constraints.add(constraint);
      final Set<Variable> relevantVars = constraint.relevantVars();
      for (Variable var : relevantVars)
         varsToConstraints.get(var).add(constraint);
      constraint.setID(id);
      idsToConstraints.put(id, constraint);
      if (id > constraintIdCounter)
         constraintIdCounter = id + 1;
      subsumedConstraints.put(constraint, new HashSet<BooleanConstraint>());
      
      // remove constraints that have been subsumed by being part of a new
      // boolean constraint 
      if (constraint instanceof BooleanConstraint)
      {
         final BooleanConstraint c = (BooleanConstraint) constraint;
         final List<Constraint> subConstraints = c.getConstraints();
         for (Constraint sc : subConstraints)
         {
            subsumedConstraints.get(sc).add(c);
            final Set<Variable> relevantScVars = sc.relevantVars();
            for (Variable v : relevantScVars)
               varsToConstraints.get(v).remove(sc);
            constraints.remove(sc);
         }
      }
   }

   /**
    * Remove a constraint.
    */
   public void removeConstraint(Constraint constraint)
   {
      constraints.remove(constraint);
      for (List<Constraint> cs : varsToConstraints.values())
         cs.remove(constraint);
      idsToConstraints.remove(constraint.getID());
      subsumedConstraints.remove(constraint);
      
      // re-add constraints that had been subsumed by the now-being-removed
      // boolean constraint if they aren't still subsumed by another
      // constraint, so they will get checked again separately
      if (constraint instanceof BooleanConstraint)
      {
         final BooleanConstraint c = (BooleanConstraint) constraint;
         final List<Constraint> subConstraints = c.getConstraints();
         for (Constraint sc : subConstraints)
         {
            final Set<BooleanConstraint> subsumedBy = subsumedConstraints.get(sc);
            subsumedBy.remove(c);
            if (subsumedBy.isEmpty())
            {
               final Set<Variable> relevantScVars = sc.relevantVars();
               for (Variable v : relevantScVars)
                  varsToConstraints.get(v).add(sc);
               constraints.add(sc);
               subsumedConstraints.remove(sc);
            }
         }
      }
   }

   /**
    * Set the default possible assignments for a variable type.
    *
    * Variables that don't have a list of possible values they can be assigned
    * set explicitly in their Variable() constructor will use this default list
    * for their type.
    */
   public void setDefaultValues(String type, Collection<String> values)
   {
      defaultValues.put(type, new ArrayList<String>(values));
   }

   /**
    * Get the default possible assignments for a variable type.
    *
    * An empty list (no possible assignments) if no default has been set for
    * this type via setDefaultValues().
    */
   public List<String> getDefaultValues(String type)
   {
      List<String> def = defaultValues.get(type);
      if (def != null)
         return Collections.unmodifiableList(def);

      return new ArrayList<String>();
   }

   /**
    * Generate a list of assignments matching the constraints.
    *
    * A simple brute-force constraint solver, with minor optimizations to avoid
    * re-testing constraints not affected by a particular variable's value.  An
    * assignment consists of variable to value mappings.
    *
    * @param limit The maximum number of assignments to generate.
    */
   public List<Map<Variable, String>> generate(int limit)
   {
      assignment.clear();
      assignments.clear();
      assignmentCounter = 0;

      if (!vars.isEmpty())
         assign(limit, 0);

      return assignments;
   }

   /**
    * Given a variable and set of assignments, return traces for how the
    * variable's assignment satisfies its constraints.
    */
   public String[] howSatisfied(Variable var, Map<Variable, String> assignment)
   {
      final List<Constraint> cs = varsToConstraints.get(var);
      final String traces[] = new String[cs.size()];

      for (int i = 0; i < cs.size(); ++i)
      {
         final Constraint c = cs.get(i);
         traces[i] = c.howSatisfied(var, assignment);
      }
      return traces;
   }

   /**
     * Given an assignment, return an explanation for why it doesn't
     * satisfy the constraints, or a success message if it does.
     */
   public String howFailed(Map<Variable, String> assignment)
   {
      String ret = "";
      for (Constraint c : constraints)
      {
         if (!c.check(assignment))
            ret += "Constraint not met: " + c.toString() + "\n";
      }
      if (ret.equals(""))
         ret = "Assignment meets all constraints";
      return ret;
   }


   // assign a value to the variable with index varIndex, then recurse to the next one
   private void assign(int limit, int varIndex)
   {
      if (assignmentCounter >= limit)
         return;

      final Variable var = vars.get(varIndex);
      final List<String> varValues = var.values != null ? var.values : getDefaultValues(var.type);
      final List<Constraint> cs = varsToConstraints.get(var);

      // TODO: alternate assignment method... try e.g. walking trees, which
      // will allow fake-sprites.. maybe look up the first constraint that's
      // walkable, then use that, and check the others normally?

      // TODO: check the non-boolean base constraints individually then handle
      // the boolean constraints as a tree on top of them, so we don't re-check
      // all the base constraints multiple times unnecessarily by subsuming
      // them into a boolean constraint with a big relevantVars() list

assign:
      for (String value : varValues)
      {
         assignment.put(var, value);
         // check relevant constraints
         for (Constraint c : cs)
         {
            if (!c.check(assignment))
            {
               // didn't work, so try another one
               assignment.remove(var);
               continue assign;
            }
         }
         // if we got here, the assignment is okay (so far), so recurse to next var,
         // unless it's the last one, in which case we're done
         if (varIndex == vars.size() - 1)
         {
            // add the assignment to the list
            assignments.add(new HashMap<Variable, String>(assignment));
            ++assignmentCounter;
            if (assignmentCounter >= limit)
               return;

            // continue to find other solutions
            assignment.remove(var);
            continue assign;
         }
         assign(limit, varIndex + 1);
      }
      // if we got here, ran out of assignments to try, so backtrack
      return;
   }

   /**
    * Output a collection of strings as a comma-separated list.
    */
   private static void outputStringList(BufferedWriter file, Collection<String> strings)
      throws IOException
   {
      Iterator<String> it = strings.iterator();
      while (it.hasNext())
      {
         file.write(it.next());
         if (it.hasNext())
            file.write(", ");
      }
   }
}
