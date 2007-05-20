/* net.relet.freimap.OLSRDDataSource.java

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
import java.net.*;
import java.util.*;

public class OLSRDDataSource implements DataSource {
  DotPluginListener dot;
  DataSourceListener listener;
  TreeMap<Long, Vector<FreiLink>> data = new TreeMap<Long, Vector<FreiLink>>();
  Hashtable<String, FreiNode> knownNodes=new Hashtable<String, FreiNode>();
  Hashtable<String, FreiNode> generatedNodes=new Hashtable<String, FreiNode>();
  
  long lastUpdateTime = 1, 
       firstUpdateTime = 1;
  
  MysqlDataSource mysqlSource;
  boolean useMysqlSource = false;
       
  public OLSRDDataSource() {
    String host = Configurator.get("olsrd.host");
    String sport= Configurator.get("olsrd.port");
    if (sport==null) { 
      System.err.println("invalid port parameter "+sport);
      System.exit(1);
    }
    int port=2004;
    try {
      port=Integer.parseInt(sport);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println("invalid port parameter "+sport);
      System.exit(1);
    }
    //fetching node data from Mysqldatasource
    if (useMysqlSource) mysqlSource=new MysqlDataSource("localhost", "root", "", "freiberlin", true);
    //opening dot plugin connection
    dot = new DotPluginListener(host, port, this);
  }
  
  public Vector<FreiNode> getNodeList() {
    if (useMysqlSource) {
      Vector<FreiNode> nodes = mysqlSource.getNodeList();
      for (Enumeration<String> enu = generatedNodes.keys(); enu.hasMoreElements();) {
        nodes.add(generatedNodes.get(enu.nextElement()));
      }
      return nodes;
    } else {
      try {
        ObjectInputStream ois=new ObjectInputStream(ClassLoader.getSystemResourceAsStream(Configurator.get("olsrd.nodefile")));
        Vector<FreiNode> nodes = (Vector<FreiNode>)ois.readObject();
        ois.close();
        for (int i=0;i<nodes.size();i++) { 
          knownNodes.put(nodes.elementAt(i).id, nodes.elementAt(i));
        }
        return nodes;
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    return null;
  }

  public FreiNode getNodeByName(String id) {
    if (useMysqlSource) return mysqlSource.getNodeByName(id);
    else { 
      FreiNode node= knownNodes.get(id);
      if (node!=null) return node;
      else return generatedNodes.get(id);
    }
  }
  
  public Hashtable<String, Float> getNodeAvailability(long time) {
    if (useMysqlSource) {
      return mysqlSource.getNodeAvailability(time);
    } else {
      return new Hashtable<String, Float>(); //empty
    }
  }
  public long getLastUpdateTime() {
    return lastUpdateTime;
  }
  public long getFirstUpdateTime() {
    return firstUpdateTime;
  }
  public long getClosestUpdateTime(long time) {
    long cur=-1, closest = Long.MAX_VALUE;
    Set<Long> keys = data.keySet();
    Iterator<Long> ki = keys.iterator();
    while (ki.hasNext()) {
      cur = ki.next().longValue();
      long d=Math.abs(time-cur);
      if (d<closest) closest=d;
      else break;
    }
    return cur;
  }
  
  public Vector<FreiLink> getLinks(long time) {
    return data.get(new Long(time));
  }
  //threaded information fetching
  public void addDataSourceListener(DataSourceListener dsl) {
    listener = dsl;
    if (!dot.isAlive()) dot.start();
  }
  //some optional methods
  public void getLinkCountProfile(FreiNode node, NodeInfo info) {
    LinkedList<LinkCount> lcp=new LinkedList<LinkCount>();
    //select HIGH_PRIORITY unix_timestamp(clock) as time, count(*) as num_links from "+TABLE_LINKS+" where dest='"+node.id+"' group by clock"
    Iterator<Long> times=data.keySet().iterator(); 
    while(times.hasNext()) {
      Long time=times.next();
      Vector<FreiLink> links=data.get(time);
      int lc=0;
      for (int i=0; i<links.size(); i++) {
        FreiLink link=links.elementAt(i);
        if (link.to.equals(node)) {
          lc++;
        }
      }
      lcp.add(new LinkCount(time.longValue(), lc));
    }
    info.setLinkCountProfile(lcp);
  }
  public void getLinkProfile(FreiLink mylink, LinkInfo info) {
    LinkedList<LinkData> lp=new LinkedList<LinkData>();
    //select HIGH_PRIORITY unix_timestamp(clock) as time, quality from "+TABLE_LINKS+" where src='"+link.from.id+"' and dest='"+link.to.id+"'");
    Iterator <Long> times=data.keySet().iterator();
    while (times.hasNext()) {
      Long time=times.next();
      Vector<FreiLink> links=data.get(time);
      float quality=0;
      for (int i=0; i<links.size(); i++) {
        FreiLink link=links.elementAt(i);
        if (link.equals(mylink)) {
          quality=link.etx;
        }
      }
      lp.add(new LinkData(time.longValue(), quality));
    }
    info.setLinkProfile(lp);
  }

  //private methods
  private void addLinkData(long time, Vector<FreiLink> linkData) {
    data.put(new Long(time), linkData);
    if (firstUpdateTime==1) firstUpdateTime=time;
    lastUpdateTime=time;
    if (listener != null) listener.timeRangeAvailable(firstUpdateTime, lastUpdateTime);
  }
  
  class DotPluginListener extends Thread {
    BufferedReader in;
    String host;
    int port;
    OLSRDDataSource parent;
    
    public DotPluginListener(String host, int port, OLSRDDataSource parent) {
      this.parent=parent;
      this.host=host;
      this.port=port;
      System.setProperty("java.net.IPv4Stack", "true"); //not necessary, but works around a bug in older java versions.
    }

    public void run() {
      Vector<FreiLink> linkData = null;
      try {
        InetSocketAddress destination = new InetSocketAddress(host, port);
        while (true) { //reconnect upon disconnection
        Socket s = new Socket();
	//s.setSoTimeout(10000);
        s.connect(destination, 25000);
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        while (in!=null) {
          String line=in.readLine();
          if (line==null) break;
          if (line.equals("digraph topology")) {
            if (linkData!=null) parent.addLinkData(System.currentTimeMillis()/1000, linkData);
            linkData = new Vector<FreiLink>();
          } else if ((line.length()>0) && (line.charAt(0)=='"')) {
            StringTokenizer st=new StringTokenizer(line, "\"", false);
            String from = st.nextToken();
            st.nextToken();
            if (st.hasMoreTokens()) { //otherwise it's a gateway node!
              String to = st.nextToken();
              st.nextToken();
              String setx = st.nextToken();
              boolean hna = setx.equals("HNA"); 
              float etx = hna?0:Float.parseFloat(setx);
              FreiNode nfrom = getNodeByName(from),
                       nto   = getNodeByName(to);
              if (nfrom == null) {
                         nfrom = generatedNodes.get(from);
                         if (nfrom==null) {
                           nfrom = new FreiNode(from);
                           generatedNodes.put(from, nfrom);
                           if (listener!=null) listener.nodeListUpdate(nfrom);
                         }
              }
              if (nto   == null) {
                         nto = generatedNodes.get(to);
                         if (nto==null) {
                           nto = new FreiNode(to);
                           generatedNodes.put(to, nto);
                           if (listener!=null) listener.nodeListUpdate(nto);
                         }
              }
              linkData.add(new FreiLink(nfrom, nto, etx, hna));
            }
          } 
        }
        }
      } catch (SocketTimeoutException ex) {
        System.err.println("[OLSRDataSource] timeout while trying to connect. "+ex.getMessage());
        return;
      } catch (ConnectException ex) {
        System.err.println("connection to " + host + ":" + port + " failed. Detailed node data won't be available.");
        return;
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }
}
