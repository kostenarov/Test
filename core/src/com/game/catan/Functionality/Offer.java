package com.game.catan.Functionality;

import com.game.catan.Map.Cell.ResourceType;

import java.io.Serializable;
import java.util.HashMap;

public class Offer implements Serializable {
    private boolean isBankOffer = false;
    private Deck givenOffer;
    private Deck wantedOffer;
    private final int playerId;
    private HashMap<Integer, Boolean> players;

    public Offer(Deck offer, int playerId) {
        this.givenOffer = offer;
        this.wantedOffer = new Deck(true);
        this.playerId = playerId;
    }

    public Offer(int playerId) {
        this.givenOffer = new Deck(true);
        this.wantedOffer = new Deck(true);
        this.playerId = playerId;
        players = new HashMap<>();

    }

    public synchronized void addResourceToWantedOffer(ResourceType resource) {
        wantedOffer.addResource(resource);
    }

    public synchronized void removeResourceFromWantedOffer(ResourceType resource) {
        wantedOffer.removeResource(resource);
    }

    public synchronized void addResourceToGivenOffer(ResourceType resource) {
        givenOffer.addResource(resource);
    }

    public synchronized void removeResourceFromGivenOffer(ResourceType resource) {
        givenOffer.removeResource(resource);
    }


    public Deck getGivenOffer() {
        return givenOffer;
    }

    public void addAcceptance(int playerId) {
        players.put(playerId, true);
    }
    public void addRejection(int playerId) {
        players.put(playerId, false);
    }

    public int getPlayerId() {
        return playerId;
    }

    public HashMap<Integer, Boolean> getPlayers() {
        return players;
    }

    public Integer getWantedOfferResourceAmount(ResourceType resourceType) {
        return wantedOffer.getResourceAmount(resourceType);
    }

    public Integer getGivenOfferResourceAmount(ResourceType resourceType) {
        return givenOffer.getResourceAmount(resourceType);
    }

    public boolean isBankOffer() {
        for(ResourceType resourceType : givenOffer.getResources().keySet()) {
            if(givenOffer.getResourceAmount(resourceType) == 4 && isBankOffer) {
                return true;
            }
        }
        return false;
    }
}
