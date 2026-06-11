package net.cnjgaming.mobcatcher.util;

import net.cnjgaming.mobcatcher.CNJMobCatcherPlugin;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

	public static final class Keys {
		public static final String WORLD_BLOCKED_CAPTURE = "messages.world-blocked-capture";
		public static final String WORLD_BLOCKED_RELEASE = "messages.world-blocked-release";
		public static final String NO_USES = "messages.no-uses";
		public static final String BLOCKED_MOB = "messages.blocked-mob";
		public static final String CATCHER_FULL = "messages.catcher-full";
		public static final String SAME_STACK_ONLY = "messages.same-stack-only";
		public static final String CAPTURED = "messages.captured";
		public static final String CAPTURED_EGG = "messages.captured-egg";
		public static final String RELEASED = "messages.released";
		public static final String CLAIM_BLOCKED_RELEASE = "messages.claim-blocked-release";
		public static final String FAILED_RELEASE = "messages.failed-release";
		public static final String INVALID_DATA = "messages.invalid-data";
		public static final String INVALID_EGG_DATA = "messages.invalid-egg-data";

		public static final String CMD_NO_PERMISSION = "messages.commands.no-permission";
		public static final String CMD_USAGE_HEADER = "messages.commands.usage-header";
		public static final String CMD_RELOAD_SUCCESS = "messages.commands.reload-success";
		public static final String CMD_RELOAD_FAILED = "messages.commands.reload-failed";
		public static final String CMD_GIVE_USAGE = "messages.commands.give-usage";
		public static final String CMD_PLAYER_NOT_FOUND = "messages.commands.player-not-found";
		public static final String CMD_INVALID_USES_NUMBER = "messages.commands.invalid-uses-number";
		public static final String CMD_INVALID_USES_VALUE = "messages.commands.invalid-uses-value";
		public static final String CMD_GIVE_SUCCESS = "messages.commands.give-success";
		public static final String CMD_UNKNOWN_SUBCOMMAND = "messages.commands.unknown-subcommand";

		private Keys() {
		}
	}

	private MessageUtil() {
	}

	public static String color(String text) {
		return text == null ? "" : text.replace("&", "§");
	}

	public static String prettyLabel(String input) {
		if (input == null || input.isBlank()) {
			return "";
		}

		String[] parts = input.toLowerCase().split("_");
		StringBuilder out = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
		}
		return out.toString().trim();
	}

	public static void send(CNJMobCatcherPlugin plugin, CommandSender sender, String path, String fallback, String... replacements) {
		String prefix = plugin.getConfig().getString("messages.prefix", "");
		String msg = plugin.getConfig().getString(path, fallback);

		if (msg == null) {
			msg = fallback;
		}

		for (int i = 0; i + 1 < replacements.length; i += 2) {
			String key = replacements[i];
			String value = replacements[i + 1];
			if (key != null) {
				msg = msg.replace(key, value == null ? "" : value);
			}
		}

		sender.sendMessage(color(prefix + msg));
	}

	public static void sendRaw(CNJMobCatcherPlugin plugin, CommandSender sender, String message) {
		String prefix = plugin.getConfig().getString("messages.prefix", "");
		sender.sendMessage(color(prefix + (message == null ? "" : message)));
	}
}
