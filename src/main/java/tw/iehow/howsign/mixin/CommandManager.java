package tw.iehow.howsign.mixin;

import com.mojang.brigadier.ParseResults;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.server.command.CommandManager.class)
public abstract class CommandManager {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    public void tradeWithVillager(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci){
        Entity source = parseResults.getContext().getSource().getEntity();
        if (source instanceof ServerPlayerEntity){
            try {
                boolean expired = ((ServerPlayerEntity) source).getSession().isKeyExpired();
            }catch (Exception e){
                ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) source;
                serverPlayerEntity.sendMessage(Text.literal("您的驗證公鑰已過期，請重新登入（依然出現本錯誤，請參考下方解決方式）").formatted(Formatting.YELLOW), false);
                serverPlayerEntity.sendMessage(Text.literal("1. 若有安裝 No Chat Reports 模組，請將其移除").formatted(Formatting.WHITE), false);
                serverPlayerEntity.sendMessage(Text.literal("2. 若使用 EasyMC 等非正規來源的帳號，請購買正版帳號").formatted(Formatting.WHITE), false);

                ci.cancel();
            }
        }
    }
}