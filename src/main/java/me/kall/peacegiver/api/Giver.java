package me.kall.peacegiver.api;

public interface Giver {
    boolean peace$isGiver();
    void peace$setAsGiver(boolean giver);

    int peace$radius();
    void peace$setRadius(int radius);
}
