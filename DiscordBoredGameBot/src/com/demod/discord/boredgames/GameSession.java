package com.demod.discord.boredgames;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Stack;

import com.demod.discord.boredgames.Game.Action;

import net.dv8tion.jda.core.entities.Message;

public class GameSession {
	private Game game;
	private Optional<Message> display = Optional.empty();
	private Optional<LinkedHashMap<String, Action>> actions = Optional.empty();
	private final Stack<Game> undo = new Stack<>();
	private long lastUpdateMillis = System.currentTimeMillis();

	public GameSession(Game game) {
		this.game = game;
	}

	public Optional<LinkedHashMap<String, Action>> getActions() {
		return actions;
	}

	public Optional<Message> getDisplay() {
		return display;
	}

	public Game getGame() {
		return game;
	}

	public long getLastUpdateMillis() {
		return lastUpdateMillis;
	}

	public Stack<Game> getUndo() {
		return undo;
	}

	public void setActions(Optional<LinkedHashMap<String, Action>> actions) {
		this.actions = actions;
	}

	public void setDisplay(Optional<Message> display) {
		this.display = display;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public void setLastUpdateMillis() {
		lastUpdateMillis = System.currentTimeMillis();
	}

}
