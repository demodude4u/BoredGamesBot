package com.demod.discord.boredgames.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.demod.discord.boredgames.Display;
import com.demod.discord.boredgames.Emojis;
import com.demod.discord.boredgames.Game;
import com.google.common.util.concurrent.Uninterruptibles;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class Connect4Game extends Game {
	public static enum Tile {
		NONE, P1, P2, P3, P4
	}

	private static final int IN_A_ROW = 4;

	private boolean hotseat;
	private UnicodeEmoji[] emojiPlayerSet;
	private int columns = 7;
	private int rows = 6;
	private Tile[/* slot */][/* height */] tiles;
	private final List<User> players = new ArrayList<>(4);
	private int winner = -1;
	private int lastTurnColumn = -1;
	private int lastTurn = -1;

	public Connect4Game() {
		tiles = new Tile[columns][rows];
		for (Tile[] column : tiles) {
			Arrays.fill(column, Tile.NONE);
		}

		emojiPlayerSet = Emojis.getRandomPlayerSet();
	}

	private void animateMove(int column, Tile tile) {
		for (int i = rows - 1; i >= 0; i--) {
			int height = i;
			if (tiles[column][height] == Tile.NONE) {

				tiles[column][height] = tile;
				displayChannel(embed -> {
					embed.setDescription(generateTilesEmoji());
				}).ignoreReactions().send();
				Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);
				tiles[column][height] = Tile.NONE;

			} else {
				break;
			}
		}
	}

	public void applyMove(int column, Tile tile) {
		for (int i = rows - 1; i >= 0; i--) {
			int height = i;
			if (height == 0 || tiles[column][height - 1] != Tile.NONE) {
				tiles[column][height] = tile;
				checkForWinner(column, height, tile);
				return;
			}
		}
		throw new InternalError("Illegal Game State!");
	}

	private boolean canMove(int column) {
		return tiles[column][rows - 1] == Tile.NONE;
	}

	private void checkForWinner(int column, int height, Tile tile) {
		int inARow, startColumn, startHeight;

		// Check for horizontal
		inARow = 0;
		for (int i = 0; i < columns; i++) {
			if (tiles[i][height] == tile) {
				inARow++;
				if (inARow == IN_A_ROW) {
					winner = tile.ordinal() - 1;
					return;
				}
			} else {
				inARow = 0;
			}
		}

		// Check for vertical
		inARow = 0;
		for (int i = 0; i < rows; i++) {
			if (tiles[column][i] == tile) {
				inARow++;
				if (inARow == IN_A_ROW) {
					winner = tile.ordinal() - 1;
					return;
				}
			} else {
				inARow = 0;
			}
		}

		// Check for diagonal SE -> NW
		inARow = 0;
		startColumn = Math.max(0, column - height);
		startHeight = height - (column - startColumn);
		for (int i = 0; (startColumn + i < columns) && (startHeight + i < rows); i++) {
			if (tiles[startColumn + i][startHeight + i] == tile) {
				inARow++;
				if (inARow == IN_A_ROW) {
					winner = tile.ordinal() - 1;
					return;
				}
			} else {
				inARow = 0;
			}
		}

		// Check for diagonal SW -> NE
		inARow = 0;
		startColumn = Math.min(columns - 1, column + height);
		startHeight = height - (startColumn - column);
		for (int i = 0; (startColumn - i >= 0) && (startHeight + i < rows); i++) {
			if (tiles[startColumn - i][startHeight + i] == tile) {
				inARow++;
				if (inARow == IN_A_ROW) {
					winner = tile.ordinal() - 1;
					return;
				}
			} else {
				inARow = 0;
			}
		}
	}

	private String generateTilesEmoji() {
		StringBuilder sb = new StringBuilder();
		for (int height = rows - 1; height >= 0; height--) {
			for (int column = 0; column < columns; column++) {
				Tile tile = tiles[column][height];
				if (tile == Tile.NONE) {
					if (height == rows - 1 && column == lastTurnColumn) {
						sb.append(Emojis.SMALL_RED_DOWN_ARROW.getFormatted());
					} else {
						sb.append(Emojis.SMALL_BLACK_SQUARE.getFormatted());
					}
				} else {
					sb.append(emojiPlayerSet[tile.ordinal() - 1].getFormatted());
				}
			}
			sb.append('\n');
		}
		for (int column = 0; column < columns; column++) {
			sb.append(Emojis.BLOCK_NUMBER[column + 1].getFormatted());
		}
		return sb.toString();
	}

	@Override
	protected String getTitle() {
		return "Connect Four!";
	}

	private void hotseatNewPlayer(User player) {
		if (players.size() < 4 && !players.contains(player)) {
			players.add(player);

			if (players.size() >= 3) {
				rows++;
				columns++;
				tiles = Arrays.copyOf(tiles, columns);
				for (int i = 0; i < columns - 1; i++) {
					tiles[i] = Arrays.copyOf(tiles[i], rows);
					tiles[i][rows - 1] = Tile.NONE;
				}
				tiles[columns - 1] = new Tile[rows];
				Arrays.fill(tiles[columns - 1], Tile.NONE);
			}
		}
	}

	public boolean isGameOver() {
		if (winner != -1) {
			return true;
		}
		for (int column = 0; column < columns; column++) {
			if (tiles[column][rows - 1] == Tile.NONE) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void run() {
		runHotseatPhase();
		runPlayPhase();
		runGameOverPhase();
	}

	private void runGameOverPhase() {
		displayChannel(embed -> {
			boolean mystery = emojiPlayerSet[0].equals(Emojis.QUESTION);
			if (mystery) {
				emojiPlayerSet = Emojis.getRandomPlayerSet();
			}
			if (winner != -1) {
				embed.addField(players.get(winner).getEffectiveName() + " Wins!",
						IntStream.range(0, 4).mapToObj(i -> mystery ? Emojis.QUESTION : emojiPlayerSet[winner])
								.map(Emoji::getFormatted).collect(Collectors.joining()),
						true);
			} else {
				embed.addField("It's a Draw!",
						IntStream.range(0, players.size()).mapToObj(i -> mystery ? Emojis.QUESTION : emojiPlayerSet[i])
								.map(Emoji::getFormatted).collect(Collectors.joining()),
						true);
			}

			embed.setDescription(generateTilesEmoji());

			embed.setFooter("This game has ended.", null);
		}).send();
	}

	private void runHotseatPhase() {
		hotseat = true;
		while (hotseat) {
			Display<Boolean> display = displayChannel(embed -> {
				embed.setDescription(
						"This is a 2-4 player game. Try to create a 4 in a row chain of your player token to win!");

				if (!players.isEmpty()) {
					embed.addField("Players",
							IntStream.range(0, players.size())
									.mapToObj(i -> emojiPlayerSet[i] + " " + players.get(i).getEffectiveName())
									.collect(Collectors.joining("\n")),
							true);
				}
			});

			if (players.size() < 4) {
				display.addAction(ButtonStyle.PRIMARY, Emojis.HAND_SPLAYED, "Join", player -> {
					hotseatNewPlayer(player);
				});
			}
			if (players.size() >= 2) {
				display.addExclusiveAction(players, ButtonStyle.DANGER, Emojis.GAME_DIE, "Start Game", player -> {
					hotseat = false;
				});
			}

			display.send();
		}
	}

	private void runPlayPhase() {
		while (!isGameOver()) {
			int turn = (lastTurn + 1) % players.size();
			User player = players.get(turn);

			Display<Integer> display = displayChannel(embed -> {
				String message = IntStream.range(0, players.size()).mapToObj(i -> emojiPlayerSet[i])
						.map(Emoji::getFormatted).collect(Collectors.joining())
						+ "\n"
						+ IntStream.range(0, players.size())
								.mapToObj(i -> (turn == i) ? Emojis.ARROW_UP : Emojis.SMALL_BLACK_SQUARE)
								.map(Emoji::getFormatted).collect(Collectors.joining());
				embed.addField(player.getEffectiveName() + "'s Turn", message, true);

				embed.setDescription(generateTilesEmoji());

				embed.setFooter("Press the column number to place your piece.", null);
			});

			for (int i = 0; i < columns; i++) {
				final int column = i;
				if (canMove(column)) {
					display.addExclusiveResult(player, ButtonStyle.SECONDARY, null, Integer.toString(i + 1), column);
				}
			}

			Tile tile = Tile.values()[1 + turn];
			lastTurnColumn = display.send();
			lastTurn = turn;

			animateMove(lastTurnColumn, tile);
			applyMove(lastTurnColumn, tile);
		}
	}
}
