package oscrabble.client.ui;

import lombok.Data;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import oscrabble.player.ai.AIPlayer;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class AIPlayerConfigPanelTest {

	/**
	 * test disabled because requires user interaction.
	 */
	@Disabled
	@Test
	void displayPanel() {

		System.out.println("TEST");
		final PropertyObject properties = new PropertyObject();
		properties.value = 2;

		final ArrayList<AIPlayerConfigPanel> panels = new ArrayList<>();
		panels.add(new AIPlayerConfigPanel(properties));
		panels.add(new AIPlayerConfigPanel(properties));

		Executors.newScheduledThreadPool(8).scheduleAtFixedRate(
				() -> panels.forEach(p -> p.refreshContent()),
				0,
				2,
				TimeUnit.SECONDS
		);

		final JPanel demoPanel = new JPanel(new GridLayout(0, 2));
		panels.forEach(p -> demoPanel.add(p));
		JOptionPane.showConfirmDialog(null, demoPanel);
		System.out.println(properties);
	}

	@Data
	private static class PropertyObject {
		boolean chosen;
		String text;
		int value;
		LocalDate date;
		Month month;
		AIPlayer.Level level;
	}
}