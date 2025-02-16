package com.demod.discord.boredgames;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class Display<T> {

	@FunctionalInterface
	public interface Action {
		default boolean accept(User player) {
			return true;
		}

		default <T> ResultAction<T> asResultAction() {
			return new ResultAction<T>() {
				@Override
				public T call(User player) {
					Action.this.call(player);
					return null;
				}
			};
		}

		void call(User player);
	}

	public static class ActionButton<T> {
		public final Button button;
		public final ResultAction<T> action;

		public ActionButton(ButtonStyle style, Emoji emoji, String label, List<User> players, ResultAction<T> action) {
			this(style, emoji, label, new ResultAction<T>() {
				@Override
				public boolean accept(User player) {
					return players.stream().anyMatch(p2 -> player.getId().equals(p2.getId())) && action.accept(player);
				}

				@Override
				public T call(User p) {
					return action.call(p);
				}
			});
		}

		public ActionButton(ButtonStyle style, Emoji emoji, String label, ResultAction<T> action) {
			this.button = Button.of(style, Integer.toString(System.identityHashCode(this)), label, emoji);
			this.action = action;
		}
	}

	@FunctionalInterface
	public interface ResultAction<T> {
		default boolean accept(User player) {
			return true;
		}

		T call(User player);
	}

	private final Function<Display<T>, T> displayer;

	private final EmbedBuilder builder = new EmbedBuilder();

	private final LinkedHashMap<String, ActionButton<T>> actions = new LinkedHashMap<>();
	private final List<List<ActionButton<T>>> actionRows = new ArrayList<>();
	private boolean nextActionRow = true;

	private final Set<User> notify = new LinkedHashSet<>();

	private boolean ignoreReactions;

	Display(Function<Display<T>, T> displayer) {
		this.displayer = displayer;
	}

	public Display<T> addAction(ActionButton<T> button) {
		actions.put(button.button.getId(), button);
		if (!actionRows.isEmpty()
				&& actionRows.get(actionRows.size() - 1).size() + 1 > button.button.getType().getMaxPerRow()) {
			nextActionRow = true;
		}
		if (nextActionRow) {
			nextActionRow = false;
			actionRows.add(new ArrayList<>());
		}
		actionRows.get(actionRows.size() - 1).add(button);
		return this;
	}

	public Display<T> addAction(ButtonStyle style, Emoji emoji, String label, Action action) {
		return addAction(new ActionButton<>(style, emoji, label, action.asResultAction()));
	}

	public Display<T> addExclusiveAction(List<User> players, ButtonStyle style, Emoji emoji, String label,
			Action action) {
		return addAction(new ActionButton<>(style, emoji, label, players, action.asResultAction()));
	}

	public Display<T> addExclusiveAction(User player, ButtonStyle style, Emoji emoji, String label, Action action) {
		return addAction(new ActionButton<>(style, emoji, label, ImmutableList.of(player), action.asResultAction()));
	}

	public Display<T> addExclusiveResult(List<User> players, ButtonStyle style, Emoji emoji, String label, T result) {
		return addAction(new ActionButton<>(style, emoji, label, players, p -> result));
	}

	public Display<T> addExclusiveResult(User player, ButtonStyle style, Emoji emoji, String label, T result) {
		return addAction(new ActionButton<>(style, emoji, label, ImmutableList.of(player), p -> result));
	}

	public Display<T> addExclusiveResultAction(List<User> players, ButtonStyle style, Emoji emoji, String label,
			ResultAction<T> action) {
		return addAction(new ActionButton<>(style, emoji, label, players, action));
	}

	public Display<T> addExclusiveResultAction(User player, ButtonStyle style, Emoji emoji, String label,
			ResultAction<T> action) {
		return addAction(new ActionButton<>(style, emoji, label, ImmutableList.of(player), action));
	}

	public Display<T> addResult(ButtonStyle style, Emoji emoji, String label, T result) {
		return addAction(new ActionButton<>(style, emoji, label, p -> result));
	}

	public Display<T> addResultAction(ButtonStyle style, Emoji emoji, String label, ResultAction<T> action) {
		return addAction(new ActionButton<>(style, emoji, label, action));
	}

	public List<List<ActionButton<T>>> getActionRows() {
		return actionRows;
	}

	LinkedHashMap<String, ActionButton<T>> getActions() {
		return actions;
	}

	public EmbedBuilder getBuilder() {
		return builder;
	}

	public Set<User> getNotify() {
		return notify;
	}

	public Display<T> ignoreReactions() {
		ignoreReactions = true;
		return this;
	}

	public boolean isIgnoreReactions() {
		return ignoreReactions;
	}

	public void nextActionRow() {
		nextActionRow = true;
	}

	public T send() {
		return displayer.apply(this);
	}
}
