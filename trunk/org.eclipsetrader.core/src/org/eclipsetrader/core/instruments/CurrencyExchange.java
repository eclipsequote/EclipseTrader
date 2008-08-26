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

package org.eclipsetrader.core.instruments;

import java.util.Currency;

import org.eclipsetrader.core.feed.IFeedIdentifier;
import org.eclipsetrader.core.repositories.IPropertyConstants;
import org.eclipsetrader.core.repositories.IStore;
import org.eclipsetrader.core.repositories.IStoreProperties;

public class CurrencyExchange extends Security implements ICurrencyExchange {
	private Currency from;
	private Currency to;
	private Double multiplier;

	public CurrencyExchange() {
	}

	public CurrencyExchange(Currency from, Currency to, Double multiplier) {
	    this.from = from;
	    this.to = to;
	    this.multiplier = multiplier;
    }

	public CurrencyExchange(String name, IFeedIdentifier identifier) {
		super(name, identifier);
	}

	public CurrencyExchange(IStore store, IStoreProperties storeProperties) {
		super(store, storeProperties);
	}

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.instruments.ICurrencyExchange#getFromCurrency()
     */
    public Currency getFromCurrency() {
	    return from;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.instruments.ICurrencyExchange#getToCurrency()
     */
    public Currency getToCurrency() {
	    return to;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.instruments.ICurrencyExchange#getMultiplier()
     */
    public Double getMultiplier() {
	    return multiplier;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.instruments.Security#getStoreProperties()
     */
    @Override
    public IStoreProperties getStoreProperties() {
    	IStoreProperties storeProperties = super.getStoreProperties();

		storeProperties.setProperty(IPropertyConstants.OBJECT_TYPE, ICurrencyExchange.class.getName());

		storeProperties.setProperty("from-currency", from);
    	storeProperties.setProperty("to-currency", to);
    	storeProperties.setProperty("multiplier", multiplier);

    	return storeProperties;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.instruments.Security#setStoreProperties(org.eclipsetrader.core.repositories.IStoreProperties)
     */
    @Override
    public void setStoreProperties(IStoreProperties storeProperties) {
	    super.setStoreProperties(storeProperties);

	    from = (Currency) storeProperties.getProperty("from-currency");
	    to = (Currency) storeProperties.getProperty("to-currency");
	    multiplier = (Double) storeProperties.getProperty("multiplier");
    }
}
