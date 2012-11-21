package ext.jubilee;

import org.jruby.Ruby;
import org.jruby.runtime.load.BasicLibraryService;

import java.io.IOException;

public class ServerService implements BasicLibraryService {
    public boolean basicLoad(final Ruby ruby) throws IOException {
        Server.createServerClass(ruby);
        return true;
    }
}
