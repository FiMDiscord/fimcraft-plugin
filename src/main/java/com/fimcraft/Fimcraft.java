package com.fimcraft;

import com.fimcraft.commands.CommandVerify;
import com.fimcraft.DatabaseHandler;
import com.fimcraft.db.tables.records.UsersRecord;
import com.fimcraft.db.tables.records.VerificationRecord;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.commons.lang.text.StrSubstitutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Fimcraft extends JavaPlugin {
    public boolean enabled = true;
    public JDA bot;
    public DatabaseHandler db;
    public String prefix;

    private FileConfiguration setupConfig() {
        FileConfiguration config = this.getConfig();
        config.addDefault("botToken", "TOKEN");
        config.addDefault("prefix", "%");
        config.addDefault("verifyDiscordMessage", "You are now verified as %s on the Fimcraft server!");
        config.addDefault("verifyMinecraftMessage", "&6%s&a is now verified.");
        config.addDefault("verifyCommandMessage", "&5Send the command &6&l&n${prefix}verify ${token}&r&5 in the discord &o&d#bot-spam&r&5 channel");
        config.addDefault("alreadyVerifiedMessage", "&4You're already verified!");
        config.addDefault("verifiedRole", "Fimcraft Verified");
        config.options().copyDefaults(true);
        saveConfig();
        prefix = config.getString("prefix");
        return config;
    }

    private void setupDatabase() {
        db = new DatabaseHandler();
    }

    private Role verifiedRole(GenericEvent event, FileConfiguration config) {
        try {
            return event.getJDA().getRolesByName(config.getString("verifiedRole"), true).get(0);
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    @Override
    public void onEnable() {
        setupDatabase();

        FileConfiguration config = setupConfig();
        getLogger().info("Connecting to discord");
        if (config.get("botToken").equals("TOKEN")) {
            enabled = false;
        }
        if (enabled) {
            // CONNECT TO DISCORD
            JDABuilder builder = JDABuilder.createDefault(config.getString("botToken"));
            builder.setMemberCachePolicy(MemberCachePolicy.NONE);
            builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.EMOTE, CacheFlag.MEMBER_OVERRIDES);
            builder.addEventListeners(new EventListener() {
                public void onEvent(@Nonnull GenericEvent event) {
                    if (event instanceof ReadyEvent) {
                        getLogger().info("Connected to discord");
                        Presence p = bot.getPresence();
                        p.setStatus(OnlineStatus.ONLINE);
                    }
                }
            });
            builder.addEventListeners(new EventListener() {
                @Override
                public void onEvent(@Nonnull GenericEvent event) {
                    if (event instanceof MessageReceivedEvent) {
                        MessageReceivedEvent e = (MessageReceivedEvent) event;
                        if (!e.getAuthor().isBot()) {
                            String msg = e.getMessage().getContentStripped();
                            if (msg.startsWith("%")) {
                                if (msg.toLowerCase().matches(prefix + "verify\\s\\w{5}")) {
                                    String[] args = msg.split(" ");
                                    if (args.length > 1) {
                                        VerificationRecord rec = db.verifyCheckCode(args[1]);
                                        System.out.println(rec);
                                        if (rec != null) {
                                            UsersRecord user = db.finishAuthentication(rec, e.getAuthor().getId());
                                            e.getMessage().delete().queue();
                                            e.getChannel().sendMessage(String.format(config.getString("verifyDiscordMessage"), user.getUsername())).queue();
                                            e.getGuild().addRoleToMember(user.getDiscordid(), verifiedRole(event, config)).queue();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
            try {
                bot = builder.build();
            } catch (LoginException e) {
                enabled = false;
                getLogger().warning("Discord failed to connect, invalid token");
            }


            // REGISTER COMMANDS
            this.getCommand("verify").setExecutor(
                    new CommandVerify(config, db)
            );
        }
    }

    @Override
    public void onDisable() {
        if (enabled) {
            Presence p = bot.getPresence();
            p.setStatus(OnlineStatus.OFFLINE);
        }
        bot.shutdown();
        bot = null;
    }
}
