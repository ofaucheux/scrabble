package oscrabble.client;

import oscrabble.controller.MicroServiceDictionary;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Action displaying the definition of words in an own window.
 */
public class DisplayDefinitionAction extends AbstractAction
{
	private static DictionaryComponent component = null;

	private final MicroServiceDictionary dictionary;
	private final Supplier<Collection<String>> wordsSupplier;

	/**
	 * Component to position the window on.
	 */
	private Component relativeComponentPosition;

	public DisplayDefinitionAction(final MicroServiceDictionary dictionary, final Supplier<Collection<String>> wordsSupplier)
	{
		super("Show definitions");  // todo: i18n
		this.dictionary = dictionary;
		this.wordsSupplier = wordsSupplier;
	}

	public void setRelativeComponentPosition(final Component component)
	{
		this.relativeComponentPosition = component;
	}

	@Override
	public void actionPerformed(final ActionEvent e)
	{
		if (component == null)
		{
			component = new DictionaryComponent(this.dictionary);
		}
		final Collection<String> words = this.wordsSupplier.get();
		if (words != null)
		{
			words.forEach(word -> showDefinition(word));
		}
	}

	private void showDefinition(final String word)
	{
		Window dictionaryFrame = SwingUtilities.getWindowAncestor(component);
		if (dictionaryFrame == null)
		{
			dictionaryFrame = new JFrame(Playground.MESSAGES.getString("description"));
			dictionaryFrame.add(component);
			dictionaryFrame.setSize(600, 200);
			if (this.relativeComponentPosition != null)
			{
				final Point pt = this.relativeComponentPosition.getLocation();
				pt.translate(this.relativeComponentPosition.getWidth() + 10, 10);
				dictionaryFrame.setLocation(pt);
			}
		}


		component.showDescription(word);
		dictionaryFrame.setVisible(true);
		dictionaryFrame.toFront();
	}

}

