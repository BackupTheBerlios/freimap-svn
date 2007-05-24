/* net.relet.freimap.BackgroundElement.java

  This file is part of the freimap software available at freimap.berlios.de

  This software is copyright (c)2007 Thomas Hirsch <thomas hirsch gmail com>

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License with
  the Debian GNU/Linux distribution in file /doc/gpl.txt
  if not, write to the Free Software Foundation, Inc., 59 Temple Place,
  Suite 330, Boston, MA 02111-1307 USA
*/

package net.relet.freimap.background;


import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;

import java.security.*;
import java.util.Formatter;

import javax.imageio.ImageIO;
import net.relet.freimap.Configurator;

public class Tile {
  Image image;
  URL url;
  
  final int x, y;
  
  final int zoom;
  
  long startTime = -1;

  /**
   * Where to cache background tiles, or null if disabled.
   */
  static String cacheDir = Configurator.get("background.osm.cache.dir");
  /**
   * Background colour filter to be applied
   */
  static String filter = Configurator.get("background.osm.filter");
  final static String FILTER_DARK = "dark";
  /**
   * Amount of time to pass until a tile is actually loaded.
   */
  final static long LOAD_TIMEOUT = Configurator.getI("background.osm.delay"); 
   

  /**
   * Amount of KiB an image consumes in memory.
   * 
   */
  final static int IMAGE_SIZE_KiB = (256 * 256 * 3) / 1024; 
  
  static enum State
  {
	  CREATED,    // Freshly created, no image present 
	  LOADED,     // Image loaded
	  SCHEDULED,  // Scheduled for loading the image
	  WAITING     // Counting age to determine load scheduling
  }
  
  State state;
  

  public Tile (URL url, int zoom, int x, int y) {
    this.url = url;
    this.zoom = zoom;
    this.x = x;
    this.y = y;
    
    state = State.CREATED;
  }
  
  boolean shouldLoad()
  {
	  if (state == State.CREATED)
	  {
	    startTime = System.currentTimeMillis();
		  state = State.WAITING;
	  } else if(state == State.WAITING)
	  {
		  if (System.currentTimeMillis() - startTime > LOAD_TIMEOUT)
		  {
			  state = State.SCHEDULED;
			  return true;
		  }
	  }
	  
	  return false;
  }
  
  void loadImage()
  {
	BufferedImage bfimage = null;
	  try {
            String hash = getMD5Hash(url.toString());
            File cacheFile = new File(cacheDir + "/" + hash);
	    if (cacheDir != null) {
	      //query disk cache before loading
              try {
  	        bfimage = ImageIO.read(cacheFile);
		//if (bfimage != null) System.out.println("got tile from cache!");
              } catch (Exception _) {
              }
            }
	    //else query server
	    if (bfimage == null) bfimage = ImageIO.read(url);
            //modify colors to match color scheme
	    if (filter.equals(FILTER_DARK)) image = makeImageBlack(bfimage);
            else image=bfimage;
	    state = State.LOADED;
            if (cacheDir != null) 
	      try {
                ImageIO.write(bfimage, "png", cacheFile);
	      } catch (Exception ex) {
                System.err.println(ex.getMessage());
              };
	  }
	  catch (IOException _)
	  {
		  System.err.println("failed: " + url);
		  state = State.CREATED;
		  startTime = System.currentTimeMillis();
	  }
  }

  public static String getMD5Hash(String in) {
    StringBuffer result = new StringBuffer(32);
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.update(in.getBytes());
      Formatter f = new Formatter(result);
      for (byte b : md5.digest()) {
        f.format("%02x", b);
      }
    } catch (NoSuchAlgorithmException ex) {
      ex.printStackTrace();
    }
    return result.toString();
  }
  
  static ImageFilter subtractWhite = new RGBImageFilter() {
    final static int OPAQUE = 0xff000000;
    final static int RGB    = 0x00ffffff;
    final static int WHITE  = 0xffffffff;
    public final int filterRGB(int x, int y, int argb) {
      int rgb = argb & RGB;
      int r = (rgb & 0xff0000) >> 16;
      int g = (rgb & 0x00ff00) >> 8;
      int b = (rgb & 0x0000ff);
      if ((r<96) && (g<96) && (b<96)) return OPAQUE + (RGB - rgb);
      int min = Math.min(r,Math.min(g,b)); 
      return ((0xff - min) << 24) + rgb;
    }
  };
	
  static Image makeImageBlack (Image i) {
    if ((i == null) || (i.getSource() == null)) return null;
    ImageProducer ip = new FilteredImageSource(i.getSource(), subtractWhite);
    return Toolkit.getDefaultToolkit().createImage(ip);
  }


  /**
   * Returns an image of the current tile.
   * 
   * The returned image may denote the condition the current tile
   * is in. E.g. if it is loading a "loading" image is displayed.
   * 
   * @return
   */
  Image getImage()
  {
	  // TODO: Implement me.
	  return null;
  }
  
}
