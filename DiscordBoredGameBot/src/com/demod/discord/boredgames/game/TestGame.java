package com.demod.discord.boredgames.game;

import com.demod.discord.boredgames.Emojis;
import com.demod.discord.boredgames.Game;

import net.dv8tion.jda.core.entities.Member;

public class TestGame extends Game {

	@Override
	protected String getTitle() {
		return "Test Game";
	}

	private Member getVolunteer() {
		return this.<Member>displayChannel(embed -> {
			embed.setDescription("Raise your hand for testing!");
		}).addAction(Emojis.HAND_SPLAYED, p -> p).send();
	}

	private boolean privateHighFive(Member player) {
		displayChannel(embed -> {
			embed.setDescription("Waiting for a response from **" + player.getEffectiveName() + "**...");
		}).send();
		boolean result = this.<Boolean>displayPrivate(player, embed -> {
			embed.setDescription("Give me a high five! " + Emojis.HAND_SPLAYED);
		})//
				.addAction(Emojis.HAND_SPLAYED, true)//
				.addAction(Emojis.TRACK_NEXT, false)//
				.send();
		displayPrivate(player, embed -> {
			embed.setDescription("Thank you for the response!");
		}).send();
		return result;
	}

	@Override
	public void run() {
		Member player = getVolunteer();
		boolean highFived = privateHighFive(player);
		displayChannel(embed -> {
			if (highFived) {
				embed.setDescription("Rightous! " + Emojis.HAND_SPLAYED);
			} else {
				embed.setDescription("Aww man...");
			}
		}).send();
	}

}
