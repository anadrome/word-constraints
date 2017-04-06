/**
  * Minor encapsulation of JWordNet for stuff we do commonly.
  *
  * Note: The most recent JWordNet barfs on WordNets more recent than 1.7.1, so
  * use that.
  *
  * @author Mark J. Nelson
  * @date   2007
  */

import edu.brandeis.cs.steele.wn.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class WordNet
{
   /* encapsulate the part-of-speech static variables here */
   public static final POS NOUN = POS.NOUN;
   public static final POS VERB = POS.VERB;
   public static final POS ADJ = POS.ADJ;
   public static final POS ADV = POS.ADV;

   /* interface to the dictionary itself */
   private DictionaryDatabase dictionary = new FileBackedDictionary("wordnet");

   public WordNet()
   {
   }

   /**
    * Check if a word exists in WordNet.
    */
   public boolean isWord(POS pos, String word)
   {
      return dictionary.lookupIndexWord(pos, word) != null;
   }

   /**
     * Check if two words are synonymous, in any of their senses.
     *
     * Note that this is faster than calling getSynonyms and then checking for
     * presence in that list.
     */
   public boolean isSynonym(POS pos1, String word1, POS pos2, String word2)
   {
      IndexWord indexWord1 = dictionary.lookupIndexWord(pos1, word1);
      IndexWord indexWord2 = dictionary.lookupIndexWord(pos2, word2);
      if (indexWord1 == null || indexWord2 == null)
         return false;

      Synset[] synsets1 = indexWord1.getSenses();
      Synset[] synsets2 = indexWord2.getSenses();
      for (Synset s1 : synsets1)
         for (Synset s2 : synsets2)
            if (s1.equals(s2))
               return true;

      return false;
   }
   
   /**
     * Get all the words in a word's synonym set (including itself), for any of
     * its senses of the same part of speech.
     */
   public List<String> getSynonyms(POS pos, String word)
   {
      IndexWord indexWord = dictionary.lookupIndexWord(pos, word);
      if (indexWord == null)
         return null;
      Synset[] synsets = indexWord.getSenses();
      List<String> ret = new ArrayList<String>();
      for (Synset synset : synsets)
      {
         Word[] synWords = synset.getWords();
         for (Word synWord : synWords)
            ret.add(synWord.getLemma());
      }
      return ret;
   }
   
   /**
     * Check whether word1 is a hypernym of word2 (equivalently: whether pos1
     * is a hyponum of word2), either directly or via inheritance.
     *
     * Note that this is faster than calling getHypernyms and then checking for
     * presence in that list.
     */
   public boolean isHypernym(POS pos1, String word1, POS pos2, String word2)
   {
      IndexWord indexWord1 = dictionary.lookupIndexWord(pos1, word1);
      IndexWord indexWord2 = dictionary.lookupIndexWord(pos2, word2);
      if (indexWord1 == null || indexWord2 == null)
         return false;

      Synset[] synsets1 = indexWord1.getSenses();
      Synset[] synsets2 = indexWord2.getSenses();
      for (Synset s1 : synsets1)
         for (Synset s2 : synsets2)
            if (isHypernym(s1, s2))
               return true;

      return false;
   }
   /* Internal implementation */
   private boolean isHypernym(Synset syn1, Synset syn2)
   {
      PointerTarget[] targets = syn2.getTargets(PointerType.HYPERNYM);
      for (PointerTarget target : targets)
      {
         if (!(target instanceof Synset))
            continue;
         Synset s = (Synset)target;
         if (s.equals(syn1))
            return true;
         if (isHypernym(syn1, s))
            return true;
      }
      return false;
   }

   /**
     * Get all the hypernyms of a word (including inherited ones).
     *
     * @return null if the word is not found in WordNet, or a list of hypernyms
     *         otherwise (if the word is found but has no hypernyms, a zero-element
     *         list is returned)
     */
   public List<String> getHypernyms(POS pos, String word)
   {
      IndexWord indexWord = dictionary.lookupIndexWord(pos, word);
      if (indexWord == null)
         return null;

      List<String> hypernyms = new ArrayList<String>();
      Synset[] synsets = indexWord.getSenses();
      for (Synset s : synsets)
      {
         List<Synset> hyps = getHypernyms(s);
         for (Synset hyp : hyps)
         {
            Word[] hypWords = hyp.getWords();
            for (Word hypWord : hypWords)
               hypernyms.add(hypWord.getLemma());
         }
      }
      return hypernyms;
   }
   /* Internal implementation */
   private List<Synset> getHypernyms(Synset synset)
   {
      List<Synset> hypernyms = new ArrayList<Synset>();
      PointerTarget[] targets = synset.getTargets(PointerType.HYPERNYM);
      for (PointerTarget target : targets)
      {
         if (!(target instanceof Synset))
            continue;
         Synset s = (Synset)target;
         hypernyms.add(s);
         hypernyms.addAll(getHypernyms(s));
      }
      return hypernyms;
   }

   /**
     * Get all the hyponyms of a word (including inherited ones).
     *
     * @return null if the word is not found in WordNet, or a list of hyponyms
     *         otherwise (if the word is found but has no hyponyms, a zero-element
     *         list is returned)
     */
   public List<String> getHyponyms(POS pos, String word)
   {
      IndexWord indexWord = dictionary.lookupIndexWord(pos, word);
      if (indexWord == null)
         return null;

      List<String> hyponyms = new ArrayList<String>();
      Synset[] synsets = indexWord.getSenses();
      for (Synset s : synsets)
      {
         List<Synset> hyps = getHyponyms(s);
         for (Synset hyp : hyps)
         {
            Word[] hypWords = hyp.getWords();
            for (Word hypWord : hypWords)
               hyponyms.add(hypWord.getLemma());
         }
      }
      return hyponyms;
   }
   /* Internal implementation */
   private List<Synset> getHyponyms(Synset synset)
   {
      List<Synset> hyponyms = new ArrayList<Synset>();
      PointerTarget[] targets = synset.getTargets(PointerType.HYPONYM);
      for (PointerTarget target : targets)
      {
         if (!(target instanceof Synset))
            continue;
         Synset s = (Synset)target;
         hyponyms.add(s);
         hyponyms.addAll(getHyponyms(s));
      }
      return hyponyms;
   }

   /**
     * Find the distance between two words of the same part-of-speech,
     * according to hypernymy/hyponymy relationships.
     */
   public int wordDistance(String word1, String word2, POS pos, int maxHops)
   {
      /* Note: This uses a somewhat inefficient alg: we just recurse both
       * up and down with noun1 while leaving noun2 in the same place,
       * and don't avoid visiting nodes multiple times... but it's "fast
       * enough" for most purposes.
       */

      IndexWord indexWord1 = dictionary.lookupIndexWord(pos, word1);
      if (indexWord1 == null)
         return maxHops;
      Synset[] synsets1 = indexWord1.getSenses();
      IndexWord indexWord2 = dictionary.lookupIndexWord(pos, word2);
      if (indexWord2 == null)
         return maxHops;
      Synset[] synsets2 = indexWord2.getSenses();
      int minDistance = maxHops;
      for (Synset synset1 : synsets1)
      {
         for (Synset synset2 : synsets2)
         {
            final int dist = wordDistance(synset1, synset2, maxHops);
            if (dist < minDistance)
               minDistance = dist;
         }
      }

      return minDistance;
   }
   /* Internal implementation */
   private int wordDistance(Synset word1, Synset word2, int maxHops)
   {
      if (maxHops < 1)
         return 0;
      
      if (word1.equals(word2))
         return 0;

      PointerTarget[] hyperTargets = word1.getTargets(PointerType.HYPERNYM);
      PointerTarget[] hypoTargets = word1.getTargets(PointerType.HYPONYM);
      if (hyperTargets.length < 1 && hypoTargets.length < 1)
         return maxHops;

      int minDistance = maxHops;
      for (PointerTarget target : hyperTargets)
      {
         if (!(target instanceof Synset))
            continue;
         Synset s = (Synset)target;
         final int dist = 1 + wordDistance(s, word2, minDistance-1);
         if (dist < minDistance)
            minDistance = dist;
      }
      for (PointerTarget target : hypoTargets)
      {
         if (!(target instanceof Synset))
            continue;
         Synset s = (Synset)target;
         final int dist = 1 + wordDistance(s, word2, minDistance-1);
         if (dist < minDistance)
            minDistance = dist;
      }
      return minDistance;
   }

   /**
    * Finds the closest noun among a set of noun to a query noun, according to
    * hypernymy/hyponym relationships.
    *
    * Basically, follows hypernym/hyponym links, and returns the noun in the
    * query set that can be reached via the fewest links.  Synonymy is not
    * counted as a hop.
    *
    * @return The closest noun, or null if none found within maxHops hops.
    */
   public String closestNounInSet(String query, Set<String> set, int maxHops)
   {
      /* Note: This uses a somewhat inefficient double-layered recursive alg,
       * and doesn't avoid visiting nodes multiple times, but it's "fast
       * enough" for most purposes.
       */

      IndexWord indexWord = dictionary.lookupIndexWord(NOUN, query);
      if (indexWord == null)
         return null;
      Synset[] synsets = indexWord.getSenses();
      for (int i = 1; i <= maxHops; ++i)
      {
         for (Synset synset : synsets)
         {
            String s = closestNounInSet(synset, set, i);
            if (s != null)
               return s;
         }
      }
      return null;
   }
   /* Internal implementation */
   private String closestNounInSet(Synset query, Set<String> set, int maxHops)
   {
      Word[] words = query.getWords();
      for (Word word : words)
         if (set.contains(word.getLemma()))
            return word.getLemma();

      if (maxHops < 1)
         return null;
      
      PointerTarget[] hyperTargets = query.getTargets(PointerType.HYPERNYM);
      PointerTarget[] hypoTargets = query.getTargets(PointerType.HYPONYM);
      if (hyperTargets.length < 1 && hypoTargets.length < 1)
         return null;

      for (int i = 1; i <= maxHops; ++i)
      {
         for (PointerTarget target : hyperTargets)
         {
            if (!(target instanceof Synset))
               continue;
            Synset s = (Synset)target;
            String closest = closestNounInSet(s, set, i-1);
            if (closest != null)
               return closest;
         }
         for (PointerTarget target : hypoTargets)
         {
            if (!(target instanceof Synset))
               continue;
            Synset s = (Synset)target;
            String closest = closestNounInSet(s, set, i-1);
            if (closest != null)
               return closest;
         }
      }
      return null;
   }
}
