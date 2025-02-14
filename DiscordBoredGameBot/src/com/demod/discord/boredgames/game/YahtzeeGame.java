package com.demod.discord.boredgames.game;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.demod.discord.boredgames.Display;
import com.demod.discord.boredgames.Emojis;
import com.demod.discord.boredgames.Game;
import com.google.common.primitives.Booleans;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class YahtzeeGame extends Game {

	private enum Category {//
		ONES("Ones", Emojis.BLOCK_1), //
		TWOS("Twos", Emojis.BLOCK_2), //
		THREES("Threes", Emojis.BLOCK_3), //
		FOURS("Fours", Emojis.BLOCK_4), //
		FIVES("Fives", Emojis.BLOCK_5), //
		SIXES("Sixes", Emojis.BLOCK_6), //
		THREE_OF_A_KIND("Three of a Kind", Emojis.FAMILY_MMB), //
		FOUR_OF_A_KIND("Four of a Kind", Emojis.FAMILY_MMBB), //
		FULL_HOUSE("Full House", Emojis.HOUSE), //
		SMALL_STRAIGHT("Small Straight", Emojis.BLOCK_1234), //
		LARGE_STRAIGHT("Large Straight", Emojis.SIGNAL_STRENGTH), //
		CHANCE("Chance", Emojis.QUESTION), //
		YAHTZEE("Yahtzee", Emojis.BANGBANG),//
		;

		private final String label;
		private final Emoji emoji;

		private Category(String label, Emoji emoji) {
			this.label = label;
			this.emoji = emoji;
		}
	}

	private static final String JSONKEY_PLAYCOUNT = "play-count";
	private static final String JSONKEY_BESTSCORE = "best-score";

	private static final SortedMap<Integer, String> gameOverPhrases = new TreeMap<>();
	static {
		gameOverPhrases.put(0, "You want a higher score, not a lower one...");
		gameOverPhrases.put(50, "You can do better than that...");
		gameOverPhrases.put(100, "Go play Yahtzee Junior...");
		gameOverPhrases.put(150, "Must've been unlucky rolls.");
		gameOverPhrases.put(200, "Not a bad score.");
		gameOverPhrases.put(250, "Good score.");
		gameOverPhrases.put(300, "Well played!");
		gameOverPhrases.put(350, "You are good at this!");
		gameOverPhrases.put(400, "NICE ROLLS!");
		gameOverPhrases.put(600, "WHAT A GOOD SCORE!");
		gameOverPhrases.put(800, "GO BUY A LOTTERY TICKET!");
		gameOverPhrases.put(1000, "YOU ARE A YAHTZEE GOD!");
	}

	private User player = null;
	private final int[] categoryPoints;
	private final boolean[] categoryScored;
	private int upperBonusPoints;
	private int yahtzeeBonusPoints;
	private final int[] rolledDice;
	private final boolean[] lockedDice;
	private int rollCount;
	private final Random rand;

	public YahtzeeGame() {
		categoryPoints = new int[Category.values().length];
		categoryScored = new boolean[categoryPoints.length];
		upperBonusPoints = 0;
		yahtzeeBonusPoints = 0;
		rolledDice = new int[5];
		lockedDice = new boolean[5];
		rand = new Random();
	}

	private boolean checkFullHouse() {
		return Arrays.stream(rolledDice).boxed().collect(Collectors.groupingBy(i -> i)).values().stream()
				.mapToInt(List::size).filter(s -> s >= 2 && s <= 3).sum() == 5;
	}

	private boolean checkJoker() {
		return checkYahtzee() && categoryScored[Category.YAHTZEE.ordinal()];
	}

	private boolean checkJokerUpperScored() {
		return categoryScored[rolledDice[0] - 1];
	}

	private boolean checkLowerAvailable() {
		return Booleans.asList(categoryScored).stream().skip(6).anyMatch(s -> !s);
	}

	private boolean checkOfAKind(int count) {
		return Arrays.stream(rolledDice).boxed().collect(Collectors.groupingBy(i -> i)).values().stream()
				.anyMatch(l -> l.size() >= count);
	}

	private boolean checkStraight(int count) {
		int sequence = 0;
		for (int i = 1; i <= 6; i++) {
			final int match = i;
			if (Arrays.stream(rolledDice).anyMatch(dice -> dice == match)) {
				sequence++;
				if (sequence == count) {
					return true;
				}
			} else {
				sequence = 0;
			}
		}
		return false;
	}

	private boolean checkYahtzee() {
		return Arrays.stream(rolledDice).distinct().count() == 1;
	}

	private void chooseCategory(Category category) {
		if (checkJoker() && (categoryPoints[Category.YAHTZEE.ordinal()] > 0)) {
			yahtzeeBonusPoints += 100;
		}

		if ((upperBonusPoints == 0) && (category.ordinal() < 6) && (getUpperPoints() + getPoints(category) >= 63)) {
			upperBonusPoints = 35;
		}

		int points = checkJoker() ? getJokerPoints(category) : getPoints(category);
		categoryPoints[category.ordinal()] = points;
		categoryScored[category.ordinal()] = true;
	}

	private int[] generateBonusPoints() {
		int[] bonusPoints = new int[categoryPoints.length];
		for (int i = 0; i < bonusPoints.length; i++) {
			Category category = Category.values()[i];
			bonusPoints[i] = getBonusPoints(category);
		}
		return bonusPoints;
	}

	private String generateDisplayChoices(int[] gainPoints, int[] bonusPoints) {
		StringBuilder sb = new StringBuilder();

		boolean firstLine = true;
		for (int i = 0; i < categoryPoints.length; i++) {
			Category category = Category.values()[i];

			if (categoryScored[i]) {
				continue;
			}

			if (!firstLine) {
				sb.append('\n');
			} else {
				firstLine = false;
			}

			sb.append(category.emoji.getFormatted() + " ");

			sb.append((gainPoints[i] > 0) ? ("**+" + gainPoints[i] + "**") : Integer.toString(gainPoints[i]));
			sb.append((bonusPoints[i] > 0) ? (" **(+" + bonusPoints[i] + " Bonus)**") : "");

			sb.append(" " + category.label);
		}

		return sb.toString();
	}

	private String generateDisplayLeaderboard() {
		List<Entry<User, Integer>> leaderboard = getSaves().entrySet().stream()
				.map(e -> new SimpleImmutableEntry<>(e.getKey(), e.getValue().optInt(JSONKEY_BESTSCORE, 0)))
				.sorted((p1, p2) -> Integer.compare(p2.getValue(), p1.getValue())).limit(10)
				.collect(Collectors.toList());

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < leaderboard.size(); i++) {
			if (i > 0) {
				sb.append('\n');
			}
			Entry<User, Integer> pair = leaderboard.get(i);
			sb.append("#" + (i + 1) + " " + pair.getKey().getEffectiveName() + " (" + pair.getValue() + " points)");
		}
		return sb.toString();
	}

	private String generateDisplayRolledDice(boolean showLocks) {
		StringBuilder sb = new StringBuilder();

		for (int dice : rolledDice) {
			sb.append(Emojis.BLOCK_NUMBER[dice].getFormatted());
		}

		if (showLocks) {
			sb.append('\n');
			for (boolean lock : lockedDice) {
				sb.append((lock ? Emojis.LOCK : Emojis.SMALL_BLACK_SQUARE).getFormatted());
			}
		}

		return sb.toString();
	}

	private String generateDisplayScoreCard() {
		StringBuilder sb = new StringBuilder();

		BiFunction<Integer, String, String> formatter = (num, label) -> num != null
				? String.format("`|%3d|` %s\n", num, label)
				: String.format("`|   |` %s\n", label);
		BiFunction<Integer, String, String> bonusFormatter = (num, label) -> formatter.apply(num > 0 ? num : null,
				label);

		for (int i = 0; i < categoryPoints.length; i++) {
			Category category = Category.values()[i];
			sb.append(formatter.apply(categoryScored[i] ? categoryPoints[i] : null, category.label));
		}

		sb.append(bonusFormatter.apply(upperBonusPoints, "Upper Bonus (" + getUpperPoints() + "/" + 63 + ")"));
		if (yahtzeeBonusPoints > 0) {
			sb.append(bonusFormatter.apply(yahtzeeBonusPoints, "__*Yahtzee Bonus*__"));
		}
		sb.append(formatter.apply(getTotalScore(), "__**Total Score**__"));

		return sb.toString();
	}

	private int[] generateGainPoints() {
		boolean joker = checkJoker();
		int[] gainPoints = new int[categoryPoints.length];
		for (int i = 0; i < gainPoints.length; i++) {
			if (categoryScored[i]) {
				continue;
			}
			Category category = Category.values()[i];
			gainPoints[i] = joker ? getJokerPoints(category) : getPoints(category);
		}
		return gainPoints;
	}

	private int getBonusPoints(Category category) {
		if (checkJoker()) {
			return 100;
		}
		switch (category) {
		case ONES:
		case TWOS:
		case THREES:
		case FOURS:
		case FIVES:
		case SIXES:
			return ((upperBonusPoints == 0) && (getUpperPoints() + getPoints(category) >= 63)) ? 35 : 0;
		case THREE_OF_A_KIND:
		case FOUR_OF_A_KIND:
		case FULL_HOUSE:
		case SMALL_STRAIGHT:
		case LARGE_STRAIGHT:
		case CHANCE:
		case YAHTZEE:
			return 0;
		}
		throw new InternalError();
	}

	private String getGameOverMessage() {
		int totalScore = getTotalScore();
		String message = null;
		for (Entry<Integer, String> entry : gameOverPhrases.entrySet()) {
			if (totalScore >= entry.getKey()) {
				message = entry.getValue();
			} else {
				break;
			}
		}
		return message;
	}

	private int getJokerPoints(Category category) {
		switch (category) {
		case ONES:
		case TWOS:
		case THREES:
		case FOURS:
		case FIVES:
		case SIXES:
		case THREE_OF_A_KIND:
		case FOUR_OF_A_KIND:
		case CHANCE:
			return getPoints(category);
		case FULL_HOUSE:
			return 25;
		case SMALL_STRAIGHT:
			return 30;
		case LARGE_STRAIGHT:
			return 40;
		case YAHTZEE:
			throw new InternalError();
		}
		throw new InternalError();
	}

	private int getPoints(Category category) {
		switch (category) {
		case ONES:
		case TWOS:
		case THREES:
		case FOURS:
		case FIVES:
		case SIXES:
			return Arrays.stream(rolledDice).filter(dice -> dice == category.ordinal() + 1).sum();
		case THREE_OF_A_KIND:
			return checkOfAKind(3) ? Arrays.stream(rolledDice).sum() : 0;
		case FOUR_OF_A_KIND:
			return checkOfAKind(4) ? Arrays.stream(rolledDice).sum() : 0;
		case FULL_HOUSE:
			return checkFullHouse() ? 25 : 0;
		case SMALL_STRAIGHT:
			return checkStraight(4) ? 30 : 0;
		case LARGE_STRAIGHT:
			return checkStraight(5) ? 40 : 0;
		case CHANCE:
			return Arrays.stream(rolledDice).sum();
		case YAHTZEE:
			return checkYahtzee() ? 50 : 0;
		}
		throw new InternalError();
	}

	@Override
	protected String getTitle() {
		return "Yahtzee!";
	}

	public int getTotalScore() {
		return Arrays.stream(categoryPoints).sum() + upperBonusPoints + yahtzeeBonusPoints;
	}

	private int getUpperPoints() {
		return Arrays.stream(categoryPoints).limit(6).sum();
	}

	public boolean isGameOver() {
		for (boolean scored : categoryScored) {
			if (!scored) {
				return false;
			}
		}
		return true;
	}

	private void rollDice() {
		for (int i = 0; i < rolledDice.length; i++) {
			if (!lockedDice[i]) {
				rolledDice[i] = rand.nextInt(6) + 1;
			}
		}

		rollCount++;
	}

	@Override
	public void run() {
		runHotseatPhase();
		while (!isGameOver()) {
			runRollingPhase();
			runChoosingPhase();
		}
		runGameOverPhase();
	}

	private void runChoosingPhase() {
		int[] gainPoints = generateGainPoints();
		int[] bonusPoints = generateBonusPoints();
		Function<Integer, ButtonStyle> style = i -> bonusPoints[i] > 0 ? ButtonStyle.SUCCESS
				: gainPoints[i] > 0 ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY;

		Display<?> display = displayChannel(embed -> {
			embed.setAuthor(player.getEffectiveName(), null, player.getEffectiveAvatarUrl());
			embed.addField("Score Card", generateDisplayScoreCard(), true);
			embed.addField("Choose Scoring", generateDisplayChoices(gainPoints, bonusPoints), true);
			embed.addField("Rolled Dice", generateDisplayRolledDice(false), false);
			embed.setFooter("Press the matching symbol to choose a category.", null);
		});

		if (checkJoker()) {// Forced Joker Rules
			if (!checkJokerUpperScored()) {
				Category category = Category.values()[rolledDice[0] - 1];
				display.addExclusiveAction(player, style.apply(category.ordinal()), category.emoji, category.label,
						p -> chooseCategory(category));
			} else if (checkLowerAvailable()) {
				for (int i = 6; i < categoryPoints.length; i++) {
					final Category category = Category.values()[i];
					if (categoryScored[i]) {
						continue;
					}
					display.addExclusiveAction(player, style.apply(i), category.emoji, category.label,
							p -> chooseCategory(category));
				}
			} else {
				for (int i = 0; i < 6; i++) {
					final Category category = Category.values()[i];
					if (categoryScored[i]) {
						continue;
					}
					display.addExclusiveAction(player, style.apply(i), category.emoji, category.label,
							p -> chooseCategory(category));
				}
			}
		} else {
			for (int i = 0; i < categoryPoints.length; i++) {
				final Category category = Category.values()[i];
				if (categoryScored[i]) {
					continue;
				}
				display.addExclusiveAction(player, style.apply(i), category.emoji, category.label,
						p -> chooseCategory(category));
			}
		}

		display.send();
	}

	private void runGameOverPhase() {
		savePlayerScore();

		displayChannel(embed -> {
			embed.setAuthor(player.getEffectiveName(), null, player.getEffectiveAvatarUrl());
			embed.addField("Score Card", generateDisplayScoreCard(), true);
			embed.addField("Leaderboard", generateDisplayLeaderboard(), true);
			embed.setFooter("Game over! " + getGameOverMessage(), null);
		}).send();
	}

	private void runHotseatPhase() {
		displayChannel(embed -> {
			embed.setDescription(
					"This is a singleplayer game. Press the " + Emojis.GAME_DIE.getFormatted() + " to start!");
			embed.addField("Leaderboard", generateDisplayLeaderboard(), true);

		}).addAction(ButtonStyle.SUCCESS, Emojis.GAME_DIE, "Roll", p -> {
			this.player = p;

		}).send();
	}

	private void runRollingPhase() {
		rollCount = 0;
		Arrays.fill(lockedDice, false);
		rollDice();

		while (rollCount < 3) {
			Display<?> display = displayChannel(embed -> {
				embed.setAuthor(player.getEffectiveName(), null, player.getEffectiveAvatarUrl());
				embed.addField("Score Card", generateDisplayScoreCard(), false);
				embed.addField("Rolled Dice", generateDisplayRolledDice(true), true);
				embed.addField("Rolls", "**" + (3 - rollCount) + "** Remaining", true);
				embed.setFooter("Press 1-5 to lock the dice, press dice to roll again.", null);
			});//

			display.addExclusiveAction(player, lockedDice[0] ? ButtonStyle.DANGER : ButtonStyle.SECONDARY,
					Emojis.BLOCK_NUMBER[rolledDice[0]], null, p -> toggleLockDice(0));
			display.addExclusiveAction(player, lockedDice[1] ? ButtonStyle.DANGER : ButtonStyle.SECONDARY,
					Emojis.BLOCK_NUMBER[rolledDice[1]], null, p -> toggleLockDice(1));
			display.addExclusiveAction(player, lockedDice[2] ? ButtonStyle.DANGER : ButtonStyle.SECONDARY,
					Emojis.BLOCK_NUMBER[rolledDice[2]], null, p -> toggleLockDice(2));
			display.addExclusiveAction(player, lockedDice[3] ? ButtonStyle.DANGER : ButtonStyle.SECONDARY,
					Emojis.BLOCK_NUMBER[rolledDice[3]], null, p -> toggleLockDice(3));
			display.addExclusiveAction(player, lockedDice[4] ? ButtonStyle.DANGER : ButtonStyle.SECONDARY,
					Emojis.BLOCK_NUMBER[rolledDice[4]], null, p -> toggleLockDice(4));
			display.addExclusiveAction(player, ButtonStyle.PRIMARY, Emojis.GAME_DIE, "Roll", p -> rollDice());
			display.send();
		}
	}

	private void savePlayerScore() {
		JSONObject playerSave = getSave(player).orElseGet(JSONObject::new);
		System.out.println(playerSave.toString(2));
		playerSave.put(JSONKEY_BESTSCORE, Math.max(getTotalScore(), playerSave.optInt(JSONKEY_BESTSCORE, 0)));
		playerSave.put(JSONKEY_PLAYCOUNT, playerSave.optInt(JSONKEY_PLAYCOUNT, 0) + 1);
		System.out.println(playerSave.toString(2));
		setSave(player, Optional.of(playerSave));
	}

	private void toggleLockDice(int dice) {
		lockedDice[dice] = !lockedDice[dice];
	}

}
