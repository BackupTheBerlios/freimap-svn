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

import org.ho.yaml.Yaml;

/*
todo dimension -> configfile
     sensible method names
     rename x and y into lon and lat
*/

public class VisorFrame extends JPanel implements ComponentListener, MouseListener, MouseMotionListener, MouseWheelListener {
  double scale=1.0d;  // current scaling
  int zoom = 0;       // zoom factor, according to OSM tiling
  int w=800,h=600;    //screen width, hight
  int cx=400, cy=300; //center of screen

  int timelinex0=w/3, timelinex1=11*w/12;
  long firstUpdateTime, crtTime, oldTime=0, lastUpdateTime, 
       firstAvailableTime=-1, lastAvailableTime=-1;
  boolean playing=false;
  //first, lastupdatetime = min and max of time interval as reported by datasource - data may however only be retrieved in the range below
  //first, lastavailabletime = available range of time interval
  //crttime = ...
  //oldtime = ...
  
  ImageIcon logo1  = new ImageIcon(getClass().getResource("/gfx/logo1.png"));
  ImageIcon logo2  = new ImageIcon(getClass().getResource("/gfx/logo2.png"));
  ImageIcon play   = new ImageIcon(getClass().getResource("/gfx/play.png"));
  ImageIcon stop   = new ImageIcon(getClass().getResource("/gfx/stop.png"));

  public static Font mainfont = new Font("SansSerif", 0, 12),
                     smallerfont = new Font("SansSerif", 0, 9);

  public static Color fgcolor = new Color(20,200,20),     //used for text, lines etc., accessed globally! FIXME move these into colorscheme!
                bgcolor = new Color(64,128,64,196),       //used for transparent backgrounds of most status boxes
                fgcolor2 = new Color(150,150,255),        //used for foreground of link status boxes
                bgcolor2 = new Color(40,40,192,196);      //used for transparent backgrounds of link status boxes
  ColorScheme cs = ColorScheme.NO_MAP;

  int mousex=0, mousey=0;

  int      selectedTime;
  
  Runtime runtime;

  DateFormat dfdate=DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMANY),
             dftime=DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.GERMANY);
  
  Vector<VisorLayer> layers = new Vector<VisorLayer>();
  VisorLayer activeLayer;

  Image buf; //double buffer

  Converter converter = new Converter();

  public VisorFrame(DataSource source) {

    this.addComponentListener(this);
    this.addMouseListener(this);
    this.addMouseMotionListener(this);
    this.addMouseWheelListener(this);
    runtime=Runtime.getRuntime();
    
    initZoom(0, cx, cy);
  }

  public void addLayer(VisorLayer layer) {
    addLayer(layer, false);
  }
  public void addLayer(VisorLayer layer, boolean active) {
    layer.setConverter(converter);
    layer.setDimension(w,h);
    layer.setZoom(zoom);
    if (active) activeLayer=layer;
    layers.add(layer);
  }
  public void removeLayer(VisorLayer layer) {
    layers.remove(layer);
  }
  public Dimension getPreferredSize() {
    return new Dimension(w,h);
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

    for (int i=0;i<layers.size();i++) {
      layers.elementAt(i).setDimension(w, h);
    }
  }

  public void paint(Graphics gra) {
    if (buf==null) {
      buf=this.createImage(w,h);
    }
    Graphics2D g=(Graphics2D)buf.getGraphics();
    g.setColor(cs.getColor(ColorScheme.Key.MAP_BACKGROUND));
    g.fillRect(0,0,w,h);

    //draw all layers
    for (int i=0;i<layers.size();i++) {
      layers.elementAt(i).paint(g);
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
    g.drawString("{ http://freimap.berlios.de };", 10, 20); //TODO: replace this string by location information gathered from osm namefinder api :) 
    g.drawString("coom " + zoom + "/17", w/4, 20);
    g.drawString("lon " + converter.viewXToLon(mousex), w/2, 20);
    g.drawString("lat " + converter.viewYToLat(mousey), 3*w/4, 20);
    g.drawString(new Date(oldTime*1000).toString(), 10, h-10);
    
    long free  =runtime.freeMemory(),
         //total =runtime.totalMemory(),
         max   =runtime.maxMemory();
    g.drawString("resource usage: "+free*100/max+"%", 3*w/4, h-10);

    gra.drawImage(buf, 0, 0, this);
    paintChildren(gra);
  }

  double refx, refy, refscale;
  public void saveScale() {
    refscale=scale;
  }
  
  private void initZoom(int zoom, int viewX, int viewY)
  {
	  converter.initZoom(zoom, viewX, viewY);
	  for (int i=0;i<layers.size();i++) {
            layers.elementAt(i).setZoom(zoom);
          }
  }
  
  private void centerOn(Point p) {
    converter.setWorld(
    		converter.viewToWorldX(p.x) - cx,
    		converter.viewToWorldY(p.y) - cy);
  }

  public double sqr(double x) { 
    return x*x;
  }
  public double dist (double x1, double x2, double y1, double y2) {
    return Math.sqrt(sqr(x1-x2)+sqr(y1-y2));
  }
  
  public void mouseMoved(MouseEvent e) {
    mousex = e.getX();
    mousey = e.getY();
    
    double lon = converter.viewXToLon(mousex);
    double lat = converter.viewYToLat(mousey);

    if ((mousey>h-100)&&(mousex >= timelinex0)&&(mousex <= timelinex1)) {
      activeLayer.mouseMoved(0,0); //unset any mouse motion      
      selectedTime = mousex;
    } else {
      activeLayer.mouseMoved(lat, lon);
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
              zoom=zoom+1; //DEBUG only
              initZoom(zoom, mousex, mousey);
//            System.out.println("Center on: "+lat+" "+lon);
//            centerOn(e.getPoint());
            break;
          }
        }
        break;
      }
      case MouseEvent.BUTTON3: {
        activeLayer.mouseClicked(lat, lon, MouseEvent.BUTTON3);
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
    if (playing) crtTime += 1;
    crtTime = lastAvailableTime; //huh?
    for (int i=0; i<layers.size(); i++) {
      //layers.elementAt(i).setCurrentTime(crtTime);
    }
//FIXME move this to nodelayer & co
//    long closestTime = source.getClosestUpdateTime(crtTime);
//    if (closestTime != oldTime) {
//      links = source.getLinks(closestTime);
//      this.repaint();
//    } 
//    oldTime = closestTime;
  }

}
