package io.github.overlordsiii.ranking;

public class RankedObject<T> {

    private T object;

    private String name; // used as identifier for when asking user to rank

    private int priority = 0;

    public RankedObject(T object, String name) {
        this.object = object;
        this.name = name;
    }

    public void incrementPriority() {
        priority++;
    }

    public void incrementPriority(int priority) {
        this.priority += priority;
    }

    public void decrementPriority() {
        priority--;
    }

    public void decrementPriority(int priority) {
        this.priority -= priority;
    }

    public T getObject() {
        return object;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return name;
    }
}
