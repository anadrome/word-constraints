/**
  * A high-level interface to WordNet with useful utility functions, backed by JWI.
  *
  * @author Mark J. Nelson
  * @date   2007,2017
  */

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.IStemmer;
import edu.mit.jwi.morph.WordnetStemmer;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.io.File;
import java.io.IOException;

public class WordNet
{
   /* encapsulate the part-of-speech static variables here */
   public static final POS NOUN = POS.NOUN;
   public static final POS VERB = POS.VERB;
   public static final POS ADJ = POS.ADJECTIVE;
   public static final POS ADV = POS.ADVERB;

   private IDictionary dictionary;
   private IStemmer stemmer;

   public WordNet()
      throws IOException
   {
      dictionary = new Dictionary(new File("dict"));
      dictionary.open();
      stemmer = new WordnetStemmer(dictionary);
   }

   /**
    * Check if a word exists in WordNet.
    */
   public boolean isWord(POS pos, String word)
   {
      List<String> stems = stemmer.findStems(word, pos);
      for (String s : stems)
         if (dictionary.getIndexWord(s, pos) != null)
            return true;
      return false;
   }

   /* Internal helper function */
   private List<ISynset> getSynsets(POS pos, String word)
   {
      List<String> stems = stemmer.findStems(word, pos);

      List<IWordID> wordids = new ArrayList<IWordID>();
      for (String s : stems)
         wordids.addAll(dictionary.getIndexWord(s, pos).getWordIDs());

      List<ISynset> synsets = new ArrayList<ISynset>();
      for (IWordID w : wordids)
         synsets.add(dictionary.getWord(w).getSynset());

      return synsets;
   }

   /**
     * Check if two words are synonymous, in any of their senses.
     *
     * Note that this is faster than calling getSynonyms and then checking for
     * overlaps in the lists (because it tests at the level of synsets, not
     * expanding out the words in each synset).
     */
   public boolean isSynonym(POS pos1, String word1, POS pos2, String word2)
   {
      List<ISynset> synsets1 = getSynsets(pos1, word1);
      List<ISynset> synsets2 = getSynsets(pos2, word2);
      for (ISynset s1 : synsets1)
         for (ISynset s2 : synsets2)
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
      List<ISynset> synsets = getSynsets(pos, word);

      List<IWord> words = new ArrayList<IWord>();
      for (ISynset s : synsets)
         words.addAll(s.getWords());

      List<String> synonyms = new ArrayList<String>();
      for (IWord w : words)
         synonyms.add(w.getLemma());

      return synonyms;
   }

   /**
     * Check whether word1 is a hypernym of word2 (equivalently: whether word2
     * is a hyponym of word1), either directly or via inheritance.
     *
     * Note that this is faster than calling getHypernyms and then checking for
     * presence in that list.
     */
   public boolean isHypernym(POS pos1, String word1, POS pos2, String word2)
   {
      List<ISynset> synsets1 = getSynsets(pos1, word1);
      List<ISynset> synsets2 = getSynsets(pos2, word2);

      for (ISynset s1 : synsets1)
         for (ISynset s2 : synsets2)
            if (isHypernym(s1, s2))
               return true;

      return false;
   }
   /* Internal implementation */
   private boolean isHypernym(ISynset syn1, ISynset syn2)
   {
      List<ISynsetID> hypernyms = syn2.getRelatedSynsets(Pointer.HYPERNYM);
      for (ISynsetID hypernym : hypernyms)
      {
         ISynset s = dictionary.getSynset(hypernym);
         if (s.equals(syn1))
            return true;
         if (isHypernym(syn1, s))
            return true;
      }
      return false;
   }

   /**
     * Get all the hypernyms of a word (including inherited ones).
     */
   public List<String> getHypernyms(POS pos, String word)
   {
      List<ISynset> synsets = getSynsets(pos, word);
      List<String> hypernyms = new ArrayList<String>();
      for (ISynset s : synsets)
      {
         List<ISynset> hyps = getHypernyms(s);
         for (ISynset hyp : hyps)
         {
            List<IWord> hypWords = hyp.getWords();
            for (IWord hypWord : hypWords)
               hypernyms.add(hypWord.getLemma());
         }
      }
      return hypernyms;
   }
   /* Internal implementation */
   private List<ISynset> getHypernyms(ISynset synset)
   {
      List<ISynset> hypernyms = new ArrayList<ISynset>();
      List<ISynsetID> targets = synset.getRelatedSynsets(Pointer.HYPERNYM);
      for (ISynsetID target : targets)
      {
         ISynset s = dictionary.getSynset(target);
         hypernyms.add(s);
         hypernyms.addAll(getHypernyms(s));
      }
      return hypernyms;
   }

   /**
     * Get all the hyponyms of a word (including inherited ones).
     */
   public List<String> getHyponyms(POS pos, String word)
   {
      // TODO: this is structurally identical to getHypernyms, factor out
      List<ISynset> synsets = getSynsets(pos, word);
      List<String> hyponyms = new ArrayList<String>();
      for (ISynset s : synsets)
      {
         List<ISynset> hyps = getHyponyms(s);
         for (ISynset hyp : hyps)
         {
            List<IWord> hypWords = hyp.getWords();
            for (IWord hypWord : hypWords)
               hyponyms.add(hypWord.getLemma());
         }
      }
      return hyponyms;
   }
   /* Internal implementation */
   private List<ISynset> getHyponyms(ISynset synset)
   {
      List<ISynset> hyponyms = new ArrayList<ISynset>();
      List<ISynsetID> targets = synset.getRelatedSynsets(Pointer.HYPONYM);
      for (ISynsetID target : targets)
      {
         ISynset s = dictionary.getSynset(target);
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

      List<ISynset> synsets1 = getSynsets(pos, word1);
      List<ISynset> synsets2 = getSynsets(pos, word2);
      int minDistance = maxHops;
      for (ISynset synset1 : synsets1)
      {
         for (ISynset synset2 : synsets2)
         {
            final int dist = wordDistance(synset1, synset2, maxHops);
            if (dist < minDistance)
               minDistance = dist;
         }
      }
      return minDistance;
   }
   /* Internal implementation */
   private int wordDistance(ISynset syn1, ISynset syn2, int maxHops)
   {
      if (maxHops < 1)
         return 0;
      
      if (syn1.equals(syn2))
         return 0;

      List<ISynsetID> hyperTargets = syn1.getRelatedSynsets(Pointer.HYPERNYM);
      List<ISynsetID> hypoTargets = syn1.getRelatedSynsets(Pointer.HYPONYM);
      int minDistance = maxHops;
      for (ISynsetID target : hyperTargets)
      {
         ISynset s = dictionary.getSynset(target);
         final int dist = 1 + wordDistance(s, syn2, minDistance-1);
         if (dist < minDistance)
            minDistance = dist;
      }
      for (ISynsetID target : hypoTargets)
      {
         ISynset s = dictionary.getSynset(target);
         final int dist = 1 + wordDistance(s, syn2, minDistance-1);
         if (dist < minDistance)
            minDistance = dist;
      }
      return minDistance;
   }

   /**
    * Finds the closest noun among a set of nouns to a query noun, according to
    * hypernymy/hyponymy relationships.
    *
    * Basically, follows hypernym/hyponym links, and returns the noun in the
    * query set that can be reached via the fewest links. Synonymy is not
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

      List<ISynset> synsets = getSynsets(NOUN, query);
      for (int i = 1; i <= maxHops; ++i)
      {
         for (ISynset synset : synsets)
         {
            String s = closestNounInSet(synset, set, i);
            if (s != null)
               return s;
         }
      }
      return null;
   }
   /* Internal implementation */
   private String closestNounInSet(ISynset query, Set<String> set, int maxHops)
   {
      List<IWord> words = query.getWords();
      for (IWord word : words)
         if (set.contains(word.getLemma()))
            return word.getLemma();

      if (maxHops < 1)
         return null;
      
      List<ISynsetID> hyperTargets = query.getRelatedSynsets(Pointer.HYPERNYM);
      List<ISynsetID> hypoTargets = query.getRelatedSynsets(Pointer.HYPONYM);

      for (int i = 1; i <= maxHops; ++i)
      {
         for (ISynsetID target : hyperTargets)
         {
            ISynset s = dictionary.getSynset(target);
            String closest = closestNounInSet(s, set, i-1);
            if (closest != null)
               return closest;
         }
         for (ISynsetID target : hypoTargets)
         {
            ISynset s = dictionary.getSynset(target);
            String closest = closestNounInSet(s, set, i-1);
            if (closest != null)
               return closest;
         }
      }
      return null;
   }
}
