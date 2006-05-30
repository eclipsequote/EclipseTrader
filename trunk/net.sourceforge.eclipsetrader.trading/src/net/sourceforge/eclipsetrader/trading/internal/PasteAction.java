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

package net.sourceforge.eclipsetrader.trading.internal;

import net.sourceforge.eclipsetrader.core.CorePlugin;
import net.sourceforge.eclipsetrader.core.db.Security;
import net.sourceforge.eclipsetrader.core.db.WatchlistItem;
import net.sourceforge.eclipsetrader.core.transfers.SecurityTransfer;
import net.sourceforge.eclipsetrader.core.transfers.WatchlistItemTransfer;
import net.sourceforge.eclipsetrader.trading.views.WatchlistView;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public class PasteAction extends Action implements IPartListener
{
    private WatchlistView view;

    public PasteAction(WatchlistView view)
    {
        this.view = view;
        view.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
        setText("&Paste");
        ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
        setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
        setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run()
    {
        Clipboard clipboard = new Clipboard(Display.getDefault());
        
        WatchlistItem[] items = (WatchlistItem[])clipboard.getContents(WatchlistItemTransfer.getInstance());
        if (items != null && items.length != 0)
        {
            for (int i = 0; i < items.length; i++)
            {
                items[i].setParent(view.getWatchlist());
                view.getWatchlist().getItems().add(items[i]);
            }
            CorePlugin.getRepository().save(view.getWatchlist());
        }
        else
        {
            Security[] securities = (Security[])clipboard.getContents(SecurityTransfer.getInstance());
            if (securities != null)
            {
                for (int i = 0; i < securities.length; i++)
                {
                    WatchlistItem item = new WatchlistItem();
                    item.setParent(view.getWatchlist());
                    item.setSecurity(securities[i]);
                    view.getWatchlist().getItems().add(item);
                }
                CorePlugin.getRepository().save(view.getWatchlist());
            }
        }

        clipboard.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
     */
    public void partActivated(IWorkbenchPart part)
    {
        if (part == view)
        {
            Clipboard clipboard = new Clipboard(Display.getDefault());
            TransferData[] types = clipboard.getAvailableTypes();
            for (int i = 0; i < types.length; i++)
            {
                if (SecurityTransfer.getInstance().isSupportedType(types[i]))
                {
                    setEnabled(true);
                    clipboard.dispose();
                    return;
                }
            }
            clipboard.dispose();
        }
        
        setEnabled(false);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
     */
    public void partBroughtToTop(IWorkbenchPart part)
    {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
     */
    public void partClosed(IWorkbenchPart part)
    {
        if (part == view)
            view.getSite().getWorkbenchWindow().getPartService().removePartListener(this);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
     */
    public void partDeactivated(IWorkbenchPart part)
    {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
     */
    public void partOpened(IWorkbenchPart part)
    {
    }
}