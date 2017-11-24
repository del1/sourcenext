package com.mobiroo.n.sourcenextcorporation.agent.service.driving.lists;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by krohnjw on 3/6/14.
 */
public class QueueList<T> {
    protected int mNodeCount;
    public Queue<Node<T>> nodes;
    public Node<T> tail;
    public long lastUpdated;

    protected void logd(String message) {
        Logger.d(this.getClass().getSimpleName() + ":" + message);
    }

    protected class Node<T> {
        public T data;
        public long timestamp;

        public Node(T data) {
            this.data = data;
            timestamp = System.currentTimeMillis();
        }
    }

    public QueueList() {
        mNodeCount = 0;
        nodes = new LinkedList<Node<T>>();
    }

    public void add(T data) {
        mNodeCount++;
        lastUpdated = System.currentTimeMillis();
        Node<T> node = new Node(data);
        nodes.add(node);
        tail = node;
        if (mNodeCount > getMaxNodes()) {
            nodes.poll();
            mNodeCount--;
        }
    }

    public int getNodeCount() {
        return mNodeCount;
    }

    public void clear() {
        nodes.clear();
    }

    public Queue<Node<T>> getQueue() {
        return nodes;
    }

    protected int getMaxNodes() {
        return 10;
    }

    protected int getMinNodes() {
        return 1;
    }

    public Node<T> getTail() {
        if ((nodes == null) || (nodes.size() < 1)) { return null; }
        return tail;
    }

    public Node<T> getPenultimateNode() {
        Node<T> penultimate = null;

        if ((nodes == null) || (nodes.size() < 2)) { return null; }

        Iterator iterator = nodes.iterator();
        while (iterator.hasNext()) {
            Node<T> node = (Node<T>) iterator.next();
            if (iterator.hasNext()) {
                penultimate = node;
            }
        }
        return penultimate;
    }

}
