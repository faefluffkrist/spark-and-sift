package com.faefluffkrist.siftinggravel.advancement;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class SiftSparkCriterion extends SimpleCriterionTrigger<SiftSparkCriterion.Conditions> {
    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, conditions -> true);
    }

    public record Conditions(Optional<ContextAwarePredicate> player) implements SimpleInstance {
        public static final Codec<Conditions> CODEC = ContextAwarePredicate.CODEC
                .optionalFieldOf("player")
                .xmap(Conditions::new, Conditions::player)
                .codec();
    }
}
