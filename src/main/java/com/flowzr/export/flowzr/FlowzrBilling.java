package com.flowzr.export.flowzr;


import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.flowzr.activity.FlowzrSyncActivity;
import com.flowzr.export.billing.IabHelper;
import com.flowzr.export.billing.IabResult;
import com.flowzr.export.billing.Inventory;
import com.flowzr.export.billing.Purchase;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class FlowzrBilling {
	
	boolean mSubscribed = false;
	static final String SKU_FLOWZR = "flowzr_sub";
    static final int RC_REQUEST = 10001;
    private String TAG ;
    IabHelper mHelper;
    private Context context;
	public String FLOWZR_API_URL;
    private DefaultHttpClient http_client;
    private String user;
    
    public FlowzrBilling(Context context,  DefaultHttpClient pHttp_client,String payload,String user) {
    	this.context=context;
        //String payload1 = payload;
    	this.user=user;
    	this.http_client=pHttp_client;
    	this.FLOWZR_API_URL="https://flowzr-hrd.appspot.com/financisto3/";
    }

  	 // Listener that's called when we finish querying the items and subscriptions we own
      IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
          public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
              Log.d(TAG, "Query inventory finished.");
              if (result.isFailure()) {
                  Log.e(TAG,"Failed to query inventory: " + result);
                  return;
              }
              
              final Purchase subscriptionPurchase = inventory.getPurchase(SKU_FLOWZR);

              mSubscribed = (subscriptionPurchase != null && verifyDeveloperPayload(subscriptionPurchase));
               //if (true) {
               if (!mSubscribed) {

            	if (mHelper.subscriptionsNotSupported()) {
                   	//Toast.makeText(context, , Toast.LENGTH_SHORT).show();                		
                    Log.e("flowzr","Subscriptions not supported on your device yet. Sorry!");
            		return;
                  }
             
                  mHelper.launchPurchaseFlow((Activity) context,
                          SKU_FLOWZR, IabHelper.ITEM_TYPE_SUBS, 
                          RC_REQUEST, mPurchaseFinishedListener, user);                	            	            	
              } else {
            	  // You'he got a signed response.
            	  
            	  Thread thread = new Thread(new Runnable(){
            		    @Override
            		    public void run() {
            		        try {

            		        	informWebOfSubscription(subscriptionPurchase);
            		        } catch (Exception e) {
            		            e.printStackTrace();
            		        }
            		    }
            		});

            		thread.start();             	  
            	  
            	  
            	  // Checked for signed validity on the server side
            	  // Server is informed anyway so can recover on information error
            	  // Server may not send all data, but User can still export it or use Financisto
              }
          }
      };	
      
      // Callback for when a purchase is finished
      IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
          public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
              Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
              if (result.isFailure()) {
            	  Log.d(TAG, "Purchase failure:" + result);
                  return;
              }
              
              if (!verifyDeveloperPayload(purchase)) {
                  return;
              }  		
              
              Log.d(TAG, "Purchase successful.");              
              if (purchase.getSku().equals(SKU_FLOWZR)) {
                  // bought the subscription
                  Log.d(TAG, "Subscription purchased.");
                  mSubscribed = true;
                  informWebOfSubscription(purchase);
              }
          }
      };
      
      /** Verifies the developer payload of a purchase. */
      boolean verifyDeveloperPayload(Purchase p) {
          //String returnedPayload = p.getDeveloperPayload();
          return true;
      }
      
      public boolean launchPlayFlow() {    	
      	  String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmJmA1uoirmWBBj7NH8oduA7ET+2QheBE/3JsAi5/d0fDO96P+T896cPbEx7QTeMK8rIeqyc1tt0AryZMvFS7JUNoZ149jcJWSpRcrWRWvTe98i5LkG4HKq1X5sUdejijxvtZymn4mQ27rzkgzMcSUxt3HZK6TadCzM2YG/uUbCcbZn0zVetxHNDOCXGO2+VNM+7ARS+4R5ZoHuMGahIbELT77TZVZ6QEb2k4yNi3WJ2ue6KHUCgY7tf71h7dvOgwTLfSjwOJOHFGWci2GeA2Hg7H3C6E/x5VJ81mO6OIED7aIrtu41kKE3b60a27fDDGzIqP7ByakP4FgYuTW7gi1QIDAQAB";    	
          mHelper = new IabHelper(context, base64EncodedPublicKey);
           
           // enable debug logging (for a production application, you should set this to false).
           mHelper.enableDebugLogging(true);
           
           // Start setup. This is asynchronous and the specified listener
           // will be called once setup completes.
           mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
               public void onIabSetupFinished(IabResult result) {
 
                   if (result.isFailure2()) {
                      // Oh noes, there was a problem.
                   	  //Toast.makeText(context, "Problem setting up in-app billing: " + result, Toast.LENGTH_SHORT).show();                    
                      return;
                   }
                   // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
                   Log.d(TAG, "Setup successful. Querying inventory.");
                   try {
                	   mHelper.queryInventoryAsync(mGotInventoryListener);
                   } catch(Exception e) {
                	   e.printStackTrace();
                   }
               }
           });
           return true;
       }

      private boolean checkSubscriptionFromWeb() {
  		String url=FLOWZR_API_URL + "?action=checkSubscription&regid=" + ((FlowzrSyncActivity)context).regid;
  		try {
            HttpGet httpGet = new HttpGet(url); 
            HttpResponse httpResponse = http_client.execute(httpGet);      
            int code = httpResponse.getStatusLine().getStatusCode();
            if (code==402) {
          		Log.i("flowzr","server rejected / subscription invalid");
            	return false;
            }
        } catch (Exception e) {
        	Log.e("flowzr","unable to check subscription from web");
            e.printStackTrace();
        } 
  		Log.i("flowzr","subscription from web is ok!");
      	return true;
      }

      public boolean informWebOfSubscription(Purchase p) {
    	if (p==null) {
    		return false;
    	}
  		ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
  		nameValuePairs.add(new BasicNameValuePair("action","gotSubscription"));
  		nameValuePairs.add(new BasicNameValuePair("payload",p.getDeveloperPayload()));
  		nameValuePairs.add(new BasicNameValuePair("purchase",p.getOriginalJson()));
  		nameValuePairs.add(new BasicNameValuePair("signature",p.getSignature()));
  		
        HttpPost httppost = new HttpPost(FLOWZR_API_URL);
        try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;				
		}
        HttpResponse response;
        //String strResponse;
		try {
			response = http_client.execute(httppost);
	        HttpEntity entity = response.getEntity();
            int code = response.getStatusLine().getStatusCode();
	        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			reader.readLine();
	        entity.consumeContent();			
	        if (code!=200) {
	        	return false;
	        }

		} catch (ClientProtocolException e) {
			e.printStackTrace();				
			return false;				
		} catch (IOException e) {				
			e.printStackTrace();
			return false;							
		}
  	  	
		return true;
      }      
      
      public boolean checkSubscription()  {
    	if (!mSubscribed) {
    		if (!checkSubscriptionFromWeb()) {
    			launchPlayFlow();	 
    			mSubscribed=false;
      			return false;      		
      		} else {
      			mSubscribed=true;
      			return true;
      		}
    	} else {
    		mSubscribed=true;
    		return true;
    	}
      }      
}
