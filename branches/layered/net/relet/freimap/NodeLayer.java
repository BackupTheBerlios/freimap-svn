/* net.relet.freimap.VisorLayer.java

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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

import javax.swing.border.LineBorder;

public class NodeLayer implements VisorLayer, DataSourceListener {

  Converter converter;

  double scale;  // current scaling
  int zoom;       // zoom factor, according to OSM tiling
  int w, h;    //screen width, hight
  int cx, cy; //center of screen

  Vector<FreiNode> nodes; //vector of known nodes
  Vector<FreiLink> links; //vector of currently displayed links
  Hashtable<String, Float> availmap; //node availability in percent (0f-1f)
  Hashtable<String, NodeInfo> nodeinfo=new Hashtable<String, NodeInfo>(); //stores nodeinfo upon right click
  Hashtable<FreiLink, LinkInfo> linkinfo=new Hashtable<FreiLink, LinkInfo>(); //stores linkinfo upon right click

  //FIXME the following paragraph is identical and static in VisorFrame. Use these definitions and remove the paragraph.
  public static Font mainfont = new Font("SansSerif", 0, 12),
                     smallerfont = new Font("SansSerif", 0, 9);
  public static Color fgcolor = new Color(20,200,20),     //used for text, lines etc., accessed globally! FIXME move these into colorscheme!
                bgcolor = new Color(64,128,64,196),       //used for transparent backgrounds of most status boxes
                fgcolor2 = new Color(150,150,255),       //used for foreground of link status boxes
                bgcolor2 = new Color(40,40,192,196);       //used for transparent backgrounds of link status boxes
  ColorScheme cs = ColorScheme.NO_MAP;

  DataSource source;

  FreiNode selectedNode;
  FreiLink selectedLink;
  double selectedNodeDistance,
         selectedLinkDistance;

  private FreiNode uplink = new FreiNode("0.0.0.0/0.0.0.0");

  private boolean transparent = true;

  long crtTime;


  public NodeLayer(DataSource source) {
    this.source=source;
    this.source.addDataSourceListener(this);

    System.out.println("reading node list.");
    nodes=source.getNodeList();
    System.out.println("reading node availability.");
    availmap=source.getNodeAvailability(0);
    System.out.print("reading link list.");
    long now = System.currentTimeMillis();
    links = new Vector<FreiLink>();//source.getLinks(firstUpdateTime);
    System.out.println("("+(System.currentTimeMillis()-now)+"ms)");
    
  }

  /* datasourcelistener */
  public void timeRangeAvailable(long from, long until) {
    //obsolete.
  }
  public void nodeListUpdate(FreiNode node) {
    if (nodes!=null) { //this really should not happen
      nodes.add(node);
    }
  }

  /**
   * returns the DataSource of this layer. If the layer is just decorative, returns null.
   * 
   * @return null or DataSource
   */

  public DataSource getSource() {
    return source;
  }

  /**
   * Paints the layer.
   * 
   * @param g, a Graphics2D object.
   */
  public void paint(Graphics2D g) {
    if (!transparent) {
      g.setColor(cs.getColor(ColorScheme.Key.MAP_BACKGROUND));
      g.fillRect(0,0,w,h);
    } 

    g.setFont(VisorFrame.mainfont);

    //draw links
    Stroke linkStroke = new BasicStroke((float)(Math.min(5,0.00005 * scale)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    Stroke cableStroke = new BasicStroke((float)(Math.min(15,0.00015 * scale)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    Stroke selectedStroke = new BasicStroke((float)(Math.min(30,0.00030 * scale)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    //draw selected link extra thick
    if (selectedLink != null) {
      g.setStroke(selectedStroke);
      g.setColor(fgcolor2);
      if (selectedLink.to.equals(uplink)) {
        double nsize = Math.min(45,Math.round(0.0015 * scale));
        g.drawOval((int)(converter.lonToViewX(selectedLink.from.lon)-nsize/2), (int)(converter.latToViewY(selectedLink.from.lat)-nsize/2), (int)(nsize), (int)(nsize));
      } else {
        g.drawLine(converter.lonToViewX(selectedLink.from.lon),
        	   converter.latToViewY(selectedLink.from.lat),
        	   converter.lonToViewX(selectedLink.to.lon), 
        	   converter.latToViewY(selectedLink.to.lat));
      }
    }

    //draw other links 
    g.setStroke(linkStroke);
    if ((links != null) && (links.size()>0)) {
      for(int i = 0; i < links.size(); i++) {
        FreiLink link = (FreiLink)links.elementAt(i);
        if (link.to.equals(uplink)) {
          g.setColor(Color.cyan);
          g.setStroke(cableStroke);
          double nsize = Math.min(45,Math.round(0.0015 * scale));
          g.drawOval((int)(converter.lonToViewX(link.from.lon)-nsize/2), (int)(converter.latToViewY(link.from.lat)-nsize/2), (int)(nsize), (int)(nsize));
          g.setStroke(linkStroke);
        } else {
          float green = 1;
          if (link.HNA || (link.etx < 0)) {
            g.setColor(Color.cyan);
          } else if (link.etx<1) {
            g.setColor(Color.white);
          } else {
            green=1/link.etx;
            g.setColor(new Color(1-green, green, 0.5f));
          }
          if ((link.from.lat != link.from.DEFAULT_LAT) && (link.to.lat != link.to.DEFAULT_LAT)) //ignore links to truly unlocated nodes (at default position)
            g.drawLine(converter.lonToViewX(link.from.lon),
        		  converter.latToViewY(link.from.lat),
        		  converter.lonToViewX(link.to.lon), 
        		  converter.latToViewY(link.to.lat));
          if (link.to.unlocated && (link.from.lat != link.from.DEFAULT_LAT)) {
            double netx = (link.etx<1)?0d:1d/link.etx;
            link.to.lonsum+=link.from.lon*netx;
            link.to.latsum+=link.from.lat*netx;
            link.to.nc+= netx;
          }
          if (link.from.unlocated && (link.to.lat != link.to.DEFAULT_LAT)) {
            double netx = (link.etx<1)?0d:1d/link.etx;
            link.from.lonsum+=link.to.lon * netx;
            link.from.latsum+=link.to.lat * netx;
            link.from.nc+= netx;
          }
        }
      }
    }

    //draw nodes
    g.setColor(cs.getColor(ColorScheme.Key.NODE_UPLINK));
    for (int i=0; i<nodes.size(); i++) {
      FreiNode node=(FreiNode)nodes.elementAt(i);
      if (node.equals(uplink)) continue;
      if (node.unlocated) {
        g.setColor(cs.getColor(ColorScheme.Key.NODE_UNLOCATED));
      } else if (availmap!=null) {
        Object oavail = availmap.get(node.id);
        if (oavail==null) {
          g.setColor(cs.getColor(ColorScheme.Key.NODE_UNAVAILABLE));
        } else {
          float avail = (float)Math.sqrt(((Float)oavail).floatValue()); 
          g.setColor(new Color(1.0f-avail, avail, 0.5f));
        }
      }
      double nsize = Math.max(1,Math.min(15,Math.round(0.0003 * scale)));
      double nx = converter.lonToViewX(node.lon) - nsize/2,
             ny = converter.latToViewY(node.lat) - nsize/2;
             
      g.fillOval((int)nx, (int)ny, (int)nsize, (int)nsize);
      if (node.unlocated) {
        if (node.nc > 1) {
          node.lon=node.lonsum / node.nc;
          node.lat=node.latsum / node.nc;
        } else if (node.nc == 1) {
          node.lon=node.lonsum + 0.0003;
          node.lat=node.latsum + 0.0003;
        } /*else {
          System.err.println("Node unlocated with no neighbours: "+node.id);
        }*/
        node.lonsum=0; node.latsum=0; node.nc=0;
      }
    }

    //draw highlight
    if ((selectedNode != null)||(selectedLink != null)) {
    	g.setPaint(fgcolor);
    	g.setStroke(new BasicStroke((float)1f));

        double nx=0, ny=0;
        boolean showNodeInfo = true;

        //draw selected node
        if (selectedNode != null) {
    	  double nsize = Math.min(30,Math.round(0.0006 * scale));
    	  nx = converter.lonToViewX(selectedNode.lon);
          ny = converter.latToViewY(selectedNode.lat);
    	  g.draw(new Ellipse2D.Double(nx - nsize/2, ny - nsize/2, nsize, nsize));
        } 
        
        if ((selectedLink!=null) && (selectedLinkDistance < selectedNodeDistance)) {
          if (selectedLink.to.equals(uplink)) {
            nx = converter.lonToViewX(selectedLink.from.lon);
            ny = converter.latToViewY(selectedLink.from.lat);
          } else {
            nx = converter.lonToViewX((selectedLink.from.lon + selectedLink.to.lon)/2);
            ny = converter.latToViewY((selectedLink.from.lat + selectedLink.to.lat)/2);
            int oof = 8; //an obscure offscreen compensation factor
            int ooc = 0;
            while ((ooc<10)&&((nx<0)||(ny<0)||(nx>w)||(ny>h))) { //do not draw boxes offscreen too easily
              nx = converter.lonToViewX((selectedLink.from.lon * (oof-1) + selectedLink.to.lon)/oof);
              ny = converter.latToViewY((selectedLink.from.lat * (oof-1) + selectedLink.to.lat)/oof);
              oof *= 8;
              ooc++;
            }
          }
          showNodeInfo = false; //show linkInfo instead
          g.setPaint(fgcolor2);
    	}

        double boxw;

        String label;
        Vector<String> infos= new Vector<String>();
        if (showNodeInfo) {
          label = "Node: "+selectedNode.fqid;
        	boxw = Math.max(180, g.getFontMetrics(VisorFrame.mainfont).stringWidth(label)+20);

          Float favail = availmap.get(selectedNode.id);
	  String savail=(favail==null)?"N/A":Math.round(favail.floatValue()*100)+"%";
	  infos.add ("Availability: "+savail);

	  NodeInfo info = nodeinfo.get(selectedNode.id);
          if (info!=null) {
	    if (info.status == info.STATUS_AVAILABLE) {
	      infos.add("min. links: " + info.minLinks );
	      infos.add("max. links: " + info.maxLinks );

	      if (info.linkCountChart != null) {
		  info.linkCountChart.draw(g, new Rectangle2D.Float(20, 80, 250, 150));
	      }
            } else if (info.status == info.STATUS_FETCHING) {
              infos.add("retrieving information");
	    }
	  } else {
	    infos.add("+ right click for more +");
	  }
        } else {
          boxw = g.getFontMetrics(VisorFrame.mainfont).stringWidth("Link: 999.999.999.999 -> 999.999.999.999/999.999.999.999");

          label = "Link: "+selectedLink.toString();
          LinkInfo info = linkinfo.get(selectedLink);
          if (info != null) {
            if (info.status==info.STATUS_AVAILABLE) { 
	      if (info.linkChart != null) {
		  info.linkChart.draw(g, new Rectangle2D.Float(20, 80, 250, 150));
	      }
            } else if (info.status == info.STATUS_FETCHING) {
              infos.add("retrieving information");
            }
	  } else {
	    infos.add("+ right click for more +");
	  }
        }

        // Put box at fixed location.
        double boxx = w - 10 - boxw / 2;
        double boxy = 100;

               double labelh = g.getFontMetrics(VisorFrame.mainfont).getHeight(),
	       infoh = g.getFontMetrics(smallerfont).getHeight(),
               boxh = labelh + infoh * infos.size() + 10;


	    // Connect with the bottom line of the box.
		g.draw(new Line2D.Double(nx, ny, boxx, boxy+boxh/2));

        Shape box = new RoundRectangle2D.Double(boxx-boxw/2, boxy-boxh/2, boxw, boxh, 10, 10);
        Color mybgcolor = showNodeInfo?bgcolor:bgcolor2;
	Color myfgcolor = showNodeInfo?fgcolor:fgcolor2;
	g.setPaint(mybgcolor);
    	g.fill(box); 
        g.setPaint(myfgcolor);
    	g.draw(box);
	g.setColor(showNodeInfo?Color.green:Color.cyan); 
        g.setFont(VisorFrame.mainfont);
	g.drawString(label, (int)(boxx - boxw/2 + 10), (int)(boxy - boxh/2 + labelh));
	g.setColor(myfgcolor); 
	g.setFont(smallerfont);
	for (int i=0; i<infos.size(); i++) {
		g.drawString(infos.elementAt(i), (int)(boxx - boxw/2 + 10), (int)(boxy - boxh/2 + labelh + infoh*i + 15));
	}
    }


  }

  public FreiNode getSelectedNode() {
    return selectedNode;
  }

  public double sqr(double x) { 
    return x*x;
  }
  public double dist (double x1, double x2, double y1, double y2) {
    return Math.sqrt(sqr(x1-x2)+sqr(y1-y2));
  }
  public FreiLink getClosestLink(double lon, double lat) {
    if (links==null) return null;
    double dmin=Double.POSITIVE_INFINITY;
    FreiLink closest=null, link;
    boolean within;
    for (int i=0; i<links.size(); i++) {
      link=links.elementAt(i);
      within=true;
      if (link.from.lon < link.to.lon) { 
        if ((lon < link.from.lon) || (lon > link.to.lon)) within = false;
      } else {
        if ((lon > link.from.lon) || (lon < link.to.lon)) within = false;
      }
      if (link.from.lat < link.to.lat) { 
        if ((lat < link.from.lat) || (lat > link.to.lat)) within = false;
      } else {
        if ((lat > link.from.lat) || (lat < link.to.lat)) within = false;
      }
      if (within) {
        if (dist(lat, link.from.lat, lon, link.from.lon) > dist(lat, link.to.lat, lon, link.to.lon)) continue; 
           //we will then select the other link direction.
        double x1 = link.from.lat, 
               x2 = link.to.lat,
               y1 = link.from.lon,
               y2 = link.to.lon;
        double d = Math.abs((x2-x1)*(y1-lon) - (x1-lat)*(y2-y1)) / Math.sqrt(sqr(x2-x1)+sqr(y2-y1));
        if (d<dmin) {
	  dmin=d;
	  closest=link;
        }
      }
    }
    selectedLinkDistance=dmin;
    return closest;
  }

  public FreiNode getClosestNode(double lon, double lat) {
    double dmin=Double.POSITIVE_INFINITY;
    FreiNode closest=null, node;
    for (int i=0; i<nodes.size(); i++) {
      node=nodes.elementAt(i);
      double d = Math.abs(node.lon - lon) + Math.abs(node.lat - lat); //no need to sqrt here
      if (d<dmin) {
	      dmin=d;
	      closest=node;
      }
    }
    selectedNodeDistance = (closest==null)?dmin:dist(closest.lon, lon, closest.lat, lat); //recalculate exact distance
    return closest;
  }


  /**
   * Indiciates whether this VisorLayer instance is transparent. 
   * 
   * @return true
   */

  public boolean isTransparent() {
    return transparent;
  }

  /**
   * Attempts to set transparency to this VisorLayer.
   */

  public void setTransparent(boolean t) {
    this.transparent=t;
  }

 /**
   * Sets the scaling converter for this background.
   */

  public void setConverter(Converter c) {
    this.converter = c;
  }

 /**
  * Sets the width and height of the section the layer is
  * showing.
  * 
  * <p>This method must be called whenever the size changes
  * otherwise calculations will get incorrect and drawing problems
  * may occur.</p>
  * 
  * @param w
  * @param h
  */
 public void setDimension(int w, int h) {
   this.w=w; this.h=h;
   cx = w/2; cy = h/2;
 }

 /**
  * Sets the <code>VisorLayer</code>s zoom.
  * 
  * <p>This method must be called whenever the zoom changes
  * otherwise calculations will get incorrect and drawing problems
  * may occur.</p>
  * 
  * @param zoom
  */
 public void setZoom(int zoom) {
   this.zoom=zoom;
   this.scale=converter.getScale();
 }

 /**
  * Sets the current point in time to be displayed
  * 
  * @param crtTime, an unix time stamp
  * @return true, if the layer has to be repainted
  */
 public boolean setCurrentTime(long crtTime) {
   long adjusted=source.getClosestUpdateTime(crtTime);
   if (adjusted != this.crtTime) {
     links = source.getLinks(this.crtTime);
     this.crtTime = adjusted;
     return true;
   }
   return false;
 }

 public void mouseMoved(double lat, double lon) {
   if ((lon==0) && (lat==0)) {
     selectedNode = null;
     selectedLink = null;
   } else {
     selectedNode = getClosestNode(lon, lat); 
     selectedLink = getClosestLink(lon, lat);
     if (selectedNodeDistance * scale < 10) selectedLinkDistance=Double.POSITIVE_INFINITY; //when close to a node, select a node
   }
 }
 public void mouseClicked(double lat, double lon, int button) {
   if (button==MouseEvent.BUTTON3) {
     if (selectedNodeDistance < selectedLinkDistance) {
       if (selectedNode != null) {
         NodeInfo info=new NodeInfo();
         source.getLinkCountProfile(selectedNode, info);
         nodeinfo.put(selectedNode.id, info);
       }
     } else if (selectedLink != null) {
       LinkInfo info=new LinkInfo();
       source.getLinkProfile(selectedLink, info);
       linkinfo.put(selectedLink, info);
     } 
   } 
 }
}
