package net.relet.freimap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.LinkedList;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A {@link DataSource} implementation that reads node data from the
 * <a href="http://www.layereight.de/software.php">FreifunkMap</a> server.
 * 
 * Since it works with URLs, downloaded XML data can be used as well.
 *  
 * @author Robert Schuster (robertschuster@fsfe.org)
 *
 */
public class FreifunkMapDataSource implements DataSource {

	Vector<FreiNode> nodes = new Vector<FreiNode>();

	public FreifunkMapDataSource() {
		try {
			URL serverURL = new URL(Configurator.get("ffmds.url"));
			
			System.out.println("fetching node data from URL: " + serverURL);
			System.out.print("This may take a while ... ");
			Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(serverURL.openStream());
			System.out.println("finished.");
			
			parseXml(dom);
		} catch (MalformedURLException mue) {
			System.out.println("failed!");
			throw new IllegalStateException("Invalid server URL: "
					+ Configurator.get("fmds.url"));
		} catch (IOException ioe) {
			throw new IllegalStateException("IOException while receiving XML");
		} catch (ParserConfigurationException pce) {
			throw new IllegalStateException(
					"Class library broken. No suitable XML parser.");
		} catch (SAXException saxe) {
			throw new IllegalStateException("XML broken. Not valid.");
		}
	}
	
	String getValue(Node n)
	{
		if (n != null)
			return n.getNodeValue();
		
		return null;
	}

	private void parseXml(Document dom) {
		Node ffmap = dom.getElementsByTagName("ffmap").item(0);
		if (ffmap == null)
          throw new IllegalStateException("XML data contains no <ffmap> tag. Aborting ...");
		
		Node versionNode = ffmap.getAttributes().getNamedItem("version");
		if (versionNode == null || Double.parseDouble(versionNode.getNodeValue()) != 1.0)
		  throw new IllegalStateException("Version info in XML does not exist or is invalid. Aborting ...");
		
		NodeList nl = dom.getElementsByTagName("node");
		int size = nl.getLength();
		for (int i = 0; i < size; i++) {
			Node node = nl.item(i);
			NamedNodeMap attr = node.getAttributes();

			String klass = getValue(attr.getNamedItem("class"));
			String coords = getValue(attr.getNamedItem("coords"));
      String tooltip = getValue(attr.getNamedItem("tooltip"));
      String ip = getValue(attr.getNamedItem("ip"));

      // Skips old geo data for now.
      if (klass != null && klass.equals("old"))
        continue;
			
      if ((ip != null)&&(ip.equals(""))) ip=null; //empty ips == null

			// Use ip or coordinates as fqid if tooltip is missing
			if (tooltip == null) {
        if (ip == null) {
				  tooltip = coords;
        } else {
          tooltip = ip;
        }
      }

      String[] splitCoords = coords.split("\\s*,\\s*");

      if (ip == null) { //we need at least one identifier
        ip = tooltip;
      } else if (ip.indexOf(',')>-1) {  //someone abused XML to stuff several ips into a single attribute.
        StringTokenizer stip=new StringTokenizer(ip, ",", false);
        while (stip.hasMoreTokens()) {
          String oneip=stip.nextToken();
          FreiNode nnode = new FreiNode(oneip, tooltip, Double.parseDouble(splitCoords[1]), Double.parseDouble(splitCoords[0]));
          nodes.add(nnode);
        }
        continue;
      } 
      FreiNode nnode = new FreiNode(ip, tooltip, Double.parseDouble(splitCoords[1]), Double.parseDouble(splitCoords[0]));
		  nodes.add(nnode);
		}
	}

	public void addDataSourceListener(DataSourceListener dsl) {
		// TODO: Implement me.
	}

	public long getClosestUpdateTime(long time) {
		// TODO: Implement me.
		return 1;
	}

	public long getFirstUpdateTime() {
		// TODO: Implement me.
		return 1;
	}

	public long getLastUpdateTime() {
		// TODO: Implement me.
		return 1;
	}

	public void getLinkCountProfile(FreiNode node, NodeInfo info) {
		// TODO: Implement me.
		info.setLinkCountProfile(new LinkedList<LinkCount>());
	}

	public void getLinkProfile(FreiLink link, LinkInfo info) {
		// TODO: Implement me.
		info.setLinkProfile(new LinkedList<LinkData>());
	}

	public Vector<FreiLink> getLinks(long time) {
		// TODO: Implement me.
		return new Vector<FreiLink>();
	}

	public Hashtable<String, Float> getNodeAvailability(long time) {
		// TODO: Implement me.
		return new Hashtable<String, Float>();
	}

	public Vector<FreiNode> getNodeList() {
		return nodes;
	}

}
