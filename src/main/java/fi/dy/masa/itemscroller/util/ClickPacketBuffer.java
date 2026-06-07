package fi.dy.masa.itemscroller.util;

import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

public class ClickPacketBuffer
{
    private static final Queue<Packet<?>> BUFFER = new ArrayDeque<>(2048);
    private static boolean shouldBufferPackets;
    private static boolean hasBufferedPackets;

    public static void reset()
    {
        shouldBufferPackets = false;
        hasBufferedPackets = false;
        BUFFER.clear();
    }

    public static int getBufferedActionsCount()
    {
        return BUFFER.size();
    }

    public static boolean shouldBufferClickPackets()
    {
        return shouldBufferPackets;
    }

    public static boolean shouldCancelWindowClicks()
    {
        // Don't cancel the clicks on the client if we have some Item Scroller actions in progress
        return shouldBufferPackets == false && BUFFER.isEmpty() == false;
    }

    public static void setShouldBufferClickPackets(boolean shouldBuffer)
    {
        shouldBufferPackets = shouldBuffer;
    }

    public static void bufferPacket(Packet<?> packet)
    {
        BUFFER.offer(packet);
        hasBufferedPackets = true;
    }

    public static void sendBufferedPackets(int maxCount)
    {
        Minecraft mc = Minecraft.getInstance();

        if (hasBufferedPackets)
        {
            if (mc.gui.screen() == null)
            {
                reset();
            }
            else if (mc.player != null)
            {
                maxCount = Math.min(maxCount, BUFFER.size());
    
                for (int i = 0; i < maxCount; ++i)
                {
					Packet<?> packet = BUFFER.poll();

					if (packet != null)
					{
						mc.player.connection.send(packet);
					}
                }

                hasBufferedPackets = BUFFER.isEmpty() == false;
            }
        }
    }
}
