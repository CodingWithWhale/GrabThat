package net.fabric.grabthat.registry;

import net.fabric.grabthat.GrabThat;
import net.fabric.grabthat.data.CarryData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GrabThat.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CarryData>> CARRY_DATA =
            ATTACHMENT_TYPES.register("carry_data", () ->
                    AttachmentType.<CarryData>builder(() -> CarryData.EMPTY)
                            .serialize(CarryData.CODEC)
                            .copyOnDeath()
                            .build());
}
