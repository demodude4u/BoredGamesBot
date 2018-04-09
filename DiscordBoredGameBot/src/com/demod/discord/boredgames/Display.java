package com.demod.discord.boredgames;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;

public class Display<T> {

	@FunctionalInterface
	public interface Action {
		default boolean accept(Member player) {
			return true;
		}

		void call(Member player);
	}

	@FunctionalInterface
	public interface ResultAction<T> {
		default boolean accept(Member player) {
			return true;
		}

		T call(Member player);
	}

	private final Function<Display<T>, T> displayer;

	private final EmbedBuilder builder = new EmbedBuilder();

	private final LinkedHashMap<String, ResultAction<T>> actions = new LinkedHashMap<>();
	private final Set<Member> notify = new LinkedHashSet<>();

	Display(Function<Display<T>, T> displayer) {
		this.displayer = displayer;
	}

	public Display<T> addAction(String emoji, Action action) {
		return addAction(emoji, new ResultAction<T>() {
			@Override
			public boolean accept(Member player) {
				return action.accept(player);
			}

			@Override
			public T call(Member p) {
				action.call(p);
				return null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public Display<T> addAction(String emoji, ResultAction<? extends T> action) {
		actions.put(emoji, (ResultAction<T>) action);
		return this;
	};

	public Display<T> addAction(String emoji, T result) {
		return addAction(emoji, p -> {
			return result;
		});
	}

	public Display<T> addExclusiveAction(List<Member> players, String emoji, Action action) {
		return addExclusiveAction(players, emoji, new ResultAction<T>() {
			@Override
			public boolean accept(Member player) {
				return action.accept(player);
			}

			@Override
			public T call(Member p) {
				action.call(p);
				return null;
			}
		});
	}

	public Display<T> addExclusiveAction(List<Member> players, String emoji, ResultAction<? extends T> action) {
		notify.addAll(players);
		return addAction(emoji, new ResultAction<T>() {
			@Override
			public boolean accept(Member p) {
				return players.stream().anyMatch(p2 -> p.getUser().getId().equals(p2.getUser().getId()))
						&& action.accept(p);
			}

			@Override
			public T call(Member p) {
				return action.call(p);
			}
		});
	}

	public Display<T> addExclusiveAction(List<Member> players, String emoji, T result) {
		return addExclusiveAction(players, emoji, p -> {
			return result;
		});
	}

	public Display<T> addExclusiveAction(Member player, String emoji, Action action) {
		return addExclusiveAction(player, emoji, new ResultAction<T>() {
			@Override
			public boolean accept(Member player) {
				return action.accept(player);
			}

			@Override
			public T call(Member p) {
				action.call(p);
				return null;
			}
		});
	}

	public Display<T> addExclusiveAction(Member player, String emoji, ResultAction<? extends T> action) {
		return addExclusiveAction(Arrays.asList(player), emoji, action);
	}

	public Display<T> addExclusiveAction(Member player, String emoji, T result) {
		return addExclusiveAction(player, emoji, p -> {
			return result;
		});
	}

	LinkedHashMap<String, ResultAction<T>> getActions() {
		return actions;
	}

	public EmbedBuilder getBuilder() {
		return builder;
	}

	public Set<Member> getNotify() {
		return notify;
	}

	public T send() {
		return displayer.apply(this);
	}
}
