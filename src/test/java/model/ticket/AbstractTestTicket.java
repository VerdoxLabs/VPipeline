package model.ticket;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import de.verdox.vpipeline.api.ticket.Ticket;

import java.util.UUID;

public abstract class AbstractTestTicket implements Ticket {
    public int testInteger;
    public String testString;
    public UUID testUUID;

    public AbstractTestTicket(int testInteger, String testString, UUID testUUID) {
        this.testInteger = testInteger;
        this.testString = testString;
        this.testUUID = testUUID;
    }

    public AbstractTestTicket() {

    }

    @Override
    public void writeInputParameter(ByteArrayDataOutput out) {
        out.writeInt(testInteger);
        out.writeUTF(testString);
        out.writeUTF(testUUID.toString());
    }

    @Override
    public void readInputParameter(ByteArrayDataInput in) {
        this.testInteger = in.readInt();
        this.testString = in.readUTF();
        this.testUUID = UUID.fromString(in.readUTF());
    }

    @Override
    public void triggerDataPreloadBlocking() {

    }

    @Override
    public TriState apply(Object[] objects) {
        return TriState.TRUE;
    }
}
