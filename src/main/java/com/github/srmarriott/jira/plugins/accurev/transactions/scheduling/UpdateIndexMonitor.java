package com.github.srmarriott.jira.plugins.accurev.transactions.scheduling;

public interface UpdateIndexMonitor {
    void schedule();
    void onStart();
}
