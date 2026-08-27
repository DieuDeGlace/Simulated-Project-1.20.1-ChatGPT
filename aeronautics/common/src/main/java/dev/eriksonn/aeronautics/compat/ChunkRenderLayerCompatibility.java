package dev.eriksonn.aeronautics.compat;

/**
 * Runtime control for the legacy chunk render-layer compatibility bridge.
 *
 * <p>The startup layer table cannot be safely reverted after render-layer sets
 * have been created. This switch therefore isolates the risky runtime behavior:
 * interpreting every existing set against the latest layer ids. When disabled,
 * each set is iterated using the layer meanings captured when that set was
 * created.</p>
 */
public final class ChunkRenderLayerCompatibility {

    private static volatile boolean liveLayerIdInterpretationEnabled = true;

    private ChunkRenderLayerCompatibility() {
    }

    public static boolean isLiveLayerIdInterpretationEnabled() {
        return liveLayerIdInterpretationEnabled;
    }

    public static boolean setLiveLayerIdInterpretationEnabled(final boolean enabled) {
        final boolean changed = liveLayerIdInterpretationEnabled != enabled;
        liveLayerIdInterpretationEnabled = enabled;
        return changed;
    }
}
