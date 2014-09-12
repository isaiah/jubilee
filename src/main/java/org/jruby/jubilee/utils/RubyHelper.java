package org.jruby.jubilee.utils;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.util.ByteList;

/**
 * Ruby reflection helper utilities.
 *
 * @author Bob McWhirter <bmcwhirt@redhat.com>
 */
public class RubyHelper {

    public static final RubyString toUsAsciiRubyString(final Ruby runtime, final String string) {
        byte[] bytes = new byte[string.length()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) string.charAt(i);
        }
        return toUsAsciiRubyString(runtime, bytes);
    }

    public static final RubyString toUsAsciiRubyString(final Ruby runtime, final byte[] bytes) {
        return RubyString.newString(runtime, new ByteList(bytes, USASCIIEncoding.INSTANCE, false));
    }

    public static final RubyString toUnicodeRubyString(final Ruby runtime, final String string) {
        return RubyString.newUnicodeString(runtime, string);
    }
}

