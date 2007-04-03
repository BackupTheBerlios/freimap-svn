/* net.relet.freimap.FreiLink.java

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

package net.relet.freimap;

public class FreiLink {
  public FreiNode from, to;
  public float etx=-1;
  public boolean HNA=false;
  public FreiLink(FreiNode from, FreiNode to, float etx) {
    this(from,to,etx,false);
  }
  public FreiLink(FreiNode from, FreiNode to, float etx, boolean HNA) {
    this.from=from;
    this.to=to;
    this.etx=etx;
    this.HNA=HNA;
  }
  public int hashCode() {
    return from.hashCode() ^ to.hashCode();
  }
  public boolean equals(Object o) {
    if (!(o instanceof FreiLink)) return false;
    FreiLink other=(FreiLink)o;
    if ((this.from == null)||(this.to == null)) return false;
    return this.from.equals(other.from) && this.to.equals(other.to);
  }
  public String toString() {
    return from+" -> "+to;
  }
}
