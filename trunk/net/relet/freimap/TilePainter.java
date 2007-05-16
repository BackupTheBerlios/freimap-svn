package net.relet.freimap;

import java.awt.Graphics;
import java.awt.Image;

public interface TilePainter {
	
	void update();
	
	void paint(Image image, int worldX, int worldY);

	void setGraphics(Graphics g);
	
}
