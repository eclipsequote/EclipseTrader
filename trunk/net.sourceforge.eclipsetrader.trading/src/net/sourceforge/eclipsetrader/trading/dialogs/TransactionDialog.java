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

package net.sourceforge.eclipsetrader.trading.dialogs;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.eclipsetrader.core.CorePlugin;
import net.sourceforge.eclipsetrader.core.db.Account;
import net.sourceforge.eclipsetrader.core.db.Security;
import net.sourceforge.eclipsetrader.core.db.Transaction;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class TransactionDialog extends TitleAreaDialog
{
    private Account account;
    private Security security;
    private Transaction transaction;
    private Text dateText;
    private Combo accountCombo;
    private Combo securityCombo;
    private Button buyButton;
    private Button sellButton;
    private Spinner quantitySpinner;
    private Spinner priceSpinner;
    private Spinner expensesSpinner;
    private Text totalText;
    private SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private NumberFormat nf = NumberFormat.getInstance();

    public TransactionDialog(Account account, Shell parentShell)
    {
        super(parentShell);
        this.account = account;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText("Transaction");

        nf.setGroupingUsed(true);
        nf.setMinimumIntegerDigits(1);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected Control createDialogArea(Composite parent)
    {
        Composite content = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        content.setLayout(gridLayout);
        content.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        Label label = new Label(content, SWT.NONE);
        label.setText("Date / Time");
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        dateText = new Text(content, SWT.BORDER);
        dateText.setLayoutData(new GridData(125, SWT.DEFAULT));
        dateText.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e)
            {
                try {
                    Date date = df.parse(dateText.getText());
                    dateText.setText(df.format(date));
                } catch(Exception e1) {
                }
            }
        });

        label = new Label(content, SWT.NONE);
        label.setText("Account");
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        accountCombo = new Combo(content, SWT.READ_ONLY);
        accountCombo.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        accountCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e)
            {
                updateTotals();
            }
        });

        label = new Label(content, SWT.NONE);
        label.setText("Security");
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        securityCombo = new Combo(content, SWT.READ_ONLY);
        securityCombo.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        securityCombo.setVisibleItemCount(25);
        securityCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e)
            {
                Security security = (Security)securityCombo.getData(securityCombo.getText());
                if (security != null && security.getQuote() != null)
                {
                    priceSpinner.setSelection((int)Math.round(security.getQuote().getLast() * Math.pow(10, priceSpinner.getDigits())));
                    updateTotals();
                }
            }
        });

        label = new Label(content, SWT.NONE);
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        Composite group = new Composite(content, SWT.NONE);
        gridLayout = new GridLayout(2, false);
        gridLayout.marginWidth = gridLayout.marginHeight = 0;
        group.setLayout(gridLayout);
        buyButton = new Button(group, SWT.RADIO);
        buyButton.setText("Buy");
        buyButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e)
            {
                updateTotals();
            }
        });
        sellButton = new Button(group, SWT.RADIO);
        sellButton.setText("Sell");
        sellButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e)
            {
                updateTotals();
            }
        });

        label = new Label(content, SWT.NONE);
        label.setText("Quantity");
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        quantitySpinner = new Spinner(content, SWT.BORDER);
        quantitySpinner.setMinimum(1);
        quantitySpinner.setMaximum(999999);
        quantitySpinner.setSelection(1);
        quantitySpinner.setLayoutData(new GridData(60, SWT.DEFAULT));
        quantitySpinner.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e)
            {
                updateTotals();
            }
        });

        label = new Label(content, SWT.NONE);
        label.setText("Price");
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        priceSpinner = new Spinner(content, SWT.BORDER);
        priceSpinner.setMinimum(0);
        priceSpinner.setMaximum(999999999);
        priceSpinner.setDigits(4);
        priceSpinner.setIncrement(100);
        priceSpinner.setSelection(0);
        priceSpinner.setLayoutData(new GridData(60, SWT.DEFAULT));
        priceSpinner.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e)
            {
                updateTotals();
            }
        });

        label = new Label(content, SWT.NONE);
        label.setText("Expenses");
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        expensesSpinner = new Spinner(content, SWT.BORDER);
        expensesSpinner.setMinimum(0);
        expensesSpinner.setMaximum(99999999);
        expensesSpinner.setDigits(2);
        expensesSpinner.setIncrement(1);
        expensesSpinner.setSelection(0);
        expensesSpinner.setLayoutData(new GridData(60, SWT.DEFAULT));

        label = new Label(content, SWT.NONE);
        label.setText("Total");
        label.setLayoutData(new GridData(125, SWT.DEFAULT));
        totalText = new Text(content, SWT.BORDER|SWT.READ_ONLY|SWT.RIGHT);
        totalText.setEnabled(false);
        totalText.setLayoutData(new GridData(60, SWT.DEFAULT));
        
        List list = CorePlugin.getRepository().allAccounts();
        Collections.sort(list, new Comparator() {
            public int compare(Object arg0, Object arg1)
            {
                return ((Account)arg0).getDescription().compareTo(((Account)arg1).getDescription());
            }
        });
        for (Iterator iter = list.iterator(); iter.hasNext(); )
        {
            Account s = (Account)iter.next();
            accountCombo.add(s.getDescription());
            accountCombo.setData(s.getDescription(), s);
        }
        if (account != null)
            accountCombo.setText(account.getDescription());
        
        list = CorePlugin.getRepository().allSecurities();
        Collections.sort(list, new Comparator() {
            public int compare(Object arg0, Object arg1)
            {
                return ((Security)arg0).getDescription().compareTo(((Security)arg1).getDescription());
            }
        });
        for (Iterator iter = list.iterator(); iter.hasNext(); )
        {
            Security s = (Security)iter.next();
            securityCombo.add(s.getDescription());
            securityCombo.setData(s.getDescription(), s);
        }
        
        if (transaction != null)
        {
            setTitle("Edit a Transaction");
            setMessage("Enter the details of the transaction to edit");
            dateText.setText(df.format(transaction.getDate()));
            securityCombo.setText(transaction.getSecurity().getDescription());
            buyButton.setSelection(transaction.getQuantity() >= 0);
            sellButton.setSelection(transaction.getQuantity() < 0);
            quantitySpinner.setSelection(Math.abs(transaction.getQuantity()));
            priceSpinner.setSelection((int)Math.round(transaction.getPrice() * Math.pow(10, priceSpinner.getDigits())));
            expensesSpinner.setSelection((int)Math.round(transaction.getExpenses() * Math.pow(10, expensesSpinner.getDigits())));
            totalText.setText(nf.format(transaction.getAmount()));
        }
        else
        {
            setTitle("Create a new Transaction");
            setMessage("Enter the details of the transaction to create");
            dateText.setText(df.format(Calendar.getInstance().getTime()));
            buyButton.setSelection(true);
        }
        
        if (security != null)
        {
            securityCombo.setText(security.getDescription());
            if (security.getQuote() != null)
            {
                priceSpinner.setSelection((int)Math.round(security.getQuote().getLast() * Math.pow(10, priceSpinner.getDigits())));
                updateTotals();
            }
            quantitySpinner.setFocus();
        }
        else
            securityCombo.setFocus();
        
        dateText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                try {
                    df.parse(dateText.getText());
                    setErrorMessage(null);
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                } catch(Exception e1) {
                    setErrorMessage("Invalid date and time format");
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                }
            }
        });

        return super.createDialogArea(parent);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.window.Window#open()
     */
    public int open(Transaction transaction)
    {
        this.transaction = transaction;
        return super.open();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.window.Window#open()
     */
    public int open(Security security)
    {
        this.security = security;
        return super.open();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    protected void okPressed()
    {
        if (transaction != null)
            account.getTransactions().remove(transaction);

        transaction = new Transaction();
        transaction.setDate(Calendar.getInstance().getTime());
        try {
            transaction.setDate(df.parse(dateText.getText()));
        } catch(Exception e) {
            CorePlugin.logException(e);
        }
        transaction.setSecurity((Security)securityCombo.getData(securityCombo.getText()));
        if (buyButton.getSelection())
            transaction.setQuantity(quantitySpinner.getSelection());
        else
            transaction.setQuantity(-quantitySpinner.getSelection());
        transaction.setPrice(priceSpinner.getSelection() / Math.pow(10, priceSpinner.getDigits()));
        transaction.setExpenses(expensesSpinner.getSelection() / Math.pow(10, expensesSpinner.getDigits()));
        
        account.getTransactions().add(transaction);
        CorePlugin.getRepository().save(account);
        
        super.okPressed();
    }
    
    private void updateTotals()
    {
        int quantity = quantitySpinner.getSelection();
        double price = priceSpinner.getSelection() / Math.pow(10, priceSpinner.getDigits());
        double total = quantity * price;

        double expenses = account.getFixedCommissions() + (total / 100.0 * account.getVariableCommissions());
        if (expenses < account.getMinimumCommission())
            expenses = account.getMinimumCommission();
        if (account.getMaximumCommission() != 0 && expenses > account.getMaximumCommission())
            expenses = account.getMaximumCommission();
        expensesSpinner.setSelection((int)Math.round(expenses * Math.pow(10, expensesSpinner.getDigits())));
        if (buyButton.getSelection())
            total += expenses;
        else
            total -= expenses;

        totalText.setText(nf.format(total));
    }
}