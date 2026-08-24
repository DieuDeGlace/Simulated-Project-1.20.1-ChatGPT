package dev.ryanhcode.offroad.neoforge;


import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.data.OffroadTags;
import dev.ryanhcode.offroad.events.OffroadCommonEvents;
import dev.ryanhcode.offroad.neoforge.data.OffroadDatagen;
import dev.ryanhcode.offroad.neoforge.service.NeoForgeOffroadConfigService;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Offroad.MOD_ID)
public class OffroadNeoForge {
    public OffroadNeoForge() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        this.modBusRegistry(modBus);
        this.listenCommonEvents(MinecraftForge.EVENT_BUS);

        Offroad.init();

        NeoForgeOffroadConfigService.register();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            new OffroadNeoForgeClient(modBus);
        }
    }

    private void listenCommonEvents(final IEventBus eventBus) {
        eventBus.addListener((final TickEvent.LevelTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                OffroadCommonEvents.tickLevelEvent(event.level);
            }
        });
    }

    private void modBusRegistry(final IEventBus modBus) {
        modBus.register(NeoForgeOffroadConfigService.class);

        modBus.addListener(OffroadNeoForge::init);
        modBus.addListener(EventPriority.HIGHEST, OffroadDatagen::gatherDataHighPriority);
        modBus.addListener(EventPriority.LOWEST, OffroadDatagen::gatherData);
        modBus.addListener(OffroadDatagen::registerEvent);

        Offroad.getRegistrate().registerEventListeners(modBus);
    }

    private static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(OffroadDatagen::registerCommonContent);
    }
}
