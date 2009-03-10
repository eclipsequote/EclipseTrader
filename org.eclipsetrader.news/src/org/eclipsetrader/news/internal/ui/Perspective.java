/*
 * Copyright (c) 2004-2008 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package org.eclipsetrader.news.internal.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class Perspective implements IPerspectiveFactory {

	public Perspective() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	public void createInitialLayout(IPageLayout layout) {
		// Editors are not needed
		layout.setEditorAreaVisible(false);

		// Our editor area
		IFolderLayout editorsFolder = layout.createFolder("org.eclipsetrader.ui.editorss", IPageLayout.LEFT, (float) 100.0, layout.getEditorArea()); //$NON-NLS-1$
		editorsFolder.addPlaceholder("org.eclipsetrader.ui.views.watchlist:*");
		editorsFolder.addPlaceholder("org.eclipsetrader.news.browser:*");
		editorsFolder.addView("org.eclipsetrader.news.browser"); //$NON-NLS-1$

        // Left.
        IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, (float) 0.20, "org.eclipsetrader.ui.editorss"); //$NON-NLS-1$
        left.addView("org.eclipsetrader.ui.views.navigator"); //$NON-NLS-1$

        // Bottom
        IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.75, "org.eclipsetrader.ui.editorss"); //$NON-NLS-1$
        bottom.addView("org.eclipsetrader.ui.views.headlines"); //$NON-NLS-1$
		bottom.addPlaceholder("org.eclipse.ui.views.ProgressView"); //$NON-NLS-1$
	}
}
