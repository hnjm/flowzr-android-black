/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TimePicker;

import com.flowzr.R;
import com.flowzr.datetime.DateUtils;
import com.flowzr.db.DatabaseHelper.AccountColumns;
import com.flowzr.model.Account;
import com.flowzr.model.Attribute;
import com.flowzr.model.Budget;
import com.flowzr.model.Category;
import com.flowzr.model.MyEntity;
import com.flowzr.model.MyLocation;
import com.flowzr.model.Payee;
import com.flowzr.model.SystemAttribute;
import com.flowzr.model.Total;
import com.flowzr.model.Transaction;
import com.flowzr.model.TransactionAttribute;
import com.flowzr.model.TransactionStatus;
import com.flowzr.recur.NotificationOptions;
import com.flowzr.recur.Recurrence;
import com.flowzr.utils.EnumUtils;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.TransactionUtils;
import com.flowzr.utils.Utils;
import com.flowzr.view.AttributeView;
import com.flowzr.view.AttributeViewFactory;
import com.flowzr.view.FloatingActionButton;
import com.flowzr.view.MyFloatingActionMenu;
import com.flowzr.view.NodeInflater;
import com.flowzr.widget.AmountInput;
import com.flowzr.widget.CalculatorInput;
import com.flowzr.widget.RateLayoutView;

import java.io.File;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


import static com.flowzr.utils.AndroidUtils.isCompatible;
import static com.flowzr.utils.ThumbnailUtil.PICTURES_DIR;
import static com.flowzr.utils.ThumbnailUtil.PICTURES_THUMB_DIR;
import static com.flowzr.utils.ThumbnailUtil.PICTURE_FILE_NAME_FORMAT;
import static com.flowzr.utils.ThumbnailUtil.createAndStoreImageThumbnail;
import static com.flowzr.utils.Utils.text;

public abstract class AbstractTransactionActivity extends AbstractEditorActivity implements CategorySelector.CategorySelectorListener {
	
	public static final String TRAN_ID_EXTRA = "tranId";
	public static final String ACCOUNT_ID_EXTRA = "accountId";
	public static final String DUPLICATE_EXTRA = "isDuplicate";
	public static final String TEMPLATE_EXTRA = "isTemplate";
    public static final String DATETIME_EXTRA = "dateTimeExtra";
    public static final String NEW_FROM_TEMPLATE_EXTRA = "newFromTemplateExtra";

	private static final int NEW_LOCATION_REQUEST = 4002;
	protected static final int RECURRENCE_REQUEST = 4003;
	private static final int NOTIFICATION_REQUEST = 4004;
	private static final int PICTURE_REQUEST = 4005;
    protected static final int CATEGORY_REQUEST = 4006;
	protected static final int NEW_PROJECT_REQUEST = 4007;
    protected static final int CALCULATOR_REQUEST = 4008;

	private static final TransactionStatus[] statuses = TransactionStatus.values();

	public static final String CURRENT_BALANCE_EXTRA = "accountCurrentBalance";
	public static final String AMOUNT_EXTRA = "accountAmount";
	public static final String ACTIVITY_STATE = "ACTIVITY_STATE";

	protected static final int SPLIT_REQUEST = 5001;
	protected static final int BLOTTER_PREFERENCES = 5002;

	public static final String STATUS_EXTRA = "statusExtra";
	public static final String LOCATION_ID_EXTRA = "locationId";
	public static final String PAYEE_ID_EXTRA = "payeeId";
	public static final String CATEGORY_ID_EXTRA ="categoryId" ;
	public static final String PROJECT_ID_EXTRA="projectId";
	public static final String BUDGET_ID_EXTRA="budgetId";

	protected RateLayoutView rateView;

    protected EditText templateName;
	protected TextView accountText;	
	protected Cursor accountCursor;
	protected ListAdapter accountAdapter;
	
	protected TextView locationText;
	protected Cursor locationCursor;
	protected ListAdapter locationAdapter;

	protected Calendar dateTime;
	protected ImageButton status;
	protected Button dateText;
	protected Button timeText;
	
    protected EditText noteText;
	protected TextView recurText;	
	protected TextView notificationText;	
	
	private ImageView pictureView;
	private String pictureFileName;
	
	private CheckBox ccardPayment;
	
	protected Account selectedAccount;

	protected long selectedLocationId = 0;
	protected String recurrence;
	protected String notificationOptions;
	
	private LocationManager locationManager;
	private Location lastFix;

    protected boolean isDuplicate = false;
	
	private boolean setCurrentLocation;

    protected ProjectSelector projectSelector;
    protected CategorySelector categorySelector;

    protected boolean isRememberLastAccount;
	protected boolean isRememberLastCategory;
	protected boolean isRememberLastLocation;
	protected boolean isRememberLastProject;
	protected boolean isShowLocation;
	protected boolean isShowNote;
	protected boolean isShowTakePicture;
	protected boolean isShowIsCCardPayment;
	protected boolean isOpenCalculatorForTemplates;

    protected boolean isShowPayee = true;
    protected AutoCompleteTextView payeeText;
    protected SimpleCursorAdapter payeeAdapter;

	protected AttributeView deleteAfterExpired;
	
	protected DateFormat df;
	protected DateFormat tf;
	
	protected Transaction transaction = new Transaction();
	protected TextView totalText;

	private boolean locationShown=false;
	//private AmountInput amountInput;

	public AbstractTransactionActivity() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		NodeInflater nodeInflater = new NodeInflater(inflater);
		x = new ActivityLayout(nodeInflater, this);
		return inflater.inflate(getLayoutId(), container, false);
	}


	public void onAttach(Context a) {
		super.onAttach(a);
		setHasOptionsMenu(true);
		activity=(MainActivity)a;
	}

    protected boolean saveAndFinish() {
        long id = save();
        if (id > 0) {
            Bundle bundle = new Bundle();
            bundle.putLong(ACCOUNT_ID_EXTRA,transaction.fromAccountId);
            bundle.putLong(TRAN_ID_EXTRA,transaction.id);
            bundle.putBoolean(TEMPLATE_EXTRA,transaction.isTemplate());
            bundle.putLong(DATETIME_EXTRA,transaction.dateTime);
			bundle.putInt(MyFragmentAPI.RESULT_EXTRA,AppCompatActivity.RESULT_OK);
			bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA,transaction.id);
            activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
            return true;
        }
        return false;
    }


    public boolean finishAndClose(int result) {
        Bundle bundle = new  Bundle();
        bundle.putInt(MyFragmentAPI.RESULT_EXTRA,result);
        activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
        return true;
    }


    public boolean finishAndClose(Bundle bundle) {
        activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
        return true;
    }

    private long save() {
        if (onOKClicked()) {
            boolean isNew = transaction.id == -1;
            long id = db.insertOrUpdate(transaction, getAttributes());
            if (isNew) {
                MyPreferences.setLastAccount(getContext(), transaction.fromAccountId);
            }
            AccountWidget.updateWidgets(getContext());
            return id;
        }
        return -1;
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.transaction_actions, menu);
        if (isCompatible(14)) {
            menu.removeItem(R.id.saveAddButton);
            menu.removeItem(R.id.saveButton);
        }
    }




	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		u = new Utils(getContext());

		df = DateUtils.getShortDateFormat(getContext());
		tf = DateUtils.getTimeFormat(getContext());

		long t0 = System.currentTimeMillis();

		isRememberLastAccount = MyPreferences.isRememberAccount(getContext());
		isRememberLastCategory = isRememberLastAccount && MyPreferences.isRememberCategory(getContext());
		isRememberLastLocation = isRememberLastCategory && MyPreferences.isRememberLocation(getContext());
		isRememberLastProject = isRememberLastCategory && MyPreferences.isRememberProject(getContext());
		isShowLocation = MyPreferences.isShowLocation(getContext());
		isShowNote = MyPreferences.isShowNote(getContext());
		isShowTakePicture = MyPreferences.isShowTakePicture(getContext());
		isShowIsCCardPayment = MyPreferences.isShowIsCCardPayment(getContext());
		isOpenCalculatorForTemplates = MyPreferences.isOpenCalculatorForTemplates(getContext());

		categorySelector = new CategorySelector(activity,this, db, x);
		categorySelector.setListener(this);
		fetchCategories();

		projectSelector = new ProjectSelector(activity,this, em, x);
		projectSelector.fetchProjects();

		if (isShowLocation) {
			locationCursor = em.getAllLocations(true);
			getActivity().startManagingCursor(locationCursor);
			locationAdapter = TransactionUtils.createLocationAdapter(getContext(), locationCursor);
		}

		long accountId = -1;
		long transactionId = -1;
		boolean isNewFromTemplate = false;

		//Intent intent = getActivity().getIntent();
		Bundle bundle = getArguments();
		if (bundle != null) {
			accountId = bundle.getLong(ACCOUNT_ID_EXTRA, -1);
			transactionId = bundle.getLong(TRAN_ID_EXTRA, -1);
			transaction.dateTime = bundle.getLong(DATETIME_EXTRA, System.currentTimeMillis());
			if (transactionId != -1) {
				transaction = db.getTransaction(transactionId);
				transaction.categoryAttributes = db.getAllAttributesForTransaction(transactionId);
				isDuplicate = bundle.getBoolean(DUPLICATE_EXTRA, false);
				isNewFromTemplate = bundle.getBoolean(NEW_FROM_TEMPLATE_EXTRA, false);
				if (isDuplicate) {
					transaction.id = -1;
					transaction.dateTime = System.currentTimeMillis();
				}
			}
			transaction.isTemplate = bundle.getInt(TEMPLATE_EXTRA, transaction.isTemplate);
		}

		if (transaction.id == -1) {
			accountCursor = em.getAllActiveAccounts();
		} else {
			accountCursor = em.getAccountsForTransaction(transaction);
		}
		getActivity().startManagingCursor(accountCursor);
		accountAdapter = TransactionUtils.createAccountAdapter(getContext(), accountCursor);

		dateTime = Calendar.getInstance();
		Date date = dateTime.getTime();

		status = (ImageButton) getView().findViewById(R.id.status);
		status.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(AbstractTransactionActivity.this.getContext(), statuses);
				x.selectPosition(AbstractTransactionActivity.this.getContext(), R.id.status, R.string.transaction_status, adapter, transaction.status.ordinal());
			}
		});

		dateText = (Button) getView().findViewById(R.id.date);
		dateText.setText(df.format(date));
		dateText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				DatePickerDialog d = new DatePickerDialog(AbstractTransactionActivity.this.getContext(), new OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker arg0, int y, int m, int d) {
						dateTime.set(y, m, d);
						dateText.setText(df.format(dateTime.getTime()));
					}
				}, dateTime.get(Calendar.YEAR), dateTime.get(Calendar.MONTH), dateTime.get(Calendar.DAY_OF_MONTH));
				d.show();
			}
		});

		timeText = (Button) getView().findViewById(R.id.time);
		timeText.setText(tf.format(date));
		timeText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				boolean is24Format = DateUtils.is24HourFormat(AbstractTransactionActivity.this.getContext());
				TimePickerDialog d = new TimePickerDialog(AbstractTransactionActivity.this.getContext(), new OnTimeSetListener() {
					@Override
					public void onTimeSet(TimePicker picker, int h, int m) {
						dateTime.set(Calendar.HOUR_OF_DAY, picker.getCurrentHour());
						dateTime.set(Calendar.MINUTE, picker.getCurrentMinute());
						timeText.setText(tf.format(dateTime.getTime()));
					}
				}, dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE), is24Format);
				d.show();
			}
		});

		internalOnCreate();

		LinearLayout layout = (LinearLayout) getView().findViewById(R.id.listlayout);

		LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.templateName = (EditText) layoutInflater.inflate(R.layout.edit_text, null);

		if (transaction.isTemplate()) {
			x.addEditNode(layout, R.string.template_name, templateName);
		}

		//amountInput = new AmountInput(getContext());
		//amountInput.setOwner(this);

        rateView = new RateLayoutView(this, x, layout);

		createListNodes(layout);
		rateView.hideFromAmount();

		categorySelector.createAttributesLayout(layout);
		createCommonNodes(layout);

		if (transaction.isScheduled()) {
			recurText = x.addListNode2(layout, R.id.recurrence_pattern,R.drawable.ic_schedule, R.string.recur, getResources().getString(R.string.recur_interval_no_recur));
			notificationText = x.addListNode2(layout, R.id.notification,R.drawable.ic_alarm, R.string.notification, getResources().getString(R.string.notification_options_default));
			Attribute sa = db.getSystemAttribute(SystemAttribute.DELETE_AFTER_EXPIRED);
			deleteAfterExpired = AttributeViewFactory.createViewForAttribute(getContext(), sa);
			//String value = transaction.getSystemAttribute(SystemAttribute.DELETE_AFTER_EXPIRED);
			//deleteAfterExpired.inflateView(layout, value != null ? value : sa.defaultValue);
			x.addCheckboxNode(layout,R.id.deleteAfterExpired,R.string.system_attribute_delete_after_expired, R.drawable.ic_delete, R.string.system_attribute_delete_after_expired, true);
		}

		//final boolean isEdit = transaction.id > 0;
		if (transactionId != -1) {
			isOpenCalculatorForTemplates &= isNewFromTemplate;
			editTransaction(transaction);
		} else {
			setDateTime(transaction.dateTime);
			categorySelector.selectCategory(0);
			if (accountId != -1) {
				selectAccount(accountId);
			} else {
				long lastAccountId = MyPreferences.getLastAccount(getContext());
				if (isRememberLastAccount && lastAccountId != -1) {
					selectAccount(lastAccountId);
				}
			}

			if (!isRememberLastProject) {
				projectSelector.selectProject(0);
			}

			if (!isRememberLastLocation && isShowLocation) {
				selectCurrentLocation(false);
			}
			if (transaction.isScheduled()) {
				selectStatus(TransactionStatus.PN);
			}

			if (bundle.containsKey(BUDGET_ID_EXTRA)) {
				Budget b=em.load(Budget.class,bundle.getLong(BUDGET_ID_EXTRA,-1));
				if (b!=null && b.projects!=null) {
					long[] pids = MyEntity.splitIds(b.projects);
					if (pids!=null) {
						projectSelector.fetchProjects(pids);
						if (pids.length>0) {
							projectSelector.selectProject(pids[0]);
						}
					}
				}
				if (b!=null && b.categories!=null) {
					long[] cids = MyEntity.splitIds(b.categories);
					if (cids!=null) {
						fetchCategories(cids);
						if (cids.length>0) {
							categorySelector.selectCategory(cids[0]);
						}
					}
				}
			}

			if (bundle.containsKey(PROJECT_ID_EXTRA)) {
				projectSelector.selectProject(bundle.getLong(PROJECT_ID_EXTRA, 0));
			}

			if (bundle.containsKey(CATEGORY_ID_EXTRA)) {
				transaction.categoryId=bundle.getLong(CATEGORY_ID_EXTRA,0);
				categorySelector.selectCategory(bundle.getLong(CATEGORY_ID_EXTRA, 0), false);
			}

			if (bundle.containsKey(PAYEE_ID_EXTRA)) {
				selectPayee(bundle.getLong(PAYEE_ID_EXTRA, -1));
			}

			if (bundle.containsKey(LOCATION_ID_EXTRA)) {
				selectLocation(bundle.getLong(LOCATION_ID_EXTRA, -1));
			}


			if (bundle.containsKey(STATUS_EXTRA)) {
				selectStatus(TransactionStatus.valueOf(bundle.getString(STATUS_EXTRA)));
			}
		}



        if (getView().findViewById(R.id.fab1)!=null) {
            getView().findViewById(R.id.fab1).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onOKClicked();
                    saveAndFinish();
                }
            });
        }


        totalText = ( TextView ) getView().findViewById(R.id.total);
        totalText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getContext(), CalculatorInput.class);
                //if (budget.currencyId > -1) {
                //    intent.putExtra(AmountInput.EXTRA_CURRENCY, budget.currencyId);
                //}
                //transaction.fromAmount
                rateView.openFromAmountCalculator(transaction.toString()); //.openCalculator(transaction.toString());

            }
        });

        Total t = new Total(rateView.getCurrencyFrom());
        t.balance = transaction.fromAmount;
        u.setTotal(totalText, t);


		if (transactionId == -1) {
			String title=(getResources().getString(R.string.add_transaction));
			if (transaction.isTransfer()|| bundle.containsKey(TransactionActivity.IS_TRANSFER_EXTRA)) {
				title=getResources().getString(R.string.add_transfer);
			}
			if (transaction.isTemplate()) {
				title=getResources().getString(R.string.template);
			}
			//rateView.openFromAmountCalculator(title);
		}
            setupFab();
			long t1 = System.currentTimeMillis();
			Log.i("TransactionActivity","onCreate "+(t1-t0)+"ms");
		}

    public void setupFab() {
        if (isCompatible(14)) {
            final MyFloatingActionMenu menu1 = (MyFloatingActionMenu) getActivity().findViewById(R.id.menu1);
            FloatingActionButton fab1 = (FloatingActionButton) activity.findViewById(R.id.fab1);
            FloatingActionButton fab2 = (FloatingActionButton) activity.findViewById(R.id.fab2);
            menu1.getMenuIconView().setImageResource(R.drawable.ic_check);
            fab1.setImageResource(R.drawable.ic_check);
            fab2.setImageResource(R.drawable.ic_add);
            fab1.setLabelText(getResources().getString(R.string.save));
            fab2.setLabelText(getResources().getString(R.string.save_and_new));

            if (menu1!=null) {
                    menu1.setOnMenuButtonClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    menu1.toggle(true);
                                }
                            });

                    Handler mUiHandler = new Handler();
                    List<MyFloatingActionMenu> menus = new ArrayList<>();
                    menus.add(menu1);
                    //menu1.showMenuButton(true);
                    int delay = 400;
                    for (final MyFloatingActionMenu menu : menus) {
                        mUiHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                menu.showMenuButton(true);
                            }
                        }, delay);
                        delay += 150;
                    }
                    menu1.setClosedOnTouchOutside(true);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            menu1.showMenuButton(true);
                        }
                    }, delay + 150);

                    menu1.setOnMenuToggleListener(new MyFloatingActionMenu.OnMenuToggleListener() {
                        @Override
                        public void onMenuToggle(boolean opened) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    menu1.close(true);
                                    menu1.close(true);
                                }
                            },3500);
                        }
                    });

                    if (getActivity().findViewById(R.id.fab1)!=null) {
                        getActivity().findViewById(R.id.fab1).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onOKClicked();
                                saveAndFinish();
                            }
                        });
                    }

                    if (getActivity().findViewById(R.id.fab2)!=null) {
                        getActivity().findViewById(R.id.fab2).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onOKClicked();
                                Intent intent2= getActivity().getIntent();
                                intent2.putExtra(DATETIME_EXTRA, transaction.dateTime);
                                if (saveAndFinish()) {
                                    intent2.putExtra(DATETIME_EXTRA, transaction.dateTime);
                                    Bundle bundle = new Bundle();
                                    if (transaction.isTransfer()) {
                                        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,TransferActivity.class.getCanonicalName());

                                    } else {
                                        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,TransactionActivity.class.getCanonicalName());

                                    }
                                    bundle.putAll(intent2.getExtras());
                                    activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
                                }
                            }
                        });
                    }

                }



                menu1.showMenu(true);
                getView().findViewById(R.id.scroll)
                        .setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                switch (event.getAction()) {
                                    case MotionEvent.ACTION_SCROLL:
                                    case MotionEvent.ACTION_MOVE:
                                        menu1.hideMenu(true);
                                        break;
                                    case MotionEvent.ACTION_CANCEL:
                                    case MotionEvent.ACTION_UP:

                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                menu1.showMenu(true);
                                                new Handler().postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        //menu1.hideMenu(true);
                                                    }
                                                }, 10000);

                                            }
                                        }, 3000);
                                        break;
                                }
                                return false;
                            }
                        });
            }


    }

	protected void createPayeeNode(LinearLayout layout) {
        payeeAdapter = TransactionUtils.createPayeeAdapter(getContext(), db);
        LayoutInflater layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        payeeText = (AutoCompleteTextView) layoutInflater.inflate(R.layout.autocomplete, null);
        payeeText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS |
				InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
				InputType.TYPE_TEXT_VARIATION_FILTER);
        payeeText.setThreshold(1);
        payeeText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    payeeText.setAdapter(payeeAdapter);
                    payeeText.selectAll();
                }
            }
        });
        x.addEditNode2(layout, R.drawable.ic_person, payeeText);
    }

    protected abstract void fetchCategories();

	protected abstract void fetchCategories(long[] cids);


    private List<TransactionAttribute> getAttributes() {
        List<TransactionAttribute> attributes = categorySelector.getAttributes();
        if (deleteAfterExpired != null) {
            TransactionAttribute ta = deleteAfterExpired.newTransactionAttribute();
            attributes.add(ta);
        }
        return attributes;
    }

    protected void internalOnCreate() {
	}

	protected void selectCurrentLocation(boolean forceUseGps) {
        setCurrentLocation = true;
        selectedLocationId = 0;

		if (transaction.isTemplateLike()) {
			if (isShowLocation) {
				locationText.setText(R.string.current_location);
			}
			return;
		}		
		if (!locationShown)  {
			isShowLocation=true;
			locationText= x.addListNode2((LinearLayout) getView().findViewById(R.id.listlayout), R.id.location, R.drawable.ic_my_location, R.string.location, getResources().getString(R.string.select_location));
			locationShown=true;
		}
        // Start listener to find current location
        locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(new Criteria(), true);
        
        if (provider != null) {
        	lastFix = locationManager.getLastKnownLocation(provider);        	
        }  

        if (lastFix != null) {
        	setLocation(lastFix);
        	connectGps(forceUseGps);
        } else {
        	// No enabled providers found, so disable option
        	if (isShowLocation) {
        		locationText.setText(R.string.no_fix);
        	}
        }
	}

	private void connectGps(boolean forceUseGps) {
		if (locationManager != null) {
			boolean useGps = forceUseGps || MyPreferences.isUseGps(getContext());
            try {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {	    	        
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkLocationListener);	        	    	        
                }
            } catch (Exception e) {
                Log.e("Financisto", "Unable to connect network provider");
            }
            try {
                if (useGps && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, gpsLocationListener);
                }
            } catch (Exception e) {
                Log.e("Financisto", "Unable to connect gps provider");
            }
		}
	}

	private void disconnectGPS() {
		if (locationManager != null) {
			locationManager.removeUpdates(networkLocationListener);
			locationManager.removeUpdates(gpsLocationListener);
		}
	}

	@Override
	public void onDestroy() {
		disconnectGPS();
		super.onDestroy();
	}

	@Override
	public void onPause() {
		disconnectGPS();
		super.onPause();
	}

    @Override
    protected boolean shouldLock() {
        return MyPreferences.isPinProtectedNewTransaction(getContext());
    }

    @Override
	public void onResume() {
		super.onResume();
		if (lastFix != null) {
			connectGps(false);
		}
	}

	private class DefaultLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			lastFix = location;
			if (setCurrentLocation) {
				setLocation(location);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
		
	}

	private final LocationListener networkLocationListener = new DefaultLocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			super.onLocationChanged(location);
			locationManager.removeUpdates(networkLocationListener);
		}

	};
	
	private final LocationListener gpsLocationListener = new DefaultLocationListener();

	protected void createCommonNodes(LinearLayout layout) {
		int locationOrder = MyPreferences.getLocationOrder(getContext());
		int noteOrder = MyPreferences.getNoteOrder(getContext());
		int projectOrder = MyPreferences.getProjectOrder(getContext());
		for (int i=0; i<6; i++) {
			if (i == locationOrder) {
				if (isShowLocation) {
					//location
					connectGps(true);
					//locationText = x.addListNode2(layout, R.id.location, R.drawable.ic_action_location_found, R.string.select_location);
					locationText= x.addListNode2(layout, R.id.location, R.drawable.ic_my_location, R.string.location, getResources().getString(R.string.select_location));

					locationShown=true;
					//amount
				}
			}
			if (i == noteOrder) {
				if (isShowNote) {
					//note
					LayoutInflater layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					noteText = (EditText) layoutInflater.inflate(R.layout.edit_text, null);
					x.addEditNode2(layout,  R.drawable.ic_subject, noteText);
				}
			}
			if (i == projectOrder) {
                projectSelector.createNode(layout);
			}
		}

		if (isShowTakePicture && transaction.isNotTemplateLike()) {
			pictureView = x.addPictureNodeMinus(getContext(), layout, R.id.attach_picture,R.drawable.ic_camera_alt, R.id.delete_picture, R.string.attach_picture, R.string.new_picture);
		}
		if (isShowIsCCardPayment) {
			// checkbox to register if the transaction is a credit card payment. 
			// this will be used to exclude from totals in bill preview
			ccardPayment = x.addCheckboxNode(layout, R.id.is_ccard_payment,
					R.string.is_ccard_payment, R.drawable.ic_credit_card, R.string.is_ccard_payment_summary, false);
		}
	}

    protected abstract void createListNodes(LinearLayout layout);
	
	protected abstract boolean onOKClicked();

	@Override
	protected void onClick(View v, int id) {
        projectSelector.onClick(id);
        categorySelector.onClick(id);
		switch(id) {
			case R.id.account:				
				x.select(getContext(), R.id.account, R.string.account, accountCursor, accountAdapter,
                        AccountColumns.ID, getSelectedAccountId());
				break;
			case R.id.location: {
				x.selectWithAddOption(getContext(), R.id.location, R.string.location, locationCursor, locationAdapter, "_id", selectedLocationId, R.string.create,NEW_LOCATION_REQUEST);
				break;
			}
			case R.id.location_add: {
				Bundle bundle=new Bundle();
				bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,LocationActivity.class.getCanonicalName());
                // @TODO location activity to fragment
                //Fragment fragment = new LocationActivity();
                //fragment.setArguments(bundle);
                //fragment.setTargetFragment(this,0);
                //activity.startFragmentForResult(fragment);
				break;
			}
			case R.id.recurrence_pattern: {
				Bundle bundle=new Bundle();
				bundle.putString(RecurrenceActivity.RECURRENCE_PATTERN, recurrence);
                bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,RECURRENCE_REQUEST);
                Fragment fragment = new RecurrenceActivity();
                fragment.setArguments(bundle);
                activity.startFragmentForResult(fragment,this);
				break;
			}
			case R.id.notification: {
                Bundle bundle = new Bundle();
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,NotificationOptionsActivity.class.getCanonicalName());
                bundle.putString(NotificationOptionsActivity.NOTIFICATION_OPTIONS,notificationOptions);
                Fragment fragment = new NotificationOptionsActivity();
                fragment.setArguments(bundle);
                activity.startFragmentForResult(fragment,this);
				break;
			}
			case R.id.attach_picture: {
				PICTURES_DIR.mkdirs();
				PICTURES_THUMB_DIR.mkdirs();				
				pictureFileName = PICTURE_FILE_NAME_FORMAT.format(new Date())+".jpg";
				transaction.blobKey=null;		
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, 
						Uri.fromFile(new File(PICTURES_DIR, pictureFileName)));
                startActivityForResult(intent,PICTURE_REQUEST);
                //activity.onFragmentMessage(MyFragmentAPI.REQUEST_ACTIVITY,PICTURE_REQUEST,intent,this);
				break;
			}
			case R.id.delete_picture: {
				removePicture();
				break;
			}
			case R.id.is_ccard_payment: {
				ccardPayment.setChecked(!ccardPayment.isChecked());
				transaction.isCCardPayment = ccardPayment.isChecked()?1:0;
			}
			case R.id.deleteAfterExpired: {
				break;
			}
		}
	}	

	@Override
	public void onSelectedPos(int id, int selectedPos) {
        projectSelector.onSelectedPos(id, selectedPos);
		switch(id) {
			case R.id.status:
				selectStatus(statuses[selectedPos]);
				break;
		}
	}
 
	@Override
	public void onSelectedId(int id, long selectedId) {
		switch(id) {
			case NEW_LOCATION_REQUEST:

				break;
			case R.id.account:				
				selectAccount(selectedId);
				break;
			case R.id.location:
				if (selectedId==NEW_LOCATION_REQUEST) {
					Intent intent = new Intent(getContext(), LocationActivity.class);
                    activity.onFragmentMessage(MyFragmentAPI.REQUEST_ACTIVITY,NEW_LOCATION_REQUEST,intent,this);
					break;
				}
				selectLocation(selectedId);
				break;
		}
		categorySelector.onSelectedId(id, selectedId);

	}
	
	private void selectStatus(TransactionStatus transactionStatus) {
		transaction.status = transactionStatus;
		status.setImageResource(transactionStatus.iconId);
	}

	protected Account selectAccount(long accountId) {
		return selectAccount(accountId, true);
	}
	
	protected Account selectAccount(long accountId, boolean selectLast) {
        Account a = em.getAccount(accountId);
        if (a != null) {
            accountText.setText(a.title);
            rateView.selectCurrencyFrom(a.currency);
            selectedAccount = a;
        }
        return a;
	}

    protected long getSelectedAccountId() {
        return selectedAccount != null ? selectedAccount.id : -1;
    }

    @Override
    public void onCategorySelected(Category category, boolean selectLast) {
        addOrRemoveSplits();
        categorySelector.addAttributes(transaction);
        switchIncomeExpenseButton(category);
        if (selectLast && isRememberLastLocation) {
            selectLocation(category.lastLocationId);
        }
        if (selectLast && isRememberLastProject) {
            projectSelector.selectProject(category.lastProjectId);
        }
        projectSelector.setProjectNodeVisible(!category.isSplit());
    }

    protected abstract void switchIncomeExpenseButton(Category category);

    protected void addOrRemoveSplits() {
    }



	private void selectLocation(long locationId) {
		if (locationId == 0) {
			selectCurrentLocation(false);
		} else {
			if (isShowLocation) {
                MyLocation location = em.get(MyLocation.class, locationId);
				if (location != null) {
					locationText.setText(location.toString());
					selectedLocationId = locationId;
					setCurrentLocation = false;
				}
			}
		}
	}

	private void setRecurrence(String recurrence) {
		this.recurrence = recurrence;
		if (recurrence == null) {
			recurText.setText(R.string.recur_interval_no_recur);
			dateText.setEnabled(true);
			timeText.setEnabled(true);
		} else {			
			dateText.setEnabled(false);
			timeText.setEnabled(false);
			Recurrence r = Recurrence.parse(recurrence);
			recurText.setText(r.toInfoString(getContext()));
		}
	}

	private void setNotification(String notificationOptions) {
		this.notificationOptions = notificationOptions;
		if (notificationOptions == null) {
			notificationText.setText(R.string.notification_options_default);
		} else {			
			NotificationOptions o = NotificationOptions.parse(notificationOptions);
			notificationText.setText(o.toInfoString(getContext()));
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

        projectSelector.onActivityResult(requestCode, resultCode, data);
        categorySelector.onActivityResult(requestCode, resultCode, data);

		if (resultCode == AppCompatActivity.RESULT_OK) {

            //rateView.onActivityResult(requestCode,resultCode);
            //if (rateView.processActivityResult(requestCode, data)) {
            //    transaction.fromAmount=amountInput.getAmount();
            //    Total t = new Total(rateView.getCurrencyFrom());
            //    t.balance=transaction.fromAmount;
            //    u.setTotal(totalText,t);
            //}


            rateView.onActivityResult(requestCode, data);
			switch (requestCode) {
				case NEW_LOCATION_REQUEST:
					locationCursor.requery();
					long locationId = data.getLongExtra(LocationActivity.LOCATION_ID_EXTRA, -1);
					if (locationId != -1) {
						selectLocation(locationId);
					}
					break;
				case RECURRENCE_REQUEST:
					String recurrence = data.getStringExtra(RecurrenceActivity.RECURRENCE_PATTERN);
					setRecurrence(recurrence);
					break;
				case NOTIFICATION_REQUEST:					
					String notificationOptions = data.getStringExtra(NotificationOptionsActivity.NOTIFICATION_OPTIONS);
					setNotification(notificationOptions);
					break;
				case PICTURE_REQUEST:
                    transaction.blobKey=null;
					selectPicture(pictureFileName);	
					break;
                case CATEGORY_REQUEST:
                    long id=data.getLongExtra(MyFragmentAPI.ENTITY_ID_EXTRA,0);
                    categorySelector.selectCategory(id);
                    break;
				default:
					break;
			}
		} else {
			if (requestCode == PICTURE_REQUEST) {
				removePicture();
			}
		}
	}
	
	private void selectPicture(String pictureFileName) {
		if (pictureView == null) {
			return;
		}
		if (pictureFileName == null) {
			return;
		}
		File pictureFile = new File(PICTURES_DIR, pictureFileName);
		if (pictureFile.exists()) {
            Bitmap thumb = createThumbnail(pictureFile);
            pictureView.setImageBitmap(thumb);
            pictureView.setAdjustViewBounds(true); // set the ImageView bounds to match the Drawable's dimensions
            transaction.attachedPicture = pictureFileName;

		}				
	}

    private void selectPicture2(final String pictureFileName) {
        if (pictureView == null) {
            return;
        }
        if (pictureFileName == null) {
            return;
        }
			final Fragment f =this;
            pictureView.setImageResource(R.drawable.ic_action_drive);
            pictureView.setTag(pictureFileName);
            pictureView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pictureFileName));
                    // nothing to edit (CANCELED)
                    activity.onFragmentMessage(MyFragmentAPI.REQUEST_ACTIVITY,AppCompatActivity.RESULT_CANCELED,browserIntent,f);
                }
            });
    }


	private void removePicture() {
		if (pictureView == null) {
			return;
		}
		if (pictureFileName != null) {
			new File(PICTURES_DIR, pictureFileName).delete();
			new File(PICTURES_THUMB_DIR, pictureFileName).delete();
		}		
		pictureFileName = null;
		transaction.attachedPicture = null;
		transaction.blobKey=null;	
		pictureView.setImageBitmap(null);		
		pictureView.setTag(null);
	}

	private Bitmap createThumbnail(File pictureFile) {
		return createAndStoreImageThumbnail(getActivity().getContentResolver(), pictureFile);
	}

	protected void setDateTime(long date) {
		Date d = new Date(date);
		dateTime.setTime(d);
		dateText.setText(df.format(d));
		timeText.setText(tf.format(d));		
	}

    protected abstract void editTransaction(Transaction transaction);

	protected void commonEditTransaction(Transaction transaction) {
		selectStatus(transaction.status);
		categorySelector.selectCategory(transaction.categoryId, false);
		projectSelector.selectProject(transaction.projectId);
		setDateTime(transaction.dateTime);		
		if (transaction.locationId > 0 ) {
			selectLocation(transaction.locationId);
		} else {
			if (isShowLocation) {
				setLocation(transaction.provider, transaction.accuracy, transaction.latitude, transaction.longitude);
			}
		}
		if (isShowNote) {
			noteText.setText(transaction.note);
		}
		if (transaction.isTemplate()) {
			templateName.setText(transaction.templateName);
		}
		if (transaction.isScheduled()) {
			setRecurrence(transaction.recurrence);
			setNotification(transaction.notificationOptions);
		}
		if (isShowTakePicture) {
            if (transaction.attachedPicture!=null ) {
                selectPicture(transaction.attachedPicture);
            } else if (transaction.blobKey!=null && !"".equals(transaction.blobKey)) {
				selectPicture2(transaction.blobKey);
            }
		}
		if (isShowIsCCardPayment) {
			setIsCCardPayment(transaction.isCCardPayment);
		}
	}

	private void setIsCCardPayment(int isCCardPaymentValue) {
		transaction.isCCardPayment = isCCardPaymentValue;
		ccardPayment.setChecked(isCCardPaymentValue==1);
	}

	private void setLocation(String provider, float accuracy, double latitude, double longitude) {
		lastFix = new Location(provider);
		lastFix.setLatitude(latitude);
		lastFix.setLongitude(longitude);
		lastFix.setAccuracy(accuracy);
		setLocation(lastFix);
	}

	private void setLocation(Location lastFix) {
		if (isShowLocation) {
			if (lastFix.getProvider() == null) {
				locationText.setText(R.string.no_fix);
			} else {
				locationText.setText(Utils.locationToText(lastFix.getProvider(), 
					lastFix.getLatitude(), lastFix.getLongitude(), 
					lastFix.hasAccuracy() ? lastFix.getAccuracy() : 0, null));
			}
		}
	}

	protected void updateTransactionFromUI(Transaction transaction) {
		transaction.categoryId = categorySelector.getSelectedCategoryId();
		transaction.projectId = projectSelector.getSelectedProjectId();
		if (transaction.isScheduled()) {
			DateUtils.zeroSeconds(dateTime);
		}
		transaction.dateTime = dateTime.getTime().getTime();
		if (selectedLocationId > 0) {
			transaction.locationId = selectedLocationId;
		} else {
			transaction.locationId = 0;
			transaction.provider = lastFix != null ? lastFix.getProvider() : null;
			transaction.accuracy = lastFix != null ? lastFix.getAccuracy() : 0;
			transaction.latitude = lastFix != null ? lastFix.getLatitude() : 0;
			transaction.longitude = lastFix != null ? lastFix.getLongitude() : 0;
		}
        if (isShowPayee) {
            transaction.payeeId = db.insertPayee(text(payeeText));
        }
		if (isShowNote) {
			transaction.note = text(noteText);
		}
		if (transaction.isTemplate()) {
			transaction.templateName = text(templateName);
		}
		if (transaction.isScheduled()) {
			transaction.recurrence = recurrence;
			transaction.notificationOptions = notificationOptions;
		}
	}

    protected void selectPayee(long payeeId) {
        if (isShowPayee) {
            Payee p = db.em().get(Payee.class, payeeId);
            selectPayee(p);
        }
    }

    protected void selectPayee(Payee p) {
        if (p != null) {
            payeeText.setText(p.title);
            transaction.payeeId = p.id;
        }
    }

}
