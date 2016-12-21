package com.dhenry.baseadapter;

/**
 * Created by hendavid on 12/20/16.
 */

public interface Function<T, I, R> {
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R getViewType(T t, I index);
}
