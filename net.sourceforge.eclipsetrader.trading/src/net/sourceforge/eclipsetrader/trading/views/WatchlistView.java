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

package net.sourceforge.eclipsetrader.trading.views;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import net.sourceforge.eclipsetrader.core.CorePlugin;
import net.sourceforge.eclipsetrader.core.FeedMonitor;
import net.sourceforge.eclipsetrader.core.ICollectionObserver;
import net.sourceforge.eclipsetrader.core.db.Security;
import net.sourceforge.eclipsetrader.core.db.Watchlist;
import net.sourceforge.eclipsetrader.core.db.WatchlistItem;
import net.sourceforge.eclipsetrader.core.transfers.SecurityTransfer;
import net.sourceforge.eclipsetrader.core.transfers.WatchlistItemTransfer;
import net.sourceforge.eclipsetrader.core.ui.SelectionProvider;
import net.sourceforge.eclipsetrader.trading.TradingPlugin;
import net.sourceforge.eclipsetrader.trading.actions.SetRibbonLayoutAction;
import net.sourceforge.eclipsetrader.trading.actions.SetTableLayoutAction;
import net.sourceforge.eclipsetrader.trading.actions.ToggleShowTotalsAction;
import net.sourceforge.eclipsetrader.trading.internal.AbstractLayout;
import net.sourceforge.eclipsetrader.trading.internal.BoxedLayout;
import net.sourceforge.eclipsetrader.trading.internal.CopyAction;
import net.sourceforge.eclipsetrader.trading.internal.CutAction;
import net.sourceforge.eclipsetrader.trading.internal.DeleteAction;
import net.sourceforge.eclipsetrader.trading.internal.PasteAction;
import net.sourceforge.eclipsetrader.trading.internal.TableLayout;
import net.sourceforge.eclipsetrader.trading.wizards.WatchlistSettingsDialog;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

/**
 */
public class WatchlistView extends ViewPart implements ICollectionObserver, Observer
{
    public static final String VIEW_ID = "net.sourceforge.eclipsetrader.watchlist";
    public static final String PREFS_SHOW_TOTALS = "SHOW_TOTALS_";
    public static final int TABLE = 0;
    public static final int RIBBON = 1;
    private Watchlist watchlist;
    private Composite parent;
    private AbstractLayout layout;
    private Action tableLayout = new SetTableLayoutAction(this);
    private Action ribbonLayout = new SetRibbonLayoutAction(this);
    private Action toggleShowTotals = new ToggleShowTotalsAction(this);
    private Action cutAction = new CutAction(this);
    private Action copyAction = new CopyAction(this);
    private Action pasteAction;
    private Action deleteAction = new DeleteAction(this);

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite)
     */
    public void init(IViewSite site) throws PartInitException
    {
        IMenuManager menuManager = site.getActionBars().getMenuManager();
        menuManager.add(new Separator("top")); //$NON-NLS-1$
        menuManager.add(new Separator("group1")); //$NON-NLS-1$
        menuManager.add(new Separator("group2")); //$NON-NLS-1$
        menuManager.add(new Separator("group3")); //$NON-NLS-1$
        menuManager.add(new Separator("group4")); //$NON-NLS-1$
        menuManager.add(new Separator("group5")); //$NON-NLS-1$
        menuManager.add(new Separator("group6")); //$NON-NLS-1$
        menuManager.add(new Separator("additions")); //$NON-NLS-1$
        menuManager.add(new Separator("bottom")); //$NON-NLS-1$
        
        IMenuManager layoutMenu = new MenuManager("Layout", "layout");
        layoutMenu.add(tableLayout);
        layoutMenu.add(ribbonLayout);
        menuManager.appendToGroup("group5", layoutMenu);
        menuManager.appendToGroup("group5", toggleShowTotals);
        
        IToolBarManager toolBarManager = site.getActionBars().getToolBarManager();
        toolBarManager.add(new Separator("begin")); //$NON-NLS-1$
        toolBarManager.add(new Separator("group1")); //$NON-NLS-1$
        toolBarManager.add(new Separator("group2")); //$NON-NLS-1$
        toolBarManager.add(new Separator("group3")); //$NON-NLS-1$
        toolBarManager.add(new Separator("group4")); //$NON-NLS-1$
        toolBarManager.add(new Separator("group5")); //$NON-NLS-1$
        toolBarManager.add(new Separator("group6")); //$NON-NLS-1$
        toolBarManager.add(new Separator("additions")); //$NON-NLS-1$
        toolBarManager.add(new Separator("end")); //$NON-NLS-1$
        
        site.getActionBars().updateActionBars();
        
        super.init(site);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    public void createPartControl(Composite parent)
    {
        this.parent = parent;
        getSite().setSelectionProvider(new SelectionProvider());

        // Drag and drop support
        DropTarget target = new DropTarget(parent, DND.DROP_COPY|DND.DROP_MOVE);
        target.setTransfer(new Transfer[] { SecurityTransfer.getInstance(), WatchlistItemTransfer.getInstance() });
        target.addDropListener(new DropTargetListener() {
            public void dragEnter(DropTargetEvent event)
            {
                if (event.detail == DND.DROP_DEFAULT)
                    event.detail = DND.DROP_COPY;
            
                event.currentDataType = null;
                
                TransferData[] data = event.dataTypes;
                for (int i = 0; i < data.length; i++)
                {
                    if (WatchlistItemTransfer.getInstance().isSupportedType(data[i]))
                    {
                        event.currentDataType = data[i];
                        break;
                    }
                }
                if (event.currentDataType == null)
                {
                    for (int i = 0; i < data.length; i++)
                    {
                        if (SecurityTransfer.getInstance().isSupportedType(data[i]))
                        {
                            event.currentDataType = data[i];
                            break;
                        }
                    }
                }
            }

            public void dragOver(DropTargetEvent event)
            {
                event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
            }

            public void dragOperationChanged(DropTargetEvent event)
            {
            }

            public void dragLeave(DropTargetEvent event)
            {
            }

            public void dropAccept(DropTargetEvent event)
            {
            }

            public void drop(DropTargetEvent event)
            {
                if (SecurityTransfer.getInstance().isSupportedType(event.currentDataType))
                {
                    System.out.println("Drop of SecurityTransfer type");
                    Security[] securities = (Security[]) event.data;
                    if (watchlist != null)
                    {
                        for (int i = 0; i < securities.length; i++)
                        {
                            WatchlistItem item = new WatchlistItem();
                            item.setParent(watchlist);
                            item.setSecurity(securities[i]);
                            watchlist.getItems().add(item);
                        }
                        CorePlugin.getRepository().save(watchlist);
                    }
                }
                else if (WatchlistItemTransfer.getInstance().isSupportedType(event.currentDataType))
                {
                    System.out.println("Drop of WatchlistItemTransfer type");
                    WatchlistItem[] items = (WatchlistItem[]) event.data;
                    if (watchlist != null)
                    {
                        for (int i = 0; i < items.length; i++)
                        {
                            items[i].setParent(watchlist);
                            watchlist.getItems().add(items[i]);
                        }
                        CorePlugin.getRepository().save(watchlist);
                    }
                }
            }
        });

        IActionBars actionBars = getViewSite().getActionBars();
        actionBars.setGlobalActionHandler("cut", cutAction);
        actionBars.setGlobalActionHandler("copy", copyAction);
        actionBars.setGlobalActionHandler("paste", pasteAction = new PasteAction(this));
        actionBars.setGlobalActionHandler("delete", deleteAction);
        actionBars.setGlobalActionHandler("settings", new Action() {
            public void run()
            {
                WatchlistSettingsDialog dlg = new WatchlistSettingsDialog(getWatchlist(), getViewSite().getShell());
                dlg.open();
            }
        });

        watchlist = (Watchlist)CorePlugin.getRepository().load(Watchlist.class, new Integer(getViewSite().getSecondaryId()));
        if (watchlist.getDescription().length() != 0)
        {
            setTitleToolTip(getPartName());
            setPartName(watchlist.getDescription());
        }
        toggleShowTotals.setChecked(TradingPlugin.getDefault().getPreferenceStore().getBoolean(WatchlistView.PREFS_SHOW_TOTALS + getViewSite().getSecondaryId()));

        parent.getDisplay().asyncExec(new Runnable() {
            public void run()
            {
                if (watchlist != null)
                {
                    setLayout(watchlist.getStyle());
                    watchlist.getItems().addCollectionObserver(WatchlistView.this);
                    watchlist.addObserver(WatchlistView.this);
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    public void setFocus()
    {
        parent.setFocus();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    public void dispose()
    {
        if (watchlist != null)
        {
            watchlist.deleteObserver(this);
            watchlist.getItems().removeCollectionObserver(this);

            for (Iterator iter = watchlist.getItems().iterator(); iter.hasNext(); )
            {
                WatchlistItem watchlistItem = (WatchlistItem)iter.next();
                Security security = watchlistItem.getSecurity();
                if (security != null && security.getQuoteFeed() != null)
                    FeedMonitor.cancelMonitor(security);
            }
        }
        layout.dispose();
        super.dispose();
    }
    
    public void fillMenuBars(IMenuManager mgr)
    {
        mgr.add(new Separator());
        mgr.add(cutAction);
        mgr.add(copyAction);
        mgr.add(pasteAction);
        mgr.add(new Separator());
        mgr.add(deleteAction);
    }
    
    public void setLayout(int style)
    {
        if (layout != null)
            layout.dispose();

        switch(style)
        {
            case TABLE:
                layout = new TableLayout(this);
                break;
            case RIBBON:
                layout = new BoxedLayout(this);
                break;
        }

        tableLayout.setChecked(style == TABLE);
        ribbonLayout.setChecked(style == RIBBON);
        watchlist.setStyle(style);
        CorePlugin.getRepository().save(watchlist);

        layout.createPartControl(parent);
        layout.updateView();
        parent.layout();
    }

    public AbstractLayout getLayout()
    {
        return layout;
    }

    public Watchlist getWatchlist()
    {
        return watchlist;
    }

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    public void update(Observable o, Object arg)
    {
        parent.getDisplay().syncExec(new Runnable() {
            public void run()
            {
                if (watchlist.getDescription().length() != 0 && !watchlist.getDescription().equals(getPartName()))
                    setPartName(watchlist.getDescription());
                layout.updateView();
            }
        });
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.ICollectionObserver#itemAdded(java.lang.Object)
     */
    public void itemAdded(Object o)
    {
        WatchlistItem watchlistItem = (WatchlistItem)o;

        layout.itemAdded(o);

        Security security = watchlistItem.getSecurity();
        if (security != null && security.getQuoteFeed() != null)
            FeedMonitor.monitor(security);
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.ICollectionObserver#itemRemoved(java.lang.Object)
     */
    public void itemRemoved(Object o)
    {
        WatchlistItem watchlistItem = (WatchlistItem)o;

        layout.itemRemoved(o);

        Security security = watchlistItem.getSecurity();
        if (security != null && security.getQuoteFeed() != null)
            FeedMonitor.cancelMonitor(security);
    }
    
    public WatchlistItem[] getSelection()
    {
        return layout.getSelection();
    }
    
    public Table getTable()
    {
        if (layout instanceof TableLayout)
            return ((TableLayout)layout).getTable();
        return null;
    }
}
