package org.geysermc.geyser.entity.type.display;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import java.util.UUID;

public class ItemDisplayEntity extends DisplayEntity {
    public ItemDisplayEntity(GeyserSession session, int entityId, long geyserId, UUID uuid,
            EntityDefinition<?> definition,
            Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setDisplayedItem(EntityMetadata<ItemStack, ?> entityMetadata) {
        ItemData item = ItemTranslator.translateToBedrock(session, entityMetadata.getValue());
        this.hand = item;
        updateMainHand(session);
    }

    public void setDisplayType(ByteEntityMetadata entityMetadata) {

    }

}
