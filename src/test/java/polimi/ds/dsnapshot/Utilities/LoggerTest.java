package polimi.ds.dsnapshot.Utilities;

import org.junit.jupiter.api.Test;

import java.util.Optional;

public class LoggerTest {

    @Test
    public void testLogger() {
        LoggerManager.getInstance().mutableInfo("test logger", Optional.empty(),Optional.empty());

        LoggerManager.getInstance().mutableInfo("test logger1", Optional.of(this.getClass().getName()), Optional.of("test"));
    }
}
