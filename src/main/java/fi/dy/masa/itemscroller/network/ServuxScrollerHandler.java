package fi.dy.masa.itemscroller.network;

import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.data.DataManager;
import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.PacketSplitter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public abstract class ServuxScrollerHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T>
{
    private static final ServuxScrollerHandler<ServuxScrollerPacket.Payload> INSTANCE = new ServuxScrollerHandler<>() {
        @Override
        public void receive(ServuxScrollerPacket.Payload payload, ClientPlayNetworking.Context context)
        {
            ServuxScrollerHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxScrollerHandler<ServuxScrollerPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "scroller");

    private boolean servuxRegistered;
    private boolean payloadRegistered = false;
    private int failures = 0;
    private static final int MAX_FAILURES = 4;
    private long readingSessionKey = -1;

    @Override
    public Identifier getPayloadChannel() { return CHANNEL_ID; }

    @Override
    public boolean isPlayRegistered(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID))
        {
            return payloadRegistered;
        }

        return false;
    }

    @Override
    public void setPlayRegistered(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID))
        {
            this.payloadRegistered = true;
        }
    }

    @Override
    public <P extends IClientPayloadData> void decodeClientData(Identifier channel, P data)
    {
        ServuxScrollerPacket packet = (ServuxScrollerPacket) data;

        if (!channel.equals(CHANNEL_ID) || packet == null)
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_S2C_METADATA ->
            {
                if (DataManager.getInstance().receiveMetadata(packet.getCompound()))
                {
                    this.servuxRegistered = true;
                }
            }
            case PACKET_S2C_MASS_CRAFT_RESPONSE -> DataManager.getInstance().receiveMassCraftResponse(packet.getCompound());
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                if (this.readingSessionKey == -1)
                {
                    this.readingSessionKey = Random.create(Util.getMeasuringTimeMs()).nextLong();
                }

                ItemScroller.printDebug("ServuxScrollerHandler#decodeClientData(): received Recipe Data Packet Slice of size {} (in bytes) // reading session key [{}]", packet.getTotalSize(), this.readingSessionKey);
                PacketByteBuf fullPacket = PacketSplitter.receive(this, this.readingSessionKey, packet.getBuffer());

                if (fullPacket != null)
                {
                    try
                    {
                        this.readingSessionKey = -1;
                        DataManager.getInstance().receiveRecipeManager((NbtCompound) fullPacket.readNbt(NbtSizeTracker.ofUnlimitedBytes()));
                    }
                    catch (Exception e)
                    {
                        ItemScroller.logger.error("ServuxScrollerHandler#decodeClientData(): Recipe Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                    }
                }
            }
            default -> ItemScroller.logger.warn("ServuxScrollerHandler#decodeClientData(): received unhandled packetType {} of size {} bytes.", packet.getPacketType(), packet.getTotalSize());
        }
    }

    @Override
    public void reset(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
            this.failures = 0;
            this.readingSessionKey = -1;
        }
    }

    public void resetFailures(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID) && this.failures > 0)
        {
            this.failures = 0;
        }
    }

    @Override
    public void encodeWithSplitter(PacketByteBuf buf, ClientPlayNetworkHandler handler)
    {
        // NO-OP
    }

    @Override
    public void receivePlayPayload(T payload, ClientPlayNetworking.Context ctx)
    {
        if (payload.getId().id().equals(CHANNEL_ID))
        {
            ServuxScrollerHandler.INSTANCE.decodeClientData(CHANNEL_ID, ((ServuxScrollerPacket.Payload) payload).data());
        }
    }

    @Override
    public <P extends IClientPayloadData> void encodeClientData(P data)
    {
        ServuxScrollerPacket packet = (ServuxScrollerPacket) data;

        if (!ServuxScrollerHandler.INSTANCE.sendPlayPayload(new ServuxScrollerPacket.Payload(packet)))
        {
            if (this.failures > MAX_FAILURES)
            {
                ItemScroller.printDebug("encodeClientData(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", MAX_FAILURES);
                this.servuxRegistered = false;
                ServuxScrollerHandler.INSTANCE.unregisterPlayReceiver();
                DataManager.getInstance().onPacketFailure();
            }
            else
            {
                this.failures++;
            }
        }
    }
}
