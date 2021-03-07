package cgeo.geocaching.maps.routing;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import androidx.annotation.StringRes;

/**
 * Mapping of locus routing action number to turn instruction
 *
 * see https://github.com/abrensch/brouter/blob/master/brouter-core/src/main/java/btools/router/VoiceHint.java for reference
 */
public enum RoutingTurnType {
    TU(13, R.string.routinginstruction_TU),
    TSHL(5, R.string.routinginstruction_TSHL),
    TL(4, R.string.routinginstruction_TL),
    TSLL(3, R.string.routinginstruction_TSLL),
    KL(9, R.string.routinginstruction_KL),
    C(1, R.string.routinginstruction_C),
    KR(10, R.string.routinginstruction_KR),
    TSLR(6, R.string.routinginstruction_TSLR),
    TR(7, R.string.routinginstruction_TR),
    TSHR(8, R.string.routinginstruction_TSHR),
    TRU(14, R.string.routinginstruction_TU),
    RNDB(26 /* + roundabout exit number */, R.string.routinginstruction_RNDB);


    public final int locusAction;
    public final @StringRes int resId;

    RoutingTurnType(final int locusAction, @StringRes final int resId) {
        this.locusAction = locusAction;
        this.resId = resId;
    }

    private static RoutingTurnType fromLocusAction(final int locusAction) {
        for (final RoutingTurnType type : values()) {
            if (type.locusAction == locusAction) {
                return type;
            }
        }
        return RNDB;
    }

    public static String getTurnInstructionString(final int locusAction) {
        final RoutingTurnType turnType = fromLocusAction(locusAction);

        if (turnType.equals(RNDB)) {
            return CgeoApplication.getInstance().getString(turnType.resId, locusAction - turnType.locusAction);
        }

        return CgeoApplication.getInstance().getString(turnType.resId);
    }
}
