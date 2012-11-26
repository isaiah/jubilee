package jubilee;

import org.jruby.Ruby;
import org.jruby.runtime.load.BasicLibraryService;

import org.jruby.jubilee.Server;

import java.io.IOException;

public class JubileeService implements BasicLibraryService {
    public boolean basicLoad(final Ruby ruby) throws IOException {
        Server.createServerClass(ruby);
        return true;
    }
}
