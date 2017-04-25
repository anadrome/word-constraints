/**
  * A class to load and interface with ConceptNet.
  *
  * Requires ConceptNet predicate files, as specified in FILENAMES,
  * to be present in the working directory.
  *
  * @author Mark J. Nelson
  * @date   2007-2008
  */

// maybe TODO: currently some inefficient RAM usage due to every source/target being
//             stored both inside the Relation and inside the outgoing/incoming indices
// also along those lines: could turn relation type into an enum instead of having
//             strings like ConceptuallyRelatedTo stored literally every time

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

public class ConceptNet
{
   private static final String[] FILENAMES =
      new String[] { "conceptnet_singlewords.txt.gz" };
/*      new String[] { "predicates_concise_kline.txt.gz",
                     "predicates_concise_nonkline.txt.gz",
                     "predicates_nonconcise_kline.txt.gz",
                     "predicates_nonconcise_nonkline.txt.gz" };
                     */

   public class Relation
   {
      public String type;
      public String source;
      public String target;
      
      public Relation(String type_, String source_, String target_)
      {
         type = type_;
         source = source_;
         target = target_;
      }

      public String toString()
      {
         return "(" + type + " \"" + source + "\" \"" + target + "\")";
      }
   }
   
   /* The relations, indexed by source and target */
   private Map<String, List<Relation>> outgoing = new HashMap<>();
   private Map<String, List<Relation>> incoming = new HashMap<>();
   
   public ConceptNet()
      throws IOException
   {
      /* Match against this pattern, made unreadable due to escaping:
       *    ^\((\S+) "(.*)" "(.*)" ".*")$
       */
      Pattern regex = Pattern.compile("^\\((\\S+) \"(.*)\" \"(.*)\" \".*\"\\)$");
      int count = 0;
      for (String filename : FILENAMES)
      {
	 BufferedReader file = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));

         for (String s = file.readLine(); s != null; s = file.readLine())
         {
            Matcher m = regex.matcher(s);
            boolean b = m.matches();
            assert b;

            Relation r = new Relation(m.group(1), m.group(2), m.group(3));
            addRelation(r);
            ++count;
         }
      }
   }

   /**
     * Return a list of all relations going out of a node.
     */
   public List<Relation> getOutgoing(String node)
   {
      List<Relation> out = outgoing.get(node);
      if (out == null)
         out = new ArrayList<>();
      return Collections.unmodifiableList(out);
   }

   /**
     * Return a list of all relations coming into a node.
     */
   public List<Relation> getIncoming(String node)
   {
      List<Relation> in = incoming.get(node);
      if (in == null)
         in = new ArrayList<>();
      return Collections.unmodifiableList(in);
   }

   /**
     * Find a shortest path between two nodes, following edges forwards or backwards.
     *
     * @return An ordered list of Relations that forms the shortest path from
     * the source to the target, or null if there is no path.
     */
   public List<Relation> shortestPath(String source, String target, int maxHops)
   {
      /* Note: this is just a copy/paste of shortestPath(String, String, int, List<string>)
       * with the relationTypes checks removed.  Yeah, bad software engineering. */

      List<Relation> path = new LinkedList<>();
      if (source.equals(target))
         return path;

      if (maxHops < 1)
         return null;

      List<Relation> rs_out = outgoing.get(source);
      List<Relation> rs_in = incoming.get(source);
      if (rs_in == null && rs_out == null)
         return null;

      for (int i = 1; i <= maxHops; ++i)
      {
         if (rs_out != null)
         {
            for (Relation r : rs_out)
            {
               List<Relation> pathRec = shortestPath(r.target, target, i-1);
               if (pathRec != null)
               {
                  path.add(r);
                  path.addAll(pathRec);
                  return path;
               }
            }
         }
         if (rs_in != null)
         {
            for (Relation r : rs_in)
            {
               List<Relation> pathRec = shortestPath(r.source, target, i-1);
               if (pathRec != null)
               {
                  path.add(r);
                  path.addAll(pathRec);
                  return path;
               }
            }
         }
      }

      return null;
   }

   /**
     * Find a shortest path between two nodes, only following the indicated
     * relation types (but following them forwards or backwards).
     */
   public List<Relation> shortestPath(String source, String target, int maxHops,
                                      Set<String> relationTypes)
   {
      /* TODO: This could be a lot more efficient, e.g. by searching simultaneously
       * from each end, and/or marking already-visited nodes, and/or doing BFS instead
       * of iterative deepening. */

      /* For now, do iterative deepening, 'cause it's easy to code and "fast enough". */

      List<Relation> path = new LinkedList<>();
      if (source.equals(target))
         return path;

      if (maxHops < 1)
         return null;

      List<Relation> rs_out = outgoing.get(source);
      List<Relation> rs_in = incoming.get(source);
      if (rs_in == null && rs_out == null)
         return null;
      boolean oneLink = false;
      for (Relation r : rs_out)
      {
         if (relationTypes.contains(r.type))
         {
            oneLink = true;
            break;
         }
      }
      if (!oneLink)
      {
         for (Relation r : rs_in)
         {
            if (relationTypes.contains(r.type))
            {
               oneLink = true;
               break;
            }
         }
      }
      if (!oneLink)
         return null;

      for (int i = 1; i <= maxHops; ++i)
      {
         for (Relation r : rs_out)
         {
            if (relationTypes.contains(r.type))
            {
               List<Relation> pathRec = shortestPath(r.target, target, i-1, relationTypes);
               if (pathRec != null)
               {
                  path.add(r);
                  path.addAll(pathRec);
                  return path;
               }
            }
         }
         for (Relation r : rs_in)
         {
            if (relationTypes.contains(r.type))
            {
               List<Relation> pathRec = shortestPath(r.source, target, i-1, relationTypes);
               if (pathRec != null)
               {
                  path.add(r);
                  path.addAll(pathRec);
                  return path;
               }
            }
         }
      }
      
      return null;
   }

   /**
     * Finds the closest node among a set of nodes to a query node, following
     * links as if they were undirected.
     *
     * @return The closest node, or null if none found within maxHops hops.
     */
   public String closestInSet(String query, Set<String> set, int maxHops)
   {
      /* Note: This uses a somewhat inefficient iterative deepening, and doesn't
       * avoid visiting the same node multiple times, but is "fast enough" for
       * most purposes.
       */

      if (set.contains(query))
         return query;

      if (maxHops < 1)
         return null;

      List<Relation> rs_out = outgoing.get(query);
      List<Relation> rs_in = incoming.get(query);
      if (rs_in == null && rs_out == null)
         return null;

      for (int i = 1; i <= maxHops; ++i)
      {
         if (rs_out != null)
         {
            for (Relation r : rs_out)
            {
               String closest = closestInSet(r.target, set, i-1);
               if (closest != null)
                  return closest;
            }
         }
         if (rs_in != null)
         {
            for (Relation r : rs_in)
            {
               String closest = closestInSet(r.source, set, i-1);
               if (closest != null)
                  return closest;
            }
         }
      }

      return null;
   }

   /**
    * Check whether a specific link exists.
    */
   public boolean linkExists(String type, String source, String target)
   {
      final List<Relation> out = outgoing.get(source);
      if (out == null)
         return false;

      for (Relation r : out)
      {
         if (r.type.equals(type) && r.target.equals(target))
            return true;
      }

      return false;
   }
   
   public boolean linkExists(String type, String source, String target,
         boolean[] inheritance)
   {
      return linkExists(type, source, target, inheritance, null);
   }

   /**
     * Check whether a specific link exists, optionally with WordNet "inheritance".
     *
     * WordNet inheritance means that we look for not only the literal link in
     * ConceptNet, but also (if the literal link doesn't exist) whether a link
     * exists with some hyponym or hypernym of either the source or target
     * (depending on the inheritance parameter). For example, in checking for
     * CapableOf duck run, if we ask for hypernym inheritance on the source, we'll
     * return true because CapableOf animal run, and animal is a hypernum of duck.
     * <p>
     * Important note: If inheritance is requested for a term, the term is assumed
     * to be a noun!
     *
     * @param inheritance A 4-element array, specifying whether inheritance will be
     *                    done on, respectively, hypernyms of source, hyponyms of source,
     *                    hypernyms of target, and hyponyms of target.
     *
     * Also: as a hack, put a trace string in trace[0] if trace != null
     */
   public boolean linkExists(String type, String source, String target,
         boolean[] inheritance, String trace[])
   {
      if (linkExists(type, source, target))
      {
         if (trace != null)
            trace[0] = source + " --(ConceptNet)--> " + target;
         return true;
      }

      final WordNet wordNet = GlobalData.getInstance().wordNet;

      // don't do inheritance if: 1) not requested; or 2) requested but word
      // isn't in WordNet
      if (!(inheritance[0] || inheritance[1] || inheritance[2] || inheritance[3])
            || !wordNet.isWord(WordNet.NOUN, source))
         return false;

      // now check the inheritance stuff

      // TODO: it'd be faster if we short-circuited upon success instead of first
      // collecting all the equivs and then checking them, but that makes the
      // cn/wn separation less nice code-wise, unless we write an iterator in wn
      // Also: getHyp*nyms() basically walks synonyms anyway so it'd be faster if
      // that were consolidated instead of a separate getSynonyms() call

      List<String> sourceEquiv = new ArrayList<>();
      if (inheritance[0] || inheritance[1])
         sourceEquiv.addAll(wordNet.getSynonyms(WordNet.NOUN, source));
      else
         sourceEquiv.add(source);
      if (inheritance[0])
         sourceEquiv.addAll(wordNet.getHypernyms(WordNet.NOUN, source));
      if (inheritance[1])
         sourceEquiv.addAll(wordNet.getHyponyms(WordNet.NOUN, source));

      List<String> targetEquiv = new ArrayList<>();
      if (inheritance[2] || inheritance[3])
         targetEquiv.addAll(wordNet.getSynonyms(WordNet.NOUN, target));
      else
         targetEquiv.add(target);
      if (inheritance[2])
         targetEquiv.addAll(wordNet.getHypernyms(WordNet.NOUN, target));
      if (inheritance[3])
         targetEquiv.addAll(wordNet.getHyponyms(WordNet.NOUN, target));
      
      for (String s : sourceEquiv)
      {
         for (String t : targetEquiv)
         {
            if (linkExists(type, s, t))
            {
               if (trace != null)
                  trace[0] =
                     (source.equals(s)
                      ? source
                      : source + " --(WordNet)--> " + s)
                     + " --(ConceptNet)--> "
                     + (target.equals(t)
                        ? target
                        : target + " --(WordNet)--> " + t);

               return true;
            }
         }
      }
      
      return false;
   }
   
   /**
     * Convert a path to a string, in the format "foo -&gt; bar &lt;- baz".
     *
     * Nodes in order of visitation in the path, with link direction indicated
     * by the arrows.  We need source and target given to us explicitly, since
     * otherwise we don't know what order the first and last links are going.
     */
   public static String pathToString(List<Relation> path, String source, String target)
   {
      String ret = "";
      String cur = source;
      for (Relation r : path)
      {
         ret = ret + cur;
         if (cur.equals(r.source)) /* forward */
         {
            ret = ret + " -> ";
            cur = r.target;
         }
         else /* backward */
         {
            ret = ret + " <- ";
            cur = r.source;
         }
      }
      ret = ret + cur;
      return ret;
   }
      
   
   /* private utility stuff */

   private void addRelation(Relation r)
   {
      List<Relation> out = outgoing.get(r.source);
      if (out == null)
      {
         out = new ArrayList<>(1);
         outgoing.put(r.source, out);
      }
      out.add(r);

      List<Relation> in = incoming.get(r.target);
      if (in == null)
      {
         in = new ArrayList<>(1);
         incoming.put(r.target, in);
      }
      in.add(r);
   }

}
