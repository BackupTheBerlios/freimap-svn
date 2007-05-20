package net.relet.freimap.background;

import java.awt.Graphics2D;

import net.relet.freimap.ColorScheme;
import net.relet.freimap.Converter;

public abstract class Background {

	protected int zoom, x, y, width, height;
	
	protected Converter converter;
	
	public final void setConverter(Converter conv)
	{
		converter = conv;
	}

	public final void setDimension(int w, int h) {
		width = w;
		height = h;
	}

	public final void setWorld(int z, int x, int y) {
		zoom = z;
		this.x = x;
		this.y = y;
		worldUpdated();
	}
	
	protected void worldUpdated()
	{
		// To be overwritten by subclasses.
	}
	
	public ColorScheme getColorScheme()
	{
		return ColorScheme.NO_MAP;
	}

	public abstract void paint(Graphics2D g);

	public static Background createBlankBackground() {
		return new Background() {
			public void paint(Graphics2D g) {
			};
		};
	}

	public static Background createOpenStreetMapBackground() {
		return new OpenStreetMapBackground();
	}
	
	public static Background createImagesBackground()
	{
		return new ImagesBackground();
	}

	public static Background createBackground(String type) {
		if (type == null) {
			System.err.println("warning: no background specified. Defaulting to blank.");
			return createBlankBackground();
		}

		if (type.equalsIgnoreCase("blank"))
			return createBlankBackground();

		if (type.equalsIgnoreCase("images"))
			return createImagesBackground();

		if (type.equalsIgnoreCase("openstreetmap"))
			return createOpenStreetMapBackground();

		System.err.println("warning: no valid background specified (" + type
				+ "). Defaulting to blank.");
		return createBlankBackground();
	}

}
