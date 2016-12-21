package com.dhenry.sampleapp;

import android.databinding.BindingAdapter;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.dhenry.baseadapter.BaseAdapter;
import com.dhenry.baseadapter.Function;
import com.dhenry.sampleapp.model.SimpleItem;
import com.dhenry.sampleapp.model.SimpleListItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final List<SimpleItem> sampleData = getSampleData();
        final RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        BaseAdapter.with(sampleData)
                .map(SimpleItem.class, R.layout.item_text, BR.simpleItem)
                .map(SimpleListItem.class, R.layout.item_list, BR.simpleListItem)
//                .onClickListener()
//                .onLongClickListener()
//                .enableSelectionMode()
//                .selectionModeClickListener()
//                .onBindListener()
                // viewType implementation!
                .map(SimpleItem.class, new int[]{R.layout.item_text, R.layout.item_text}, BR.simpleItem,
                        new Function<SimpleItem, Integer, Integer>() {
                            @Override
                            public Integer getViewType(SimpleItem o, Integer index) {
                                return 0;
                            }
                        })
                .into(recyclerView);
    }

    @BindingAdapter({"bind:simpleItemList"})
    public static void setSimpleItemList(RecyclerView container, List<SimpleItem> moreSimpleItems) {
        container.setLayoutManager(new LinearLayoutManager(container.getContext()));

        BaseAdapter.with(moreSimpleItems)
                .map(SimpleItem.class, R.layout.item_text, BR.simpleItem)
                .into(container);
    }

    @NonNull
    private List<SimpleItem> getSampleData() {
        List<SimpleItem> sampleData = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                sampleData.add(new SimpleItem(i));
            } else {
                sampleData.add(new SimpleListItem(i));
            }
        }
        return sampleData;
    }
}
