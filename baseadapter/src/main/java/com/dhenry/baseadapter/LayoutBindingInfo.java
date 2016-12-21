package com.dhenry.baseadapter;

import android.support.annotation.Nullable;
import android.util.Pair;

/**
 * Created by hendavid on 12/20/16.
 */
class LayoutBindingInfo<T> {

    // the possible layouts for this data model
    private final int[] layouts;

    // the function to determine which layout to use
    private final Function<T, Integer, Integer> viewTypeFunction;

    // the binding variable associated with the layouts
    private final int bindingVariable;

    LayoutBindingInfo(int[] layouts, int variable, Function<T, Integer, Integer> viewTypeFunction) {
        this.layouts = layouts;
        this.bindingVariable = variable;
        this.viewTypeFunction = viewTypeFunction;
    }


    LayoutBindingInfo(int layout, int variable) {
        this.layouts = new int[]{layout};
        this.bindingVariable = variable;
        this.viewTypeFunction = new Function<T, Integer, Integer>() {
            @Override
            public Integer getViewType(T t, Integer index) {
                return 0;
            }
        };
    }

    @Nullable
    Pair<Integer, Integer> getLayoutBindingVariablePair(@Nullable final T dataModel, @Nullable final Integer itemIndex) {
        if (dataModel != null) {
            Integer viewTypeIndex = viewTypeFunction.getViewType(dataModel, itemIndex);
            if (viewTypeIndex < layouts.length) {
                return new Pair<>(layouts[viewTypeIndex], bindingVariable);
            }
        }
        return null;
    }

    int getBindingVariable() {
        return bindingVariable;
    }
}
