/*
 * Copyright (c) 2004-2006 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package net.sourceforge.eclipsetrader.charts;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * Base abstract class for all indicator plugin preference pages
 */
public abstract class IndicatorPluginPreferencePage
{
    private String title;
    private String description;
    private Settings settings = new Settings();
    private Object container;
    private Control control;
    private boolean isPageComplete = true;
    private Map controls = new HashMap();
    
    public IndicatorPluginPreferencePage()
    {
    }

    public Object getContainer()
    {
        return container;
    }

    public void setContainer(Object container)
    {
        this.container = container;
        if (container instanceof WizardPage)
            ((WizardPage)container).setPageComplete(isPageComplete);
    }
    
    public abstract void createControl(Composite parent);

    /**
     * The default implementation of this method 
     * returns the value of an internal state variable set by
     * <code>setPageComplete</code>. Subclasses may extend.
     */
    public boolean isPageComplete()
    {
        return isPageComplete;
    }

    /**
     * Sets whether this page is complete. 
     *
     * @param complete <code>true</code> if this page is complete, and
     *   and <code>false</code> otherwise
     * @see #isPageComplete
     */
    public void setPageComplete(boolean complete) 
    {
        isPageComplete = complete;
        if (container instanceof WizardPage)
            ((WizardPage)container).setPageComplete(complete);
    }
    
    /**
     * Subclasses must implement this method to perform
     * any special finish processing for their page.
     */
    public void performFinish()
    {
        for (Iterator iter = controls.keySet().iterator(); iter.hasNext(); )
        {
            String key = (String) iter.next();
            Object control = controls.get(key);
            if (control instanceof Text)
                getSettings().set(key, ((Text) control).getText());
            else if (control instanceof Combo)
                getSettings().set(key, ((Combo) control).getSelectionIndex());
            else if (control instanceof ColorSelector)
                getSettings().set(key, ((ColorSelector) control).getColorValue());
            else if (control instanceof Spinner)
                getSettings().set(key, ((Spinner) control).getSelection());
        }
    }

    public Settings getSettings()
    {
        return settings;
    }

    public void setSettings(Settings settings)
    {
        this.settings = settings;
    }

    public Control getControl()
    {
        return control;
    }

    public void setControl(Control control)
    {
        this.control = control;
    } 

    public void setVisible(boolean visible)
    {
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
    
    public Text addLabelField(Composite parent, String id, String text, String defaultValue)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        Text lineLabel = new Text(parent, SWT.BORDER);
        lineLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        lineLabel.setText(getSettings().getString(id, defaultValue));
        controls.put(id, lineLabel);
        return lineLabel;
    }
    
    public ColorSelector addColorSelector(Composite parent, String id, String text, RGB defaultValue)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        ColorSelector colorSelector = new ColorSelector(parent);
        colorSelector.setColorValue(getSettings().getColor(id, defaultValue).getRGB());
        controls.put(id, colorSelector);
        return colorSelector;
    }
    
    public Combo addInputSelector(Composite parent, String id, String text, int defaultValue, boolean volume)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        Combo combo = new Combo(parent, SWT.READ_ONLY);
        combo.add("OPEN");
        combo.add("HIGH");
        combo.add("LOW");
        combo.add("CLOSE");
        if (volume)
            combo.add("VOLUME");
        combo.select(getSettings().getInteger(id, defaultValue).intValue());
        controls.put(id, combo);
        return combo;
    }
    
    public Combo addLineTypeSelector(Composite parent, String id, String text, int defaultValue)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        Combo lineType = new Combo(parent, SWT.READ_ONLY);
        lineType.add("Dot");
        lineType.add("Dash");
        lineType.add("Histogram");
        lineType.add("Histogram Bar");
        lineType.add("Line");
        lineType.add("Invisible");
        lineType.select(getSettings().getInteger(id, defaultValue).intValue());
        controls.put(id, lineType);
        return lineType;
    }
    
    public Combo addMovingAverageSelector(Composite parent, String id, String text, int defaultValue)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        Combo maType = new Combo(parent, SWT.READ_ONLY);
        maType.add("SIMPLE");
        maType.add("EXPONENTIAL");
        maType.add("WEIGHTED");
        maType.add("WILLIAM'S");
        maType.select(getSettings().getInteger(id, defaultValue).intValue());
        controls.put(id, maType);
        return maType;
    }
    
    public Spinner addIntegerValueSelector(Composite parent, String id, String text, int min, int max, int defaultValue)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        Spinner spinner = new Spinner(parent, SWT.BORDER);
        spinner.setLayoutData(new GridData(25, SWT.DEFAULT));
        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setSelection(getSettings().getInteger(id, defaultValue).intValue());
        controls.put(id, spinner);
        return spinner;
    }
    
    public Combo createLineTypeCombo(Composite parent, String text, int value)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        Combo lineType = new Combo(parent, SWT.READ_ONLY);
        lineType.add("Dot");
        lineType.add("Dash");
        lineType.add("Histogram");
        lineType.add("Histogram Bar");
        lineType.add("Line");
        lineType.add("Invisible");
        lineType.select(value);
        return lineType;
    }
    
    public Combo createMovingAverageCombo(Composite parent, String text, int value)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        Combo maType = new Combo(parent, SWT.READ_ONLY);
        maType.add("SIMPLE");
        maType.add("EXPONENTIAL");
        maType.add("WEIGHTED");
        maType.add("WILLIAM'S");
        maType.select(value);
        return maType;
    }
}
