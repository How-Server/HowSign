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
                source.sendMessage(Text.literal("您的驗證公鑰已過期，請重新登入（若您使用EasyMC，請購買正版帳號）").formatted(Formatting.YELLOW));
                ci.cancel();
            }
        }
    }
}