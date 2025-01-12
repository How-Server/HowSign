package tw.iehow.howsign;

import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
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

        for (int i = 0; i < amount; i++) {
            switch (type) {
                case "v1000":
                    apply(uuid, "vip1000", 128, 10000);
                    break;
                case "v500":
                    apply(uuid, "vip500", 64, 5000);
                    break;
                case "v200":
                    apply(uuid, "vip200", 0, 0);
                    break;
                default:
                    source.sendMessage(Text.literal("❌ 未知的會員類型").formatted(Formatting.RED));
                    return 0;
            }
        }
        source.sendMessage(Text.literal("✔ " + playerID + " 已獲得 " + type + " 會員 " + amount + " 期").formatted(Formatting.GREEN));
        return 1;
    }

    private static void apply(String uuid, String vipType, int money, int claimBlocks) {
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        dm.changeBalance(uuid, money);

        int blocks = DataManager.getUserData(UUID.fromString(uuid)).blocks();
        int newAmount = MathHelper.clamp(blocks + claimBlocks, 0, Integer.MAX_VALUE);
        DataManager.updateUserData(UUID.fromString(uuid)).setBlocks(newAmount);


        UserManager userManager = LuckPermsProvider.get().getUserManager();
        CompletableFuture<User> userFuture = userManager.loadUser(UUID.fromString(uuid));

        userFuture.thenAcceptAsync(user -> {
            long expiryDuration = 0;
            for (Node node : user.getNodes()) {
                if (node.getKey().equals("group."+vipType) && node.hasExpiry()) {
                    expiryDuration = node.getExpiryDuration().toMinutes();
                    user.data().remove(node);
                }
            }
            if (!vipType.equals("vip200")) user.data().add(InheritanceNode.builder("vipoutdated").build());
            user.data().add(InheritanceNode.builder(vipType).expiry(43500 + expiryDuration, TimeUnit.MINUTES).build());
            LuckPermsProvider.get().getUserManager().saveUser(user);
        });
    }
}