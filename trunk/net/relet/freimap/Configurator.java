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
import java.util.*;

import org.ho.yaml.*;


public class Configurator {
  private static HashMap<String, Object> config;

  public static final String[] CONFIG_LOCATIONS = new String[]{ 
    "./config.yaml",
//    "./freimap.cfg",
    "./.freimaprc",
    "/etc/freimap.cfg",
    "~/.freimaprc"
  };

  public void initDefaultConfig() {
    config = new HashMap<String, Object>();
    config.put("version" , "svn");
    config.put("subversion", new Integer(60));
    HashMap<String, Object> background = new HashMap<String, Object>();
    background.put("type", "blank");
    config.put("background", background);
    HashMap<String, Object> datasources = new HashMap<String, Object>();
    HashMap<String, Object> source = new HashMap<String, Object>();
    source.put("class", "net.relet.freimap.OLSRDDataSource");
    source.put("host", "localhost");
    source.put("port", new Integer(2004));
    source.put("nodesource", "freifunk-map");
    datasources.put("dotplugin-localhost", source);
    source = new HashMap<String, Object>();
    source.put("class", "net.relet.freimap.FreifunkMapDataSource");
    source.put("url", "file:data/ffmap.xml");
    datasources.put("freifunk-map",source);
    config.put("datasources",datasources);
  }

  public Configurator() {
    initDefaultConfig();
    parseConfigFile();
  }

  public static Object get(String key) {
    return get(key, null);
  }
  public static Object get(String key, HashMap<String, Object> parent) {
    return get(new String[]{key}, parent);
  }
  public static Object get(String[] keys) {
    return get(keys, null);
  }
  @SuppressWarnings("unchecked")
  public static Object get(String[] keys, HashMap<String, Object> parent) {
    if (parent==null) parent = config;
    Object value = parent;
    for (int i=0; i<keys.length; i++) {
      try {
        parent = (HashMap<String, Object>)value;
      } catch (Exception ex) {
        return null;
      }
      value = parent.get(keys[i]);
    }
    return value;
  }
  public static int getI(String[] keys) {
    return getI(keys, null);
  }
  public static int getI(String key, HashMap<String, Object> parent) {
    return getI(new String[]{key}, parent);
  }
  public static int getI(String[] keys, HashMap<String, Object> parent) {
    try {
      return (Integer)get(keys, parent); 
    } catch (Exception ex) {
    }
    return -1;
  }
  public static double getD(String key) {
    return getD(key, null);
  }
  public static double getD(String key, HashMap<String, Object> parent) {
    return getD(new String[]{key}, parent);
  }
  public static double getD(String[] keys) {
    return getD(keys, null);
  }
  public static double getD(String[] keys, HashMap<String, Object> parent) {
    try {
      Object o = get(keys, parent);
      if (o instanceof Double) return ((Double)o).doubleValue();
      if (o instanceof Integer) return ((Integer)o).doubleValue();
    } catch (Exception ex) {
    }
    return Double.NaN;
  }
  public static String getS(String[] keys) {
    return getS(keys, null);
  }
  public static String getS(String key, HashMap<String, Object> parent) {
    return getS(new String[]{key}, parent);
  }
  public static String getS(String[] keys, HashMap<String, Object> parent) {
    try {
      return (String)get(keys, parent); 
    } catch (Exception ex) {
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  void parseConfigFile() {
    File found=null;
    for (int i=0;i<CONFIG_LOCATIONS.length;i++) {
      File f = new File(CONFIG_LOCATIONS[i]);
      if (!f.exists()) continue;
      found=f;
    }
    if (found==null) {
      System.out.println("Could not find a configuration file, attempting to create file config.yaml with default values");
      File f=null;
      try {
        f=new File(CONFIG_LOCATIONS[0]);
        Yaml.dump(config, f, true);
        System.out.println("Please have a look at "+f.getName()+". Then re-run this program.");
        System.exit(0);
      } catch (IOException ex) {
        System.out.println("Failed to create configuration file "+f.getName()+". "+ex.getMessage());
        System.exit(1);
      } catch (Exception ex2) {
        ex2.printStackTrace();
      }
    }

    if (found!=null) {
      try {
        config = (HashMap<String, Object>)Yaml.load(found);
      } catch (Exception ex) {
        System.out.println("Failed to load configuration file "+found.getName()+". "+ex.getMessage());
        System.exit(1);
      }
    }
  }
}
