package jubilee;

import org.jruby.Ruby;
import org.jruby.jubilee.RubyHttpServerResponse;
import org.jruby.jubilee.RubyPlatformManager;
import org.jruby.jubilee.RubyServer;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.jubilee.impl.RubyNullIO;
import org.jruby.runtime.load.BasicLibraryService;

import java.io.IOException;

public class JubileeService implements BasicLibraryService {
    public boolean basicLoad(final Ruby ruby) throws IOException {
        RubyServer.createServerClass(ruby);
        RubyHttpServerResponse.createHttpServerResponseClass(ruby);
        RubyIORackInput.createIORackInputClass(ruby);
        RubyNullIO.createNullIOClass(ruby);
      RubyPlatformManager.createPlatformManagerClass(ruby);
        return true;
    }
}
