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

import java.awt.Image;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

public class Tile {
  Image image;
  URL url;
  
  final int x, y;
  
  final int zoom;
  
  long startTime = -1;
  
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
  
  /**
   * Amount of time to pass until a tile is actually loaded.
   */
  final static long LOAD_TIMEOUT = 2500; 

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
	  try
	  {
	    image = ImageIO.read(url);
	    state = State.LOADED;
	  }
	  catch (IOException _)
	  {
		  System.err.println("failed: " + url);
		  state = State.CREATED;
		  startTime = System.currentTimeMillis();
	  }
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
