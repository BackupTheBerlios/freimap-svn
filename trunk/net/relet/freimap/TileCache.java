package net.relet.freimap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
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
public class TileCache extends Thread {

	public static final String TILE_SERVER_URL = "http://tile.openstreetmap.org/mapnik/";

	HashMap<Long, Tile> cache = new HashMap<Long, Tile>();

	private ImageIcon REPLACEMENT;

	private LinkedList<Tile> loadQueue = new LinkedList<Tile>();

	private TilePainter tp;

	/**
	 * How often a tile paint is attempted before loading is triggered.
	 */
	private static final int MAX_PAINT_ATTEMPTS = 3;

	TileCache(TilePainter tp) {
		this.tp = tp;

		REPLACEMENT = new ImageIcon(ClassLoader
				.getSystemResource("gfx/loading.png"));

		setDaemon(true);

		start();

	}

	public void run() {
		while (true) {
			Tile t;

			synchronized (loadQueue) {
				while (loadQueue.isEmpty()) {
					try {
						loadQueue.wait();
					} catch (InterruptedException _) {
						// Unexpected.
					}
				}

				t = loadQueue.removeFirst();
			}

			System.err.println("fetching image:" + t.url);
			t.icon = new ImageIcon(t.url);
			if (t.icon == null)
			  t.paintAttempt = 0;
			else
			  t.url = null;

			tp.update();

		}
	}

	private void createTile(int zoom, int tx, int ty) {
		String tileName = zoom + "/" + tx + "/" + ty + ".png";

		try {
			URL url = new URL(TILE_SERVER_URL + tileName);
			Tile e = new Tile(url, tx, ty);

			cache.put(key(zoom, tx, ty), e);
		} catch (MalformedURLException e) {
			// Unexpected
			System.err.println("failed: " + e);
		}
	}

	private long key(long zoom, long tx, long ty) {
		return (zoom << 24) + (ty << 16) + tx;
	}

	void paintTiles(int zoom, int wx, int wy, int ww, int wh) {
		int max = (int) Math.pow(2, zoom) - 1;
		int x1 = OSMMercatorProjection.worldToTile(Math.max(wx, 0)); 
		int x2 = Math.min(OSMMercatorProjection.worldToTile(wx + ww), max); 
		int y1 = OSMMercatorProjection.worldToTile(Math.max(wy, 0)); 
		int y2 = Math.min(OSMMercatorProjection.worldToTile(wy + wh), max); 
		
		for (int ty = y1; ty <= y2; ty++) {
			for (int tx = x1; tx <= x2; tx++) {
				Tile tile = cache.get(key(zoom, tx, ty));
				if (tile == null) {
					createTile(zoom, tx, ty);
					tp.paint(REPLACEMENT.getImage(), tx << 8, ty << 8);
				} else if (tile.icon == null) {
					// Image is not there.
					
					// Postpone image loading a few times but then do it.
					if (tile.paintAttempt >= 0
							&& ++tile.paintAttempt >= MAX_PAINT_ATTEMPTS) {
						
						// Triggers image loading.
						synchronized (loadQueue) {
							loadQueue.addLast(tile);
							loadQueue.notifyAll();
						}
						
						// Mark tile as "load in progress"
						tile.paintAttempt = -1;
					}
					tp.paint(REPLACEMENT.getImage(), tx << 8, ty << 8);

				} else
					tp.paint(tile.icon.getImage(), tx << 8, ty << 8);

			}
		}
	}

}
