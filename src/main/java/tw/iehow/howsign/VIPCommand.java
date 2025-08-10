package tw.iehow.howsign;

import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import com.gmail.sneakdevs.diamondeconomy.sql.TransactionType;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.drex.itsours.data.DataManager;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class VIPCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("vip").requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.argument("playerID", GameProfileArgumentType.gameProfile())
                        .then(CommandManager.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("v1000").suggest("v500").suggest("v200");
                                    return builder.buildFuture();
                                })
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> execute(
                                                context.getSource(),
                                                GameProfileArgumentType.getProfileArgument(context, "playerID"),
                                                StringArgumentType.getString(context, "type"),
                                                IntegerArgumentType.getInteger(context, "amount"))
                                        ))
                        )
                )
        );
    }

    private static int execute(ServerCommandSource source, Collection<GameProfile> players, String type, int amount) {
        if (amount > 12){
            source.sendMessage(Text.literal("❌ 一次最多只能新增 12 期").formatted(Formatting.RED));
            return 0;
        }
        String uuid = null;
        String playerID = null;
        for (GameProfile player : players) {
            uuid = player.getId().toString();
            playerID = player.getName();
        }

        switch (type) {
            case "v1000":
                apply(uuid, "vip1000", amount, 128 * amount, 10000 * amount);
                break;
            case "v500":
                apply(uuid, "vip500", amount, 64 * amount, 5000 * amount);
                break;
            case "v200":
                apply(uuid, "vip200", amount, 0, 0);
                break;
            default:
                source.sendMessage(Text.literal("❌ 未知的會員類型").formatted(Formatting.RED));
                return 0;
        }
        source.sendMessage(Text.literal("✔ " + playerID + " 已獲得 " + type + " 會員 " + amount + " 期").formatted(Formatting.GREEN));
        return 1;
    }

    private static void apply(String uuid, String vipType, int amount, int totalMoney, int totalClaimBlocks) {
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        dm.changeBalance(uuid, TransactionType.INCOME, totalMoney, "會員 " + vipType + " " + amount + " 期");

        long blocks = DataManager.getUserData(UUID.fromString(uuid)).blocks();
        long newAmount = MathHelper.clamp(blocks + totalClaimBlocks, 0, Integer.MAX_VALUE);
        DataManager.updateUserData(UUID.fromString(uuid)).setBlocks(newAmount);

        UserManager userManager = LuckPermsProvider.get().getUserManager();
        CompletableFuture<User> userFuture = userManager.loadUser(UUID.fromString(uuid));

        userFuture.thenAcceptAsync(user -> {
            long existingExpiryMinutes = 0;
            for (Node node : user.getNodes()) {
                if (node.getKey().equals("group." + vipType) && node.hasExpiry()) {
                    existingExpiryMinutes = node.getExpiryDuration().toMinutes();
                    user.data().remove(node);
                }
            }

            if (!vipType.equals("vip200")) {
                user.data().add(InheritanceNode.builder("vipoutdated").build());
            }

            long addedMinutes = amount * 43500L;
            user.data().add(
                    InheritanceNode.builder(vipType)
                            .expiry(addedMinutes + existingExpiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
            LuckPermsProvider.get().getUserManager().saveUser(user);
        });
    }
}