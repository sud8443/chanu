package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.RefreshableActivity;
import com.chanapps.four.component.Dismissable;
import com.chanapps.four.task.LoadCaptchaTask;
import com.chanapps.four.task.ReportPostTask;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class ReportPostDialogFragment extends DialogFragment {

    public static final String TAG = ReportPostDialogFragment.class.getSimpleName();

    private Dismissable dismissable;
    private RefreshableActivity refreshableActivity;
    private String boardCode;
    private long threadNo = 0;
    private long postNo = 0;
    private Spinner reportTypeSpinner;
    private EditText reportRecaptchaResponse;
    private ImageButton recaptchaButton;
    private ImageView recaptchaLoading;
    private LoadCaptchaTask loadCaptchaTask;


    public ReportPostDialogFragment(Dismissable dismissable, RefreshableActivity refreshableActivity,
                                    String boardCode, long threadNo, long postNo) {
        super();
        this.dismissable = dismissable;
        this.refreshableActivity = refreshableActivity;
        this.boardCode = boardCode;
        this.threadNo = threadNo;
        this.postNo = postNo;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.report_post_dialog_fragment, null);
        builder
            .setView(view)
            .setTitle(R.string.report_post_title)
            .setPositiveButton(R.string.report_post, null)
            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ReportPostDialogFragment.this.dismiss();
                }
            });
        reportTypeSpinner = (Spinner)view.findViewById(R.id.report_post_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.report_post_types,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(adapter);
        reportRecaptchaResponse = (EditText)view.findViewById(R.id.report_post_recaptcha_response);
        recaptchaButton = (ImageButton)view.findViewById(R.id.report_post_recaptcha_imgview);
        recaptchaButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                reloadCaptcha();
            }
        });
        recaptchaLoading = (ImageView)view.findViewById(R.id.report_post_recaptcha_loading);
        reloadCaptcha();
        AlertDialog dialog = builder.create();
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.report_post), (DialogInterface.OnClickListener)null);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dismissable.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        dismissable.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        AlertDialog dialog = (AlertDialog)getDialog();
        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String reportType = reportTypeSpinner.getSelectedItem().toString();
                if ("".equals(reportType)) {
                    Toast.makeText(getActivity(), R.string.report_post_select_type, Toast.LENGTH_SHORT).show();
                    return;
                }
                int reportTypeIndex = reportTypeSpinner.getSelectedItemPosition();
                String recaptchaResponse = reportRecaptchaResponse.getText().toString();
                if ("".equals(recaptchaResponse)) {
                    Toast.makeText(getActivity(), R.string.post_reply_enter_captcha, Toast.LENGTH_SHORT).show();
                    return;
                }
                String recaptchaChallenge = loadCaptchaTask.getRecaptchaChallenge();
                if (recaptchaChallenge == null || recaptchaChallenge.trim().isEmpty()) {
                    Toast.makeText(getActivity(), R.string.post_reply_captcha_error, Toast.LENGTH_SHORT).show();
                    return;
                }

                closeKeyboard();
                ReportPostTask reportPostTask = new ReportPostTask(
                        refreshableActivity, boardCode, threadNo, postNo,
                        reportType, reportTypeIndex, recaptchaChallenge, recaptchaResponse);
                ReportingPostDialogFragment dialogFragment = new ReportingPostDialogFragment(reportPostTask);
                dialogFragment.show(getActivity().getSupportFragmentManager(), ReportingPostDialogFragment.TAG);
                if (!reportPostTask.isCancelled())
                    reportPostTask.execute(dialogFragment);
                dismiss();
            }
        });
    }

    private void closeKeyboard() {
        IBinder windowToken = getActivity().getCurrentFocus() != null ?
                getActivity().getCurrentFocus().getWindowToken()
                : null;
        if (windowToken != null) { // close the keyboard
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }

    public void reloadCaptcha() {
        reportRecaptchaResponse.setText("");
        reportRecaptchaResponse.setHint(R.string.post_reply_recaptcha_hint);
        loadCaptchaTask = new LoadCaptchaTask(getActivity(), recaptchaButton, recaptchaLoading);
        loadCaptchaTask.execute(getString(R.string.post_reply_recaptcha_url_root));
    }

}