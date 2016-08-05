package com.dhenry.baseadapter;

import android.databinding.DataBindingUtil;
import android.databinding.ObservableList;
import android.databinding.OnRebindCallback;
import android.databinding.ViewDataBinding;
import android.os.Looper;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hendavid on 7/13/16.
 * <p>
 * One RecyclerView Adapter to rule them all.
 * <p>
 * Usage:
 * <pre>
 * {@code
 * BaseAdapter.with(new List<Object>, BR.item)
 * .map(SomeItemView.class, R.layout.some_item_view_layout)
 * .onBindListener(this)
 * .onClickListener(R.id.some_item_view_clickable, this)
 * .onLongClickListener(R.id.some_item_view_long_clickable, this)
 * .into(myRecyclerView)
 * .build();
 * }
 * </pre>
 * <p>
 * Modified from: https://github.com/nitrico/LastAdapter
 */

public class BaseAdapter<T> extends RecyclerView.Adapter<BaseAdapter.ViewHolder> {

    // for persisting selected items during configuration changes
    public static final String STATE_SELECTED_ITEMS = "state-selected-items";

    public interface OnItemDeletedListener<T> {
        void onItemsDeleted(List<T> items);
    }

    public interface OnBindListener<T> {
        void onBind(T item, View view, int position, boolean isSelectionModeEnabled, boolean itemSelected);
    }

    public interface OnClickListener<T> {
        void onClick(T item, View view, int position);
    }

    public interface OnLongClickListener<T> {
        void onLongClick(T item, View view, int position);
    }

    public interface SelectionModeOnClickListener<T> {
        void refreshViewState();
    }

    public static <T> Builder<T> with(List<T> list, int variable) {
        return new Builder<>(list, variable);
    }

    public static <T> Builder<T> with(List<T> list) {
        return new Builder<>(list);
    }

    private static final Object DATA_INVALIDATION = new Object();

    /**
     * Indicates that the default click listener/long click listener was attached.
     */
    private static final int DEFAULT_LISTENER_INDEX = 0;

    private WeakReferenceOnListChangedCallback onListChangedCallback = new WeakReferenceOnListChangedCallback(this);
    private RecyclerView recyclerView = null;
    private LayoutInflater inflater = null;
    private final List<T> list;
    private final List<T> selectedItems;
    private final Map<Class, Pair<Integer, Integer>> map;
    private final OnBindListener<T> onBindListener;
    private final SparseArray<OnClickListener<T>> clickListeners;
    private final SparseArray<OnLongClickListener<T>> longClickListeners;
    private final SelectionModeOnClickListener<T> selectionModeClickListener;
    private boolean isSelectionModeActivated = false;
    private boolean isEnteringSelectionMode = false;
    private final boolean isSelectionModeEnabled;
    private final WeakReference<Toolbar> toolbarRef;

    private BaseAdapter(List<T> list, Map<Class, Pair<Integer, Integer>> map,
                        OnBindListener<T> onBindListener,
                        SparseArray<OnClickListener<T>> clickListeners,
                        SparseArray<OnLongClickListener<T>> longClickListeners,
                        SelectionModeOnClickListener<T> selectionModeClickListener,
                        boolean isSelectionModeEnabled,
                        WeakReference<Toolbar> toolbarRef, List<T> selectedItems) {

        this.list = list;
        this.selectedItems = new ArrayList<>();
        this.map = map;
        this.onBindListener = onBindListener;
        this.clickListeners = clickListeners;
        this.longClickListeners = longClickListeners;
        this.selectionModeClickListener = selectionModeClickListener;
        this.isSelectionModeEnabled = isSelectionModeEnabled;
        this.toolbarRef = toolbarRef;
        selectItems(selectedItems);
        updateToolbar();
        if (this.selectionModeClickListener != null) {
            this.selectionModeClickListener.refreshViewState();
        }
    }

    /**
     * Constructs an Adapter
     *
     * @param <T>
     */
    public static class Builder<T> {
        private final List<T> list;
        private Integer variable;
        private BaseAdapter adapter;
        private boolean isSelectionModeEnabled = false;
        private WeakReference<Toolbar> toolbarRef;
        private List<T> selectedItems;

        Builder(List<T> list, int variable) {
            this.list = list;
            this.variable = variable;
        }

        Builder(List<T> list) {
            this.list = list;
        }

        private Map<Class, Pair<Integer, Integer>> map = new HashMap<>();
        private OnBindListener<T> onBind = null;
        private SparseArray<OnClickListener<T>> clickListenerMap = new SparseArray<>();
        private SparseArray<OnLongClickListener<T>> longClickListenerMap = new SparseArray<>();
        private SelectionModeOnClickListener<T> selectionModeClickListener;

        public Builder<T> map(Class clazz, @LayoutRes int layout, int variable) {
            map.put(clazz, new Pair<>(layout, variable));
            return this;
        }

        public Builder<T> map(Class clazz, @LayoutRes int layout) {
            if (variable == null) {
                throw new NullPointerException("View Binding variable must be specified during construction to " +
                        "use this method");
            }
            return map(clazz, layout, this.variable);
        }

        public Builder<T> onBindListener(OnBindListener<T> listener) {
            onBind = listener;
            return this;
        }

        /**
         * Click listener that will be bound to the item view.
         *
         * @param listener the listener
         * @return the builder
         */
        public Builder<T> onClickListener(OnClickListener<T> listener) {
            clickListenerMap.put(DEFAULT_LISTENER_INDEX, listener);
            return this;
        }

        /**
         * Click listener that will be bound a view.
         *
         * @param viewId   the id of the view to bind to
         * @param listener the listener
         * @return
         */
        public Builder<T> onClickListener(@IdRes int viewId, OnClickListener<T> listener) {
            clickListenerMap.put(viewId, listener);
            return this;
        }

        /**
         * Long click listener that will be bound to the item view.
         *
         * @param listener the listener
         * @return the builder
         */
        public Builder<T> onLongClickListener(OnLongClickListener<T> listener) {
            longClickListenerMap.put(DEFAULT_LISTENER_INDEX, listener);
            return this;
        }

        /**
         * Long click listener that will be bound a view.
         *
         * @param viewId   the id of the view to bind to
         * @param listener the listener
         * @return the builder
         */
        public Builder<T> onLongClickListener(@IdRes int viewId, OnLongClickListener<T> listener) {
            longClickListenerMap.put(viewId, listener);
            return this;
        }

        /**
         * LongClick events trigger multiselect mode.
         *
         * @param toolbar this toolbar has its view state updated during multi select mode
         * @return the builder
         */
        public Builder<T> enableSelectionMode(@NonNull Toolbar toolbar) {
            toolbarRef = new WeakReference<>(toolbar);
            isSelectionModeEnabled = true;
            return this;
        }

        /**
         * ClickListener that will be fired when selection mode is activated and when a click occurs
         * whilst selection mode is activated.
         *
         * @param selectionModeOnClickListener the listener
         * @return the builder
         */
        public Builder<T> selectionModeClickListener(SelectionModeOnClickListener<T> selectionModeOnClickListener) {
            selectionModeClickListener = selectionModeOnClickListener;
            return this;
        }

        public Builder<T> selectedItems(List<T> selectedItems) {
            this.selectedItems = selectedItems;
            return this;
        }

        public BaseAdapter<T> into(RecyclerView recyclerView) {
            adapter = new BaseAdapter<>(list, map, onBind, clickListenerMap,
                    longClickListenerMap, selectionModeClickListener, isSelectionModeEnabled, toolbarRef,
                    selectedItems);
            recyclerView.setAdapter(adapter);
            return adapter;
        }

    }

    private void updateToolbar() {
        if (toolbarRef == null) return;
        Toolbar toolbar = toolbarRef.get();
        if (toolbar == null) return;

        toolbar.setSubtitle(null);

        if (isSelectionModeActivated()) {
            toolbar.setSubtitle(getTotalSelectedItems() + " Selected");
        }

        if (isEnteringSelectionMode()) {
            toolbar.setNavigationIcon(R.drawable.ic_close_white);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private ViewDataBinding binding;

        public ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ViewHolder(View itemView) {
            super(itemView);
        }

        void bindTo(T item, int position, OnBindListener<T> onBindListener,
                    SparseArray<OnClickListener<T>> clickListeners,
                    SparseArray<OnLongClickListener<T>> longClickListeners) {
            int variable = getVariableForType(position);
            binding.setVariable(variable, item);
            binding.executePendingBindings();
            View view = binding.getRoot();
            if (isSelectionModeEnabled) {
                setMultiSelectModeLongClickListener(view, position);
                setMultiSelectModeClickListener(view, position, item, clickListeners);
            } else {
                setClickListeners(clickListeners, item, position, view);
                setLongClickListeners(longClickListeners, item, position, view);
            }
            if (onBindListener != null) {
                onBindListener.onBind(item, view, position, isSelectionModeEnabled, isItemSelected(position));
            }
        }

        private int getVariableForType(int position) {
            return map.get(list.get(position).getClass()).second;
        }

        /**
         * Attaches a click listener that performs the following:
         * <p>
         * If selection mode is activated, select or deselect the item
         * then fire the SelectionModeOnClickListener if one exists
         * then update the toolbar
         * Else if selection mode is not activated fire any click listeners attached to the root view
         * If the total number of selected items is 0, disable selection mode
         *
         * @param view           the view in question
         * @param position       the adapter position
         * @param item           the data object for the view
         * @param clickListeners the clickListeners associated with the view
         */
        private void setMultiSelectModeClickListener(final View view, final int position, final T item,
                                                     @Nullable final SparseArray<OnClickListener<T>> clickListeners) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view1) {
                    if (isSelectionModeActivated()) {
                        toggleItemSelection(position);
                        if (getTotalSelectedItems() == 0) {
                            disableSelectionMode();
                        }
                        updateToolbar();
                        if (selectionModeClickListener != null) {
                            selectionModeClickListener.refreshViewState();
                        }
                    } else {
                        if (isNullOrEmpty(clickListeners)) return;

                        for (int i = 0; i < clickListeners.size(); i++) {
                            final View viewForListener = ViewHolder.this.getViewForListener(clickListeners.keyAt(i), view);
                            if (viewForListener == view) {
                                clickListeners.valueAt(i).onClick(item, viewForListener, position);
                            }
                        }
                    }
                }
            });
        }

        /**
         * Attaches a long click listener that performs the following:
         * <p>
         * Enable selection mode and select the current item
         * Fire the SelectionModeOnClickListener if one exists
         * Update the toolbar
         *
         * @param view     the view in question
         * @param position the adapter position
         */
        private void setMultiSelectModeLongClickListener(View view, final int position) {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View LongClickedView) {
                    if (position != RecyclerView.NO_POSITION) {
                        enableSelectionMode(true);
                        toggleItemSelection(position);
                    }
                    updateToolbar();
                    if (selectionModeClickListener != null) {
                        selectionModeClickListener.refreshViewState();
                    }
                    return true;
                }
            });
        }

        /**
         * Attach click listeners to views
         *
         * @param clickListeners sparse array of viewId -> clickListener
         * @param item           the data item
         * @param position       the position
         * @param view           the view
         */
        private void setClickListeners(@Nullable final SparseArray<OnClickListener<T>> clickListeners,
                                       final T item, final int position, View view) {
            if (isNullOrEmpty(clickListeners)) return;

            for (int i = 0; i < clickListeners.size(); i++) {
                final int index = i;
                final View viewForListener = getViewForListener(clickListeners.keyAt(index), view);
                if (viewForListener != null) {
                    viewForListener.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view1) {
                            clickListeners.valueAt(index).onClick(item, viewForListener, position);
                        }
                    });
                }
            }
        }

        /**
         * Attach long click listeners to views
         *
         * @param clickListeners sparse array of viewId -> clickListener
         * @param item           the data item
         * @param position       the position
         * @param view           the view
         */
        private void setLongClickListeners(@Nullable final SparseArray<OnLongClickListener<T>> clickListeners,
                                           final T item, final int position, View view) {
            if (isNullOrEmpty(clickListeners)) return;

            for (int i = 0; i < clickListeners.size(); i++) {
                final int index = i;
                final View viewForListener = getViewForListener(clickListeners.keyAt(index), view);
                if (viewForListener != null) {
                    viewForListener.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view1) {
                            clickListeners.valueAt(index).onLongClick(item, viewForListener, position);
                            return true;
                        }
                    });
                }
            }
        }

        /**
         * Looks in view for viewId and returns that view. If viewId is
         * DEFAULT_LISTENER_INDEX then the input view is returned.
         *
         * @param viewId the viewId
         * @param view   the parent view
         * @return the view that the listener should be attached to.
         */
        private View getViewForListener(@IdRes int viewId, View view) {
            if (viewId != DEFAULT_LISTENER_INDEX) {
                return view.findViewById(viewId);
            } else {
                return view;
            }
        }

    }

    private static boolean isNullOrEmpty(@Nullable SparseArray collection) {
        return collection == null || collection.size() == 0;
    }

    private static boolean isNullOrEmpty(@Nullable Collection collection) {
        return collection == null || collection.size() == 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, viewType, parent, false);
        ViewHolder holder = new ViewHolder(binding);
        addOnRebindCallback(binding, recyclerView, holder.getAdapterPosition());
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseAdapter.ViewHolder holder, int position) {
        holder.bindTo(list.get(position), position, onBindListener, clickListeners, longClickListeners);
    }

    public void onBindViewHolder(ViewHolder holder, int position, List<T> payloads) {
        if (isForDataBinding(payloads)) holder.binding.executePendingBindings();
        else onBindViewHolder(holder, position);
        holder.bindTo(list.get(position), position, onBindListener, clickListeners, longClickListeners);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (map != null) {
            Pair<Integer, Integer> viewType = map.get(list.get(position).getClass());
            if (viewType == null) {
                throw new RuntimeException("Invalid viewType at position " + position);
            }
            return viewType.first;
        } else {
            throw new RuntimeException("Invalid object at position " + position);
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (recyclerView == null && list instanceof ObservableList) {
            ((ObservableList<T>) list).addOnListChangedCallback(onListChangedCallback);
        }
        this.recyclerView = recyclerView;
        inflater = LayoutInflater.from(recyclerView.getContext());
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (recyclerView != null && list instanceof ObservableList) {
            ((ObservableList<T>) list).removeOnListChangedCallback(onListChangedCallback);
        }
        this.recyclerView = null;
    }

    private void toggleItemSelection(int position) {
        if (isItemSelected(position)) {
            selectedItems.remove(list.get(position));
        } else {
            // Select the item
            selectedItems.add(list.get(position));
        }
        notifyItemChanged(position);
    }

    public void disableSelectionMode() {
        selectedItems.clear();
        enableSelectionMode(false);
        updateToolbar();
    }

    public boolean isSelectionModeActivated() {
        return isSelectionModeActivated;
    }

    private BaseAdapter<T> enableSelectionMode(boolean selectionModeEnabled) {
        notifyDataSetChanged();
        isSelectionModeActivated = selectionModeEnabled;
        if (isSelectionModeActivated && !isEnteringSelectionMode) {
            isEnteringSelectionMode = true;
        }
        if (!selectionModeEnabled) {
            isEnteringSelectionMode = false;
        }
        return this;
    }

    private boolean isEnteringSelectionMode() {
        return isEnteringSelectionMode;
    }

    public int getTotalSelectedItems() {
        return selectedItems.size();
    }

    public List<T> getSelectedItems() {
        return selectedItems;
    }

    private void addOnRebindCallback(ViewDataBinding viewDataBinding, final RecyclerView recyclerView, final int position) {
        viewDataBinding.addOnRebindCallback(new OnRebindCallback() {
            @Override
            public boolean onPreBind(ViewDataBinding binding) {
                return recyclerView != null && recyclerView.isComputingLayout();
            }

            @Override
            public void onCanceled(ViewDataBinding binding) {
                if (recyclerView == null || recyclerView.isComputingLayout()) return;
                if (position != RecyclerView.NO_POSITION)
                    notifyItemChanged(position, DATA_INVALIDATION);
            }
        });
    }

    private void selectItems(List<T> itemsToSelect) {
        if (isNullOrEmpty(itemsToSelect)) return;
        for (T itemToSelect : itemsToSelect) {
            int index = list.indexOf(itemToSelect);
            if (index != -1) {
                selectedItems.add(list.get(index));
            }
        }
        enableSelectionMode(true);
        notifyDataSetChanged();
    }

    public void deleteSelectedItems(@NonNull OnItemDeletedListener<T> callback) {
        callback.onItemsDeleted(selectedItems);
        list.removeAll(selectedItems);
        selectedItems.clear();
        notifyDataSetChanged();
    }

    private Boolean isForDataBinding(List<T> payloads) {
        if (payloads == null || payloads.size() == 0) return false;
        for (T object : payloads) {
            if (object == DATA_INVALIDATION) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    private boolean isItemSelected(int position) {
        return selectedItems.contains(list.get(position));
    }

    private class WeakReferenceOnListChangedCallback extends ObservableList.OnListChangedCallback<ObservableList<T>> {

        private WeakReference<BaseAdapter<T>> reference;

        WeakReferenceOnListChangedCallback(BaseAdapter<T> adapter) {
            reference = new WeakReference<>(adapter);
        }

        private BaseAdapter<T> getAdapter() {

            if (Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId()) {
                return reference.get();
            } else {
                throw new IllegalStateException("You cannot modify the ObservableList on a background thread");
            }
        }

        @Override
        public void onChanged(ObservableList<T> t) {
            getAdapter().notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(ObservableList<T> t, int from, int count) {
            getAdapter().notifyItemRangeChanged(from, count);
        }

        @Override
        public void onItemRangeInserted(ObservableList<T> t, int from, int count) {
            getAdapter().notifyItemRangeInserted(from, count);
        }

        @Override
        public void onItemRangeMoved(ObservableList<T> list, int from, int to, int count) {
            for (int i = 0; i < count; i++) {
                notifyItemMoved(from + i, to + i);
            }
        }

        @Override
        public void onItemRangeRemoved(ObservableList<T> t, int from, int count) {
            getAdapter().notifyItemRangeRemoved(from, count);
        }
    }

}
