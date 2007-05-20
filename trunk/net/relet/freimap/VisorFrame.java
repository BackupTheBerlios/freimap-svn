/* net.relet.freimap.VisorFrame.java

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.relet.freimap.background.Background;

import org.ho.yaml.Yaml;

/*
todo dimension -> configfile
     sensible method names
     rename x and y into lon and lat
*/

public class VisorFrame extends JPanel implements DataSourceListener, ComponentListener, ActionListener, MouseListener, MouseMotionListener, MouseWheelListener {
  double scale=1.0d; // current scaling
  
  int zoom = 0;
  
  Vector<FreiNode> nodes; //vector of known nodes
  Vector<FreiLink> links; //vector of currently displayed links
  Hashtable<String, Float> availmap; //node availability in percent (0f-1f)
  Hashtable<String, NodeInfo> nodeinfo=new Hashtable<String, NodeInfo>(); //stores nodeinfo upon right click
  Hashtable<FreiLink, LinkInfo> linkinfo=new Hashtable<FreiLink, LinkInfo>(); //stores linkinfo upon right click

  Image buf; //double buffer
  int w=800,h=600; //screen width, hight
  int cx=400, cy=300; //center of screen
  int timelinex0=w/3, timelinex1=11*w/12;
  long firstUpdateTime, crtTime, oldTime=0, lastUpdateTime, 
       firstAvailableTime=-1, lastAvailableTime=-1;
  boolean playing=false;
  //first, lastupdatetime = min and max of time interval as reported by datasource - data may however only be retrieved in the range below
  //first, lastavailabletime = available range of time interval
  //crttime = ...
  //oldtime = ...
  
  DataSource source;
  
  ImageIcon logo1  = new ImageIcon(getClass().getResource("/gfx/logo1.png"));
  ImageIcon logo2  = new ImageIcon(getClass().getResource("/gfx/logo2.png"));
  ImageIcon play   = new ImageIcon(getClass().getResource("/gfx/play.png"));
  ImageIcon stop   = new ImageIcon(getClass().getResource("/gfx/stop.png"));

  int mousex=0, mousey=0;
  FreiNode selectedNode;
  FreiLink selectedLink;
  double selectedNodeDistance,
         selectedLinkDistance;
  int      selectedTime;

  public static Color fgcolor = new Color(20,200,20),     //used for text, lines etc., accessed globally!
                bgcolor = new Color(64,128,64,196),       //used for transparent backgrounds of most status boxes
                fgcolor2 = new Color(150,150,255),       //used for foreground of link status boxes
                bgcolor2 = new Color(40,40,192,196);       //used for transparent backgrounds of link status boxes
  public static Font mainfont = new Font("SansSerif", 0, 12),
                     smallerfont = new Font("SansSerif", 0, 9);
  
  protected JComboBox tfsearch;

  Runtime runtime;

  DateFormat dfdate=DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMANY),
             dftime=DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.GERMANY);
  
  Background background;
  ColorScheme cs;

  Converter converter = new Converter();

  public VisorFrame(DataSource source, Background bg) {
    
	background = bg;
	background.setConverter(converter);
	cs = background.getColorScheme();

    this.source=source;
    this.source.addDataSourceListener(this);
    this.addComponentListener(this);
    this.addMouseListener(this);
    this.addMouseMotionListener(this);
    this.addMouseWheelListener(this);
    runtime=Runtime.getRuntime();

    System.out.println("reading node list.");
    nodes=source.getNodeList();
    dumpNodes();
    System.out.println("reading node availability.");
    availmap=source.getNodeAvailability(0);
    System.out.print("reading link list.");
    long now = System.currentTimeMillis();
    lastAvailableTime = firstAvailableTime = crtTime = firstUpdateTime = source.getFirstUpdateTime();
    lastUpdateTime = source.getLastUpdateTime();
    links = source.getLinks(firstUpdateTime);
    System.out.println("("+(System.currentTimeMillis()-now)+"ms)");
    
    initZoom(0, cx, cy);
    scale=converter.getScale();

    FreiNode[] anodes=(nodes.toArray(new FreiNode[nodes.size()]));
    Arrays.sort(anodes);
    tfsearch = new JComboBox(anodes);

    tfsearch.setFont(smallerfont);
    tfsearch.setOpaque(false);
    tfsearch.setEditable(true);
    //tfsearch.setForeground(fgcolor);
    tfsearch.setBackground(bgcolor);
    tfsearch.setBorder(new LineBorder(fgcolor, 1, true));
    tfsearch.addActionListener(this);
    this.add(tfsearch);
    
    //debug
    System.out.println(nodes.size() + " nodes.");
  }
  
  //datasourcelistener
  public void timeRangeAvailable(long from, long until) {
    firstAvailableTime=from;
    lastAvailableTime=until;
    if ((firstUpdateTime>from)||(firstUpdateTime<100)) firstUpdateTime=from;
    if (lastUpdateTime<until) lastUpdateTime=until;
    nextFrame();
  }
  public void nodeListUpdate(FreiNode node) {
    if (nodes!=null) { //fixme: this really should not happen
      nodes.add(node);
      dumpNodes();
    }
  }
  private void dumpNodes() {
    //exportNodes();
    //exportLinks();
    /*try {
      ObjectOutputStream oos=new ObjectOutputStream(new FileOutputStream("nodes.dump"));
      oos.writeObject(nodes);
      oos.flush();
      oos.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    } */
  }
  
  public Dimension getPreferredSize() {
    return new Dimension(w,h);
  }
  
  public void actionPerformed(ActionEvent e) {
    FreiNode node;
    String query=tfsearch.getSelectedItem().toString();
    for (int i=0;i<nodes.size();i++) {
      node = nodes.elementAt(i);
      if (node.id.equals(query)) {
    	  converter.setWorld(converter.lonToWorld(node.lon) - cx, converter.latToWorld(node.lat) - cy);
    	  return;
      }
    }
  }

  public void componentHidden(ComponentEvent e) {}
  public void componentMoved(ComponentEvent e) {}
  public void componentShown(ComponentEvent e) {}
  public void componentResized(ComponentEvent e) {
    w = this.getWidth();
    h = this.getHeight();
    cx = w / 2;
    cy = h / 2;
    buf=this.createImage(w,h);
    background.setDimension(w, h);
  }

  private FreiNode uplink = new FreiNode("0.0.0.0/0.0.0.0");
  public void paint(Graphics gra) {
    if (buf==null) {
      w = this.getWidth();
      h = this.getHeight();
      background.setDimension(w, h);
      buf=this.createImage(w,h);
    }
    tfsearch.setLocation(5,33); //why do we have to redo this every time?

    Graphics2D g=(Graphics2D)buf.getGraphics();
    g.setFont(mainfont);
    g.setColor(cs.getColor(ColorScheme.Key.MAP_BACKGROUND));
    g.fillRect(0,0,w,h);

    background.paint(g);
        
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
          g.drawLine(converter.lonToViewX(link.from.lon),
        		  converter.latToViewY(link.from.lat),
        		  converter.lonToViewX(link.to.lon), 
        		  converter.latToViewY(link.to.lat));
        
          if (link.to.unlocated) {
            link.to.lonsum+=link.from.lon;
            link.to.latsum+=link.from.lat;
            link.to.nc++;
          }
          if (link.from.unlocated) {
            link.from.lonsum+=link.to.lon;
            link.from.latsum+=link.to.lat;
            link.from.nc++;
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
        	boxw = g.getFontMetrics(mainfont).stringWidth("Node: 999.999.999.999")+20;

          label = "Node: "+selectedNode.id;
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
          boxw = g.getFontMetrics(mainfont).stringWidth("Link: 999.999.999.999 -> 999.999.999.999/999.999.999.999");

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

               double labelh = g.getFontMetrics(mainfont).getHeight(),
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
        g.setFont(mainfont);
	g.drawString(label, (int)(boxx - boxw/2 + 10), (int)(boxy - boxh/2 + labelh));
	g.setColor(myfgcolor); 
	g.setFont(smallerfont);
	for (int i=0; i<infos.size(); i++) {
		g.drawString(infos.elementAt(i), (int)(boxx - boxw/2 + 10), (int)(boxy - boxh/2 + labelh + infoh*i + 15));
	}
    }

    //draw logos
    g.drawImage(logo1.getImage(), new AffineTransform(1d,0d,0d,1d,20d,-120d+h), this);
    g.drawImage(logo2.getImage(), new AffineTransform(1d,0d,0d,1d,100d,-120d+h), this);

    //draw time line

    g.setPaint(bgcolor);
    g.setStroke(new BasicStroke((float)3f));
    int x0=timelinex0, x1=timelinex1; 
    g.draw(new Line2D.Double(x0, h-60, x1, h-60));
    if ((firstAvailableTime > 0 ) && (firstUpdateTime != lastUpdateTime)) {
      int tmin = (int)Math.round((double)(firstAvailableTime - firstUpdateTime) / (lastUpdateTime - firstUpdateTime) * (x1-x0) + x0),
          tmax = (int)Math.round((double)(lastAvailableTime - firstUpdateTime) / (lastUpdateTime - firstUpdateTime) * (x1-x0) + x0);
      g.setPaint(fgcolor);
      g.draw(new Line2D.Double(tmin, h-60, tmax, h-60));
 
      if ((selectedTime>0) && (selectedTime < tmax)) {
        g.setPaint(Color.green);
        g.setStroke(new BasicStroke((float)2f));
        g.draw(new Line2D.Double(selectedTime, h-70, selectedTime, h-50));
      }
   }
    
    g.setPaint(fgcolor);
    g.setStroke(new BasicStroke((float)1f));
    g.draw(new Line2D.Double(x0, h-65, x0, h-55));
    g.draw(new Line2D.Double(x1, h-65, x1, h-55));
    g.setFont(smallerfont);
    g.drawString(dfdate.format(firstUpdateTime*1000), x0, h-45);
    g.drawString(dftime.format(firstUpdateTime*1000), x0, h-35);
    g.drawString(dfdate.format(lastUpdateTime*1000), x1, h-45);
    g.drawString(dftime.format(lastUpdateTime*1000), x1, h-35);

    g.setPaint(Color.green);
    if (lastUpdateTime>firstUpdateTime) {
      int xc = (int)Math.round((double)(oldTime - firstUpdateTime) / (lastUpdateTime - firstUpdateTime) * (x1-x0) + x0);
      g.draw(new Line2D.Double(xc, h-65, xc, h-55));
      g.drawString(dfdate.format(oldTime*1000), xc, h-75);
      g.drawString(dftime.format(oldTime*1000), xc, h-65);
    }

 
    //draw play/stop button
    if (playing) {
      g.drawImage(play.getImage(), x0-30, h-70, this);
    } else {
      g.drawImage(stop.getImage(), x0-30, h-70, this);
    }

    //draw status bars
    g.setStroke(new BasicStroke((float)1f));
    Shape header = new Rectangle2D.Double(-1,-1,w+2,30);
    Shape menus = new Rectangle2D.Double(-1,30,w+2,27);
    Shape footer = new Rectangle2D.Double(-1,h-30,w+2,30);
    g.setColor(bgcolor);
    g.fill(header);
    g.fill(menus);
    g.fill(footer);
    g.setColor(fgcolor);
    g.draw(header);
    g.draw(menus);
    g.draw(footer);
    g.setFont(mainfont);
    g.drawString("{ cim city || freifunc ^ cience };", 10, 20);
    g.drawString("coom " + zoom + "/17", w/4, 20);
    g.drawString("lon " + converter.viewXToLon(mousex), w/2, 20);
    g.drawString("lat " + converter.viewYToLat(mousey), 3*w/4, 20);
    g.drawString(new Date(oldTime*1000).toString(), 10, h-10);
    
    long free  =runtime.freeMemory(),
         //total =runtime.totalMemory(),
         max   =runtime.maxMemory();
    g.drawString("resource usage: "+free*100/max+"%", 3*w/4, h-10);
    //paintChildren(g);

    gra.drawImage(buf, 0, 0, this);
    paintChildren(gra);
  }

  public Object getElementAt(Point p) {
	//todo
	return null;
  }
  
  double refx, refy, refscale;
  
  public void saveScale() {
    refscale=scale;
  }
  
  private void initZoom(int zoom, int viewX, int viewY)
  {
	  converter.initZoom(zoom, viewX, viewY);
	  background.setZoom(zoom);
  }
  
  private void centerOn(Point p) {
    converter.setWorld(
    		converter.viewToWorldX(p.x) - cx,
    		converter.viewToWorldY(p.y) - cy);
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

  
  public void mouseMoved(MouseEvent e) {
    mousex = e.getX();
    mousey = e.getY();
    
    double lon = converter.viewXToLon(mousex);
    double lat = converter.viewYToLat(mousey);
    if ((mousey>h-100)&&(mousex >= timelinex0)&&(mousex <= timelinex1)) {
        selectedNode = null;
        selectedLink = null;
        selectedTime = mousex;
    } else {
    	selectedNode = getClosestNode(lon, lat);
    	selectedLink = getClosestLink(lon, lat);
        if (selectedNodeDistance * scale < 10) selectedLinkDistance=Double.POSITIVE_INFINITY; //when close to a node, select a node
        selectedTime = 0;
    }
    repaint();
  }

  public void mouseClicked(MouseEvent e) { 
    mousex = e.getX();
    mousey = e.getY();
    
    double lon = converter.viewXToLon(mousex);
    double lat = converter.viewYToLat(mousey);

    switch (e.getButton()) {
      case MouseEvent.BUTTON1: {
        switch (e.getClickCount()) {
          case 1: {
            if ((mousey>h-100)&&(mousex >= timelinex0)&&(mousex <= timelinex1)) {
              setCurrentTime(mousex-timelinex0);
            } else if ((mousey>h-70)&&(mousey<h-45)&&(mousex >= timelinex0-30)&&(mousex <= timelinex1)) {
              playing=!playing;
            }
            this.repaint();
            break;
          }
          case 2: {
            System.out.println("Center on: "+lat+" "+lon);
            centerOn(e.getPoint());
            break;
          }
        }
        break;
      }
      case MouseEvent.BUTTON3: {
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

  void setCurrentTime(int x) {
    long time = (long)(firstUpdateTime + (lastUpdateTime - firstUpdateTime) * ((double)x) / (timelinex1-timelinex0));
    crtTime=time;
  }

  int mrefx, mrefy, mouseMode;
  public void mousePressed(MouseEvent e) {
    mrefx = e.getX();
    mrefy = e.getY();
    mouseMode = e.getButton();
  }

  public void mouseDragged(MouseEvent e) {
    mousex = e.getX();
    mousey = e.getY();
    if ((mousey>h-100)&&(mousex >= timelinex0)&&(mousex <= timelinex1)) {
      setCurrentTime(mousex-timelinex0);
    } else {
      switch(mouseMode) {
        case MouseEvent.BUTTON1:
          converter.setWorldRel(mrefx - mousex, mrefy - mousey);
          
          mrefx = mousex;
          mrefy = mousey;
          
          repaint();
        	  
          break;
      }
    }
  }
  //the rest  
  public void mouseWheelMoved(MouseWheelEvent e) {
    saveScale();
    
    Point p = e.getPoint();
    
	zoom += ((e.getWheelRotation() < 0) ? +1 : -1);
	zoom = Math.min(17, Math.max(0, zoom));
	
	initZoom(zoom, p.x, p.y);
	
    // Calculate the unit size
	scale = converter.getScale();

	repaint();

  }
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}

  public void nextFrame() {
/*
	  //begin: this should move into repaint block
    offsetX = (offsetX * 3 + targetOffsetX) / 4;
    offsetY = (offsetY * 3 + targetOffsetY) / 4;
    // end
    //crtTime += 100;
    if (playing) crtTime += 1;
    //crtTime = lastAvailableTime;
    long closestTime = source.getClosestUpdateTime(crtTime);
    if (closestTime != oldTime) {
      links = source.getLinks(closestTime);
      this.repaint();
    } 
    oldTime = closestTime;
    */
  }

  //XStream xstream = new XStream();
  public void exportNodes() {
    if (nodes==null) return;
    try {
      Yaml.dumpStream(nodes.iterator(), new File("nodes.yaml"), true);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
/*
    xstream.alias("node", FreiNode.class);
    xstream.omitField(FreiNode.class, "lonsum");
    xstream.omitField(FreiNode.class, "latsum");
    xstream.omitField(FreiNode.class, "nc");
    try {
      String xmlNodes = xstream.toXML(nodes);
      PrintWriter out=new PrintWriter(new FileWriter("nodes.xml"));
      out.println(xmlNodes);
      out.close();
    } catch (ConcurrentModificationException cmex) {
      System.out.println("An exception happened which may be safely ignored.");
    } catch (Exception ex) {
      System.out.println(ex);
    }
*/
  }
  public void exportLinks() {
    if (links==null) return;
    try {
      Yaml.dumpStream(links.iterator(), new File("links.yaml"), true);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
/*    xstream.alias("link", FreiLink.class);
    try {
      String xmlLinks = xstream.toXML(links);
      PrintWriter out=new PrintWriter(new FileWriter("links.xml"));
      out.println(xmlLinks);
      out.close();
    } catch (ConcurrentModificationException cmex) {
      System.out.println("An exception happened which may be safely ignored.");
    } catch (Exception ex) {
      System.out.println(ex);
    }*/
  }
  
}
