package tw.iehow;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;


import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class HowSign implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("HowItem");
	public static Connection connection;

	@Override
	public void onInitialize() {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:howsign.db");
			Statement statement = connection.createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS sign_data (uuid TEXT PRIMARY KEY, last_sign_time INTEGER, sign_count INTEGER DEFAULT 0, player_id TEXT, remind_enabled INTEGER DEFAULT 1, last_sign_date TEXT DEFAULT '', daily_sign_count INTEGER DEFAULT 0)");
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			SignCommand.register(dispatcher);
		});

		Placeholders.register(Identifier.of("how", "sign"), (ctx, arg) -> {
			if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
			return PlaceholderResult.value(SignCommand.getRemainingTime(ctx.player()));
		});
		LOGGER.info("Load Successfully.");
	}
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:sqlite:howsign.db");
	}

}