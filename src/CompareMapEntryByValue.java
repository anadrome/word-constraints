import java.util.Comparator;
import java.util.Map;

/**
  * Compare two map entries by value.
  *
  * Used for the common but inexplicably impossible to do in standard Java
  * task of sorting a map by its values.
  *
  * @author Mark J. Nelson
  * @date 2006
  */
class CompareMapEntryByValue<T, S extends Comparable<? super S>>
   implements Comparator<Map.Entry<T, S>>
{
   public int compare(Map.Entry<T,S> lhs, Map.Entry<T,S> rhs)
   {
      return lhs.getValue().compareTo(rhs.getValue());
   }

   public boolean equals(Map.Entry<T,S> lhs, Map.Entry<T,S> rhs)
   {
      return lhs.getValue().equals(rhs.getValue());
   }
}
