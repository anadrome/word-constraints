/**
  * A combobox that groups its options with separators.
  *
  * Pass it an Object[][] nested array with the grouped items.
  *
  * @author Mark J. Nelson, adapted from example by Brian Duff
  * @date 2004, 2008
  */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class SepComboBox extends JComboBox
{
   private Object[] _items;

   public SepComboBox(Object[][] options)
   {
      int numItems = options.length - 1;
      for (int i = 0; i < options.length; ++i)
         numItems += options[i].length;
      _items = new Object[numItems];

      int pos = 0;
      for (int i = 0; i < options.length; ++i)
      {
         if (i > 0)
         {
            _items[pos] = new ComboItem(pos);
            ++pos;
         }
         for (int j = 0; j < options[i].length; ++j)
         {
            _items[pos] = new ComboItem(pos, options[i][j]);
            ++pos;
         }
      }

      setModel(createModel());
      setRenderer(createRenderer());
      setSelectedIndex(0);
   }

   private ComboBoxModel createModel()
   {
      return new NaiiveComboModel();
   }

   private class NaiiveComboModel extends AbstractListModel 
         implements ComboBoxModel
   {
      private ComboItem m_selectedItem;

      private Object[] getItems()
      {
         return _items;
      }

      public Object getElementAt( int index )
      {
         return getItems()[ index ];
      }

      public int getSize()
      {
         return getItems().length;
      }

      public void setSelectedItem( Object o )
      {
         if ( o instanceof ComboItem )
         {
            // If the user tries to select a separator...
            if ( ((ComboItem)o).getDelegate() == null )
            {
               if ( m_selectedItem != null )
               {
                  int oldIndex = ((ComboItem)m_selectedItem).getIndex();
                  int newIndex = ((ComboItem)o).getIndex();

                  if ( newIndex < oldIndex )
                  {
                     // Select the item before the separator.
                     if ( newIndex - 1 >= 0 )
                     {
                        m_selectedItem = (ComboItem)getItems()[ newIndex - 1 ];
                     }
                  }
                  else if ( newIndex > oldIndex )
                  {
                     // Select the item after the separator.
                     if ( newIndex + 1 < getItems().length )
                     {
                        m_selectedItem = (ComboItem)getItems()[ newIndex + 1 ];
                     }
                  }

               }
            }
            else
            {
               m_selectedItem = (ComboItem) o;
            }

            super.fireContentsChanged( this, -1, -1 );
         }
      }

      public Object getSelectedItem()
      {
         if (m_selectedItem == null)
            return null;
         return m_selectedItem.getDelegate();
      }

   }

   private class ComboItem
   {
      private final Object _delegate;
      private final int _index;

      /**
       * Constructor for a separator.
       */
      private ComboItem( int index )
      {
         this( index, null );
      }

      /**
       * Constructor for a normal item.
       * 
       * @param index the index of this item in the model.
       * @param delegate the actual item in the combo box, null for separators
       */
      private ComboItem( int index, Object delegate )
      {
         _index = index;
         _delegate = delegate;  
      }

      private Object getDelegate()
      {
         return _delegate;
      }

      private int getIndex()
      {
         return _index;
      }
   }

   private ListCellRenderer createRenderer()
   {
      return new DefaultListCellRenderer()
      {
         private final Component SEPARATOR = new SeparatorComponent();

         public Component getListCellRendererComponent( JList list,
               Object value, int index, boolean isSelected, boolean cellHasFocus)
         {
            if ( value instanceof ComboItem )
            {
               value = ((ComboItem)value).getDelegate();
            }

            if ( value == null )
            {
               return SEPARATOR;
            }
            return super.getListCellRendererComponent( list, value, index, 
                  isSelected, cellHasFocus );
         }
      };
   }

   /**
    * A separator component, used by the renderer. A more mature implementation
    * would override a whole bunch of methods for performance reasons, just like
    * DefaultListCellRenderer
    */
   private class SeparatorComponent extends JComponent
   {
      private final Dimension PREFERRED_SIZE = new Dimension( 5, 7 );
      private final int LINE_POS = 4;

      private SeparatorComponent()
      {
         setOpaque( false );
      }

      public Dimension getPreferredSize()
      {
         return PREFERRED_SIZE;
      }

      public void paintComponent( Graphics g )
      {
         g.setColor( Color.BLACK );    // @TODO: That's evil.
         g.drawLine( 0, LINE_POS, getWidth(), LINE_POS );
      }
   }
}
