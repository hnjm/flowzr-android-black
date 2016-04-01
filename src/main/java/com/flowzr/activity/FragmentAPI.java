package com.flowzr.activity;

import android.content.Intent;
import android.os.Bundle;

public interface FragmentAPI {

    String REQUEST_REPORTS="REQUEST_REPORTS";
    String CONVENTIONAL_REPORTS="CONVENTIONAL_REPORTS";
    String EXTRA_REPORT_TYPE = "EXTRA_REPORT_TYPE";
    String REQUEST_BLOTTER = "REQUEST_BLOTTER";

    void onFragmentMessage(String TAG, Bundle data);

    void onFragmentMessage(int requestCode, int resultCode, Intent data);

}
