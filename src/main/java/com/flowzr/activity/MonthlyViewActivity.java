package com.flowzr.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ListActivity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.adapter.CreditCardStatementAdapter;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.Account;
import com.flowzr.model.AccountType;
import com.flowzr.model.Currency;
import com.flowzr.model.TransactionInfo;
import com.flowzr.utils.MonthlyViewPlanner;
import com.flowzr.utils.TransactionList;
import com.flowzr.utils.Utils;
import com.flowzr.view.NodeInflater;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Display the credit card bill, including scheduled and future transactions for a given period.
 * Display only expenses, ignoring payments (positive values) in Credit Card accounts. 
 * @author Abdsandryk
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MonthlyViewActivity extends ListFragment {
	
    public static final String ACCOUNT_EXTRA = "account_id";
    public static final String BILL_PREVIEW_EXTRA = "bill_preview";

	private DatabaseAdapter dbAdapter;

	private long accountId = 0;
    private Account account;
    private Currency currency;
	private boolean isCreditCard = false;
	private boolean isStatementPreview = false;
	
	private String title;
	private int closingDay = 0;
	private int paymentDay = 0;

    private int month = 0;
    private int year = 0;
    private Calendar closingDate;

	private Utils u;

	private MainActivity activity;

    private MonthlyPreviewTask currentTask;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		activity=(MainActivity)context;
	}


	protected int getLayoutId() {
		return R.layout.monthly_view;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		NodeInflater nodeInflater = new NodeInflater(inflater);

		final Bundle args = getArguments();
		return inflater.inflate(getLayoutId(), container, false);
	}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        u = new Utils(activity);
		Bundle bundle = getArguments();
		if (bundle != null) {
			accountId = bundle.getLong(ACCOUNT_EXTRA, 0);
			isStatementPreview = bundle.getBoolean(BILL_PREVIEW_EXTRA, false);
		}
		initialize();
    }
    
    @Override
    public void onDestroy() {
        cancelCurrentTask();
        dbAdapter.close();
    	super.onDestroy();
    }

    private void cancelCurrentTask() {
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    
    
    /**
     * Initialize data and GUI elements.
     */
    private void initialize() {
    	
    	// get account data
		dbAdapter = new DatabaseAdapter(activity);
		dbAdapter.open();
		
		// set currency based on account
		MyEntityManager em = dbAdapter.em();
        account = em.getAccount(accountId);
		
        if (month==0 && year==0) {
	        // get current month and year in first launch
			Calendar cal = Calendar.getInstance();
			month = cal.get(Calendar.MONTH) + 1;
			year = cal.get(Calendar.YEAR);
        }

		// set part of the title, based on account name: "<CCARD> Bill"
		if (account != null) {
			
			// get account type
			isCreditCard = AccountType.valueOf(account.type).isCreditCard;
	
			currency = account.currency;
			
			if (isCreditCard) {
				if (isStatementPreview) { 
					// assuming that expensesOnly is true only if payment and closing days > 0 [BlotterFragment]
					title = getString(R.string.ccard_statement_title);
					String accountTitle = account.title;
					if (account.title==null || account.title.length()==0) {
						accountTitle = account.cardIssuer;
					}
					String toReplace = getString(R.string.ccard_par);
					title = title.replaceAll(toReplace, accountTitle);
					paymentDay = account.paymentDay;
					closingDay = account.closingDay;
					// set activity window title
					activity.setTitle(R.string.ccard_statement);
					setCCardTitle();
					setCCardInterval();
				} else {
					title = (account.title==null|| account.title.length()==0? account.cardIssuer: account.title);
					paymentDay = 1;
					closingDay = 31;
					setTitle();
					setInterval();
					
					// set payment date and label on total bar
					TextView totalLabel = (TextView) getView().findViewById(R.id.monthly_result_label);
					totalLabel.setText(getResources().getString(R.string.monthly_result));
				}
			} else {
				if (account.title==null|| account.title.length()==0) {
					if (isCreditCard) {
						// title = <CARD_ISSUER>
						title = account.cardIssuer;
					} else {
						// title = <ACCOUNT_TYPE_TITLE>
						AccountType type = AccountType.valueOf(account.type);
						title = getString(type.titleId);
					}
				} else {
					// title = <TITLE>
					title = account.title;
				}
				
				paymentDay = 1;
				closingDay = 31;
				setTitle();
				setInterval();
				
				// set payment date and label on total bar
				TextView totalLabel = (TextView) getView().findViewById(R.id.monthly_result_label);
				totalLabel.setText(getResources().getString(R.string.monthly_result));
			}

            ImageButton bPrevious = (ImageButton) getView().findViewById(R.id.bt_month_previous);
			bPrevious.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    month--;
                    if (month < 1) {
                        month = 12;
                        year--;
                    }
                    if (isCreditCard) {
                        if (isStatementPreview) {
                            setCCardTitle();
                            setCCardInterval();
                        } else {
                            setTitle();
                            setInterval();
                        }
                    } else {
                        setTitle();
                        setInterval();
                    }
                }
            });

            ImageButton bNext = (ImageButton) getView().findViewById(R.id.bt_month_next);
			bNext.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    month++;
                    if (month > 12) {
                        month = 1;
                        year++;
                    }
                    if (isCreditCard) {
                        if (isStatementPreview) {
                            setCCardTitle();
                            setCCardInterval();
                        } else {
                            setTitle();
                            setInterval();
                        }
                    } else {
                        setTitle();
                        setInterval();
                    }
                }
            });
		
		}
	}

    /**
     * Configure the interval based on the bill period of a credit card.
     * Attention: 
     *   Calendar.MONTH = 0 to 11
     *   integer  month = 1 to 12
     */
	private void setCCardInterval() {
		
		Calendar close = getClosingDate(month, year);
		Calendar open;
		
		if (month>1) {
			// closing date from previous month
			open = getClosingDate(month-1, year);
		} else {
			open = getClosingDate(12, year-1);
		}
		// add one day to the closing date of previous month
		open.add(Calendar.DAY_OF_MONTH, +1);
		
		// adjust time for closing day
		close.set(Calendar.HOUR_OF_DAY, 23);
		close.set(Calendar.MINUTE, 59);
		close.set(Calendar.SECOND, 59);
		
		this.closingDate = new GregorianCalendar(close.get(Calendar.YEAR), 
				  								 close.get(Calendar.MONTH),
				  								 close.get(Calendar.DAY_OF_MONTH));
		
		// Verify custom closing date
		int periodKey = Integer.parseInt(Integer.toString(close.get(Calendar.MONTH))+
					 	Integer.toString(close.get(Calendar.YEAR)));
		
		int cd = dbAdapter.getCustomClosingDay(accountId, periodKey);
		if (cd>0) {
			// use custom closing day
			close.set(Calendar.DAY_OF_MONTH, cd);
		}
		
		// Verify custom opening date = closing day of previous month + 1
		periodKey = Integer.parseInt(Integer.toString(open.get(Calendar.MONTH))+
				 	Integer.toString(open.get(Calendar.YEAR)));
		
		int od = dbAdapter.getCustomClosingDay(accountId, periodKey);
		if (od>0) {
			// use custom closing day
			open.set(Calendar.DAY_OF_MONTH, od);
			open.add(Calendar.DAY_OF_MONTH, +1);
		}
		
		fillData(open, close);
	}
	
    /**
     * Configure the interval in a monthly perspective.
     * Attention: 
     *   Calendar.MONTH = 0 to 11
     *   integer  month = 1 to 12
     */
	private void setInterval() {
		
		Calendar close = new GregorianCalendar(year, month-1, getLastDayOfMonth(month, year));
		Calendar open = new GregorianCalendar(year, month-1, 1);
		
		// adjust time for closing day
		close.set(Calendar.HOUR_OF_DAY, 23);
		close.set(Calendar.MINUTE, 59);
		close.set(Calendar.SECOND, 59);
        close.set(Calendar.MILLISECOND, 999);
		
		fillData(open, close);
	}
	
	 // Returns the day on which the credit card bill closes for a given month/year.
	private Calendar getClosingDate(int month, int year) {
		int m = month;
		if (closingDay > paymentDay) {
			m--;
		}
		int maxDay = getLastDayOfMonth(m, year);
		int day = closingDay;
		if (closingDay>maxDay) {
			day = maxDay;
		}
		
		return new GregorianCalendar(year, m-1, day);
	}
	
	private int getLastDayOfMonth(int month, int year) {
		Calendar calCurr = GregorianCalendar.getInstance();
		calCurr.set(year, month-1, 1); // Months are 0 to 11
		return calCurr.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
	}

    private class MonthlyPreviewTask extends AsyncTask<Void, Void, TransactionList> {

        private final Date open;
        private final Date close;
        private final Date now;

        private MonthlyPreviewTask(Date open, Date close, Date now) {
            this.open = open;
            this.close = close;
            this.now = now;
        }

        @Override
        protected void onPreExecute() {
            ((TextView)getView().findViewById(android.R.id.empty)).setText(R.string.calculating);
        }

        @Override
        protected TransactionList doInBackground(Void... voids) {
            MonthlyViewPlanner planner = new MonthlyViewPlanner(dbAdapter, account, isStatementPreview, open, close, now);
            TransactionList transactions;
            if (isStatementPreview) {
                transactions = planner.getCreditCardStatement();
            } else {
                transactions = planner.getPlannedTransactionsWithTotals();
            }
            return transactions;
        }

        @Override
        protected void onPostExecute(TransactionList monthlyPreviewReport) {
            List<TransactionInfo> transactions = monthlyPreviewReport.transactions;
            long total = monthlyPreviewReport.totals[0].balance;
            if (transactions == null || transactions.isEmpty()) {
                displayNoTransactions();
            } else { // display data

                // Mapping data to view
                CreditCardStatementAdapter expenses = new CreditCardStatementAdapter(getContext(), R.layout.credit_card_transaction, transactions, currency, accountId);
                expenses.setStatementPreview(isStatementPreview);
                setListAdapter(expenses);

                // calculate total
                // display total
                TextView totalText = (TextView)getView().findViewById(R.id.monthly_result);
                if (isStatementPreview) {
                    u.setAmountText(totalText, currency, (-1)*total, false);
                    //totalText.setTextColor(Color.BLACK);
                } else {
                    if (total<0) {
                        u.setAmountText(totalText, currency, (-1)*total, false);
                        u.setNegativeColor(totalText);
                    } else {
                        u.setAmountText(totalText, currency, total, false);
                        u.setPositiveColor(totalText);
                    }
                }
            }
        }
    }

	/**
	 * Get data for a given period and display the related credit card expenses.
	 * @param open Start of period.
	 * @param close End of period.
	 */
	private void fillData(Calendar open, Calendar close) {
        cancelCurrentTask();
        currentTask = new MonthlyPreviewTask(open.getTime(), close.getTime(), new Date());
        currentTask.execute();
    }

    private void displayNoTransactions() {
        TextView totalText = (TextView)getView().findViewById(R.id.monthly_result);
        // display total = 0
        u.setAmountText(totalText, currency, 0, false);
        //totalText.setTextColor(Color.BLACK);
        // hide list and display empty message
        ((TextView)getView().findViewById(android.R.id.empty)).setText(R.string.no_transactions);
        setListAdapter(null);
    }

    /**
	 * Adjust the title based on the credit card's payment day.
	 */
	private void setCCardTitle() {
		
		Calendar date = new GregorianCalendar(year, month-1, paymentDay);

        String monthStr = Integer.toString(date.get(Calendar.MONTH) + 1);
        String yearStr = Integer.toString(date.get(Calendar.YEAR));

        String paymentDayStr;
        if (paymentDay < 10) {
        	paymentDayStr = "0"+paymentDay;
        } else {
        	paymentDayStr = Integer.toString(paymentDay);
        }
		
		if (monthStr.length()<2) {
			monthStr = "0"+ monthStr;
		}
		
		String pd = paymentDayStr + "/" + monthStr + "/" + yearStr;
		
        // set payment date and label on title bar
		TextView label = (TextView)getView().findViewById(R.id.monthly_view_title);
		label.setText(title+"\n" + pd);
		// set payment date and label on total bar
		TextView totalLabel = (TextView) getView().findViewById(R.id.monthly_result_label);
		totalLabel.setText(getResources().getString(R.string.bill_on)+" "+pd);
	}
	
	/**
	 * Adjust the title based on the credit card's payment day.
	 */
	private void setTitle() {
		
		Calendar date = new GregorianCalendar(year, month-1, 1);
		 
		@SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM yyyy");
		String pd = dateFormat.format(date.getTime()); 
		
		TextView label = (TextView)getView().findViewById(R.id.monthly_view_title);
		label.setText(title+"\n" + pd);
		
	}

	// Update view 
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (resultCode) {
		case MainActivity.RESULT_OK:
			int update = data.getIntExtra(CCardStatementClosingDayActivity.UPDATE_VIEW, 0);
			if (update>0) {
				setCCardTitle();
				setCCardInterval();
			}
			break;
		case MainActivity.RESULT_CANCELED:
			break;
		}
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.statement_preview_menu, menu);
        getActivity().setTitle(getString(R.string.monthly_view));
        super.onCreateOptionsMenu(menu, inflater);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		//Intent intent = new Intent(this, CCardStatementClosingDayActivity.class);
		
		int closingDay = getClosingDate(month, year).get(Calendar.DAY_OF_MONTH);
		
		switch (item.getItemId()) {
			case R.id.opt_menu_closing_day:
				// call credit card closing day sending period
                Bundle bundle = new Bundle();
                bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, accountId);
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,CCardStatementClosingDayActivity.class.getCanonicalName());
                bundle.putLong(CCardStatementClosingDayActivity.PERIOD_MONTH, closingDate.get(Calendar.MONTH));
                bundle.putLong(CCardStatementClosingDayActivity.PERIOD_YEAR, closingDate.get(Calendar.YEAR));
                bundle.putLong(CCardStatementClosingDayActivity.ACCOUNT, accountId);
                bundle.putLong(CCardStatementClosingDayActivity.REGULAR_CLOSING_DAY, closingDay);
                activity.onFragmentMessage(MyFragmentAPI.REQUEST_BLOTTER,bundle);
	            return true;
	            
			default:
	            return super.onOptionsItemSelected(item);
		}
    }
}
