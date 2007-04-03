/* net.relet.freimap.DataSource.java 

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

import java.util.*;

public interface DataSource {
  public Vector<FreiNode> getNodeList();
  public Hashtable<String, Float> getNodeAvailability(long time);
  public long getLastUpdateTime();
  public long getFirstUpdateTime();
  public long getClosestUpdateTime(long time);
  public Vector<FreiLink> getLinks(long time);
  //threaded information fetching
  public void addDataSourceListener(DataSourceListener dsl);
  //some optional methods
  public void getLinkProfile(FreiLink link, LinkInfo info); 
  public void getLinkCountProfile(FreiNode node, NodeInfo info); 
}
