package com.artificialsolutions.common;

import org.slf4j.LoggerFactory;

/**
 * Defines a deferred task execution. The actual task should be implemented in its {@code run()}
 * method.
 */
public abstract class OneTimeTask implements Runnable {

    /**
     * The state before starting. The task can be cancelled during this state.
     */
    public static final int STATE_INITIAL = 0;

    /**
     * The state after starting and before the actual task execution. The task can be cancelled during
     * this state.
     */
    public static final int STATE_WAITING = 1;

    /**
     * The state when the task is being executed. It can not be cancelled during this state.
     */
    public static final int STATE_RUNNING = 2;

    /**
     * The state when the task has already been executed.
     */
    public static final int STATE_EXECUTED = 3;

    /**
     * The state when the task has been cancelled.
     */
    public static final int STATE_CANCELLED = 4;

    /**
     * The actual work thread.
     */
    private final Thread taskThread = new Thread() {

        /**
         * Manages the waiting logic and runs the task.
         */
        @Override
        public void run() {
            synchronized (this) {
                for (;;) {
                	// The goal of this loop is to handle spurious wakeups 
                    if (state == STATE_CANCELLED) return;
                    final long remainingWaitMillis = waitMillis - (System.currentTimeMillis() - startMillis);
                    if (remainingWaitMillis <= 1) break;
                    try {
                        wait(remainingWaitMillis);
                    } catch (final Exception ex) {
                        state = STATE_CANCELLED;
                        return;
                    }
                }
                state = STATE_RUNNING;
            }
            try {
                OneTimeTask.this.run();
            } catch (final Throwable ex) {
                LoggerFactory.getLogger(getClass()).warn("Failure in thread {}", this, ex);
            }
            synchronized (this) {
                state = STATE_EXECUTED;
            }
        }
    };

    /**
     * The time in milliseconds to wait before executing the task.
     */
    private final long waitMillis;

    /**
     * The time point in milliseconds when the waiting started.
     */
    private volatile long startMillis;

    /**
     * The current state of the task.
     */
    private volatile int state = STATE_INITIAL;

    /**
     * Creates an instance of this object.
     * 
     * @param waitMillis the time in milliseconds to wait before executing the task.
     * 
     * @throws IllegalArgumentException if waitMillis is less then 0.
     */
    public OneTimeTask(final long waitMillis) throws IllegalArgumentException {
        if (waitMillis < 0) throw new IllegalArgumentException("waitMillis < 0");
        this.waitMillis = waitMillis;
        if (!taskThread.isDaemon()) {
            try {
                taskThread.setDaemon(true);
            } catch (final SecurityException ex) {
                LoggerFactory.getLogger(OneTimeTask.class).warn("Failure to set thread {} as daemon", taskThread, ex);
            }
        }
    }

    /**
     * Gets the current state of the task.
     * 
     * @return the current state of the task.
     */
    public int getState() {
        synchronized (taskThread) {
            return state;
        }
    }

    /**
     * Checks if the task has been ended, i.e. its state is {@link #STATE_EXECUTED} or
     * {@link #STATE_CANCELLED}.
     * 
     * @return {@code true} if the task is completed, {@code false} otherwise.
     */
    public boolean isEnded() {
        synchronized (taskThread) {
            return state == STATE_EXECUTED || state == STATE_CANCELLED;
        }
    }

    /**
     * Starts the waiting process.
     * 
     * @return the time point in milliseconds when the waiting started.
     * 
     * @throws IllegalStateException if the task has already been started.
     */
    public long start() throws IllegalStateException {
        synchronized (taskThread) {
            if (state != STATE_INITIAL) throw new IllegalStateException("OneTimeTask was already started");
            state = STATE_WAITING;
        }
        startMillis = System.currentTimeMillis();
        taskThread.start();
        return startMillis;
    }

    /**
     * Cancels the task.
     * 
     * @return {@code true} if this task neither has not nor will not be executed and {@code false}
     * otherwise, i.e., if the task either has been executed or is in the process of being executed and
     * it is too late to cancel it. A task cannot be cancelled when it is "too late", i.e. when its
     * current state is {@link #STATE_RUNNING} or {@link #STATE_EXECUTED}.
     */
    public boolean cancel() {
        synchronized (taskThread) {
            if (state == STATE_RUNNING || state == STATE_EXECUTED) return false;
            if (state != STATE_CANCELLED) {
                state = STATE_CANCELLED;
                taskThread.notify();
            }
        }
        return true;
    }
}
