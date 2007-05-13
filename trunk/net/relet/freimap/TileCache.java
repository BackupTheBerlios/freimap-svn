package net.relet.freimap;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.ImageIcon;

/**
 * In a distant future this class will be a fully featured cache for downloaded
 * tiles. It will allow retrieving tiles (based on their zoom, x and y
 * specifier), delayed downloading and automatic discarding tiles (when memory
 * gets sparse).
 * 
 * @author Robert Schuster
 * 
 */
public class TileCache {

	public static final String TILE_SERVER_URL = "http://tile.openstreetmap.org/mapnik/";
	
	HashMap<Integer, LinkedList<Tile>> cache = new HashMap<Integer, LinkedList<Tile>>();

	void requestTile(final int zoom, final int tx, final int ty) {
		Thread t = new Thread() {
			public void run() {
				try {

					String tileName = zoom + "/" + tx + "/" + ty + ".png";
					System.out.println("trying: " + tileName);

					URL url = new URL(TILE_SERVER_URL + tileName);
					ImageIcon icon = new ImageIcon(url);
					Tile e = new Tile(icon, tx * 256, ty * 256);
					
					putTile(zoom, e);

					System.out.println("success: " + tileName);
				} catch (Exception e) {
					System.err.println("failed: " + e);
				}
			}
		};

		t.start();
	}
	
	private void putTile(int zoom, Tile tile)
	{
		LinkedList ll = cache.get(zoom);
		
		if (ll == null)
		{
			ll = new LinkedList<Tile>();
			cache.put(zoom, ll);
		}
		
		ll.add(tile);
	}
	
	Iterator<Tile> tileIterator(int zoom)
	{
		LinkedList ll = cache.get(zoom);
		
		if (ll == null || ll.isEmpty())
			return null;
		
		return ll.iterator();
	}

}
