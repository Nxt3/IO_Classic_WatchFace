package io.nxt3.ioclassic.model;


public enum PrefKey {
    /* Recommended naming convention:
     * ints, floats, doubles, longs:
     * SAMPLE_NUM or SAMPLE_COUNT or SAMPLE_INT, SAMPLE_LONG etc.
     *
     * boolean: IS_SAMPLE, HAS_SAMPLE, CONTAINS_SAMPLE
     *
     * String: SAMPLE_KEY, SAMPLE_STR or just SAMPLE
     */

    /**
     * Colors for each of the different components
     * This is the KEY we'll be using for storing in Settings
     */
    HOUR_HAND_COLOR,
    MINUTE_HAND_COLOR,
    SECOND_HAND_COLOR,
    BACKGROUND_COLOR,
    CIRCLE_AND_TICKS_COLOR,
    OUTER_CIRCLE_COLOR
}
