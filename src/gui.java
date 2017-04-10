/**
 * The GUI editing front end.
 *
 * @author Mark J. Nelson
 * @date   2007-2008
 */

import org.jgraph.*;
import org.jgraph.graph.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Random;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;

// TODO: 
//   -- "why?" explanation thing isn't fully implemented
//   -- add adjectives, adverbs, and other such things
//   -- allow saving generated solutions to a file (though see solve.java for a non-GUI version)
//   -- allow editing of nodes
// See also other TODOs sprinkled through this file.

public class gui
{
   // ConceptNet link types to show in the drop-down boxes
   static final Object[][] cnConstraintTypes =
      {{ "CapableOf", "CapableOfReceivingAction", "UsedFor" },
       { "DefinedAs", "IsA", "MadeOf", "LocationOf", "PartOf", "PropertyOf" },
       { "DesireOf", "EffectOf", "DesirousEffectOf", "MotivationOf" },
       { "PrerequisiteEventOf", "SubeventOf", "FirstSubeventOf", "LastSubeventOf" },
       { "ConceptuallyRelatedTo", "ThematicKLine", "SuperThematicKLine" }};
   static final String[] varTypes = { "noun", "verb" };

   static DefaultGraphModel model = new DefaultGraphModel();
   static JGraph graph = new JGraph(model, new CustomMarqueeHandler());
   static JFrame frame = new JFrame("Constraint editor");
   static ConstraintSolver solver = new ConstraintSolver();
   static final JFileChooser fc = new JFileChooser();
   static Map<Object, DefaultGraphCell> userObjectToCell = new HashMap<Object, DefaultGraphCell>();
   static final Random random = new Random();

   public static void main(String[] args)
   {
      // Implementation notes:
      //
      // the "underlying model" of the solver isn't the GraphModel in the
      // JGraph sense... we keep the ConstraintSolver's data and the graphical
      // depiction in JGraph in sync manually by adding and removing things at
      // the same time, since that's actually less of a hassle imo.  yeah,
      // manually keeping the view in sync with the model isn't good MVC
      // design, but this is research code.

      // also, the serialization solution is: just write coordinates to a
      // separate layout file, and use our own file format for the semantic
      // stuff, and ignore JGraph's stuff altogether

      // force loading ConceptNet up front
      GlobalData.getInstance();

      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add(new JScrollPane(graph));
      frame.setSize(1000, 750);
      frame.setVisible(true);
   }

   /**
    * Use a custom marquee handler to grab mouse events for our menus.
    *
    * This is where all the GUI stuff happens.
    */
   public static class CustomMarqueeHandler extends BasicMarqueeHandler
   {
      /**
       * Grab all middle and right mouse clicks.
       */
      public boolean isForceMarqueeEvent(MouseEvent e)
      {
         if (SwingUtilities.isMiddleMouseButton(e) || SwingUtilities.isRightMouseButton(e))
            return true;
         return super.isForceMarqueeEvent(e);
      }

      /**
       * Display menus on middle- and right-click.
       */
      public void mousePressed(MouseEvent e)
      {
         if (SwingUtilities.isMiddleMouseButton(e)) // system/file stuff
         {
            final JPopupMenu menu = new JPopupMenu();

            menu.add(new AbstractAction("Load Constraint Space") {
               public void actionPerformed(ActionEvent e) {
                  final int returnVal = fc.showOpenDialog(frame);
                  if (returnVal == JFileChooser.APPROVE_OPTION)
                  {
                     File file = fc.getSelectedFile();
                     loadGraph(file);
                  }
               }
            });

            menu.add(new AbstractAction("Save Constraint Space") {
               public void actionPerformed(ActionEvent e) {
                  final int returnVal = fc.showSaveDialog(frame);
                  if (returnVal == JFileChooser.APPROVE_OPTION)
                  {
                     File file = fc.getSelectedFile();
                     saveGraph(file);
                  }
               }
            });
            menu.show(graph, e.getX(), e.getY());
         }

         if (SwingUtilities.isRightMouseButton(e)) // the graph-manipulation stuff
         {
            // set up some variables: point is the point clicked on; cell the cell at that point, if any
            final Point point = e.getPoint();
            final DefaultGraphCell cell = (DefaultGraphCell) graph.getFirstCellForLocation(point.getX(), point.getY());
            final JPopupMenu menu = new JPopupMenu();

            menu.add(new AbstractAction("New variable") {
               public void actionPerformed(ActionEvent e) {
                  final JComboBox typeSelect = new JComboBox(varTypes);
                  final JTextField nameInput = new JTextField("");
                  final JCheckBox useDefaults = new JCheckBox("Use default possible values");
                  final JTextField valuesInput = new JTextField("");
                  final Object components[] = {
                     new JLabel("Variable type:"),
                     typeSelect,
                     new JLabel("Variable name:"),
                     nameInput,
                     useDefaults,
                     new JLabel("Possible values (comma-separated, if not using defaults):"),
                     valuesInput
                  };

                  int okOrCancel =
                     JOptionPane.showOptionDialog(frame, components,
                        "New verb", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
                  if (okOrCancel == JOptionPane.CANCEL_OPTION)
                     return;

                  final String type = (String) typeSelect.getSelectedItem();
                  final String name = nameInput.getText();

                  Variable var = null;
                  if (useDefaults.isSelected())
                  {
                     var = new Variable(name, type);
                  }
                  else
                  {
                     final String values = valuesInput.getText();
                     // TODO: check that input is valid, e.g. no spaces in variable names

                     var = new Variable(name, type, Arrays.asList(values.split(", ?")));
                  }

                  solver.addVariable(var);
                  addVarCell(var, point);
               }
            });

            menu.add(new AbstractAction("New string literal") {
               public void actionPerformed(ActionEvent e) {
                  final JTextField nameInput = new JTextField("");
                  final Object components[] = {
                     new JLabel("String literal:"),
                     nameInput
                  };

                  int okOrCancel =
                     JOptionPane.showOptionDialog(frame, components,
                        "New string literal", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null, null);
                  if (okOrCancel == JOptionPane.CANCEL_OPTION)
                     return;

                  final String name = nameInput.getText();

                  addLiteralCell(name, point);
               }
            });
               

            if (cell != null)
            {
               // TODO: remove when we're sure we don't need, was a hack to set the border on some cells for screenshots
//               menu.add(new AbstractAction("Set border") {
//                  public void actionPerformed(ActionEvent e) {
//                     AttributeMap att = new AttributeMap();
//                     GraphConstants.setBorder(att, BorderFactory.createLineBorder(Color.black, 2));
//                     graph.getGraphLayoutCache().editCell(cell, att);
//                  }
//               });

               final Object cellUserObject = cell.getUserObject();
               if (cellUserObject instanceof Variable)
               {
                  menu.add(new AbstractAction("Remove Variable") {
                     public void actionPerformed(ActionEvent e) {
                        // remove from the underlying constraint solver, which
                        // auto-removes all relevant constraints too
                        solver.removeVariable((Variable)cellUserObject);

                        // remove the cell and relevant constraints from the
                        // graph, including the constraint-labeling cells and
                        // their edges
                        
                        // get a list of the constraint cells to remove
                        final Object[] outgoingEdges = DefaultGraphModel.getOutgoingEdges(model, cell);
                        final Object[] incomingEdges = DefaultGraphModel.getIncomingEdges(model, cell);
                        List constraintCells = new ArrayList();
                        for (Object out : outgoingEdges)
                           constraintCells.add(DefaultGraphModel.getTargetVertex(model, out));
                        for (Object in : incomingEdges)
                           constraintCells.add(DefaultGraphModel.getSourceVertex(model, in));

                        // remove all the edges incident on those constraint cells
                        graph.getModel().remove(DefaultGraphModel.getEdges(model, constraintCells.toArray()).toArray());
                        // remove the constraint cells themselves
                        graph.getModel().remove(constraintCells.toArray());
                        // remove the variable cell
                        graph.getModel().remove(new Object[] { cell });
                     }
                  });
               }
               else if (cellUserObject instanceof Constraint)
               {
                  menu.add(new AbstractAction("Remove Constraint") {
                     public void actionPerformed(ActionEvent e) {
                        // remove from the underlying constraint solver
                        solver.removeConstraint((Constraint)cellUserObject);

                        // remove the constraint cell and its incident edges from the graph
                        graph.getModel().remove(DefaultGraphModel.getEdges(model, new Object[] { cell }).toArray());
                        graph.getModel().remove(new Object[] { cell });
                     }
                  });
               }

               // TODO: might be convenient to add a constraint from a variable
               // node to to a new literal in one operation, instead of adding
               // the literal node first then the constraint to it

               // TODO: some sort of suggestion of constraints based on CN/WN
               // browsing would be nice

               // TODO: should have better editability, e.g. right-click on a CN
               // constraint node to edit its WN inheritance checkboxes
            }

            menu.addSeparator();

            menu.add(new AbstractAction("Set default possible values") {
               public void actionPerformed(ActionEvent e) {
                  /* TODO: if a type already has default values set, let user
                   * edit the list instead of having to enter a new one */

                  final JComboBox typeSelect = new JComboBox(varTypes);
                  final JTextField valuesInput = new JTextField("");
                  final Object components[] = {
                     new JLabel("Variable type:"),
                     typeSelect,
                     new JLabel("Default possible values (comma-separated list):"),
                     valuesInput
                  };

                  int okOrCancel =
                     JOptionPane.showOptionDialog(frame, components,
                        "Set default possible values", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null, null);
                  if (okOrCancel == JOptionPane.CANCEL_OPTION)
                     return;

                  final String type = (String) typeSelect.getSelectedItem();
                  final String values = valuesInput.getText();
                  // TODO: check that input is valid, e.g. no spaces in variable names
                  
                  solver.setDefaultValues(type, Arrays.asList(values.split(", ?")));
               }
            });

            menu.addSeparator();

            if (!graph.isSelectionEmpty())
            {
               final Object[] selectionObj = graph.getSelectionCells();
               final DefaultGraphCell[] selection = new DefaultGraphCell[selectionObj.length];
               for (int i = 0; i < selectionObj.length; ++i)
                  selection[i] = (DefaultGraphCell) selectionObj[i];

               if (selection.length == 2) 
               {
                  final DefaultGraphCell cell1 = selection[0];
                  final DefaultGraphCell cell2 = selection[1];
                  final Object cell1userObject = cell1.getUserObject();
                  final Object cell2userObject = cell2.getUserObject();

                  // if two Variables, or one Variable and one string literal,
                  // are selected, offer to let the user add WN or CN constraints
                  if ((cell1userObject instanceof Variable && cell2userObject instanceof Variable)
                        || (cell1userObject instanceof String && cell2userObject instanceof Variable)
                        || (cell1userObject instanceof Variable && cell2userObject instanceof String))
                  {
                     menu.add(new AbstractAction("Add ConceptNet constraint to selected nodes") {
                        public void actionPerformed(ActionEvent e) {
                           addCnConstraint(cell1, cell2);
                        }
                     });

                     menu.add(new AbstractAction("Add WordNet constraint to selected nodes") {
                        public void actionPerformed(ActionEvent e) {
                           addWnConstraint(cell1, cell2);
                        }
                     });
                  }
               }

               // if all the selected nodes are Constraint nodes, let the user
               // add a boolean constraint on top of them
               boolean onlyConstraintsSelected = true;
               final List<Constraint> selectedConstraints = new ArrayList<Constraint>();
               for (DefaultGraphCell c : selection)
               {
                  Object cellUserObject = c.getUserObject();
                  if (!(cellUserObject instanceof Constraint))
                  {
                     onlyConstraintsSelected = false;
                     break;
                  }
                  selectedConstraints.add((Constraint) cellUserObject);
               }

               if (onlyConstraintsSelected)
               {
                  if (selection.length > 1)
                  {
                     menu.add(new AbstractAction("AND the selected constraints") {
                        public void actionPerformed(ActionEvent e) {
                           AndConstraint c = new AndConstraint(selectedConstraints);
                           solver.addConstraint(c);
                           addConstraintCell(c, new DefaultGraphCell[0], selection);
                        }
                     });
                     menu.add(new AbstractAction("OR the selected constraints") {
                        public void actionPerformed(ActionEvent e) {
                           OrConstraint c = new OrConstraint(selectedConstraints);
                           solver.addConstraint(c);
                           addConstraintCell(c, new DefaultGraphCell[0], selection);
                        }
                     });
                  }
                  else // selection.length == 1
                  {
                     menu.add(new AbstractAction("NOT the selected constraint") {
                        public void actionPerformed(ActionEvent e) {
                           NotConstraint c = new NotConstraint(selectedConstraints.get(0));
                           solver.addConstraint(c);
                           addConstraintCell(c, new DefaultGraphCell[0], selection, new Point((int)point.getX(), (int)point.getY() - 75));
                        }
                     });
                  }
               }

            }

            // TODO: edit a node without removing/readding

            menu.addSeparator();

            menu.add(new AbstractAction("Show possible assignments") {
               public void actionPerformed(ActionEvent e) {

                  // TODO: don't hardcode '10'
                  final List<Map<Variable, String>> assignments = solver.generate(10);

                  final List<Variable> vars = solver.getVariables();
                  final int numVars = vars.size();
                  final int numAssignments = assignments.size();

                  Object[][] data = new Object[numAssignments][numVars];
                  for (int g = 0; g < numAssignments; ++g)
                  {
                     final Map<Variable, String> assignment = assignments.get(g);
                     for (int v = 0; v < numVars; ++v)
                        data[g][v] = assignment.get(vars.get(v));
                  }
                  String[] varNames = new String[numVars];
                  for (int v = 0; v < numVars; ++v)
                     varNames[v] = vars.get(v).name;

                  final TableModel tableModel = new DefaultTableModel(data, varNames);
                  final JTable table = new JTable(tableModel);
                  final JScrollPane scrollPane = new JScrollPane(table);
                  final JButton whyButton = new JButton("Why?");
                  final JPanel panel = new JPanel();
                  // TODO: better sizing
                  panel.setPreferredSize(new Dimension(425, 185));
                  scrollPane.setPreferredSize(new Dimension(300, 185));
                  table.setPreferredSize(new Dimension(250, 185));
                  panel.add(scrollPane);
                  panel.add(whyButton);

                  whyButton.addActionListener(new AbstractAction("Why?") {
                     public void actionPerformed(ActionEvent e) {
                        final int col = table.getSelectedColumn();
                        final int row = table.getSelectedRow();
                        if (col != -1 && row != -1)
                        {
                           final Variable var = vars.get(col);
                           final Map<Variable, String> assignment = assignments.get(row);
                           String howSatisfied[] = solver.howSatisfied(var, assignment);

                           JOptionPane.showMessageDialog(panel, howSatisfied, "Why?",
                              JOptionPane.PLAIN_MESSAGE);
                        }
                     }
                  });
                  

                  JOptionPane.showMessageDialog(frame, panel, "Variable assignments",
                        JOptionPane.PLAIN_MESSAGE);
               }
            });

            menu.add(new AbstractAction("Try a specific assignment") {
               public void actionPerformed(ActionEvent e) {
                  final List<Variable> vars = solver.getVariables();
                  final int numVars = vars.size();
                  String[] varNames = new String[numVars];
                  for (int v = 0; v < numVars; ++v)
                     varNames[v] = vars.get(v).name;

                  final Object components[] = new Object[numVars*2];
                  for (int v = 0; v < numVars; ++v)
                  {
                     components[2*v] = (Object) new JLabel(varNames[v]);
                     components[2*v+1] = (Object) new JTextField("");
                  }

                  int okOrCancel =
                     JOptionPane.showOptionDialog(frame, components,
                        "New noun", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null, null);
                  if (okOrCancel == JOptionPane.CANCEL_OPTION)
                     return;

                  Map<Variable, String> assignment = new HashMap<Variable, String>();
                  for (int v = 0; v < numVars; ++v)
                     assignment.put(vars.get(v), ((JTextField)components[2*v+1]).getText());

                  final String whyNot = solver.howFailed(assignment);
                  JOptionPane.showMessageDialog(frame, whyNot, "Check",
                        JOptionPane.PLAIN_MESSAGE);
               }
            });

            menu.show(graph, e.getX(), e.getY());
         }
         else
         {
            super.mousePressed(e);
         }
      }
   }

   /* load a saved constraint graph */
   private static void loadGraph(File file)
   {
      try
      {
         // load the constraints into the backend
         solver = new ConstraintSolver(file.getPath());

         // load the graph layout that displays the constraints
         final BufferedReader graphFile = new BufferedReader(new FileReader(file.getPath() + ".graph"));
         for (String s = graphFile.readLine(); s != null; s = graphFile.readLine())
         {
            if (s.startsWith("var "))
            {
               int pos = 4;

               int nextPos = s.indexOf(':', pos);
               final String name = s.substring(pos, nextPos);
               pos = nextPos+2;
               nextPos = s.indexOf(' ', pos);
               final int xPos = Integer.valueOf(s.substring(pos, nextPos));
               pos = nextPos+1;
               final int yPos = Integer.valueOf(s.substring(pos));
               Point p = new Point(xPos, yPos);
               addVarCell(solver.getVariable(name), p);
            }
            else if (s.startsWith("constraint "))
            {
               // this *just* adds the cell, not the edges; see "connection: " below
               // for those
               int pos = 11;
               int nextPos = s.indexOf(':', pos);
               final int constraintId = Integer.valueOf(s.substring(pos, nextPos));
               pos = nextPos + 2;
               nextPos = s.indexOf(' ', pos);
               final int xPos = Integer.valueOf(s.substring(pos, nextPos));
               pos = nextPos+1;
               final int yPos = Integer.valueOf(s.substring(pos));

               final Constraint constraint = solver.getConstraint(constraintId);
               assert constraint != null;

               addConstraintCell(constraint, new DefaultGraphCell[0], new DefaultGraphCell[0], new Point(xPos, yPos));
            }
            else if (s.startsWith("literal "))
            {
               int pos = 8;
               int nextPos = s.indexOf('"', pos+1);
               final String literal = s.substring(pos, nextPos+1);
               pos = nextPos + 3;
               nextPos = s.indexOf(' ', pos);
               final int xPos = Integer.valueOf(s.substring(pos, nextPos));
               pos = nextPos+1;
               final int yPos = Integer.valueOf(s.substring(pos));

               addLiteralCell(literal, new Point(xPos, yPos));
            }
            else if (s.startsWith("connection: "))
            {
               int pos = 12;
               final char fromType = s.charAt(pos);
               pos += 2;
               int nextPos = s.indexOf(' ', pos);
               final String fromNameOrId = s.substring(pos, nextPos); // name, id, or literal
               pos = nextPos + 1;
               final char toType = s.charAt(pos);
               pos += 2;
               final String toNameOrId = s.substring(pos);

               final Object fromObject = (Object)(fromType == 'v'
                                                  ? solver.getVariable(fromNameOrId)
                                                  : (fromType == 'c'
                                                     ? solver.getConstraint(Integer.valueOf(fromNameOrId))
                                                     : fromNameOrId));
               final Object toObject = (Object)(toType == 'v'
                                                ? solver.getVariable(toNameOrId)
                                                : (toType == 'c'
                                                   ? solver.getConstraint(Integer.valueOf(toNameOrId))
                                                   : toNameOrId));

               final DefaultGraphCell fromCell = userObjectToCell.get(fromObject);
               final DefaultGraphCell toCell = userObjectToCell.get(toObject);
               assert fromCell != null;
               assert toCell != null;

               fromCell.add(new DefaultPort());
               toCell.add(new DefaultPort());
               final DefaultEdge edge = new DefaultEdge();
               edge.setSource(fromCell.getChildAt(0));
               edge.setTarget(toCell.getChildAt(0));
               GraphConstants.setLineEnd(edge.getAttributes(), GraphConstants.ARROW_CLASSIC);
               GraphConstants.setEndFill(edge.getAttributes(), true);
               graph.getGraphLayoutCache().insert(new Object[] { edge });
            }
            else if (s.length() > 0)
            {
               throw new IOException("Unrecognized line in save file: \"" + s + "\"");
            }
         }

         // TODO: if there's no .graph file (e.g. user wrote directly in the
         // text format), do some sort of default layout based on the
         // constraint file
      }
      catch (Exception exception)
      {
         throw new RuntimeException(exception);
      }
   }

   private static void saveGraph(File file)
   {
      try
      {
         // save the backend constraint set
         solver.save(file.getPath());

         // save the visual graph layout
         final BufferedWriter graphFile = new BufferedWriter(new FileWriter(file.getPath() + ".graph"));
         final List roots = model.getRoots();

         // first output all the nodes
         // Note: we assume all nodes (both var. and constraint nodes) are
         // roots, which would stop being true if we end up using JGraph's
         // grouping functionality
         for (Object r : roots)
         {
            if (DefaultGraphModel.isVertex(model, r))
            {
               final DefaultGraphCell graphCell = (DefaultGraphCell) r;
               final Rectangle2D coord = GraphConstants.getBounds(graphCell.getAttributes());
               final Object userObject = graphCell.getUserObject();
               if (userObject instanceof Variable)
               {
                  final Variable var = (Variable) userObject;
                  graphFile.write("var " + var.name + ": " + (int)coord.getX() + " " + (int)coord.getY() + "\n");
               }
               else if (userObject instanceof Constraint)
               {
                  final Constraint constraint = (Constraint) userObject;
                  graphFile.write("constraint " + constraint.getID() + ": " + (int)coord.getX() + " " + (int)coord.getY() + "\n");
               }
               else if (userObject instanceof String)
               {
                  final String literal = (String) userObject;
                  graphFile.write("literal " + literal + ": " + (int)coord.getX() + " " + (int)coord.getY() + "\n");
               }
            }
         }
         // now output connections (arrows) between nodes, in the format:
         //   connection: v:name c:id
         // where v:name or c:id are 'v' or 'c' or 'l' for a variable or constraint,
         // or string literal, followed by a colon, followed by a name (if var) or ID
         // (if constraint) or string literal, and creates a
         // connection (arrow) from the first to the second item
         for (Object r : roots)
         {
            if (DefaultGraphModel.isVertex(model, r))
            {
               final DefaultGraphCell graphCell = (DefaultGraphCell) r;
               final Object userObject = graphCell.getUserObject();
               // source of connections is responsible for the output (to avoid double-outputting)
               final Object[] outgoingEdges = DefaultGraphModel.getOutgoingEdges(model, graphCell);
               for (Object edge : outgoingEdges)
               {
                  graphFile.write("connection: ");
                  if (userObject instanceof Variable)
                     graphFile.write("v:" + ((Variable)userObject).name);
                  else if (userObject instanceof Constraint)
                     graphFile.write("c:" + ((Constraint)userObject).getID());
                  else if (userObject instanceof String)
                     graphFile.write("l:" + ((String)userObject));

                  final DefaultGraphCell target = (DefaultGraphCell) DefaultGraphModel.getTargetVertex(model, edge);
                  final Object targetUserObject = target.getUserObject();
                  if (targetUserObject instanceof Variable)
                     graphFile.write(" v:" + ((Variable)targetUserObject).name);
                  else if (targetUserObject instanceof Constraint)
                     graphFile.write(" c:" + ((Constraint)targetUserObject).getID());
                  else if (targetUserObject instanceof String)
                     graphFile.write(" l:" + ((String)targetUserObject));
                  graphFile.write("\n");
               }
            }
         }
         graphFile.close();
      }
      catch (Exception exception)
      {
         throw new RuntimeException("Error saving to file: " + file.getPath());
      }
   }

   /* add a variable cell to the graph */
   private static void addVarCell(Variable var, Point point)
   {
      final DefaultGraphCell newCell = new DefaultGraphCell(var);
      userObjectToCell.put(var, newCell);
      GraphConstants.setBounds(newCell.getAttributes(),
                               new Rectangle2D.Double(point.getX(), point.getY(), 0, 0));
      GraphConstants.setResize(newCell.getAttributes(), true);
      GraphConstants.setBorder(newCell.getAttributes(), BorderFactory.createLineBorder(Color.black, 2));
      graph.getGraphLayoutCache().insert(new DefaultGraphCell[] { newCell });
   }

   /* add a string literal to the graph */
   private static void addLiteralCell(String string, Point point)
   {
      final String quotedString = (string.charAt(0) == '"') ? string : "\"" + string + "\"";
      final DefaultGraphCell newCell = new DefaultGraphCell(quotedString);
      userObjectToCell.put(string, newCell);
      GraphConstants.setBounds(newCell.getAttributes(),
                               new Rectangle2D.Double(point.getX(), point.getY(), 0, 0));
      GraphConstants.setResize(newCell.getAttributes(), true);
      GraphConstants.setBorder(newCell.getAttributes(), BorderFactory.createLineBorder(Color.black, 2));
      graph.getGraphLayoutCache().insert(new DefaultGraphCell[] { newCell });
   }

   /* add a constraint cell to the graph, with edges to/from the specified
    * source and target cells, and a location chosen to be the mean x/y
    * location of its connected cells */
   private static void addConstraintCell(Constraint c, DefaultGraphCell[] sources, DefaultGraphCell[] targets)
   {
      final int numConnections = sources.length + targets.length;
      double x = 0.0;
      double y = 0.0;
      for (DefaultGraphCell cell : sources)
      {
         final Rectangle2D rect = GraphConstants.getBounds(cell.getAttributes());
         x += rect.getX();
         y += rect.getY();
      }
      for (DefaultGraphCell cell : targets)
      {
         final Rectangle2D rect = GraphConstants.getBounds(cell.getAttributes());
         x += rect.getX();
         y += rect.getY();
      }
      x /= numConnections;
      y /= numConnections;

      addConstraintCell(c, sources, targets, new Point((int)x, (int)y));
   }

   /* add a constraint cell to the graph, with edges to/from the specified
    * source and target cells, and at a specific location
    */
   private static void addConstraintCell(Constraint c, DefaultGraphCell[] sources, DefaultGraphCell[] targets, Point p)
   {
      final DefaultGraphCell constraintCell = new DefaultGraphCell(c);
      userObjectToCell.put(c, constraintCell);

      GraphConstants.setBounds(constraintCell.getAttributes(), new Rectangle2D.Double(p.getX(), p.getY(), 0, 0));
      GraphConstants.setResize(constraintCell.getAttributes(), true);

      // add the edges
      final int numConnections = sources.length + targets.length;
      Object[] toInsert = new Object[numConnections+1];
      int connectionCount = 0;
      for (DefaultGraphCell source : sources)
      {
         source.add(new DefaultPort());
         constraintCell.add(new DefaultPort());
         final DefaultEdge edge = new DefaultEdge();
         edge.setSource(source.getChildAt(0));
         edge.setTarget(constraintCell.getChildAt(connectionCount));
         GraphConstants.setLineEnd(edge.getAttributes(), GraphConstants.ARROW_CLASSIC);
         GraphConstants.setEndFill(edge.getAttributes(), true);
         toInsert[connectionCount] = edge;
         ++connectionCount;
      }
      for (DefaultGraphCell target : targets)
      {
         constraintCell.add(new DefaultPort());
         target.add(new DefaultPort());
         final DefaultEdge edge = new DefaultEdge();
         edge.setSource(constraintCell.getChildAt(connectionCount));
         edge.setTarget(target.getChildAt(0));
         GraphConstants.setLineEnd(edge.getAttributes(), GraphConstants.ARROW_CLASSIC);
         GraphConstants.setEndFill(edge.getAttributes(), true);
         toInsert[connectionCount] = edge;
         ++connectionCount;
      }
      toInsert[numConnections] = constraintCell;

      // insert the new cell and edges
      graph.getGraphLayoutCache().insert(toInsert);
   }

   private static void addCnConstraint(DefaultGraphCell cell1, DefaultGraphCell cell2)
   {
      final JLabel sourceLabel = new JLabel(cell1.toString());
      final JLabel targetLabel = new JLabel(cell2.toString());

      // TODO: offer types in a "smart" way based on node types (i.e. actions are verbs, properties are nouns)
      final SepComboBox constraintTypeInput = new SepComboBox(cnConstraintTypes);

      final JButton swapButton = new JButton("Swap source/target");
      final boolean[] swapped = new boolean[1]; // stupid 1-element boolean so it's something final that we can access in a closure
      swapped[0] = false;
      final boolean[] inheritance = new boolean[] { false, false, false, false };
      swapButton.addActionListener(new AbstractAction("Swap source/target") {
         public void actionPerformed(ActionEvent e) {
            final String temp = sourceLabel.getText();
            sourceLabel.setText(targetLabel.getText());
            targetLabel.setText(temp);
            swapped[0] = !swapped[0];

            boolean tempB = inheritance[0]; 
            inheritance[0] = inheritance[2];
            inheritance[2] = tempB;
            tempB = inheritance[1];
            inheritance[1] = inheritance[3];
            inheritance[3] = tempB;
         }
      });

      final JButton inheritanceButton = new JButton("Set WordNet inheritance");
      inheritanceButton.addActionListener(new AbstractAction("Set WordNet inheritance") {
         public void actionPerformed(ActionEvent e) {
            final JRadioButton[] buttons = new JRadioButton[6];

            final ButtonGroup sourceGroup = new ButtonGroup();
            buttons[0] = new JRadioButton("Exact match");
            buttons[0].setActionCommand("None");
            sourceGroup.add(buttons[0]);
            buttons[1] = new JRadioButton("Exact or more general match");
            buttons[1].setActionCommand("Hypernym");
            sourceGroup.add(buttons[1]);
            buttons[2] = new JRadioButton("Exact or more specific match");
            buttons[2].setActionCommand("Hyponym");
            sourceGroup.add(buttons[2]);
            buttons[0].setSelected(true);

            final ButtonGroup targetGroup = new ButtonGroup();
            buttons[3] = new JRadioButton("Exact match");
            buttons[3].setActionCommand("None");
            targetGroup.add(buttons[3]);
            buttons[4] = new JRadioButton("Exact or more general match");
            buttons[4].setActionCommand("Hypernym");
            targetGroup.add(buttons[4]);
            buttons[5] = new JRadioButton("Exact or more specific match");
            buttons[5].setActionCommand("Hyponym");
            targetGroup.add(buttons[5]);
            buttons[3].setSelected(true);

            Object inheritanceComponents[] = {
               new JLabel("WordNet inheritance for " + sourceLabel.getText() + ":"),
               buttons[0], buttons[1], buttons[2],
               new JLabel("WordNet inheritance for " + targetLabel.getText() + ":"),
               buttons[3], buttons[4], buttons[5]
            };

            int okOrCancel =
               JOptionPane.showOptionDialog(frame, inheritanceComponents,
                                            "Set WordNet inheritance", JOptionPane.OK_CANCEL_OPTION,
                                            JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (okOrCancel == JOptionPane.CANCEL_OPTION)
               return;

            final String sourceSelection = sourceGroup.getSelection().getActionCommand();
            inheritance[0] = sourceSelection.equals("Hypernym");
            inheritance[1] = sourceSelection.equals("Hyponym");
            final String targetSelection = targetGroup.getSelection().getActionCommand();
            inheritance[2] = targetSelection.equals("Hypernym");
            inheritance[3] = targetSelection.equals("Hyponym");
         }
      });


      final Object components[] = {
         sourceLabel,
         constraintTypeInput,
         targetLabel,
         swapButton,
         inheritanceButton
      };

      int okOrCancel =
         JOptionPane.showOptionDialog(frame, components,
                                      "New ConceptNet constraint", JOptionPane.OK_CANCEL_OPTION,
                                      JOptionPane.QUESTION_MESSAGE, null, null, null);
      if (okOrCancel == JOptionPane.CANCEL_OPTION)
         return;

      final DefaultGraphCell source = swapped[0] ? cell2 : cell1;
      final DefaultGraphCell target = swapped[0] ? cell1 : cell2;

      // add the underlying constraints
      final String type = (String) constraintTypeInput.getSelectedItem();
      final Object sourceUserObject = source.getUserObject();
      final Object targetUserObject = target.getUserObject();

      Constraint newConstraint;

      if (sourceUserObject instanceof Variable
          && targetUserObject instanceof Variable)
      {
         newConstraint = new ConceptNetConstraint(type,
                                                  (Variable) sourceUserObject,
                                                  (Variable) targetUserObject,
                                                  inheritance);
      }
      else if (sourceUserObject instanceof Variable
               && targetUserObject instanceof String)
      {
         newConstraint = new ConceptNetConstraint(type,
                                                  (Variable) sourceUserObject,
                                                  trimFirstLast((String) targetUserObject),
                                                  inheritance);
      }
      else // (sourceUserObject instanceof String && targetUserObject instanceof Variable)
      {
         newConstraint = new ConceptNetConstraint(type,
                                                  trimFirstLast((String) sourceUserObject),
                                                  (Variable) targetUserObject,
                                                  inheritance);
      }

      solver.addConstraint(newConstraint);

      // now add the visual constraints
      addConstraintCell(newConstraint, new DefaultGraphCell[] { source }, new DefaultGraphCell[] { target });

      // TODO: would be nice if some CN and/or WN browsing were integrating in here
   }

   private static void addWnConstraint(DefaultGraphCell cell1, DefaultGraphCell cell2)
   {
      final JLabel sourceLabel = new JLabel(cell1.toString());
      final JLabel targetLabel = new JLabel(cell2.toString());

      final ButtonGroup typeGroup = new ButtonGroup();
      final JRadioButton hypernym = new JRadioButton("Generalization of");
      hypernym.setActionCommand("Hypernym");
      hypernym.setSelected(true);
      typeGroup.add(hypernym);
      final JRadioButton hyponym = new JRadioButton("Specialization of");
      hyponym.setActionCommand("Hyponym");
      typeGroup.add(hyponym);

      final JButton swapButton = new JButton("Swap source/target");
      final boolean[] swapped = new boolean[1]; // stupid 1-element boolean so it's something final that we can access in a closure
      swapped[0] = false;
      swapButton.addActionListener(new AbstractAction("Swap source/target") {
         public void actionPerformed(ActionEvent e) {
            final String temp = sourceLabel.getText();
            sourceLabel.setText(targetLabel.getText());
            targetLabel.setText(temp);
            swapped[0] = !swapped[0];
         }
      });

      final Object components[] = {
         sourceLabel,
         hypernym,
         hyponym,
         targetLabel,
         swapButton
      };

      int okOrCancel =
         JOptionPane.showOptionDialog(frame, components,
                                      "New WordNet constraint", JOptionPane.OK_CANCEL_OPTION,
                                      JOptionPane.QUESTION_MESSAGE, null, null, null);
      if (okOrCancel == JOptionPane.CANCEL_OPTION)
         return;

      final boolean isHypernym = typeGroup.getSelection().getActionCommand().equals("Hypernym");
      final DefaultGraphCell source = swapped[0] ? cell2 : cell1;
      final DefaultGraphCell target = swapped[0] ? cell1 : cell2;

      // add the underlying constraints
      final Object sourceUserObject = source.getUserObject();
      final Object targetUserObject = target.getUserObject();

      Constraint newConstraint;

      if (sourceUserObject instanceof Variable
          && targetUserObject instanceof Variable)
      {
         newConstraint = new WordNetConstraint(
            (Variable) sourceUserObject,
            (Variable) targetUserObject,
            isHypernym);
      }
      else if (sourceUserObject instanceof Variable
               && targetUserObject instanceof String)
      {
         newConstraint = new WordNetConstraint(
            (Variable) sourceUserObject,
            trimFirstLast((String) targetUserObject),
            isHypernym);
      }
      else // (sourceUserObject instanceof String && targetUserObject instanceof Variable)
      {
         newConstraint = new WordNetConstraint(
            trimFirstLast((String) sourceUserObject),
            (Variable) targetUserObject,
            isHypernym);
      }

      solver.addConstraint(newConstraint);

      // now add the visual constraints
      addConstraintCell(newConstraint, new DefaultGraphCell[] { source }, new DefaultGraphCell[] { target });
   }

   // utility function to trim off the first and last chars of a string
   private static String trimFirstLast(String string)
   {
      return string.substring(1, string.length() - 1);
   }
}
