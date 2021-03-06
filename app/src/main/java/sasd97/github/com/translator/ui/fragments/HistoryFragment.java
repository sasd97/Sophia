package sasd97.github.com.translator.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import sasd97.github.com.translator.R;
import sasd97.github.com.translator.events.OnTranslationChangedListener;
import sasd97.github.com.translator.models.TranslationModel;
import sasd97.github.com.translator.services.HistorySqlService;
import sasd97.github.com.translator.ui.adapters.HistoryAdapter;
import sasd97.github.com.translator.ui.base.BaseHistoryFragment;
import sasd97.github.com.translator.utils.watchers.SearchDetector;

/**
 * Created by alexander on 10/04/2017.
 */

public class HistoryFragment extends BaseHistoryFragment
        implements HistoryAdapter.OnItemSelectedListener {

    private static final String TAG = HistoryFragment.class.getCanonicalName();

    private SearchDetector searchDetector;
    private HistoryAdapter historyAdapter;
    private ItemTouchHelper itemTouchHelper;
    private OnTranslationChangedListener translationChangedListener;

    @BindView(R.id.fab)
    FloatingActionButton clearAllFab;
    @BindView(R.id.history_recyclerview)
    RecyclerView historyRecyclerView;

    private ItemTouchHelper.SimpleCallback swipeToDismissListener = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            final int positionToDelete = viewHolder.getAdapterPosition();
            final TranslationModel translationToDelete = historyAdapter.get(positionToDelete);
            historyAdapter.removeHistory(positionToDelete);
            translations.remove(translationToDelete);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    HistorySqlService.delete(translationToDelete);
                }
            });
            t.start();
        }
    };

    @Override
    protected int getLayout() {
        return R.layout.fragment_history;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            setTranslationChangedListener((OnTranslationChangedListener) getActivity());
        } catch (ClassCastException classCastException) {
            classCastException.printStackTrace();
        }
    }

    @Override
    protected void onViewCreated(Bundle state) {
        super.onViewCreated(state);

        itemTouchHelper = new ItemTouchHelper(swipeToDismissListener);

        historyAdapter = new HistoryAdapter();
        historyAdapter.setItemSelectedListener(this);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        historyRecyclerView.setHasFixedSize(true);
        historyRecyclerView.setAdapter(historyAdapter);
        itemTouchHelper.attachToRecyclerView(historyRecyclerView);
        historyRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && clearAllFab.isShown()) {
                    clearAllFab.hide();
                } else if (dy < 0 && !clearAllFab.isShown()) {
                    clearAllFab.show();
                }
            }
        });

        searchDetector = new SearchDetector(this);
        searchInputEditText.addTextChangedListener(searchDetector);

        Thread t = new Thread(obtainHistory);
        t.start();
    }

    public void setTranslationChangedListener(OnTranslationChangedListener translationChangedListener) {
        this.translationChangedListener = translationChangedListener;
    }

    @OnClick(R.id.fab)
    public void onClearAllClick(View v) {
        translations.clear();
        historyAdapter.removeAllHistories();
        clearAllFab.hide();
        translationChangedListener.onTranslationChanged(null, null);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                HistorySqlService.deleteAll();
            }
        });
        t.start();
    }

    @Override
    public void onObtain(List<TranslationModel> translations) {
        historyAdapter.addHistories(translations);
        if (!translations.isEmpty()) clearAllFab.show();
    }

    @Override
    public void onFilter(List<TranslationModel> translations) {
        historyAdapter.filterHistories(translations, searchQuery);
    }

    @Override
    public void onSelect(TranslationModel translation, int position) {
        translationChangedListener.onTranslationChanged(translation, null);
        translationChangedListener.onFragmentNeedToBeSwitched(0);
    }
}
