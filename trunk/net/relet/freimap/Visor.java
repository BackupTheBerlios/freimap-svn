/* net.relet.freimap.Visor.java

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

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import net.relet.freimap.background.Background;

public class Visor extends JFrame implements WindowListener {
  public static Configurator config;

  public static void main(String[] args) {
    config=new Configurator();
    
    DataSource source = null;
    try {
      Class<DataSource> csource=(Class<DataSource>)Class.forName(config.get("DataSource")); //this cast cannot be checked!
      source = csource.newInstance();
    } catch (Exception ex) {
      ex.printStackTrace();
      return;
    }
    
    Background bg = Background.createBackground(Configurator.get("background"));
    
    new Visor(source, bg);
  }
  
  VisorFrame viz;
  DataSource source;
  
  public Visor(DataSource source, Background background) {
    super("freimap.berlios.de / (c)opyleft thomas hirsch");
    this.source=source;
    viz=new VisorFrame(source, background);
    Container c = this.getContentPane();
    c.add(viz);
    c.setBackground(Color.black);
    this.pack();
    this.setVisible(true);
    this.addWindowListener(this);
    
    try {
      while (true) {
        Thread.sleep(1);
        viz.nextFrame(); //todo: make this mouse controlled
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }  
  
  public void windowActivated(WindowEvent e) {}
  public void windowClosed(WindowEvent e) {}
  public void windowClosing(WindowEvent e) {
    System.exit(0);
  }
  public void windowDeactivated(WindowEvent e) {}
  public void windowDeiconified(WindowEvent e) {}
  public void windowIconified(WindowEvent e) {}
  public void windowOpened(WindowEvent e) {}
}
