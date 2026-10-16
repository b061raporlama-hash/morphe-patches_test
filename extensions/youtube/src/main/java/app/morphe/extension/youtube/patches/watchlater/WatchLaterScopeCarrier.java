/* Copyright 2026 Cep Patcher contributors. See NOTICE for GPLv3 Section 7 terms. */
package app.morphe.extension.youtube.patches.watchlater;

/** Implemented by patched ConversionContext and its builder; never indexed by template hash. */
public interface WatchLaterScopeCarrier {
    Object patch_getWatchLaterScope();
    void patch_setWatchLaterScope(Object scope);
}
