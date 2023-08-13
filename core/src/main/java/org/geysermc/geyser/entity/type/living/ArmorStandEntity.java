/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.entity.type.living;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.MathUtils;

import java.util.Optional;
import java.util.UUID;

public class ArmorStandEntity extends LivingEntity {

    // These are used to store the state of the armour stand for use when handling invisibility
    @Getter
    private boolean isMarker = false;
    private boolean isInvisible = false;
    @Getter
    private boolean isSmall = false;

    private boolean isNameTagVisible = false;

    /**
     * On Java Edition, armor stands always show their name. Invisibility hides the name on Bedrock.
     * By having a second entity, we can allow an invisible entity with the name tag.
     * (This lets armor on armor stands still show)
     */
    private ArmorStandEntity secondEntity = null;
    /**
     * Whether this is the primary armor stand that holds the armor and not the name tag.
     */
    private boolean primaryEntity = true;
    /**
     * Whether the entity's position must be updated to included the offset.
     *
     * This should be true when the Java server marks the armor stand as invisible, but we shrink the entity
     * to allow the nametag to appear. Basically:
     * - Is visible: this is irrelevant (false)
     * - Has armor, no name: false
     * - Has armor, has name: false, with a second entity
     * - No armor, no name: false
     * - No armor, yes name: true
     */
    private boolean positionRequiresOffset = false;
    /**
     * Whether we should update the position of this armor stand after metadata updates.
     */
    private boolean positionUpdateRequired = false;

    public ArmorStandEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    public void spawnEntity() {
        Vector3f javaPosition = position;
        // Apply the offset if we're the second entity
        position = position.up(getYOffset());
        super.spawnEntity();
        position = javaPosition;
    }

    @Override
    public boolean despawnEntity() {
        if (secondEntity != null) {
            secondEntity.despawnEntity();
        }
        return super.despawnEntity();
    }

    @Override
    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        moveAbsolute(position.add(relX, relY, relZ), yaw, pitch, headYaw, onGround, false);
    }

    @Override
    public void moveAbsolute(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        if (secondEntity != null) {
            secondEntity.moveAbsolute(position, yaw, pitch, headYaw, isOnGround, teleported);
        }
        // Fake the height to be above where it is so the nametag appears in the right location
        float yOffset = getYOffset();
        super.moveAbsolute(yOffset != 0 ? position.up(yOffset) : position , yaw, yaw, yaw, isOnGround, teleported);
        this.position = position;
    }

    @Override
    public void setDisplayName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        super.setDisplayName(entityMetadata);
        updateSecondEntityStatus(false);
    }

    public void setArmorStandFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        boolean offsetChanged = false;
        // isSmall
        boolean newIsSmall = (xd & 0x01) == 0x01;
        if (newIsSmall != isSmall) {
            isSmall = newIsSmall;
            offsetChanged = true;
            // Update the passenger offset as the armor stand's height has changed
            updatePassengerOffsets();
        }

        // setMarker
        boolean oldIsMarker = isMarker;
        isMarker = (xd & 0x10) == 0x10;
        if (oldIsMarker != isMarker) {
            if (isMarker) {
                setBoundingBoxWidth(0.0f);
                setBoundingBoxHeight(0.0f);
            } else {
                setBoundingBoxWidth(definition.width());
                setBoundingBoxHeight(definition.height());
            }

            updateMountOffset();
            offsetChanged = true;
        }

        if (offsetChanged) {
            if (positionRequiresOffset) {
                positionUpdateRequired = true;
            } else if (secondEntity != null) {
                secondEntity.positionUpdateRequired = true;
            }
            updateSecondEntityStatus(false);
        }

        // The following values don't do anything on normal Bedrock.
        // But if given a resource pack, then we can use these values to control armor stand visual properties
        propertyManager.add("geyser:arms", (xd & 0x04) == 0x04);
        propertyManager.add("geyser:no_bp", (xd & 0x08) == 0x08);
        propertyManager.add("geyser:small", isSmall);
        updateBedrockEntityProperties();
    }

    public void setHeadRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate("geyser:he_rx", "geyser:he_ry", "geyser:he_rz", entityMetadata.getValue());
    }

    public void setBodyRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate("geyser:bo_rx", "geyser:bo_ry", "geyser:bo_rz", entityMetadata.getValue());
    }

    public void setLeftArmRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate("geyser:la_rx", "geyser:la_ry", "geyser:la_rz", entityMetadata.getValue());
    }

    public void setRightArmRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate("geyser:ra_rx", "geyser:ra_ry", "geyser:ra_rz", entityMetadata.getValue());
    }

    public void setLeftLegRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate("geyser:ll_rx", "geyser:ll_ry", "geyser:ll_rz", entityMetadata.getValue());
    }

    public void setRightLegRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate("geyser:rl_rx", "geyser:rl_ry", "geyser:rl_rz", entityMetadata.getValue());
    }

    /**
     * Update the rotation properties for GeyserOptionalPack.
     * 
     * @param xProp the x property to update 
     * @param yProp the y property to update
     * @param zProp the z property to update
     * @param rotation the rotation of the armor stand
     */
    private void onRotationUpdate(String xProp, String yProp, String zProp, Vector3f rotation) {
        propertyManager.add(xProp, MathUtils.wrapDegrees(rotation.getX()));
        propertyManager.add(yProp, MathUtils.wrapDegrees(rotation.getY()));
        propertyManager.add(zProp, MathUtils.wrapDegrees(rotation.getZ()));
        updateBedrockEntityProperties();
    }

    @Override
    public void updateBedrockMetadata() {
        if (secondEntity != null) {
            secondEntity.updateBedrockMetadata();
        }
        super.updateBedrockMetadata();
        if (positionUpdateRequired) {
            positionUpdateRequired = false;
            moveAbsolute(position, yaw, pitch, headYaw, onGround, true);
        }
    }

    @Override
    public void updateBedrockEntityProperties() {
        if (secondEntity != null) {
            if (propertyManager.hasFloatProperties() || propertyManager.hasIntProperties()) {
                SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
                entityDataPacket.setRuntimeEntityId(secondEntity.geyserId);
                entityDataPacket.getProperties().getIntProperties().addAll(propertyManager.intProperties());
                entityDataPacket.getProperties().getFloatProperties().addAll(propertyManager.floatProperties());
                session.sendUpstreamPacket(entityDataPacket);
            }
        }
        super.updateBedrockEntityProperties();
    }

    @Override
    protected void setInvisible(boolean value) {
        // Check if the armour stand is invisible and store accordingly
        if (primaryEntity) {
            isInvisible = value;
            updateSecondEntityStatus(false);
        }
    }

    @Override
    public InteractionResult interactAt(Hand hand) {
        if (!isMarker && session.getPlayerInventory().getItemInHand(hand).asItem() != Items.NAME_TAG) {
            // Java Edition returns SUCCESS if in spectator mode, but this is overrided with an earlier check on the client
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void setHelmet(ItemData helmet) {
        super.setHelmet(helmet);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setChestplate(ItemData chestplate) {
        super.setChestplate(chestplate);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setLeggings(ItemData leggings) {
        super.setLeggings(leggings);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setBoots(ItemData boots) {
        super.setBoots(boots);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setHand(ItemData hand) {
        super.setHand(hand);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setOffHand(ItemData offHand) {
        super.setOffHand(offHand);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setDisplayNameVisible(BooleanEntityMetadata entityMetadata) {
        super.setDisplayNameVisible(entityMetadata);
        isNameTagVisible = entityMetadata.getPrimitiveValue();
        updateSecondEntityStatus(false);
    }

    /**
     * Determine if we need to load or unload the second entity.
     *
     * @param sendMetadata whether to send a metadata update after a change.
     */
    private void updateSecondEntityStatus(boolean sendMetadata) {
        // A secondary entity always has to have the offset applied, so it remains invisible and the nametag shows.
        if (!primaryEntity) return;
        if (!isInvisible) {
            // The armor stand isn't invisible. We good.
            setFlag(EntityFlag.INVISIBLE, false);
            dirtyMetadata.put(EntityDataTypes.SCALE, getScale());
            updateOffsetRequirement(false);

            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
            if (sendMetadata) {
                this.updateBedrockMetadata();
            }
            return;
        }
        boolean isNametagEmpty = nametag.isEmpty();
        if (!isNametagEmpty && (!helmet.equals(ItemData.AIR) || !chestplate.equals(ItemData.AIR) || !leggings.equals(ItemData.AIR)
                || !boots.equals(ItemData.AIR) || !hand.equals(ItemData.AIR) || !offHand.equals(ItemData.AIR))) {
            // Reset scale of the proper armor stand
            this.dirtyMetadata.put(EntityDataTypes.SCALE, getScale());
            // Set the proper armor stand to invisible to show armor
            setFlag(EntityFlag.INVISIBLE, true);
            // Update the position of the armor stand
            updateOffsetRequirement(false);

            if (secondEntity == null) {
                // Create the second entity. It doesn't need to worry about the items, but it does need to worry about
                // the metadata as it will hold the name tag.
                secondEntity = new ArmorStandEntity(session, 0, session.getEntityCache().getNextEntityId().incrementAndGet(), null,
                        EntityDefinitions.ARMOR_STAND, position, motion, getYaw(), getPitch(), getHeadYaw());
                secondEntity.primaryEntity = false;
            }
            // Copy metadata
            secondEntity.isSmall = isSmall;
            secondEntity.isMarker = isMarker;
            secondEntity.positionRequiresOffset = true; // Offset should always be applied
            secondEntity.getDirtyMetadata().put(EntityDataTypes.NAME, nametag);
            secondEntity.getDirtyMetadata().put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, isNameTagVisible ? (byte) 1 : (byte) 0);
            secondEntity.flags.addAll(this.flags);
            // Guarantee this copy is NOT invisible
            secondEntity.setFlag(EntityFlag.INVISIBLE, false);
            // Scale to 0 to show nametag
            secondEntity.getDirtyMetadata().put(EntityDataTypes.SCALE, 0.0f);
            // No bounding box as we don't want to interact with this entity
            secondEntity.getDirtyMetadata().put(EntityDataTypes.WIDTH, 0.0f);
            secondEntity.getDirtyMetadata().put(EntityDataTypes.HEIGHT, 0.0f);
            if (!secondEntity.valid) { // Spawn the entity once
                secondEntity.spawnEntity();
            }
        } else if (isNametagEmpty) {
            // We can just make an invisible entity
            // Reset scale of the proper armor stand
            dirtyMetadata.put(EntityDataTypes.SCALE, getScale());
            // Set the proper armor stand to invisible to show armor
            setFlag(EntityFlag.INVISIBLE, true);
            // Update offset
            updateOffsetRequirement(false);

            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
        } else {
            // Nametag is not empty and there is no armor
            // We don't need to make a new entity
            setFlag(EntityFlag.INVISIBLE, false);
            dirtyMetadata.put(EntityDataTypes.SCALE, 0.0f);
            // As the above is applied, we need an offset
            updateOffsetRequirement(!isMarker);

            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
        }
        if (sendMetadata) {
            this.updateBedrockMetadata();
        }
    }

    @Override
    public float getBoundingBoxWidth() {
        // For consistency with getBoundingBoxHeight()
        return super.getBoundingBoxWidth() * getScale();
    }

    @Override
    public float getBoundingBoxHeight() {
        // This is required so that EntityUtils#updateMountOffset() calculates the correct offset for small
        // armor stands. The bounding box height is not changed as the SCALE entity data handles that for us.
        return super.getBoundingBoxHeight() * getScale();
    }

    /**
     * @return the y offset required to position the name tag correctly
     */
    public float getYOffset() {
        if (!positionRequiresOffset || isMarker || secondEntity != null) {
            return 0;
        }
        return definition.height() * getScale();
    }

    /**
     * @return the scale according to Java
     */
    private float getScale() {
        return isSmall ? 0.5f : 1f;
    }

    /**
     * Set the offset to a new value; if it changed, update the position, too.
     */
    private void updateOffsetRequirement(boolean newValue) {
        if (newValue != positionRequiresOffset) {
            this.positionRequiresOffset = newValue;
            this.positionUpdateRequired = true;
            // Update the passenger offset as the armor stand's y offset has changed
            updatePassengerOffsets();
        }
    }

    @Override
    public Vector3f getBedrockRotation() {
        return Vector3f.from(getYaw(), getYaw(), getYaw());
    }
}
