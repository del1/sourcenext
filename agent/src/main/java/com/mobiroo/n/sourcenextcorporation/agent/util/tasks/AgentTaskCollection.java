package com.mobiroo.n.sourcenextcorporation.agent.util.tasks;

import android.os.AsyncTask;

import java.util.ArrayList;

/**
 * Created by omarseyal on 4/5/14.
 */
public class AgentTaskCollection {
    ArrayList<AsyncTask> mTasks;

    public AgentTaskCollection() {
        mTasks = new ArrayList<AsyncTask>();
    }

    public void addTask(AsyncTask task) {
        mTasks.add(task);
    }

    public void completeTask(AsyncTask task) {
        mTasks.remove(task);
    }

    public void cancelTasks() {
        for(AsyncTask task : mTasks) {
            task.cancel(false);
        }
    }
}
