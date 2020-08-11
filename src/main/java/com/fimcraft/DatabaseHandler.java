package com.fimcraft;

import com.fimcraft.db.tables.Users;
import com.fimcraft.db.tables.records.UsersRecord;
import com.fimcraft.db.tables.records.VerificationRecord;
import org.bukkit.entity.Player;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import static com.fimcraft.db.Tables.*;
import static org.jooq.impl.DSL.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLData;
import java.sql.SQLException;

public class DatabaseHandler {
    protected Connection con;
    protected DSLContext context;
    private final String dbName = "fimcraft.db";

    public DatabaseHandler() {
        String url = "jdbc:sqlite:" + dbName;
        try {
            File file = new File("./" + dbName);
            file.getParentFile().mkdirs(); // Will create parent directories if not exists
            file.createNewFile();
            con = DriverManager.getConnection(url);
            context = DSL.using(con, SQLDialect.SQLITE);
            createTables();
        }
        catch(SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createTables() {
        System.out.println(context.createTableIfNotExists(VERIFICATION).getSQL());
        context.createTableIfNotExists(VERIFICATION)
                .column("uuid", SQLDataType.INTEGER)
                .column("key", SQLDataType.NVARCHAR)
                .constraints(
                        primaryKey("uuid"),
                        unique("uuid"),
                        unique("key")
                ).execute();
        context.createTableIfNotExists(USERS)
                .column("uuid", SQLDataType.NVARCHAR)
                .column("discordID", SQLDataType.NVARCHAR)
                .column("username", SQLDataType.NVARCHAR)
                .column("verified", SQLDataType.INTEGER)
                .constraints(
                        primaryKey("uuid"),
                        unique("uuid"),
                        unique("discordID")
                ).execute();
    }

    public UsersRecord getUserByPlayer(Player player) {
        UsersRecord res = context.selectFrom(USERS).where(USERS.UUID.like(player.getUniqueId().toString())).fetchOne();
        if(res == null) {
            res = context.newRecord(USERS);
            res.setUsername(player.getDisplayName());
            res.setUuid(player.getUniqueId().toString());
            res.setVerified(false);
            res.store();
        }
        return res;
    }

    public UsersRecord getUserByDiscordID(String id) {
        UsersRecord res = context.selectFrom(USERS).where(USERS.DISCORDID.like(id)).fetchOne();
        return res;
    }

    public UsersRecord getUserByUsername(String username) {
        UsersRecord res = context.selectFrom(USERS).where(upper(USERS.USERNAME).eq(upper(username))).fetchOne();
        return res;
    }

    public void deleteVerificationCode(String code) {
        context.deleteFrom(VERIFICATION).where(upper(VERIFICATION.KEY).eq(upper(code))).execute();
    }

    public String startAuthentication(String uuid) {

        String code = Verification.generateVerificationToken(5);
        int count = context.selectCount().from(VERIFICATION).where(VERIFICATION.UUID.eq(uuid)).fetchOne(0, int.class);
        if(count == 0) {
            context.insertInto(VERIFICATION, VERIFICATION.KEY, VERIFICATION.UUID).values(code, uuid).execute();
        } else {
            VerificationRecord rec = context.selectFrom(VERIFICATION).where(VERIFICATION.UUID.eq(uuid)).fetchOne();
            rec.setKey(code);
            rec.update();
        }
        return code;
    }

    public UsersRecord finishAuthentication(VerificationRecord record, String discordID) {
        UsersRecord user = context.selectFrom(USERS).where(USERS.UUID.eq(record.getUuid())).fetchOne();
        user.setDiscordid(discordID);
        user.setVerified(true);
        user.update();
        record.delete();
        return user;
    }

    public VerificationRecord verifyCheckCode(String code) {
        VerificationRecord rec = context.selectFrom(VERIFICATION).where(upper(VERIFICATION.KEY).eq(upper(code))).fetchOne();
        return rec;
    }

}
