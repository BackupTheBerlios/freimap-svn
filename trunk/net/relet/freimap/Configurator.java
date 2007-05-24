/* net.relet.freimap.Configurator.java

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

import java.io.*;
import java.util.Hashtable;

public class Configurator {
  public static final String[] CONFIG_LOCATIONS = new String[]{ 
    "./freimap.cfg",
    "./.freimaprc",
    "/etc/freimap.cfg",
    "~/.freimaprc"
  };

  private static final String[][] CONFIG_DEFAULTS = new String[][]{
    {"DataSource", "net.relet.freimap.FreifunkMapDataSource", "# net.relet.freimap.{FreifunkMap|OLSRD|Mysql}DataSource"},
    {"#DataSource", "net.relet.freimap.OLSRDDataSource", ""},
    {"#DataSource", "net.relet.freimap.MysqlDataSource", ""},
    {"background", "blank", "# Use blank, images or openstreetmap" },
    {"background.osm.delay", "2500", "# wait this number of milliseconds before fetching background tiles"}, 
    {"#background.osm.filter", "dark", "# apply a colour filter to openstreetmap tiles (use 'dark' or 'none')"}, 
    {"#background.osm.tileserver", "mapnik", "# OpenStreetMap map server: 'mapnik' (default), 'osmarender' or URL"}, 
    {"#background.osm.cache.dir", "./cache", "# if given, background map tiles will be cached here. CAUTION! caching will eat your disk space. Make sure the directory exists." },
    {"ffmds.url", "file:data/sample-map-berlin-20070519.xml", "# Sample data"},
    {"#ffmds.url", "http://map.olsrexperiment.de/freifunkmap.php?getArea=52.6351465262243,13.718490600585938,52.39278242102423,13.1011962890625&z=19", ""},
    {"yaml.url", "http://ffsomething.somewhere.tld", "# url of the ffsomething yaml server"},
    {"olsrd.host", "localhost", "# hostname"},
    {"olsrd.port", "2004", "# port"},
    {"olsrd.nodefile", "data/nodes.dump", "# keep the default value unless you know what you're doing"},
    {"mysql.host", "localhost", "# hostname"},
    {"mysql.user", "root", "# mysql username"},
    {"mysql.pass", " ", "# mysql password"},
    {"mysql.db", "freifunk", "# database name"},
    {"mysql.tables.nodes", "nodes_interpolated", "# table name for node positions"},
    {"mysql.tables.links", "links", "# table name for link data"},
    {"image.count", "1", "# background image count"},
    {"image.1.gfx", "gfx/cbase.png", "# background image"},
    {"image.1.lat", "52.520869", "# latitude of center of image"},
    {"image.1.lon", "13.409457", "# longitude of center of image"},
    {"image.1.scale", "40000", "# scale - you need to experiment here"},
  };
  public static Hashtable<String,String> config=new Hashtable<String,String>();

  public Configurator() {
    readDefaultConfig();
    parseConfigFile();
  }

  public static String get(String key) {
    return config.get(key);
  }
  public static int getI(String key) {
    try {
      return Integer.parseInt(config.get(key)); 
    } catch (Exception ex) {
      return -1;
    }
  }
  public static double getD(String key) {
    try {
      return Double.parseDouble(config.get(key)); 
    } catch (Exception ex) {
      return Double.NaN;
    }
  }

  void readDefaultConfig() {
    for (int i=0; i<CONFIG_DEFAULTS.length; i++) {
      config.put(CONFIG_DEFAULTS[i][0], CONFIG_DEFAULTS[i][1]);
    }
  }

  void parseConfigFile() {
    File found=null;
    for (int i=0;i<CONFIG_LOCATIONS.length;i++) {
      File f = new File(CONFIG_LOCATIONS[i]);
      if (!f.exists()) continue;
      found=f;
    }
    if (found==null) {
      System.out.println("Could not find configuration file freimap.cfg or .freimaprc, attempting to create a file with default values");
      File f=null;
      try {
        f=new File(CONFIG_LOCATIONS[0]);
        PrintWriter out=new PrintWriter(new FileWriter(f));
        for (int i=0; i<CONFIG_DEFAULTS.length; i++) {
          out.println(CONFIG_DEFAULTS[i][0]+"\t = "+CONFIG_DEFAULTS[i][1]+"\t"+CONFIG_DEFAULTS[i][2]);
        }
        out.close();
        System.out.println("Please have a look at "+f.getName()+". Then re-run this program.");
        System.exit(0);
      } catch (IOException ex) {
        System.out.println("Failed to create configuration file "+f.getName()+". "+ex.getMessage()+"\nUsing default configuration anyway.");
      } catch (Exception ex2) {
        ex2.printStackTrace();
      }
    }

    if (found!=null) {
      try {
        BufferedReader in=new BufferedReader(new FileReader(found));
        int n=0, pos;
        while (true) {
          String line=in.readLine();
          n++;
          if (line==null) break;
          if ((line.length()==0)||(line.charAt(0)=='#')) continue;
 	  if ((pos = line.indexOf("="))>0) {
            String key=line.substring(0, pos).trim();
            String value=line.substring(pos+1);
            if (value.indexOf("\t#")>0) value=value.substring(0,value.indexOf("\t#"));
            value=value.trim();
            /* if (config.get(key)==null) {
              System.out.println("Warning: Possibly ignoring unknown configuration key "+key);
            } */
            config.put(key, value);
          } else {
            System.out.println("Ignoring malformed configuration line "+n+".");
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }
}