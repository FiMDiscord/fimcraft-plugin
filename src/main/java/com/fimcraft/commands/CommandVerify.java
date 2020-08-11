package com.fimcraft.commands;

import com.fimcraft.DatabaseHandler;
import com.fimcraft.Util;
import com.fimcraft.db.tables.records.UsersRecord;
import org.apache.commons.lang.text.StrSubstitutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CommandVerify implements CommandExecutor {

    public String prefix;
    public String verifyMessage;
    public String alreadyMessage;
    public DatabaseHandler db;

    public CommandVerify(FileConfiguration config, DatabaseHandler db) {
        this.prefix = config.getString("prefix");
        this.verifyMessage = config.getString("verifyCommandMessage");
        this.alreadyMessage = config.getString("alreadyVerifiedMessage");
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            UsersRecord u = db.getUserByPlayer((Player)sender);
            if(!u.getVerified()) {
                System.out.println("Creating verification code");
                String token = db.startAuthentication(u.getUuid());
                System.out.println("Verification code: " + token);
                Map<String, String> values = new HashMap<String, String>();
                values.put("token", token);
                values.put("prefix", prefix);
                StrSubstitutor sub = new StrSubstitutor(values, "${", "}");
                ((Player) sender).sendMessage(Util.chatColor(sub.replace(verifyMessage)));
            } else {
                ((Player) sender).sendMessage(Util.chatColor(alreadyMessage));
            }
            return true;
        }
        return false;
    }
}
