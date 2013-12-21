package jubilee;

import org.jruby.Ruby;
import org.jruby.jubilee.RubyHttpServerResponse;
import org.jruby.jubilee.RubyServer;
import org.jruby.jubilee.impl.NullIO;
import org.jruby.jubilee.impl.RubyIORackErrors;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.runtime.load.BasicLibraryService;

import java.io.IOException;

public class JubileeService implements BasicLibraryService {
    public boolean basicLoad(final Ruby ruby) throws IOException {
        RubyServer.createServerClass(ruby);
        RubyHttpServerResponse.createHttpServerResponseClass(ruby);
        RubyIORackErrors.createRubyIORackErrorsClass(ruby);
        RubyIORackInput.createRubyIORackInputClass(ruby);
        NullIO.createNullIOClass(ruby);
        return true;
    }
}
