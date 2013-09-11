package jubilee;

import org.jruby.Ruby;
import org.jruby.jubilee.Server;
import org.jruby.jubilee.impl.NullIO;
import org.jruby.jubilee.impl.RubyIORackErrors;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.runtime.load.BasicLibraryService;

import java.io.IOException;

public class JubileeService implements BasicLibraryService {
    public boolean basicLoad(final Ruby ruby) throws IOException {
        Server.createServerClass(ruby);
        RubyIORackErrors.createRubyIORackErrorsClass(ruby);
        RubyIORackInput.createRubyIORackInputClass(ruby);
        NullIO.createNullIOClass(ruby);
        return true;
    }
}
