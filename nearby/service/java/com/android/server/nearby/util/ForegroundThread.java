/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.nearby.util;

import android.annotation.NonNull;
import android.os.Handler;
import android.os.HandlerThread;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Shared singleton foreground thread.
 */
public class ForegroundThread extends HandlerThread {
    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    private static ForegroundThread sInstance;
    @GuardedBy("sLock")
    private static Handler sHandler;
    @GuardedBy("sLock")
    private static Executor sExecutor;

    private ForegroundThread() {
        super(ForegroundThread.class.getName());
    }

    @GuardedBy("sLock")
    private static void ensureInstanceLocked() {
        if (sInstance == null) {
            sInstance = new ForegroundThread();
            sInstance.start();
            sHandler = new Handler(sInstance.getLooper());
            sExecutor = new HandlerExecutor(sHandler);
        }
    }

    /**
     * Get the singleton instance of thi class.
     *
     * @return the singleton instance of thi class
     */
    @NonNull
    public static ForegroundThread get() {
        synchronized (sLock) {
            ensureInstanceLocked();
            return sInstance;
        }
    }

    /**
     * Get the {@link Handler} for this thread.
     *
     * @return the {@link Handler} for this thread.
     */
    @NonNull
    public static Handler getHandler() {
        synchronized (sLock) {
            ensureInstanceLocked();
            return sHandler;
        }
    }

    /**
     * Get the {@link Executor} for this thread.
     *
     * @return the {@link Executor} for this thread.
     */
    @NonNull
    public static Executor getExecutor() {
        synchronized (sLock) {
            ensureInstanceLocked();
            return sExecutor;
        }
    }

    /**
     * An adapter {@link Executor} that posts all executed tasks onto the given
     * {@link Handler}.
     */
    private static class HandlerExecutor implements Executor {
        private final Handler mHandler;

        HandlerExecutor(@NonNull Handler handler) {
            mHandler = Preconditions.checkNotNull(handler);
        }

        @Override
        public void execute(Runnable command) {
            if (!mHandler.post(command)) {
                throw new RejectedExecutionException(mHandler + " is shutting down");
            }
        }
    }
}
