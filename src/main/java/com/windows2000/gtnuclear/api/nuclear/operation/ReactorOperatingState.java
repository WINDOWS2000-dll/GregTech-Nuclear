package com.windows2000.gtnuclear.api.nuclear.operation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Current {@link ReactorOperatingStage} plus, only while {@link ReactorOperatingStage#TRIPPED}, the
 * {@link ScramReason} that caused it (kept for the console to display; cleared once a manual reset returns the
 * reactor to {@link ReactorOperatingStage#SHUTDOWN}).
 */
public final class ReactorOperatingState {

    @NotNull
    public final ReactorOperatingStage stage;
    @Nullable
    public final ScramReason lastScramReason;

    public ReactorOperatingState(@NotNull ReactorOperatingStage stage, @Nullable ScramReason lastScramReason) {
        this.stage = stage;
        this.lastScramReason = lastScramReason;
    }

    /** A freshly-built or never-started reactor begins fully shut down, with no prior SCRAM to report. */
    @NotNull
    public static ReactorOperatingState initial() {
        return new ReactorOperatingState(ReactorOperatingStage.SHUTDOWN, null);
    }
}
