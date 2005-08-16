/*******************************************************************************
 * Copyright (c) 2004-2005 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 *******************************************************************************/
package net.sourceforge.eclipsetrader.ui.views.charts;

import net.sourceforge.eclipsetrader.ui.views.charts.wizards.NewIndicatorWizard;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

/**
 */
public class ChartActions implements IViewActionDelegate, IWorkbenchWindowActionDelegate
{

  /* (non-Javadoc)
   * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
   */
  public void init(IViewPart viewPart)
  {
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window)
  {
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose()
  {
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action)
  {
    IWorkbenchPage pg = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    if (pg.getActivePart() instanceof ChartView)
    {
      ChartView view = (ChartView)pg.getActivePart();

      if (pg.getActivePart() instanceof HistoryChartView)
      {
        if (action.getId().equalsIgnoreCase("chart.refresh") == true)
          ((HistoryChartView)view).updateChart();
        else if (action.getId().equalsIgnoreCase("chart.next") == true)
          ((HistoryChartView)view).showNext();
        else if (action.getId().equalsIgnoreCase("chart.previous") == true)
          ((HistoryChartView)view).showPrevious();
      }
      else if (pg.getActivePart() instanceof RealtimeChartView)
      {
        if (action.getId().equalsIgnoreCase("chart.refresh") == true)
          ((RealtimeChartView)view).updateChart();
      }

      if (action.getId().equalsIgnoreCase("chart.add") == true)
      {
        NewIndicatorWizard wizard = new NewIndicatorWizard();
//        wizard.setChartView(view);
        wizard.open();
      }
      else if (action.getId().equalsIgnoreCase("view.all") == true)
        view.setLimitPeriod(0);
      else if (action.getId().equalsIgnoreCase("view.last6months") == true)
        view.setLimitPeriod(6);
      else if (action.getId().equalsIgnoreCase("view.last1year") == true)
        view.setLimitPeriod(12);
      else if (action.getId().equalsIgnoreCase("view.last2years") == true)
        view.setLimitPeriod(24);
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection)
  {
    if (selection instanceof ChartSelection)
    {
      action.setEnabled(true);
      if (((ChartSelection)selection).getChartCanvas() != null)
      {
        if (action.getId().equals("chart.edit") || action.getId().equals("chart.remove"))
          action.setEnabled(((ChartSelection)selection).getChartCanvas().getSelectedItem() instanceof PlotLine);
      }
      else
        action.setEnabled(false);
    }
    else
      action.setEnabled(false);

/*    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    if (page != null && page.getActivePart() instanceof HistoryChartView)
    {
      HistoryChartView view = (HistoryChartView)page.getActivePart();

      if (action.getId().equalsIgnoreCase("chart.line") == true)
        action.setChecked(view.getChartType() == PriceChart.LINE);
      else if (action.getId().equalsIgnoreCase("chart.candle") == true)
        action.setChecked(view.getChartType() == PriceChart.CANDLE);
      else if (action.getId().equalsIgnoreCase("chart.bar") == true)
        action.setChecked(view.getChartType() == PriceChart.BAR);
      else if (action.getId().equalsIgnoreCase("view.all") == true)
        action.setChecked(view.getLimitPeriod() == 0);
      else if (action.getId().equalsIgnoreCase("view.last6months") == true)
        action.setChecked(view.getLimitPeriod() == 6);
      else if (action.getId().equalsIgnoreCase("view.last1year") == true)
        action.setChecked(view.getLimitPeriod() == 12);
      else if (action.getId().equalsIgnoreCase("view.last2years") == true)
        action.setChecked(view.getLimitPeriod() == 24);
    }
    else if (page != null && page.getActivePart() instanceof RealtimeChartView)
    {
      RealtimeChartView view = (RealtimeChartView)page.getActivePart();

      if (action.getId().equalsIgnoreCase("chart.line") == true)
        action.setChecked(view.getChartType() == PriceChart.LINE);
      else if (action.getId().equalsIgnoreCase("chart.candle") == true)
        action.setChecked(view.getChartType() == PriceChart.CANDLE);
      else if (action.getId().equalsIgnoreCase("chart.bar") == true)
        action.setChecked(view.getChartType() == PriceChart.BAR);
      else if (action.getId().equalsIgnoreCase("view.all") == true)
      {
        action.setChecked(false);
        action.setEnabled(false);
      }
      else if (action.getId().equalsIgnoreCase("view.last6months") == true)
      {
        action.setChecked(false);
        action.setEnabled(false);
      }
      else if (action.getId().equalsIgnoreCase("view.last1year") == true)
      {
        action.setChecked(false);
        action.setEnabled(false);
      }
      else if (action.getId().equalsIgnoreCase("view.last2years") == true)
      {
        action.setChecked(false);
        action.setEnabled(false);
      }
      else if (action.getId().equalsIgnoreCase("chart.next") == true)
      {
        action.setChecked(false);
        action.setEnabled(false);
      }
      else if (action.getId().equalsIgnoreCase("chart.previous") == true)
      {
        action.setChecked(false);
        action.setEnabled(false);
      }
    }*/
  }
}
