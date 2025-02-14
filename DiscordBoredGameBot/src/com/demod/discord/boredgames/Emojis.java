package com.demod.discord.boredgames;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.utils.data.DataObject;

public final class Emojis {
	private static class FixToStringEmoji implements UnicodeEmoji, EmojiUnion {
		UnicodeEmoji delegate;

		public FixToStringEmoji(UnicodeEmoji emoji) {
			delegate = emoji;
		}

		@Override
		public ApplicationEmoji asApplication() {
			return ((EmojiUnion) delegate).asApplication();
		}

		@Override
		public CustomEmoji asCustom() {
			return ((EmojiUnion) delegate).asCustom();
		}

		@Override
		public RichCustomEmoji asRich() {
			return ((EmojiUnion) delegate).asRich();
		}

		@Override
		public UnicodeEmoji asUnicode() {
			return ((EmojiUnion) delegate).asUnicode();
		}

		@Override
		public String getAsCodepoints() {
			return delegate.getAsCodepoints();
		}

		@Override
		public String getAsReactionCode() {
			return delegate.getAsReactionCode();
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public DataObject toData() {
			return delegate.toData();
		}

		@Override
		public String toString() {
			return delegate.getFormatted();
		}
	}

	public static final UnicodeEmoji BLOCK_0 = fromUnicode("0⃣");
	public static final UnicodeEmoji BLOCK_1 = fromUnicode("1⃣");
	public static final UnicodeEmoji BLOCK_2 = fromUnicode("2⃣");
	public static final UnicodeEmoji BLOCK_3 = fromUnicode("3⃣");
	public static final UnicodeEmoji BLOCK_4 = fromUnicode("4⃣");
	public static final UnicodeEmoji BLOCK_5 = fromUnicode("5⃣");
	public static final UnicodeEmoji BLOCK_6 = fromUnicode("6⃣");
	public static final UnicodeEmoji BLOCK_7 = fromUnicode("7⃣");
	public static final UnicodeEmoji BLOCK_8 = fromUnicode("8⃣");
	public static final UnicodeEmoji BLOCK_9 = fromUnicode("9⃣");
	public static final UnicodeEmoji BLOCK_10 = fromUnicode("\uD83D\uDD1F");
	public static final UnicodeEmoji BLOCK_1234 = fromUnicode("\uD83D\uDD22");
	public static final UnicodeEmoji BLOCK_OK = fromUnicode("\uD83C\uDD97");

	public static final UnicodeEmoji[] BLOCK_NUMBER = { //
			BLOCK_0, //
			BLOCK_1, //
			BLOCK_2, //
			BLOCK_3, //
			BLOCK_4, //
			BLOCK_5, //
			BLOCK_6, //
			BLOCK_7, //
			BLOCK_8, //
			BLOCK_9, //
			BLOCK_10,//
	};

	public static final UnicodeEmoji HAND_SPLAYED = fromUnicode("\uD83D\uDD90");

	public static final UnicodeEmoji SMALL_BLACK_SQUARE = fromUnicode("\u25AA");
	public static final UnicodeEmoji WHITE_CIRCLE = fromUnicode("\u26AA");
	public static final UnicodeEmoji RED_CIRCLE = fromUnicode("\uD83D\uDD34");

	public static final UnicodeEmoji BLUE_CIRCLE = fromUnicode("\uD83D\uDD35");

	public static final UnicodeEmoji SMALL_RED_DOWN_ARROW = fromUnicode("\uD83D\uDD3B");
	public static final UnicodeEmoji ARROW_UP = fromUnicode("\u2B06");
	public static final UnicodeEmoji ARROW_FORWARD = fromUnicode("\u25B6");
	public static final UnicodeEmoji ARROW_HEADING_UP = fromUnicode("\u2934");
	public static final UnicodeEmoji REWIND = fromUnicode("\u23EA");
	public static final UnicodeEmoji SHUFFLE = fromUnicode("\uD83D\uDD00");

	public static final UnicodeEmoji TRACK_NEXT = fromUnicode("\u23ED");
	public static final UnicodeEmoji QUESTION = fromUnicode("\u2753");

	public static final UnicodeEmoji BANGBANG = fromUnicode("\u203C");

	public static final UnicodeEmoji LOCK = fromUnicode("\uD83D\uDD12");
	public static final UnicodeEmoji RED_HEART = fromUnicode("\u2764");
	public static final UnicodeEmoji BLUE_HEART = fromUnicode("\uD83D\uDC99");
	public static final UnicodeEmoji GREEN_HEART = fromUnicode("\uD83D\uDC9A");

	public static final UnicodeEmoji YELLOW_HEART = fromUnicode("\uD83D\uDC9B");
	public static final UnicodeEmoji RED_BOOK = fromUnicode("\uD83D\uDCD5");
	public static final UnicodeEmoji BLUE_BOOK = fromUnicode("\uD83D\uDCD8");
	public static final UnicodeEmoji GREEN_BOOK = fromUnicode("\uD83D\uDCD7");

	public static final UnicodeEmoji ORANGE_BOOK = fromUnicode("\uD83D\uDCD9");
	public static final UnicodeEmoji RED_CAR = fromUnicode("\uD83D\uDE97");
	public static final UnicodeEmoji BLUE_CAR = fromUnicode("\uD83D\uDE99");
	public static final UnicodeEmoji TRACTOR = fromUnicode("\uD83D\uDE9C");

	public static final UnicodeEmoji TAXI = fromUnicode("\uD83D\uDE95");
	public static final UnicodeEmoji SOCCER = fromUnicode("\u26BD");
	public static final UnicodeEmoji BASKETBALL = fromUnicode("\uD83C\uDFC0");
	public static final UnicodeEmoji FOOTBALL = fromUnicode("\uD83C\uDFC8");

	public static final UnicodeEmoji BASEBALL = fromUnicode("\u26BE");
	public static final UnicodeEmoji APPLE = fromUnicode("\uD83C\uDF4E");
	public static final UnicodeEmoji TANGERINE = fromUnicode("\uD83C\uDF4A");
	public static final UnicodeEmoji PEAR = fromUnicode("\uD83C\uDF50");
	public static final UnicodeEmoji LEMON = fromUnicode("\uD83C\uDF4B");
	public static final UnicodeEmoji HAMBURGER = fromUnicode("\uD83C\uDF54");
	public static final UnicodeEmoji PIZZA = fromUnicode("\uD83C\uDF55");
	public static final UnicodeEmoji HOT_DOG = fromUnicode("\uD83C\uDF2D");
	public static final UnicodeEmoji FRIES = fromUnicode("\uD83C\uDF5F");
	public static final UnicodeEmoji ICE_CREAM = fromUnicode("\uD83C\uDF68");
	public static final UnicodeEmoji CAKE = fromUnicode("\uD83C\uDF70");
	public static final UnicodeEmoji COOKIE = fromUnicode("\uD83C\uDF6A");

	public static final UnicodeEmoji DOUGHNUT = fromUnicode("\uD83C\uDF69");
	public static final UnicodeEmoji FOX = fromUnicode("\uD83E\uDD8A");
	public static final UnicodeEmoji DOG = fromUnicode("\uD83D\uDC36");
	public static final UnicodeEmoji CAT = fromUnicode("\uD83D\uDC31");

	public static final UnicodeEmoji MOUSE = fromUnicode("\uD83D\uDC2D");
	public static final UnicodeEmoji FAMILY_MMB = fromUnicode("\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC66");
	public static final UnicodeEmoji FAMILY_MMBB = Emoji
			.fromUnicode("\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC66\u200D\uD83D\uDC66");
	public static final UnicodeEmoji CONSTRUCTION_WORKER = fromUnicode("\uD83D\uDC77");
	public static final UnicodeEmoji SANTA = fromUnicode("\uD83C\uDF85");
	public static final UnicodeEmoji OLDER_MAN = fromUnicode("\uD83D\uDC74");
	public static final UnicodeEmoji GUARDSMAN = fromUnicode("\uD83D\uDC82");

	public static final UnicodeEmoji SPY = fromUnicode("\uD83D\uDD75");

	public static final UnicodeEmoji SIGNAL_STRENGTH = fromUnicode("\uD83D\uDCF6");
	public static final UnicodeEmoji GAME_DIE = fromUnicode("\uD83C\uDFB2");
	public static final UnicodeEmoji DOLLAR = fromUnicode("\uD83D\uDCB5");
	public static final UnicodeEmoji MONEYBAG = fromUnicode("\uD83D\uDCB0");
	public static final UnicodeEmoji GEM = fromUnicode("\uD83D\uDC8E");
	public static final UnicodeEmoji SCALES = fromUnicode("\u2696");
	public static final UnicodeEmoji SKULL = fromUnicode("\uD83D\uDC80");
	public static final UnicodeEmoji MICROSCOPE = fromUnicode("\uD83D\uDD2C");
	public static final UnicodeEmoji TULIP = fromUnicode("\uD83C\uDF37");
	public static final UnicodeEmoji HAMMER_PICK = fromUnicode("\u2692");
	public static final UnicodeEmoji SHOPPING_BAGS = fromUnicode("\uD83D\uDECD");
	public static final UnicodeEmoji CRYSTAL_BALL = fromUnicode("\uD83D\uDD2E");
	public static final UnicodeEmoji CROSSED_SWORDS = fromUnicode("\u2694");
	public static final UnicodeEmoji TICKET = fromUnicode("\uD83C\uDFAB");
	public static final UnicodeEmoji SHIELD = fromUnicode("\uD83D\uDEE1");
	public static final UnicodeEmoji PACKAGE = fromUnicode("\uD83D\uDCE6");
	public static final UnicodeEmoji TOOLS = fromUnicode("\uD83D\uDEE0");
	public static final UnicodeEmoji BOW_AND_ARROW = fromUnicode("\uD83C\uDFF9");
	public static final UnicodeEmoji TOILET = fromUnicode("\uD83D\uDEBD");
	public static final UnicodeEmoji DAGGER = fromUnicode("\uD83D\uDDE1");
	public static final UnicodeEmoji BOOKS = fromUnicode("\uD83D\uDCDA");
	public static final UnicodeEmoji PICK = fromUnicode("\u26CF");
	public static final UnicodeEmoji PAINTBRUSH = fromUnicode("\uD83D\uDD8C");
	public static final UnicodeEmoji STEW = fromUnicode("\uD83C\uDF72");

	public static final UnicodeEmoji GUN = fromUnicode("\uD83D\uDD2B");
	public static final UnicodeEmoji RICE_SCENE = fromUnicode("\uD83C\uDF91");
	public static final UnicodeEmoji FOUNTAIN = fromUnicode("\u26F2");
	public static final UnicodeEmoji PARK = fromUnicode("\uD83C\uDFDE");
	public static final UnicodeEmoji HOMES = fromUnicode("\uD83C\uDFD8");
	public static final UnicodeEmoji OFFICE = fromUnicode("\uD83C\uDFE2");
	public static final UnicodeEmoji HOUSE = fromUnicode("\uD83C\uDFE0");
	public static final UnicodeEmoji CIRCUS_TENT = fromUnicode("\uD83C\uDFAA");
	public static final UnicodeEmoji CHURCH = fromUnicode("\u26EA");
	public static final UnicodeEmoji STATUE_OF_LIBERTY = fromUnicode("\uD83D\uDDFD");
	public static final UnicodeEmoji BANK = fromUnicode("\uD83C\uDFE6");
	public static final UnicodeEmoji HOUSE_ABANDONED = fromUnicode("\uD83C\uDFDA");

	public static final UnicodeEmoji CLASSICAL_BUILDING = fromUnicode("\uD83C\uDFDB");

	public static final UnicodeEmoji[][] PLAYER_SETS = { //
			{ RED_HEART, BLUE_HEART, GREEN_HEART, YELLOW_HEART }, //
			{ RED_BOOK, BLUE_BOOK, GREEN_BOOK, ORANGE_BOOK }, //
			{ RED_CAR, BLUE_CAR, TRACTOR, TAXI }, //
			{ SOCCER, BASKETBALL, FOOTBALL, BASEBALL }, //
			{ APPLE, TANGERINE, PEAR, LEMON }, //
			{ HAMBURGER, PIZZA, HOT_DOG, FRIES }, //
			{ ICE_CREAM, CAKE, COOKIE, DOUGHNUT }, //
			{ FOX, DOG, CAT, MOUSE }, //
			{ QUESTION, QUESTION, QUESTION, QUESTION },//
	};

	public static UnicodeEmoji fromUnicode(String unicode) {
		return new FixToStringEmoji(Emoji.fromUnicode(unicode));
	}

	public static UnicodeEmoji[] getRandomPlayerSet() {
		List<UnicodeEmoji> shuffleList = Arrays.asList(PLAYER_SETS[new Random().nextInt(PLAYER_SETS.length)]);
		Collections.shuffle(shuffleList);
		return shuffleList.stream().toArray(UnicodeEmoji[]::new);
	}
}
