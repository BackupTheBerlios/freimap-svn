/* net.relet.freimap.FreiNode.java 

  This file is part of the freimap software available at freimap.berlios.de

  This software is copyright (c)2007 Thomas Hirsch <thomas hirsch gmail com>

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License with
  the Debian GNU/Linux distribution in file /doc/gpl.txt
  if not, write to the Free Software Foundation, Inc., 59 Temple Place,
  Suite 330, Boston, MA  02111-1307  USA
*/

package net.relet.freimap;

import java.io.Serializable;

public class FreiNode implements Comparable, Serializable {
  public String id;
  public double lon=Double.NaN, lat=Double.NaN;
  public double lonsum=0, latsum=0; //used only for real time interpolation
  public int nc=0;
  public boolean unlocated=false;
  
  public FreiNode() {} //serializable

  public FreiNode(String id) {
    this.id=id;
    
    lat = 52.520869;// + Math.random()/50-0.01; //when no coordinates are known, generate some around the center of the cbase
    lon = 13.409457;// + Math.random()/50-0.01; //cbase center +- random 0.01
    unlocated=true; 
  }
  public FreiNode(String id, double lon, double lat) {
    this.id=id;
    this.lon=lon;
    this.lat=lat;
  }
  public int compareTo(Object o) {
    return id.compareTo(((FreiNode)o).id);
  }
  public boolean equals(Object o) {
    if (!(o instanceof FreiNode)) return false;
    return this.id.equals(((FreiNode)o).id);
  }
  public String toString() {
    return id;
  }
}