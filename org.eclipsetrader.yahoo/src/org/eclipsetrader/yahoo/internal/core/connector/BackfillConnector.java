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

package org.eclipsetrader.yahoo.internal.core.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Status;
import org.eclipsetrader.core.feed.Dividend;
import org.eclipsetrader.core.feed.IBackfillConnector;
import org.eclipsetrader.core.feed.IDividend;
import org.eclipsetrader.core.feed.IFeedIdentifier;
import org.eclipsetrader.core.feed.IOHLC;
import org.eclipsetrader.core.feed.ISplit;
import org.eclipsetrader.core.feed.OHLC;
import org.eclipsetrader.core.feed.TimeSpan;
import org.eclipsetrader.core.feed.TimeSpan.Units;
import org.eclipsetrader.yahoo.internal.YahooActivator;
import org.eclipsetrader.yahoo.internal.core.Util;

public class BackfillConnector implements IBackfillConnector, IExecutableExtension {
	private String id;
	private String name;

	private SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd"); //$NON-NLS-1$
	private SimpleDateFormat dfAlt = new SimpleDateFormat("dd-MMM-yy", Locale.US); //$NON-NLS-1$
	private NumberFormat nf = NumberFormat.getInstance(Locale.US);
	private NumberFormat pf = NumberFormat.getInstance(Locale.US);

	public BackfillConnector() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		id = config.getAttribute("id");
		name = config.getAttribute("name");
	}

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.feed.IFeedConnector#getId()
	 */
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.feed.IFeedConnector#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.feed.IBackfillConnector#canBackfill(org.eclipsetrader.core.feed.IFeedIdentifier, org.eclipsetrader.core.feed.TimeSpan)
	 */
	public boolean canBackfill(IFeedIdentifier identifier, TimeSpan timeSpan) {
		if (timeSpan.getUnits() == Units.Days && timeSpan.getLength() == 1)
			return true;

		String symbol = Util.getSymbol(identifier);
		if (symbol.startsWith("^"))
			return false;

		if (timeSpan.getUnits() == Units.Minutes && timeSpan.getLength() == 1)
			return true;
		if (timeSpan.getUnits() == Units.Minutes && timeSpan.getLength() == 5)
			return true;

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.feed.IBackfillConnector#backfillHistory(org.eclipsetrader.core.feed.IFeedIdentifier, java.util.Date, java.util.Date, org.eclipsetrader.core.feed.TimeSpan)
	 */
	public IOHLC[] backfillHistory(IFeedIdentifier identifier, Date from, Date to, TimeSpan timeSpan) {
		if (TimeSpan.days(1).equals(timeSpan)) {
			return backfillDailyHistory(identifier, from, to);
		}
		else if (TimeSpan.minutes(1).equals(timeSpan)) {
			return backfill1DayHistory(identifier);
		}
		else if (TimeSpan.minutes(5).equals(timeSpan)) {
			return backfill5DayHistory(identifier);
		}
		return null;
	}

	private IOHLC[] backfillDailyHistory(IFeedIdentifier identifier, Date from, Date to) {
		List<OHLC> list = new ArrayList<OHLC>();

		try {
			HttpMethod method = Util.getHistoryFeedMethod(identifier, from, to);
			method.setFollowRedirects(true);

			HttpClient client = new HttpClient();
			client.executeMethod(method);

			BufferedReader in = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
			readBackfillStream(list, in);
			in.close();

		} catch (Exception e) {
			Status status = new Status(Status.ERROR, YahooActivator.PLUGIN_ID, 0, "Error reading data", e);
			YahooActivator.log(status);
		}

		return list.toArray(new IOHLC[list.size()]);
	}

	private IOHLC[] backfill1DayHistory(IFeedIdentifier identifier) {
		List<OHLC> list = new ArrayList<OHLC>();

		try {
			HttpMethod method = Util.get1DayHistoryFeedMethod(identifier);
			method.setFollowRedirects(true);

			HttpClient client = new HttpClient();
			client.executeMethod(method);

			BufferedReader in = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
			read1DayBackfillStream(list, in);
			in.close();

		} catch (Exception e) {
			Status status = new Status(Status.ERROR, YahooActivator.PLUGIN_ID, 0, "Error reading data", e);
			YahooActivator.log(status);
		}

		return list.toArray(new IOHLC[list.size()]);
	}

	private IOHLC[] backfill5DayHistory(IFeedIdentifier identifier) {
		List<OHLC> list = new ArrayList<OHLC>();

		try {
			HttpMethod method = Util.get5DayHistoryFeedMethod(identifier);
			method.setFollowRedirects(true);

			HttpClient client = new HttpClient();
			client.executeMethod(method);

			BufferedReader in = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
			read1DayBackfillStream(list, in);
			in.close();

		} catch (Exception e) {
			Status status = new Status(Status.ERROR, YahooActivator.PLUGIN_ID, 0, "Error reading data", e);
			YahooActivator.log(status);
		}

		return list.toArray(new IOHLC[list.size()]);
	}

	void readBackfillStream(List<OHLC> list, BufferedReader in) throws IOException {
		String inputLine = in.readLine();
		while ((inputLine = in.readLine()) != null) {
			if (inputLine.startsWith("<")) //$NON-NLS-1$
				continue;

			try {
				OHLC bar = parseResponseLine(inputLine);
				if (bar != null)
					list.add(bar);
			} catch (ParseException e) {
				Status status = new Status(Status.ERROR, YahooActivator.PLUGIN_ID, 0, "Error parsing data: " + inputLine, e);
				YahooActivator.log(status);
			}
		}
	}

	void read1DayBackfillStream(List<OHLC> list, BufferedReader in) throws IOException {
		String inputLine;

		while ((inputLine = in.readLine()) != null) {
			if (!Character.isDigit(inputLine.charAt(0)))
				continue;

			try {
				OHLC bar = parse1DayResponseLine(inputLine);
				if (bar != null)
					list.add(bar);
			} catch (ParseException e) {
				Status status = new Status(Status.ERROR, YahooActivator.PLUGIN_ID, 0, "Error parsing data: " + inputLine, e);
				YahooActivator.log(status);
			}
		}
	}

	protected OHLC parseResponseLine(String inputLine) throws ParseException {
		String[] item = inputLine.split(","); //$NON-NLS-1$
		if (item.length < 6)
			return null;

		Calendar day = Calendar.getInstance();
		try {
			day.setTime(df.parse(item[0]));
		} catch (ParseException e) {
			try {
				day.setTime(dfAlt.parse(item[0]));
			} catch (ParseException e1) {
				throw e1;
			}
		}
		day.set(Calendar.HOUR, 0);
		day.set(Calendar.MINUTE, 0);
		day.set(Calendar.SECOND, 0);
		day.set(Calendar.MILLISECOND, 0);

		OHLC bar = new OHLC(day.getTime(), pf.parse(item[1].replace(',', '.')).doubleValue(), pf.parse(item[2].replace(',', '.')).doubleValue(), pf.parse(item[3].replace(',', '.')).doubleValue(), pf.parse(item[4].replace(',', '.')).doubleValue(), nf.parse(item[5]).longValue());

		return bar;
	}

	protected OHLC parse1DayResponseLine(String inputLine) throws ParseException {
		String[] item = inputLine.split(","); //$NON-NLS-1$
		if (item.length < 6)
			return null;

		Date date = new Date(Long.parseLong(item[0]) * 1000);

		OHLC bar = new OHLC(date, pf.parse(item[4].replace(',', '.')).doubleValue(), pf.parse(item[2].replace(',', '.')).doubleValue(), pf.parse(item[3].replace(',', '.')).doubleValue(), pf.parse(item[1].replace(',', '.')).doubleValue(), nf.parse(item[5]).longValue());

		return bar;
	}

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.feed.IBackfillConnector#backfillDividends(org.eclipsetrader.core.feed.IFeedIdentifier, java.util.Date, java.util.Date)
	 */
	public IDividend[] backfillDividends(IFeedIdentifier identifier, Date from, Date to) {
		List<IDividend> list = new ArrayList<IDividend>();

		try {
			HttpMethod method = Util.getDividendsHistoryMethod(identifier, from, to);
			method.setFollowRedirects(true);

			HttpClient client = new HttpClient();
			client.executeMethod(method);

			BufferedReader in = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));

			// The first line is the header, ignoring
			String inputLine = in.readLine();
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.startsWith("<")) //$NON-NLS-1$
					continue;

				try {
					Dividend dividend = parseDividendsResponseLine(inputLine);
					if (dividend != null)
						list.add(dividend);
				} catch (ParseException e) {
					Status status = new Status(Status.ERROR, YahooActivator.PLUGIN_ID, 0, "Error parsing data: " + inputLine, e);
					YahooActivator.log(status);
				}
			}

			in.close();

		} catch (Exception e) {
			Status status = new Status(Status.ERROR, YahooActivator.PLUGIN_ID, 0, "Error reading data", e);
			YahooActivator.log(status);
		}

		return list.toArray(new IDividend[list.size()]);
	}

	protected Dividend parseDividendsResponseLine(String inputLine) throws ParseException {
		String[] item = inputLine.split(","); //$NON-NLS-1$
		if (item.length < 2)
			return null;

		Calendar day = Calendar.getInstance();
		try {
			day.setTime(df.parse(item[0]));
		} catch (ParseException e) {
			try {
				day.setTime(dfAlt.parse(item[0]));
			} catch (ParseException e1) {
				throw e1;
			}
		}
		day.set(Calendar.HOUR, 0);
		day.set(Calendar.MINUTE, 0);
		day.set(Calendar.SECOND, 0);
		day.set(Calendar.MILLISECOND, 0);

		return new Dividend(day.getTime(), pf.parse(item[1].replace(',', '.')).doubleValue());
	}

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.feed.IBackfillConnector#backfillSplits(org.eclipsetrader.core.feed.IFeedIdentifier, java.util.Date, java.util.Date)
	 */
	public ISplit[] backfillSplits(IFeedIdentifier identifier, Date from, Date to) {
		return null;
	}
}