package tw.iehow.howsign;

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
import java.time.LocalDate;
import java.util.UUID;

public class SignCommand {
    private static final int REQUIRED_EXPERIENCE_LEVEL = 3;
    private static final long SIGN_INTERVAL = 3600000L;
    private static final int SIGN_REWARD = 3;
    private static final int MAX_DAILY_SIGNS = 10;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sign")
            .executes(context -> {
                ServerCommandSource source = context.getSource();
                if (source.getPlayer() != null) {
                    UUID playerUUID = source.getPlayer().getUuid();
                    String playerID = source.getPlayer().getName().getString();
                    long currentTime = System.currentTimeMillis();
                    LocalDate currentDate = LocalDate.now();

                    try (Connection connection = HowSign.getConnection()) {
                        PreparedStatement selectStatement = connection.prepareStatement("SELECT last_sign_time, sign_count, last_sign_date, daily_sign_count FROM sign_data WHERE uuid = ?");
                        selectStatement.setString(1, playerUUID.toString());
                        ResultSet resultSet = selectStatement.executeQuery();

                        int signCount = 0;
                        int dailySignCount = 0;
                        String lastSignDate = "";

                        if (resultSet.next()) {
                            long lastSignTime = resultSet.getLong("last_sign_time");
                            signCount = resultSet.getInt("sign_count");
                            lastSignDate = resultSet.getString("last_sign_date");
                            dailySignCount = resultSet.getInt("daily_sign_count");
                            long pastTime = currentTime - lastSignTime;

                            if (!lastSignDate.equals(currentDate.toString())) {
                                dailySignCount = 0;
                            }

                            if (pastTime < SIGN_INTERVAL) {
                                source.sendMessage(Text.literal("❌ " + ((SIGN_INTERVAL - pastTime) / 60000) + " 分鐘後才可以簽到").formatted(Formatting.RED));
                                return 1;
                            }
                        }

                        if (dailySignCount >= MAX_DAILY_SIGNS) {
                            source.sendMessage(Text.literal("❌ 今天的簽到已達到上限 " + MAX_DAILY_SIGNS + " 次").formatted(Formatting.RED));
                            return 1;
                        }

                        if (source.getPlayer().experienceLevel < REQUIRED_EXPERIENCE_LEVEL) {
                            source.sendMessage(Text.literal("❌ 你需要 " + REQUIRED_EXPERIENCE_LEVEL + " 等才能完成簽到").formatted(Formatting.RED));
                            return 1;
                        }

                        source.getPlayer().addExperienceLevels(-REQUIRED_EXPERIENCE_LEVEL);

                        signCount++;
                        dailySignCount++;

                        PreparedStatement updateStatement = connection.prepareStatement("REPLACE INTO sign_data (uuid, last_sign_time, sign_count, player_id, last_sign_date, daily_sign_count) VALUES (?, ?, ?, ?, ?, ?)");
                        updateStatement.setString(1, playerUUID.toString());
                        updateStatement.setLong(2, currentTime);
                        updateStatement.setInt(3, signCount);
                        updateStatement.setString(4, playerID);
                        updateStatement.setString(5, currentDate.toString());
                        updateStatement.setInt(6, dailySignCount);
                        updateStatement.executeUpdate();

                        DatabaseManager dm = DiamondUtils.getDatabaseManager();
                        dm.changeBalance(String.valueOf(playerUUID), SIGN_REWARD);
                        source.sendMessage(Text.literal("✅ 簽到成功，已扣除三等經驗！" + SIGN_REWARD + " How已存入銀行").formatted(Formatting.GREEN));
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

                                source.sendMessage(Text.literal("⚠ 提醒功能已" + (newRemindSetting == 1 ? "開啟" : "關閉")).formatted(Formatting.YELLOW));
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
            PreparedStatement selectStatement = connection.prepareStatement("SELECT last_sign_time, remind_enabled, , last_sign_date, daily_sign_count FROM sign_data WHERE uuid = ?");
            selectStatement.setString(1, playerUUID);
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                long lastSignTime = resultSet.getLong("last_sign_time");
                int dailySignCount = resultSet.getInt("daily_sign_count");
                long pastTime = currentTime - lastSignTime;
                if (pastTime < SIGN_INTERVAL) {
                    long remainingTime = SIGN_INTERVAL - pastTime;
                    return remainingTime / 60000 + " 分鐘後可以 /sign 簽到獲得貨幣";
                }
                if (dailySignCount >= MAX_DAILY_SIGNS && resultSet.getString("last_sign_date").equals(LocalDate.now().toString())) {
                    return "今天的簽到已達到上限 " + MAX_DAILY_SIGNS + " 次";
                }

                if (player.experienceLevel < REQUIRED_EXPERIENCE_LEVEL) {
                    return "你需要 " + REQUIRED_EXPERIENCE_LEVEL + " 等才能完成簽到";
                }

                if (resultSet.getInt("remind_enabled") == 1) {
                    player.sendMessage(Text.literal("立即 /sign 完成簽到獲得貨幣").formatted(Formatting.GOLD), true);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "立即 /sign 完成簽到獲得貨幣！";
    }
}