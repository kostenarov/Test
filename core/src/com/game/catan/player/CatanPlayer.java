package com.game.catan.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.game.catan.Functionality.*;
import com.game.catan.Map.Map;
import com.game.catan.Map.Cell.*;
import com.game.catan.Map.Cell.Cell;

import java.net.Socket;
import java.util.HashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;


public class CatanPlayer extends ApplicationAdapter {
    private int id;
    private int playersAmount;
    private int diceThrow;
    private boolean isTurn = true;
    private boolean isDiceThrown = false;
    private int points = 0;


    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    private Stage stage;
    private Stage resourceStage;
    private Stage backgroundStage;
    private SpriteBatch batch;
    private SpriteBatch backgroundBatch;
    private SpriteBatch playerIndicatorBatch;
    private Texture playerTexture;
    private Image background;
    private Image resourceBackground;

    private Label pointsLabel;
    private Label diceThrowLabel;
    private HashMap<ResourceType, ResourceDisplay> resourceLabels;

    private Map map;
    private Offer offer;
    private final Deck deck;
    private UpdateListenerThread updateThread;
    private SenderThread senderThread;
    private VillagePair<VillageCell, VillageCell> villagePair;

    public CatanPlayer(Map map) {
        this.map = map;
        this.deck = new Deck();
        this.resourceLabels = new HashMap<>();
    }

    @Override
    public void create() {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch = new SpriteBatch();
        backgroundBatch = new SpriteBatch();
        playerIndicatorBatch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        resourceStage = new Stage(new ScreenViewport());
        backgroundStage = new Stage(new ScreenViewport());
        background = new Image(new Texture("Backgrounds/background.png"));
        resourceBackground = new Image(new Texture("Backgrounds/resourceBackground.png"));
        connectToServer();
        updateThread = new UpdateListenerThread(this, inputStream);
        updateThread.start();
        setUpInitialLabels();
        setUpResourceLabels();
        displayResources();

    }

    @Override
    public void render() {
        backgroundStage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        backgroundStage.draw();
        drawBackgrounds();
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
        resourceStage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        resourceStage.draw();
        renderNormalRound();
        drawButtons();
        drawPlayerIndicator();
        Gdx.input.setInputProcessor(resourceStage);

        Gdx.input.setInputProcessor(stage);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            Map receivedMap = (Map) inputStream.readObject();
            System.out.println(receivedMap.getCenterCell().getDiceThrow());
            setMap(receivedMap);
            this.id = (int) inputStream.readObject();
            System.out.println("My id: " + id);
            if(id != 0) {
                isTurn = false;
            }
            switch (id) {
                case 0:
                    playerTexture = new Texture("Indicators/yellowIndicator.png");
                    break;
                case 1:
                    playerTexture = new Texture("Indicators/blueIndicator.png");
                    break;
                case 2:
                    playerTexture = new Texture("Indicators/redIndicator.png");
                    break;
                case 3:
                    playerTexture = new Texture("Indicators/greenIndicator.png");
                    break;
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void drawIsYourTurnLight() {
        if(isTurn) {
            playerIndicatorBatch.draw(new Texture("Indicators/greenIndicator.png"), 0, 0, 50, 50);
        }
        else {
            playerIndicatorBatch.draw(new Texture("Indicators/redIndicator.png"), 0, 0, 50, 50);
        }
    }

    private void setUpInitialLabels() {
        Label.LabelStyle pointsLabelStyle = new Label.LabelStyle();
        pointsLabelStyle.font = new BitmapFont();
        pointsLabelStyle.font.getData().setScale(2f);
        pointsLabelStyle.fontColor = Color.SALMON;
        pointsLabel = new Label("Points: " + points, pointsLabelStyle);
        pointsLabel.setPosition(1000, 50);
        diceThrowLabel = new Label("Dice throw: " + diceThrow, pointsLabelStyle);
        diceThrowLabel.setPosition(1000, 100);
        stage.addActor(pointsLabel);
        stage.addActor(diceThrowLabel);
    }

    private void setUpOffer() {

    }

    private void drawBackgrounds() {
        backgroundBatch.begin();
        drawBackground();
        drawResourceBackground();
        backgroundBatch.end();
    }

    private void drawPlayerIndicator() {
        playerIndicatorBatch.begin();
        drawIsYourTurnLight();
        displayPlayers();
        playerIndicatorBatch.end();
    }

    private void drawButtons() {
        diceThrowButton();
        endTurnButton();
    }

    private void drawBackground() {
        backgroundStage.addActor(background);
    }

    private void drawResourceBackground() {
        backgroundStage.addActor(resourceBackground);
    }

    private void endTurnButton() {
        TextButton button = ButtonSetUps.setUpTextButton("End Turn", 0, this);
        ButtonSetUps.setUpEndTurnButtonFunc(stage, button, isTurn, isDiceThrown, outputStream);
    }

    private void diceThrowButton() {
        TextButton button = ButtonSetUps.setUpTextButton("Dice Throw", 100, this);
        ButtonSetUps.setUpDiceThrowButtonFunc(stage, button, isTurn, isDiceThrown, outputStream);
    }

    private void changeLabelAmount(ResourceType type, int amount) {
        resourceLabels.get(type).changeAmount(amount);
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void displayPoints() {
        pointsLabel.setText("Points: " + points);
    }

    public void displayDiceThrow() {
        diceThrowLabel.setText("Dice throw: " + diceThrow);
    }

    private void displayResources() {
        for(ResourceType type : ResourceType.values()) {
            if(type != ResourceType.EMPTY) {
                resourceLabels.get(type).draw(resourceStage, batch);
            }
        }
    }

    private void renderThiefRound() {
        for(Cell cell : map.getMap(map.getCenterCell(), new HashSet<Cell>())) {
            if(cell instanceof ResourceCell && ((ResourceCell) cell).hasRobber()) {
                cell.buttonFunc(stage, outputStream, this);
                batch.begin();
                batch.draw(new Texture("Textures/robber.png"), cell.getCellCords().getX(), cell.getCellCords().getY());
                batch.end();
            }
            else {
                cell.buttonFunc(stage, outputStream, this);
            }
        }
    }

    private void renderNormalRound() {
        HashSet<Cell> cells = new HashSet<>();
        cells = map.getMap(map.getCenterCell(), cells);
        for(Cell cell : cells) {
            if(cell instanceof ResourceCell && ((ResourceCell) cell).hasRobber()) {
                cell.buttonFunc(stage, outputStream, this);
                batch.begin();
                batch.draw(new Texture("Textures/robber.png"), cell.getCellCords().getX(), cell.getCellCords().getY());
                batch.end();            }
            else if(cell instanceof ResourceCell) {
                cell.buttonFunc(stage, outputStream, this);
            }
        }
        renderMap();
    }

    private void renderMap() {
        for(Cell cell : map.getMap(map.getCenterCell(), new HashSet<Cell>())) {
            if(cell instanceof VillageCell || cell instanceof RoadCell) {
                cell.buttonFunc(stage, outputStream, this);
            }

        }
    }

    private void displayPlayers() {
        int y = 700;
        playerTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        playerIndicatorBatch.draw(playerTexture, 1700, y+=100, 50, 50);
    }

    private void setUpResourceLabels() {
        int x = 325;
        for(ResourceType type : ResourceType.values()) {
            if (type != ResourceType.EMPTY) {
                Label.LabelStyle labelStyle = new Label.LabelStyle();
                labelStyle.font = new BitmapFont();
                labelStyle.font.getData().setScale(2f);
                labelStyle.fontColor = Color.SALMON;
                Label resourceLabel = new Label(deck.getResourceAmount(type).toString(), labelStyle);
                resourceLabel.setPosition(x, 50);
                Texture texture = new Texture("Cards/" + type.toString().toLowerCase() + "Card.png");
                Image image = new Image(texture);
                image.setPosition(x - 25, 100);
                x += 100;
                ResourceDisplay resourceDisplay = new ResourceDisplay(image, resourceLabel);
                resourceLabels.put(type, resourceDisplay);
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        stage.dispose();
        updateThread.stopThread();
        updateThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //**********SETTERS**********//
    public synchronized void setMap(Map map) {
        this.map = map;
    }
    public synchronized void setPlayersAmount(int playersAmount) {
        this.playersAmount = playersAmount;
    }
    public synchronized void setIsTurn(boolean isTurn) {
        this.isTurn = isTurn;
    }
    public synchronized void setDiceThrow(int diceThrow) {
        this.diceThrow = diceThrow;
        displayDiceThrow();
    }
    public synchronized void setDeck(HashMap<ResourceType, Integer> deck) {
        if(!this.deck.equals(new Deck(deck))) {
            for (ResourceType type : ResourceType.values()) {
                if (type != ResourceType.EMPTY) {
                    changeLabelAmount(type, deck.get(type));
                }
            }
        }
    }
    public synchronized void setOffer(Offer offer) {
        this.offer = offer;
    }
    public synchronized void setDiceThrown(boolean isDiceThrown) {
        this.isDiceThrown = isDiceThrown;
    }
    public synchronized void setRobberCell(ResourceCell cell) {
        map.setRobberCell(cell);
    }
    public synchronized void setVillageCell(VillageCell cell) {
        map.setVillageCell(cell);
    }
    public synchronized void setRoadCell(RoadCell cell) {
        map.setRoadCell(cell);
    }

    //**********GETTERS**********//
    public synchronized boolean getIsTurn() {
        return isTurn;
    }
    public synchronized int getId() {
        return id;
    }
}
