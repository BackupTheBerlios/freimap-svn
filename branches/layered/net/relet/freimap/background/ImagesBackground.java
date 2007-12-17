package net.relet.freimap.background;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import javax.swing.ImageIcon;

import net.relet.freimap.Configurator;

/**
 * A {@link Background} implementation which displays images at certain
 * geographical locations.
 * 
 * <p>The images are specified within the configuration file.</p>
 * 
 */
class ImagesBackground extends Background {

	Vector<Element> bgitems = new Vector<Element>();

	ImagesBackground() {
		int num = Configurator.getI("image.count");
		for (int i = 1; i <= num; i++) {
			String iname = Configurator.get("image." + i + ".gfx");
 			ImageIcon ii = new ImageIcon(ClassLoader.getSystemResource(iname));
			if (ii!=null) {
				bgitems.addElement(new Element(ii,				
					    Configurator.getD("image." + i + ".lon"), Configurator
							.getD("image." + i + ".lat"), Configurator
							.getD("image." + i + ".scale")));
			} else {
				System.err.println("Could not create background image: "+ iname);
                        }
		}
	}

	public void paint(Graphics2D g) {
		// draw backgrounds
		for (int i = 0; i < bgitems.size(); i++) {
			Element e = bgitems.elementAt(i);
			Image img = e.gfx.getImage();
			double w2 = img.getWidth(null) / 2;
			double h2 = img.getHeight(null) / 2;
			double rscale = converter.getScale() / e.scale;

			int xc = converter.worldToViewX((int) (converter.lonToWorld(e.lon) - w2
					* rscale));
			int yc = converter.worldToViewY((int) (converter.latToWorld(e.lat) - h2
					* rscale));

			g.drawImage(img,
					new AffineTransform(rscale, 0d, 0d, rscale, xc, yc), null);
		}
	}

	static class Element {
		  public ImageIcon gfx;
		  public double lon, lat, scale;

		  public Element (ImageIcon gfx, double lon, double lat, double scale) {
		    this.gfx=gfx;
		    this.lon=lon;
		    this.lat=lat;
		    this.scale=scale;
		  }
		  
	}
}
