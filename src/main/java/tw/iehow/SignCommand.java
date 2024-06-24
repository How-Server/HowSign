package tw.iehow;

import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SignCommand {
    private static final int REQUIRED_EXPERIENCE_LEVEL = 3;
    private static final long SIGN_INTERVAL = 3600000L;
    private static final int SIGN_REWARD = 3;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sign")
            .executes(context -> {
                ServerCommandSource source = context.getSource();
                if (source.getPlayer() != null) {
                    UUID playerUUID = source.getPlayer().getUuid();
                    String playerID = source.getPlayer().getName().getString();
                    long currentTime = System.currentTimeMillis();

                    try (Connection connection = HowSign.getConnection()) {
                        PreparedStatement selectStatement = connection.prepareStatement("SELECT last_sign_time, sign_count FROM sign_data WHERE uuid = ?");
                        selectStatement.setString(1, playerUUID.toString());
                        ResultSet resultSet = selectStatement.executeQuery();

                        int signCount = 0;

                        if (resultSet.next()) {
                            long lastSignTime = resultSet.getLong("last_sign_time");
                            signCount = resultSet.getInt("sign_count");
                            long pastTime = currentTime - lastSignTime;
                            if (pastTime < SIGN_INTERVAL) {
                                source.sendMessage(Text.literal("❌ " + ((SIGN_INTERVAL - pastTime) / 60000) + " 分鐘後才可以簽到").formatted(Formatting.RED));
                                return 1;
                            }
                        }

                        if (source.getPlayer().experienceLevel < REQUIRED_EXPERIENCE_LEVEL) {
                            source.sendMessage(Text.literal("❌ 你需要三等才能完成簽到").formatted(Formatting.RED));
                            return 1;
                        }

                        source.getPlayer().addExperienceLevels(-REQUIRED_EXPERIENCE_LEVEL);

                        signCount++;

                        PreparedStatement updateStatement = connection.prepareStatement("REPLACE INTO sign_data (uuid, last_sign_time, sign_count, player_id) VALUES (?, ?, ?, ?)");
                        updateStatement.setString(1, playerUUID.toString());
                        updateStatement.setLong(2, currentTime);
                        updateStatement.setInt(3, signCount);
                        updateStatement.setString(4, playerID);
                        updateStatement.executeUpdate();
                        DatabaseManager dm = DiamondUtils.getDatabaseManager();
                        dm.changeBalance(String.valueOf(playerUUID), SIGN_REWARD);
                        source.sendMessage(Text.literal("✅ 簽到成功，已扣除三等經驗！" + SIGN_REWARD + " How已存入銀行。").formatted(Formatting.GREEN));
                    } catch (SQLException e) {
                        source.sendMessage(Text.literal("❌ 簽到異常，請聯繫管理員").formatted(Formatting.RED));
                        e.printStackTrace();
                        return 1;
                    }
                    return 1;
                }
                return 0;
            })
                .then(CommandManager.literal("remind")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    if (source.getPlayer() != null) {
                        UUID playerUUID = source.getPlayer().getUuid();
                        boolean remindEnabled = true;

                        try (Connection connection = HowSign.getConnection()) {
                            PreparedStatement selectStatement = connection.prepareStatement("SELECT remind_enabled FROM sign_data WHERE uuid = ?");
                            selectStatement.setString(1, playerUUID.toString());
                            ResultSet resultSet = selectStatement.executeQuery();

                            if (resultSet.next()) {
                                remindEnabled = resultSet.getInt("remind_enabled") == 1;
                            }

                            int newRemindSetting = remindEnabled ? 0 : 1;
                            PreparedStatement updateStatement = connection.prepareStatement("UPDATE sign_data SET remind_enabled = ? WHERE uuid = ?");
                            updateStatement.setInt(1, newRemindSetting);
                            updateStatement.setString(2, playerUUID.toString());
                            updateStatement.executeUpdate();

                            source.sendMessage(Text.literal("⚠ 提醒功能已" + (newRemindSetting == 1 ? "開啟" : "關閉") + "。").formatted(Formatting.YELLOW));
                        } catch (SQLException e) {
                            source.sendError(Text.literal("❌ 無法更新提醒設置，請聯繫管理員").formatted(Formatting.RED));
                            e.printStackTrace();
                            return 1;
                        }
                    }
                    return 0;
                })));
    }

    public static String getRemainingTime(PlayerEntity player) {
        String playerUUID = player.getUuid().toString();
        long currentTime = System.currentTimeMillis();
        try (Connection connection = HowSign.getConnection()) {
            PreparedStatement selectStatement = connection.prepareStatement("SELECT last_sign_time, remind_enabled FROM sign_data WHERE uuid = ?");
            selectStatement.setString(1, playerUUID);
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                long lastSignTime = resultSet.getLong("last_sign_time");
                long pastTime = currentTime - lastSignTime;
                if (pastTime < SIGN_INTERVAL) {
                    long remainingTime = SIGN_INTERVAL - pastTime;
                    return remainingTime / 60000 + " 分鐘後可以 /sign 簽到獲得貨幣";
                }
                if (resultSet.getInt("remind_enabled") == 1) {
                    player.sendMessage(Text.literal("立即 /sign 完成簽到獲得貨幣").formatted(Formatting.GOLD),true);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "立即 /sign 完成簽到獲得貨幣！";
    }
}