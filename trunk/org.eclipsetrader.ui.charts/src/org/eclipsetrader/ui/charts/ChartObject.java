/*
 * Copyright (c) 2004-2009 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package org.eclipsetrader.ui.charts;

import org.eclipsetrader.core.charts.IDataSeries;

/**
 * Default implementation of the <code>IChartObject</code> interface.
 *
 * @since 1.0
 */
public class ChartObject implements IChartObject {

	public ChartObject() {
	}

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#containsPoint(int, int)
     */
    public boolean containsPoint(int x, int y) {
	    return false;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#getDataSeries()
     */
    public IDataSeries getDataSeries() {
	    return null;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#getToolTip()
     */
    public String getToolTip() {
	    return null;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#getToolTip(int, int)
     */
    public String getToolTip(int x, int y) {
	    return null;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#paint(org.eclipsetrader.ui.charts.IGraphics)
     */
    public void paint(IGraphics graphics) {
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#paintScale(org.eclipsetrader.ui.charts.Graphics)
     */
    public void paintScale(Graphics graphics) {
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#invalidate()
     */
    public void invalidate() {
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#setDataBounds(org.eclipsetrader.ui.charts.DataBounds)
     */
    public void setDataBounds(DataBounds bounds) {
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#handleFocusGained(org.eclipsetrader.ui.charts.ChartObjectFocusEvent)
     */
    public void handleFocusGained(ChartObjectFocusEvent event) {
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#handleFocusLost(org.eclipsetrader.ui.charts.ChartObjectFocusEvent)
     */
    public void handleFocusLost(ChartObjectFocusEvent event) {
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.ui.charts.IChartObject#accept(org.eclipsetrader.ui.charts.IChartObjectVisitor)
     */
    public void accept(IChartObjectVisitor visitor) {
    	visitor.visit(this);
    }
}
