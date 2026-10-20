package io.github.dx.xmprint;

/** Pure width policy for the responsive home layout. */
final class HomeLayoutPolicy {
    static final int SIDE_BY_SIDE_MIN_WIDTH_DP = 600;

    enum Mode {
        STACKED,
        SIDE_BY_SIDE
    }

    private HomeLayoutPolicy() {
    }

    static Mode forWidthDp(int widthDp) {
        return widthDp >= SIDE_BY_SIDE_MIN_WIDTH_DP ? Mode.SIDE_BY_SIDE : Mode.STACKED;
    }
}
