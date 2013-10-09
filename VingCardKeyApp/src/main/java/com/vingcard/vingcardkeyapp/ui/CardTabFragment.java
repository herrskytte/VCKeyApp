package com.vingcard.vingcardkeyapp.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.vingcard.vingcardkeyapp.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;

public class CardTabFragment extends Fragment {
    static final String TAG = "CardTabFragment";

    public static final String TYPE = "com.vingcard.vingcardkeyapp.type";
    public static final int TYPE_ADDRESS = 1;
    public static final int TYPE_WEB = 2;
    public static final int TYPE_MAIL = 3;
    public static final int TYPE_PHONE = 4;
    public static final String DATA = "com.vingcard.vingcardkeyapp.data";
    public static final String DATA2 = "com.vingcard.vingcardkeyapp.data2";

    private int mType = 0;
    private String mData = "";
    private String mData2 = "";

    private TextView mHeaderText;
    private TextView mDetailsText;
    private Button mActionButton;
    private Button mAction2Button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mType = getArguments().getInt(TYPE);
        mData = getArguments().getString(DATA);
        mData2 = getArguments().getString(DATA2);

        Log.e(TAG,"Tabtype: " + mType);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_card_tab, null);
        mHeaderText = (TextView) root.findViewById(R.id.card_action_header);
        mDetailsText = (TextView) root.findViewById(R.id.card_action_detail);
        mActionButton = (Button) root.findViewById(R.id.card_action_button);
        mAction2Button = (Button) root.findViewById(R.id.card_action_button2);

        switch (mType){
            case TYPE_ADDRESS:
                mHeaderText.setText(R.string.card_address_title);
                mActionButton.setText(R.string.card_address_button);
                break;
            case TYPE_WEB:
                mHeaderText.setText(R.string.card_web_title);
                mActionButton.setText(R.string.card_web_button);
                mAction2Button.setVisibility(TextUtils.isEmpty(mData2) ? View.GONE : View.VISIBLE);
                mAction2Button.setText(R.string.card_web_button2);
                break;
            case TYPE_MAIL:
                mHeaderText.setText(R.string.card_mail_title);
                mActionButton.setText(R.string.card_mail_button);
                break;
            case TYPE_PHONE:
                mHeaderText.setText(R.string.card_phone_title);
                mActionButton.setText(R.string.card_phone_button);
                break;
        }

        mDetailsText.setText(mData);

        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mType){
                    case TYPE_ADDRESS:
                        // Map point based on address
                        String q;
                        try {
                            q = URLEncoder.encode(mData, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            q = mData;
                        }

                        Uri location = Uri.parse("geo:0,0?q="+ q);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, location);
                        startIntentSafely(mapIntent);
                        break;
                    case TYPE_WEB:
                        String web = mData.startsWith("http") ? mData : "http://"+mData;
                        Uri webpage = Uri.parse(web);
                        Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);
                        startIntentSafely(webIntent);
                        break;
                    case TYPE_MAIL:
                        Intent mailIntent = new Intent(Intent.ACTION_SEND);
                        mailIntent.setType("message/rfc822");
                        mailIntent.putExtra(Intent.EXTRA_EMAIL  , new String[]{mData});
                        startIntentSafely(mailIntent);
                        break;
                    case TYPE_PHONE:
                        Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mData));
                        startIntentSafely(callIntent);
                        break;
                }
            }
        });
        mAction2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mType){
                    case TYPE_WEB:
                        String web = mData2.startsWith("http") ? mData2 : "http://"+mData2;
                        Uri webpage = Uri.parse(web);
                        Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);
                        startIntentSafely(webIntent);
                        break;
                }
            }
        });

        return root;
    }

    public void startIntentSafely(Intent i){
        if(getActivity().getPackageManager().resolveActivity(i, 0) != null){
            startActivity(i);
        } else{
            Toast.makeText(getActivity(), getString(R.string.error_intent), Toast.LENGTH_SHORT).show();
        }
    }
}