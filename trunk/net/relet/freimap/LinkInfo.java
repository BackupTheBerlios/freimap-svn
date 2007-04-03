/* net.relet.freimap.LinkInfo.java 

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

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.title.*;
import org.jfree.data.xy.*;

public class LinkInfo {
  public static final int CHART_WIDTH = 100;

  public static final int STATUS_FETCHING  = 0;
  public static final int STATUS_AVAILABLE = 100;
  public static final int STATUS_FAILED    = -100;

  JFreeChart linkChart;
  public int status = STATUS_FETCHING;

  public void setLinkProfile(LinkedList<LinkData> lp) {

    XYSeries data = new XYSeries("etx");
    XYSeries avail = new XYSeries("avail");
    XYSeriesCollection datac = new XYSeriesCollection(data);
    datac.addSeries(avail);
    linkChart = ChartFactory.createXYLineChart("average link etx\r\naverage link availability", "time", "etx", datac, PlotOrientation.VERTICAL, false, false, false);
    sexupLayout(linkChart);

    long first=lp.getFirst().time,
         last =lp.getLast().time,
         lastClock = first,
         count = 0,    //number of samplis in aggregation timespan
         maxCount = 0; //max idem
    long aggregate = (last-first) / CHART_WIDTH; //calculate aggregation timespan: divide available timespan in CHART_WIDTH equal chunks
    double sum=0;

/* ok, this ain't effective, we do it just to pre-calculate maxCount */
    ListIterator<LinkData> li = lp.listIterator();
    while (li.hasNext()) { 
        LinkData ld = li.next();
        count++;
        if (ld.time - lastClock > aggregate) {
          if (maxCount < count) maxCount = count;
          lastClock=ld.time;
          count=0;
        }
    }

    //reset for second iteration
    count = 0; 
    lastClock = first;

    //iterate again
    li = lp.listIterator();
    while (li.hasNext()) { 
        LinkData ld = li.next();
        
        sum += ld.quality;
        count++; 
        
        if (aggregate==0) aggregate=1000;//dirty hack
        if (ld.time - lastClock > aggregate) {
          for (long i = lastClock; i<ld.time - aggregate; i+=aggregate) {
            data.add(i * 1000, (i==lastClock)?sum/count:Double.NaN); 
            avail.add(i * 1000, (i==lastClock)?((double)count/maxCount):0);
          }

          count=0; sum=0;
          lastClock=ld.time;
    	}
    }

    status = STATUS_AVAILABLE;
  }

  private void sexupAxis(ValueAxis axis) {
    axis.setLabelFont(VisorFrame.smallerfont);
    axis.setLabelPaint(VisorFrame.fgcolor2);
    axis.setTickLabelFont(VisorFrame.smallerfont);
    axis.setTickLabelPaint(VisorFrame.fgcolor2);
  }
  private void sexupLayout(JFreeChart chart) {
    chart.setAntiAlias(true);
    chart.setBackgroundPaint(VisorFrame.bgcolor2);
    chart.setBorderVisible(false);
    TextTitle title=chart.getTitle();
    title.setFont(VisorFrame.smallerfont);
    title.setPaint(VisorFrame.fgcolor2);
    XYPlot plot=chart.getXYPlot();
    plot.setBackgroundPaint(VisorFrame.bgcolor2);
    plot.setDomainAxis(new DateAxis());
    sexupAxis(plot.getDomainAxis());
    sexupAxis(plot.getRangeAxis());
  }
}
