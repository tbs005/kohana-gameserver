package koh.game.entities.spells;

import java.io.IOException;
import java.io.ObjectOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.mina.core.buffer.IoBuffer;

/**
 *
 * @author Neo-Craft
 */
public class EffectInstanceMinMax extends EffectInstance {

    @Override
    public byte SerializationIdentifier() {
        return 5;
    }

    public int MinValue;
    public int MaxValue;

    public EffectInstanceMinMax(EffectInstance Parent, int MinValue, int MaxValue) {
        super(Parent.effectUid, Parent.effectId, Parent.targetId, Parent.targetMask, Parent.duration, Parent.random, Parent.group, Parent.hidden, Parent.rawZone, Parent.delay, Parent.triggers, Parent.order);
        this.MinValue = MinValue;
        this.MaxValue = MaxValue;
    }
    
    @Override
    public EffectInstance Clone() {
        return new EffectInstanceMinMax(this, this.MinValue,this.MaxValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EffectInstanceMinMax)) {
            return false;
        }
        EffectInstanceMinMax rhs = (EffectInstanceMinMax) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(MinValue, rhs.MinValue).
                append(MaxValue, rhs.MaxValue).
                isEquals();
    }

    public EffectInstanceMinMax(IoBuffer buf) {
        super(buf);
        this.MinValue = buf.getInt();
        this.MaxValue = buf.getInt();
    }
    
    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        super.writeExternal(objectOutput);
        objectOutput.writeInt(this.MinValue);
        objectOutput.writeInt(this.MaxValue);
    }

    @Override
    public void toBinary(IoBuffer buf) {
        super.toBinary(buf);
        buf.putInt(MinValue);
        buf.putInt(MinValue);
    }

}
