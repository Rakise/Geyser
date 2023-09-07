package org.geysermc.geyser.entity.type.display;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;

public class BlockDisplayEntity extends SlotDisplayEntity {

    public BlockDisplayEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition,
            Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setDisplayedBlockState(IntEntityMetadata blockState) {
        
    }
}
