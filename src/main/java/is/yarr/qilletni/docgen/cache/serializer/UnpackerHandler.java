package is.yarr.qilletni.docgen.cache.serializer;

import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.buffer.MessageBuffer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class UnpackerHandler {
    
    private final MessageUnpacker messageUnpacker;
    private final MethodHandle unpackerPosition;
    private final MethodHandle unpackerBuffer;


    public UnpackerHandler(MessageUnpacker messageUnpacker) throws NoSuchFieldException, IllegalAccessException {
        this.messageUnpacker = messageUnpacker;
        this.unpackerPosition = setupUnpackerPosition();
        this.unpackerBuffer = setupUnpackerBuffer();
    }

    private MethodHandle setupUnpackerPosition() throws NoSuchFieldException, IllegalAccessException {
        var field = MessageUnpacker.class.getDeclaredField("position");
        field.setAccessible(true);

        var lookup = MethodHandles.lookup();
        return lookup.unreflectGetter(field);
    }

    private MethodHandle setupUnpackerBuffer() throws NoSuchFieldException, IllegalAccessException {
        var field = MessageUnpacker.class.getDeclaredField("buffer");
        field.setAccessible(true);

        var lookup = MethodHandles.lookup();
        return lookup.unreflectGetter(field);
    }

    public int getUnpackerPosition() {
        try {
            return (int) unpackerPosition.invokeExact(messageUnpacker);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public MessageBuffer getMessageBuffer() {
        try {
            return (MessageBuffer) unpackerBuffer.invokeExact(messageUnpacker);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
