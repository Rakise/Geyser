/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.display;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector4f;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;

public class DisplayEntity extends Entity {

    protected ItemData hand = ItemData.AIR;
    protected Vector3f translation = Vector3f.from(0, 0, 0);
    protected Vector3f scale = Vector3f.from(1, 1, 1);
    protected Vector3f rotation = Vector3f.from(0, 0, 0);

    public DisplayEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition,
            Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void updateMainHand(GeyserSession session) {
        if (!valid)
            return;

        MobEquipmentPacket handPacket = new MobEquipmentPacket();
        handPacket.setRuntimeEntityId(geyserId);
        handPacket.setItem(hand);
        handPacket.setHotbarSlot(-1);
        handPacket.setInventorySlot(0);
        handPacket.setContainerId(ContainerId.INVENTORY);

        session.sendUpstreamPacket(handPacket);
    }

    public void setTranslation(EntityMetadata<Vector3f, ?> entityMetadata) {
        this.translation = translation.add(entityMetadata.getValue());
    }

    public void setScale(EntityMetadata<Vector3f, ?> entityMetadata) {
        this.scale = scale.mul(entityMetadata.getValue());
    }

    public void setLeftRotation(EntityMetadata<Vector4f, ?> entityMetadata) {
        setRotation(entityMetadata.getValue());
    }

    public void setRightRotation(EntityMetadata<Vector4f, ?> entityMetadata) {
        setRotation(entityMetadata.getValue());
    }

    protected void setRotation(Vector4f qRotation) {
        Quaternionf q = Quaternionf.from(qRotation.getX(), qRotation.getY(), qRotation.getZ(), qRotation.getW());
        Vector3f s = getNonNormalScale(q);
        Vector3f r = toEulerZYX(q);

        this.scale = scale.mul(s);
        this.rotation = rotation.add(r);
    }

    protected Vector3f getNonNormalScale(Quaternionf q) {
        Quaternionf qx = q.mul(0, 1, 0, 0).mul(q.conjugate());
        Quaternionf qy = q.mul(0, 0, 1, 0).mul(q.conjugate());
        Quaternionf qz = q.mul(0, 0, 0, 1).mul(q.conjugate());

        float x = (float) Math.sqrt(qx.getX() * qx.getX() + qx.getY() * qx.getY() + qx.getZ() * qx.getZ());
        float y = (float) Math.sqrt(qy.getX() * qy.getX() + qy.getY() * qy.getY() + qy.getZ() * qy.getZ());
        float z = (float) Math.sqrt(qz.getX() * qz.getX() + qz.getY() * qz.getY() + qz.getZ() * qz.getZ());

        return Vector3f.from(x, y, z);
    }

    protected Vector3f toEulerZYX(Quaternionf q) {
        Quaternionf qn = q.normalize();

        float w = qn.getW();
        float x = qn.getX();
        float y = qn.getY();
        float z = qn.getZ();

        float yaw = (float) Math.atan2(2 * (y * w - x * z), 1 - 2 * (y * y + z * z));
        float pitch = (float) Math.asin(2 * (x * y + z * w));
        float roll = (float) Math.atan2(2 * (x * w - y * z), 1 - 2 * (x * x + z * z));

        return Vector3f.from(yaw, pitch, roll);
    }



    protected void hackRotation(float x, float y, float z) {
        propertyManager.add("geyser:rotation_x", x);
        propertyManager.add("geyser:rotation_y", y);
        propertyManager.add("geyser:rotation_z", z);
        updateBedrockEntityProperties();
    }

}
