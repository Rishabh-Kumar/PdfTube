package com.searchresults.fragcom.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.searchresults.fragcom.R;
import com.searchresults.fragcom.nlp.AccessTokenLoader;
import com.searchresults.fragcom.nlp.ApiFragment;
import com.searchresults.fragcom.nlp.EntityInfo;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.IOException;

import static com.searchresults.fragcom.AppConstants.INPUT;


/**
 * Created by risha on 5/1/2017.
 */

public class OneFragment extends Fragment implements ApiFragment.Callback{

    private static final String FRAGMENT_API = "api";

    private static final int LOADER_ACCESS_TOKEN = 1;

    private static final String STATE_SHOWING_RESULTS = "showing_results";

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                // The icon button is clicked; start analyzing the input.
                case R.id.analyze:
                    startAnalyze();
                    Toast.makeText(getActivity(), "This", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private View mIntroduction;

    private View mResults;

    private View mProgress;

    private View view;

    private OneFragment.EntitiesAdapter mAdapter;

    private boolean mHidingResult;

    private EntityInfo entities[];

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if(view == null) {
            view = inflater.inflate(R.layout.fragment_one, container, false);
            view.findViewById(R.id.analyze).setOnClickListener(mOnClickListener);

            mIntroduction = view.findViewById(R.id.introduction);
            mResults = view.findViewById(R.id.results);
            mProgress = view.findViewById(R.id.progress);

            RecyclerView list = (RecyclerView)view.findViewById(R.id.list);
            list.setLayoutManager(new LinearLayoutManager(getActivity()));


            mAdapter = new OneFragment.EntitiesAdapter(getActivity(), entities);
            list.setAdapter(mAdapter);


            FragmentManager fm = getChildFragmentManager();

            if (savedInstanceState == null) {
                // The app has just launched; handle share intent if it is necessary
                handleShareIntent();
            } else {
                // Configuration changes; restore UI states
                boolean results = savedInstanceState.getBoolean(STATE_SHOWING_RESULTS);
                if (results) {
                    mIntroduction.setVisibility(View.GONE);
                    mResults.setVisibility(View.VISIBLE);
                    mProgress.setVisibility(View.INVISIBLE);
                } else {
                    mResults.setVisibility(View.INVISIBLE);
                }
            }

            if (getApiFragment() == null) {
                fm.beginTransaction().add(new ApiFragment(), FRAGMENT_API).commit();
            }
            prepareApi();
        }
        return view;
    }

    private ApiFragment getApiFragment() {
        return (ApiFragment) getChildFragmentManager().findFragmentByTag(FRAGMENT_API);
    }

    private void handleShareIntent() {
        final Intent intent = getActivity().getIntent();
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_SEND)
                && TextUtils.equals(intent.getType(), "text/plain")) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                //mInput.setText(text);
            }
        }
    }

    private void prepareApi() {
        // Initiate token refresh
        getActivity().getSupportLoaderManager().initLoader(LOADER_ACCESS_TOKEN, null,
                new LoaderManager.LoaderCallbacks<String>() {
                    @Override
                    public Loader<String> onCreateLoader(int id, Bundle args) {
                        return new AccessTokenLoader(getActivity());
                    }

                    @Override
                    public void onLoadFinished(Loader<String> loader, String token) {
                        getApiFragment().setAccessToken(token);
                    }

                    @Override
                    public void onLoaderReset(Loader<String> loader) {
                    }
                });
    }

    @Override
    public void onEntitiesReady(EntityInfo[] entities) {
        showResults();
        mAdapter.setEntities(entities);
        this.entities = entities;
    }

    private void startAnalyze() {

        showProgress();

        //final String text = mInput.getText().toString();
        getApiFragment().analyzeEntities(stripText());
    }

    public String stripText() {
        String parsedText = null;
        PDDocument document = null;
        try {
            document = PDDocument.load(getActivity().getAssets().open("resume.pdf"));
        } catch(IOException e) {
            e.printStackTrace();
        }

        try {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setStartPage(0);
            pdfStripper.setEndPage(1);
            parsedText = "Parsed text: " + pdfStripper.getText(document);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (document != null) document.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return parsedText;
    }

    private void showResults() {
        mIntroduction.setVisibility(View.GONE);
        if (mProgress.getVisibility() == View.VISIBLE) {
            ViewCompat.animate(mProgress)
                    .alpha(0.f)
                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            view.setVisibility(View.INVISIBLE);
                        }
                    });
        }
        if (mHidingResult) {
            ViewCompat.animate(mResults).cancel();
        }
        if (mResults.getVisibility() == View.INVISIBLE) {
            mResults.setVisibility(View.VISIBLE);
            ViewCompat.setAlpha(mResults, 0.01f);
            ViewCompat.animate(mResults)
                    .alpha(1.f)
                    .setListener(null)
                    .start();
        }
    }
    private void showProgress() {
        mIntroduction.setVisibility(View.GONE);
        if (mResults.getVisibility() == View.VISIBLE) {
            mHidingResult = true;
            ViewCompat.animate(mResults)
                    .alpha(0.f)
                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            mHidingResult = false;
                            view.setVisibility(View.INVISIBLE);
                        }
                    });
        }
        if (mProgress.getVisibility() == View.INVISIBLE) {
            mProgress.setVisibility(View.VISIBLE);
            ViewCompat.setAlpha(mProgress, 0.f);
            ViewCompat.animate(mProgress)
                    .alpha(1.f)
                    .setListener(null)
                    .start();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView name;
        public TextView type;
        public TextView salience;
        public TextView wikipediaUrl;

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_entity, parent, false));
            name = (TextView) itemView.findViewById(R.id.name);
            type = (TextView) itemView.findViewById(R.id.type);
            salience = (TextView) itemView.findViewById(R.id.salience);
            wikipediaUrl = (TextView) itemView.findViewById(R.id.wikipedia_url);
        }

    }

    private static class EntitiesAdapter extends RecyclerView.Adapter<OneFragment.ViewHolder> {

        private final Context mContext;
        private EntityInfo[] mEntities;

        public EntitiesAdapter(Context context, EntityInfo[] entities) {
            mContext = context;
            mEntities = entities;
        }

        @Override
        public OneFragment.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new OneFragment.ViewHolder(LayoutInflater.from(mContext), parent);
        }

        @Override
        public void onBindViewHolder(OneFragment.ViewHolder holder, int position) {
            EntityInfo entity = mEntities[position];
            holder.name.setText(entity.name);
            holder.type.setText(entity.type);
            holder.salience.setText(mContext.getString(R.string.salience_format, entity.salience));
            holder.wikipediaUrl.setText(entity.wikipediaUrl);
            Linkify.addLinks(holder.wikipediaUrl, Linkify.WEB_URLS);
        }

        @Override
        public int getItemCount() {
            return mEntities == null ? 0 : mEntities.length;
        }

        public void setEntities(EntityInfo[] entities) {
            mEntities = entities;
            notifyDataSetChanged();
        }
    }
}
